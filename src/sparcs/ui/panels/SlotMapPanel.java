package sparcs.ui.panels;

import sparcs.ui.components.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SlotMapPanel extends JPanel {

    private final boolean isAdmin;

    public SlotMapPanel(boolean isAdmin) {
        this.isAdmin = isAdmin;
        setLayout(new BorderLayout());
        setOpaque(false);

        GradientPanel bg = new GradientPanel();
        bg.setLayout(new BorderLayout());
        bg.setBorder(new EmptyBorder(20, 24, 20, 24));
        add(bg, BorderLayout.CENTER);

        JPanel inner = new JPanel(new BorderLayout(0, 16));
        inner.setOpaque(false);
        bg.add(inner, BorderLayout.CENTER);

        String title = isAdmin ? "SLOT MAP" : "SLOT VIEW";
        inner.add(new PageHeader(title), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        inner.add(body, BorderLayout.CENTER);

        // Stat row
        JPanel statsRow = new JPanel(isAdmin
                ? new GridLayout(1, 3, 12, 0)
                : new GridLayout(1, 2, 12, 0));
        statsRow.setOpaque(false);

        statsRow.add(new StatCard("Available", "", "",
                SPARCSTheme.BORDER_GREEN, SPARCSTheme.ACCENT_GREEN));
        statsRow.add(new StatCard("Occupied", "", "",
                SPARCSTheme.BORDER_YELLOW, SPARCSTheme.ACCENT_YELLOW));
        if (isAdmin) {
            statsRow.add(new StatCard("Reserved", "", "",
                    SPARCSTheme.BORDER_RED, SPARCSTheme.ACCENT_RED));
        }
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        body.add(statsRow, BorderLayout.NORTH);

        // Slots / Parking Zones card
        CardPanel slotsCard = new CardPanel();
        slotsCard.setLayout(new BorderLayout());
        slotsCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        String cardTitle = isAdmin ? "SLOTS" : "PARKING ZONES";
        JLabel ct = new JLabel(cardTitle);
        ct.setFont(SPARCSTheme.boldFont(16));
        ct.setForeground(SPARCSTheme.ACCENT_PINK);
        // TODO: render slot grid from SQL data
        JPanel slotsBody = new JPanel();
        slotsBody.setOpaque(false);
        slotsCard.add(ct, BorderLayout.NORTH);
        slotsCard.add(slotsBody, BorderLayout.CENTER);

        body.add(slotsCard, BorderLayout.CENTER);
    }
}
