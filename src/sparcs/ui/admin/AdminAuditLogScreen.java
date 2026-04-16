package ui.admin;

import model.AppState;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;

public class AdminAuditLogScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_AUDIT_LOG"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("AUDIT LOG", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        String[] cols = {"Timestamp", "Admin", "Action", "Detail"};
        Object[][] data = {
            {"2025-04-10 08:00", "admin1", "ENTRY",    "ABC-1234 -> B-04"},
            {"2025-04-10 08:15", "admin1", "EXIT",     "XYZ-5678 <- A-12"},
            {"2025-04-10 09:00", "admin2", "REGISTER", "New vehicle: LMN-9012"},
            {"2025-04-10 09:30", "admin1", "FEE",      "Collected P70 - ABC-1234"},
            {"2025-04-10 10:00", "admin2", "RESERVE",  "Slot C-01 reserved"},
            {"2025-04-10 10:45", "admin1", "LOGIN",    "Admin login"},
            {"2025-04-10 11:00", "admin2", "LOGOUT",   "Admin logout"},
        };

        JTable table = new JTable(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table.setBackground(C_BG_CARD); table.setForeground(C_WHITE);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setRowHeight(30); table.setGridColor(new Color(60, 50, 100));
        table.setSelectionBackground(C_PURPLE); table.setSelectionForeground(C_WHITE);
        JTableHeader header = table.getTableHeader();
        header.setBackground(C_BG_PANEL); header.setForeground(C_MUTED);
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false); scroll.getViewport().setBackground(C_BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 20, 20, 20));
        body.add(scroll, BorderLayout.CENTER);

        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }
}
