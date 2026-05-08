package ui.user;

import dao.UserAccountDAO;
import model.UserAccount;
import util.PasswordUtil;
import util.UIFactory;
import util.DialogUtil;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * SPARCS — User register screen (v5).
 * Fixes: logo no longer clipped, input fields have no opaque dark rectangle.
 */
public class UserRegisterScreen {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_-]+(\\.[A-Za-z0-9+_-]+)*"
        + "@"
        + "[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?"
        + "(\\.[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)*"
        + "\\.[A-Za-z]{2,}$"
    );

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel) {

        final BufferedImage bgImage   = UserLoginScreen.getBgImage();
        final BufferedImage logoImage = UserLoginScreen.getLogoImage();

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

        // ── Centre column — no fixed height so nothing gets clipped ──────────
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        // Width fixed to 430; height tall enough for logo (68) + title + card (~490) + padding
        center.setPreferredSize(new Dimension(430, 660));
        center.setMinimumSize(new Dimension(430, 660));
        center.setMaximumSize(new Dimension(430, 660));

        // ── Logo — drawn at natural aspect ratio ─────────────────────────────
        final int MAX_H = 68;
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
                    g2.setFont(new Font("Dialog", Font.BOLD, 24));
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

        JLabel titleLbl = new JLabel("Create account", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Serif", Font.BOLD, 24));
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createVerticalStrut(38));
        center.add(logoPanel);
        center.add(Box.createVerticalStrut(12));
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
        card.setBorder(new EmptyBorder(24, 28, 24, 28));
        card.setMaximumSize(new Dimension(430, 999));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        GridBagConstraints cc = new GridBagConstraints();
        cc.fill = GridBagConstraints.HORIZONTAL; cc.weightx = 1.0;

        // ── First + Last name side by side ───────────────────────────────────
        cc.gridx = 0; cc.gridy = 0; cc.gridwidth = 2; cc.insets = new Insets(0, 0, 12, 0);
        JPanel nameRow = new JPanel(new GridLayout(1, 2, 10, 0));
        nameRow.setOpaque(false);

        JPanel firstCol = new JPanel(new BorderLayout(0, 5)); firstCol.setOpaque(false);
        firstCol.add(makeLabel("FIRST NAME"), BorderLayout.NORTH);
        JTextField firstNameField = makeField("Juan", false);
        firstCol.add(firstNameField, BorderLayout.CENTER);

        JPanel lastCol = new JPanel(new BorderLayout(0, 5)); lastCol.setOpaque(false);
        lastCol.add(makeLabel("LAST NAME"), BorderLayout.NORTH);
        JTextField lastNameField = makeField("dela Cruz", false);
        lastCol.add(lastNameField, BorderLayout.CENTER);

        nameRow.add(firstCol); nameRow.add(lastCol);
        card.add(nameRow, cc);

        // ── Username ─────────────────────────────────────────────────────────
        cc.gridy = 1; cc.insets = new Insets(0, 0, 5, 0);
        card.add(makeLabel("USERNAME"), cc);
        cc.gridy = 2; cc.insets = new Insets(0, 0, 12, 0);
        JTextField usernameField = makeField("juandelacruz", false);
        card.add(usernameField, cc);

        // ── Email ─────────────────────────────────────────────────────────────
        cc.gridy = 3; cc.insets = new Insets(0, 0, 5, 0);
        card.add(makeLabel("EMAIL"), cc);
        cc.gridy = 4; cc.insets = new Insets(0, 0, 12, 0);
        JTextField emailField = makeField("juan@email.com", false);
        card.add(emailField, cc);

        // ── Password + Confirm side by side ──────────────────────────────────
        cc.gridy = 5; cc.insets = new Insets(0, 0, 22, 0);
        JPanel passRow = new JPanel(new GridLayout(1, 2, 10, 0));
        passRow.setOpaque(false);

        JPanel passCol = new JPanel(new BorderLayout(0, 5)); passCol.setOpaque(false);
        passCol.add(makeLabel("PASSWORD"), BorderLayout.NORTH);
        JPasswordField passwordField = (JPasswordField) makeField("••••••••", true);
        passCol.add(passwordField, BorderLayout.CENTER);

        JPanel confirmCol = new JPanel(new BorderLayout(0, 5)); confirmCol.setOpaque(false);
        confirmCol.add(makeLabel("CONFIRM"), BorderLayout.NORTH);
        JPasswordField confirmField = (JPasswordField) makeField("••••••••", true);
        confirmCol.add(confirmField, BorderLayout.CENTER);

        passRow.add(passCol); passRow.add(confirmCol);
        card.add(passRow, cc);

        // ── Buttons ───────────────────────────────────────────────────────────
        cc.gridy = 6; cc.insets = new Insets(0, 0, 9, 0);
        JButton registerBtn = makePrimaryButton("Register");
        card.add(registerBtn, cc);

        cc.gridy = 7; cc.insets = new Insets(0, 0, 0, 0);
        JButton backBtn = makeSecondaryButton("Back to login");
        card.add(backBtn, cc);

        center.add(card);
        center.add(Box.createVerticalStrut(18));

        JLabel footer = new JLabel("Smart Parking & RFID Control System", SwingConstants.CENTER);
        footer.setFont(new Font("Dialog", Font.PLAIN, 11));
        footer.setForeground(new Color(220, 215, 255, 120));
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(footer);
        center.add(Box.createVerticalStrut(20));

        // ── Actions ───────────────────────────────────────────────────────────
        registerBtn.addActionListener(e -> {
            String firstName   = firstNameField.getText().trim();
            String lastName    = lastNameField.getText().trim();
            String username    = usernameField.getText().trim();
            String email       = emailField.getText().trim();
            String password    = new String(passwordField.getPassword());
            String confirmPass = new String(confirmField.getPassword());

            if (firstName.equals("Juan"))        firstName = "";
            if (lastName.equals("dela Cruz"))    lastName = "";
            if (username.equals("juandelacruz")) username = "";
            if (email.equals("juan@email.com"))  email = "";

            if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty()
                    || email.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
                DialogUtil.showMessageDialog(null, "Please fill in all fields.",
                        "Registration Error", JOptionPane.ERROR_MESSAGE); return;
            }
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                DialogUtil.showMessageDialog(null,
                        "Please enter a valid email address.\nExample: john.doe@example.com",
                        "Invalid Email", JOptionPane.ERROR_MESSAGE); return;
            }
            if (!password.equals(confirmPass)) {
                DialogUtil.showMessageDialog(null, "Passwords do not match.",
                        "Registration Error", JOptionPane.ERROR_MESSAGE); return;
            }
            if (password.length() < 5) {
                DialogUtil.showMessageDialog(null, "Password must be at least 5 characters.",
                        "Registration Error", JOptionPane.ERROR_MESSAGE); return;
            }
            try {
                UserAccountDAO dao = new UserAccountDAO();
                if (dao.existsByUsername(username)) {
                    DialogUtil.showMessageDialog(null, "Username is already taken.",
                            "Registration Error", JOptionPane.ERROR_MESSAGE); return;
                }
                if (dao.existsByEmail(email)) {
                    DialogUtil.showMessageDialog(null, "Email is already registered.",
                            "Registration Error", JOptionPane.ERROR_MESSAGE); return;
                }
                UserAccount newUser = new UserAccount();
                newUser.setUsername(username);
                newUser.setPasswordHash(PasswordUtil.hashPassword(password));
                newUser.setEmail(email);
                newUser.setFullName(firstName + " " + lastName);
                newUser.setRole("USER");
                newUser.setActive(true);
                dao.create(newUser);
                DialogUtil.showMessageDialog(null,
                        "Account created successfully! Please sign in.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                Color ph = new Color(185, 175, 255, 145);
                firstNameField.setText("Juan");        firstNameField.setForeground(ph);
                lastNameField.setText("dela Cruz");    lastNameField.setForeground(ph);
                usernameField.setText("juandelacruz"); usernameField.setForeground(ph);
                emailField.setText("juan@email.com");  emailField.setForeground(ph);
                passwordField.setText(""); confirmField.setText("");
                cardLayout.show(rootPanel, "USER_LOGIN");
            } catch (SQLException ex) {
                DialogUtil.showMessageDialog(null, "Database error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "USER_LOGIN"));

        p.add(center, gc);
        return p;
    }

    // ── Delegate to UserLoginScreen helpers for consistent style ─────────────
    private static JLabel     makeLabel(String t)             { return UserLoginScreen.makeLabel(t); }
    private static JTextField makeField(String p, boolean pw) { return UserLoginScreen.makeField(p, pw); }
    private static JButton    makePrimaryButton(String t)     { return UserLoginScreen.makePrimaryButton(t); }
    private static JButton    makeSecondaryButton(String t)   { return UserLoginScreen.makeSecondaryButton(t); }
}