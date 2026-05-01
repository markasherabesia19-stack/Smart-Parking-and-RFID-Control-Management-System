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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminAuditLogScreen {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.setOpaque(true);
        root.setName("ADMIN_AUDIT_LOG");
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_AUDIT_LOG"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);
        content.setOpaque(true);

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("AUDIT LOG", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);

        // Refresh button
        JButton refreshBtn = UIFactory.outlineButton("REFRESH");
        refreshBtn.setPreferredSize(new Dimension(110, 32));
        topBar.add(refreshBtn, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // Table
        String[] cols = {"Timestamp", "User ID", "Action", "Entity Type", "Entity ID", "IP Address"};
        DefaultTableModel tableModel = new DefaultTableModel(new Object[0][cols.length], cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        table.setBackground(C_BG_CARD);
        table.setForeground(C_WHITE);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setRowHeight(30);
        table.setGridColor(new Color(60, 50, 100));
        table.setSelectionBackground(C_PURPLE);
        table.setSelectionForeground(C_WHITE);

        JTableHeader header = table.getTableHeader();
        header.setBackground(C_BG_PANEL);
        header.setForeground(C_MUTED);
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(140); // Timestamp
        table.getColumnModel().getColumn(1).setPreferredWidth(70);  // User ID
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Action
        table.getColumnModel().getColumn(3).setPreferredWidth(100); // Entity Type
        table.getColumnModel().getColumn(4).setPreferredWidth(70);  // Entity ID
        table.getColumnModel().getColumn(5).setPreferredWidth(110); // IP Address

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(true);
        scroll.getViewport().setBackground(C_BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(true);
        body.setBackground(C_BG_DARK);
        body.setBorder(new EmptyBorder(20, 20, 20, 20));
        body.add(scroll, BorderLayout.CENTER);

        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        // Load data
        refreshBtn.addActionListener(e -> loadLogs(tableModel));

        // Auto-load when screen becomes visible
        root.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadLogs(tableModel);
            }
        });

        loadLogs(tableModel);
        return root;
    }

    private static void loadLogs(DefaultTableModel model) {
        model.setRowCount(0);
        try {
            AuditLogDAO dao = new AuditLogDAO();
            List<AuditLog> logs = dao.findAll();

            if (logs.isEmpty()) {
                model.addRow(new Object[]{"No records found.", "", "", "", "", "", ""});
                return;
            }

            DateTimeFormatter fmt = DT_FMT;
            for (AuditLog log : logs) {
                String timestamp = log.getCreatedAt() != null
                        ? log.getCreatedAt().format(fmt) : "—";
                String userId    = log.getUserId()    != null ? String.valueOf(log.getUserId())   : "—";
                String action    = log.getAction()    != null ? log.getAction()                   : "—";
                String entity    = log.getEntityType()!= null ? log.getEntityType()               : "—";
                String entityId  = log.getEntityId()  != null ? String.valueOf(log.getEntityId()) : "—";
                String ip        = log.getIpAddress() != null ? log.getIpAddress()                : "—";

                model.addRow(new Object[]{timestamp, userId, action, entity, entityId, ip});
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            model.addRow(new Object[]{"Error loading logs: " + ex.getMessage(), "", "", "", "", "", ""});
        }
    }
}