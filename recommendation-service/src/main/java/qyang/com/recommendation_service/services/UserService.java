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
import qyang.com.recommendation_service.dtos.ProfileResponse;
import qyang.com.recommendation_service.dtos.UserResponse;
import qyang.com.recommendation_service.exceptions.InvalidCredentialsException;
import qyang.com.recommendation_service.exceptions.ResourceNotFoundException;
import qyang.com.recommendation_service.exceptions.UserAlreadyExistsException;
import qyang.com.recommendation_service.models.Profile;
import qyang.com.recommendation_service.models.User;
import qyang.com.recommendation_service.repositories.ProfileRepository;
import qyang.com.recommendation_service.repositories.UserRepository;
import qyang.com.recommendation_service.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Service
@Transactional
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository, ProfileRepository profileRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public UserResponse findByUserId(String userId) {
        return userRepository.findById(userId)
                .map(user -> new UserResponse(user.getUserId(), user.getUsername()))
                .orElse(null);
    }

    public UserResponse findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> new UserResponse(user.getUserId(), user.getUsername()))
                .orElse(null);
    }

    public UserResponse saveUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException(
                    "Username " + user.getUsername() + " is already taken"
            );
        }

        if (user.getUserId() != null) {
            // If userId is provided, check if it exists
            if (userRepository.existsById(user.getUserId())) {
                throw new UserAlreadyExistsException(
                        "User with ID " + user.getUserId() + " already exists"
                );
            }
        } else {
            // If no userId, generate one using UUID
            user.setUserId("USER-" + UUID.randomUUID().toString().substring(0, 8));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        return new UserResponse(savedUser.getUserId(), savedUser.getUsername());
    }

    public LoginResponse login(LoginRequest request) {
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


            return new LoginResponse(
                    jwt,
                    user.getUserId(),
                    user.getUsername()
            );
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    public ProfileResponse getProfile(String username) {
        return userRepository.findByUsername(username)
                .map(user -> profileRepository.findById(user.getUserId())
                        .map(this::convertToProfileResponse)
                        .orElseThrow(() -> new ResourceNotFoundException("Profile not found")))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ProfileResponse convertToProfileResponse(Profile profile) {
        ProfileResponse response = new ProfileResponse();
        response.setUserId(profile.getUserId());
        response.setUsername(profile.getUser().getUsername());
        response.setEmail(profile.getEmail());
        response.setFirstName(profile.getFirstname());
        response.setLastName(profile.getLastname());
        response.setPhone(profile.getPhone());
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());
        return response;
    }
}
