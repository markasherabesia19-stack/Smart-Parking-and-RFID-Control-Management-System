package ui.user;

import dao.UserAccountDAO;
import model.UserAccount;
import util.PasswordUtil;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class UserRegisterScreen {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_-]+(\\.[A-Za-z0-9+_-]+)*"           // local part: no leading/trailing/consecutive dots
        + "@"                                                 // exactly one @
        + "[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?"          // first domain label, no leading/trailing hyphen
        + "(\\.[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)*"   // additional domain labels
        + "\\.[A-Za-z]{2,}$"                                  // TLD: at least 2 letters
    );

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel) {
        JPanel p = UIFactory.gradientPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;

        JPanel card = UIFactory.cardPanel(new GridBagLayout());
        card.setPreferredSize(new Dimension(360, 500));
        card.setBorder(new EmptyBorder(28, 32, 28, 32));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0; cc.fill = GridBagConstraints.HORIZONTAL;
        cc.insets = new Insets(4, 0, 2, 0);

        cc.gridy = 0;
        JLabel title = UIFactory.lbl("Create Account", Font.BOLD, 20, C_WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(title, cc);

        cc.gridy = 1; cc.insets = new Insets(0, 0, 14, 0);
        JLabel sub = UIFactory.lbl("Register as a vehicle owner", Font.PLAIN, 12, C_MUTED);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(sub, cc);

        String[] labels = {"FIRST NAME", "LAST NAME", "USERNAME", "EMAIL", "PASSWORD", "CONFIRM PASSWORD"};
        JTextField[] fields   = new JTextField[labels.length - 2];
        JPasswordField[] pfs  = new JPasswordField[2];

        for (int i = 0; i < labels.length; i++) {
            cc.gridy = 2 + i * 2; cc.insets = new Insets(6, 0, 2, 0);
            card.add(UIFactory.lbl(labels[i], Font.BOLD, 10, C_MUTED), cc);
            cc.gridy = 3 + i * 2; cc.insets = new Insets(0, 0, 0, 0);
            if (i < labels.length - 2) {
                fields[i] = UIFactory.styledField("");
                card.add(fields[i], cc);
            } else {
                pfs[i - (labels.length - 2)] = UIFactory.styledPasswordField("");
                card.add(pfs[i - (labels.length - 2)], cc);
            }
        }

        cc.gridy = 2 + labels.length * 2; cc.insets = new Insets(18, 0, 6, 0);
        JButton registerBtn = UIFactory.gradientButton("REGISTER");
        card.add(registerBtn, cc);

        cc.gridy++; cc.insets = new Insets(0, 0, 0, 0);
        JButton backBtn = UIFactory.outlineButton("BACK TO LOGIN");
        card.add(backBtn, cc);

        // fields[0] = First Name, fields[1] = Last Name, fields[2] = Username, fields[3] = Email
        // pfs[0] = Password, pfs[1] = Confirm Password
        registerBtn.addActionListener(e -> {
            String firstName    = fields[0].getText().trim();
            String lastName     = fields[1].getText().trim();
            String username     = fields[2].getText().trim();
            String email        = fields[3].getText().trim();
            String password     = new String(pfs[0].getPassword());
            String confirmPass  = new String(pfs[1].getPassword());

            // All fields required
            if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty()
                    || email.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please fill in all fields.",
                        "Registration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Valid email format
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                JOptionPane.showMessageDialog(null,
                        "Please enter a valid email address.\n" +
                        "Example: john.doe@example.com\n\n" +
                        "• Must contain exactly one @\n" +
                        "• No spaces or consecutive dots (e.g. john..doe is invalid)\n" +
                        "• Domain must have a valid extension (e.g. .com, .org)",
                        "Invalid Email", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Passwords match
            if (!password.equals(confirmPass)) {
                JOptionPane.showMessageDialog(null, "Passwords do not match.",
                        "Registration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Password length
            if (password.length() < 5) {
                JOptionPane.showMessageDialog(null, "Password must be at least 5 characters.",
                        "Registration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Database checks & save
            try {
                UserAccountDAO dao = new UserAccountDAO();

                // Check duplicate username
                if (dao.existsByUsername(username)) {
                    JOptionPane.showMessageDialog(null, "Username is already taken. Please choose another.",
                            "Registration Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Check duplicate email
                if (dao.existsByEmail(email)) {
                    JOptionPane.showMessageDialog(null, "Email is already registered. Please use a different email.",
                            "Registration Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                UserAccount newUser = new UserAccount();
                newUser.setUsername(username);
                newUser.setPasswordHash(PasswordUtil.hashPassword(password));
                newUser.setEmail(email);
                newUser.setRole("USER");
                newUser.setActive(true);

                dao.create(newUser);

                JOptionPane.showMessageDialog(null, "Account created successfully! Please sign in.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);

                // Clear fields
                for (JTextField f : fields) f.setText("");
                for (JPasswordField pf : pfs) pf.setText("");

                cardLayout.show(rootPanel, "USER_LOGIN");

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Database error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "USER_LOGIN"));

        p.add(card, gc);
        return p;
    }
}