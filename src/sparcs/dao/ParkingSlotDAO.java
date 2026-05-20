package dao;

import db.DatabaseConfig;
import model.ParkingSlot;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParkingSlotDAO {

    // Returns all slots ordered by slot_code.
    public List<ParkingSlot> findAll() throws SQLException {
        String sql = "SELECT * FROM parking_slot ORDER BY slot_code";
        List<ParkingSlot> slots = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt   = conn.createStatement();
             ResultSet rs     = stmt.executeQuery(sql)) {

            while (rs.next()) slots.add(map(rs));
        }
        return slots;
    }

    // Finds a slot by its display code, e.g. "B-04". Case-insensitive.
    public Optional<ParkingSlot> findBySlotCode(String slotCode) throws SQLException {
        String sql = "SELECT * FROM parking_slot WHERE UPPER(slot_code) = UPPER(?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, slotCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    // Finds a slot by its primary key.
    public Optional<ParkingSlot> findById(int slotId) throws SQLException {
        String sql = "SELECT * FROM parking_slot WHERE slot_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, slotId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Updates only the status column for a given slot.
     * @param slotId  primary key of the slot
     * @param status  "AVAILABLE" | "OCCUPIED" | "RESERVED"
     */
    public void updateSlotStatus(int slotId, String status) throws SQLException {
        String sql = "UPDATE parking_slot SET status = ? WHERE slot_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, slotId);
            stmt.executeUpdate();
        }
    }

    // Convenience: mark a slot as OCCUPIED.
    public void occupySlot(int slotId) throws SQLException {
        updateSlotStatus(slotId, ParkingSlot.OCCUPIED);
    }

    // Convenience: mark a slot as RESERVED (shown as yellow on the slot map).
    public void reserveSlot(int slotId) throws SQLException {
        updateSlotStatus(slotId, ParkingSlot.RESERVED);
    }

    // Convenience: mark a slot as AVAILABLE.
    public void vacateSlot(int slotId) throws SQLException {
        updateSlotStatus(slotId, ParkingSlot.AVAILABLE);
    }

    // Count slots by status string.
    public int countByStatus(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM parking_slot WHERE status = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }


    // Finds the slot currently occupied by the given vehicle.
    public Optional<ParkingSlot> findByVehicleId(int vehicleId) throws SQLException {
        String sql = "SELECT * FROM parking_slot WHERE current_vehicle_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    // Sets or clears the current_vehicle_id for a slot. Pass null to clear.
    public void updateCurrentVehicle(int slotId, Integer vehicleId) throws SQLException {
        String sql = "UPDATE parking_slot SET current_vehicle_id = ? WHERE slot_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (vehicleId != null) stmt.setInt(1, vehicleId);
            else stmt.setNull(1, java.sql.Types.INTEGER);
            stmt.setInt(2, slotId);
            stmt.executeUpdate();
        }
    }

        private ParkingSlot map(ResultSet rs) throws SQLException {
        return new ParkingSlot(
            rs.getInt("slot_id"),
            rs.getString("slot_code"),
            rs.getString("status"),
            rs.getString("zone")
        );
    }
}