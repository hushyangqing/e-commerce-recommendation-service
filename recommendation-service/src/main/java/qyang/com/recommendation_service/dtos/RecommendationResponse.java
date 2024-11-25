package qyang.com.recommendation_service.dtos;

import java.util.List;

public class RecommendationResponse {
	private String userId;
	private List<String> productList;

	public RecommendationResponse() {
	}

	public RecommendationResponse(String userId, List<String> productList) {
		this.userId = userId;
		this.productList = productList;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public List<String> getProductList() {
		return productList;
	}

	public void setProductList(List<String> productList) {
		this.productList = productList;
	}
}
