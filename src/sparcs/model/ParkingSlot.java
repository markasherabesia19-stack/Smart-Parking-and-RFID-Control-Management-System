package model;

import java.time.LocalDateTime;

/**
 * Parking Slot Model
 */
public class ParkingSlot {
    private int slotId;
    private String slotCode;
    private String zoneName;
    private String status; // AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE
    private Integer currentVehicleId;
    private LocalDateTime entryTime;
    private LocalDateTime updatedAt;
    private String locationDetails;
    private boolean isActive;

    public enum SlotStatus {
        AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE
    }

    public ParkingSlot() {}

    public ParkingSlot(String slotCode, String zoneName) {
        this.slotCode = slotCode;
        this.zoneName = zoneName;
        this.status = SlotStatus.AVAILABLE.toString();
        this.isActive = true;
    }

    // Getters and Setters
    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }

    public String getSlotCode() { return slotCode; }
    public void setSlotCode(String slotCode) { this.slotCode = slotCode; }

    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getCurrentVehicleId() { return currentVehicleId; }
    public void setCurrentVehicleId(Integer currentVehicleId) { this.currentVehicleId = currentVehicleId; }

    public LocalDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getLocationDetails() { return locationDetails; }
    public void setLocationDetails(String locationDetails) { this.locationDetails = locationDetails; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isAvailable() {
        return SlotStatus.AVAILABLE.toString().equals(this.status);
    }

    @Override
    public String toString() {
        return "ParkingSlot{" +
                "slotId=" + slotId +
                ", slotCode='" + slotCode + '\'' +
                ", zoneName='" + zoneName + '\'' +
                ", status='" + status + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
