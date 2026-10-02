package com.servicedesk.user;

import com.servicedesk.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Transactional
    public User createUser(String name, String email, String rawPassword, Role role, Team team) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmail(normalized)) throw ApiException.conflict("Email already registered");
        User u = new User();
        u.setName(name);
        u.setEmail(normalized);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole(role);
        u.setTeam(team);
        return users.save(u);
    }

    @Transactional(readOnly = true)
    public User authenticate(String email, String rawPassword) {
        User u = users.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));
        if (!encoder.matches(rawPassword, u.getPasswordHash())) throw ApiException.unauthorized("Invalid credentials");
        return u;
    }
}
