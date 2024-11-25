package qyang.com.recommendation_service.dtos;

public class ProductRequest {
	private String parentAsin;
	private String title;
	private Float price;
	private Float averageRating;
	private Integer ratingNumber;
	private String category;

	public ProductRequest() {
	}

	public ProductRequest(String parentAsin, String title, Float price, Float averageRating, Integer ratingNumber, String category) {
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
}
