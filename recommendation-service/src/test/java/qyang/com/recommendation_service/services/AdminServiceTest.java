package qyang.com.recommendation_service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import qyang.com.recommendation_service.dtos.ProductRequest;
import qyang.com.recommendation_service.dtos.ProductResponse;
import qyang.com.recommendation_service.dtos.ProductUpdateRequest;
import qyang.com.recommendation_service.exceptions.ResourceAlreadyExistsException;
import qyang.com.recommendation_service.exceptions.ResourceNotFoundException;
import qyang.com.recommendation_service.models.Product;
import qyang.com.recommendation_service.repositories.ProductRepository;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@Transactional
public class AdminServiceTest {
	@Autowired
	private AdminService adminService;

	@Autowired
	private ProductRepository productRepository;

	private ProductRequest productRequest;
	private Product testProduct;

	@BeforeEach
	public void setUp() {
		productRepository.deleteAll();

		productRequest = new ProductRequest();
		productRequest.setParentAsin("B00TEST123");
		productRequest.setTitle("Test Product");
		productRequest.setPrice(99.99f);
		productRequest.setAverageRating(4.5f);
		productRequest.setRatingNumber(100);
		productRequest.setCategory("All_Beauty");

		testProduct = new Product(
				"B00EXISTING",
				"Existing Product",
				149.99f,
				4.8f,
				200,
				"All_Beauty"
		);
		testProduct = productRepository.save(testProduct);
	}

	@Test
	public void createProduct_WhenNewProduct_ShouldSucceed() {
		ProductResponse response = adminService.createProduct(productRequest);

		assertNotNull(response);
		assertEquals(productRequest.getParentAsin(), response.getParentAsin());
		assertEquals(productRequest.getTitle(), response.getTitle());
		assertEquals(productRequest.getPrice(), response.getPrice());
		assertEquals(productRequest.getAverageRating(), response.getAverageRating());
		assertEquals(productRequest.getRatingNumber(), response.getRatingNumber());
		assertEquals(productRequest.getCategory(), response.getCategory());

		assertTrue(productRepository.existsById(response.getParentAsin()));
	}

	@Test
	public void createProduct_WhenProductExists_ShouldThrowException() {
		productRequest.setParentAsin("B00EXISTING");

		assertThrows(ResourceAlreadyExistsException.class, ()->
				adminService.createProduct(productRequest));
	}

	@Test
	public void updateProduct_WhenExists_ShouldUpdate() {
		ProductUpdateRequest updateRequest = new ProductUpdateRequest();
		updateRequest.setTitle("Updated Title");
		updateRequest.setPrice(159.99f);

		ProductResponse response = adminService.updateProduct("B00EXISTING", updateRequest);

		assertNotNull(response);
		assertEquals("B00EXISTING", response.getParentAsin());
		assertEquals("Updated Title", response.getTitle());
		assertEquals(159.99f, response.getPrice());
		assertEquals(testProduct.getAverageRating(), response.getAverageRating());
		assertEquals(testProduct.getRatingNumber(), response.getRatingNumber());
		assertEquals(testProduct.getCategory(), response.getCategory());
	}

	@Test
	void updateProduct_WhenNotExists_ShouldThrowException() {
		ProductUpdateRequest updateRequest = new ProductUpdateRequest();
		updateRequest.setTitle("Updated Title");
		updateRequest.setPrice(159.99f);

		assertThrows(ResourceNotFoundException.class, ()->
				adminService.updateProduct("nonexistent", updateRequest));
	}
}
