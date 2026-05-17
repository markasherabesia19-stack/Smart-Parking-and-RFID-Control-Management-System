package ui.shared;

import model.AppState;
import model.UserAccount;
import service.AuthenticationService;
import dao.AuditLogDAO;
import model.AuditLog;
import util.DialogUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

public class UnifiedLoginScreen {

    private static BufferedImage bgImage;
    private static BufferedImage logoImage;

    static {
        bgImage   = tryLoadFile("src/assets/gradientbg.png", "assets/gradientbg.png",
                                "src/main/resources/assets/gradientbg.png", "resources/gradientbg.png");
        logoImage = tryLoadFile("src/assets/logo.png", "assets/logo.png",
                                "src/main/resources/assets/logo.png", "resources/logo.png");

        if (bgImage   == null) bgImage   = tryLoadClasspath("/assets/gradientbg.png", "/gradientbg.png");
        if (logoImage == null) logoImage = tryLoadClasspath("/assets/logo.png", "/logo.png");
    }

    private static BufferedImage tryLoadFile(String... paths) {
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) { try { return ImageIO.read(f); } catch (IOException ignored) {} }
        }
        return null;
    }

    private static BufferedImage tryLoadClasspath(String... paths) {
        for (String path : paths) {
            try {
                URL u = UnifiedLoginScreen.class.getResource(path);
                if (u != null) return ImageIO.read(u);
            } catch (IOException ignored) {}
        }
        return null;
    }

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {

        // ── Outer background panel ───────────────────────────────────────────
        JPanel p = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                                    RenderingHints.VALUE_RENDER_QUALITY);
                if (bgImage != null) {
                    int iw = bgImage.getWidth(), ih = bgImage.getHeight();
                    int pw = getWidth(),          ph = getHeight();
                    double scale = Math.max((double) pw / iw, (double) ph / ih);
                    int dw = (int)(iw * scale), dh = (int)(ih * scale);
                    int dx = (pw - dw) / 2,     dy = (ph - dh) / 2;
                    g2.drawImage(bgImage, dx, dy, dw, dh, null);
                } else {
                    GradientPaint gp = new GradientPaint(
                            0, 0,            new Color(52, 38, 175),
                            getWidth(), getHeight(), new Color(195, 55, 155));
                    g2.setPaint(gp);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
            }
        };
        p.setOpaque(true);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;

        // ── Centre column ────────────────────────────────────────────────────
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setPreferredSize(new Dimension(370, 560));
        center.setMinimumSize(new Dimension(370, 560));
        center.setMaximumSize(new Dimension(370, 560));

        // ── Logo ─────────────────────────────────────────────────────────────
        final int MAX_H = 80;
        final int[] logoDim = new int[2];
        if (logoImage != null) {
            int iw = logoImage.getWidth(), ih = logoImage.getHeight();
            if (ih <= MAX_H) { logoDim[0] = iw;   logoDim[1] = ih;    }
            else             { logoDim[1] = MAX_H; logoDim[0] = (int)((double) iw / ih * MAX_H); }
        } else { logoDim[0] = MAX_H; logoDim[1] = MAX_H; }

        JPanel logoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
                int x = (getWidth()  - logoDim[0]) / 2;
                int y = (getHeight() - logoDim[1]) / 2;
                if (logoImage != null) {
                    g2.drawImage(logoImage, x, y, logoDim[0], logoDim[1], null);
                } else {
                    g2.setColor(new Color(130, 100, 230));
                    g2.fillOval(x, y, logoDim[0], logoDim[1]);
                    g2.setFont(new Font("Dialog", Font.BOLD, 28));
                    g2.setColor(Color.WHITE);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("S",
                            x + (logoDim[0] - fm.stringWidth("S")) / 2,
                            y + (logoDim[1] - fm.getHeight()) / 2 + fm.getAscent());
                }
                g2.dispose();
            }
        };
        logoPanel.setOpaque(false);
        Dimension logoPanelSize = new Dimension(logoDim[0] + 8, logoDim[1] + 8);
        logoPanel.setMinimumSize(logoPanelSize);
        logoPanel.setPreferredSize(logoPanelSize);
        logoPanel.setMaximumSize(logoPanelSize);
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLbl = new JLabel("Sign In", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Inter", Font.BOLD, 24));
        titleLbl.setForeground(new Color(240, 236, 255));
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createVerticalStrut(38));
        center.add(logoPanel);
        center.add(Box.createVerticalStrut(10));
        center.add(titleLbl);
        center.add(Box.createVerticalStrut(22));

        // ── Frosted card ─────────────────────────────────────────────────────
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 8, 55, 170));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(195, 175, 255, 90));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(28, 32, 28, 32));
        card.setMaximumSize(new Dimension(370, 999));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0;
        cc.fill  = GridBagConstraints.HORIZONTAL;
        cc.weightx = 1.0;

        // Username
        cc.gridy = 0; cc.insets = new Insets(0, 0, 4, 0);
        card.add(makeLabel("USERNAME"), cc);
        cc.gridy = 1; cc.insets = new Insets(0, 0, 12, 0);
        JTextField usernameField = makeStyledTextField("e.g. user123");
        card.add(usernameField, cc);

        // Password
        cc.gridy = 2; cc.insets = new Insets(0, 0, 4, 0);
        card.add(makeLabel("PASSWORD"), cc);
        cc.gridy = 3; cc.insets = new Insets(0, 0, 20, 0);
        String passwordPlaceholder = "••••••••";
        JPasswordField passwordField = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(12, 6, 45, 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        passwordField.setForeground(new Color(200, 180, 255));
        passwordField.setFont(new Font("Inter", Font.PLAIN, 13));
        passwordField.setOpaque(false);
        passwordField.setBackground(new Color(0, 0, 0, 0));
        passwordField.setCaretColor(new Color(200, 180, 255));
        passwordField.setBorder(new javax.swing.border.CompoundBorder(
            new javax.swing.border.AbstractBorder() {
                @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(180, 160, 255, 180));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(x, y, w-1, h-1, 10, 10);
                    g2.dispose();
                }
                @Override public Insets getBorderInsets(Component c) { return new Insets(1,1,1,1); }
            },
            new EmptyBorder(8, 10, 8, 10)
        ));
        passwordField.setPreferredSize(new Dimension(220, 38));
        passwordField.setEchoChar('\u2022');
        
        // Add placeholder support
        passwordField.setText(passwordPlaceholder);
        passwordField.setForeground(new Color(200, 180, 255)); // Brighter for visibility
        passwordField.setEchoChar((char) 0); // No echo for placeholder
        
        passwordField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (new String(passwordField.getPassword()).equals(passwordPlaceholder)) {
                    passwordField.setText("");
                    passwordField.setForeground(new Color(200, 180, 255));
                    passwordField.setEchoChar('\u2022'); // Show bullets for actual input
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (new String(passwordField.getPassword()).isEmpty()) {
                    passwordField.setText(passwordPlaceholder);
                    passwordField.setForeground(new Color(200, 180, 255)); // Keep bright
                    passwordField.setEchoChar((char) 0); // No echo for placeholder
                }
            }
        });
        
        card.add(passwordField, cc);

        // Sign in button
        cc.gridy = 4; cc.insets = new Insets(0, 0, 8, 0);
        JButton signInBtn = makePrimaryButton("Sign In");
        card.add(signInBtn, cc);

        // Create Account button
        cc.gridy = 5; cc.insets = new Insets(0, 0, 0, 0);
        JButton createAccountBtn = makeTertiaryButton("Create Account");
        card.add(createAccountBtn, cc);

        center.add(card);

        // ── Footer ───────────────────────────────────────────────────────────
        center.add(Box.createVerticalStrut(18));
        JLabel footer = new JLabel("Smart Parking & RFID Control System", SwingConstants.CENTER);
        footer.setFont(new Font("Inter", Font.PLAIN, 11));
        footer.setForeground(new Color(220, 210, 255, 140));
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(footer);

        // ── Actions ──────────────────────────────────────────────────────────
        signInBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            // treat placeholder text as empty
            if (username.equals("e.g. user123")) username = "";
            if (password.equals(passwordPlaceholder)) password = "";

            if (username.isEmpty() || password.isEmpty()) {
                DialogUtil.showMessageDialog(null, "Please enter both username and password.",
                        "Login Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                AuthenticationService authService = state.getAuthService();
                if (authService.authenticate(username, password)) {
                    UserAccount user = authService.getCurrentUser();

                    // Check if account is inactive
                    if (!user.isActive()) {
                        DialogUtil.showMessageDialog(null, "Your account has been deactivated.",
                                "Account Deactivated", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    state.setCurrentUser(user);
                    state.currentUsername = username;
                    state.currentRole = user.getRole();

                    // Log the login
                    try {
                        AuditLog loginLog = new AuditLog();
                        loginLog.setUserId(user.getUserId());
                        loginLog.setAction("LOGIN");
                        loginLog.setEntityType("USER");
                        loginLog.setEntityId(user.getUserId());
                        loginLog.setNewValue(username);
                        new AuditLogDAO().create(loginLog);
                    } catch (Exception auditEx) {
                        auditEx.printStackTrace();
                    }

                    // Check for default admin password — force change if needed
                    if ("ADMIN".equals(user.getRole()) && "admin".equals(username) && "admin123".equals(password)) {
                        cardLayout.show(rootPanel, "FORCE_CHANGE_PASSWORD");
                    } else if ("ADMIN".equals(user.getRole())) {
                        cardLayout.show(rootPanel, "ADMIN_DASHBOARD");
                    } else {
                        cardLayout.show(rootPanel, "USER_DASHBOARD");
                    }
                } else {
                    DialogUtil.showMessageDialog(null, "Invalid username or password.",
                            "Authentication Failed", JOptionPane.ERROR_MESSAGE);
                    resetToPlaceholder(usernameField, "e.g. user123");
                    passwordField.setText(passwordPlaceholder);
                    passwordField.setForeground(new Color(200, 190, 255, 200));
                }
            } catch (SQLException ex) {
                DialogUtil.showMessageDialog(null, "Database error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        // Create Account button
        createAccountBtn.addActionListener(e -> cardLayout.show(rootPanel, "USER_REGISTER"));

        // ── Enter key triggers sign-in from either field ─────────────────────
        java.awt.event.ActionListener enterAction = e -> signInBtn.doClick();
        usernameField.addActionListener(enterAction);
        passwordField.addActionListener(enterAction);

        // ── Clear credentials whenever this screen becomes visible ──────────
        p.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
                    && p.isShowing()) {
                resetToPlaceholder(usernameField, "e.g. user123");
                passwordField.setText(passwordPlaceholder);
                passwordField.setForeground(new Color(200, 190, 255, 200));
            }
        });

        p.add(center, gc);
        return p;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Resets a text field to its placeholder state cleanly — no ghost text. */
    static void resetToPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(new Color(200, 190, 255, 200));
        // Move caret to start so placeholder isn't shown selected
        field.setCaretPosition(0);
    }

    static JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Inter", Font.BOLD, 11));
        lbl.setForeground(new Color(210, 200, 255, 210));
        return lbl;
    }

    /**
     * Consistent styled text field — matches password field visually.
     * Uses custom paintComponent for rounded bg + border so there's no
     * opaque rectangle bleed-through (the ghost text artifact).
     */
    static JTextField makeStyledTextField(String placeholder) {
        JTextField field = new JTextField() {
            private boolean focused = false;
            {
                addFocusListener(new java.awt.event.FocusAdapter() {
                    @Override public void focusGained(java.awt.event.FocusEvent e) { focused = true;  repaint(); }
                    @Override public void focusLost (java.awt.event.FocusEvent e) { focused = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Rounded bg — matches password field
                g2.setColor(new Color(12, 6, 45, 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                // Border — brightens on focus
                g2.setColor(focused
                        ? new Color(180, 160, 255, 200)
                        : new Color(180, 160, 255, 180));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        field.setOpaque(false);
        field.setBackground(new Color(0, 0, 0, 0));
        field.setForeground(new Color(200, 190, 255, 200)); // placeholder colour
        field.setCaretColor(new Color(200, 180, 255));
        field.setFont(new Font("Inter", Font.PLAIN, 13));
        field.setBorder(new EmptyBorder(8, 10, 8, 10));
        field.setPreferredSize(new Dimension(220, 38));
        field.setText(placeholder);

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(new Color(200, 180, 255));
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(new Color(200, 190, 255, 200));
                }
            }
        });
        return field;
    }

    /** @deprecated Use makeStyledTextField() for text fields instead. */
    static JTextField makeField(String placeholder, boolean isPassword) {
        return makeStyledTextField(placeholder);
    }

    static JButton makePrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(120, 80, 220));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(220, 38));

        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                AbstractButton b = (AbstractButton) c;
                GradientPaint gp = b.getModel().isPressed() || b.getModel().isRollover()
                    ? new GradientPaint(0, 0, new Color(210, 50, 140), c.getWidth(), 0, new Color(127, 119, 221))
                    : new GradientPaint(0, 0, new Color(127, 119, 221), c.getWidth(), 0, new Color(210, 50, 140));
                
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 8, 8);
                
                g2.setColor(Color.WHITE);
                g2.setFont(b.getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (c.getWidth() - fm.stringWidth(b.getText())) / 2;
                int y = (c.getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(b.getText(), x, y);
                g2.dispose();
            }
        });

        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    static JButton makeTertiaryButton(String text) {
        // FIX: Removed the anonymous class override of isOpaque() which was breaking
        // Swing's mouse event hit-testing, making the button unclickable.
        // setOpaque(false) below achieves the same visual result safely.
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(new Color(127, 119, 221, 60));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(127, 119, 221, 40));
                } else {
                    g2.setColor(new Color(127, 119, 221, 20));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(160, 140, 220, 160));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(new Color(200, 185, 255));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
            // REMOVED: @Override public boolean isOpaque() { return false; }
            // This override confused Swing's repaint manager and broke mouse click dispatch.
        };
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setForeground(new Color(200, 185, 255));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false); // FIX: use the proper API instead of overriding isOpaque()
        btn.setEnabled(true); // FIX: explicitly ensure the button is enabled and interactive
        btn.setPreferredSize(new Dimension(220, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}