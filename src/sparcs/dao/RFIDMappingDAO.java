package dao;

import db.DatabaseConfig;
import model.RFIDMapping;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RFID Mapping Data Access Object
 */
public class RFIDMappingDAO {

    public void create(RFIDMapping mapping) throws SQLException {
        String sql = "INSERT INTO rfid_mapping (rfid_tag, owner_id, is_active) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, mapping.getRfidTag());
            stmt.setInt(2, mapping.getOwnerId());
            stmt.setBoolean(3, mapping.isActive());

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
        String sql = "SELECT * FROM rfid_mapping WHERE rfid_id = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, rfidId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRFIDMapping(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<RFIDMapping> findByRFIDTag(String rfidTag) throws SQLException {
        String sql = "SELECT * FROM rfid_mapping WHERE rfid_tag = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, rfidTag);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRFIDMapping(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<RFIDMapping> findByOwnerId(int ownerId) throws SQLException {
        String sql = "SELECT * FROM rfid_mapping WHERE owner_id = ? AND is_active = TRUE";
        List<RFIDMapping> mappings = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ownerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    mappings.add(mapResultSetToRFIDMapping(rs));
                }
            }
        }
        return mappings;
    }

    public List<RFIDMapping> findAll() throws SQLException {
        String sql = "SELECT * FROM rfid_mapping WHERE is_active = TRUE ORDER BY assigned_date DESC";
        List<RFIDMapping> mappings = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                mappings.add(mapResultSetToRFIDMapping(rs));
            }
        }
        return mappings;
    }

    public void update(RFIDMapping mapping) throws SQLException {
        String sql = "UPDATE rfid_mapping SET rfid_tag = ?, owner_id = ?, is_active = ? WHERE rfid_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, mapping.getRfidTag());
            stmt.setInt(2, mapping.getOwnerId());
            stmt.setBoolean(3, mapping.isActive());
            stmt.setInt(4, mapping.getRfidId());

            stmt.executeUpdate();
        }
    }

    public void delete(int rfidId) throws SQLException {
        String sql = "UPDATE rfid_mapping SET is_active = FALSE WHERE rfid_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, rfidId);
            stmt.executeUpdate();
        }
    }

    private RFIDMapping mapResultSetToRFIDMapping(ResultSet rs) throws SQLException {
        RFIDMapping mapping = new RFIDMapping();
        mapping.setRfidId(rs.getInt("rfid_id"));
        mapping.setRfidTag(rs.getString("rfid_tag"));
        mapping.setOwnerId(rs.getInt("owner_id"));
        mapping.setActive(rs.getBoolean("is_active"));

        Timestamp assignedDate = rs.getTimestamp("assigned_date");
        if (assignedDate != null) {
            mapping.setAssignedDate(assignedDate.toLocalDateTime());
        }

        return mapping;
    }
}
