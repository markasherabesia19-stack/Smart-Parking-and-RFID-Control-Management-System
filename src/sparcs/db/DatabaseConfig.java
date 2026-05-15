package db;

import javax.sql.DataSource;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.*;
import java.util.Properties;

/**
 * Database Configuration and Connection Management
 * Shows a setup dialog if db.properties is missing or connection fails.
 */
public class DatabaseConfig {
    private static DatabaseConfig instance;
    private static String JDBC_URL;
    private static String USERNAME;
    private static String PASSWORD;

    private DatabaseConfig() {
        initializeDriver();
        if (!loadProperties()) {
            showSetupDialog();
        }
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    /**
     * Loads database credentials from db.properties file.
     * Returns true if loaded successfully, false otherwise.
     */
    private boolean loadProperties() {
        Properties props = new Properties();

        // 1. Try loading from external file next to the JAR/EXE first
        File externalProps = new File("db.properties");
        if (externalProps.exists()) {
            try (InputStream input = new FileInputStream(externalProps)) {
                props.load(input);
                JDBC_URL = props.getProperty("db.url");
                USERNAME = props.getProperty("db.username");
                PASSWORD = props.getProperty("db.password");
                System.out.println("[DatabaseConfig] Loaded credentials from external db.properties");
                return testConnection();
            } catch (IOException e) {
                System.err.println("[DatabaseConfig] Error reading external db.properties: " + e.getMessage());
            }
        }

        // 2. Try loading from classpath
        try (InputStream input = DatabaseConfig.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (input != null) {
                props.load(input);
                JDBC_URL = props.getProperty("db.url");
                USERNAME = props.getProperty("db.username");
                PASSWORD = props.getProperty("db.password");
                System.out.println("[DatabaseConfig] Loaded credentials from classpath db.properties");
                return testConnection();
            }
        } catch (IOException e) {
            System.err.println("[DatabaseConfig] Error reading classpath db.properties: " + e.getMessage());
        }

        System.err.println("[DatabaseConfig] db.properties not found. Showing setup dialog.");
        return false;
    }

    /**
     * Tests the current connection credentials.
     */
    private boolean testConnection() {
        try {
            Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
            conn.close();
            System.out.println("[DatabaseConfig] Connection test successful!");
            return true;
        } catch (SQLException e) {
            System.err.println("[DatabaseConfig] Connection test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Shows a dialog to enter DB credentials.
     * Saves them to db.properties next to the JAR/EXE.
     */
    private void showSetupDialog() {
        // Keep showing dialog until connection succeeds
        while (true) {
            // Build dialog
            JDialog dialog = new JDialog();
            dialog.setTitle("SPARCS - Database Setup");
            dialog.setModal(true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setSize(420, 350);
            dialog.setLocationRelativeTo(null);
            dialog.setResizable(false);

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(new EmptyBorder(20, 30, 20, 30));
            panel.setBackground(new Color(30, 30, 30));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(6, 6, 6, 6);

            // Title
            JLabel title = new JLabel("Database Connection Setup");
            title.setForeground(Color.WHITE);
            title.setFont(new Font("Segoe UI", Font.BOLD, 16));
            title.setHorizontalAlignment(SwingConstants.CENTER);
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            panel.add(title, gbc);

            // Subtitle
            JLabel subtitle = new JLabel("Enter your MySQL database credentials");
            subtitle.setForeground(new Color(180, 180, 180));
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            subtitle.setHorizontalAlignment(SwingConstants.CENTER);
            gbc.gridy = 1;
            panel.add(subtitle, gbc);

            gbc.gridwidth = 1;

            // Host
            addLabel(panel, gbc, "Host:", 2, 0);
            JTextField hostField = addField(panel, gbc, "localhost", 2, 1);

            // Port
            addLabel(panel, gbc, "Port:", 3, 0);
            JTextField portField = addField(panel, gbc, "3306", 3, 1);

            // Database
            addLabel(panel, gbc, "Database:", 4, 0);
            JTextField dbField = addField(panel, gbc, "sparcs_db", 4, 1);

            // Username
            addLabel(panel, gbc, "Username:", 5, 0);
            JTextField userField = addField(panel, gbc, "root", 5, 1);

            // Password
            addLabel(panel, gbc, "Password:", 6, 0);
            JPasswordField passField = new JPasswordField();
            passField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            gbc.gridx = 1; gbc.gridy = 6;
            panel.add(passField, gbc);

            // Status label
            JLabel statusLabel = new JLabel(" ");
            statusLabel.setForeground(new Color(255, 100, 100));
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
            gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
            panel.add(statusLabel, gbc);

            // Connect button
            JButton connectBtn = new JButton("Connect");
            connectBtn.setBackground(new Color(0, 120, 215));
            connectBtn.setForeground(Color.WHITE);
            connectBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            connectBtn.setFocusPainted(false);
            connectBtn.setBorderPainted(false);
            connectBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            gbc.gridy = 8;
            panel.add(connectBtn, gbc);

            // Wrap result
            boolean[] connected = {false};

            connectBtn.addActionListener(e -> {
                String host = hostField.getText().trim();
                String port = portField.getText().trim();
                String db = dbField.getText().trim();
                String user = userField.getText().trim();
                String pass = new String(passField.getPassword());

                String url = "jdbc:mysql://" + host + ":" + port + "/" + db +
                        "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

                statusLabel.setForeground(new Color(255, 200, 0));
                statusLabel.setText("Testing connection...");
                connectBtn.setEnabled(false);

                // Test in background
                SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Boolean doInBackground() {
                        try {
                            Connection conn = DriverManager.getConnection(url, user, pass);
                            conn.close();
                            return true;
                        } catch (SQLException ex) {
                            return false;
                        }
                    }

                    @Override
                    protected void done() {
                        try {
                            if (get()) {
                                // Save to db.properties
                                JDBC_URL = url;
                                USERNAME = user;
                                PASSWORD = pass;
                                saveProperties(host, port, db, user, pass);
                                connected[0] = true;
                                dialog.dispose();
                            } else {
                                statusLabel.setForeground(new Color(255, 80, 80));
                                statusLabel.setText("❌ Connection failed. Check your credentials.");
                                connectBtn.setEnabled(true);
                            }
                        } catch (Exception ex) {
                            statusLabel.setText("Error: " + ex.getMessage());
                            connectBtn.setEnabled(true);
                        }
                    }
                };
                worker.execute();
            });

            dialog.add(panel);
            dialog.setVisible(true);

            if (connected[0]) break;

            // If user closed dialog without connecting, exit app
            int choice = JOptionPane.showConfirmDialog(null,
                    "No database connection. Exit application?",
                    "Connection Required",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        }
    }

    private void addLabel(JPanel panel, GridBagConstraints gbc, String text, int row, int col) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(200, 200, 200));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridx = col; gbc.gridy = row;
        panel.add(label, gbc);
    }

    private JTextField addField(JPanel panel, GridBagConstraints gbc, String placeholder, int row, int col) {
        JTextField field = new JTextField(placeholder);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridx = col; gbc.gridy = row;
        panel.add(field, gbc);
        return field;
    }

    /**
     * Saves credentials to db.properties next to the JAR/EXE
     */
    private void saveProperties(String host, String port, String db, String user, String pass) {
        Properties props = new Properties();
        props.setProperty("db.url", "jdbc:mysql://" + host + ":" + port + "/" + db +
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        props.setProperty("db.username", user);
        props.setProperty("db.password", pass);
        props.setProperty("db.driver", "com.mysql.cj.jdbc.Driver");

        try (OutputStream out = new FileOutputStream("db.properties")) {
            props.store(out, "SPARCS Database Configuration - Auto-generated");
            System.out.println("[DatabaseConfig] Credentials saved to db.properties");
        } catch (IOException e) {
            System.err.println("[DatabaseConfig] Could not save db.properties: " + e.getMessage());
        }
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
}