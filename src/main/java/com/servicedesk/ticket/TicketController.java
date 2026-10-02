package com.servicedesk.ticket;

import com.servicedesk.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService service;

    public record CreateTicketRequest(@NotBlank String title, @NotBlank String description,
                                      TicketPriority priority, Long teamId) {}
    public record AssignRequest(@NotNull Long agentId) {}
    public record StatusRequest(@NotNull TicketStatus status) {}
    public record CommentRequest(@NotBlank String body) {}
    public record CommentDto(Long id, String author, String body, Instant createdAt) {
        static CommentDto from(Comment c) {
            return new CommentDto(c.getId(), c.getAuthor().getName(), c.getBody(), c.getCreatedAt());
        }
    }

    @PostMapping
    public ResponseEntity<TicketDto> create(@AuthenticationPrincipal User user, @Valid @RequestBody CreateTicketRequest r) {
        Ticket t = service.create(user, r.title(), r.description(), r.priority(), r.teamId());
        return ResponseEntity.status(HttpStatus.CREATED).body(TicketDto.from(t));
    }

    @GetMapping
    public List<TicketDto> list(@AuthenticationPrincipal User user) {
        return service.list(user).stream().map(TicketDto::from).toList();
    }

    @GetMapping("/{id}")
    public TicketDto get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return TicketDto.from(service.get(id, user));
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasRole('AGENT')")
    public TicketDto claim(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return TicketDto.from(service.claim(id, user));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public TicketDto assign(@AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody AssignRequest r) {
        return TicketDto.from(service.assign(id, r.agentId(), user));
    }

    @PostMapping("/{id}/status")
    public TicketDto status(@AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return TicketDto.from(service.transition(id, r.status(), user));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentDto> comment(@AuthenticationPrincipal User user, @PathVariable Long id,
                                              @Valid @RequestBody CommentRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentDto.from(service.addComment(id, user, r.body())));
    }

    @GetMapping("/{id}/comments")
    public List<CommentDto> comments(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return service.comments(id, user).stream().map(CommentDto::from).toList();
    }
}
