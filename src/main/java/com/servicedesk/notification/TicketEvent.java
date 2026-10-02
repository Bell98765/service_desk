package com.servicedesk.notification;

import java.util.Collection;

/** Domain event published on ticket changes; consumed asynchronously by NotificationListener. */
public record TicketEvent(Long ticketId, String message, Collection<Long> recipientIds) {}
