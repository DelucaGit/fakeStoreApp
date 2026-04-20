package se.andaluscalendar.userorderservice.service;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import se.andaluscalendar.userorderservice.dto.user.UserResponse;
import se.andaluscalendar.userorderservice.dto.user.registration.UserRegistrationRequest;
import se.andaluscalendar.userorderservice.model.StoreUser;
import se.andaluscalendar.userorderservice.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        return new UserResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getRole(),
                savedUser.getCreatedAt()
        );
    }
}
