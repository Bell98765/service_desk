package com.servicedesk.audit;

import com.servicedesk.ticket.TicketStatus;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** Append-only audit row. @Immutable makes Hibernate refuse UPDATEs; no setters are exposed. */
@Entity
@Immutable
@Table(name = "ticket_status_history")
@Getter @NoArgsConstructor
public class TicketStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ticketId;

    @Enumerated(EnumType.STRING)
    private TicketStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus toStatus;

    /** null = system action (e.g. SLA escalation job) */
    private Long actorId;
    private Long assignedAgentId;
    private String note;

    @Column(nullable = false)
    private Instant createdAt;

    public TicketStatusHistory(Long ticketId, TicketStatus fromStatus, TicketStatus toStatus,
                               Long actorId, Long assignedAgentId, String note) {
        this.ticketId = ticketId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorId = actorId;
        this.assignedAgentId = assignedAgentId;
        this.note = note;
        this.createdAt = Instant.now();
    }
}
