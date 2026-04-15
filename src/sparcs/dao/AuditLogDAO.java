package dao;

import db.DatabaseConfig;
import model.AuditLog;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    public void create(AuditLog log) throws SQLException {
        String sql = "INSERT INTO audit_log (user_id, action, entity_type, entity_id, old_value, new_value, ip_address) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setObject(1, log.getUserId());
            stmt.setString(2, log.getAction());
            stmt.setString(3, log.getEntityType());
            stmt.setObject(4, log.getEntityId());
            stmt.setString(5, log.getOldValue());
            stmt.setString(6, log.getNewValue());
            stmt.setString(7, log.getIpAddress());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating audit log failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    log.setAuditId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<AuditLog> findAll() throws SQLException {
        String sql = "SELECT * FROM audit_log ORDER BY created_at DESC";
        List<AuditLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                logs.add(mapResultSetToAuditLog(rs));
            }
        }
        return logs;
    }

    public List<AuditLog> findByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = "SELECT * FROM audit_log WHERE DATE(created_at) BETWEEN ? AND ? ORDER BY created_at DESC";
        List<AuditLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(startDate));
            stmt.setDate(2, java.sql.Date.valueOf(endDate));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
        }
        return logs;
    }

    public List<AuditLog> findByAction(String action) throws SQLException {
        String sql = "SELECT * FROM audit_log WHERE action = ? ORDER BY created_at DESC";
        List<AuditLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, action);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
        }
        return logs;
    }

    public List<AuditLog> findByEntityType(String entityType) throws SQLException {
        String sql = "SELECT * FROM audit_log WHERE entity_type = ? ORDER BY created_at DESC";
        List<AuditLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, entityType);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
        }
        return logs;
    }

    public List<AuditLog> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM audit_log WHERE user_id = ? ORDER BY created_at DESC";
        List<AuditLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
        }
        return logs;
    }

    public long getTotalLogsCount() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM audit_log";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong("count");
            }
        }
        return 0;
    }

    private AuditLog mapResultSetToAuditLog(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setAuditId(rs.getInt("audit_id"));

        Integer userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            log.setUserId(userId);
        }

        log.setAction(rs.getString("action"));
        log.setEntityType(rs.getString("entity_type"));

        Integer entityId = rs.getInt("entity_id");
        if (!rs.wasNull()) {
            log.setEntityId(entityId);
        }

        log.setOldValue(rs.getString("old_value"));
        log.setNewValue(rs.getString("new_value"));
        log.setIpAddress(rs.getString("ip_address"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            log.setCreatedAt(createdAt.toLocalDateTime());
        }

        return log;
    }
}
