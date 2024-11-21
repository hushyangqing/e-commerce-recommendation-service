package qyang.com.recommendation_service.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import qyang.com.recommendation_service.dtos.LoginRequest;
import qyang.com.recommendation_service.dtos.UserResponse;
import qyang.com.recommendation_service.exceptions.UserAlreadyExistsException;
import qyang.com.recommendation_service.models.User;
import qyang.com.recommendation_service.repositories.UserRepository;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public UserResponse findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> new UserResponse(user.getUserId(), user.getUsername()))
                .orElse(null);
    }

    public UserResponse saveUser(User user) {
        // already exists
        if (userRepository.existsById(user.getUserId())) {
            throw new UserAlreadyExistsException(
                    "User with ID " + user.getUserId() + " already exists"
            );
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException(
                    "Username " + user.getUsername() + " is already taken"
            );
        }

        User savedUser = userRepository.save(user);
        return new UserResponse(savedUser.getUserId(), savedUser.getUsername());
    }

    public Optional<UserResponse> login(LoginRequest loginRequest) {
        return userRepository.findByUsernameAndPassword(
                loginRequest.getUsername(),
                loginRequest.getPassword()
        ).map(user->new UserResponse(user.getUserId(), user.getUsername()));
    }
}
