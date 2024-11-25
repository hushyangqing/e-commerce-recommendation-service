package qyang.com.recommendation_service.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
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
import qyang.com.recommendation_service.dtos.ProfileResponse;
import qyang.com.recommendation_service.dtos.UserResponse;
import qyang.com.recommendation_service.exceptions.InvalidCredentialsException;
import qyang.com.recommendation_service.exceptions.ResourceNotFoundException;
import qyang.com.recommendation_service.exceptions.UserAlreadyExistsException;
import qyang.com.recommendation_service.models.Profile;
import qyang.com.recommendation_service.models.User;
import qyang.com.recommendation_service.repositories.ProfileRepository;
import qyang.com.recommendation_service.repositories.UserRepository;

import java.time.LocalDateTime;

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
    private ProfileRepository profileRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Profile testProfile;
    private LoginRequest loginRequest;

    @BeforeEach
    public void setUp() {
        testUser = new User();
        testUser.setUserId("test-id");
        testUser.setUsername("testuser");
        String encodedPassword = passwordEncoder.encode("password");
        testUser.setPassword(encodedPassword);
        userRepository.save(testUser);

        testUser = userRepository.saveAndFlush(testUser);
        entityManager.clear();

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        entityManager.flush();
        entityManager.clear();

    }

    @Test
    public void findByUserId_WhenUserExists_shouldReturnUser() {
        UserResponse response = userService.findByUserId("test-id");

        assertNotNull(response);
        assertEquals(testUser.getUserId(), response.getUserId());
        assertEquals(testUser.getUsername(), response.getUsername());
    }

    @Test
    public void findByUserId_WhenUserDoesNotExist_shouldReturnNull() {
        UserResponse response = userService.findByUserId("nonexistent");

        assertNull(response);
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

    private void setupTestProfile() {
        User managedUser = entityManager.find(User.class, testUser.getUserId());

        testProfile = new Profile();
        testProfile.setUserId(testUser.getUserId());
        testProfile.setEmail("yangqinghush@gmail.com");
        testProfile.setFirstname("Test");
        testProfile.setLastname("User");
        testProfile.setPhone("1234567890");
        testProfile.setUser(managedUser);
        entityManager.persist(testProfile);  // Use persist for initial creation
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void getProfile_whenUserExists_shouldReturnProfileResponse() {
        setupTestProfile();

        ProfileResponse response = userService.getProfile(testUser.getUsername());

        assertNotNull(response);
        assertEquals(testUser.getUserId(), response.getUserId());
        assertEquals(testUser.getUsername(), response.getUsername());
        assertEquals(testProfile.getEmail(), response.getEmail());
        assertEquals(testProfile.getFirstname(), response.getFirstName());
        assertEquals(testProfile.getLastname(), response.getLastName());
        assertEquals(testProfile.getPhone(), response.getPhone());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    public void getProfile_WhenUserDoesNotExist_ThrowsResourceNotFoundException() {
        setupTestProfile();

        assertThrows(ResourceNotFoundException.class, () ->
                userService.getProfile("nonexistentuser")
        );
    }

    @Test
    public void getProfile_WhenUserExistsButProfileDoesNot_ThrowsResourceNotFoundException() {
        setupTestProfile();

        User newUser = new User();
        newUser.setUserId("new-id");
        newUser.setUsername("newuser");
        newUser.setPassword("password");
        userRepository.save(newUser);
        entityManager.flush();
        entityManager.clear();

        assertThrows(ResourceNotFoundException.class, () ->
                userService.getProfile("newuser")
        );
    }

    @Test
    public void getProfile_ProfileDataUpdated_ReturnsUpdatedProfile() {
        setupTestProfile();

        testProfile.setEmail("updated@example.com");
        testProfile.setPhone("9876543210");
        profileRepository.save(testProfile);
        entityManager.flush();
        entityManager.clear();

        ProfileResponse response = userService.getProfile(testUser.getUsername());

        assertEquals("updated@example.com", response.getEmail());
        assertEquals("9876543210", response.getPhone());
    }

    @Test public void getProfile_CheckTimestamps_HasCorrectTimestamps() {
        setupTestProfile();

        ProfileResponse response = userService.getProfile(testUser.getUsername());

        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
        assertTrue(response.getCreatedAt().isBefore(LocalDateTime.now()) ||
                response.getCreatedAt().isEqual(LocalDateTime.now()));
        assertTrue(response.getUpdatedAt().isBefore(LocalDateTime.now()) ||
                response.getUpdatedAt().isEqual(LocalDateTime.now()));
    }

    @AfterEach
    public void tearDown() {
        userRepository.deleteAll();
        profileRepository.deleteAll();
        entityManager.clear();
    }

}
