package qyang.com.recommendation_service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
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
import qyang.com.recommendation_service.models.CategoryRecommendation;
import qyang.com.recommendation_service.models.CategoryRecommendationId;
import qyang.com.recommendation_service.models.Recommendation;
import qyang.com.recommendation_service.models.User;
import qyang.com.recommendation_service.repositories.CategoryRecommendationRepository;
import qyang.com.recommendation_service.repositories.RecommendationRepository;
import qyang.com.recommendation_service.repositories.UserRepository;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class RecommendationControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RecommendationRepository recommendationRepository;

	@Autowired
	private CategoryRecommendationRepository categoryRecommendationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private String authToken;
	private User testUser;
	private Recommendation testRecommendation;
	private CategoryRecommendation testCategoryRecommendation;

	@BeforeEach
	public void setUp() throws Exception {
		recommendationRepository.deleteAll();
		categoryRecommendationRepository.deleteAll();
		userRepository.deleteAll();

		testUser = new User("testuser", "password123");
		testUser.setPassword(passwordEncoder.encode(testUser.getPassword()));
		userRepository.save(testUser);

		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setUsername("testuser");
		loginRequest.setPassword("password123");

		String result = mockMvc.perform(post("/api/users/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(loginRequest)))
				.andReturn()
				.getResponse()
				.getContentAsString();
		authToken = JsonPath.read(result, "$.token");

		List<String> productList = Arrays.asList("B001TEST", "B002TEST");
		String jsonProductList = objectMapper.writeValueAsString(productList);

		testRecommendation = new Recommendation(testUser.getUserId(), jsonProductList);
		recommendationRepository.save(testRecommendation);

		CategoryRecommendationId categoryId = new CategoryRecommendationId(testUser.getUserId(), "All_Beauty");
		testCategoryRecommendation = new CategoryRecommendation(categoryId, jsonProductList);
		categoryRecommendationRepository.save(testCategoryRecommendation);
	}

	@Test
	public void getUserRecommendations_WhenExists_ReturnRecommendations() throws Exception {
		mockMvc.perform(get("/api/recommendations/" + testUser.getUserId())
				.header("Authorization", "Bearer "+authToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(testUser.getUserId()))
				.andExpect(jsonPath("$.productList").isArray())
				.andExpect(jsonPath("$.productList", hasSize(2)))
				.andExpect(jsonPath("$.productList", hasItem("B001TEST")))
				.andExpect(jsonPath("$.productList", hasItem("B002TEST")));
	}

	@Test
	public void getUserRecommendations_WhenNotExists_ReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/recommendations/nonexistent")
						.header("Authorization", "Bearer " + authToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("User not found: nonexistent"));
	}

	@Test
	public void getUserRecommendations_WithoutAuth_ReturnsUnauthroized() throws Exception {
		mockMvc.perform(get("/api/recommendations/" + testUser.getUserId()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void getCategoryRecommendations_WhenExists_ReturnRecommendations() throws Exception {
		mockMvc.perform(get("/api/recommendations/" + testUser.getUserId() + "/All_Beauty")
						.header("Authorization", "Bearer " + authToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(testUser.getUserId()))
				.andExpect(jsonPath("$.category").value("All_Beauty"))
				.andExpect(jsonPath("$.productList").isArray())
				.andExpect(jsonPath("$.productList", hasSize(2)))
				.andExpect(jsonPath("$.productList", hasItem("B001TEST")))
				.andExpect(jsonPath("$.productList", hasItem("B002TEST")));
	}

	@Test
	public void getCategoryRecommendations_WhenUserNotExists_ReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/recommendations/nonexistent/All_Beauty")
				.header("Authorization", "Bearer " + authToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("User not found: nonexistent"));
	}

	@Test
	public void getCategoryRecommendations_WhenCategoryNotExists_ReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/recommendations/"+testUser.getUserId()+"/nonexistent")
						.header("Authorization", "Bearer " + authToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("No recommendations found for user "+testUser.getUserId()+" in category nonexistent"));
	}

	@Test
	public void getCategoryRecommendations_WithoutAuth_ReturnsUnauthroized() throws Exception {
		mockMvc.perform(get("/api/recommendations/" + testUser.getUserId() + "/All_Beauty"))
				.andExpect(status().isUnauthorized());
	}

}
