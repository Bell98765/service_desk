package com.servicedesk.sla;

import com.servicedesk.ticket.TicketPriority;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {
    Optional<SlaPolicy> findByPriority(TicketPriority priority);
}
