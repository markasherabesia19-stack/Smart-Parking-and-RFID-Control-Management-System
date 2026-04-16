package db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database Configuration and Connection Management
 * Uses basic JDBC connections (HikariCP can be added later for better pooling)
 */
public class DatabaseConfig {
    private static DatabaseConfig instance;
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/sparcs_db?useSSL=false&serverTimezone=UTC";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "NewPass2026!";

    private DatabaseConfig() {
        initializeDriver();
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    private void initializeDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC Driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load MySQL JDBC Driver: " + e.getMessage());
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
    }

    public Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
        } catch (SQLException e) {
            System.err.println("Failed to get database connection: " + e.getMessage());
            throw e;
        }
    }

    public void closePool() {
        // No connection pool to close with basic JDBC
        // In production, use HikariCP: https://github.com/brettwooldridge/HikariCP
        System.out.println("Database connections closed");
    }

    public DataSource getDataSource() {
        // Not implemented for basic JDBC
        // For pooling, add HikariCP to lib/ folder and uncomment HikariCP implementation
        return null;
    }

    /**
     * Test database connection
     */
    public static void testConnection() {
        try {
            Connection conn = DatabaseConfig.getInstance().getConnection();
            if (conn != null) {
                System.out.println("Database connection successful!");
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }
}

