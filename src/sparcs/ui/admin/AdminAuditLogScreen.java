package ui.admin;

import dao.AuditLogDAO;
import model.AppState;
import model.AuditLog;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

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
        
        // Load audit logs from database
        List<AuditLog> logs = new java.util.ArrayList<>();
        try {
            AuditLogDAO auditDAO = new AuditLogDAO();
            logs = auditDAO.findRecent(50);
        } catch (SQLException ex) {
            System.err.println("Error loading audit logs: " + ex.getMessage());
        }
        
        Object[][] data = new Object[logs.size()][4];
        for (int i = 0; i < logs.size(); i++) {
            AuditLog log = logs.get(i);
            data[i][0] = log.getCreatedAt() != null ? log.getCreatedAt().toString() : "";
            data[i][1] = "Admin";
            data[i][2] = log.getAction();
            data[i][3] = log.getChangesLog() != null ? log.getChangesLog().substring(0, Math.min(50, log.getChangesLog().length())) : "";
        }

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
