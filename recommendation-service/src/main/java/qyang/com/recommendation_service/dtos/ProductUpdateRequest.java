package qyang.com.recommendation_service.dtos;

public class ProductUpdateRequest {
	private String title;
	private Float price;
	private Float averageRating;
	private Integer ratingNumber;
	private String category;

	public ProductUpdateRequest() {
	}

	public ProductUpdateRequest(String title, Float price, Float averageRating, Integer ratingNumber, String category) {
		this.title = title;
		this.price = price;
		this.averageRating = averageRating;
		this.ratingNumber = ratingNumber;
		this.category = category;
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
}
