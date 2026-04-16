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
 * SPARCS — Admin vehicle list screen.
 * TODO (back-end): Replace mock table data with a live DB query.
 */
public class AdminVehiclesScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_VEHICLES"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("VEHICLES", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        JButton addBtn = new JButton("+ Add Vehicle");
        UIFactory.styleSmallBtn(addBtn);
        addBtn.addActionListener(e -> cardLayout.show(rootPanel, "ADMIN_REGISTER"));
        topBar.add(addBtn, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // Mock vehicle table
        String[] cols = {"Plate", "Owner", "RFID Tag", "Type", "Status"};
        Object[][] data = {
            {"ABC-1234", "Juan dela Cruz",   "RF001", "Sedan",  "Active"},
            {"XYZ-5678", "Maria Santos",     "RF002", "SUV",    "Active"},
            {"LMN-9012", "Pedro Reyes",      "RF003", "Pickup", "Suspended"},
            {"QRS-3456", "Ana Gonzales",     "RF004", "Sedan",  "Active"},
            {"TUV-7890", "Carlos Villanueva","RF005", "Van",    "Active"},
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
        body.setBorder(new EmptyBorder(20, 20, 20, 20));
        body.add(scroll, BorderLayout.CENTER);

        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private static void styleTable(JTable table) {
        table.setBackground(C_BG_CARD);
        table.setForeground(C_WHITE);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setRowHeight(32);
        table.setGridColor(new Color(60, 50, 100));
        table.setSelectionBackground(C_PURPLE);
        table.setSelectionForeground(C_WHITE);
        table.setOpaque(true);
        JTableHeader header = table.getTableHeader();
        header.setBackground(C_BG_PANEL);
        header.setForeground(C_MUTED);
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));
    }
}
