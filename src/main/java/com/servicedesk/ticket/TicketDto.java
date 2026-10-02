package com.servicedesk.ticket;

import com.servicedesk.user.UserDto;
import java.time.Instant;

public record TicketDto(Long id, String title, String description, TicketStatus status, TicketPriority priority,
                        UserDto requester, UserDto assignedAgent, String team, Instant createdAt,
                        Instant responseDeadline, Instant slaDeadline, boolean escalated, Long version) {
    public static TicketDto from(Ticket t) {
        return new TicketDto(t.getId(), t.getTitle(), t.getDescription(), t.getStatus(), t.getPriority(),
                UserDto.from(t.getRequester()),
                t.getAssignedAgent() == null ? null : UserDto.from(t.getAssignedAgent()),
                t.getTeam() == null ? null : t.getTeam().getName(),
                t.getCreatedAt(), t.getResponseDeadline(), t.getSlaDeadline(), t.isEscalated(), t.getVersion());
    }
}
