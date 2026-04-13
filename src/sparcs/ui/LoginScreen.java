package sparcs.ui;

import sparcs.ui.components.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Login screen – shared for Admin and User portals.
 * Admin shows "SIGN IN AS ADMIN" + policy note.
 * User shows SIGN IN / CREATE NEW ACCOUNT / CONTINUE AS GUEST.
 */
public class LoginScreen extends JFrame {

    private final boolean isAdmin;
    private SPARCSField     usernameField;
    private SPARCSPasswordField passwordField;

    public LoginScreen(boolean isAdmin) {
        this.isAdmin = isAdmin;
        setTitle("SPARCS – " + (isAdmin ? "Admin Login" : "User Login"));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
    }

    private JPanel buildContent() {
        GradientPanel root = new GradientPanel();
        root.setLayout(new GridBagLayout());

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        // Logo
        SPARCSLogo logo = new SPARCSLogo(80, true);
        logo.setAlignmentX(CENTER_ALIGNMENT);

        // Form card
        CardPanel card = new CardPanel(SPARCSTheme.CARD_BG, 20);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(30, 40, 30, 40));
        card.setPreferredSize(new Dimension(400, isAdmin ? 340 : 360));
        card.setMaximumSize(new Dimension(400, isAdmin ? 340 : 360));

        // USERNAME
        JLabel userLbl = fieldLabel("USERNAME");
        usernameField  = new SPARCSField(20);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        // PASSWORD
        JLabel passLbl = fieldLabel("PASSWORD");
        passwordField  = new SPARCSPasswordField(20);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        card.add(userLbl);
        card.add(Box.createVerticalStrut(6));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(16));
        card.add(passLbl);
        card.add(Box.createVerticalStrut(6));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(20));

        if (isAdmin) {
            SPARCSButton signInBtn = new SPARCSButton("SIGN IN AS ADMIN");
            signInBtn.setAlignmentX(CENTER_ALIGNMENT);
            signInBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            signInBtn.addActionListener(e -> handleAdminLogin());
            card.add(signInBtn);
            card.add(Box.createVerticalStrut(16));
            card.add(buildAdminPolicy());
        } else {
            SPARCSButton signInBtn = new SPARCSButton("SIGN IN");
            signInBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            signInBtn.addActionListener(e -> handleUserLogin());

            JLabel orLbl = new JLabel("— OR —", SwingConstants.CENTER);
            orLbl.setForeground(SPARCSTheme.TEXT_MUTED);
            orLbl.setFont(SPARCSTheme.labelFont(11));
            orLbl.setAlignmentX(CENTER_ALIGNMENT);

            SPARCSButton createBtn = new SPARCSButton("CREATE NEW ACCOUNT",
                    new Color(0x4A3580), new Color(0x5A45A0));
            createBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            createBtn.addActionListener(e -> handleCreateAccount());

            SPARCSButton guestBtn = new SPARCSButton("CONTINUE AS GUEST",
                    new Color(0x3A2870), new Color(0x4A3880));
            guestBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            guestBtn.addActionListener(e -> handleGuest());

            card.add(signInBtn);
            card.add(Box.createVerticalStrut(10));
            card.add(orLbl);
            card.add(Box.createVerticalStrut(10));
            card.add(createBtn);
            card.add(Box.createVerticalStrut(8));
            card.add(guestBtn);
        }

        // Back button
        JButton backBtn = new JButton("← Back");
        backBtn.setOpaque(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setForeground(SPARCSTheme.TEXT_MUTED);
        backBtn.setFont(SPARCSTheme.labelFont(12));
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> { new SplashScreen().setVisible(true); dispose(); });
        backBtn.setAlignmentX(CENTER_ALIGNMENT);

        center.add(logo);
        center.add(Box.createVerticalStrut(20));
        center.add(card);
        center.add(Box.createVerticalStrut(10));
        center.add(backBtn);

        root.add(center);
        return root;
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(SPARCSTheme.labelFont(12));
        lbl.setForeground(SPARCSTheme.TEXT_LABEL);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel buildAdminPolicy() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("ADMIN ACCESS POLICY");
        title.setFont(SPARCSTheme.boldFont(11));
        title.setForeground(SPARCSTheme.TEXT_WHITE);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel l1 = new JLabel("• An admin account is created directly in the MySQL database during system setup.");
        JLabel l2 = new JLabel("• Self-registration is disabled for admins to prevent unauthorized access.");
        for (JLabel l : new JLabel[]{l1, l2}) {
            l.setFont(SPARCSTheme.labelFont(10));
            l.setForeground(SPARCSTheme.TEXT_MUTED);
            l.setAlignmentX(LEFT_ALIGNMENT);
        }

        p.add(title);
        p.add(Box.createVerticalStrut(4));
        p.add(l1);
        p.add(l2);
        return p;
    }

    // ---- Action handlers (connect your SQL logic here) ----
    private void handleAdminLogin() {
        String user = usernameField.getText().trim();
        char[] pass = passwordField.getPassword();
        // TODO: validate against DB
        // On success:
        openDashboard(true, user);
    }

    private void handleUserLogin() {
        String user = usernameField.getText().trim();
        char[] pass = passwordField.getPassword();
        // TODO: validate against DB
        openDashboard(false, user);
    }

    private void handleCreateAccount() {
        // TODO: open registration dialog / screen
        JOptionPane.showMessageDialog(this, "Registration screen – connect to your SQL logic.");
    }

    private void handleGuest() {
        openDashboard(false, "Guest");
    }

    private void openDashboard(boolean admin, String username) {
        new DashboardFrame(admin, username).setVisible(true);
        dispose();
    }
}
