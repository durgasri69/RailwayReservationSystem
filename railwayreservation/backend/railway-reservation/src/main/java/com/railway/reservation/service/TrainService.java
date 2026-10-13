package com.railway.reservation.service;

import com.railway.reservation.model.Train;
import com.railway.reservation.repository.TrainRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainService {

    private final TrainRepository trainRepository;

    public TrainService(TrainRepository trainRepository) {
        this.trainRepository = trainRepository;
    }

    public Train addTrain(Train train) {
        train.setAvailableSeats(train.getTotalSeats());
        return trainRepository.save(train);
    }

    public List<Train> getAllTrains() {
        return trainRepository.findAll();
    }

    public Train getTrainById(Long id) {
        return trainRepository.findById(id).orElse(null);
    }

    public Train getTrainByNumber(String trainNumber) {
        return trainRepository.findByTrainNumber(trainNumber);
    }

    public Train updateTrain(Long id, Train updatedTrain) {

        Train train = trainRepository.findById(id).orElse(null);

        if (train == null) {
            return null;
        }

        train.setTrainNumber(updatedTrain.getTrainNumber());
        train.setTrainName(updatedTrain.getTrainName());
        train.setSource(updatedTrain.getSource());
        train.setDestination(updatedTrain.getDestination());
        train.setTotalSeats(updatedTrain.getTotalSeats());
        train.setAvailableSeats(updatedTrain.getAvailableSeats());

        return trainRepository.save(train);
    }

    public boolean deleteTrain(Long id) {

        if (!trainRepository.existsById(id)) {
            return false;
        }

        trainRepository.deleteById(id);
        return true;
    }
}