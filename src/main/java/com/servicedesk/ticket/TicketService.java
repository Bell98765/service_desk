package com.servicedesk.ticket;

import com.servicedesk.audit.AuditService;
import com.servicedesk.common.ApiException;
import com.servicedesk.notification.TicketEvent;
import com.servicedesk.sla.SlaPolicy;
import com.servicedesk.sla.SlaService;
import com.servicedesk.user.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {
    private static final Set<TicketStatus> ACTIVE =
            EnumSet.of(TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD);

    private final TicketRepository tickets;
    private final CommentRepository comments;
    private final UserRepository users;
    private final TeamRepository teams;
    private final SlaService sla;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    // ---------- create + auto-routing ----------

    @Transactional
    public Ticket create(User requester, String title, String description, TicketPriority priority, Long teamId) {
        Ticket t = new Ticket();
        t.setTitle(title);
        t.setDescription(description);
        t.setPriority(priority == null ? TicketPriority.MEDIUM : priority);
        t.setRequester(requester);
        t.setStatus(TicketStatus.NEW);
        Instant now = Instant.now();
        t.setCreatedAt(now);
        SlaPolicy policy = sla.policyFor(t.getPriority());
        t.setResponseDeadline(now.plus(Duration.ofMinutes(policy.getResponseMinutes())));
        t.setSlaDeadline(now.plus(Duration.ofMinutes(policy.getResolutionMinutes())));
        if (teamId != null) {
            t.setTeam(teams.findById(teamId).orElseThrow(() -> ApiException.badRequest("Unknown team")));
        }
        tickets.save(t);
        audit.record(t, null, TicketStatus.NEW, requester, "Ticket created");
        if (t.getTeam() != null) autoRoute(t, requester);
        publish(t, "Ticket #" + t.getId() + " created: " + t.getTitle());
        return t;
    }

    /** Least-busy routing: pick the team agent with the fewest active tickets. */
    private void autoRoute(Ticket t, User actor) {
        users.findByTeamAndRole(t.getTeam(), Role.AGENT).stream()
                .min(Comparator.comparingLong(a -> tickets.countByAssignedAgentAndStatusIn(a, ACTIVE)))
                .ifPresent(agent -> {
                    t.setAssignedAgent(agent);
                    t.setStatus(TicketStatus.ASSIGNED);
                    audit.record(t, TicketStatus.NEW, TicketStatus.ASSIGNED, actor,
                            "Auto-routed to " + agent.getName());
                });
    }

    // ---------- reads (role + team scoped) ----------

    @Transactional(readOnly = true)
    public List<Ticket> list(User user) {
        return switch (user.getRole()) {
            case REQUESTER -> tickets.findByRequesterOrderByCreatedAtDesc(user);
            case AGENT -> user.getTeam() == null
                    ? tickets.findByAssignedAgentOrderByCreatedAtDesc(user)
                    : tickets.findByTeamOrAssignedAgentOrderByCreatedAtDesc(user.getTeam(), user);
            case MANAGER, ADMIN -> tickets.findAllByOrderByCreatedAtDesc();
        };
    }

    @Transactional(readOnly = true)
    public Ticket get(Long id, User user) {
        Ticket t = find(id);
        if (!canView(t, user)) throw ApiException.forbidden("You cannot access this ticket");
        return t;
    }

    // ---------- claim / assign ----------

    /** Agent claims an unassigned ticket. @Version guarantees only one of two racing agents wins. */
    @Transactional
    public Ticket claim(Long id, User agent) {
        Ticket t = find(id);
        if (t.getAssignedAgent() != null || t.getStatus() != TicketStatus.NEW) {
            throw ApiException.conflict("Ticket is already claimed");
        }
        if (t.getTeam() != null && (agent.getTeam() == null || !agent.getTeam().getId().equals(t.getTeam().getId()))) {
            throw ApiException.forbidden("Ticket belongs to another team");
        }
        t.setAssignedAgent(agent);
        t.setStatus(TicketStatus.ASSIGNED);
        tickets.saveAndFlush(t);
        audit.record(t, TicketStatus.NEW, TicketStatus.ASSIGNED, agent, "Claimed by " + agent.getName());
        publish(t, "Ticket #" + t.getId() + " claimed by " + agent.getName());
        return t;
    }

    @Transactional
    public Ticket assign(Long id, Long agentId, User actor) {
        Ticket t = find(id);
        User agent = users.findById(agentId).filter(u -> u.getRole() == Role.AGENT)
                .orElseThrow(() -> ApiException.badRequest("Agent not found"));
        if (t.getStatus() == TicketStatus.RESOLVED || t.getStatus() == TicketStatus.CLOSED) {
            throw ApiException.badRequest("Cannot assign a " + t.getStatus() + " ticket");
        }
        TicketStatus from = t.getStatus();
        t.setAssignedAgent(agent);
        if (from == TicketStatus.NEW) t.setStatus(TicketStatus.ASSIGNED);
        tickets.saveAndFlush(t);
        audit.record(t, from, t.getStatus(), actor, "Assigned to " + agent.getName());
        publish(t, "Ticket #" + t.getId() + " assigned to " + agent.getName());
        return t;
    }

    // ---------- state machine ----------

    @Transactional
    public Ticket transition(Long id, TicketStatus to, User actor) {
        Ticket t = find(id);
        if (!canView(t, actor)) throw ApiException.forbidden("You cannot access this ticket");
        TicketStatus from = t.getStatus();
        if (!from.canTransitionTo(to)) {
            throw ApiException.badRequest("Invalid transition: " + from + " -> " + to);
        }
        if (to == TicketStatus.ASSIGNED && t.getAssignedAgent() == null) {
            throw ApiException.badRequest("Use claim or assign to give the ticket an agent");
        }
        authorizeTransition(t, from, to, actor);
        t.setStatus(to);
        if (to == TicketStatus.IN_PROGRESS && t.getFirstRespondedAt() == null) {
            t.setFirstRespondedAt(Instant.now());
        }
        tickets.saveAndFlush(t);
        audit.record(t, from, to, actor, "Status changed");
        publish(t, "Ticket #" + t.getId() + " moved " + from + " -> " + to);
        return t;
    }

    private void authorizeTransition(Ticket t, TicketStatus from, TicketStatus to, User actor) {
        switch (actor.getRole()) {
            case MANAGER, ADMIN -> { }
            case AGENT -> {
                if (t.getAssignedAgent() == null || !t.getAssignedAgent().getId().equals(actor.getId())) {
                    throw ApiException.forbidden("Only the assigned agent can change this ticket's status");
                }
            }
            case REQUESTER -> {
                // requester may only accept (close) or reject (reopen) a resolution
                boolean ok = from == TicketStatus.RESOLVED
                        && (to == TicketStatus.CLOSED || to == TicketStatus.IN_PROGRESS);
                if (!ok) throw ApiException.forbidden("Requesters can only close or reopen resolved tickets");
            }
        }
    }

    // ---------- comments ----------

    @Transactional
    public Comment addComment(Long ticketId, User author, String body) {
        Ticket t = get(ticketId, author);
        Comment c = new Comment();
        c.setTicket(t);
        c.setAuthor(author);
        c.setBody(body);
        c.setCreatedAt(Instant.now());
        comments.save(c);
        if (author.getRole() == Role.AGENT && t.getFirstRespondedAt() == null) {
            t.setFirstRespondedAt(Instant.now());
            tickets.save(t);
        }
        publish(t, author.getName() + " commented on ticket #" + t.getId());
        return c;
    }

    @Transactional(readOnly = true)
    public List<Comment> comments(Long ticketId, User user) {
        get(ticketId, user);
        return comments.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    // ---------- helpers ----------

    private Ticket find(Long id) {
        return tickets.findById(id).orElseThrow(() -> ApiException.notFound("Ticket not found"));
    }

    private boolean canView(Ticket t, User u) {
        return switch (u.getRole()) {
            case MANAGER, ADMIN -> true;
            case REQUESTER -> t.getRequester().getId().equals(u.getId());
            case AGENT -> (t.getAssignedAgent() != null && t.getAssignedAgent().getId().equals(u.getId()))
                    || (t.getTeam() != null && u.getTeam() != null && t.getTeam().getId().equals(u.getTeam().getId()));
        };
    }

    private void publish(Ticket t, String message) {
        Set<Long> recipients = new LinkedHashSet<>();
        recipients.add(t.getRequester().getId());
        if (t.getAssignedAgent() != null) recipients.add(t.getAssignedAgent().getId());
        events.publishEvent(new TicketEvent(t.getId(), message, recipients));
    }
}
