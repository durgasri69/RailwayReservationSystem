package com.railway.reservation.model;

import jakarta.persistence.*;

@Entity
@Table(name = "waiting_list")
public class WaitingPassenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long waitingId;

    private String pnr;

    private String category;

    private int priority;

    private int queuePosition;

    private String status;

    @ManyToOne
    @JoinColumn(name = "passenger_id")
    private Passenger passenger;

    @ManyToOne
    @JoinColumn(name = "train_id")
    private Train train;

    public WaitingPassenger() {
    }

    public WaitingPassenger(String pnr, String category, int priority,
                            int queuePosition, String status,
                            Passenger passenger, Train train) {
        this.pnr = pnr;
        this.category = category;
        this.priority = priority;
        this.queuePosition = queuePosition;
        this.status = status;
        this.passenger = passenger;
        this.train = train;
    }

    public Long getWaitingId() {
        return waitingId;
    }

    public void setWaitingId(Long waitingId) {
        this.waitingId = waitingId;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(int queuePosition) {
        this.queuePosition = queuePosition;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public void setPassenger(Passenger passenger) {
        this.passenger = passenger;
    }

    public Train getTrain() {
        return train;
    }

    public void setTrain(Train train) {
        this.train = train;
    }
}