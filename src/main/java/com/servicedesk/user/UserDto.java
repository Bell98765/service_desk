package com.servicedesk.user;

public record UserDto(Long id, String name, String email, Role role, String team) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole(),
                u.getTeam() == null ? null : u.getTeam().getName());
    }
}
