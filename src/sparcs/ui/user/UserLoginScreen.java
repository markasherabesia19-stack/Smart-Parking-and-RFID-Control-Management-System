package ui.user;

import model.AppState;
import model.UserAccount;
import service.AuthenticationService;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

/**
 * SPARCS — User login screen.
 * Validates credentials against the users table in database.
 */
public class UserLoginScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel p = UIFactory.backgroundImagePanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;

        JPanel card = UIFactory.cardPanel(new GridBagLayout());
        card.setPreferredSize(new Dimension(400, 400));
        card.setBorder(new EmptyBorder(30, 32, 30, 32));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0; cc.fill = GridBagConstraints.HORIZONTAL;
        cc.insets = new Insets(4, 0, 4, 0);

        cc.gridy = 0; card.add(UIFactory.logoPanel(44), cc);


        cc.gridy = 2; cc.insets = new Insets(0, 0, 18, 0);
        JLabel sub = UIFactory.lbl("Vehicle Owner Portal", Font.PLAIN, 12, C_MUTED);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(sub, cc);

        cc.gridy = 3; cc.insets = new Insets(4, 0, 2, 0);
        card.add(UIFactory.lbl("USERNAME", Font.BOLD, 10, C_MUTED), cc);
        cc.gridy = 4; cc.insets = new Insets(0, 0, 0, 0);
        JTextField usernameField = UIFactory.styledField("Enter username");
        card.add(usernameField, cc);

        cc.gridy = 5; cc.insets = new Insets(4, 0, 2, 0);
        card.add(UIFactory.lbl("PASSWORD", Font.BOLD, 10, C_MUTED), cc);
        cc.gridy = 6; cc.insets = new Insets(0, 0, 0, 0);
        JPasswordField passwordField = UIFactory.styledPasswordField("Enter password");
        card.add(passwordField, cc);

        cc.gridy = 7; cc.insets = new Insets(18, 0, 6, 0);
        JButton signInBtn = UIFactory.gradientButton("SIGN IN");
        card.add(signInBtn, cc);

        // Allow pressing Enter on either field to trigger sign in
        usernameField.addActionListener(e -> signInBtn.doClick());
        passwordField.addActionListener(e -> signInBtn.doClick());

        cc.gridy = 8; cc.insets = new Insets(0, 0, 4, 0);
        JButton registerBtn = UIFactory.outlineButton("CREATE NEW ACCOUNT");
        card.add(registerBtn, cc);

        cc.gridy = 9; cc.insets = new Insets(0, 0, 0, 0);
        JButton guestBtn = UIFactory.outlineButton("CONTINUE AS GUEST");
        card.add(guestBtn, cc);

        JButton backBtn = new JButton("<- Back to Role Picker");
        backBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        backBtn.setForeground(C_MUTED);
        backBtn.setBorderPainted(false); backBtn.setContentAreaFilled(false); backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "ROLE_PICKER"));
        cc.gridy = 10; cc.insets = new Insets(8, 0, 0, 0);
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
                        
                        // Check if user is regular user (not admin)
                        if ("USER".equals(user.getRole())) {
                            state.setCurrentUser(user);
                            state.currentUsername = username;
                            state.currentRole = "USER";
                            // Rebuild screens now that session is loaded
                            rootPanel.add(UserDashboardScreen.build(cardLayout, rootPanel, state), "USER_DASHBOARD");
                            rootPanel.add(UserRFIDCardScreen.build(cardLayout, rootPanel, state), "USER_RFID_CARD");
                            cardLayout.show(rootPanel, "USER_DASHBOARD");
                        } else if ("ADMIN".equals(user.getRole())) {
                            JOptionPane.showMessageDialog(null, "Please use the Admin portal to login.",
                                    "Access Denied", JOptionPane.ERROR_MESSAGE);
                            usernameField.setText("");
                            passwordField.setText("");
                        } else {
                            JOptionPane.showMessageDialog(null, "Unknown user role.",
                                    "Authorization Error", JOptionPane.ERROR_MESSAGE);
                            usernameField.setText("");
                            passwordField.setText("");
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Invalid username or password.",
                                "Authentication Failed", JOptionPane.ERROR_MESSAGE);
                        usernameField.setText("");
                        passwordField.setText("");
                        usernameField.requestFocus();
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(null, "Database error: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    usernameField.setText("");
                    passwordField.setText("");
                    ex.printStackTrace();
                }
            }
        });
        registerBtn.addActionListener(e -> cardLayout.show(rootPanel, "USER_REGISTER"));
        guestBtn.addActionListener(e -> {
            state.currentUsername = "Guest";
            cardLayout.show(rootPanel, "USER_DASHBOARD");
        });

        p.add(card, gc);
        return p;
    }
}