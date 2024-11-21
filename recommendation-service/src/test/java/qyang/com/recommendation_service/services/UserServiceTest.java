package qyang.com.recommendation_service.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import qyang.com.recommendation_service.dtos.LoginRequest;
import qyang.com.recommendation_service.dtos.LoginResponse;
import qyang.com.recommendation_service.dtos.UserResponse;
import qyang.com.recommendation_service.exceptions.InvalidCredentialsException;
import qyang.com.recommendation_service.exceptions.UserAlreadyExistsException;
import qyang.com.recommendation_service.models.User;
import qyang.com.recommendation_service.repositories.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@Transactional
public class UserServiceTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    public void setUp() {
        testUser = new User();
        testUser.setUserId("test-id");
        testUser.setUsername("testuser");
        String encodedPassword = passwordEncoder.encode("password");
        testUser.setPassword(encodedPassword);
        userRepository.save(testUser);

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

    }

    @Test
    public void findByUsername_WhenUserExists_ShouldReturnUser() {
        UserResponse response = userService.findByUsername("testuser");

        assertNotNull(response);
        assertEquals(testUser.getUserId(), response.getUserId());
        assertEquals(testUser.getUsername(), response.getUsername());
    }

    @Test
    public void findByUsername_WhenUserDoesNotExist_ShouldReturnNull() {
        UserResponse response = userService.findByUsername("nonexistent");

        assertNull(response);
    }

    @Test
    public void saveUser_WhenNewUser_ShouldSucceed() {
        User newUser = new User();
        newUser.setUserId("new-id");
        newUser.setUsername("newuser");
        newUser.setPassword("password");

        UserResponse response = userService.saveUser(newUser);
        assertNotNull(response);
        assertEquals("new-id", response.getUserId());
        assertEquals("newuser", response.getUsername());

        User savedUser = userRepository.findByUsername("newuser").orElse(null);
        assertNotNull(savedUser);
        assertTrue(passwordEncoder.matches("password", savedUser.getPassword()));

    }

    @Test
    public void savedUser_whenUserExists_ShouldThrowException() {
        User duplicateUser = new User();
        duplicateUser.setUserId("duplicate-id");
        duplicateUser.setUsername("testuser");
        duplicateUser.setPassword("password");

        assertThrows(UserAlreadyExistsException.class, ()->userService.saveUser(duplicateUser));
    }

    @Test
    public void login_WhenValidCredentials_ShouldSucceed() {
        LoginResponse response = userService.login(loginRequest);

        assertNotNull(response);
        assertEquals(testUser.getUserId(), response.getUserId());
        assertEquals(testUser.getUsername(), response.getUsername());
        assertNotNull(response.getToken());
    }

    @Test
    public void login_WhenInvalidCredentials_ShouldThrowException() {
        loginRequest.setPassword("wrongpassword");

        assertThrows(InvalidCredentialsException.class, () ->
                userService.login(loginRequest)
        );
    }

}
