package qyang.com.recommendation_service.models;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name="category_recommendations")
public class CategoryRecommendation {
    @EmbeddedId
    private CategoryRecommendationId id;

    @Column(name = "product_list", columnDefinition = "json")
    private String productList; // JSON string

    public CategoryRecommendation() {
    }

    public CategoryRecommendation(CategoryRecommendationId id, String productList) {
        this.id = id;
        this.productList = productList;
    }

    public CategoryRecommendationId getId() {
        return id;
    }

    public void setId(CategoryRecommendationId id) {
        this.id = id;
    }

    public String getProductList() {
        return productList;
    }

    public void setProductList(String productList) {
        this.productList = productList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryRecommendation that = (CategoryRecommendation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CategoryRecommendation{" +
                "id=" + id +
                ", productList='" + productList + '\'' +
                '}';
    }
}
