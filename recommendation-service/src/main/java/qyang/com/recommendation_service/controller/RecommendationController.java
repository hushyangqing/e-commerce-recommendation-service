package qyang.com.recommendation_service.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping ("/api/recommendations")
@CrossOrigin(origins="*")
public class RecommendationController {
}
