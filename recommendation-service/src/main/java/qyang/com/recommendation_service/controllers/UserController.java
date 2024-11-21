package qyang.com.recommendation_service.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import qyang.com.recommendation_service.dtos.ErrorResponse;
import qyang.com.recommendation_service.dtos.LoginRequest;
import qyang.com.recommendation_service.dtos.LoginResponse;
import qyang.com.recommendation_service.dtos.UserResponse;
import qyang.com.recommendation_service.exceptions.InvalidCredentialsException;
import qyang.com.recommendation_service.exceptions.UserAlreadyExistsException;
import qyang.com.recommendation_service.models.User;
import qyang.com.recommendation_service.services.UserService;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            UserResponse savedUser = userService.saveUser(user);
            return ResponseEntity.ok(savedUser);
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        log.debug("Login attempt for user: {}", loginRequest.getUsername());
        try {
           LoginResponse loginResponse = userService.login(loginRequest);
           return ResponseEntity.ok(loginResponse);
       } catch (InvalidCredentialsException e){
            log.warn("Login failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            throw e;
       } catch (Exception e) {
            log.error("Unexpected error during login for user {}", loginRequest.getUsername(), e);
            throw new RuntimeException("An error occurred during login");
        }
    }
}