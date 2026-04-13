package sparcs.ui.panels;

import sparcs.ui.components.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;

public class ReportPanel extends JPanel {

    public ReportPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        GradientPanel bg = new GradientPanel();
        bg.setLayout(new BorderLayout());
        bg.setBorder(new EmptyBorder(20, 24, 20, 24));
        add(bg, BorderLayout.CENTER);

        JPanel inner = new JPanel(new BorderLayout(0, 16));
        inner.setOpaque(false);
        bg.add(inner, BorderLayout.CENTER);

        inner.add(new PageHeader("REPORT"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        inner.add(body, BorderLayout.CENTER);

        // Stat cards
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);
        statsRow.add(new StatCard("Today",       "", "revenue",      SPARCSTheme.BORDER_GREEN,  SPARCSTheme.ACCENT_GREEN));
        statsRow.add(new StatCard("This Week",   "", "revenue",      SPARCSTheme.BORDER_BLUE,   SPARCSTheme.ACCENT_CYAN));
        statsRow.add(new StatCard("This Month",  "", "transactions", SPARCSTheme.BORDER_YELLOW, SPARCSTheme.ACCENT_YELLOW));
        statsRow.add(new StatCard("Uncollected", "", "outstanding",  SPARCSTheme.BORDER_RED,    SPARCSTheme.ACCENT_RED));
        body.add(statsRow, BorderLayout.NORTH);

        // Bottom row: chart + top users
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 12, 0));
        bottomRow.setOpaque(false);

        // Weekly Revenue chart area
        CardPanel chartCard = new CardPanel();
        chartCard.setLayout(new BorderLayout(0, 10));
        chartCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel chartTitle = new JLabel("WEEKLY REVENUE");
        chartTitle.setFont(SPARCSTheme.boldFont(16));
        chartTitle.setForeground(SPARCSTheme.TEXT_WHITE);
        // TODO: embed a chart (e.g. JFreeChart) here using SQL data
        JPanel chartBody = new JPanel();
        chartBody.setOpaque(false);
        chartCard.add(chartTitle, BorderLayout.NORTH);
        chartCard.add(chartBody,  BorderLayout.CENTER);

        // Top Users table
        CardPanel usersCard = new CardPanel();
        usersCard.setLayout(new BorderLayout(0, 10));
        usersCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel usersTitle = new JLabel("TOP USERS THIS WEEK");
        usersTitle.setFont(SPARCSTheme.boldFont(16));
        usersTitle.setForeground(SPARCSTheme.TEXT_WHITE);

        String[] cols = {"Owner", "Visits", "Paid"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        // TODO: populate from SQL
        JTable table = buildTable(model);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        usersCard.add(usersTitle, BorderLayout.NORTH);
        usersCard.add(scroll,     BorderLayout.CENTER);

        bottomRow.add(chartCard);
        bottomRow.add(usersCard);
        body.add(bottomRow, BorderLayout.CENTER);
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
