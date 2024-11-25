package qyang.com.recommendation_service.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import qyang.com.recommendation_service.dtos.ErrorResponse;
import qyang.com.recommendation_service.dtos.ProductRequest;
import qyang.com.recommendation_service.dtos.ProductResponse;
import qyang.com.recommendation_service.dtos.ProductUpdateRequest;
import qyang.com.recommendation_service.exceptions.ResourceAlreadyExistsException;
import qyang.com.recommendation_service.exceptions.ResourceNotFoundException;
import qyang.com.recommendation_service.services.AdminService;

@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {
	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	@PostMapping("/products")
	public ResponseEntity<?> createProduct(@RequestBody ProductRequest request) {
		try {
			ProductResponse response = adminService.createProduct(request);
			return ResponseEntity.ok(response);
		} catch (ResourceAlreadyExistsException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(new ErrorResponse(e.getMessage()));
		} catch (Exception e) {
			log.error("Error creating product", e);
			return ResponseEntity.internalServerError()
					.body(new ErrorResponse("Error creating product"));
		}
	}

	@PutMapping("/products/{productId}")
	public ResponseEntity<?> updateProduct(@PathVariable String productId, @RequestBody ProductUpdateRequest request) {
		try {
			ProductResponse response = adminService.updateProduct(productId, request);
			return ResponseEntity.ok(response);
		} catch (ResourceNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ErrorResponse(e.getMessage()));
		} catch (Exception e) {
			log.error("Error updating product {}", productId, e);
			return ResponseEntity.internalServerError()
					.body(new ErrorResponse("Error updating product"));
		}
	}
}
