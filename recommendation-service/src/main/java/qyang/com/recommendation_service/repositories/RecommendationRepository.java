package qyang.com.recommendation_service.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import qyang.com.recommendation_service.models.Recommendation;

import java.util.Optional;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, String> {
}
