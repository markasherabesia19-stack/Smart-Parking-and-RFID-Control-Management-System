package ui.admin;
import model.AppState;
import model.UserAccount;
import service.AuthenticationService;
import util.UIFactory;
import static util.UIConstants.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

public class AdminLoginScreen {
    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel p = UIFactory.backgroundImagePanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;

        JPanel card = UIFactory.cardPanel(new GridBagLayout());
        card.setPreferredSize(new Dimension(400, 400));
        card.setBorder(new EmptyBorder(32, 32, 32, 32));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0;
        cc.insets = new Insets(4, 0, 4, 0);

        // Logo
        cc.gridy = 0;
        cc.fill = GridBagConstraints.NONE;
        cc.weightx = 0;
        cc.anchor = GridBagConstraints.CENTER;
        cc.insets = new Insets(0, 0, 8, 0);
        card.add(UIFactory.logoPanel(90), cc);

        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.anchor = GridBagConstraints.CENTER;

        cc.gridy = 1; cc.insets = new Insets(8, 0, 2, 0);
        JLabel sub = UIFactory.lbl("Admin Portal", Font.PLAIN, 12, C_MUTED);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(sub, cc);

        cc.gridy = 2; cc.insets = new Insets(20, 0, 2, 0);
        card.add(UIFactory.lbl("USERNAME", Font.BOLD, 10, C_MUTED), cc);
        cc.gridy = 3; cc.insets = new Insets(0, 0, 0, 0);
        JTextField usernameField = UIFactory.styledField("Enter username");
        card.add(usernameField, cc);

        cc.gridy = 4; cc.insets = new Insets(6, 0, 2, 0);
        card.add(UIFactory.lbl("PASSWORD", Font.BOLD, 10, C_MUTED), cc);
        cc.gridy = 5; cc.insets = new Insets(0, 0, 0, 0);
        JPasswordField passwordField = UIFactory.styledPasswordField("Enter password");
        card.add(passwordField, cc);

        cc.gridy = 6; cc.insets = new Insets(10, 0, 4, 0);
        JButton signInBtn = UIFactory.gradientButton("SIGN IN AS ADMIN");
        card.add(signInBtn, cc);

        cc.gridy = 7; cc.insets = new Insets(0, 0, 0, 0);
        card.add(UIFactory.lbl("ADMIN ACCESS POLICY", Font.BOLD, 9, C_MUTED), cc);
        cc.gridy = 8;
        card.add(UIFactory.lbl("Admin accounts are created in the database.", Font.PLAIN, 9, new Color(100, 90, 140)), cc);
        cc.gridy = 9;
        card.add(UIFactory.lbl("Self-registration is disabled for admins.", Font.PLAIN, 9, new Color(100, 90, 140)), cc);

        JButton backBtn = new JButton("<- Back to Role Picker");
        backBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        backBtn.setForeground(C_MUTED);
        backBtn.setBorderPainted(false); backBtn.setContentAreaFilled(false); backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "ROLE_PICKER"));
        cc.gridy = 10; cc.insets = new Insets(4, 0, 0, 0);
        card.add(backBtn, cc);

        signInBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please enter both username and password.",
                        "Login Error", JOptionPane.ERROR_MESSAGE);
            } else {
                try {
                    AuthenticationService authService = state.getAuthService();
                    if (authService.authenticate(username, password)) {
                        UserAccount user = authService.getCurrentUser();

                        if ("ADMIN".equals(user.getRole())) {
                            state.setCurrentUser(user);
                            state.currentUsername = username;
                            state.currentRole = "ADMIN";
                            cardLayout.show(rootPanel, "ADMIN_DASHBOARD");
                        } else {
                            JOptionPane.showMessageDialog(null, "Access denied. Admin role required.",
                                    "Authorization Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Invalid username or password.",
                                "Authentication Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(null, "Database error: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        p.add(card, gc);
        return p;
    }
}