package se.andaluscalendar.userorderservice.dto.auth;

public record TokenRefreshRequest(
        String refreshToken
) {
}
