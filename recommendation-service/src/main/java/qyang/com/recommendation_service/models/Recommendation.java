package qyang.com.recommendation_service.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name="recommendations")
public class Recommendation {
    @Id
    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "product_list", columnDefinition = "json")
    private String productList; // JSON string

    public Recommendation() {
    }

    public Recommendation(String userId, String productList) {
        this.userId = userId;
        this.productList = productList;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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
        Recommendation that = (Recommendation) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "Recommendation{" +
                "userId='" + userId + '\'' +
                ", productList='" + productList + '\'' +
                '}';
    }
}
