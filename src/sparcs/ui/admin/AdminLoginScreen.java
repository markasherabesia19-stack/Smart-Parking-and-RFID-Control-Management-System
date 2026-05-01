package ui.admin;

import model.AppState;
import model.UserAccount;
import model.AuditLog;
import service.AuthenticationService;
import dao.AuditLogDAO;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

/**
 * SPARCS — Admin login screen.
 * Styled to match UserLoginScreen: gradient background, logo above card,
 * frosted card, rounded transparent input fields, same button styles.
 */
public class AdminLoginScreen {

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
                URL u = AdminLoginScreen.class.getResource(path);
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

        JLabel titleLbl = new JLabel("Admin Portal", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Serif", Font.BOLD, 25));
        titleLbl.setForeground(Color.WHITE);
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
        JTextField usernameField = makeField("Enter your username", false);
        card.add(usernameField, cc);

        // Password
        cc.gridy = 2; cc.insets = new Insets(0, 0, 4, 0);
        card.add(makeLabel("PASSWORD"), cc);
        cc.gridy = 3; cc.insets = new Insets(0, 0, 20, 0);
        JTextField passwordField = makeField("Enter your password", true);
        card.add(passwordField, cc);

        // Sign in button
        cc.gridy = 4; cc.insets = new Insets(0, 0, 8, 0);
        JButton signInBtn = makePrimaryButton("Sign in as Admin");
        card.add(signInBtn, cc);

        // Back button
        cc.gridy = 5; cc.insets = new Insets(0, 0, 0, 0);
        JButton backBtn = makeTertiaryButton("Back to role picker");
        card.add(backBtn, cc);

        center.add(card);

        // ── Footer ───────────────────────────────────────────────────────────
        center.add(Box.createVerticalStrut(18));
        JLabel footer = new JLabel("Smart Parking & RFID Control System", SwingConstants.CENTER);
        footer.setFont(new Font("Dialog", Font.PLAIN, 11));
        footer.setForeground(new Color(220, 210, 255, 140));
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(footer);

        // ── Actions ──────────────────────────────────────────────────────────
        signInBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(((JPasswordField) passwordField).getPassword());

            // treat placeholder text as empty
            if (username.equals("Enter your username")) username = "";
            if (password.equals("Enter your password")) password = "";

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please enter both username and password.",
                        "Login Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                AuthenticationService authService = state.getAuthService();
                if (authService.authenticate(username, password)) {
                    UserAccount user = authService.getCurrentUser();
                    if ("ADMIN".equals(user.getRole())) {
                        state.setCurrentUser(user);
                        state.currentUsername = username;
                        state.currentRole = "ADMIN";

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

                        cardLayout.show(rootPanel, "ADMIN_DASHBOARD");
                    } else {
                        JOptionPane.showMessageDialog(null, "Access denied. Admin role required.",
                                "Authorization Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid username or password.",
                            "Authentication Failed", JOptionPane.ERROR_MESSAGE);
                    usernameField.setText("Enter your username");
                    usernameField.setForeground(new Color(185, 175, 255, 145));
                    passwordField.setText("Enter your password");
                    passwordField.setForeground(new Color(185, 175, 255, 145));
                    ((JPasswordField) passwordField).setEchoChar((char) 0);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Database error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        // ── Enter key triggers sign-in from either field ─────────────────────
        java.awt.event.ActionListener enterAction = e -> signInBtn.doClick();
        usernameField.addActionListener(enterAction);
        passwordField.addActionListener(enterAction);

        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "ROLE_PICKER"));

        p.add(center, gc);
        return p;
    }

    // ── Helpers (mirrors UserLoginScreen) ────────────────────────────────────

    static JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Dialog", Font.BOLD, 10));
        lbl.setForeground(new Color(210, 200, 255, 210));
        return lbl;
    }

    static JTextField makeField(String placeholder, boolean isPassword) {
        JTextField field = isPassword
            ? new JPasswordField() {
                @Override protected void paintComponent(Graphics g) {
                    paintFieldBackground(g, this);
                    super.paintComponent(g);
                }
              }
            : new JTextField() {
                @Override protected void paintComponent(Graphics g) {
                    paintFieldBackground(g, this);
                    super.paintComponent(g);
                }
              };

        field.setFont(new Font("Dialog", Font.PLAIN, 13));
        field.setCaretColor(Color.WHITE);
        field.setOpaque(false);
        field.setBackground(new Color(0, 0, 0, 0));
        field.setBorder(new CompoundBorder(
                new RoundedBorder(new Color(180, 160, 255, 110), 1, 10),
                new EmptyBorder(10, 13, 10, 13)));
        field.setText(placeholder);
        field.setForeground(new Color(185, 175, 255, 145));

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.WHITE);
                    if (isPassword) ((JPasswordField) field).setEchoChar('•');
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(new Color(185, 175, 255, 145));
                    if (isPassword) ((JPasswordField) field).setEchoChar((char) 0);
                }
            }
        });
        if (isPassword) ((JPasswordField) field).setEchoChar((char) 0);
        return field;
    }

    private static void paintFieldBackground(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(30, 15, 70, 140));
        g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 10, 10);
        g2.dispose();
    }

    static class RoundedBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final int   thickness;
        private final int   radius;
        RoundedBorder(Color color, int thickness, int radius) {
            this.color = color; this.thickness = thickness; this.radius = radius;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(thickness, thickness, thickness, thickness); }
        @Override public Insets getBorderInsets(Component c, Insets i) {
            i.set(thickness, thickness, thickness, thickness); return i;
        }
    }

    static JButton makePrimaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color top = getModel().isPressed() ? new Color(105, 80, 205) : new Color(150, 110, 240);
                Color bot = getModel().isPressed() ? new Color( 85, 60, 175) : new Color(115,  80, 215);
                g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bot));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 18));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Dialog", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(false); btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setBorder(new EmptyBorder(12, 0, 12, 0));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    static JButton makeTertiaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Dialog", Font.PLAIN, 12));
        btn.setForeground(new Color(190, 180, 255, 155));
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorder(new CompoundBorder(
                new RoundedBorder(new Color(175, 155, 255, 55), 1, 10),
                new EmptyBorder(9, 0, 9, 0)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}