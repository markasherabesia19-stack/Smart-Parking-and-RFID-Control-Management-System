package sparcs.ui.panels;

import sparcs.ui.components.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;

public class VehiclesPanel extends JPanel {

    public VehiclesPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        GradientPanel bg = new GradientPanel();
        bg.setLayout(new BorderLayout());
        bg.setBorder(new EmptyBorder(20, 24, 20, 24));
        add(bg, BorderLayout.CENTER);

        JPanel inner = new JPanel(new BorderLayout(0, 16));
        inner.setOpaque(false);
        bg.add(inner, BorderLayout.CENTER);

        inner.add(new PageHeader("VEHICLES"), BorderLayout.NORTH);

        // Registry card
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Top bar: title + search
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JLabel cardTitle = new JLabel("VEHICLES REGISTRY");
        cardTitle.setFont(SPARCSTheme.boldFont(16));
        cardTitle.setForeground(SPARCSTheme.ACCENT_PINK);
        topBar.add(cardTitle, BorderLayout.WEST);

        SPARCSField searchField = new SPARCSField(18);
        searchField.setMaximumSize(new Dimension(200, 36));
        // Placeholder simulation
        searchField.setText("Search plate / owner");
        searchField.setForeground(SPARCSTheme.TEXT_MUTED);
        topBar.add(searchField, BorderLayout.EAST);

        card.add(topBar, BorderLayout.NORTH);

        // Table
        String[] cols = {"Owner", "Plate", "Vehicle", "RFID Tag", "Balance", "Status", ""};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        // TODO: populate rows from SQL
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
        t.setRowHeight(36);
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
