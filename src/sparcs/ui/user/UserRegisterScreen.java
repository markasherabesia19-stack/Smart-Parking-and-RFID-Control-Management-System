package ui.user;

import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * SPARCS — User self-registration screen.
 * TODO (back-end): INSERT new user record into DB on submit.
 */
public class UserRegisterScreen {

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

        registerBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(null, "Account created! Please sign in.", "Success", JOptionPane.INFORMATION_MESSAGE);
            cardLayout.show(rootPanel, "USER_LOGIN");
        });
        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "USER_LOGIN"));

        p.add(card, gc);
        return p;
    }
}
