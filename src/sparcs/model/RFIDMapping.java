package model;

import java.time.LocalDateTime;

/**
 * RFID Mapping Model
 */
public class RFIDMapping {
    private int rfidId;
    private String rfidTag;
    private int ownerId;
    private LocalDateTime assignedDate;
    private boolean isActive;

    public RFIDMapping() {}

    public RFIDMapping(String rfidTag, int ownerId) {
        this.rfidTag = rfidTag;
        this.ownerId = ownerId;
        this.isActive = true;
    }

    // Getters and Setters
    public int getRfidId() { return rfidId; }
    public void setRfidId(int rfidId) { this.rfidId = rfidId; }

    public String getRfidTag() { return rfidTag; }
    public void setRfidTag(String rfidTag) { this.rfidTag = rfidTag; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public LocalDateTime getAssignedDate() { return assignedDate; }
    public void setAssignedDate(LocalDateTime assignedDate) { this.assignedDate = assignedDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return "RFIDMapping{" +
                "rfidId=" + rfidId +
                ", rfidTag='" + rfidTag + '\'' +
                ", ownerId=" + ownerId +
                ", isActive=" + isActive +
                '}';
    }
}
