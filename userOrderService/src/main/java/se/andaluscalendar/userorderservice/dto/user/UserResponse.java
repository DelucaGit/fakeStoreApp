package se.andaluscalendar.userorderservice.dto.user;

import java.time.LocalDateTime;
import java.util.UUID;

// UserResponse is used both when registering a new user and when fetching data from a current user
public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        LocalDateTime createdAt
) {
}
