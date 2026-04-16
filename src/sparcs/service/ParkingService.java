package service;

import dao.*;
import model.*;
import exception.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Parking Service
 * Handles parking operations: vehicle entry/exit, slot management
 */
public class ParkingService {
    private ParkingTransactionDAO transactionDAO;
    private ParkingSlotDAO slotDAO;
    private VehicleDAO vehicleDAO;
    private VehicleOwnerDAO ownerDAO;
    private RFIDMappingDAO rfidDAO;
    private FeeCalculationService feeCalculationService;
    private AuditLogDAO auditLogDAO;

    public ParkingService() {
        this.transactionDAO = new ParkingTransactionDAO();
        this.slotDAO = new ParkingSlotDAO();
        this.vehicleDAO = new VehicleDAO();
        this.ownerDAO = new VehicleOwnerDAO();
        this.rfidDAO = new RFIDMappingDAO();
        this.feeCalculationService = new FeeCalculationService();
        this.auditLogDAO = new AuditLogDAO();
    }

    /**
     * Get available parking slots
     */
    public List<ParkingSlot> getAvailableSlots() throws SQLException {
        return slotDAO.findByStatus("AVAILABLE");
    }

    /**
     * Get occupied slots
     */
    public List<ParkingSlot> getOccupiedSlots() throws SQLException {
        return slotDAO.findByStatus("OCCUPIED");
    }

    /**
     * Get parking statistics
     */
    public ParkingStatistics getParkingStatistics() throws SQLException {
        ParkingStatistics stats = new ParkingStatistics();
        stats.setTotalSlots(slotDAO.getTotalSlots());
        stats.setAvailableSlots(slotDAO.countAvailableSlots());
        stats.setOccupiedSlots(slotDAO.countOccupiedSlots());
        stats.setReservedSlots(stats.getTotalSlots() - stats.getAvailableSlots() - stats.getOccupiedSlots());
        if (stats.getTotalSlots() > 0) {
            stats.setOccupancyRate((double) stats.getOccupiedSlots() / stats.getTotalSlots() * 100);
        }
        return stats;
    }

    /**
     * Get slots by zone
     */
    public List<ParkingSlot> getSlotsByZone(String zoneName) throws SQLException {
        return slotDAO.findByZone(zoneName);
    }

    /**
     * Get active transactions
     */
    public List<ParkingTransaction> getActiveTransactions() throws SQLException {
        return transactionDAO.findInProgress();
    }

    /**
     * Get pending payments
     */
    public List<ParkingTransaction> getPendingPayments() throws SQLException {
        return transactionDAO.findPending();
    }

    /**
     * Parking Statistics Inner Class
     */
    public static class ParkingStatistics {
        private int totalSlots;
        private int availableSlots;
        private int occupiedSlots;
        private int reservedSlots;
        private double occupancyRate;

        public int getTotalSlots() { return totalSlots; }
        public void setTotalSlots(int totalSlots) { this.totalSlots = totalSlots; }

        public int getAvailableSlots() { return availableSlots; }
        public void setAvailableSlots(int availableSlots) { this.availableSlots = availableSlots; }

        public int getOccupiedSlots() { return occupiedSlots; }
        public void setOccupiedSlots(int occupiedSlots) { this.occupiedSlots = occupiedSlots; }

        public int getReservedSlots() { return reservedSlots; }
        public void setReservedSlots(int reservedSlots) { this.reservedSlots = reservedSlots; }

        public double getOccupancyRate() { return occupancyRate; }
        public void setOccupancyRate(double occupancyRate) { this.occupancyRate = occupancyRate; }
    }
}
