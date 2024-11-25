package qyang.com.recommendation_service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import qyang.com.recommendation_service.dtos.CategoryRecommendationResponse;
import qyang.com.recommendation_service.dtos.RecommendationResponse;
import qyang.com.recommendation_service.exceptions.ResourceNotFoundException;
import qyang.com.recommendation_service.models.CategoryRecommendation;
import qyang.com.recommendation_service.models.CategoryRecommendationId;
import qyang.com.recommendation_service.models.Recommendation;
import qyang.com.recommendation_service.models.User;
import qyang.com.recommendation_service.repositories.CategoryRecommendationRepository;
import qyang.com.recommendation_service.repositories.RecommendationRepository;
import qyang.com.recommendation_service.repositories.UserRepository;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@Transactional
public class RecommendationServiceTest {
	@Autowired
	private RecommendationService recommendationService;

	@Autowired
	private RecommendationRepository recommendationRepository;

	@Autowired
	private CategoryRecommendationRepository categoryRecommendationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ObjectMapper objectMapper;

	private User testUser;
	private Recommendation testRecommendation;
	private CategoryRecommendation testCategoryRecommendation;

	@BeforeEach
	public void setUp() {
		recommendationRepository.deleteAll();
		categoryRecommendationRepository.deleteAll();
		userRepository.deleteAll();

		testUser = new User("testuser", "password123");
		testUser = userRepository.save(testUser);

		List<String> productList = Arrays.asList("B001TEST", "B002TEST");
		try {
			String jsonProductList = objectMapper.writeValueAsString(productList);
			testRecommendation = new Recommendation(testUser.getUserId(), jsonProductList);
			recommendationRepository.save(testRecommendation);

			CategoryRecommendationId categoryId = new CategoryRecommendationId(testUser.getUserId(), "All_Beauty");
			testCategoryRecommendation = new CategoryRecommendation(categoryId, jsonProductList);
			categoryRecommendationRepository.save(testCategoryRecommendation);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error creating test data", e);
		}
	}

	@Test
	public void getUserRecommendations_WhenExists_ReturnsRecommendations() {
		RecommendationResponse response = recommendationService.getUserRecommendation(testUser.getUserId());

		assertNotNull(response);
		assertEquals(testUser.getUserId(), response.getUserId());
		assertEquals(2, response.getProductList().size());
		assertTrue(response.getProductList().contains("B001TEST"));
		assertTrue(response.getProductList().contains("B002TEST"));
	}

	@Test
	public void getUserRecommendations_WhenNotExists_ThrowsException() {
		assertThrows(ResourceNotFoundException.class, ()->
				recommendationService.getUserRecommendation("nonexistent"));
	}

	@Test
	public void getCategoryRecommendations_WhenExists_ReturnsRecommendations() {
		CategoryRecommendationResponse response = recommendationService.getCategoryRecommendation(testUser.getUserId(), "All_Beauty");

		assertNotNull(response);
		assertEquals(testUser.getUserId(), response.getUserId());
		assertEquals("All_Beauty", response.getCategory());
		assertEquals(2, response.getProductList().size());
		assertTrue(response.getProductList().contains("B001TEST"));
		assertTrue(response.getProductList().contains("B002TEST"));
	}

	@Test
	public void getCategoryRecommendations_WhenUserNotExists_ThrowsException() {
		assertThrows(ResourceNotFoundException.class, ()->
				recommendationService.getCategoryRecommendation("nonexistent", "All_Beauty"));
	}

	@Test
	public void getCategoryRecommendations_WhenCategoryNotExists_ThrowsException() {
		assertThrows(ResourceNotFoundException.class, ()->
				recommendationService.getCategoryRecommendation(testUser.getUserId(), "nonexistent"));
	}
}
