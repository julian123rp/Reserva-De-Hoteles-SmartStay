package com.example.SmartStay.review;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    public void save(Review review) {
        reviewRepository.save(review);
    }

    public Review getReviewById(String reviewId) {
        return reviewRepository.findById(reviewId).orElse(null);
    }

    public List<Review> getReviewsByUserId(String userId) {
        return reviewRepository.findByUserId(userId);
    }

    public List<Review> getReviewsByProductId(String productId) {
        return reviewRepository.findByProductId(productId);
    }

    public List<ReviewProjection> getReviewsByProductIdProjection(String productId) {
        return reviewRepository.findByProductIdProjection(productId);
    }

    public List<ReviewProjection> getReviewsByUserIdProjection(String userId) {
        return reviewRepository.findByUserIdProjection(userId);
    }
}