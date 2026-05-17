package ui.admin;

import dao.UserAccountDAO;
import dao.AuditLogDAO;
import model.UserAccount;
import model.AuditLog;
import model.AppState;
import util.PasswordUtil;
import util.UIFactory;
import util.DialogUtil;
import util.UIConstants;
import ui.shared.SidebarPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;

/**
 * SPARCS — Admin Manage Accounts Screen (v1)
 * Allows admins to create, deactivate, reactivate, and reset passwords for user accounts.
 */
public class AdminManageAccountsScreen {

    private static DefaultTableModel tableModel;
    private static UserAccountDAO userDAO = new UserAccountDAO();
    private static AuditLogDAO auditDAO = new AuditLogDAO();

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UIConstants.C_BG_DARK);
        root.setOpaque(true);
        root.setName("ADMIN_MANAGE_ACCOUNTS");
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_MANAGE_ACCOUNTS"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UIConstants.C_BG_DARK);
        content.setOpaque(true);

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UIConstants.C_BG_PANEL);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(30, 28, 69));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        topBar.setOpaque(false);
        topBar.setPreferredSize(new Dimension(0, 54));
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("MANAGE ACCOUNTS", Font.BOLD, 22, new Color(240, 236, 255)), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Search and button panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        controlPanel.setOpaque(false);

        JTextField searchField = UIFactory.styledField("Search by name or username...");
        searchField.setPreferredSize(new Dimension(300, 38));
        controlPanel.add(searchField);

        JButton createBtn = UIFactory.gradientButton("+ Create Account");
        controlPanel.add(createBtn);

        mainPanel.add(controlPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Full Name", "Username", "Email", "Role", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                c.setBackground(isRowSelected(row)
                    ? new Color(36, 30, 107)
                    : new Color(26, 22, 80));
                c.setForeground(new Color(196, 191, 237));
                c.setFont(new Font("SansSerif", Font.PLAIN, 12));
                return c;
            }
        };
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(44);
        table.setBackground(new Color(26, 22, 80));
        table.setForeground(new Color(196, 191, 237));
        table.setGridColor(new Color(45, 40, 100));
        table.getTableHeader().setBackground(new Color(18, 16, 58));
        table.getTableHeader().setForeground(new Color(232, 228, 255));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(UIConstants.C_BG_DARK);

        // Button panel for row actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionsPanel.setOpaque(false);
        actionsPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JButton deactBtn = UIFactory.outlineButton("Deactivate");
        JButton reactBtn = UIFactory.outlineButton("Reactivate");
        JButton resetPwdBtn = UIFactory.outlineButton("Reset Password");

        actionsPanel.add(deactBtn);
        actionsPanel.add(reactBtn);
        actionsPanel.add(resetPwdBtn);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.add(actionsPanel, BorderLayout.SOUTH);

        mainPanel.add(tablePanel, BorderLayout.CENTER);

        // ── Actions ──────────────────────────────────────────────────────────
        createBtn.addActionListener(e -> showCreateAccountDialog(state));

        deactBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int userId = (int) tableModel.getValueAt(row, 0);
                String username = (String) tableModel.getValueAt(row, 2);
                
                // Don't allow deactivating own account
                if (userId == state.getCurrentUserAccount().getUserId()) {
                    DialogUtil.showMessageDialog(null, "You cannot deactivate your own account.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                deactivateAccount(userId, username, state);
                loadTableData();
            } else {
                DialogUtil.showMessageDialog(null, "Please select an account.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        reactBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int userId = (int) tableModel.getValueAt(row, 0);
                String username = (String) tableModel.getValueAt(row, 2);
                reactivateAccount(userId, username, state);
                loadTableData();
            } else {
                DialogUtil.showMessageDialog(null, "Please select an account.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        resetPwdBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int userId = (int) tableModel.getValueAt(row, 0);
                String username = (String) tableModel.getValueAt(row, 2);
                showResetPasswordDialog(userId, username, state);
            } else {
                DialogUtil.showMessageDialog(null, "Please select an account.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterTable(searchField.getText()); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterTable(searchField.getText()); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTable(searchField.getText()); }
        });

        // Load data on screen show
        root.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && root.isShowing()) {
                loadTableData();
            }
        });

        content.add(mainPanel, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private static void loadTableData() {
        tableModel.setRowCount(0);
        try {
            List<UserAccount> accounts = userDAO.findAllIncludingInactive();
            for (UserAccount account : accounts) {
                String status = account.isActive() ? "ACTIVE" : "INACTIVE";
                tableModel.addRow(new Object[]{
                    account.getUserId(),
                    account.getFullName() != null ? account.getFullName() : "",
                    account.getUsername(),
                    account.getEmail() != null ? account.getEmail() : "",
                    account.getRole(),
                    status
                });
            }
        } catch (SQLException ex) {
            DialogUtil.showMessageDialog(null, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private static void filterTable(String query) {
        tableModel.setRowCount(0);
        try {
            List<UserAccount> accounts = userDAO.findAllIncludingInactive();
            String lowerQuery = query.toLowerCase();
            for (UserAccount account : accounts) {
                if (account.getUsername().toLowerCase().contains(lowerQuery) ||
                    (account.getFullName() != null && account.getFullName().toLowerCase().contains(lowerQuery))) {
                    String status = account.isActive() ? "ACTIVE" : "INACTIVE";
                    tableModel.addRow(new Object[]{
                        account.getUserId(),
                        account.getFullName() != null ? account.getFullName() : "",
                        account.getUsername(),
                        account.getEmail() != null ? account.getEmail() : "",
                        account.getRole(),
                        status
                    });
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private static void deactivateAccount(int userId, String username, AppState state) {
        try {
            UserAccount account = new UserAccount();
            account.setUserId(userId);
            account.setActive(false);
            
            // Get current user to update only active flag
            UserAccountDAO dao = new UserAccountDAO();
            String sql = "UPDATE user_account SET is_active = FALSE WHERE user_id = ?";
            java.sql.Connection conn = db.DatabaseConfig.getInstance().getConnection();
            java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            stmt.close();
            conn.close();

            // Log action
            AuditLog log = new AuditLog();
            log.setUserId(state.getCurrentUserAccount().getUserId());
            log.setAction("DEACTIVATE_ACCOUNT");
            log.setEntityType("USER");
            log.setEntityId(userId);
            log.setNewValue(username);
            auditDAO.create(log);

            DialogUtil.showMessageDialog(null, "Account deactivated successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            DialogUtil.showMessageDialog(null, "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private static void reactivateAccount(int userId, String username, AppState state) {
        try {
            String sql = "UPDATE user_account SET is_active = TRUE WHERE user_id = ?";
            java.sql.Connection conn = db.DatabaseConfig.getInstance().getConnection();
            java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            stmt.close();
            conn.close();

            // Log action
            AuditLog log = new AuditLog();
            log.setUserId(state.getCurrentUserAccount().getUserId());
            log.setAction("REACTIVATE_ACCOUNT");
            log.setEntityType("USER");
            log.setEntityId(userId);
            log.setNewValue(username);
            auditDAO.create(log);

            DialogUtil.showMessageDialog(null, "Account reactivated successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            DialogUtil.showMessageDialog(null, "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private static void showCreateAccountDialog(AppState state) {
        JDialog dialog = new JDialog((Frame) null, "Create Account", true);
        dialog.setSize(400, 380);
        dialog.setLocationRelativeTo(null);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 10));
        panel.setBackground(UIConstants.C_BG_DARK);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLbl = new JLabel("Create New Account");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLbl.setForeground(UIConstants.C_WHITE);
        panel.add(titleLbl);

        JLabel fullNameLbl = new JLabel("Full Name:");
        fullNameLbl.setForeground(UIConstants.C_MUTED);
        panel.add(fullNameLbl);
        JTextField fullNameField = UIFactory.styledField("");
        panel.add(fullNameField);

        JLabel emailLbl = new JLabel("Email:");
        emailLbl.setForeground(UIConstants.C_MUTED);
        panel.add(emailLbl);
        JTextField emailField = UIFactory.styledField("");
        panel.add(emailField);

        JLabel usernameLbl = new JLabel("Username:");
        usernameLbl.setForeground(UIConstants.C_MUTED);
        panel.add(usernameLbl);
        JTextField usernameField = UIFactory.styledField("");
        panel.add(usernameField);

        JLabel passwordLbl = new JLabel("Password:");
        passwordLbl.setForeground(UIConstants.C_MUTED);
        panel.add(passwordLbl);
        JPasswordField passwordField = UIFactory.styledPasswordField("");
        panel.add(passwordField);

        JLabel roleLbl = new JLabel("Role:");
        roleLbl.setForeground(UIConstants.C_MUTED);
        panel.add(roleLbl);
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"USER", "ADMIN"});
        roleCombo.setBackground(new Color(25, 10, 60, 120));
        roleCombo.setForeground(UIConstants.C_WHITE);
        panel.add(roleCombo);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton cancelBtn = UIFactory.outlineButton("Cancel");
        JButton createBtn = UIFactory.gradientButton("Create");

        buttonPanel.add(cancelBtn);
        buttonPanel.add(createBtn);
        panel.add(buttonPanel);

        cancelBtn.addActionListener(e -> dialog.dispose());

        createBtn.addActionListener(e -> {
            String fullName = fullNameField.getText().trim();
            String email = emailField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String role = (String) roleCombo.getSelectedItem();

            if (username.isEmpty() || password.isEmpty() || role == null) {
                DialogUtil.showMessageDialog(dialog, "Please fill in all required fields.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                UserAccount newAccount = new UserAccount();
                newAccount.setFullName(fullName);
                newAccount.setEmail(email);
                newAccount.setUsername(username);
                newAccount.setPasswordHash(PasswordUtil.hashPassword(password));
                newAccount.setRole(role);
                newAccount.setActive(true);

                userDAO.create(newAccount);

                // Log action
                AuditLog log = new AuditLog();
                log.setUserId(state.getCurrentUserAccount().getUserId());
                log.setAction("CREATE_ACCOUNT");
                log.setEntityType("USER");
                log.setEntityId(newAccount.getUserId());
                log.setNewValue(username);
                auditDAO.create(log);

                DialogUtil.showMessageDialog(dialog, "Account created successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadTableData();
            } catch (SQLException ex) {
                DialogUtil.showMessageDialog(dialog, "Error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private static void showResetPasswordDialog(int userId, String username, AppState state) {
        JDialog dialog = new JDialog((Frame) null, "Reset Password", true);
        dialog.setSize(350, 220);
        dialog.setLocationRelativeTo(null);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 10));
        panel.setBackground(UIConstants.C_BG_DARK);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLbl = new JLabel("Reset Password for " + username);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLbl.setForeground(UIConstants.C_WHITE);
        panel.add(titleLbl);

        JLabel passwordLbl = new JLabel("New Password:");
        passwordLbl.setForeground(UIConstants.C_MUTED);
        panel.add(passwordLbl);
        JPasswordField passwordField = UIFactory.styledPasswordField("");
        panel.add(passwordField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton cancelBtn = UIFactory.outlineButton("Cancel");
        JButton resetBtn = UIFactory.gradientButton("Reset");

        buttonPanel.add(cancelBtn);
        buttonPanel.add(resetBtn);
        panel.add(buttonPanel);

        cancelBtn.addActionListener(e -> dialog.dispose());

        resetBtn.addActionListener(e -> {
            String newPassword = new String(passwordField.getPassword());

            if (newPassword.isEmpty()) {
                DialogUtil.showMessageDialog(dialog, "Please enter a new password.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                userDAO.updatePassword(userId, PasswordUtil.hashPassword(newPassword));

                // Log action
                AuditLog log = new AuditLog();
                log.setUserId(state.getCurrentUserAccount().getUserId());
                log.setAction("RESET_PASSWORD");
                log.setEntityType("USER");
                log.setEntityId(userId);
                log.setNewValue(username);
                auditDAO.create(log);

                DialogUtil.showMessageDialog(dialog, "Password reset successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (SQLException ex) {
                DialogUtil.showMessageDialog(dialog, "Error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }
}
