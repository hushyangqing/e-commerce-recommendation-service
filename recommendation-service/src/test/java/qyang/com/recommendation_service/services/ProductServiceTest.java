package qyang.com.recommendation_service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import qyang.com.recommendation_service.dtos.ProductResponse;
import qyang.com.recommendation_service.models.Product;
import qyang.com.recommendation_service.repositories.ProductRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@Transactional
public class ProductServiceTest {

	@Autowired
	ProductService productService;

	@Autowired
	ProductRepository productRepository;

	private Product testProduct;

	@BeforeEach
	public void setUp() {
		productRepository.deleteAll();
		testProduct = new Product(
				"B00TEST123",
				"Test Product",
				99.99f,
				4.5f,
				100,
				"All_Beauty"
		);
		productRepository.save(testProduct);
	}

	@Test
	public void findByParentAsin_WhenExists_ReturnsProduct() {
		ProductResponse response= productService.findByParentAsin("B00TEST123");

		assertNotNull(response);
		assertEquals("B00TEST123", response.getParentAsin());
		assertEquals("Test Product", response.getTitle());
		assertEquals(99.99f, response.getPrice());
		assertEquals(4.5f, response.getAverageRating());
		assertEquals(100, response.getRatingNumber());
		assertEquals("All_Beauty", response.getCategory());
	}

	@Test
	public void findByParentAsin_WhenNotExists_ReturnsNull() {
		ProductResponse response= productService.findByParentAsin("notexistent");
		assertNull(response);
	}

	@Test
	public void findByCategory_WhenExists_ReturnsProducts() {
		List<ProductResponse> responses = productService.findByCategory("All_Beauty");

		assertFalse(responses.isEmpty());
		assertEquals(1, responses.size());
		assertEquals("B00TEST123", responses.get(0).getParentAsin());
	}

	@Test
	public void findByCategory_WhenNotExists_ReturnsNull() {
		List<ProductResponse> responses = productService.findByCategory("notexistent");
		assertTrue(responses.isEmpty());
	}

}
