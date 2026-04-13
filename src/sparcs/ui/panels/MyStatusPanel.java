package sparcs.ui.panels;

import sparcs.ui.components.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MyStatusPanel extends JPanel {

    private final String username;

    public MyStatusPanel(String username) {
        this.username = username;
        setLayout(new BorderLayout());
        setOpaque(false);

        GradientPanel bg = new GradientPanel();
        bg.setLayout(new BorderLayout());
        bg.setBorder(new EmptyBorder(20, 24, 20, 24));
        add(bg, BorderLayout.CENTER);

        JPanel inner = new JPanel(new BorderLayout(0, 16));
        inner.setOpaque(false);
        bg.add(inner, BorderLayout.CENTER);

        inner.add(new PageHeader("MY STATUS"), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(2, 1, 0, 14));
        body.setOpaque(false);

        // --- Top status card ---
        CardPanel statusCard = new CardPanel();
        statusCard.setLayout(new BorderLayout(0, 12));
        statusCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        // User row: avatar + name + ID + status badge
        JPanel userRow = new JPanel(new BorderLayout());
        userRow.setOpaque(false);

        JPanel avatarAndName = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        avatarAndName.setOpaque(false);

        // Avatar circle
        JLabel avatar = new JLabel(getInitials(username), SwingConstants.CENTER);
        avatar.setFont(SPARCSTheme.boldFont(20));
        avatar.setForeground(Color.WHITE);
        avatar.setBackground(SPARCSTheme.ACCENT_PURPLE);
        avatar.setOpaque(true);
        avatar.setPreferredSize(new Dimension(56, 56));
        avatar.setBorder(BorderFactory.createLineBorder(SPARCSTheme.ACCENT_PURPLE, 28));

        JPanel nameBlock = new JPanel(new GridLayout(2, 1));
        nameBlock.setOpaque(false);
        JLabel nameLbl = new JLabel(username);
        nameLbl.setFont(SPARCSTheme.boldFont(20));
        nameLbl.setForeground(Color.WHITE);
        JLabel idLbl = new JLabel(""); // TODO: load user ID from SQL
        idLbl.setFont(SPARCSTheme.labelFont(12));
        idLbl.setForeground(SPARCSTheme.TEXT_MUTED);
        nameBlock.add(nameLbl);
        nameBlock.add(idLbl);

        avatarAndName.add(avatar);
        avatarAndName.add(nameBlock);
        userRow.add(avatarAndName, BorderLayout.WEST);

        // Status badge
        JLabel badge = new JLabel(""); // TODO: set from SQL (PARKED / NOT PARKED)
        badge.setFont(SPARCSTheme.boldFont(12));
        badge.setForeground(SPARCSTheme.ACCENT_CYAN);
        badge.setBorder(BorderFactory.createLineBorder(SPARCSTheme.ACCENT_CYAN, 1));
        badge.setBorder(new EmptyBorder(4, 12, 4, 12));
        userRow.add(badge, BorderLayout.EAST);

        statusCard.add(userRow, BorderLayout.NORTH);

        // Info fields grid
        JPanel infoGrid = new JPanel(new GridLayout(3, 2, 14, 10));
        infoGrid.setOpaque(false);
        infoGrid.add(readOnlyField("VEHICLE"));
        infoGrid.add(readOnlyField("CURRENT SLOT"));
        infoGrid.add(readOnlyField("TIME IN"));
        infoGrid.add(readOnlyField("DURATION"));
        infoGrid.add(readOnlyField("BALANCE"));
        infoGrid.add(readOnlyField("EST. FEE"));
        statusCard.add(infoGrid, BorderLayout.CENTER);

        // --- Bottom row: totals + vehicle details ---
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 14, 0));
        bottomRow.setOpaque(false);

        // Totals column
        JPanel totalsCol = new JPanel(new GridLayout(2, 1, 0, 14));
        totalsCol.setOpaque(false);
        totalsCol.add(readOnlyCard("TOTAL VISITS", ""));    // TODO: from SQL
        totalsCol.add(readOnlyCard("TOTAL SPENT",  ""));    // TODO: from SQL

        // Vehicle details card
        CardPanel vehCard = new CardPanel();
        vehCard.setLayout(new BorderLayout(0, 10));
        vehCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel vehTitle = new JLabel("VEHICLE DETAILS");
        vehTitle.setFont(SPARCSTheme.boldFont(16));
        vehTitle.setForeground(Color.WHITE);
        vehCard.add(vehTitle, BorderLayout.NORTH);

        JPanel vehGrid = new JPanel(new GridLayout(2, 2, 14, 10));
        vehGrid.setOpaque(false);
        vehGrid.add(readOnlyField("PLATE NUMBER"));
        vehGrid.add(readOnlyField("RFID TAG"));
        vehGrid.add(readOnlyField("VEHICLE TYPE"));
        vehGrid.add(readOnlyField("COLOR"));
        vehCard.add(vehGrid, BorderLayout.CENTER);

        bottomRow.add(totalsCol);
        bottomRow.add(vehCard);

        body.add(statusCard);
        body.add(bottomRow);
        inner.add(body, BorderLayout.CENTER);
    }

    /** Single field panel with label above a dark rounded box. */
    private JPanel readOnlyField(String label) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(SPARCSTheme.labelFont(10));
        lbl.setForeground(SPARCSTheme.TEXT_LABEL);

        JPanel box = new JPanel();
        box.setBackground(SPARCSTheme.FIELD_BG);
        box.setOpaque(true);
        box.setPreferredSize(new Dimension(0, 40));
        // TODO: add a label inside box and set value from SQL

        p.add(lbl, BorderLayout.NORTH);
        p.add(box, BorderLayout.CENTER);
        return p;
    }

    /** Mini card for total visits / total spent. */
    private CardPanel readOnlyCard(String label, String value) {
        CardPanel c = new CardPanel();
        c.setLayout(new BorderLayout(0, 6));
        c.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel lbl = new JLabel(label);
        lbl.setFont(SPARCSTheme.labelFont(10));
        lbl.setForeground(SPARCSTheme.TEXT_LABEL);

        JLabel val = new JLabel(value);
        val.setFont(SPARCSTheme.boldFont(28));
        val.setForeground(SPARCSTheme.ACCENT_CYAN);
        // TODO: set from SQL

        c.add(lbl, BorderLayout.NORTH);
        c.add(val, BorderLayout.CENTER);
        return c;
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
