package qyang.com.recommendation_service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import qyang.com.recommendation_service.dtos.CategoryRecommendationResponse;
import qyang.com.recommendation_service.dtos.RecommendationResponse;
import qyang.com.recommendation_service.exceptions.ResourceNotFoundException;
import qyang.com.recommendation_service.models.CategoryRecommendationId;
import qyang.com.recommendation_service.repositories.CategoryRecommendationRepository;
import qyang.com.recommendation_service.repositories.RecommendationRepository;

import java.util.List;

@Service
@Transactional
public class RecommendationService {
	private final RecommendationRepository recommendationRepository;
	private final CategoryRecommendationRepository categoryRecommendationRepository;
	private final UserService userService;
	private ObjectMapper objectMapper;

	public RecommendationService(RecommendationRepository recommendationRepository, CategoryRecommendationRepository categoryRecommendationRepository, UserService userService, ObjectMapper objectMapper) {
		this.recommendationRepository = recommendationRepository;
		this.categoryRecommendationRepository = categoryRecommendationRepository;
		this.userService = userService;
		this.objectMapper = objectMapper;
	}

	public RecommendationResponse getUserRecommendation(String userId) {
		if (userService.findByUserId(userId) == null) {
			throw new ResourceNotFoundException("User not found: " + userId);
		}

		return recommendationRepository.findById(userId)
				.map(rec -> {
					try {
						List<String> productList = objectMapper.readValue(rec.getProductList(), new TypeReference<List<String>>() {
						});
						return new RecommendationResponse(rec.getUserId(), productList);
					} catch (JsonProcessingException e) {
						throw new RuntimeException("Error parsing product list", e);
					}
				})
				.orElseThrow(() -> new ResourceNotFoundException("No recommendation found for user: " + userId));
	}

	public CategoryRecommendationResponse getCategoryRecommendation(String userId, String category) {
		if (userService.findByUserId(userId) == null) {
			throw new ResourceNotFoundException("User not found: " + userId);
		}
		CategoryRecommendationId categoryRecommendationId = new CategoryRecommendationId(userId, category);
		return categoryRecommendationRepository.findById(categoryRecommendationId)
				.map(rec -> {
					try {
						List<String> productList = objectMapper.readValue(rec.getProductList(), new TypeReference<List<String>>() {});
						return new CategoryRecommendationResponse(rec.getId().getUserId(), rec.getId().getCategory(), productList);
					} catch (JsonProcessingException e) {
						throw new RuntimeException("Error parsing product list", e);
					}
				})
				.orElseThrow(() -> new ResourceNotFoundException("No recommendations found for user " + userId + " in category " + category));
	}
}
