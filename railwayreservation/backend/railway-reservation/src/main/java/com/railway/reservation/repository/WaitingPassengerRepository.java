package com.railway.reservation.repository;

import com.railway.reservation.model.Train;
import com.railway.reservation.model.WaitingPassenger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WaitingPassengerRepository
        extends JpaRepository<WaitingPassenger, Long> {

    List<WaitingPassenger> findByTrainAndStatusOrderByPriorityAscQueuePositionAsc(
            Train train, String status);

    Optional<WaitingPassenger> findByPnr(String pnr);

    int countByTrainAndStatus(Train train, String status);
}
