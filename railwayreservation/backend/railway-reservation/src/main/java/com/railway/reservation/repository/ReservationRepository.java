package com.railway.reservation.repository;

import com.railway.reservation.model.Reservation;
import com.railway.reservation.model.Passenger;
import com.railway.reservation.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, String> {

    List<Reservation> findByPassenger(Passenger passenger);

    List<Reservation> findByTrain(Train train);

    List<Reservation> findByStatus(String status);
}