package com.railway.reservation.repository;

import com.railway.reservation.model.Seat;
import com.railway.reservation.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByTrain(Train train);

    Optional<Seat> findByTrainAndSeatNumber(Train train, int seatNumber);
}