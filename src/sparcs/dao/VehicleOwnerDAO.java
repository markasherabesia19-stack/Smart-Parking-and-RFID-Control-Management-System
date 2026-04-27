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
        String sql = "INSERT INTO vehicle_owner (user_id, phone_number, address) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setObject(1, owner.getUserId());
            stmt.setString(2, owner.getContactNumber());
            stmt.setString(3, owner.getAddress());

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
        String sql = "SELECT * FROM vehicle_owner WHERE owner_id = ?";

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

    public Optional<VehicleOwner> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM vehicle_owner WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToVehicleOwner(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<VehicleOwner> findAll() throws SQLException {
        String sql = "SELECT * FROM vehicle_owner ORDER BY owner_id";
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
        String sql = "UPDATE vehicle_owner SET phone_number = ?, address = ? WHERE owner_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, owner.getContactNumber());
            stmt.setString(2, owner.getAddress());
            stmt.setInt(3, owner.getOwnerId());

            stmt.executeUpdate();
        }
    }

    public void delete(int ownerId) throws SQLException {
        String sql = "DELETE FROM vehicle_owner WHERE owner_id = ?";

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
        owner.setContactNumber(rs.getString("phone_number"));
        owner.setAddress(rs.getString("address"));
        owner.setActive(true);

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
