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

        // TODO (back-end): Load fee schedule from DB via FeeScheduleDAO
        String[] cols = {"Duration", "Rate (₱)"};
        Object[][] data = {}; // Fee schedule data will be populated from database
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

        // TODO (back-end): Load pending fees from DB via ParkingTransactionDAO
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        // Pending fees will be populated from database
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
