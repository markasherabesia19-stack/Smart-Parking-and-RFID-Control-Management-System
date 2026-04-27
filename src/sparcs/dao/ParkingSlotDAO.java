package dao;

import db.DatabaseConfig;
import model.ParkingSlot;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ParkingSlotDAO {

    public void create(ParkingSlot slot) throws SQLException {
        String sql = "INSERT INTO parking_slot (slot_code, zone_name, status, location_details, is_active) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, slot.getSlotCode());
            stmt.setString(2, slot.getZoneName());
            stmt.setString(3, slot.getStatus());
            stmt.setString(4, slot.getLocationDetails());
            stmt.setBoolean(5, slot.isActive());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating parking slot failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    slot.setSlotId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public Optional<ParkingSlot> findById(int slotId) throws SQLException {
        String sql = "SELECT * FROM parking_slot WHERE slot_id = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, slotId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToParkingSlot(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<ParkingSlot> findBySlotCode(String slotCode) throws SQLException {
        String sql = "SELECT * FROM parking_slot WHERE slot_code = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, slotCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToParkingSlot(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<ParkingSlot> findAll() throws SQLException {
        String sql = "SELECT * FROM parking_slot ORDER BY zone, slot_code";
        List<ParkingSlot> slots = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                slots.add(mapResultSetToParkingSlot(rs));
            }
        }
        return slots;
    }

    public List<ParkingSlot> findByZone(String zoneName) throws SQLException {
        String sql = "SELECT * FROM parking_slot WHERE zone = ? ORDER BY slot_code";
        List<ParkingSlot> slots = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, zoneName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    slots.add(mapResultSetToParkingSlot(rs));
                }
            }
        }
        return slots;
    }

    public List<ParkingSlot> findByStatus(String status) throws SQLException {
        String sql = "SELECT * FROM parking_slot WHERE status = ? ORDER BY zone, slot_code";
        List<ParkingSlot> slots = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    slots.add(mapResultSetToParkingSlot(rs));
                }
            }
        }
        return slots;
    }

    public void update(ParkingSlot slot) throws SQLException {
        String sql = "UPDATE parking_slot SET zone_name = ?, status = ?, current_vehicle_id = ?, " +
                "entry_time = ?, location_details = ?, is_active = ? WHERE slot_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, slot.getZoneName());
            stmt.setString(2, slot.getStatus());
            stmt.setObject(3, slot.getCurrentVehicleId());

            if (slot.getEntryTime() != null) {
                stmt.setTimestamp(4, Timestamp.valueOf(slot.getEntryTime()));
            } else {
                stmt.setNull(4, Types.TIMESTAMP);
            }

            stmt.setString(5, slot.getLocationDetails());
            stmt.setBoolean(6, slot.isActive());
            stmt.setInt(7, slot.getSlotId());

            stmt.executeUpdate();
        }
    }

    public void delete(int slotId) throws SQLException {
        String sql = "UPDATE parking_slot SET is_active = FALSE WHERE slot_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, slotId);
            stmt.executeUpdate();
        }
    }

    public int countAvailableSlots() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM parking_slot WHERE status = 'AVAILABLE' AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    public int countOccupiedSlots() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM parking_slot WHERE status = 'OCCUPIED' AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    public int getTotalSlots() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM parking_slot WHERE is_active = TRUE";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    private ParkingSlot mapResultSetToParkingSlot(ResultSet rs) throws SQLException {
        ParkingSlot slot = new ParkingSlot();
        slot.setSlotId(rs.getInt("slot_id"));
        slot.setSlotCode(rs.getString("slot_code"));
        slot.setZoneName(rs.getString("zone_name"));
        slot.setStatus(rs.getString("status"));

        Integer currentVehicleId = rs.getInt("current_vehicle_id");
        if (!rs.wasNull()) {
            slot.setCurrentVehicleId(currentVehicleId);
        }

        Timestamp entryTime = rs.getTimestamp("entry_time");
        if (entryTime != null) {
            slot.setEntryTime(entryTime.toLocalDateTime());
        }

        slot.setLocationDetails(rs.getString("location_details"));
        slot.setActive(rs.getBoolean("is_active"));

        return slot;
    }
}
