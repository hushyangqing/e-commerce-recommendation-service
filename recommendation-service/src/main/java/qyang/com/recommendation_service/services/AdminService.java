package qyang.com.recommendation_service.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import qyang.com.recommendation_service.dtos.ProductRequest;
import qyang.com.recommendation_service.dtos.ProductResponse;
import qyang.com.recommendation_service.dtos.ProductUpdateRequest;
import qyang.com.recommendation_service.exceptions.ResourceAlreadyExistsException;
import qyang.com.recommendation_service.exceptions.ResourceNotFoundException;
import qyang.com.recommendation_service.models.Product;
import qyang.com.recommendation_service.repositories.ProductRepository;

@Service
@Transactional
@Slf4j
public class AdminService {
	private final ProductRepository productRepository;

	public AdminService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public ProductResponse createProduct(ProductRequest request) {
		if (productRepository.existsById(request.getParentAsin())) {
			throw new ResourceAlreadyExistsException("Product already exists with ID: " + request.getParentAsin());
		}

		Product product = new Product(
				request.getParentAsin(),
				request.getTitle(),
				request.getPrice(),
				request.getAverageRating(),
				request.getRatingNumber(),
				request.getCategory()
		);

		Product savedProduct = productRepository.save(product);
		return new ProductResponse(
				savedProduct.getParentAsin(),
				savedProduct.getTitle(),
				savedProduct.getPrice(),
				savedProduct.getCategory(),
				savedProduct.getAverageRating(),
				savedProduct.getRatingNumber()
		);
	}

	public ProductResponse updateProduct(String productId, ProductUpdateRequest request) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

		if (request.getTitle() != null) {
			product.setTitle(request.getTitle());
		}
		if (request.getPrice() != null) {
			product.setPrice(request.getPrice());
		}
		if (request.getAverageRating() != null) {
			product.setAverageRating(request.getAverageRating());
		}
		if (request.getRatingNumber() != null) {
			product.setRatingNumber(request.getRatingNumber());
		}
		if (request.getCategory() != null) {
			product.setCategory(request.getCategory());
		}

		Product updatedProduct = productRepository.save(product);
		return new ProductResponse(
				updatedProduct.getParentAsin(),
				updatedProduct.getTitle(),
				updatedProduct.getPrice(),
				updatedProduct.getCategory(),
				updatedProduct.getAverageRating(),
				updatedProduct.getRatingNumber()
		);
	}
}
