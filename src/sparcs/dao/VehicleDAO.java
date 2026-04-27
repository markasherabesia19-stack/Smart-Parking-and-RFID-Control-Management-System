package dao;

import db.DatabaseConfig;
import model.Vehicle;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Vehicle Data Access Object
 */
public class VehicleDAO {

    public void create(Vehicle vehicle) throws SQLException {
        String sql = "INSERT INTO vehicle (license_plate, vehicle_owner_id, vehicle_type, color) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, vehicle.getPlateNumber());
            stmt.setInt(2, vehicle.getOwnerId());
            stmt.setString(3, vehicle.getVehicleType());
            stmt.setString(4, vehicle.getColor());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating vehicle failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    vehicle.setVehicleId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public Optional<Vehicle> findById(int vehicleId) throws SQLException {
        String sql = "SELECT * FROM vehicle WHERE vehicle_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToVehicle(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Vehicle> findByPlateNumber(String plateNumber) throws SQLException {
        String sql = "SELECT * FROM vehicle WHERE license_plate = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, plateNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToVehicle(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Vehicle> findByOwnerId(int ownerId) throws SQLException {
        String sql = "SELECT * FROM vehicle WHERE vehicle_owner_id = ? ORDER BY created_at DESC";
        List<Vehicle> vehicles = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ownerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vehicles.add(mapResultSetToVehicle(rs));
                }
            }
        }
        return vehicles;
    }

    public List<Vehicle> findAll() throws SQLException {
        String sql = "SELECT * FROM vehicle ORDER BY license_plate";
        List<Vehicle> vehicles = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                vehicles.add(mapResultSetToVehicle(rs));
            }
        }
        return vehicles;
    }

    public void update(Vehicle vehicle) throws SQLException {
        String sql = "UPDATE vehicle SET license_plate = ?, vehicle_owner_id = ?, " +
                "vehicle_type = ?, color = ? WHERE vehicle_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, vehicle.getPlateNumber());
            stmt.setInt(2, vehicle.getOwnerId());
            stmt.setString(3, vehicle.getVehicleType());
            stmt.setString(4, vehicle.getColor());
            stmt.setInt(5, vehicle.getVehicleId());

            stmt.executeUpdate();
        }
    }

    public void delete(int vehicleId) throws SQLException {
        String sql = "DELETE FROM vehicle WHERE vehicle_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vehicleId);
            stmt.executeUpdate();
        }
    }

    public int countActiveVehicles() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM vehicle";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    private Vehicle mapResultSetToVehicle(ResultSet rs) throws SQLException {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(rs.getInt("vehicle_id"));
        vehicle.setOwnerId(rs.getInt("vehicle_owner_id"));
        vehicle.setPlateNumber(rs.getString("license_plate"));

        vehicle.setVehicleType(rs.getString("vehicle_type"));
        vehicle.setColor(rs.getString("color"));
        vehicle.setActive(true);

        Timestamp registrationDate = rs.getTimestamp("created_at");
        if (registrationDate != null) {
            vehicle.setRegistrationDate(registrationDate.toLocalDateTime());
        }

        return vehicle;
    }
}
