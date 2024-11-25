package qyang.com.recommendation_service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import qyang.com.recommendation_service.dtos.LoginRequest;
import qyang.com.recommendation_service.models.Profile;
import qyang.com.recommendation_service.models.User;
import qyang.com.recommendation_service.repositories.ProfileRepository;
import qyang.com.recommendation_service.repositories.UserRepository;

import java.net.PasswordAuthentication;

import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
    }

    @Test
    public void register_WithValidUser_ShouldSucceed() throws Exception {
        User newUser = new User("newuser", "password123");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.userId", hasLength(13)))
                .andExpect(jsonPath("$.username").value("newuser"));

        assertTrue(userRepository.findByUsername("newuser").isPresent());
    }

    @Test
    void register_WithExistingUsername_ShouldReturnBadRequest() throws Exception {
        User existingUser = new User("testuser", "password123");
        userRepository.save(existingUser);

        User duplicateUser = new User("testuser", "differentpassword");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username testuser is already taken"));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnToken() throws Exception {
        User user = new User("testuser", "password123");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        User user = new User("testuser", "password123");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_WithNonexistentUser_ShouldReturnUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistent");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    private void profileSetup() {
        User user = new User("testuser", "password123");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        User managedUser = entityManager.find(User.class, user.getUserId());
        Profile profile = new Profile();
        profile.setUserId(managedUser.getUserId());
        profile.setUser(managedUser);
        profile.setEmail("test@example.com");
        profile.setFirstname("Test");
        profile.setLastname("User");
        profile.setPhone("1234567890");
        entityManager.persist(profile);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void getProfile_WhenProfileExists_ShouldReturnProfile() throws Exception {
        profileSetup();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        String result = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(result).get("token").asText();

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.phone").value("1234567890"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getProfile_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        this.mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getProfile_WithExpiredToken_ShouldReturnUnauthorized() throws Exception {
        profileSetup();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        String result = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(result).get("token").asText();

        Thread.sleep(3500);

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("JWT token has expired"));
    }

    @Test
    void getProfile_WhenProfileNotFound_ShouldReturnNotFound() throws Exception {
        User user = new User("testuser", "password123");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        String result = this.mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(result).get("token").asText();

        this.mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }



    @AfterEach
    public void tearDown() {
        userRepository.deleteAll();
        profileRepository.deleteAll();
        entityManager.clear();
    }

}
