package qyang.com.recommendation_service.dtos;

public class ProductResponse {
    private String parentAsin;
    private String title;
    private Float price;
    private String category;
    private Float averageRating;
    private Integer ratingNumber;

    public ProductResponse(String parentAsin, String title, Float price, String category, Float averageRating, Integer ratingNumber) {
        this.parentAsin = parentAsin;
        this.title = title;
        this.price = price;
        this.category = category;
        this.averageRating = averageRating;
        this.ratingNumber = ratingNumber;
    }

    public String getParentAsin() {
        return parentAsin;
    }

    public String getTitle() {
        return title;
    }

    public Float getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public Float getAverageRating() {
        return averageRating;
    }

    public Integer getRatingNumber() {
        return ratingNumber;
    }
}
