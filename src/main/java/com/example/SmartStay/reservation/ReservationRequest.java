package com.example.SmartStay.reservation;

import lombok.Getter;
import org.bson.types.ObjectId;

@Getter
public class ReservationRequest {
    private ObjectId productId;
    private Long start;
    private Long end;
}