package ui.user;

import model.AppState;
import ui.shared.SidebarPanel;   // Fix: was missing, caused compile error
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * SPARCS — User "My Status" screen.
 * Shows the user's current parking session details.
 * TODO (back-end): Poll DB for active session tied to this user's RFID/account.
 */
public class UserMyStatusScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "USER", "USER_MY_STATUS"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("MY STATUS", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        JPanel card = UIFactory.cardPanel(new GridBagLayout());
        card.setPreferredSize(new Dimension(420, 360));
        card.setBorder(new EmptyBorder(28, 32, 28, 32));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0; cc.fill = GridBagConstraints.HORIZONTAL;
        cc.insets = new Insets(0, 0, 18, 0);

        // Status badge
        cc.gridy = 0;
        JLabel statusBadge = UIFactory.lbl("* CURRENTLY PARKED", Font.BOLD, 13, C_AVAILABLE);
        statusBadge.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(statusBadge, cc);

        // Detail rows
        String[][] details = {
            {"Slot",          " "},
            {"Zone",          " "},
            {"Entry Time",    " "},
            {"Duration",      " "},
            {"Estimated Fee", " "},
            {"Wallet",        " "},
        };
        for (String[] d : details) {
            cc.gridy++;
            cc.insets = new Insets(0, 0, 10, 0);
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.add(UIFactory.lbl(d[0], Font.PLAIN, 12, C_MUTED), BorderLayout.WEST);
            JLabel val = UIFactory.lbl(d[1], Font.BOLD, 13, C_WHITE);
            val.setHorizontalAlignment(SwingConstants.RIGHT);
            row.add(val, BorderLayout.EAST);
            card.add(row, cc);
        }

        cc.gridy++;
        cc.insets = new Insets(18, 0, 0, 0);
        JButton refreshBtn = UIFactory.gradientButton("REFRESH STATUS");
        refreshBtn.addActionListener(e ->
            JOptionPane.showMessageDialog(null, "Status refreshed. (TODO: re-query DB)", "Refresh", JOptionPane.INFORMATION_MESSAGE));
        card.add(refreshBtn, cc);

        center.add(card, new GridBagConstraints());
        content.add(center, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }
}