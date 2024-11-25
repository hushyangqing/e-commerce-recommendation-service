package qyang.com.recommendation_service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import qyang.com.recommendation_service.dtos.LoginRequest;
import qyang.com.recommendation_service.models.Product;
import qyang.com.recommendation_service.models.User;
import qyang.com.recommendation_service.repositories.ProductRepository;
import qyang.com.recommendation_service.repositories.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private String authToken;
	private Product testProduct;

	@BeforeEach
	public void setup() throws Exception {
		productRepository.deleteAll();
		userRepository.deleteAll();

		testProduct = new Product(
				"B00TEST123",
				"Test Product",
				99.99f,
				4.5f,
				100,
				"All_Beauty"
		);
		productRepository.save(testProduct);

		// Create user and get auth token
		User user = new User("testuser", "password123");
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		userRepository.save(user);

		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setUsername("testuser");
		loginRequest.setPassword("password123");

		String result = mockMvc.perform(post("/api/users/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
				.andReturn()
				.getResponse()
				.getContentAsString();
		authToken = JsonPath.read(result, "$.token");
	}

	@Test
	public void getProductDetails_WhenExists_ReturnsProduct() throws Exception {
		mockMvc.perform(get("/api/products/B00TEST123")
				.header("Authorization", "Bearer "+authToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.parentAsin").value("B00TEST123"))
				.andExpect(jsonPath("$.title").value("Test Product"))
				.andExpect(jsonPath("$.price").value(99.99))
				.andExpect(jsonPath("$.averageRating").value(4.5))
				.andExpect(jsonPath("$.ratingNumber").value(100))
				.andExpect(jsonPath("$.category").value("All_Beauty"));
	}

	@Test
	public void getProductDetails_WhenNotExists_ReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/products/nonexistent")
						.header("Authorization", "Bearer "+authToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Product not found with ID: nonexistent"));;
	}

	@Test
	public void getProductByCategory_WhenExists_ReturnsProduct() throws Exception {
		mockMvc.perform(get("/api/products/category/All_Beauty")
						.header("Authorization", "Bearer " + authToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].parentAsin").value("B00TEST123"));
	}

	@Test
	public void getProductByCategory_WhenNotExists_ReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/products/category/nonexistent")
						.header("Authorization", "Bearer " + authToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Product not found in category: nonexistent"));
	}

	@Test
	public void getProductDetail_WithoutAuth_ReturnsForbidden() throws Exception {
		mockMvc.perform(get("/api/products/B00TEST123"))
				.andExpect(status().isUnauthorized());
	}


}
