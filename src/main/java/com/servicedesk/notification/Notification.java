package com.servicedesk.notification;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recipientId;

    private Long ticketId;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private boolean seen;

    public Notification(Long recipientId, Long ticketId, String message) {
        this.recipientId = recipientId;
        this.ticketId = ticketId;
        this.message = message;
    }
}
