package ui.admin;
import model.AppState;
import model.UserAccount;
import model.AuditLog;
import service.AuthenticationService;
import dao.AuditLogDAO;
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
        card.setPreferredSize(new Dimension(320, 380));
        card.setBorder(new EmptyBorder(32, 32, 32, 32));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0;
        cc.insets = new Insets(4, 0, 4, 0);

        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.anchor = GridBagConstraints.CENTER;

        cc.gridy = 0; cc.insets = new Insets(8, 0, 6, 0);
        JLabel sub = UIFactory.lbl("Admin Portal", Font.BOLD, 20, C_ACCENT);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(sub, cc);

        cc.gridy = 1; cc.insets = new Insets(25, 0, 2, 0);
        card.add(UIFactory.lbl("USERNAME", Font.BOLD, 10, C_MUTED), cc);
        cc.gridy = 2; cc.insets = new Insets(0, 0, 0, 0);
        JTextField usernameField = UIFactory.styledField("Enter username");
        card.add(usernameField, cc);

        cc.gridy = 3; cc.insets = new Insets(6, 0, 2, 0);
        card.add(UIFactory.lbl("PASSWORD", Font.BOLD, 10, C_MUTED), cc);
        cc.gridy = 4; cc.insets = new Insets(0, 0, 0, 0);
        JPasswordField passwordField = UIFactory.styledPasswordField("Enter password");
        card.add(passwordField, cc);

        cc.gridy = 5; cc.insets = new Insets(10, 0, 4, 0);
        JButton signInBtn = UIFactory.gradientButton("SIGN IN AS ADMIN");
        card.add(signInBtn, cc);

        JButton backBtn = UIFactory.outlineButton("BACK TO ROLE PICKER");
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
                            
                            // Log admin login to audit log
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