package com.example.SmartStay.reservation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ReservationRepository extends MongoRepository<Reservation, String> {
    List<Reservation> findByUserId(String userId);
    List<Reservation> findByProductId(String productId);

    @Query(value = "{'productId': ?0}", fields = "{'productId': 1, 'start': 1, 'end': 1}")
    List<ReservationProjection> findByProductIdProjection(String id);
}