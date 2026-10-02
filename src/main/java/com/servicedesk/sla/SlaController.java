package com.servicedesk.sla;

import com.servicedesk.ticket.TicketPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sla-policies")
@RequiredArgsConstructor
public class SlaController {
    private final SlaPolicyRepository repo;
    private final SlaService service;

    public record UpdateSlaRequest(@Min(1) int responseMinutes, @Min(1) int resolutionMinutes) {}

    @GetMapping
    public List<SlaPolicy> list() { return repo.findAll(); }

    @PutMapping("/{priority}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public SlaPolicy update(@PathVariable TicketPriority priority, @Valid @RequestBody UpdateSlaRequest r) {
        return service.update(priority, r.responseMinutes(), r.resolutionMinutes());
    }
}
