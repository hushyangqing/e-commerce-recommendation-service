package qyang.com.recommendation_service.services;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Service;
import qyang.com.recommendation_service.dtos.ProductResponse;
import qyang.com.recommendation_service.repositories.ProductRepository;
import qyang.com.recommendation_service.security.JwtUtil;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse findByParentAsin(String asin) {
        return productRepository.findById(asin)
                .map(product->new ProductResponse(product.getParentAsin(), product.getTitle(), product.getPrice(), product.getCategory(), product.getAverageRating(), product.getRatingNumber()))
                .orElse(null);
    }

    public List<ProductResponse> findByCategory(String category) {
        return productRepository.findByCategory(category).stream()
                .map(product->new ProductResponse(product.getParentAsin(), product.getTitle(), product.getPrice(), product.getCategory(), product.getAverageRating(), product.getRatingNumber()))
                .collect(Collectors.toList());
    }
}
