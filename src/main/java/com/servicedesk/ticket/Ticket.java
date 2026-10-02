package com.servicedesk.ticket;

import com.servicedesk.user.Team;
import com.servicedesk.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tickets")
@Getter @Setter @NoArgsConstructor
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketPriority priority;

    @ManyToOne(optional = false)
    private User requester;

    @ManyToOne
    private User assignedAgent;

    @ManyToOne
    private Team team;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant responseDeadline;
    private Instant slaDeadline;
    private Instant firstRespondedAt;

    private boolean escalated;

    /** Optimistic lock: a concurrent update to the same ticket fails instead of silently overwriting. */
    @Version
    private Long version;
}
