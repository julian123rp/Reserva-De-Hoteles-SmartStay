package com.example.SmartStay.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "reservations")
@AllArgsConstructor
@NoArgsConstructor
public class Reservation {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String productId;

    private Long start;

    private Long end;

    public Reservation(String userId, String productId, Long start, Long end) {
        this.userId = userId;
        this.productId = productId;
        this.start = start;
        this.end = end;
    }
}