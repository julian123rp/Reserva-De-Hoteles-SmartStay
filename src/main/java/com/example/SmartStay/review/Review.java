package com.example.SmartStay.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "reviews")
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    private String id;

    @Indexed
    private String productId;

    @Indexed
    private String userId;

    private int rating;

    private String comment;

    public Review(String productId, String userId, int rating, String comment) {
        this.productId = productId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
    }
}
