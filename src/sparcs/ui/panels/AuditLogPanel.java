package sparcs.ui.panels;

import sparcs.ui.components.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;

public class AuditLogPanel extends JPanel {

    public AuditLogPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        GradientPanel bg = new GradientPanel();
        bg.setLayout(new BorderLayout());
        bg.setBorder(new EmptyBorder(20, 24, 20, 24));
        add(bg, BorderLayout.CENTER);

        JPanel inner = new JPanel(new BorderLayout(0, 16));
        inner.setOpaque(false);
        bg.add(inner, BorderLayout.CENTER);

        inner.add(new PageHeader("AUDIT LOG"), BorderLayout.NORTH);

        // System Events card
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel cardTitle = new JLabel("SYSTEM EVENTS");
        cardTitle.setFont(SPARCSTheme.boldFont(16));
        cardTitle.setForeground(SPARCSTheme.ACCENT_PINK);
        card.add(cardTitle, BorderLayout.NORTH);

        // Table for audit events
        String[] cols = {"Timestamp", "User", "Action", "Details"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        // TODO: populate from SQL audit_log table
        JTable table = buildTable(model);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);

        inner.add(card, BorderLayout.CENTER);
    }

    private JTable buildTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setBackground(SPARCSTheme.CARD_BG);
        t.setForeground(SPARCSTheme.TEXT_WHITE);
        t.setFont(SPARCSTheme.labelFont(13));
        t.setRowHeight(32);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.getTableHeader().setBackground(SPARCSTheme.CARD_BG);
        t.getTableHeader().setForeground(SPARCSTheme.TEXT_MUTED);
        t.getTableHeader().setFont(SPARCSTheme.labelFont(12));
        t.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
        t.setSelectionBackground(SPARCSTheme.SIDEBAR_ACTIVE);
        t.setSelectionForeground(SPARCSTheme.TEXT_WHITE);
        return t;
    }
}
