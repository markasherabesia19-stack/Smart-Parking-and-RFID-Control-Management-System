package ui.shared;

import model.AppState;
import dao.UserAccountDAO;
import model.AuditLog;
import dao.AuditLogDAO;
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

/**
 * SPARCS — Force Change Password Screen (v1)
 * Shown to admin user logging in with default password (admin/admin123).
 * Cannot be skipped — no back button.
 * After successful change, navigates to ADMIN_DASHBOARD.
 */
public class ForceChangePasswordScreen {

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
                URL u = ForceChangePasswordScreen.class.getResource(path);
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

        JLabel titleLbl = new JLabel("Change Password", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Serif", Font.BOLD, 25));
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLbl = new JLabel("Required for first login", SwingConstants.CENTER);
        subtitleLbl.setFont(new Font("Dialog", Font.PLAIN, 11));
        subtitleLbl.setForeground(new Color(200, 180, 220));
        subtitleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createVerticalStrut(38));
        center.add(logoPanel);
        center.add(Box.createVerticalStrut(10));
        center.add(titleLbl);
        center.add(Box.createVerticalStrut(4));
        center.add(subtitleLbl);
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

        // New Password
        cc.gridy = 0; cc.insets = new Insets(0, 0, 4, 0);
        card.add(makeLabel("NEW PASSWORD"), cc);
        cc.gridy = 1; cc.insets = new Insets(0, 0, 12, 0);
        JPasswordField newPasswordField = makePasswordField("••••••••");
        card.add(newPasswordField, cc);

        // Confirm Password
        cc.gridy = 2; cc.insets = new Insets(0, 0, 4, 0);
        card.add(makeLabel("CONFIRM PASSWORD"), cc);
        cc.gridy = 3; cc.insets = new Insets(0, 0, 20, 0);
        JPasswordField confirmPasswordField = makePasswordField("••••••••");
        card.add(confirmPasswordField, cc);

        // Change Password button
        cc.gridy = 4; cc.insets = new Insets(0, 0, 0, 0);
        JButton changeBtn = makePrimaryButton("Change Password");
        card.add(changeBtn, cc);

        center.add(card);

        // ── Footer ───────────────────────────────────────────────────────────
        center.add(Box.createVerticalStrut(18));
        JLabel footer = new JLabel("Smart Parking & RFID Control System", SwingConstants.CENTER);
        footer.setFont(new Font("Dialog", Font.PLAIN, 11));
        footer.setForeground(new Color(220, 210, 255, 140));
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(footer);

        // ── Actions ──────────────────────────────────────────────────────────
        changeBtn.addActionListener(e -> {
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (newPassword.isEmpty()) {
                DialogUtil.showMessageDialog(null, "Please enter a new password.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                DialogUtil.showMessageDialog(null, "Passwords do not match.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                UserAccountDAO userDAO = new UserAccountDAO();
                int userId = state.getCurrentUserAccount().getUserId();

                // Update password in database
                String hashedPassword = PasswordUtil.hashPassword(newPassword);
                userDAO.updatePassword(userId, hashedPassword);

                // Log the password change
                try {
                    AuditLog auditLog = new AuditLog();
                    auditLog.setUserId(userId);
                    auditLog.setAction("PASSWORD_CHANGE");
                    auditLog.setEntityType("USER");
                    auditLog.setEntityId(userId);
                    new AuditLogDAO().create(auditLog);
                } catch (Exception auditEx) {
                    auditEx.printStackTrace();
                }

                DialogUtil.showMessageDialog(null, "Password changed successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);

                // Navigate to admin dashboard
                cardLayout.show(rootPanel, "ADMIN_DASHBOARD");

            } catch (SQLException ex) {
                DialogUtil.showMessageDialog(null, "Database error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        // ── Enter key triggers change from either field ─────────────────────
        java.awt.event.ActionListener enterAction = e -> changeBtn.doClick();
        newPasswordField.addActionListener(enterAction);
        confirmPasswordField.addActionListener(enterAction);

        p.add(center, gc);
        return p;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    static JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Dialog", Font.BOLD, 10));
        lbl.setForeground(new Color(210, 200, 255, 210));
        return lbl;
    }

    static JPasswordField makePasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setText(placeholder);
        field.setForeground(new Color(185, 175, 255, 145));
        field.setFont(new Font("Dialog", Font.PLAIN, 13));
        field.setBackground(new Color(25, 10, 60, 120));
        field.setCaretColor(new Color(200, 180, 255));
        field.setBorder(new EmptyBorder(8, 10, 8, 10));
        field.setPreferredSize(new Dimension(220, 38));
        field.setEchoChar((char) 0);

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (String.valueOf(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setForeground(new Color(200, 180, 255));
                    field.setEchoChar('\u2022');
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getPassword().length == 0) {
                    field.setText(placeholder);
                    field.setForeground(new Color(185, 175, 255, 145));
                    field.setEchoChar((char) 0);
                }
            }
        });

        return field;
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
}
