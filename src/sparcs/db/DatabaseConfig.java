package db;

import javax.sql.DataSource;
import javax.swing.*;
import java.awt.*;
import util.UIFactory;
import static util.UIConstants.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;


public class DatabaseConfig {
    private static final String SCHEMA_FILE = "db/sparcs_db.sql";
    private static final String SCHEMA_FILE_FALLBACK = "sparcs_db.sql";
    private static final String[] REQUIRED_TABLES = {
            "audit_log",
            "fee_schedule",
            "parking_slot",
            "parking_transaction",
            "rfid_mapping",
            "user_account",
            "vehicle",
            "vehicle_owner"
    };

    private static DatabaseConfig instance;
    private static String JDBC_URL;
    private static String USERNAME;
    private static String PASSWORD;
    private static boolean configured;
    private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".sparcs");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("db.properties");

    private DatabaseConfig() {
        initializeDriver();
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            if (!configured) {
                throw new IllegalStateException("Database credentials have not been configured yet");
            }
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
            if (!configured || JDBC_URL == null || USERNAME == null || PASSWORD == null) {
                throw new SQLException("Database credentials are not configured");
            }
            Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
            // Force READ COMMITTED so every query sees the latest committed data
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(true);
            return conn;
        } catch (SQLException e) {
            System.err.println("Failed to get database connection: " + e.getMessage());
            throw e;
        }
    }

    public static void promptForCredentials(Component parentComponent) {
        // Try auto-loading saved credentials first
        try {
            if (Files.exists(CONFIG_FILE)) {
                Properties saved = loadSavedCredentials();
                if (saved != null) {
                    String host = saved.getProperty("db.host");
                    String port = saved.getProperty("db.port");
                    String database = saved.getProperty("db.name");
                    String username = saved.getProperty("db.user");
                    String password = new String(Base64.getDecoder().decode(saved.getProperty("db.password")));
                    try {
                        prepareDatabase(host, port, database, username, password);
                        JDBC_URL = buildJdbcUrl(host, port, database);
                        USERNAME = username;
                        PASSWORD = password;
                        configured = true;
                        instance = new DatabaseConfig();
                        return; // success, don't show dialog
                    } catch (Exception ex) {
                        // failed to use saved credentials — fall through to show dialog
                        System.err.println("Saved DB credentials failed: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading saved DB credentials: " + e.getMessage());
        }

        final JDialog dialog = new JDialog((Frame) null, "Connect to Database", true);
        final boolean[] connected = {false};
        JPanel root = UIFactory.cardPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        GridBagConstraints topGbc = new GridBagConstraints();
        topGbc.gridx = 0; topGbc.gridy = 0; topGbc.insets = new Insets(0,0,8,0);
        top.add(UIFactory.logoPanel(48), topGbc);
        topGbc.gridy = 1; topGbc.insets = new Insets(0,0,0,0);
        top.add(UIFactory.lbl("Database Connection", Font.BOLD, 16, C_WHITE), topGbc);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField hostField = UIFactory.styledField("Host"); hostField.setText("localhost");
        JTextField portField = UIFactory.styledField("Port"); portField.setText("3306");
        JTextField databaseField = UIFactory.styledField("Database"); databaseField.setText("sparcs_db");
        JTextField usernameField = UIFactory.styledField("Username"); usernameField.setText("root");
        JPasswordField passwordField = UIFactory.styledPasswordField("Password");

        gbc.gridx = 0; gbc.gridy = 0; form.add(UIFactory.lbl("Host", Font.PLAIN, 12, C_MUTED), gbc);
        gbc.gridx = 1; form.add(hostField, gbc);

        gbc.gridx = 0; gbc.gridy++; form.add(UIFactory.lbl("Port", Font.PLAIN, 12, C_MUTED), gbc);
        gbc.gridx = 1; form.add(portField, gbc);

        gbc.gridx = 0; gbc.gridy++; form.add(UIFactory.lbl("Database", Font.PLAIN, 12, C_MUTED), gbc);
        gbc.gridx = 1; form.add(databaseField, gbc);

        gbc.gridx = 0; gbc.gridy++; form.add(UIFactory.lbl("Username", Font.PLAIN, 12, C_MUTED), gbc);
        gbc.gridx = 1; form.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy++; form.add(UIFactory.lbl("Password", Font.PLAIN, 12, C_MUTED), gbc);
        gbc.gridx = 1; form.add(passwordField, gbc);

        JCheckBox remember = new JCheckBox("Remember for session");
        remember.setOpaque(false);
        remember.setForeground(C_MUTED);
        gbc.gridx = 1; gbc.gridy++; form.add(remember, gbc);

        JButton connectBtn = UIFactory.gradientButton("Connect");
        JButton cancelBtn = UIFactory.outlineButton("Cancel");

        JLabel status = UIFactory.lbl("", Font.PLAIN, 12, C_MUTED);

        root.add(top, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8,8));
        bottom.setOpaque(false);
        bottom.add(status, BorderLayout.NORTH);
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnWrap.setOpaque(false);
        btnWrap.add(cancelBtn);
        btnWrap.add(connectBtn);
        bottom.add(btnWrap, BorderLayout.CENTER);

        root.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(parentComponent);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        cancelBtn.addActionListener(e -> {
            dialog.dispose();
        });

        connectBtn.addActionListener(e -> {
            String host = hostField.getText().trim();
            String port = portField.getText().trim();
            String database = databaseField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (host.isEmpty() || port.isEmpty() || database.isEmpty() || username.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please complete all database fields.", "Missing Information", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                Integer.parseInt(port);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Port must be a valid number.", "Invalid Port", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // disable while working
            connectBtn.setEnabled(false);
            cancelBtn.setEnabled(false);
            status.setText("Connecting...");

            SwingWorker<Void,Void> worker = new SwingWorker<>() {
                Exception problem = null;

                @Override
                protected Void doInBackground() {
                    try {
                        prepareDatabase(host, port, database, username, password);
                        JDBC_URL = buildJdbcUrl(host, port, database);
                        USERNAME = username;
                        PASSWORD = password;
                        configured = true;
                        instance = new DatabaseConfig();
                        if (remember.isSelected()) {
                            try {
                                saveCredentials(host, port, database, username, password);
                            } catch (IOException ioe) {
                                System.err.println("Failed to save DB credentials: " + ioe.getMessage());
                            }
                        }
                    } catch (Exception ex) {
                        problem = ex;
                    }
                    return null;
                }

                @Override
                protected void done() {
                    connectBtn.setEnabled(true);
                    cancelBtn.setEnabled(true);
                    if (problem == null) {
                        connected[0] = true;
                        dialog.dispose();
                    } else {
                        status.setText("");
                        JOptionPane.showMessageDialog(dialog, "Could not connect: " + problem.getMessage(), "Connection Failed", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        });

        dialog.setVisible(true);

        if (!connected[0]) {
            throw new IllegalStateException("Database connection setup cancelled by user");
        }
    }

    private static void prepareDatabase(String host, String port, String database, String username, String password)
            throws SQLException, IOException {
        String targetUrl = buildJdbcUrl(host, port, database);

        try (Connection connection = DriverManager.getConnection(targetUrl, username, password)) {
            if (hasRequiredTables(connection)) {
                return;
            }

            if (!hasAnyTables(connection)) {
                importSchema(host, port, database, username, password);
                return;
            }

            throw new SQLException("Database exists but its schema is incomplete. Import the schema manually before starting the app.");
        } catch (SQLException ex) {
            if (!isUnknownDatabase(ex)) {
                throw ex;
            }

            createDatabase(host, port, database, username, password);
            importSchema(host, port, database, username, password);
        }
    }

    private static void createDatabase(String host, String port, String database, String username, String password)
            throws SQLException {
        String serverUrl = buildJdbcUrl(host, port, null);
        String createSql = "CREATE DATABASE IF NOT EXISTS `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci";

        try (Connection connection = DriverManager.getConnection(serverUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(createSql);
        }
    }

    private static void importSchema(String host, String port, String database, String username, String password)
            throws SQLException, IOException {
        String schemaSql = readSchemaSql();
        String targetUrl = buildJdbcUrl(host, port, database) + "&allowMultiQueries=true";

        try (Connection connection = DriverManager.getConnection(targetUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute(schemaSql);
        }
    }

    private static boolean hasRequiredTables(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();

        for (String table : REQUIRED_TABLES) {
            try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, table, new String[]{"TABLE"})) {
                if (!resultSet.next()) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean hasAnyTables(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();

        try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    private static boolean isUnknownDatabase(SQLException exception) {
        return exception.getErrorCode() == 1049 || "42000".equals(exception.getSQLState());
    }

    private static String buildJdbcUrl(String host, String port, String database) {
        StringBuilder builder = new StringBuilder("jdbc:mysql://")
                .append(host)
                .append(":")
                .append(port)
                .append("/");

        if (database != null && !database.isBlank()) {
            builder.append(database);
        }

        builder.append("?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useLocalSessionState=false&useLocalTransactionState=false&autoReconnect=true&failOverReadOnly=false&cachePrepStmts=false&cacheResultSetMetadata=false");
        return builder.toString();
    }

    private static String readSchemaSql() throws IOException {
        byte[] schemaBytes = null;

        try (InputStream classpathStream = DatabaseConfig.class.getClassLoader().getResourceAsStream(SCHEMA_FILE)) {
            if (classpathStream != null) {
                schemaBytes = classpathStream.readAllBytes();
            }
        }

        if (schemaBytes == null) {
            try (InputStream fallbackStream = DatabaseConfig.class.getClassLoader().getResourceAsStream(SCHEMA_FILE_FALLBACK)) {
                if (fallbackStream != null) {
                    schemaBytes = fallbackStream.readAllBytes();
                }
            }
        }

        if (schemaBytes == null) {
            Path schemaPath = Paths.get(SCHEMA_FILE);
            if (Files.exists(schemaPath)) {
                schemaBytes = Files.readAllBytes(schemaPath);
            }
        }

        if (schemaBytes == null) {
            Path fallbackPath = Paths.get(SCHEMA_FILE_FALLBACK);
            if (Files.exists(fallbackPath)) {
                schemaBytes = Files.readAllBytes(fallbackPath);
            }
        }

        if (schemaBytes == null) {
            throw new IOException("Schema file not found in classpath or working directory: " + SCHEMA_FILE);
        }

        return decodeSchemaBytes(schemaBytes);
    }

    private static String decodeSchemaBytes(byte[] bytes) {
        Charset charset = StandardCharsets.UTF_8;
        int offset = 0;

        if (bytes.length >= 2) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            if (b0 == 0xFF && b1 == 0xFE) {
                charset = StandardCharsets.UTF_16LE;
                offset = 2;
            } else if (b0 == 0xFE && b1 == 0xFF) {
                charset = StandardCharsets.UTF_16BE;
                offset = 2;
            }
        }

        if (offset == 0 && bytes.length >= 3) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            int b2 = bytes[2] & 0xFF;
            if (b0 == 0xEF && b1 == 0xBB && b2 == 0xBF) {
                charset = StandardCharsets.UTF_8;
                offset = 3;
            }
        }

        return new String(bytes, offset, bytes.length - offset, charset);
    }

    private static Properties loadSavedCredentials() throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            p.load(in);
        }
        return p;
    }

    private static void saveCredentials(String host, String port, String db, String user, String password) throws IOException {
        if (!Files.exists(CONFIG_DIR)) {
            Files.createDirectories(CONFIG_DIR);
        }

        Properties p = new Properties();
        p.setProperty("db.host", host);
        p.setProperty("db.port", port);
        p.setProperty("db.name", db);
        p.setProperty("db.user", user);
        p.setProperty("db.password", Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)));

        try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
            p.store(out, "SPARCS saved DB credentials (password is base64-encoded)");
        }

        // attempt to set restrictive permissions on POSIX systems
        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(CONFIG_FILE, perms);
        } catch (UnsupportedOperationException | IOException ignored) {
            // ignore on Windows / systems without POSIX file attrs
        }
    }

    private static void addField(JPanel panel, GridBagConstraints constraints, String label, JComponent field) {
        constraints.gridx = 0;
        panel.add(new JLabel(label + ":"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        panel.add(field, constraints);
        constraints.gridy++;
    }

    public void closePool() {
        System.out.println("Database connections closed");
    }

    public DataSource getDataSource() {
        return null;
    }

    public static void testConnection() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            if (conn != null) {
                System.out.println("Database connection successful!");
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}