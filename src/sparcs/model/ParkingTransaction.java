package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Parking Transaction Model
 */
public class ParkingTransaction {
    private int transactionId;
    private int vehicleId;
    private int slotId;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Integer durationMinutes;
    private BigDecimal calculatedFee;
    private String paymentStatus; // PENDING, PAID, FAILED, CANCELLED
    private String transactionStatus; // IN_PROGRESS, COMPLETED, CANCELLED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum PaymentStatus {
        PENDING, PAID, FAILED, CANCELLED
    }

    public enum TransactionStatus {
        IN_PROGRESS, COMPLETED, CANCELLED, RESERVED
    }

    public ParkingTransaction() {}

    public ParkingTransaction(int vehicleId, int slotId, LocalDateTime entryTime) {
        this.vehicleId = vehicleId;
        this.slotId = slotId;
        this.entryTime = entryTime;
        this.paymentStatus = PaymentStatus.PENDING.toString();
        this.transactionStatus = TransactionStatus.IN_PROGRESS.toString();
    }

    // Getters and Setters
    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }

    public LocalDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }

    public LocalDateTime getExitTime() { return exitTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public BigDecimal getCalculatedFee() { return calculatedFee; }
    public void setCalculatedFee(BigDecimal calculatedFee) { this.calculatedFee = calculatedFee; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getTransactionStatus() { return transactionStatus; }
    public void setTransactionStatus(String transactionStatus) { this.transactionStatus = transactionStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isCompleted() {
        return TransactionStatus.COMPLETED.toString().equals(this.transactionStatus);
    }

    public boolean isReserved() {
        return TransactionStatus.RESERVED.toString().equals(this.transactionStatus);
    }

    public boolean isPaid() {
        return PaymentStatus.PAID.toString().equals(this.paymentStatus);
    }

    @Override
    public String toString() {
        return "ParkingTransaction{" +
                "transactionId=" + transactionId +
                ", vehicleId=" + vehicleId +
                ", slotId=" + slotId +
                ", calculatedFee=" + calculatedFee +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", transactionStatus='" + transactionStatus + '\'' +
                '}';
    }
}