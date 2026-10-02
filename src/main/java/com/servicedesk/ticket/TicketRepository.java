package com.servicedesk.ticket;

import com.servicedesk.user.Team;
import com.servicedesk.user.User;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByRequesterOrderByCreatedAtDesc(User requester);
    List<Ticket> findByAssignedAgentOrderByCreatedAtDesc(User agent);
    List<Ticket> findByTeamOrAssignedAgentOrderByCreatedAtDesc(Team team, User agent);
    List<Ticket> findAllByOrderByCreatedAtDesc();
    long countByAssignedAgentAndStatusIn(User agent, Collection<TicketStatus> statuses);
    List<Ticket> findByEscalatedFalseAndSlaDeadlineBeforeAndStatusIn(Instant now, Collection<TicketStatus> statuses);
}
