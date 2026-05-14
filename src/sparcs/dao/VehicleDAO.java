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
        String sql = "INSERT INTO vehicle (owner_id, plate_number, rfid_tag_id, vehicle_type, model, color, is_active, parking_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, vehicle.getOwnerId());
            stmt.setString(2, vehicle.getPlateNumber());
            stmt.setObject(3, vehicle.getRfidTagId());
            stmt.setString(4, vehicle.getVehicleType());
            stmt.setString(5, vehicle.getModel());
            stmt.setString(6, vehicle.getColor());
            stmt.setBoolean(7, vehicle.isActive());
            stmt.setString(8, vehicle.getParkingStatus() != null ? vehicle.getParkingStatus() : "Not Parked");

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
        String sql = "SELECT * FROM vehicle WHERE plate_number = ?";

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
        String sql = "SELECT * FROM vehicle WHERE owner_id = ? ORDER BY registration_date DESC";
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

    /**
     * Fetches all active vehicles for a given user_id by joining across all owner rows.
     * Use this instead of chaining findAllByUserId + findByOwnerId when you only need vehicles.
     */
    public List<Vehicle> findAllByUserId(int userId) throws SQLException {
        String sql = "SELECT v.* FROM vehicle v " +
                "INNER JOIN vehicle_owner vo ON v.owner_id = vo.owner_id " +
                "WHERE vo.user_id = ? " +
                "ORDER BY v.registration_date DESC";
        List<Vehicle> vehicles = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vehicles.add(mapResultSetToVehicle(rs));
                }
            }
        }
        return vehicles;
    }

    public List<Vehicle> findAll() throws SQLException {
        String sql = "SELECT * FROM vehicle ORDER BY plate_number";
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
        String sql = "UPDATE vehicle SET owner_id = ?, plate_number = ?, rfid_tag_id = ?, " +
                "vehicle_type = ?, model = ?, color = ?, is_active = ?, parking_status = ? WHERE vehicle_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vehicle.getOwnerId());
            stmt.setString(2, vehicle.getPlateNumber());
            stmt.setObject(3, vehicle.getRfidTagId());
            stmt.setString(4, vehicle.getVehicleType());
            stmt.setString(5, vehicle.getModel());
            stmt.setString(6, vehicle.getColor());
            stmt.setBoolean(7, vehicle.isActive());
            stmt.setString(8, vehicle.getParkingStatus());
            stmt.setInt(9, vehicle.getVehicleId());

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

    public void updateParkingStatus(int vehicleId, String parkingStatus) throws SQLException {
        String sql = "UPDATE vehicle SET parking_status = ? WHERE vehicle_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, parkingStatus);
            stmt.setInt(2, vehicleId);
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

    public int countParkedVehicles() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM vehicle WHERE parking_status = 'Parked'";

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
        vehicle.setOwnerId(rs.getInt("owner_id"));
        vehicle.setPlateNumber(rs.getString("plate_number"));

        Integer rfidTagId = rs.getInt("rfid_tag_id");
        if (!rs.wasNull()) {
            vehicle.setRfidTagId(rfidTagId);
        }

        vehicle.setVehicleType(rs.getString("vehicle_type"));
        vehicle.setModel(rs.getString("model"));
        vehicle.setColor(rs.getString("color"));
        vehicle.setActive(rs.getBoolean("is_active"));
        vehicle.setParkingStatus(rs.getString("parking_status"));

        Timestamp registrationDate = rs.getTimestamp("registration_date");
        if (registrationDate != null) {
            vehicle.setRegistrationDate(registrationDate.toLocalDateTime());
        }

        return vehicle;
    }
}