package dao;

import db.DatabaseConfig;
import model.VehicleOwner;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Vehicle Owner Data Access Object — matches actual vehicle_owner table.
 * DB columns: owner_id, user_id, full_name, phone_number, address, created_at, updated_at
 */
public class VehicleOwnerDAO {

    /**
     * Creates a new owner row only if no owner exists for the given user_id.
     * Returns the existing or newly created VehicleOwner.
     */
    public VehicleOwner createOrFind(VehicleOwner owner) throws SQLException {
        List<VehicleOwner> existing = findAllByUserId(owner.getUserId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        create(owner);
        return owner;
    }

    public void create(VehicleOwner owner) throws SQLException {
        String sql = "INSERT INTO vehicle_owner (user_id, full_name, phone_number, address) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setObject(1, owner.getUserId());
            stmt.setString(2, owner.getFirstName() + " " + owner.getLastName());
            stmt.setString(3, owner.getContactNumber());
            stmt.setString(4, owner.getAddress());

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

    /**
     * Returns the first owner row for a user_id.
     * Prefer findAllByUserId() when fetching vehicles to avoid missing registrations.
     */
    public Optional<VehicleOwner> findByUserId(int userId) throws SQLException {
        List<VehicleOwner> owners = findAllByUserId(userId);
        return owners.isEmpty() ? Optional.empty() : Optional.of(owners.get(0));
    }

    /**
     * Returns ALL owner rows for a given user_id.
     * Use this when collecting vehicles across all registrations for a user.
     */
    public List<VehicleOwner> findAllByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM vehicle_owner WHERE user_id = ?";
        List<VehicleOwner> owners = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    owners.add(mapResultSetToVehicleOwner(rs));
                }
            }
        }
        return owners;
    }

    public List<VehicleOwner> findAll() throws SQLException {
        String sql = "SELECT * FROM vehicle_owner ORDER BY full_name";
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
        String sql = "UPDATE vehicle_owner SET full_name = ?, phone_number = ?, address = ? " +
                "WHERE owner_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, owner.getFirstName() + " " + owner.getLastName());
            stmt.setString(2, owner.getContactNumber());
            stmt.setString(3, owner.getAddress());
            stmt.setInt(4, owner.getOwnerId());

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

    private VehicleOwner mapResultSetToVehicleOwner(ResultSet rs) throws SQLException {
        VehicleOwner owner = new VehicleOwner();
        owner.setOwnerId(rs.getInt("owner_id"));
        owner.setUserId(rs.getInt("user_id"));

        String fullName = rs.getString("full_name");
        if (fullName != null && fullName.contains(" ")) {
            owner.setFirstName(fullName.substring(0, fullName.indexOf(" ")));
            owner.setLastName(fullName.substring(fullName.indexOf(" ") + 1));
        } else {
            owner.setFirstName(fullName != null ? fullName : "");
            owner.setLastName("");
        }

        owner.setContactNumber(rs.getString("phone_number"));
        owner.setAddress(rs.getString("address"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) owner.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) owner.setUpdatedAt(updatedAt.toLocalDateTime());

        return owner;
    }
}