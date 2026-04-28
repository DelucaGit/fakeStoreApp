package se.andaluscalendar.userorderservice.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.UUID;

// UserResponse is used both when registering a new user and when fetching data from a current user
/* JsonInclude makes sure that NULL variables are not sent in the response. This is because this
* DTO is used both when registering and fetching an user. So the JWT token will sometimes be filled and sometimes
* NULL.
* */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        LocalDateTime createdAt,
        String token
) {
}
