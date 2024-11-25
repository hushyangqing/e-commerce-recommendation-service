package qyang.com.recommendation_service.dtos;

import java.util.List;

public class CategoryRecommendationResponse {
	private String userId;
	private String category;
	private List<String> productList;

	public CategoryRecommendationResponse() {
	}

	public CategoryRecommendationResponse(String userId, String category, List<String> productList) {
		this.userId = userId;
		this.category = category;
		this.productList = productList;
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

	public List<String> getProductList() {
		return productList;
	}

	public void setProductList(List<String> productList) {
		this.productList = productList;
	}
}
