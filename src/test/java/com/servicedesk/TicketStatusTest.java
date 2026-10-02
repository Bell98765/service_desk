package com.servicedesk;

import static com.servicedesk.ticket.TicketStatus.*;
import static org.junit.jupiter.api.Assertions.*;

import com.servicedesk.ticket.TicketStatus;
import org.junit.jupiter.api.Test;

class TicketStatusTest {

    @Test
    void happyPathIsAllowed() {
        assertTrue(NEW.canTransitionTo(ASSIGNED));
        assertTrue(ASSIGNED.canTransitionTo(IN_PROGRESS));
        assertTrue(IN_PROGRESS.canTransitionTo(RESOLVED));
        assertTrue(RESOLVED.canTransitionTo(CLOSED));
    }

    @Test
    void cannotSkipStates() {
        assertFalse(NEW.canTransitionTo(CLOSED));
        assertFalse(NEW.canTransitionTo(RESOLVED));
        assertFalse(ASSIGNED.canTransitionTo(CLOSED));
    }

    @Test
    void onHoldRoundTrip() {
        assertTrue(ASSIGNED.canTransitionTo(ON_HOLD));
        assertTrue(IN_PROGRESS.canTransitionTo(ON_HOLD));
        assertTrue(ON_HOLD.canTransitionTo(IN_PROGRESS));
        assertFalse(ON_HOLD.canTransitionTo(RESOLVED));
    }

    @Test
    void closedIsTerminal() {
        for (TicketStatus s : TicketStatus.values()) assertFalse(CLOSED.canTransitionTo(s));
    }
}
