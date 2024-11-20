package qyang.com.recommendation_service.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name="products")
public class Product {
    @Id
    @Column(name = "parent_asin", length = 50)
    private String parentAsin;

    @Column(columnDefinition = "TEXT")
    private String title;

    private Float price;

    @Column(name = "average_rating")
    private Float averageRating;

    @Column(name = "rating_number")
    private Integer ratingNumber;

    @Column(length = 50)
    private String category;

    public Product() {
    }

    public Product(String parentAsin, String title, Float price, Float averageRating, Integer ratingNumber, String category) {
        this.parentAsin = parentAsin;
        this.title = title;
        this.price = price;
        this.averageRating = averageRating;
        this.ratingNumber = ratingNumber;
        this.category = category;
    }

    public String getParentAsin() {
        return parentAsin;
    }

    public void setParentAsin(String parentAsin) {
        this.parentAsin = parentAsin;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public Float getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Float averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getRatingNumber() {
        return ratingNumber;
    }

    public void setRatingNumber(Integer ratingNumber) {
        this.ratingNumber = ratingNumber;
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
        Product product = (Product) o;
        return Objects.equals(parentAsin, product.parentAsin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentAsin);
    }

    @Override
    public String toString() {
        return "Product{" +
                "parentAsin='" + parentAsin + '\'' +
                ", title='" + title + '\'' +
                ", price=" + price +
                ", averageRating=" + averageRating +
                ", ratingNumber=" + ratingNumber +
                ", category='" + category + '\'' +
                '}';
    }
}
