package com.servicedesk.sla;

import com.servicedesk.audit.AuditService;
import com.servicedesk.common.ApiException;
import com.servicedesk.notification.TicketEvent;
import com.servicedesk.ticket.*;
import com.servicedesk.user.Role;
import com.servicedesk.user.UserRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlaService {
    /** ON_HOLD is treated as "SLA clock paused", so held tickets are not escalated. */
    private static final Set<TicketStatus> TRACKED =
            EnumSet.of(TicketStatus.NEW, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS);

    private final SlaPolicyRepository policies;
    private final TicketRepository tickets;
    private final UserRepository users;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    /** Cached in-memory (swap the cache manager for Redis without touching this code). */
    @Cacheable("slaPolicies")
    public SlaPolicy policyFor(TicketPriority priority) {
        return policies.findByPriority(priority)
                .orElseThrow(() -> ApiException.badRequest("No SLA policy for " + priority));
    }

    @CacheEvict(value = "slaPolicies", allEntries = true)
    @Transactional
    public SlaPolicy update(TicketPriority priority, int responseMinutes, int resolutionMinutes) {
        SlaPolicy p = policies.findByPriority(priority).orElseGet(() -> new SlaPolicy(priority, 0, 0));
        p.setResponseMinutes(responseMinutes);
        p.setResolutionMinutes(resolutionMinutes);
        return policies.save(p);
    }

    /** Flags overdue tickets, bumps priority one level, writes an audit row and notifies stakeholders. */
    @Transactional
    public int escalateOverdue() {
        List<Ticket> overdue =
                tickets.findByEscalatedFalseAndSlaDeadlineBeforeAndStatusIn(Instant.now(), TRACKED);
        for (Ticket t : overdue) {
            t.setEscalated(true);
            t.setPriority(t.getPriority().escalate());
            tickets.save(t);
            audit.record(t, t.getStatus(), t.getStatus(), null,
                    "SLA breached - escalated, priority now " + t.getPriority());

            Set<Long> recipients = new LinkedHashSet<>();
            recipients.add(t.getRequester().getId());
            if (t.getAssignedAgent() != null) recipients.add(t.getAssignedAgent().getId());
            users.findByRole(Role.MANAGER).forEach(m -> recipients.add(m.getId()));
            events.publishEvent(new TicketEvent(t.getId(),
                    "SLA breached on ticket #" + t.getId() + " - escalated to " + t.getPriority(), recipients));
        }
        if (!overdue.isEmpty()) log.info("Escalated {} overdue ticket(s)", overdue.size());
        return overdue.size();
    }
}
