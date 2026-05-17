package ui.user;

import model.AppState;
import model.UserAccount;
import dao.UserAccountDAO;
import dao.AuditLogDAO;
import model.AuditLog;
import util.PasswordUtil;
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

public class UserRegisterScreen {

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
                URL u = UserRegisterScreen.class.getResource(path);
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
        gc.anchor = GridBagConstraints.CENTER;

        // ── Centre column ────────────────────────────────────────────────────
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setPreferredSize(new Dimension(370, 660));
        center.setMinimumSize(new Dimension(370, 660));
        center.setMaximumSize(new Dimension(370, 660));

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

        JLabel titleLbl = new JLabel("Create Account", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Serif", Font.BOLD, 25));
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLbl = new JLabel("Register as a new user", SwingConstants.CENTER);
        subtitleLbl.setFont(new Font("Dialog", Font.PLAIN, 11));
        subtitleLbl.setForeground(new Color(200, 180, 220));
        subtitleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createVerticalStrut(28));
        center.add(logoPanel);
        center.add(Box.createVerticalStrut(8));
        center.add(titleLbl);
        center.add(Box.createVerticalStrut(4));
        center.add(subtitleLbl);
        center.add(Box.createVerticalStrut(16));

        // ── Frosted card ─────────────────────────────────────────────────────
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
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
        card.setBorder(new EmptyBorder(24, 32, 24, 32));
        card.setMaximumSize(new Dimension(370, 999));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0;
        cc.fill  = GridBagConstraints.HORIZONTAL;
        cc.weightx = 1.0;

        // Full Name
        cc.gridy = 0; cc.insets = new Insets(0, 0, 4, 0);
        card.add(makeLabel("FULL NAME"), cc);
        cc.gridy = 1; cc.insets = new Insets(0, 0, 12, 0);
        JTextField fullNameField = makeField("e.g. Juan dela Cruz");
        card.add(fullNameField, cc);

        // Email
        cc.gridy = 2; cc.insets = new Insets(0, 0, 4, 0);
        card.add(makeLabel("EMAIL"), cc);
        cc.gridy = 3; cc.insets = new Insets(0, 0, 12, 0);
        JTextField emailField = makeField("e.g. juan@email.com");
        card.add(emailField, cc);

        // Username
        cc.gridy = 4; cc.insets = new Insets(0, 0, 4, 0);
        card.add(makeLabel("USERNAME"), cc);
        cc.gridy = 5; cc.insets = new Insets(0, 0, 12, 0);
        JTextField usernameField = makeField("e.g. user123");
        card.add(usernameField, cc);

        // Password
        cc.gridy = 6; cc.insets = new Insets(0, 0, 4, 0);
        card.add(makeLabel("PASSWORD"), cc);
        cc.gridy = 7; cc.insets = new Insets(0, 0, 12, 0);
        JPasswordField passwordField = makePasswordField();
        card.add(passwordField, cc);

        // Confirm Password
        cc.gridy = 8; cc.insets = new Insets(0, 0, 4, 0);
        card.add(makeLabel("CONFIRM PASSWORD"), cc);
        cc.gridy = 9; cc.insets = new Insets(0, 0, 20, 0);
        JPasswordField confirmPasswordField = makePasswordField();
        card.add(confirmPasswordField, cc);

        // Register button
        cc.gridy = 10; cc.insets = new Insets(0, 0, 10, 0);
        JButton registerBtn = makePrimaryButton("Create Account");
        card.add(registerBtn, cc);

        // Back to Login button
        cc.gridy = 11; cc.insets = new Insets(0, 0, 0, 0);
        JButton backBtn = makeSecondaryButton("Back to Login");
        card.add(backBtn, cc);

        center.add(card);

        // ── Footer ───────────────────────────────────────────────────────────
        center.add(Box.createVerticalStrut(16));
        JLabel footer = new JLabel("Smart Parking & RFID Control System", SwingConstants.CENTER);
        footer.setFont(new Font("Dialog", Font.PLAIN, 11));
        footer.setForeground(new Color(220, 210, 255, 140));
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(footer);

        // ── Actions ──────────────────────────────────────────────────────────
        registerBtn.addActionListener(e -> {
            // Read raw values
            String rawFullName = fullNameField.getText().trim();
            String rawEmail    = emailField.getText().trim();
            String rawUsername = usernameField.getText().trim();
            String password    = new String(passwordField.getPassword());
            String confirm     = new String(confirmPasswordField.getPassword());

            // Strip placeholders into new effectively-final variables
            String cleanFullName = rawFullName.equals("e.g. Juan dela Cruz") ? "" : rawFullName;
            String cleanEmail    = rawEmail.equals("e.g. juan@email.com")    ? "" : rawEmail;
            String cleanUsername = rawUsername.equals("e.g. user123")        ? "" : rawUsername;

            // Validation
            if (cleanFullName.isEmpty() || cleanEmail.isEmpty() || cleanUsername.isEmpty() || password.isEmpty()) {
                DialogUtil.showMessageDialog(null, "Please fill in all fields.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                DialogUtil.showMessageDialog(null, "Please enter a valid email address.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (cleanUsername.length() < 4) {
                DialogUtil.showMessageDialog(null, "Username must be at least 4 characters.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (password.length() < 6) {
                DialogUtil.showMessageDialog(null, "Password must be at least 6 characters.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!password.equals(confirm)) {
                DialogUtil.showMessageDialog(null, "Passwords do not match.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                UserAccountDAO userDAO = new UserAccountDAO();

                if (userDAO.existsByUsername(cleanUsername)) {
                    DialogUtil.showMessageDialog(null, "Username is already taken. Please choose another.",
                            "Registration Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (userDAO.existsByEmail(cleanEmail)) {
                    DialogUtil.showMessageDialog(null, "An account with that email already exists.",
                            "Registration Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Build and save new user account
                UserAccount newUser = new UserAccount();
                newUser.setUsername(cleanUsername);
                newUser.setFullName(cleanFullName);
                newUser.setEmail(cleanEmail);
                newUser.setPasswordHash(PasswordUtil.hashPassword(password));
                newUser.setRole("USER");
                newUser.setActive(true);

                userDAO.create(newUser);

                // Audit log — fetch created user to get their generated ID
                try {
                    new UserAccountDAO().findByUsername(cleanUsername).ifPresent(created -> {
                        try {
                            AuditLog log = new AuditLog();
                            log.setUserId(created.getUserId());
                            log.setAction("REGISTER");
                            log.setEntityType("USER");
                            log.setEntityId(created.getUserId());
                            log.setNewValue(cleanUsername);
                            new AuditLogDAO().create(log);
                        } catch (Exception ignored) {}
                    });
                } catch (Exception auditEx) {
                    auditEx.printStackTrace();
                }

                DialogUtil.showMessageDialog(null,
                        "Account created successfully! You can now log in.",
                        "Registration Successful", JOptionPane.INFORMATION_MESSAGE);

                clearFields(fullNameField, emailField, usernameField, passwordField, confirmPasswordField);
                cardLayout.show(rootPanel, "UNIFIED_LOGIN");

            } catch (SQLException ex) {
                DialogUtil.showMessageDialog(null, "Database error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        // ── Back to Login ─────────────────────────────────────────────────────
        backBtn.addActionListener(e -> {
            clearFields(fullNameField, emailField, usernameField, passwordField, confirmPasswordField);
            cardLayout.show(rootPanel, "UNIFIED_LOGIN");
        });

        // Enter key on confirm field triggers register
        confirmPasswordField.addActionListener(e -> registerBtn.doClick());

        // ── Clear fields when screen becomes visible ──────────────────────────
        p.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
                    && p.isShowing()) {
                clearFields(fullNameField, emailField, usernameField, passwordField, confirmPasswordField);
            }
        });

        p.add(center, gc);
        return p;
    }

    // ── Helper: reset all fields to placeholder state ─────────────────────────
    private static void clearFields(JTextField fullName, JTextField email,
                                    JTextField username, JPasswordField pass,
                                    JPasswordField confirm) {
        fullName.setText("e.g. Juan dela Cruz");
        fullName.setForeground(new Color(185, 175, 255, 145));
        email.setText("e.g. juan@email.com");
        email.setForeground(new Color(185, 175, 255, 145));
        username.setText("e.g. user123");
        username.setForeground(new Color(185, 175, 255, 145));
        pass.setText("");
        confirm.setText("");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    static JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Dialog", Font.BOLD, 10));
        lbl.setForeground(new Color(210, 200, 255, 210));
        return lbl;
    }

    static JTextField makeField(String placeholder) {
        JTextField field = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 15, 70, 140));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        field.setOpaque(false);
        field.setBackground(new Color(0, 0, 0, 0));
        field.setText(placeholder);
        field.setForeground(new Color(185, 175, 255, 145));
        field.setFont(new Font("Dialog", Font.PLAIN, 13));
        field.setCaretColor(new Color(200, 180, 255));
        field.setBorder(new javax.swing.border.CompoundBorder(
            new javax.swing.border.AbstractBorder() {
                @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(180, 160, 255, 110));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
                    g2.dispose();
                }
                @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
            },
            new EmptyBorder(8, 10, 8, 10)
        ));
        field.setPreferredSize(new Dimension(220, 38));
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
                    field.setForeground(new Color(185, 175, 255, 145));
                }
            }
        });
        return field;
    }

    static JPasswordField makePasswordField() {
        JPasswordField field = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 15, 70, 140));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        field.setForeground(new Color(200, 180, 255));
        field.setFont(new Font("Dialog", Font.PLAIN, 13));
        field.setOpaque(false);
        field.setBackground(new Color(0, 0, 0, 0));
        field.setCaretColor(new Color(200, 180, 255));
        field.setBorder(new javax.swing.border.CompoundBorder(
            new javax.swing.border.AbstractBorder() {
                @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(180, 160, 255, 110));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
                    g2.dispose();
                }
                @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
            },
            new EmptyBorder(8, 10, 8, 10)
        ));
        field.setPreferredSize(new Dimension(220, 38));
        field.setEchoChar('\u2022');
        return field;
    }

    static JButton makePrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
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

    static JButton makeSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setForeground(new Color(200, 185, 255));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setPreferredSize(new Dimension(220, 38));
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                AbstractButton b = (AbstractButton) c;
                if (b.getModel().isPressed()) {
                    g2.setColor(new Color(127, 119, 221, 70));
                } else if (b.getModel().isRollover()) {
                    g2.setColor(new Color(127, 119, 221, 45));
                } else {
                    g2.setColor(new Color(127, 119, 221, 20));
                }
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 8, 8);
                g2.setColor(new Color(160, 140, 220, 170));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 8, 8);
                g2.setColor(new Color(200, 185, 255));
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
}