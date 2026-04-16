package ui.admin;

import model.AppState;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * SPARCS — Admin register vehicle screen.
 * TODO (back-end): INSERT new vehicle + owner record into DB on form submit.
 */
public class AdminRegisterScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_REGISTER"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("REGISTER VEHICLE", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        JPanel card = UIFactory.cardPanel(new GridBagLayout());
        card.setPreferredSize(new Dimension(420, 480));
        card.setBorder(new EmptyBorder(28, 32, 28, 32));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0; cc.fill = GridBagConstraints.HORIZONTAL;
        cc.insets = new Insets(6, 0, 2, 0);

        String[] labels = {"OWNER FULL NAME", "PLATE NUMBER", "RFID TAG ID", "VEHICLE TYPE", "CONTACT NUMBER"};
        JTextField[] fields = new JTextField[labels.length];

        for (int i = 0; i < labels.length; i++) {
            cc.gridy = i * 2;
            card.add(UIFactory.lbl(labels[i], Font.BOLD, 10, C_MUTED), cc);
            cc.gridy = i * 2 + 1; cc.insets = new Insets(0, 0, 4, 0);
            fields[i] = UIFactory.styledField("");
            card.add(fields[i], cc);
            cc.insets = new Insets(6, 0, 2, 0);
        }

        cc.gridy = labels.length * 2; cc.insets = new Insets(18, 0, 6, 0);
        JButton registerBtn = UIFactory.gradientButton("REGISTER VEHICLE");
        card.add(registerBtn, cc);

        cc.gridy++; cc.insets = new Insets(0, 0, 0, 0);
        JButton backBtn = UIFactory.outlineButton("BACK TO VEHICLES");
        card.add(backBtn, cc);

        registerBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(null, "Vehicle registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            cardLayout.show(rootPanel, "ADMIN_VEHICLES");
        });
        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "ADMIN_VEHICLES"));

        center.add(card, new GridBagConstraints());
        content.add(center, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }
}
