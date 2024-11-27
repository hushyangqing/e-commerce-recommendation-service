package qyang.com.recommendation_service.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import qyang.com.recommendation_service.dtos.ErrorResponse;
import qyang.com.recommendation_service.dtos.ProductResponse;
import qyang.com.recommendation_service.exceptions.ResourceNotFoundException;
import qyang.com.recommendation_service.services.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> getProductDetailByProductId(@PathVariable String productId) {
        try {
            ProductResponse product = productService.findByParentAsin(productId);
            if (product == null) {
                throw new ResourceNotFoundException("Product not found with ID: " + productId);
            }
            return ResponseEntity.ok(product);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("An error occurred while retrieving the product: " + productId));
        }
    }

    @GetMapping("/category/{categoryName}")
    public ResponseEntity<?> getProductsByCategory(@PathVariable String categoryName) {
        try{
            List<ProductResponse> products= productService.findByCategory(categoryName);
            if (products.isEmpty()) {
                throw new ResourceNotFoundException("Product not found in category: " + categoryName);
            }
            return ResponseEntity.ok(products);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("An error occurred while retrieving products: "+categoryName));
        }
    }

}
