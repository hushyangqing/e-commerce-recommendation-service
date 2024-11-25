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
import qyang.com.recommendation_service.dtos.ProductRequest;
import qyang.com.recommendation_service.dtos.ProductUpdateRequest;
import qyang.com.recommendation_service.models.Product;
import qyang.com.recommendation_service.models.User;
import qyang.com.recommendation_service.repositories.ProductRepository;
import qyang.com.recommendation_service.repositories.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminControllerTest {

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

	private String adminToken;
	private String userToken;
	private Product testProduct;

	@BeforeEach
	void setUp() throws Exception {
		productRepository.deleteAll();
		userRepository.deleteAll();

		User adminUser = new User("admin", "password123");
		adminUser.setPassword(passwordEncoder.encode(adminUser.getPassword()));
		adminUser.setRole("ROLE_ADMIN");
		userRepository.save(adminUser);

		User regularUser = new User("user", "password123");
		regularUser.setPassword(passwordEncoder.encode(regularUser.getPassword()));
		regularUser.setRole("ROLE_USER");
		userRepository.save(regularUser);

		LoginRequest adminLogin = new LoginRequest();
		adminLogin.setUsername("admin");
		adminLogin.setPassword("password123");

		String adminResult = mockMvc.perform(post("/api/users/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(adminLogin)))
				.andReturn()
				.getResponse()
				.getContentAsString();

		adminToken = JsonPath.read(adminResult, "$.token");

		LoginRequest userLogin = new LoginRequest();
		userLogin.setUsername("user");
		userLogin.setPassword("password123");

		String userResult = mockMvc.perform(post("/api/users/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(userLogin)))
				.andReturn()
				.getResponse()
				.getContentAsString();

		userToken = JsonPath.read(userResult, "$.token");

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
	public void createProduct_WithValidData_ShouldSucceed() throws Exception {
		ProductRequest request = new ProductRequest();
		request.setParentAsin("B00TEST123");
		request.setTitle("Test Product");
		request.setPrice(99.99f);
		request.setAverageRating(4.5f);
		request.setRatingNumber(100);
		request.setCategory("All_Beauty");

		mockMvc.perform(post("/api/admin/products")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.parentAsin").value("B00TEST123"))
				.andExpect(jsonPath("$.title").value("Test Product"))
				.andExpect(jsonPath("$.price").value(99.99))
				.andExpect(jsonPath("$.averageRating").value(4.5))
				.andExpect(jsonPath("$.ratingNumber").value(100))
				.andExpect(jsonPath("$.category").value("All_Beauty"));
	}

	@Test
	void createProduct_WithExistingId_ShouldReturnConflict() throws Exception {
		ProductRequest request = new ProductRequest();
		request.setParentAsin("B00EXISTING");
		request.setTitle("New Product");

		mockMvc.perform(post("/api/admin/products")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Product already exists with ID: B00EXISTING"));
	}

	@Test
	void createProduct_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
		ProductRequest request = new ProductRequest();
		request.setParentAsin("B00TEST123");
		request.setTitle("Test Product");

		mockMvc.perform(post("/api/admin/products")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isForbidden());
	}

	@Test
	void updateProduct_WithValidData_ShouldSucceed() throws Exception {
		ProductUpdateRequest request = new ProductUpdateRequest();
		request.setTitle("Updated Product");
		request.setPrice(159.99f);

		mockMvc.perform(put("/api/admin/products/B00EXISTING")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.parentAsin").value("B00EXISTING"))
				.andExpect(jsonPath("$.title").value("Updated Product"))
				.andExpect(jsonPath("$.price").value(159.99));
	}

	@Test
	void updateProduct_WhenNotFound_ShouldReturnNotFound() throws Exception {
		ProductUpdateRequest request = new ProductUpdateRequest();
		request.setTitle("Updated Product");

		mockMvc.perform(put("/api/admin/products/nonexistent")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Product not found: nonexistent"));
	}

	@Test
	void updateProduct_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
		ProductUpdateRequest request = new ProductUpdateRequest();
		request.setTitle("Updated Product");

		mockMvc.perform(put("/api/admin/products/B00EXISTING")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isForbidden());
	}

}
