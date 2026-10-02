package com.servicedesk.audit;

import com.servicedesk.ticket.TicketService;
import com.servicedesk.user.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuditController {
    private final AuditService audit;
    private final TicketService tickets;

    @GetMapping("/api/tickets/{id}/history")
    public List<TicketStatusHistory> history(@AuthenticationPrincipal User user, @PathVariable Long id) {
        tickets.get(id, user); // enforces the same visibility rules as the ticket itself
        return audit.history(id);
    }
}
