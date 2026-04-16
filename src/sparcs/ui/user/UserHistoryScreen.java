package ui.user;

import model.AppState;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;

/**
 * SPARCS — User parking history screen.
 * TODO (back-end): Query parking_log table filtered by current user's account ID.
 */
public class UserHistoryScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "USER", "USER_HISTORY"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("HISTORY", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        // Summary stats
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));
        statsRow.add(UIFactory.statCard("Total Sessions", "24",    C_ACCENT));
        statsRow.add(UIFactory.statCard("Total Hours",    "48h",   C_AVAILABLE));
        statsRow.add(UIFactory.statCard("Total Fees Paid","₱1,200",C_RESERVED));

        // History table
        String[] cols = {"Date", "Entry", "Exit", "Slot", "Duration", "Fee"};
        Object[][] data = {
            {"Apr 10, 2025", "09:00 AM", "11:20 AM", "B-12", "2h 20m", "₱70"},
            {"Apr 09, 2025", "02:00 PM", "03:40 PM", "C-11", "1h 40m", "₱50"},
            {"Apr 08, 2025", "08:30 AM", "06:00 PM", "A-05", "9h 30m", "₱150"},
            {"Apr 07, 2025", "10:15 AM", "12:00 PM", "B-03", "1h 45m", "₱50"},
            {"Apr 06, 2025", "07:00 AM", "08:30 AM", "D-07", "1h 30m", "₱30"},
            {"Apr 05, 2025", "01:00 PM", "04:00 PM", "C-02", "3h 00m", "₱70"},
        };

        JTable table = new JTable(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(C_BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 20, 20, 20));
        body.add(scroll, BorderLayout.CENTER);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body, BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private static void styleTable(JTable t) {
        t.setBackground(C_BG_CARD);
        t.setForeground(C_WHITE);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setRowHeight(32);
        t.setGridColor(new Color(60, 50, 100));
        t.setSelectionBackground(C_PURPLE);
        t.setSelectionForeground(C_WHITE);
        t.setOpaque(true);
        JTableHeader header = t.getTableHeader();
        header.setBackground(C_BG_PANEL);
        header.setForeground(C_MUTED);
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));
    }
}
