package qyang.com.recommendation_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-jmeter-test.properties")
public class IntegrationTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	public void contextLoads() {
		// Integration test to verify the Spring context loads for JMeter.
	}

	@Test
	public void verifyDatabaseInitialization() {
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
		assertEquals(2, count, "User table should have 2 entries");
	}
}