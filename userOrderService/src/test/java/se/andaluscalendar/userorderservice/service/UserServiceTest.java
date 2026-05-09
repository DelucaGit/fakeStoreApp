package se.andaluscalendar.userorderservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import se.andaluscalendar.userorderservice.dto.auth.AuthTokensResponse;
import se.andaluscalendar.userorderservice.dto.user.UserResponse;
import se.andaluscalendar.userorderservice.dto.user.login.UserLoginRequest;
import se.andaluscalendar.userorderservice.dto.user.registration.UserRegistrationRequest;
import se.andaluscalendar.userorderservice.exception.UnauthorizedException;
import se.andaluscalendar.userorderservice.exception.UserNotFoundException;
import se.andaluscalendar.userorderservice.model.StoreUser;
import se.andaluscalendar.userorderservice.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthTokenService authTokenService;

    @InjectMocks
    private UserService userService;

    @Nested
    class UserRegistration{

        @Test
        @DisplayName("Test/ Register user if email is not saved")
        void whenRegisterUser_andEmailIsFree_ThenSaveUser(){
            // Arrange
            UserRegistrationRequest request = new UserRegistrationRequest(
                    "ny@test.com",
                    "password123",
                    "myName",
                    "myLastName"
            );

            StoreUser savedUser = new StoreUser();
            savedUser.setId(UUID.randomUUID());
            savedUser.setEmail("ny@test.com");
            savedUser.setRole("USER");
            savedUser.setCreatedAt(LocalDateTime.now());

            when(userRepository.findByEmail(anyString())).
                    thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
            when(userRepository.save(any(StoreUser.class))).thenReturn(savedUser);
            when(authTokenService.issueTokensForUser(any(StoreUser.class)))
                    .thenReturn(new AuthTokensResponse("mocked.access.token", "mocked.refresh.token"));

            // Act
            UserResponse response = userService.registerUser(request);

            // Assert
            assertNotNull(response.id());
            assertEquals("ny@test.com", response.email());
            assertEquals("mocked.access.token", response.accessToken());
            assertEquals("mocked.refresh.token", response.refreshToken());
        }

        @Test
        @DisplayName("Test/ Throw Exception if email exists")
        void whenEmailAlreadyExists_shouldThrowException(){

            // Arrange
            UserRegistrationRequest request = new UserRegistrationRequest(
                    "already@exists.com",
                    "password",
                    "firstName1",
                    "lastName2"
            );

            when(userRepository.findByEmail("already@exists.com")).thenReturn(Optional.of(new StoreUser()));

            // Act
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                userService.registerUser(request);
            });

            // Assert
            assertEquals("This email is already registered", exception.getMessage());

        }


    }

    @Nested
    class UserLogin {
        @Test
        @DisplayName("Test/ Login user if credentials are valid")
        void whenLoginUser_andCredentialsAreValid_ThenReturnTokens() {
            UserLoginRequest request = new UserLoginRequest("login@test.com", "password123");
            StoreUser existingUser = new StoreUser();
            existingUser.setId(UUID.randomUUID());
            existingUser.setEmail("login@test.com");
            existingUser.setPasswordHash("hashed_password");
            existingUser.setRole("USER");
            existingUser.setCreatedAt(LocalDateTime.now());

            when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches(request.password(), existingUser.getPasswordHash())).thenReturn(true);
            when(authTokenService.issueTokensForUser(existingUser))
                    .thenReturn(new AuthTokensResponse("new.access.token", "new.refresh.token"));

            UserResponse response = userService.loginUser(request);

            assertEquals(existingUser.getId(), response.id());
            assertEquals("new.access.token", response.accessToken());
            assertEquals("new.refresh.token", response.refreshToken());
        }

        @Test
        @DisplayName("Test/ Throw exception when password is invalid")
        void whenLoginUser_andPasswordIsInvalid_ThenThrowUnauthorized() {
            UserLoginRequest request = new UserLoginRequest("login@test.com", "wrong-password");
            StoreUser existingUser = new StoreUser();
            existingUser.setPasswordHash("hashed_password");

            when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches(request.password(), existingUser.getPasswordHash())).thenReturn(false);

            UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> userService.loginUser(request));
            assertEquals("Invalid email or password", exception.getMessage());
        }
    }

    @Nested
    class UserFetching{
        @Test
        @DisplayName("Test/ Get user if ID exists")
        void whenGettingUser_andIdIsFound_ThenFetch(){
            // Arrange
            StoreUser existingUser = new StoreUser();
            existingUser.setId(UUID.randomUUID());

            when(userRepository.findById((existingUser.getId()))).thenReturn(Optional.of(existingUser));

            // Act
            UserResponse savedUser = userService.getUserById(existingUser.getId());

            // Assert
            assertEquals(savedUser.id(), existingUser.getId());

        }

        @Test
        @DisplayName("Test/ Throw exception if ID doesn't exist")
        void whenGettingUser_andIdIsNotFound_ThenThrowException(){
            // Arrange
            UUID fakeId = UUID.randomUUID();
            when(userRepository.findById(fakeId)).thenReturn(Optional.empty());

            // Act
            UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> userService.getUserById(fakeId));

            // Assert
            assertEquals("The user with the provided ID wasn't found", exception.getMessage());
        }
    }

}
