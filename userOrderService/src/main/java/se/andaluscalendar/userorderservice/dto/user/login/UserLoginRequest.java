package se.andaluscalendar.userorderservice.dto.user.login;

public record UserLoginRequest(
        String email,
        String password
) {
}
