package sparcs.db;

import java.sql.*;

public class DBConnection {

    //  Configure these to match your MySQL setup 
    private static final String DB_URL  = "jdbc:mysql://localhost:3306/sparcs_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "your_password";

    private static Connection connection = null;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC Driver not found. " +
                        "Add mysql-connector-java to your lib/ folder.", e);
            }
        }
        return connection;
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
