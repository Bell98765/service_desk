package com.servicedesk.ticket;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Ticket lifecycle state machine:
 * NEW -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED, with ON_HOLD reachable from
 * ASSIGNED / IN_PROGRESS. A resolved ticket can be reopened (RESOLVED -> IN_PROGRESS).
 */
public enum TicketStatus {
    NEW, ASSIGNED, IN_PROGRESS, ON_HOLD, RESOLVED, CLOSED;

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = new EnumMap<>(TicketStatus.class);

    static {
        ALLOWED.put(NEW, EnumSet.of(ASSIGNED));
        ALLOWED.put(ASSIGNED, EnumSet.of(IN_PROGRESS, ON_HOLD));
        ALLOWED.put(IN_PROGRESS, EnumSet.of(RESOLVED, ON_HOLD));
        ALLOWED.put(ON_HOLD, EnumSet.of(ASSIGNED, IN_PROGRESS));
        ALLOWED.put(RESOLVED, EnumSet.of(CLOSED, IN_PROGRESS));
        ALLOWED.put(CLOSED, EnumSet.noneOf(TicketStatus.class));
    }

    public boolean canTransitionTo(TicketStatus target) {
        return ALLOWED.get(this).contains(target);
    }
}
