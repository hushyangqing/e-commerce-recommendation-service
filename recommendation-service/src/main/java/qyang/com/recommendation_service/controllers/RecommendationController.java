package qyang.com.recommendation_service.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import qyang.com.recommendation_service.dtos.CategoryRecommendationResponse;
import qyang.com.recommendation_service.dtos.ErrorResponse;
import qyang.com.recommendation_service.dtos.RecommendationResponse;
import qyang.com.recommendation_service.exceptions.ResourceNotFoundException;
import qyang.com.recommendation_service.services.RecommendationService;

@RestController
@RequestMapping ("/api/recommendations")
public class RecommendationController {
	private final RecommendationService recommendationService;

	public RecommendationController(RecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@GetMapping("/{userId}")
	public ResponseEntity<?> getUserRecommendations(@PathVariable String userId) {
		try {
			RecommendationResponse recommendations = recommendationService.getUserRecommendation(userId);
			return ResponseEntity.ok(recommendations);
		} catch (ResourceNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ErrorResponse(e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(new ErrorResponse("Error retrieving recommendations"));
		}
	}

	@GetMapping("/{userId}/{category}")
	public ResponseEntity<?> getCategoryRecommendations(@PathVariable String userId, @PathVariable String category) {
		try {
			CategoryRecommendationResponse recommendations = recommendationService.getCategoryRecommendation(userId, category);
			return ResponseEntity.ok(recommendations);
		} catch (ResourceNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ErrorResponse(e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(new ErrorResponse("Error retrieving category recommendations"));
		}
	}
}
