package ui.admin;

import model.AppState;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;

/**
 * SPARCS — Admin parking fees screen.
 * TODO (back-end): Load fee schedule from DB; allow live edits and save back.
 */
public class AdminFeesScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_FEES"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("FEES", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Rate table
        JPanel rateCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        rateCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        rateCard.add(UIFactory.lbl("RATE SCHEDULE", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);

        String[] cols = {"Duration", "Rate (₱)"};
        Object[][] data = {
            {"First hour",        "30"},
            {"Succeeding hours",  "20 / hr"},
            {"Overnight (8hrs+)", "150"},
            {"Lost ticket fee",   "500"},
        };
        JTable table = new JTable(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false); scroll.getViewport().setBackground(C_BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));
        rateCard.add(scroll, BorderLayout.CENTER);
        body.add(rateCard);

        // Pending fees panel
        JPanel pendingCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        pendingCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        pendingCard.add(UIFactory.lbl("PENDING FEES", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        String[][] pending = {
            {"ABC-1234", "₱70",  "2h 20m"},
            {"LMN-9012", "₱50",  "1h 40m"},
            {"TUV-7890", "₱150", "Overnight"},
        };
        for (String[] p : pending) {
            JPanel row = new JPanel(new GridLayout(1, 3));
            row.setOpaque(false); row.setBorder(new EmptyBorder(8, 0, 8, 0));
            row.add(UIFactory.lbl(p[0], Font.BOLD,  12, C_WHITE));
            row.add(UIFactory.lbl(p[1], Font.BOLD,  13, C_RESERVED));
            row.add(UIFactory.lbl(p[2], Font.PLAIN, 11, C_MUTED));
            list.add(row);
        }
        pendingCard.add(list, BorderLayout.CENTER);
        JButton collectBtn = UIFactory.gradientButton("COLLECT ALL FEES");
        collectBtn.addActionListener(e ->
            JOptionPane.showMessageDialog(null, "All pending fees collected.", "Success", JOptionPane.INFORMATION_MESSAGE));
        pendingCard.add(collectBtn, BorderLayout.SOUTH);
        body.add(pendingCard);

        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private static void styleTable(JTable t) {
        t.setBackground(C_BG_CARD); t.setForeground(C_WHITE);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setRowHeight(32); t.setGridColor(new Color(60, 50, 100));
        t.setSelectionBackground(C_PURPLE); t.setSelectionForeground(C_WHITE);
        JTableHeader h = t.getTableHeader();
        h.setBackground(C_BG_PANEL); h.setForeground(C_MUTED);
        h.setFont(new Font("SansSerif", Font.BOLD, 11));
        h.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));
    }
}
