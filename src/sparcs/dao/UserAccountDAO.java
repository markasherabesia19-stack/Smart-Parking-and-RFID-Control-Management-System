package dao;

import db.DatabaseConfig;
import model.UserAccount;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserAccountDAO {

    public void create(UserAccount userAccount) throws SQLException {
        String sql = "INSERT INTO user_account (username, password_hash, role, email, is_active) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, userAccount.getUsername());
            stmt.setString(2, userAccount.getPasswordHash());
            stmt.setString(3, userAccount.getRole());
            stmt.setString(4, userAccount.getEmail());
            stmt.setBoolean(5, userAccount.isActive());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user account failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    userAccount.setUserId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public Optional<UserAccount> findById(int userId) throws SQLException {
        String sql = "SELECT * FROM user_account WHERE user_id = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUserAccount(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<UserAccount> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM user_account WHERE username = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUserAccount(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if a username is already taken (case-insensitive).
     */
    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT 1 FROM user_account WHERE LOWER(username) = LOWER(?) AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Checks if an email is already registered (case-insensitive).
     */
    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM user_account WHERE LOWER(email) = LOWER(?) AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<UserAccount> findAll() throws SQLException {
        String sql = "SELECT * FROM user_account WHERE is_active = TRUE ORDER BY created_at DESC";
        List<UserAccount> accounts = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                accounts.add(mapResultSetToUserAccount(rs));
            }
        }
        return accounts;
    }

    public void update(UserAccount userAccount) throws SQLException {
        String sql = "UPDATE user_account SET username = ?, role = ?, email = ?, is_active = ? " +
                "WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userAccount.getUsername());
            stmt.setString(2, userAccount.getRole());
            stmt.setString(3, userAccount.getEmail());
            stmt.setBoolean(4, userAccount.isActive());
            stmt.setInt(5, userAccount.getUserId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating user account failed, no rows affected.");
            }
        }
    }

    public void delete(int userId) throws SQLException {
        String sql = "UPDATE user_account SET is_active = FALSE WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    public boolean authenticate(String username, String passwordHash) throws SQLException {
        String sql = "SELECT user_id FROM user_account WHERE username = ? AND password_hash = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<UserAccount> findByRole(String role) throws SQLException {
        String sql = "SELECT * FROM user_account WHERE role = ? AND is_active = TRUE";
        List<UserAccount> accounts = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapResultSetToUserAccount(rs));
                }
            }
        }
        return accounts;
    }

    private UserAccount mapResultSetToUserAccount(ResultSet rs) throws SQLException {
        UserAccount account = new UserAccount();
        account.setUserId(rs.getInt("user_id"));
        account.setUsername(rs.getString("username"));
        account.setPasswordHash(rs.getString("password_hash"));
        account.setRole(rs.getString("role"));
        account.setEmail(rs.getString("email"));
        account.setActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            account.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            account.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return account;
    }
}