package com.servicedesk.audit;

import com.servicedesk.ticket.Ticket;
import com.servicedesk.ticket.TicketStatus;
import com.servicedesk.user.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final TicketStatusHistoryRepository repo;

    /** Joins the caller's transaction, so the audit row and the ticket change commit (or roll back) together. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Ticket t, TicketStatus from, TicketStatus to, User actor, String note) {
        repo.save(new TicketStatusHistory(t.getId(), from, to,
                actor == null ? null : actor.getId(),
                t.getAssignedAgent() == null ? null : t.getAssignedAgent().getId(), note));
    }

    @Transactional(readOnly = true)
    public List<TicketStatusHistory> history(Long ticketId) {
        return repo.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }
}
