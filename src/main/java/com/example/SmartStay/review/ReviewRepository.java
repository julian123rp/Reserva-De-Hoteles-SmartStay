package com.example.SmartStay.review;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByProductId(String productId);
    List<Review> findByUserId(String userId);

    @Query(value = "{'productId': ?0}", fields = "{'rating': 1}")
    List<ReviewProjection> findByProductIdProjection(String productId);

    @Query(value = "{'userId': ?0}", fields = "{'rating': 1}")
    List<ReviewProjection> findByUserIdProjection(String userId);
}
