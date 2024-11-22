package qyang.com.recommendation_service.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import qyang.com.recommendation_service.services.ProductService;

@RestController
@RequestMapping("/api/products")
@Slf4j
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> getProductDetailByProductId(@PathVariable String productId) {
    }

    @GetMapping("/category/{categoryName}")
    public ResponseEntity<?> getProductsByCategory(@PathVariable String categoryName) {

    }

}
