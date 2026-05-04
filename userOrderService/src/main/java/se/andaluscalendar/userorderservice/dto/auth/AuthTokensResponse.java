package se.andaluscalendar.userorderservice.dto.auth;

public record AuthTokensResponse(
        String accessToken,
        String refreshToken
) {
}
