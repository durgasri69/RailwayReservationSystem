package com.railway.reservation.datastructure;

import java.util.HashMap;

public class PNRManager {

    private final HashMap<String, String> pnrMap = new HashMap<>();
    private int pnrNumber = 1000;

    public String generatePNR() {
        pnrNumber++;
        return "P" + pnrNumber;
    }

    public void setStartingNumber(int number) {
        pnrNumber = Math.max(pnrNumber, number);
    }

    public void addPNR(String pnr, String passengerName) {
        pnrMap.put(pnr, passengerName);
    }

    public String searchPNR(String pnr) {
        return pnrMap.get(pnr);
    }

    public boolean containsPNR(String pnr) {
        return pnrMap.containsKey(pnr);
    }
}
