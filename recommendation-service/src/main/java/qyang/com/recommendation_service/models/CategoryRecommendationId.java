package qyang.com.recommendation_service.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CategoryRecommendationId implements Serializable {
    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(length = 50)
    private String category;

    public CategoryRecommendationId() {
    }

    public CategoryRecommendationId(String userId, String category) {
        this.userId = userId;
        this.category = category;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryRecommendationId that = (CategoryRecommendationId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, category);
    }

    @Override
    public String toString() {
        return "CategoryRecommendationId{" +
                "userId='" + userId + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}
