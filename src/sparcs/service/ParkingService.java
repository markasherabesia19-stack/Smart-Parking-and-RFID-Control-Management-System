package service;

import dao.ParkingSlotDAO;
import dao.ParkingTransactionDAO;
import model.ParkingSlot;
import model.ParkingTransaction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parking Service
 * Handles parking operations: slot management and transaction queries.
 */
public class ParkingService {

    private final ParkingTransactionDAO transactionDAO;
    private final ParkingSlotDAO        slotDAO;

    public ParkingService() {
        this.transactionDAO        = new ParkingTransactionDAO();
        this.slotDAO               = new ParkingSlotDAO();
    }

    // ── Slot queries ──────────────────────────────────────────────────────────

    /** Returns all slots with status = AVAILABLE. */
    public List<ParkingSlot> getAvailableSlots() throws SQLException {
        List<ParkingSlot> available = new ArrayList<>();
        for (ParkingSlot s : slotDAO.findAll()) {
            if (s.isAvailable()) available.add(s);
        }
        return available;
    }

    /** Returns all slots with status = OCCUPIED. */
    public List<ParkingSlot> getOccupiedSlots() throws SQLException {
        List<ParkingSlot> occupied = new ArrayList<>();
        for (ParkingSlot s : slotDAO.findAll()) {
            if (s.isOccupied()) occupied.add(s);
        }
        return occupied;
    }

    /**
     * Returns slots whose code starts with the given zone letter.
     * e.g. getSlotsByZone("B") returns B-01 … B-08.
     */
    public List<ParkingSlot> getSlotsByZone(String zoneLetter) throws SQLException {
        List<ParkingSlot> zone = new ArrayList<>();
        String prefix = zoneLetter.toUpperCase() + "-";
        for (ParkingSlot s : slotDAO.findAll()) {
            if (s.getSlotCode() != null && s.getSlotCode().startsWith(prefix)) {
                zone.add(s);
            }
        }
        return zone;
    }

    /** Builds a full ParkingStatistics snapshot from the DB. */
    public ParkingStatistics getParkingStatistics() throws SQLException {
        List<ParkingSlot> all = slotDAO.findAll();
        int total = all.size(), available = 0, occupied = 0, reserved = 0;
        for (ParkingSlot s : all) {
            switch (s.getStatus()) {
                case ParkingSlot.OCCUPIED -> occupied++;
                case ParkingSlot.RESERVED -> reserved++;
                default                   -> available++;
            }
        }
        ParkingStatistics stats = new ParkingStatistics();
        stats.setTotalSlots(total);
        stats.setAvailableSlots(available);
        stats.setOccupiedSlots(occupied);
        stats.setReservedSlots(reserved);
        if (total > 0) stats.setOccupancyRate((double) occupied / total * 100);
        return stats;
    }

    // ── Transaction queries ───────────────────────────────────────────────────

    /** Returns all in-progress transactions. */
    public List<ParkingTransaction> getActiveTransactions() throws SQLException {
        return transactionDAO.findInProgress();
    }

    /** Returns all transactions with a pending payment. */
    public List<ParkingTransaction> getPendingPayments() throws SQLException {
        return transactionDAO.findPending();
    }

    // ── Inner class ───────────────────────────────────────────────────────────

    public static class ParkingStatistics {
        private int    totalSlots;
        private int    availableSlots;
        private int    occupiedSlots;
        private int    reservedSlots;
        private double occupancyRate;

        public int    getTotalSlots()          { return totalSlots; }
        public void   setTotalSlots(int v)     { this.totalSlots = v; }

        public int    getAvailableSlots()      { return availableSlots; }
        public void   setAvailableSlots(int v) { this.availableSlots = v; }

        public int    getOccupiedSlots()       { return occupiedSlots; }
        public void   setOccupiedSlots(int v)  { this.occupiedSlots = v; }

        public int    getReservedSlots()       { return reservedSlots; }
        public void   setReservedSlots(int v)  { this.reservedSlots = v; }

        public double getOccupancyRate()       { return occupancyRate; }
        public void   setOccupancyRate(double v) { this.occupancyRate = v; }
    }
}