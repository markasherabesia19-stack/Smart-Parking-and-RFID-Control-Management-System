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
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;

/**
 * SPARCS — Admin Manage Accounts Screen (v2)
 * Redesigned to match design system: consistent with AdminAuditLogScreen.
 * - Badge renderers for Status (ACTIVE/INACTIVE) and Role (ADMIN/USER)
 * - Rounded card container wrapping search + table + actions
 * - Proper search field with focus ring
 * - Action buttons styled per design system (Danger / Success / Secondary)
 * - Table header, row, hover all matching #1A1650 / #241E6B
 */
public class AdminManageAccountsScreen {

    // ── Design tokens ────────────────────────────────────────────────────────
    private static final Color C_BG_DARK    = new Color(13, 11, 31);    // #0D0B1F
    private static final Color C_BG_PANEL   = new Color(18, 16, 58);    // #12103A
    private static final Color C_BG_ROW     = new Color(26, 22, 80);    // #1A1650
    private static final Color C_ROW_HOVER  = new Color(36, 30, 107);   // #241E6B
    private static final Color C_BORDER     = new Color(45, 40, 96);    // #2D2860
    private static final Color C_BORDER_SUB = new Color(30, 28, 69);    // #1E1C45
    private static final Color C_FOCUS      = new Color(124, 92, 191);  // #7C5CBF
    private static final Color C_TEXT_HEAD  = new Color(232, 228, 255); // #E8E4FF
    private static final Color C_TEXT_BODY  = new Color(196, 191, 237); // #C4BFED
    private static final Color C_TEXT_MUTED = new Color(155, 143, 212); // #9B8FD4
    private static final Color C_TEXT_HINT  = new Color(107, 95, 160);  // #6B5FA0
    private static final Color C_GREEN      = new Color(29, 185, 84);   // #1DB954
    private static final Color C_RED        = new Color(232, 54, 93);   // #E8365D
    private static final Color C_ORANGE     = new Color(255, 140, 66);  // #FF8C42
    private static final Color C_BLUE       = new Color(79, 142, 247);  // #4F8EF7
    private static final Color C_PURPLE     = new Color(124, 92, 191);  // #7C5CBF
    private static final Color C_GRAD_START = new Color(124, 92, 191);  // #7C5CBF
    private static final Color C_GRAD_END   = new Color(214, 56, 128);  // #D63880

    private static DefaultTableModel tableModel;
    private static UserAccountDAO userDAO = new UserAccountDAO();
    private static AuditLogDAO auditDAO = new AuditLogDAO();

    // ── Build ─────────────────────────────────────────────────────────────────
    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.setOpaque(true);
        root.setName("ADMIN_MANAGE_ACCOUNTS");
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_MANAGE_ACCOUNTS"),
                BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);
        content.setOpaque(true);

        // ── Top bar ──────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(C_BG_PANEL);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(C_BORDER_SUB);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        topBar.setOpaque(false);
        topBar.setPreferredSize(new Dimension(0, 54));
        topBar.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel titleLbl = new JLabel("MANAGE ACCOUNTS");
        titleLbl.setFont(new Font("Inter", Font.BOLD, 22));
        titleLbl.setForeground(new Color(240, 236, 255));
        topBar.add(titleLbl, BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        // ── Main area ────────────────────────────────────────────────────────
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // ── Card container (rounded, wraps everything) ───────────────────────
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        // ── Control row: search + create button ──────────────────────────────
        JPanel controlRow = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(C_BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); // top radius only
                g2.setColor(C_BORDER_SUB);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        controlRow.setOpaque(false);
        controlRow.setBorder(new EmptyBorder(14, 16, 14, 16));

        JTextField searchField = buildSearchField("Search by username or email…");
        searchField.setPreferredSize(new Dimension(340, 38));

        JButton createBtn = buildGradientButton("+ Create Account");

        JPanel rightCtrl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightCtrl.setOpaque(false);
        rightCtrl.add(createBtn);

        controlRow.add(searchField, BorderLayout.CENTER);
        controlRow.add(rightCtrl, BorderLayout.EAST);
        card.add(controlRow, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────────────────────
        String[] columns = {"ID", "Username", "Email", "Role", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel) {
            private int hoveredRow = -1;
            {
                addMouseMotionListener(new MouseAdapter() {
                    @Override public void mouseMoved(MouseEvent e) {
                        int row = rowAtPoint(e.getPoint());
                        if (row != hoveredRow) { hoveredRow = row; repaint(); }
                    }
                });
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseExited(MouseEvent e) { hoveredRow = -1; repaint(); }
                });
            }
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                boolean selected = isRowSelected(row);
                boolean hovered  = (row == hoveredRow && !selected);
                Color bg = selected ? C_ROW_HOVER : (hovered ? new Color(30, 26, 90) : C_BG_ROW);
                if (c instanceof JPanel) {
                    c.setBackground(bg);
                    for (Component child : ((JPanel) c).getComponents()) child.setBackground(bg);
                } else {
                    c.setBackground(bg);
                    c.setForeground(C_TEXT_BODY);
                    c.setFont(new Font("Inter", Font.PLAIN, 13));
                }
                return c;
            }
        };
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(44);
        table.setBackground(C_BG_ROW);
        table.setForeground(C_TEXT_BODY);
        table.setGridColor(C_BORDER_SUB);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(C_ROW_HOVER);
        table.setSelectionForeground(C_TEXT_BODY);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFocusable(false);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setBackground(C_BG_PANEL);
        header.setForeground(C_TEXT_HEAD);
        header.setFont(new Font("Inter", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = new JLabel(v != null ? v.toString().toUpperCase() : "");
                lbl.setFont(new Font("Inter", Font.BOLD, 13));
                lbl.setForeground(C_TEXT_HEAD);
                lbl.setBorder(new EmptyBorder(0, col == 0 ? 16 : 12, 0, 4));
                lbl.setOpaque(true);
                lbl.setBackground(C_BG_PANEL);
                return lbl;
            }
        });

        // Column widths — ID, Username, Email, Role, Status
        int[] widths = {55, 200, 320, 90, 100};
        for (int i = 0; i < widths.length; i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setPreferredWidth(widths[i]);
            col.setMinWidth(widths[i] / 2);
        }

        // Custom renderers — Role=col3, Status=col4
        table.getColumnModel().getColumn(3).setCellRenderer(new RoleBadgeRenderer());   // Role
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeRenderer()); // Status

        // Default text renderer with left padding — cols 0..2 (ID, Username, Email)
        DefaultTableCellRenderer paddedRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                lbl.setBorder(new EmptyBorder(0, col == 0 ? 16 : 12, 0, 4));
                lbl.setFont(new Font("Inter", Font.PLAIN, 13));
                lbl.setForeground(C_TEXT_BODY);
                return lbl;
            }
        };
        paddedRenderer.setOpaque(true);
        for (int i = 0; i < 3; i++) table.getColumnModel().getColumn(i).setCellRenderer(paddedRenderer);

        // Scroll pane — no border, clip corners inside card
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(C_BG_ROW);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(60, 50, 120);
                trackColor = C_BG_PANEL;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0)); b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
        });

        card.add(scrollPane, BorderLayout.CENTER);

        // ── Action buttons row ────────────────────────────────────────────────
        JPanel actionsBar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(C_BG_PANEL);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(C_BORDER_SUB);
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        actionsBar.setOpaque(false);
        actionsBar.setBorder(new EmptyBorder(12, 16, 14, 16));

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnGroup.setOpaque(false);

        JButton deactBtn    = buildDangerButton("Deactivate");
        JButton reactBtn    = buildSuccessButton("Reactivate");
        JButton resetPwdBtn = buildSecondaryButton("Reset Password");

        btnGroup.add(deactBtn);
        btnGroup.add(reactBtn);
        btnGroup.add(resetPwdBtn);
        actionsBar.add(btnGroup, BorderLayout.WEST);
        card.add(actionsBar, BorderLayout.SOUTH);

        mainPanel.add(card, BorderLayout.CENTER);

        // ── Wire actions ──────────────────────────────────────────────────────
        createBtn.addActionListener(e -> showCreateAccountDialog(state));

        deactBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { showError("Please select an account first."); return; }
            int userId   = (int) tableModel.getValueAt(row, 0);
            String uname = (String) tableModel.getValueAt(row, 2);
            if (userId == state.getCurrentUserAccount().getUserId()) {
                showError("You cannot deactivate your own account.");
                return;
            }
            deactivateAccount(userId, uname, state);
            loadTableData();
        });

        reactBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { showError("Please select an account first."); return; }
            int userId   = (int) tableModel.getValueAt(row, 0);
            String uname = (String) tableModel.getValueAt(row, 2);
            reactivateAccount(userId, uname, state);
            loadTableData();
        });

        resetPwdBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { showError("Please select an account first."); return; }
            int userId   = (int) tableModel.getValueAt(row, 0);
            String uname = (String) tableModel.getValueAt(row, 2);
            showResetPasswordDialog(userId, uname, state);
        });

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterTable(getRealText(searchField)); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterTable(getRealText(searchField)); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTable(getRealText(searchField)); }
        });

        // Load data when screen is shown
        root.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && root.isShowing()) {
                loadTableData();
            }
        });

        content.add(mainPanel, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static String getRealText(JTextField field) {
        String t = field.getText();
        return (t.startsWith("Search")) ? "" : t;
    }

    private static void showError(String msg) {
        DialogUtil.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ── Data ──────────────────────────────────────────────────────────────────
    private static void loadTableData() {
        tableModel.setRowCount(0);
        try {
            List<UserAccount> accounts = userDAO.findAllIncludingInactive();
            for (UserAccount a : accounts) {
                if (!"ADMIN".equalsIgnoreCase(a.getRole())) continue; // admins only
                tableModel.addRow(new Object[]{
                    a.getUserId(),
                    a.getUsername(),
                    a.getEmail()     != null ? a.getEmail()     : "",
                    a.getRole(),
                    a.isActive() ? "ACTIVE" : "INACTIVE"
                });
            }
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static void filterTable(String query) {
        tableModel.setRowCount(0);
        try {
            List<UserAccount> accounts = userDAO.findAllIncludingInactive();
            String q = query.toLowerCase();
            for (UserAccount a : accounts) {
                if (!"ADMIN".equalsIgnoreCase(a.getRole())) continue; // admins only
                boolean userMatch  = a.getUsername().toLowerCase().contains(q);
                boolean emailMatch = a.getEmail()     != null && a.getEmail().toLowerCase().contains(q);
                if (q.isEmpty() || userMatch || emailMatch) {
                    tableModel.addRow(new Object[]{
                        a.getUserId(),
                        a.getUsername(),
                        a.getEmail()     != null ? a.getEmail()     : "",
                        a.getRole(),
                        a.isActive() ? "ACTIVE" : "INACTIVE"
                    });
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private static void deactivateAccount(int userId, String username, AppState state) {
        try {
            String sql = "UPDATE user_account SET is_active = FALSE WHERE user_id = ?";
            java.sql.Connection conn = db.DatabaseConfig.getInstance().getConnection();
            java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            stmt.close(); conn.close();

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
            showError("Error: " + ex.getMessage());
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
            stmt.close(); conn.close();

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
            showError("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    private static void showCreateAccountDialog(AppState state) {
        JDialog dialog = buildStyledDialog("Create Account", 440, 0); // height via pack()

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(18, 16, 58));              // #12103A — card bg
        panel.setBorder(new EmptyBorder(28, 28, 24, 28));

        // Title — 22px #F0ECFF per design system §2
        addDialogTitle(panel, "Create New Account");
        addVGap(panel, 4);

        // Subtitle
        JLabel sub = new JLabel("Fill in the fields below to register an account.");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(new Color(107, 95, 160));              // #6B5FA0 muted
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sub);
        addVGap(panel, 20);

        // Fields — no Full Name (admins don't use it per DB schema)
        JTextField emailField    = addLabeledField(panel, "EMAIL", "");
        addVGap(panel, 12);
        JTextField usernameField = addLabeledField(panel, "USERNAME", "");
        addVGap(panel, 12);
        JPasswordField pwdField  = addLabeledPassword(panel, "PASSWORD");
        addVGap(panel, 12);

        // Role combo
        addFieldLabel(panel, "ROLE");
        JComboBox<String> roleCombo = buildStyledCombo(new String[]{"USER", "ADMIN"});
        roleCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        roleCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(roleCombo);
        addVGap(panel, 24);

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(30, 28, 69));                // #1E1C45
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sep);
        addVGap(panel, 16);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        JButton cancelBtn = buildSecondaryButton("Cancel");
        JButton createBtn = buildGradientButton("Create Account");
        btnRow.add(cancelBtn);
        btnRow.add(createBtn);
        panel.add(btnRow);

        cancelBtn.addActionListener(e -> dialog.dispose());

        // Enter on any text field fires create — set as dialog default button
        emailField.addActionListener(e -> createBtn.doClick());
        usernameField.addActionListener(e -> createBtn.doClick());
        pwdField.addActionListener(e -> createBtn.doClick());
        dialog.getRootPane().setDefaultButton(createBtn);

        createBtn.addActionListener(e -> {
            String email    = emailField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(pwdField.getPassword());
            String role     = (String) roleCombo.getSelectedItem();

            if (username.isEmpty() || password.isEmpty()) {
                DialogUtil.showMessageDialog(dialog, "Username and password are required.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                UserAccount a = new UserAccount();
                a.setFullName(null);                             // not used for admin accounts
                a.setEmail(email);
                a.setUsername(username);
                a.setPasswordHash(PasswordUtil.hashPassword(password));
                a.setRole(role); a.setActive(true);
                userDAO.create(a);

                AuditLog log = new AuditLog();
                log.setUserId(state.getCurrentUserAccount().getUserId());
                log.setAction("CREATE_ACCOUNT"); log.setEntityType("USER");
                log.setEntityId(a.getUserId()); log.setNewValue(username);
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

        dialog.add(wrapInCard(panel));
        dialog.pack();                                           // auto-size — no cut-off
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private static void showResetPasswordDialog(int userId, String username, AppState state) {
        JDialog dialog = buildStyledDialog("Reset Password", 420, 0); // height via pack()

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(18, 16, 58));              // #12103A — card bg
        panel.setBorder(new EmptyBorder(28, 28, 24, 28));

        // Title — 22px #F0ECFF per design system §2
        addDialogTitle(panel, "Reset Password");
        addVGap(panel, 4);

        // Subtitle — who we're resetting for
        JLabel subLbl = new JLabel("Resetting password for:  " + username);
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subLbl.setForeground(new Color(107, 95, 160));           // #6B5FA0 muted
        subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(subLbl);
        addVGap(panel, 20);

        JPasswordField pwdField = addLabeledPassword(panel, "NEW PASSWORD");
        addVGap(panel, 24);

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(30, 28, 69));                // #1E1C45
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sep);
        addVGap(panel, 16);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        JButton cancelBtn = buildSecondaryButton("Cancel");
        JButton resetBtn  = buildGradientButton("Reset Password");
        btnRow.add(cancelBtn);
        btnRow.add(resetBtn);
        panel.add(btnRow);

        cancelBtn.addActionListener(e -> dialog.dispose());

        // Enter on the password field fires reset — and set as dialog default button
        pwdField.addActionListener(e -> resetBtn.doClick());
        dialog.getRootPane().setDefaultButton(resetBtn);

        resetBtn.addActionListener(e -> {
            String newPwd = new String(pwdField.getPassword());
            if (newPwd.isEmpty()) {
                DialogUtil.showMessageDialog(dialog, "Please enter a new password.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // ── Same-password check ───────────────────────────────────────────
            // Fetch the account's current password hash and compare against new input.
            // If they match, reject — password must be different from the old one.
            try {
                List<UserAccount> accounts = userDAO.findAllIncludingInactive();
                String currentHash = null;
                for (UserAccount a : accounts) {
                    if (a.getUserId() == userId) {
                        currentHash = a.getPasswordHash();
                        break;
                    }
                }
                if (currentHash != null && PasswordUtil.verifyPassword(newPwd, currentHash)) {
                    DialogUtil.showMessageDialog(dialog,
                        "New password cannot be the same as the current password.\nPlease choose a different one.",
                        "Same Password", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (Exception ex) {
                // If we can't verify, proceed — don't block the reset on a lookup failure
            }

            try {
                userDAO.updatePassword(userId, PasswordUtil.hashPassword(newPwd));
                AuditLog log = new AuditLog();
                log.setUserId(state.getCurrentUserAccount().getUserId());
                log.setAction("RESET_PASSWORD"); log.setEntityType("USER");
                log.setEntityId(userId); log.setNewValue(username);
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

        dialog.add(wrapInCard(panel));
        dialog.pack();                                           // auto-size — no cut-off
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    // ── Dialog builder helpers ────────────────────────────────────────────────
    private static JDialog buildStyledDialog(String title, int w, int h) {
        JDialog d = new JDialog((Frame) null, title, true);
        d.setUndecorated(true);                                  // remove native title bar
        d.setMinimumSize(new Dimension(w, 200));
        d.setResizable(false);
        d.setBackground(new Color(0, 0, 0, 0));
        d.getRootPane().setOpaque(false);
        d.getRootPane().setBackground(new Color(0, 0, 0, 0));
        return d;
    }

    /** Wraps the form panel in a DialogUtil-style card with rounded corners + purple border glow. */
    private static JPanel wrapInCard(JPanel inner) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Same bg as DialogUtil card
                g2.setColor(new Color(22, 14, 56));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // Purple border glow — matches DialogUtil exactly
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(new Color(80, 60, 160, 180));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        inner.setOpaque(false);                                  // let card bg show through
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private static void addDialogTitle(JPanel panel, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 16));       // matches DialogUtil title 16px bold
        lbl.setForeground(new Color(240, 235, 255));             // #F0EBFF — DialogUtil C_WHITE
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
    }

    private static void addFieldLabel(JPanel panel, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Inter", Font.BOLD, 11));
        lbl.setForeground(C_TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(4));
    }

    private static JTextField addLabeledField(JPanel panel, String label, String placeholder) {
        addFieldLabel(panel, label);
        JTextField field = buildStyledTextField(placeholder);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(field);
        return field;
    }

    private static JPasswordField addLabeledPassword(JPanel panel, String label) {
        addFieldLabel(panel, label);
        JPasswordField field = buildStyledPasswordField();
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(field);
        return field;
    }

    private static void addVGap(JPanel panel, int h) {
        panel.add(Box.createVerticalStrut(h));
    }

    // ── Component builders ────────────────────────────────────────────────────
    /** Search field with placeholder simulation and focus ring. */
    private static JTextField buildSearchField(String placeholder) {
        JTextField field = new JTextField() {
            private boolean focused = false;
            {
                addFocusListener(new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                    @Override public void focusLost(FocusEvent e)   { focused = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(13, 11, 31));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(focused ? C_FOCUS : C_BORDER);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        field.setOpaque(false);
        field.setForeground(C_TEXT_HINT);
        field.setCaretColor(C_TEXT_BODY);
        field.setFont(new Font("Inter", Font.PLAIN, 13));
        field.setBorder(new EmptyBorder(5, 12, 5, 12));
        field.setText(placeholder);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(C_TEXT_BODY);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(C_TEXT_HINT);
                }
            }
        });
        return field;
    }

    private static JTextField buildStyledTextField(String placeholder) {
        JTextField field = new JTextField() {
            private boolean focused = false;
            {
                addFocusListener(new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                    @Override public void focusLost(FocusEvent e)   { focused = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BG_DARK);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(focused ? C_FOCUS : C_BORDER);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        field.setOpaque(false);
        field.setForeground(C_TEXT_BODY);
        field.setCaretColor(C_TEXT_BODY);
        field.setFont(new Font("Inter", Font.PLAIN, 13));
        field.setBorder(new EmptyBorder(5, 12, 5, 12));
        return field;
    }

    private static JPasswordField buildStyledPasswordField() {
        JPasswordField field = new JPasswordField() {
            private boolean focused = false;
            {
                addFocusListener(new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                    @Override public void focusLost(FocusEvent e)   { focused = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BG_DARK);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(focused ? C_FOCUS : C_BORDER);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        field.setOpaque(false);
        field.setForeground(C_TEXT_BODY);
        field.setCaretColor(C_TEXT_BODY);
        field.setFont(new Font("Inter", Font.PLAIN, 13));
        field.setBorder(new EmptyBorder(5, 12, 5, 12));
        return field;
    }

    private static JComboBox<String> buildStyledCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setBackground(C_BG_ROW);
        cb.setForeground(C_TEXT_BODY);
        cb.setFont(new Font("Inter", Font.PLAIN, 13));
        cb.setBorder(BorderFactory.createLineBorder(C_BORDER));
        cb.setFocusable(false);
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                lbl.setBackground(selected ? C_ROW_HOVER : C_BG_ROW);
                lbl.setForeground(C_TEXT_BODY);
                lbl.setBorder(new EmptyBorder(5, 10, 5, 10));
                lbl.setFont(new Font("Inter", Font.PLAIN, 13));
                lbl.setOpaque(true);
                return lbl;
            }
        });
        return cb;
    }

    /** Primary gradient button (#7C5CBF → #D63880). */
    private static JButton buildGradientButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, C_GRAD_START, getWidth(), 0, C_GRAD_END);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Inter", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 24, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Danger button — #E8365D background. */
    private static JButton buildDangerButton(String text) {
        return buildColorButton(text, C_RED);
    }

    /** Success button — #1DB954 background. */
    private static JButton buildSuccessButton(String text) {
        return buildColorButton(text, C_GREEN);
    }

    private static JButton buildColorButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover()
                        ? bg.brighter()
                        : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Inter", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 20, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Secondary button — transparent with 1.5px #4A3F8A border. */
    private static JButton buildSecondaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(74, 63, 138)); // #4A3F8A border fill on hover
                if (getModel().isRollover()) {
                    g2.setColor(new Color(74, 63, 138, 40));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.setColor(new Color(74, 63, 138));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Inter", Font.BOLD, 13));
        btn.setForeground(C_TEXT_BODY);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 20, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Badge Renderers ───────────────────────────────────────────────────────
    /**
     * Status badge: ACTIVE → green, INACTIVE → orange (matches design system).
     */
    static class StatusBadgeRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object value, boolean sel, boolean foc, int row, int col) {
            String text = value != null ? value.toString() : "—";
            Color[] clr;
            switch (text.toUpperCase()) {
                case "ACTIVE":   clr = new Color[]{ new Color(29, 185, 84, 38),  C_GREEN  }; break;
                case "INACTIVE": clr = new Color[]{ new Color(255, 140, 66, 38), C_ORANGE }; break;
                default:         clr = new Color[]{ new Color(124, 92, 191, 38), C_PURPLE }; break;
            }

            // Dot + label
            JLabel dot = new JLabel("\u2022");
            dot.setForeground(clr[1]);
            dot.setFont(new Font("Inter", Font.BOLD, 14));

            JLabel badge = new JLabel(text) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(clr[0]);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setOpaque(false);
            badge.setForeground(clr[1]);
            badge.setFont(new Font("Inter", Font.BOLD, 11));
            badge.setBorder(new EmptyBorder(3, 10, 3, 10));

            JPanel cell = new JPanel(new GridBagLayout());
            cell.setOpaque(true);
            cell.setBackground(sel ? C_ROW_HOVER : C_BG_ROW);
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.WEST;
            gc.insets = new Insets(0, 12, 0, 0);
            gc.weightx = 1.0;
            cell.add(badge, gc);
            return cell;
        }
    }

    /**
     * Role badge: ADMIN → purple, USER → blue chip.
     */
    static class RoleBadgeRenderer implements TableCellRenderer {
        private static final Color ADMIN_BG = new Color(124, 92, 191, 46);
        private static final Color ADMIN_FG = new Color(167, 139, 250);  // light purple
        private static final Color ADMIN_BD = new Color(107, 95, 160, 60);
        private static final Color USER_BG  = new Color(79, 142, 247, 38);
        private static final Color USER_FG  = C_BLUE;
        private static final Color USER_BD  = new Color(79, 142, 247, 60);

        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object value, boolean sel, boolean foc, int row, int col) {
            String text = value != null ? value.toString() : "—";
            boolean isAdmin = "ADMIN".equalsIgnoreCase(text);
            Color bg = isAdmin ? ADMIN_BG : USER_BG;
            Color fg = isAdmin ? ADMIN_FG : USER_FG;
            Color bd = isAdmin ? ADMIN_BD : USER_BD;

            JLabel chip = new JLabel(text) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(bd);
                    g2.setStroke(new BasicStroke(0.8f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            chip.setOpaque(false);
            chip.setForeground(fg);
            chip.setFont(new Font("Inter", Font.BOLD, 11));
            chip.setBorder(new EmptyBorder(3, 9, 3, 9));

            JPanel cell = new JPanel(new GridBagLayout());
            cell.setOpaque(true);
            cell.setBackground(sel ? C_ROW_HOVER : C_BG_ROW);
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.WEST;
            gc.insets = new Insets(0, 12, 0, 0);
            gc.weightx = 1.0;
            cell.add(chip, gc);
            return cell;
        }
    }
}