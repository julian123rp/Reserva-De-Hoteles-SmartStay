package com.example.SmartStay.reservation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    public void save(Reservation reservation) {
        reservationRepository.save(reservation);
    }

    public List<Reservation> getReservationsByUserId(String userId) {
        return reservationRepository.findByUserId(userId);
    }

    public List<Reservation> getReservationsByProductId(String productId) {
        return reservationRepository.findByProductId(productId);
    }

    public List<ReservationProjection> getReservationsByProductIdProjection(String productId) {
        return reservationRepository.findByProductIdProjection(productId);
    }
}