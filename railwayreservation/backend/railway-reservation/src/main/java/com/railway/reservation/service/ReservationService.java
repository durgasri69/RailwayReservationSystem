package com.railway.reservation.service;

import com.railway.reservation.datastructure.CancellationStack;
import com.railway.reservation.datastructure.PNRManager;
import com.railway.reservation.datastructure.SeatManager;
import com.railway.reservation.datastructure.WaitingQueueManager;
import com.railway.reservation.model.Passenger;
import com.railway.reservation.model.Reservation;
import com.railway.reservation.model.Seat;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.WaitingPassenger;
import com.railway.reservation.repository.PassengerRepository;
import com.railway.reservation.repository.ReservationRepository;
import com.railway.reservation.repository.SeatRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.repository.WaitingPassengerRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final PassengerRepository passengerRepository;
    private final TrainRepository trainRepository;
    private final SeatRepository seatRepository;
    private final WaitingPassengerRepository waitingPassengerRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final PNRManager pnrManager = new PNRManager();
    private final SeatManager seatManager = new SeatManager();
    private final WaitingQueueManager waitingQueueManager = new WaitingQueueManager();
    private final CancellationStack cancellationStack = new CancellationStack();

    public record BookingBatchResult(List<Reservation> reservations, int availableSeats) {
        public BookingBatchResult {
            reservations = List.copyOf(reservations);
        }
    }

    public ReservationService(
            ReservationRepository reservationRepository,
            PassengerRepository passengerRepository,
            TrainRepository trainRepository,
            SeatRepository seatRepository,
            WaitingPassengerRepository waitingPassengerRepository) {

        this.reservationRepository = reservationRepository;
        this.passengerRepository = passengerRepository;
        this.trainRepository = trainRepository;
        this.seatRepository = seatRepository;
        this.waitingPassengerRepository = waitingPassengerRepository;
    }

    @Transactional
    public Reservation bookTicket(Long passengerId, Long trainId) {

        Passenger passenger = passengerRepository.findById(passengerId).orElse(null);
        if (passenger == null) {
            return null;
        }

        return bookTickets(List.of(passenger), trainId, true)
                .reservations()
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public BookingBatchResult bookTickets(
            List<Passenger> passengers, Long trainId, boolean joinWaitingListWhenFull) {

        if (passengers == null || passengers.isEmpty()) {
            throw new IllegalArgumentException("At least one passenger is required.");
        }

        Train train = trainRepository.findByIdForUpdate(trainId).orElse(null);
        if (train == null) {
            throw new IllegalArgumentException("Train not found.");
        }

        syncSeats(train);
        int availableSeats = seatManager.getAvailableSeatCount(train.getTrainId());
        boolean waitingListBooking = availableSeats == 0 && joinWaitingListWhenFull;

        if (availableSeats < passengers.size() && !waitingListBooking) {
            return new BookingBatchResult(List.of(), availableSeats);
        }

        List<Reservation> reservations = new ArrayList<>();
        int waitingPosition =
                waitingPassengerRepository.countByTrainAndStatus(train, "WAITING");

        for (int i = 0; i < passengers.size(); i++) {
            Passenger passenger = passengerRepository.save(passengers.get(i));
            String pnr = generateUniquePNR();
            Integer seatNumber = null;
            String status;

            if (waitingListBooking) {
                status = "WAITING";
                int position = waitingPosition + i + 1;
                int priority = getCategoryPriority(passenger.getCategory());
                waitingPassengerRepository.save(new WaitingPassenger(
                        pnr,
                        passenger.getCategory(),
                        priority,
                        position,
                        "WAITING",
                        passenger,
                        train
                ));
                waitingQueueManager.addPassenger(pnr, priority, position);
            } else {
                status = "CONFIRMED";
                seatNumber = seatManager.bookSeat(train.getTrainId());
                if (seatNumber == -1) {
                    throw new IllegalStateException(
                            "Available seats changed while the train was locked.");
                }
                saveSeat(train, seatNumber, "OCCUPIED", pnr);
                pnrManager.addPNR(pnr, passenger.getName());
            }

            passenger.setPnr(pnr);
            passengerRepository.save(passenger);

            Reservation reservation = new Reservation(
                    pnr,
                    passenger,
                    train,
                    seatNumber,
                    LocalDateTime.now(),
                    status
            );
            reservations.add(reservationRepository.saveAndFlush(reservation));
        }

        long confirmedCount = reservationRepository.findByTrain(train).stream()
                .filter(r -> "CONFIRMED".equals(r.getStatus()))
                .count();
        train.setAvailableSeats(
                Math.max(0, train.getTotalSeats() - (int) confirmedCount)
        );
        trainRepository.save(train);

        return new BookingBatchResult(reservations, train.getAvailableSeats());
    }

    public Reservation searchPNR(String pnr) {
        return reservationRepository.findById(pnr).orElse(null);
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public List<Reservation> getPassengerReservations(Long passengerId) {
        Passenger passenger =
                passengerRepository.findById(passengerId).orElse(null);

        if (passenger == null) {
            return List.of();
        }

        return reservationRepository.findByPassenger(passenger);
    }

    @Transactional
    public boolean cancelTicket(String pnr) {

        Reservation reservation =
                reservationRepository.findById(pnr).orElse(null);

        if (reservation == null
                || (!"CONFIRMED".equals(reservation.getStatus())
                && !"WAITING".equals(reservation.getStatus()))) {
            return false;
        }

        Train train = trainRepository.findByIdForUpdate(
                reservation.getTrain().getTrainId()
        ).orElse(null);
        if (train == null) {
            return false;
        }
        entityManager.refresh(reservation);
        if (!"CONFIRMED".equals(reservation.getStatus())
                && !"WAITING".equals(reservation.getStatus())) {
            return false;
        }
        syncSeats(train);

        Integer seatNumber = reservation.getSeatNumber();
        boolean wasWaiting = "WAITING".equals(reservation.getStatus());

        reservation.setStatus("CANCELLED");
        reservationRepository.save(reservation);

        if (wasWaiting) {
            waitingPassengerRepository.findByPnr(pnr).ifPresent(waitingPassenger -> {
                waitingPassenger.setStatus("CANCELLED");
                waitingPassengerRepository.save(waitingPassenger);
            });
        }

        if (seatNumber != null) {
            seatManager.releaseSeat(train.getTrainId(), seatNumber);
            saveSeat(train, seatNumber, "AVAILABLE", null);
        }

        cancellationStack.addCancellation(pnr);

        allocateNextWaitingPassenger(train, seatNumber);

        long confirmedCount =
                reservationRepository.findByTrain(train).stream()
                        .filter(r -> "CONFIRMED".equals(r.getStatus()))
                        .count();

        train.setAvailableSeats(
                Math.max(0, train.getTotalSeats() - (int) confirmedCount)
        );
        trainRepository.save(train);

        return true;
    }

    private void allocateNextWaitingPassenger(Train train, Integer releasedSeat) {

        if (releasedSeat == null) {
            return;
        }

        List<WaitingPassenger> waiting =
                waitingPassengerRepository
                        .findByTrainAndStatusOrderByPriorityAscQueuePositionAsc(
                                train, "WAITING");

        if (waiting.isEmpty()) {
            return;
        }

        WaitingPassenger next = waiting.get(0);

        Reservation waitingReservation =
                reservationRepository.findById(next.getPnr()).orElse(null);

        if (waitingReservation == null) {
            return;
        }

        waitingReservation.setSeatNumber(releasedSeat);
        waitingReservation.setStatus("CONFIRMED");
        waitingReservation.setBookingDate(LocalDateTime.now());
        reservationRepository.save(waitingReservation);

        Passenger waitingPassengerEntity = next.getPassenger();
        waitingPassengerEntity.setPnr(next.getPnr());
        passengerRepository.save(waitingPassengerEntity);

        next.setStatus("ALLOCATED");
        waitingPassengerRepository.save(next);

        saveSeat(train, releasedSeat, "OCCUPIED", next.getPnr());

        seatManager.occupySpecificSeat(train.getTrainId(), releasedSeat);
    }

    @Transactional
    public int getAvailableSeatCount(Long trainId) {

        Train train = trainRepository.findByIdForUpdate(trainId).orElse(null);

        if (train == null) {
            return 0;
        }

        syncSeats(train);

        int available = seatManager.getAvailableSeatCount(train.getTrainId());

        if (train.getAvailableSeats() != available) {
            train.setAvailableSeats(available);
            trainRepository.save(train);
        }

        return available;
    }

    public List<Integer> getAvailableSeats(Long trainId) {
        Train train = trainRepository.findById(trainId).orElse(null);
        if (train == null) {
            return List.of();
        }

        syncSeats(train);
        return List.copyOf(seatManager.getAvailableSeats(train.getTrainId()));
    }

    public List<WaitingPassenger> getWaitingList(Long trainId) {
        Train train = trainRepository.findById(trainId).orElse(null);
        if (train == null) {
            return List.of();
        }

        return waitingPassengerRepository
                .findByTrainAndStatusOrderByPriorityAscQueuePositionAsc(train, "WAITING");
    }

    public int getWaitingCount(Long trainId) {
        Train train = trainRepository.findById(trainId).orElse(null);
        if (train == null) {
            return 0;
        }

        return waitingPassengerRepository.countByTrainAndStatus(train, "WAITING");
    }

    public String getLastCancellation() {
        return cancellationStack.getLastCancellation();
    }

    public int getCancellationCount() {
        return (int) reservationRepository.findByStatus("CANCELLED").size();
    }

    private int getCategoryPriority(String category) {
        return switch (category) {
            case "Divyang" -> 1;
            case "Senior Citizen" -> 2;
            case "Women" -> 3;
            case "General" -> 4;
            default -> 4;
        };
    }

    private void syncSeats(Train train) {

        seatManager.initializeSeats(
                train.getTrainId(),
                train.getTotalSeats()
        );

        Set<Integer> occupied = new HashSet<>();

        for (Reservation reservation : reservationRepository.findByTrain(train)) {
            if ("CONFIRMED".equals(reservation.getStatus())
                    && reservation.getSeatNumber() != null) {
                occupied.add(reservation.getSeatNumber());
            }
        }

        seatManager.syncOccupiedSeats(train.getTrainId(), occupied);

        for (int i = 1; i <= train.getTotalSeats(); i++) {
            Seat seat = seatRepository
                    .findByTrainAndSeatNumber(train, i)
                    .orElse(null);

            boolean isOccupied = occupied.contains(i);

            if (seat == null) {
                saveSeat(
                        train,
                        i,
                        isOccupied ? "OCCUPIED" : "AVAILABLE",
                        isOccupied ? findPNRBySeat(train, i) : null
                );
            } else {
                String expectedStatus = isOccupied ? "OCCUPIED" : "AVAILABLE";
                if (!expectedStatus.equals(seat.getStatus())) {
                    seat.setStatus(expectedStatus);
                    seat.setPnr(isOccupied ? findPNRBySeat(train, i) : null);
                    seatRepository.save(seat);
                }
            }
        }
    }

    private String findPNRBySeat(Train train, int seatNumber) {
        return reservationRepository.findByTrain(train).stream()
                .filter(r -> "CONFIRMED".equals(r.getStatus()))
                .filter(r -> r.getSeatNumber() != null
                        && r.getSeatNumber() == seatNumber)
                .map(Reservation::getPnr)
                .findFirst()
                .orElse(null);
    }

    private void saveSeat(
            Train train, int seatNumber, String status, String pnr) {

        Seat seat = seatRepository
                .findByTrainAndSeatNumber(train, seatNumber)
                .orElse(new Seat());

        seat.setSeatNumber(seatNumber);
        seat.setStatus(status);
        seat.setPnr(pnr);
        seat.setTrain(train);

        seatRepository.save(seat);
    }

    private synchronized String generateUniquePNR() {
        int max = 1000;

        for (Reservation r : reservationRepository.findAll()) {
            max = Math.max(max, pnrNumber(r.getPnr()));
        }

        for (WaitingPassenger w : waitingPassengerRepository.findAll()) {
            max = Math.max(max, pnrNumber(w.getPnr()));
        }

        pnrManager.setStartingNumber(max);
        return pnrManager.generatePNR();
    }

    private int pnrNumber(String pnr) {
        if (pnr == null || !pnr.matches("P\\d+")) {
            return 1000;
        }

        return Integer.parseInt(pnr.substring(1));
    }
}
