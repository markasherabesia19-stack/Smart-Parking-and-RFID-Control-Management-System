package ui.admin;

import model.AppState;
import ui.shared.SidebarPanel;
import ui.shared.SlotGridPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * SPARCS — Admin main dashboard.
 * TODO (back-end): Replace mock stat values and activity rows with live DB queries.
 */
public class AdminDashboardScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_DASHBOARD"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("DASHBOARD", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        JButton signOutBtn = new JButton("SIGN OUT");
        UIFactory.styleSmallBtn(signOutBtn);
        signOutBtn.addActionListener(e -> { state.clearSession(); cardLayout.show(rootPanel, "ROLE_PICKER"); });
        topBar.add(signOutBtn, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // Stats row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));
        statsRow.add(UIFactory.statCard("Available Slots", String.valueOf(state.availableSlots), C_AVAILABLE));
        statsRow.add(UIFactory.statCard("Occupied",        String.valueOf(state.occupiedSlots),  C_OCCUPIED));
        statsRow.add(UIFactory.statCard("Revenue Today",   "₱1,000",                            C_ACCENT));
        statsRow.add(UIFactory.statCard("Pending Fees",    "3",                                  C_RESERVED));

        // Body
        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 20, 20, 20));

        JPanel zoneCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        zoneCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        zoneCard.add(UIFactory.lbl("ZONE OVERVIEW", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);
        zoneCard.add(SlotGridPanel.buildMiniGrid(state, false), BorderLayout.CENTER);
        body.add(zoneCard);

        JPanel actCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        actCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        actCard.add(UIFactory.lbl("RECENT ACTIVITY", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);
        // TODO (back-end): Replace with live entry/exit log from DB
        JPanel actList = new JPanel();
        actList.setLayout(new BoxLayout(actList, BoxLayout.Y_AXIS));
        actList.setOpaque(false);
        String[][] acts = {
            {"ABC-1234", "Entry", "B-04", "Just now"},
            {"XYZ-5678", "Exit",  "A-12", "2 min ago"},
            {"LMN-9012", "Entry", "C-07", "5 min ago"},
            {"QRS-3456", "Exit",  "B-19", "8 min ago"},
        };
        for (String[] a : acts) actList.add(activityRow(a[0], a[1], a[2], a[3]));
        JScrollPane actScroll = new JScrollPane(actList);
        actScroll.setBorder(null); actScroll.setOpaque(false); actScroll.getViewport().setOpaque(false);
        actCard.add(actScroll, BorderLayout.CENTER);
        body.add(actCard);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body, BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private static JPanel activityRow(String plate, String action, String slot, String time) {
        JPanel row = new JPanel(new GridLayout(1, 4));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(6, 0, 6, 0));
        row.add(UIFactory.lbl(plate,  Font.BOLD,  12, C_WHITE));
        row.add(UIFactory.lbl(action, Font.PLAIN, 12, action.equals("Entry") ? C_AVAILABLE : C_OCCUPIED));
        row.add(UIFactory.lbl(slot,   Font.PLAIN, 12, C_MUTED));
        row.add(UIFactory.lbl(time,   Font.PLAIN, 11, C_MUTED));
        return row;
    }
}
