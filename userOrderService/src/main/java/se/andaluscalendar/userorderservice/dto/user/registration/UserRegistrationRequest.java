package se.andaluscalendar.userorderservice.dto.user.registration;

public record UserRegistrationRequest(
        String email,
        String password,
        String firstName,
        String lastName
) {
}
