package com.servicedesk.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Runs on a separate thread and only after the publishing transaction commits,
 * so notification failures never roll back a ticket change.
 * Swap the body for an email sender or a RabbitMQ publisher later.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {
    private final NotificationRepository repo;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(TicketEvent e) {
        for (Long userId : e.recipientIds()) {
            repo.save(new Notification(userId, e.ticketId(), e.message()));
            log.info("[notify] user={} ticket={} :: {}", userId, e.ticketId(), e.message());
        }
    }
}
