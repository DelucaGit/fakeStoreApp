package se.andaluscalendar.userorderservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import se.andaluscalendar.userorderservice.dto.user.UserResponse;
import se.andaluscalendar.userorderservice.dto.user.registration.UserRegistrationRequest;
import se.andaluscalendar.userorderservice.model.StoreUser;
import se.andaluscalendar.userorderservice.repository.UserRepository;
import se.andaluscalendar.userorderservice.util.JwtUtil;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private  UserRepository userRepository;
    @Mock
    private  PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

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
            when(jwtUtil.generateToken(anyString())).thenReturn("mocked.jwt.token");

            // Act
            UserResponse response = userService.registerUser(request);

            // Assert
            assertNotNull(response.id());
            assertEquals("ny@test.com", response.email());
            assertEquals("mocked.jwt.token", response.token());
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
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                userService.getUserById(fakeId);
            });

            // Assert
            assertEquals("The user wasn't found", exception.getMessage());
        }
    }

}
