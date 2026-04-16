package model;

import java.time.LocalDateTime;

/**
 * Vehicle Model
 */
public class Vehicle {
    private int vehicleId;
    private int ownerId;
    private String plateNumber;
    private Integer rfidTagId;
    private String vehicleType; // SEDAN, SUV, TRUCK, MOTORCYCLE, OTHER
    private String model;
    private String color;
    private LocalDateTime registrationDate;
    private boolean isActive;

    public enum VehicleType {
        SEDAN, SUV, TRUCK, MOTORCYCLE, OTHER
    }

    public Vehicle() {}

    public Vehicle(int ownerId, String plateNumber, String vehicleType) {
        this.ownerId = ownerId;
        this.plateNumber = plateNumber;
        this.vehicleType = vehicleType;
        this.isActive = true;
    }

    // Getters and Setters
    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public Integer getRfidTagId() { return rfidTagId; }
    public void setRfidTagId(Integer rfidTagId) { this.rfidTagId = rfidTagId; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return "Vehicle{" +
                "vehicleId=" + vehicleId +
                ", plateNumber='" + plateNumber + '\'' +
                ", vehicleType='" + vehicleType + '\'' +
                ", model='" + model + '\'' +
                ", color='" + color + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
