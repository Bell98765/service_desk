package com.servicedesk.user;

import com.servicedesk.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserRepository users;
    private final TeamRepository teams;

    public record CreateUserRequest(@NotBlank String name, @NotBlank @Email String email,
                                    @NotBlank @Size(min = 8) String password, @NotNull Role role, Long teamId) {}
    public record CreateTeamRequest(@NotBlank String name) {}

    @GetMapping("/users/me")
    public UserDto me(@AuthenticationPrincipal User user) { return UserDto.from(user); }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public List<UserDto> list() { return users.findAll().stream().map(UserDto::from).toList(); }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest r) {
        Team team = r.teamId() == null ? null
                : teams.findById(r.teamId()).orElseThrow(() -> ApiException.badRequest("Unknown team"));
        User u = userService.createUser(r.name(), r.email(), r.password(), r.role(), team);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.from(u));
    }

    @GetMapping("/teams")
    public List<Team> teams() { return teams.findAll(); }

    @PostMapping("/teams")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Team> createTeam(@Valid @RequestBody CreateTeamRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teams.save(new Team(r.name())));
    }
}
