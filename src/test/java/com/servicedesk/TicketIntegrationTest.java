package com.servicedesk;

import static org.junit.jupiter.api.Assertions.*;

import com.servicedesk.audit.AuditService;
import com.servicedesk.common.ApiException;
import com.servicedesk.sla.SlaService;
import com.servicedesk.ticket.*;
import com.servicedesk.user.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Runs against a real PostgreSQL in Docker (no H2). */
@SpringBootTest(properties = "app.sla.check-interval-ms=3600000")
@Testcontainers
class TicketIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired UserService users;
    @Autowired TeamRepository teams;
    @Autowired TicketService tickets;
    @Autowired TicketRepository ticketRepo;
    @Autowired AuditService audit;
    @Autowired SlaService sla;

    private User user(String prefix, Role role, Team team) {
        return users.createUser(prefix, prefix + "-" + UUID.randomUUID() + "@test.local", "password123", role, team);
    }

    @Test
    void onlyOneAgentCanClaimATicket() throws Exception {
        Team team = teams.save(new Team("claim-" + UUID.randomUUID()));
        User requester = user("req", Role.REQUESTER, null);
        User a1 = user("a1", Role.AGENT, team);
        User a2 = user("a2", Role.AGENT, team);
        // created without a team => stays NEW (no auto-routing), both agents race to claim it
        Ticket t = tickets.create(requester, "VPN down", "Cannot connect", TicketPriority.HIGH, null);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        Callable<Boolean> c1 = () -> tryClaim(go, t.getId(), a1);
        Callable<Boolean> c2 = () -> tryClaim(go, t.getId(), a2);
        List<Future<Boolean>> results = List.of(pool.submit(c1), pool.submit(c2));
        go.countDown();
        long winners = 0;
        for (Future<Boolean> f : results) if (f.get(30, TimeUnit.SECONDS)) winners++;
        pool.shutdown();

        assertEquals(1, winners, "exactly one agent must win the claim");
    }

    private boolean tryClaim(CountDownLatch go, Long ticketId, User agent) throws InterruptedException {
        go.await();
        try {
            tickets.claim(ticketId, agent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void autoRoutesToLeastBusyAgentAndWritesAudit() {
        Team team = teams.save(new Team("route-" + UUID.randomUUID()));
        User requester = user("req", Role.REQUESTER, null);
        user("b1", Role.AGENT, team);
        user("b2", Role.AGENT, team);

        Ticket t1 = tickets.create(requester, "One", "d", TicketPriority.MEDIUM, team.getId());
        Ticket t2 = tickets.create(requester, "Two", "d", TicketPriority.MEDIUM, team.getId());

        assertEquals(TicketStatus.ASSIGNED, t1.getStatus());
        assertNotEquals(t1.getAssignedAgent().getId(), t2.getAssignedAgent().getId());
        assertEquals(2, audit.history(t1.getId()).size()); // created + auto-routed
    }

    @Test
    void rejectsInvalidTransition() {
        User requester = user("req", Role.REQUESTER, null);
        User admin = user("adm", Role.ADMIN, null);
        Ticket t = tickets.create(requester, "Printer", "jammed", TicketPriority.LOW, null);
        assertThrows(ApiException.class, () -> tickets.transition(t.getId(), TicketStatus.CLOSED, admin));
    }

    @Test
    void overdueTicketsAreEscalatedOnce() {
        User requester = user("req", Role.REQUESTER, null);
        Ticket t = tickets.create(requester, "Slow", "d", TicketPriority.LOW, null);
        t.setSlaDeadline(java.time.Instant.now().minusSeconds(60));
        ticketRepo.save(t);

        sla.escalateOverdue();
        Ticket after = ticketRepo.findById(t.getId()).orElseThrow();
        assertTrue(after.isEscalated());
        assertEquals(TicketPriority.MEDIUM, after.getPriority());

        sla.escalateOverdue(); // idempotent
        assertEquals(TicketPriority.MEDIUM, ticketRepo.findById(t.getId()).orElseThrow().getPriority());
    }
}
