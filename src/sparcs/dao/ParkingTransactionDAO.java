package dao;

import db.DatabaseConfig;
import model.ParkingTransaction;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parking Transaction Data Access Object
 */
public class ParkingTransactionDAO {

    public void create(ParkingTransaction transaction) throws SQLException {
        String sql = "INSERT INTO parking_transaction (vehicle_id, slot_id, entry_time, " +
                "transaction_status, payment_status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, transaction.getVehicleId());
            stmt.setInt(2, transaction.getSlotId());
            stmt.setTimestamp(3, Timestamp.valueOf(transaction.getEntryTime()));
            stmt.setString(4, transaction.getTransactionStatus());
            stmt.setString(5, transaction.getPaymentStatus());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating parking transaction failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    transaction.setTransactionId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public Optional<ParkingTransaction> findById(int transactionId) throws SQLException {
        String sql = "SELECT * FROM parking_transaction WHERE transaction_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, transactionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTransaction(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<ParkingTransaction> findByVehicleId(int vehicleId) throws SQLException {
        String sql = "SELECT * FROM parking_transaction WHERE vehicle_id = ? ORDER BY entry_time DESC";
        List<ParkingTransaction> transactions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return transactions;
    }

    public List<ParkingTransaction> findByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = "SELECT * FROM parking_transaction WHERE DATE(entry_time) BETWEEN ? AND ? ORDER BY entry_time DESC";
        List<ParkingTransaction> transactions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(startDate));
            stmt.setDate(2, java.sql.Date.valueOf(endDate));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return transactions;
    }

    public List<ParkingTransaction> findPending() throws SQLException {
        String sql = "SELECT * FROM parking_transaction WHERE payment_status = 'PENDING' ORDER BY entry_time DESC";
        List<ParkingTransaction> transactions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        }
        return transactions;
    }

    public List<ParkingTransaction> findInProgress() throws SQLException {
        String sql = "SELECT * FROM parking_transaction WHERE transaction_status = 'IN_PROGRESS' ORDER BY entry_time DESC";
        List<ParkingTransaction> transactions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        }
        return transactions;
    }

    public void update(ParkingTransaction transaction) throws SQLException {
        String sql = "UPDATE parking_transaction SET vehicle_id = ?, slot_id = ?, entry_time = ?, " +
                "exit_time = ?, duration_minutes = ?, calculated_fee = ?, payment_status = ?, " +
                "transaction_status = ? WHERE transaction_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, transaction.getVehicleId());
            stmt.setInt(2, transaction.getSlotId());
            stmt.setTimestamp(3, Timestamp.valueOf(transaction.getEntryTime()));

            if (transaction.getExitTime() != null) {
                stmt.setTimestamp(4, Timestamp.valueOf(transaction.getExitTime()));
            } else {
                stmt.setNull(4, Types.TIMESTAMP);
            }

            if (transaction.getDurationMinutes() != null) {
                stmt.setInt(5, transaction.getDurationMinutes());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            if (transaction.getCalculatedFee() != null) {
                stmt.setBigDecimal(6, transaction.getCalculatedFee());
            } else {
                stmt.setNull(6, Types.DECIMAL);
            }

            stmt.setString(7, transaction.getPaymentStatus());
            stmt.setString(8, transaction.getTransactionStatus());
            stmt.setInt(9, transaction.getTransactionId());

            stmt.executeUpdate();
        }
    }

    public int countTodayTransactions() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM parking_transaction WHERE DATE(entry_time) = CURDATE()";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    private ParkingTransaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        ParkingTransaction transaction = new ParkingTransaction();
        transaction.setTransactionId(rs.getInt("transaction_id"));
        transaction.setVehicleId(rs.getInt("vehicle_id"));
        transaction.setSlotId(rs.getInt("slot_id"));

        Timestamp entryTime = rs.getTimestamp("entry_time");
        if (entryTime != null) {
            transaction.setEntryTime(entryTime.toLocalDateTime());
        }

        Timestamp exitTime = rs.getTimestamp("exit_time");
        if (exitTime != null) {
            transaction.setExitTime(exitTime.toLocalDateTime());
        }

        Integer durationMinutes = rs.getInt("duration_minutes");
        if (!rs.wasNull()) {
            transaction.setDurationMinutes(durationMinutes);
        }

        transaction.setCalculatedFee(rs.getBigDecimal("calculated_fee"));
        transaction.setPaymentStatus(rs.getString("payment_status"));
        transaction.setTransactionStatus(rs.getString("transaction_status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            transaction.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            transaction.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return transaction;
    }

    public List<ParkingTransaction> findRecent(int limit) throws SQLException {
        String sql = "SELECT * FROM parking_transaction ORDER BY entry_time DESC LIMIT ?";
        List<ParkingTransaction> transactions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return transactions;
    }

    public List<ParkingTransaction> findPendingPayments() throws SQLException {
        String sql = "SELECT * FROM parking_transaction WHERE payment_status = 'PENDING' ORDER BY exit_time DESC";
        List<ParkingTransaction> transactions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        }
        return transactions;
    }

    public int countPendingPayments() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM parking_transaction WHERE payment_status = 'PENDING'";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    public java.math.BigDecimal calculateTodayRevenue() throws SQLException {
        String sql = "SELECT COALESCE(SUM(calculated_fee), 0) as total FROM parking_transaction WHERE DATE(entry_time) = CURDATE() AND payment_status = 'PAID'";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
        }
        return java.math.BigDecimal.ZERO;
    }

    public List<ParkingTransaction> findByUserId(int userId) throws SQLException {
        String sql = "SELECT pt.* FROM parking_transaction pt " +
                "JOIN vehicle v ON pt.vehicle_id = v.vehicle_id " +
                "JOIN vehicle_owner vo ON v.vehicle_owner_id = vo.owner_id " +
                "WHERE vo.user_id = ? ORDER BY pt.entry_time DESC";
        List<ParkingTransaction> transactions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return transactions;
    }
}
