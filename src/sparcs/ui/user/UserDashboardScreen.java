package ui.user;

import model.AppState;
import ui.shared.SidebarPanel;
import ui.shared.SlotGridPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
 
public class UserDashboardScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "USER", "USER_DASHBOARD"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        String greeting = "HELLO, " + (state.currentUsername.isEmpty() ? "USER" : state.currentUsername.toUpperCase()) + "!";
        topBar.add(UIFactory.lbl(greeting, Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        JButton signOutBtn = new JButton("SIGN OUT");
        UIFactory.styleSmallBtn(signOutBtn);
        signOutBtn.addActionListener(e -> { state.clearSession(); cardLayout.show(rootPanel, "ROLE_PICKER"); });
        topBar.add(signOutBtn, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // Stats row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));
        statsRow.add(UIFactory.statCard("Current Slot",   "B-12",  C_ACCENT));
        statsRow.add(UIFactory.statCard("Duration",       "01:45", C_AVAILABLE));
        statsRow.add(UIFactory.statCard("Estimated Fee",  "₱50",   C_RESERVED));
        statsRow.add(UIFactory.statCard("Wallet Balance", "₱250",  C_PINK));

        // Body
        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 20, 20, 20));

        JPanel mapCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        mapCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        mapCard.add(UIFactory.lbl("PARKING MAP", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);
        mapCard.add(SlotGridPanel.buildMiniGrid(state, true), BorderLayout.CENTER);
        body.add(mapCard);

        JPanel actCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        actCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        actCard.add(UIFactory.lbl("RECENT ACTIVITY", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);

        JPanel actList = new JPanel();
        actList.setLayout(new BoxLayout(actList, BoxLayout.Y_AXIS));
        actList.setOpaque(false);
        // TODO (back-end): Load from user's parking history in DB
        String[][] acts = {
            {"Entry", "B-12", "Today 09:00 AM"},
            {"Exit",  "A-05", "Yesterday 06:30 PM"},
            {"Entry", "C-11", "Apr 9"},
        };
        for (String[] a : acts) {
            JPanel row = new JPanel(new GridLayout(1, 3));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(6, 0, 6, 0));
            Color ac = a[0].equals("Entry") ? C_AVAILABLE : C_OCCUPIED;
            row.add(UIFactory.lbl(a[0], Font.BOLD,  12, ac));
            row.add(UIFactory.lbl(a[1], Font.PLAIN, 12, C_WHITE));
            row.add(UIFactory.lbl(a[2], Font.PLAIN, 11, C_MUTED));
            actList.add(row);
        }
        actCard.add(actList, BorderLayout.CENTER);
        body.add(actCard);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body, BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }
}
