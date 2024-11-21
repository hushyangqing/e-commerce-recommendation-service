package qyang.com.recommendation_service.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import qyang.com.recommendation_service.models.CategoryRecommendation;
import qyang.com.recommendation_service.models.CategoryRecommendationId;

import java.util.List;

@Repository
public interface CategoryRecommendationRepository extends JpaRepository<CategoryRecommendation, CategoryRecommendationId> {
    List<CategoryRecommendation> findByIdUserId(String userId);
}
