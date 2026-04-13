package sparcs.ui.panels;

import sparcs.ui.components.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DashboardPanel extends JPanel {

    public DashboardPanel(boolean isAdmin) {
        setLayout(new BorderLayout());
        setOpaque(false);

        GradientPanel bg = new GradientPanel();
        bg.setLayout(new BorderLayout());
        bg.setBorder(new EmptyBorder(20, 24, 20, 24));
        add(bg, BorderLayout.CENTER);

        JPanel inner = new JPanel(new BorderLayout(0, 16));
        inner.setOpaque(false);
        bg.add(inner, BorderLayout.CENTER);

        // Header
        inner.add(new PageHeader("DASHBOARD"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        inner.add(body, BorderLayout.CENTER);

        // Stat cards row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);

        statsRow.add(new StatCard("Available Slots", "",  "of total",      SPARCSTheme.BORDER_GREEN,  SPARCSTheme.ACCENT_GREEN));
        statsRow.add(new StatCard("Occupied",        "",  "of total",      SPARCSTheme.BORDER_YELLOW, SPARCSTheme.ACCENT_YELLOW));
        statsRow.add(new StatCard("Revenue Today",   "",  "transactions",  SPARCSTheme.BORDER_BLUE,   SPARCSTheme.ACCENT_CYAN));
        statsRow.add(new StatCard("Pending Fees",    "",  "outstanding",   SPARCSTheme.BORDER_RED,    SPARCSTheme.ACCENT_RED));

        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        body.add(statsRow, BorderLayout.NORTH);

        // Bottom two panels
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 12, 0));
        bottomRow.setOpaque(false);

        // Zone Overview
        CardPanel zoneCard = new CardPanel();
        zoneCard.setLayout(new BorderLayout());
        zoneCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel zoneTitle = new JLabel("ZONE OVERVIEW");
        zoneTitle.setFont(SPARCSTheme.boldFont(16));
        zoneTitle.setForeground(SPARCSTheme.TEXT_WHITE);
        // TODO: populate with zone data from SQL
        JPanel zoneBody = new JPanel();
        zoneBody.setOpaque(false);
        zoneCard.add(zoneTitle, BorderLayout.NORTH);
        zoneCard.add(zoneBody, BorderLayout.CENTER);

        // Recent Activity
        CardPanel actCard = new CardPanel();
        actCard.setLayout(new BorderLayout());
        actCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel actTitle = new JLabel("RECENT ACTIVITY");
        actTitle.setFont(SPARCSTheme.boldFont(16));
        actTitle.setForeground(SPARCSTheme.TEXT_WHITE);
        // TODO: populate with activity log from SQL
        JPanel actBody = new JPanel();
        actBody.setOpaque(false);
        actCard.add(actTitle, BorderLayout.NORTH);
        actCard.add(actBody, BorderLayout.CENTER);

        bottomRow.add(zoneCard);
        bottomRow.add(actCard);
        body.add(bottomRow, BorderLayout.CENTER);
    }
}
