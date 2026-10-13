package com.railway.reservation.datastructure;

import java.util.Stack;

public class CancellationStack {

    private final Stack<String> cancellationStack = new Stack<>();

    public void addCancellation(String pnr) {
        cancellationStack.push(pnr);
    }

    public String getLastCancellation() {
        return cancellationStack.isEmpty() ? null : cancellationStack.peek();
    }

    public boolean isEmpty() {
        return cancellationStack.isEmpty();
    }

    public int getCancellationCount() {
        return cancellationStack.size();
    }
}
