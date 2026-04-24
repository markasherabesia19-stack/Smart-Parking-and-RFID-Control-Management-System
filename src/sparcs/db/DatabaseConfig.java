package db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Database Configuration and Connection Management
 * Loads credentials from db.properties (never committed to GitHub)
 */
public class DatabaseConfig {
    private static DatabaseConfig instance;
    private static String JDBC_URL;
    private static String USERNAME;
    private static String PASSWORD;

    private DatabaseConfig() {
        loadProperties();
        initializeDriver();
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    /**
     * Loads database credentials from db.properties file.
     * Each developer keeps their own db.properties locally.
     * This file is listed in .gitignore so it's never pushed to GitHub.
     */
    private void loadProperties() {
        Properties props = new Properties();

        // Try loading from classpath (works when db.properties is in project root
        // and root is marked as a source/resource path)
        try (InputStream input = DatabaseConfig.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (input != null) {
                props.load(input);
                JDBC_URL = props.getProperty("db.url");
                USERNAME = props.getProperty("db.username");
                PASSWORD = props.getProperty("db.password");
                System.out.println("[DatabaseConfig] Loaded credentials from db.properties");
                return;
            }
        } catch (IOException e) {
            System.err.println("[DatabaseConfig] Error reading db.properties: " + e.getMessage());
        }

        // Fallback — remind developer to create the file
        System.err.println("[DatabaseConfig] db.properties not found!");
        System.err.println("  → Create a file called 'db.properties' in your project root.");
        System.err.println("  → Contents should be:");
        System.err.println("       db.url=jdbc:mysql://localhost:3306/sparcs_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        System.err.println("       db.username=root");
        System.err.println("       db.password=YourMySQLPasswordHere");
        throw new RuntimeException("db.properties not found. See instructions above.");
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
        System.out.println("Database connections closed");
    }

    public DataSource getDataSource() {
        return null;
    }

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