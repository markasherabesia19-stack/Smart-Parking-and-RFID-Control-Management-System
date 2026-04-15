package dao;

import db.DatabaseConfig;
import model.VehicleOwner;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Vehicle Owner Data Access Object
 */
public class VehicleOwnerDAO {

    public void create(VehicleOwner owner) throws SQLException {
        String sql = "INSERT INTO vehicle_owner (first_name, last_name, student_id, contact_number, email, " +
                "address, wallet_balance, is_active, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, owner.getFirstName());
            stmt.setString(2, owner.getLastName());
            stmt.setString(3, owner.getStudentId());
            stmt.setString(4, owner.getContactNumber());
            stmt.setString(5, owner.getEmail());
            stmt.setString(6, owner.getAddress());
            stmt.setBigDecimal(7, owner.getWalletBalance());
            stmt.setBoolean(8, owner.isActive());
            stmt.setObject(9, owner.getUserId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating owner failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    owner.setOwnerId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public Optional<VehicleOwner> findById(int ownerId) throws SQLException {
        String sql = "SELECT * FROM vehicle_owner WHERE owner_id = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ownerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToVehicleOwner(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<VehicleOwner> findByStudentId(String studentId) throws SQLException {
        String sql = "SELECT * FROM vehicle_owner WHERE student_id = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToVehicleOwner(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<VehicleOwner> findAll() throws SQLException {
        String sql = "SELECT * FROM vehicle_owner WHERE is_active = TRUE ORDER BY last_name, first_name";
        List<VehicleOwner> owners = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                owners.add(mapResultSetToVehicleOwner(rs));
            }
        }
        return owners;
    }

    public void update(VehicleOwner owner) throws SQLException {
        String sql = "UPDATE vehicle_owner SET first_name = ?, last_name = ?, contact_number = ?, " +
                "email = ?, address = ?, wallet_balance = ?, is_active = ? WHERE owner_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, owner.getFirstName());
            stmt.setString(2, owner.getLastName());
            stmt.setString(3, owner.getContactNumber());
            stmt.setString(4, owner.getEmail());
            stmt.setString(5, owner.getAddress());
            stmt.setBigDecimal(6, owner.getWalletBalance());
            stmt.setBoolean(7, owner.isActive());
            stmt.setInt(8, owner.getOwnerId());

            stmt.executeUpdate();
        }
    }

    public void delete(int ownerId) throws SQLException {
        String sql = "UPDATE vehicle_owner SET is_active = FALSE WHERE owner_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ownerId);
            stmt.executeUpdate();
        }
    }

    public void addBalance(int ownerId, BigDecimal amount) throws SQLException {
        String sql = "UPDATE vehicle_owner SET wallet_balance = wallet_balance + ? WHERE owner_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, amount);
            stmt.setInt(2, ownerId);
            stmt.executeUpdate();
        }
    }

    public void deductBalance(int ownerId, BigDecimal amount) throws SQLException {
        String sql = "UPDATE vehicle_owner SET wallet_balance = wallet_balance - ? WHERE owner_id = ? " +
                "AND wallet_balance >= ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, amount);
            stmt.setInt(2, ownerId);
            stmt.setBigDecimal(3, amount);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Insufficient balance or owner not found");
            }
        }
    }

    public BigDecimal getBalance(int ownerId) throws SQLException {
        String sql = "SELECT wallet_balance FROM vehicle_owner WHERE owner_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ownerId);
            try (ResultSet rs= stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("wallet_balance");
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private VehicleOwner mapResultSetToVehicleOwner(ResultSet rs) throws SQLException {
        VehicleOwner owner = new VehicleOwner();
        owner.setOwnerId(rs.getInt("owner_id"));
        owner.setFirstName(rs.getString("first_name"));
        owner.setLastName(rs.getString("last_name"));
        owner.setStudentId(rs.getString("student_id"));
        owner.setContactNumber(rs.getString("contact_number"));
        owner.setEmail(rs.getString("email"));
        owner.setAddress(rs.getString("address"));
        owner.setWalletBalance(rs.getBigDecimal("wallet_balance"));
        owner.setActive(rs.getBoolean("is_active"));

        Integer userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            owner.setUserId(userId);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            owner.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            owner.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return owner;
    }
}
