package dao;

import db.DatabaseConfig;
import model.FeeSchedule;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fee Schedule Data Access Object
 */
public class FeeScheduleDAO {

    public void create(FeeSchedule schedule) throws SQLException {
        String sql = "INSERT INTO fee_schedule (rate_per_hour, rate_per_day, grace_period_minutes, is_active, created_by) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setBigDecimal(1, schedule.getRatePerHour());
            stmt.setBigDecimal(2, schedule.getRatePerDay());
            stmt.setInt(3, schedule.getGracePeriodMinutes());
            stmt.setBoolean(4, schedule.isActive());
            stmt.setObject(5, schedule.getCreatedBy());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating fee schedule failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    schedule.setFeeId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public Optional<FeeSchedule> findById(int feeId) throws SQLException {
        String sql = "SELECT * FROM fee_schedule WHERE fee_id = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, feeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToFeeSchedule(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<FeeSchedule> findCurrentActive() throws SQLException {
        String sql = "SELECT * FROM fee_schedule ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return Optional.of(mapResultSetToFeeSchedule(rs));
            }
        }
        return Optional.empty();
    }

    public List<FeeSchedule> findAll() throws SQLException {
        String sql = "SELECT * FROM fee_schedule ORDER BY effective_date DESC";
        List<FeeSchedule> schedules = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                schedules.add(mapResultSetToFeeSchedule(rs));
            }
        }
        return schedules;
    }

    public List<FeeSchedule> findActive() throws SQLException {
        String sql = "SELECT * FROM fee_schedule ORDER BY created_at DESC";
        List<FeeSchedule> schedules = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                schedules.add(mapResultSetToFeeSchedule(rs));
            }
        }
        return schedules;
    }

    public void update(FeeSchedule schedule) throws SQLException {
        String sql = "UPDATE fee_schedule SET rate_per_hour = ?, rate_per_day = ?, " +
                "grace_period_minutes = ?, is_active = ? WHERE fee_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, schedule.getRatePerHour());
            stmt.setBigDecimal(2, schedule.getRatePerDay());
            stmt.setInt(3, schedule.getGracePeriodMinutes());
            stmt.setBoolean(4, schedule.isActive());
            stmt.setInt(5, schedule.getFeeId());

            stmt.executeUpdate();
        }
    }

    public void deactivate(int feeId) throws SQLException {
        String sql = "UPDATE fee_schedule SET is_active = FALSE WHERE fee_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, feeId);
            stmt.executeUpdate();
        }
    }

    private FeeSchedule mapResultSetToFeeSchedule(ResultSet rs) throws SQLException {
        FeeSchedule schedule = new FeeSchedule();
        schedule.setFeeId(rs.getInt("fee_id"));
        schedule.setRatePerHour(rs.getBigDecimal("fee_per_hour"));
        schedule.setRatePerDay(rs.getBigDecimal("daily_rate"));
        schedule.setGracePeriodMinutes(rs.getInt("grace_period_minutes"));
        schedule.setActive(true);

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            schedule.setEffectiveDate(createdAt.toLocalDateTime());
        }

        return schedule;
    }
}
