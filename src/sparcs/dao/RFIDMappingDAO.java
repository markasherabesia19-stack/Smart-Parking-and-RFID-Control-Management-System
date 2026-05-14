package dao;

import db.DatabaseConfig;
import model.RFIDMapping;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RFID Mapping Data Access Object
 * Each RFID tag maps to a unique Vehicle (vehicle_id), not an owner.
 */
public class RFIDMappingDAO {

    public void create(RFIDMapping mapping) throws SQLException {
        String sql = "INSERT INTO rfid_mapping (rfid_tag_number, vehicle_id, status) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, mapping.getRfidTag());
            stmt.setInt(2, mapping.getVehicleId());
            stmt.setString(3, mapping.isActive() ? "Active" : "Inactive");

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating RFID mapping failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    mapping.setRfidId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public Optional<RFIDMapping> findById(int rfidId) throws SQLException {
        String sql = "SELECT * FROM rfid_mapping WHERE rfid_id = ? AND status = 'Active'";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, rfidId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<RFIDMapping> findByRFIDTag(String rfidTag) throws SQLException {
        String sql = "SELECT * FROM rfid_mapping WHERE rfid_tag_number = ? AND status = 'Active'";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, rfidTag);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /** Returns the RFID mapping for a specific vehicle (one-to-one). */
    public Optional<RFIDMapping> findByVehicleId(int vehicleId) throws SQLException {
        String sql = "SELECT * FROM rfid_mapping WHERE vehicle_id = ? AND status = 'Active'";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /** Returns ALL RFID mappings for a vehicle regardless of status. Use for deletion. */
    public List<RFIDMapping> findAllByVehicleId(int vehicleId) throws SQLException {
        String sql = "SELECT * FROM rfid_mapping WHERE vehicle_id = ?";
        List<RFIDMapping> mappings = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    mappings.add(mapRow(rs));
                }
            }
        }
        return mappings;
    }

    /** Returns all RFID mappings for vehicles belonging to an owner (via JOIN). */
    public List<RFIDMapping> findByOwnerId(int ownerId) throws SQLException {
        String sql = """
                SELECT rm.*
                FROM rfid_mapping rm
                JOIN vehicle v ON rm.vehicle_id = v.vehicle_id
                WHERE v.owner_id = ? AND rm.status = 'Active'
                """;
        List<RFIDMapping> mappings = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ownerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    mappings.add(mapRow(rs));
                }
            }
        }
        return mappings;
    }

    public List<RFIDMapping> findAll() throws SQLException {
        String sql = "SELECT * FROM rfid_mapping WHERE status = 'Active' ORDER BY created_at DESC";
        List<RFIDMapping> mappings = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                mappings.add(mapRow(rs));
            }
        }
        return mappings;
    }

    public void update(RFIDMapping mapping) throws SQLException {
        String sql = "UPDATE rfid_mapping SET rfid_tag_number = ?, vehicle_id = ?, status = ? WHERE rfid_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, mapping.getRfidTag());
            stmt.setInt(2, mapping.getVehicleId());
            stmt.setString(3, mapping.isActive() ? "Active" : "Inactive");
            stmt.setInt(4, mapping.getRfidId());

            stmt.executeUpdate();
        }
    }

    public void delete(int rfidId) throws SQLException {
        String sql = "DELETE FROM rfid_mapping WHERE rfid_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, rfidId);
            stmt.executeUpdate();
        }
    }

    private RFIDMapping mapRow(ResultSet rs) throws SQLException {
        RFIDMapping mapping = new RFIDMapping();
        mapping.setRfidId(rs.getInt("rfid_id"));
        mapping.setRfidTag(rs.getString("rfid_tag_number"));
        mapping.setVehicleId(rs.getInt("vehicle_id"));
        mapping.setActive("Active".equalsIgnoreCase(rs.getString("status")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            mapping.setAssignedDate(createdAt.toLocalDateTime());
        }

        return mapping;
    }
}