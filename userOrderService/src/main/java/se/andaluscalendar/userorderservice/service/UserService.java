package se.andaluscalendar.userorderservice.service;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import se.andaluscalendar.userorderservice.dto.auth.AuthTokensResponse;
import se.andaluscalendar.userorderservice.dto.user.UserResponse;
import se.andaluscalendar.userorderservice.dto.user.login.UserLoginRequest;
import se.andaluscalendar.userorderservice.dto.user.registration.UserRegistrationRequest;
import se.andaluscalendar.userorderservice.exception.UnauthorizedException;
import se.andaluscalendar.userorderservice.exception.UserNotFoundException;
import se.andaluscalendar.userorderservice.model.StoreUser;
import se.andaluscalendar.userorderservice.repository.UserRepository;

import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthTokenService authTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
    }

    public UserResponse registerUser(UserRegistrationRequest request){
        if(userRepository.findByEmail(request.email()).isPresent()){
            throw new IllegalArgumentException("This email is already registered");
        }

        StoreUser newUser = new StoreUser();
        newUser.setEmail(request.email());
        newUser.setPasswordHash(passwordEncoder.encode(request.password()));
        newUser.setFirstName(request.firstName());
        newUser.setLastName(request.lastName());
        newUser.setRole("USER");

        // JPA sends back the same user but this time it has an ID and createdAt
        StoreUser savedUser = userRepository.save(newUser);
        return mapUserWithFreshTokens(savedUser);
    }

    public UserResponse loginUser(UserLoginRequest request) {
        StoreUser user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return mapUserWithFreshTokens(user);
    }

    public UserResponse getUserById(UUID id){
        return userRepository.findById(id)
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getRole(),
                        user.getCreatedAt(),
                        null,
                        null
                )).orElseThrow(() -> new UserNotFoundException("The user with the provided ID wasn't found"));
    }

    private UserResponse mapUserWithFreshTokens(StoreUser user) {
        AuthTokensResponse authTokens = authTokenService.issueTokensForUser(user);
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getCreatedAt(),
                authTokens.accessToken(),
                authTokens.refreshToken()
        );
    }
}
