package qyang.com.recommendation_service.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import qyang.com.recommendation_service.dtos.LoginRequest;
import qyang.com.recommendation_service.dtos.LoginResponse;
import qyang.com.recommendation_service.dtos.UserResponse;
import qyang.com.recommendation_service.exceptions.InvalidCredentialsException;
import qyang.com.recommendation_service.exceptions.UserAlreadyExistsException;
import qyang.com.recommendation_service.models.User;
import qyang.com.recommendation_service.repositories.UserRepository;
import qyang.com.recommendation_service.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public UserResponse findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> new UserResponse(user.getUserId(), user.getUsername()))
                .orElse(null);
    }

    public UserResponse saveUser(User user) {
        log.debug("Attempting to register user: {}", user.getUsername());
        // already exists
        if (userRepository.existsById(user.getUserId())) {
            log.warn("Registration failed - username already exists: {}", user.getUsername());
            throw new UserAlreadyExistsException(
                    "User with ID " + user.getUserId() + " already exists"
            );
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            log.warn("Registration failed - username already exists: {}", user.getUsername());
            throw new UserAlreadyExistsException(
                    "Username " + user.getUsername() + " is already taken"
            );
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        return new UserResponse(savedUser.getUserId(), savedUser.getUsername());
    }

    public LoginResponse login(LoginRequest request) {
        log.debug("Attempting to authenticate user: {}", request.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String jwt = jwtUtil.generateToken(userDetails);

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            log.info("Successfully authenticated user: {}", user.getUsername());

            return new LoginResponse(
                    jwt,
                    user.getUserId(),
                    user.getUsername()
            );
        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", request.getUsername(), e);
            throw new InvalidCredentialsException("Invalid username or password");
        }

    }
}
