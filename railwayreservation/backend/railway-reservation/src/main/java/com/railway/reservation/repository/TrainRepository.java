package com.railway.reservation.repository;

import com.railway.reservation.model.Train;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TrainRepository extends JpaRepository<Train, Long> {

    Train findByTrainNumber(String trainNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select train from Train train where train.trainId = :trainId")
    Optional<Train> findByIdForUpdate(@Param("trainId") Long trainId);
}