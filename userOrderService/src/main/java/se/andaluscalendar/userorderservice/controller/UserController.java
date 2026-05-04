package se.andaluscalendar.userorderservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.andaluscalendar.userorderservice.dto.auth.AuthTokensResponse;
import se.andaluscalendar.userorderservice.dto.auth.TokenRefreshRequest;
import se.andaluscalendar.userorderservice.dto.user.UserResponse;
import se.andaluscalendar.userorderservice.dto.user.login.UserLoginRequest;
import se.andaluscalendar.userorderservice.dto.user.registration.UserRegistrationRequest;
import se.andaluscalendar.userorderservice.service.AuthTokenService;
import se.andaluscalendar.userorderservice.service.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {


    private final UserService userService;
    private final AuthTokenService authTokenService;

    public UserController(UserService userService, AuthTokenService authTokenService) {
        this.userService = userService;
        this.authTokenService = authTokenService;
    }

     @GetMapping("/{id}")
     public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id){
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
     }


    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@RequestBody UserRegistrationRequest request){
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> loginUser(@RequestBody UserLoginRequest request){
        UserResponse response = userService.loginUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokensResponse> refreshToken(@RequestBody TokenRefreshRequest request){
        AuthTokensResponse response = authTokenService.refreshTokens(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorizationHeader){
        authTokenService.logout(authorizationHeader);
        return ResponseEntity.noContent().build();
    }
}
