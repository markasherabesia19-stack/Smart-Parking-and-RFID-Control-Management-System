package model;

import java.time.LocalDateTime;

// RFID Mapping Model
// Each RFID tag maps to a unique Vehicle, not an owner.

public class RFIDMapping {
    private int rfidId;
    private String rfidTag;
    private int vehicleId;
    private LocalDateTime assignedDate;
    private boolean isActive;

    public RFIDMapping() {}

    public RFIDMapping(String rfidTag, int vehicleId) {
        this.rfidTag = rfidTag;
        this.vehicleId = vehicleId;
        this.isActive = true;
    }

    // Getters and Setters
    public int getRfidId() { return rfidId; }
    public void setRfidId(int rfidId) { this.rfidId = rfidId; }

    public String getRfidTag() { return rfidTag; }
    public void setRfidTag(String rfidTag) { this.rfidTag = rfidTag; }

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public LocalDateTime getAssignedDate() { return assignedDate; }
    public void setAssignedDate(LocalDateTime assignedDate) { this.assignedDate = assignedDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return "RFIDMapping{" +
                "rfidId=" + rfidId +
                ", rfidTag='" + rfidTag + '\'' +
                ", vehicleId=" + vehicleId +
                ", isActive=" + isActive +
                '}';
    }
}