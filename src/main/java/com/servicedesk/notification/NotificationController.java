package com.servicedesk.notification;

import com.servicedesk.user.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository repo;

    @GetMapping
    public List<Notification> mine(@AuthenticationPrincipal User user) {
        return repo.findByRecipientIdOrderByCreatedAtDesc(user.getId());
    }
}
