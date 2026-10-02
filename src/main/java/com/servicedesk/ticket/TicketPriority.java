package com.servicedesk.ticket;

public enum TicketPriority {
    LOW, MEDIUM, HIGH, CRITICAL;

    public TicketPriority escalate() {
        return this == CRITICAL ? CRITICAL : values()[ordinal() + 1];
    }
}
