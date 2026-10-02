package com.servicedesk.common;

import com.servicedesk.sla.SlaPolicy;
import com.servicedesk.sla.SlaPolicyRepository;
import com.servicedesk.ticket.TicketPriority;
import com.servicedesk.user.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Seeds teams, SLA policies and a bootstrap admin so the API is usable on first run. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final TeamRepository teams;
    private final SlaPolicyRepository policies;
    private final UserService users;
    private final UserRepository userRepo;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(TeamRepository teams, SlaPolicyRepository policies, UserService users, UserRepository userRepo,
                      @Value("${app.admin.email}") String adminEmail,
                      @Value("${app.admin.password}") String adminPassword) {
        this.teams = teams; this.policies = policies; this.users = users; this.userRepo = userRepo;
        this.adminEmail = adminEmail; this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (teams.count() == 0) {
            teams.save(new Team("Service Desk"));
            teams.save(new Team("Network"));
        }
        if (policies.count() == 0) {
            policies.save(new SlaPolicy(TicketPriority.LOW, 1440, 7200));
            policies.save(new SlaPolicy(TicketPriority.MEDIUM, 480, 2880));
            policies.save(new SlaPolicy(TicketPriority.HIGH, 240, 1440));
            policies.save(new SlaPolicy(TicketPriority.CRITICAL, 60, 240));
        }
        if (!userRepo.existsByEmail(adminEmail)) {
            users.createUser("Admin", adminEmail, adminPassword, Role.ADMIN, null);
        }
    }
}
