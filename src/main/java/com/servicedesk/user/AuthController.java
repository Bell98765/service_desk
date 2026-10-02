package com.servicedesk.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService users;
    private final JwtService jwt;

    public record RegisterRequest(@NotBlank String name, @NotBlank @Email String email,
                                  @NotBlank @Size(min = 8) String password) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String token, UserDto user) {}

    /** Self-registration always creates a REQUESTER. Elevated roles are created by an ADMIN. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest r) {
        User u = users.createUser(r.name(), r.email(), r.password(), Role.REQUESTER, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(jwt.generate(u), UserDto.from(u)));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        User u = users.authenticate(r.email(), r.password());
        return new AuthResponse(jwt.generate(u), UserDto.from(u));
    }
}
