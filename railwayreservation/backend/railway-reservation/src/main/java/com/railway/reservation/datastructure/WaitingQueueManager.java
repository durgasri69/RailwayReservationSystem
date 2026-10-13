package com.railway.reservation.datastructure;

import java.util.Comparator;
import java.util.PriorityQueue;

public class WaitingQueueManager {

    private static class WaitingEntry {
        String pnr;
        int priority;
        int queuePosition;

        WaitingEntry(String pnr, int priority, int queuePosition) {
            this.pnr = pnr;
            this.priority = priority;
            this.queuePosition = queuePosition;
        }
    }

    private final PriorityQueue<WaitingEntry> waitingQueue =
            new PriorityQueue<>(Comparator
                    .comparingInt((WaitingEntry e) -> e.priority)
                    .thenComparingInt(e -> e.queuePosition));

    public void addPassenger(String pnr, int priority, int queuePosition) {
        waitingQueue.add(new WaitingEntry(pnr, priority, queuePosition));
    }

    public String getNextPassenger() {
        return waitingQueue.isEmpty() ? null : waitingQueue.poll().pnr;
    }

    public boolean isEmpty() {
        return waitingQueue.isEmpty();
    }

    public int getWaitingCount() {
        return waitingQueue.size();
    }
}
