package sparcs.ui.panels;

import sparcs.ui.components.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;

public class HistoryPanel extends JPanel {

    public HistoryPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        GradientPanel bg = new GradientPanel();
        bg.setLayout(new BorderLayout());
        bg.setBorder(new EmptyBorder(20, 24, 20, 24));
        add(bg, BorderLayout.CENTER);

        JPanel inner = new JPanel(new BorderLayout(0, 16));
        inner.setOpaque(false);
        bg.add(inner, BorderLayout.CENTER);

        inner.add(new PageHeader("HISTORY"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        inner.add(body, BorderLayout.CENTER);

        // Stat cards row
        JPanel statsRow = new JPanel(new GridLayout(1, 2, 12, 0));
        statsRow.setOpaque(false);
        statsRow.add(new StatCard("Current Balance",  "", "", SPARCSTheme.BORDER_BLUE,   SPARCSTheme.ACCENT_CYAN));
        statsRow.add(new StatCard("This Month Spent", "", "", SPARCSTheme.BORDER_YELLOW, SPARCSTheme.ACCENT_YELLOW));
        body.add(statsRow, BorderLayout.NORTH);

        // Recent Transactions card
        CardPanel txCard = new CardPanel();
        txCard.setLayout(new BorderLayout(0, 10));
        txCard.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel txTitle = new JLabel("RECENT TRANSACTIONS");
        txTitle.setFont(SPARCSTheme.boldFont(16));
        txTitle.setForeground(SPARCSTheme.ACCENT_PINK);
        txCard.add(txTitle, BorderLayout.NORTH);

        String[] cols = {"Date", "Time In", "Time Out", "Duration", "Slot", "Fee Paid"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        // TODO: populate from SQL transaction history for current user
        JTable table = buildTable(model);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        txCard.add(scroll, BorderLayout.CENTER);

        body.add(txCard, BorderLayout.CENTER);
    }

    private JTable buildTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setBackground(SPARCSTheme.CARD_BG);
        t.setForeground(SPARCSTheme.TEXT_WHITE);
        t.setFont(SPARCSTheme.labelFont(13));
        t.setRowHeight(34);
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
