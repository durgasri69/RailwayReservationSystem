package com.railway.reservation.datastructure;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class SeatManager {

    private final ConcurrentHashMap<Long, HashMap<Integer, String>> seatMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, HashSet<Integer>> occupiedSeats =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, TreeSet<Integer>> availableSeats =
            new ConcurrentHashMap<>();

    public void initializeSeats(Long trainId, int totalSeats) {
        if (availableSeats.containsKey(trainId)) {
            return;
        }

        HashMap<Integer, String> seats = new HashMap<>();
        HashSet<Integer> occupied = new HashSet<>();
        TreeSet<Integer> available = new TreeSet<>();

        for (int i = 1; i <= totalSeats; i++) {
            seats.put(i, "AVAILABLE");
            available.add(i);
        }

        seatMap.put(trainId, seats);
        occupiedSeats.put(trainId, occupied);
        availableSeats.put(trainId, available);
    }

    public void syncOccupiedSeats(Long trainId, Collection<Integer> occupied) {
        HashSet<Integer> currentOccupied = occupiedSeats.get(trainId);
        TreeSet<Integer> currentAvailable = availableSeats.get(trainId);
        HashMap<Integer, String> currentSeats = seatMap.get(trainId);

        if (currentOccupied == null || currentAvailable == null || currentSeats == null) {
            return;
        }

        currentOccupied.clear();
        currentAvailable.clear();

        for (Integer seatNumber : currentSeats.keySet()) {
            if (occupied.contains(seatNumber)) {
                currentOccupied.add(seatNumber);
                currentSeats.put(seatNumber, "OCCUPIED");
            } else {
                currentAvailable.add(seatNumber);
                currentSeats.put(seatNumber, "AVAILABLE");
            }
        }
    }

    public int bookSeat(Long trainId) {
        TreeSet<Integer> available = availableSeats.get(trainId);

        if (available == null || available.isEmpty()) {
            return -1;
        }

        int seatNumber = available.first();
        available.remove(seatNumber);
        occupiedSeats.get(trainId).add(seatNumber);
        seatMap.get(trainId).put(seatNumber, "OCCUPIED");

        return seatNumber;
    }

    public void occupySpecificSeat(Long trainId, int seatNumber) {
        TreeSet<Integer> available = availableSeats.get(trainId);
        HashSet<Integer> occupied = occupiedSeats.get(trainId);
        HashMap<Integer, String> seats = seatMap.get(trainId);

        if (available == null || occupied == null || seats == null) {
            return;
        }

        if (available.remove(seatNumber)) {
            occupied.add(seatNumber);
            seats.put(seatNumber, "OCCUPIED");
        }
    }

    public void releaseSeat(Long trainId, int seatNumber) {
        if (!occupiedSeats.containsKey(trainId)) {
            return;
        }

        if (occupiedSeats.get(trainId).remove(seatNumber)) {
            availableSeats.get(trainId).add(seatNumber);
            seatMap.get(trainId).put(seatNumber, "AVAILABLE");
        }
    }

    public int getAvailableSeatCount(Long trainId) {
        return availableSeats.containsKey(trainId)
                ? availableSeats.get(trainId).size() : 0;
    }

    public TreeSet<Integer> getAvailableSeats(Long trainId) {
        return availableSeats.getOrDefault(trainId, new TreeSet<>());
    }
}
