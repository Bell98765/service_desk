package com.servicedesk.sla;

import com.servicedesk.ticket.TicketPriority;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sla_policies")
@Getter @Setter @NoArgsConstructor
public class SlaPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private TicketPriority priority;

    private int responseMinutes;
    private int resolutionMinutes;

    public SlaPolicy(TicketPriority priority, int responseMinutes, int resolutionMinutes) {
        this.priority = priority;
        this.responseMinutes = responseMinutes;
        this.resolutionMinutes = resolutionMinutes;
    }
}
