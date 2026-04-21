package se.andaluscalendar.userorderservice.service;

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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private  UserRepository userRepository;
    @Mock
    private  PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Nested
    class UserRegistration{

        @Test
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

            // Act
            UserResponse response = userService.registerUser(request);

            // Assert
            assertNotNull(response.id());
            assertEquals("ny@test.com", response.email());
        }
    }

}
