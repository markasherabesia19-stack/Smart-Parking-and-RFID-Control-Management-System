package ui.admin;

import model.AppState;
import ui.shared.SidebarPanel;
import ui.shared.SlotGridPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * SPARCS — Admin slot map screen.
 * Reloads slot data from DB every time the screen becomes visible,
 * AND immediately whenever state.notifySlotChange() is called
 * (e.g. right after a vehicle entry or exit).
 */
public class AdminSlotMapScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.setOpaque(true);
        root.setName("ADMIN_SLOT_MAP");
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_SLOT_MAP"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);
        content.setOpaque(true);

        // ── Top Bar ───────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("SLOT MAP", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        // ── Stats row ─────────────────────────────────────────────────────────
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));
        buildStatsRow(statsRow, state);

        // ── Map card ──────────────────────────────────────────────────────────
        JPanel mapCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        mapCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header row: "SLOTS" title left, legend right
        JPanel slotsHeader = new JPanel(new BorderLayout());
        slotsHeader.setOpaque(false);
        slotsHeader.add(UIFactory.lbl("SLOTS", Font.BOLD, 12, C_MUTED), BorderLayout.WEST);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        legend.setOpaque(false);
        legend.add(UIFactory.legendDot(C_AVAILABLE, "Available"));
        legend.add(UIFactory.legendDot(C_OCCUPIED,  "Occupied"));
        legend.add(UIFactory.legendDot(C_RESERVED,  "Reserved"));
        slotsHeader.add(legend, BorderLayout.EAST);

        mapCard.add(slotsHeader, BorderLayout.NORTH);

        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setOpaque(false);
        gridWrapper.add(SlotGridPanel.buildFullGrid(state, false), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(gridWrapper, BorderLayout.CENTER);
        mapCard.add(south, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 20, 20, 20));
        body.add(mapCard, BorderLayout.CENTER);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body, BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        // ── Shared redraw logic (slotData[] already up-to-date when called) ────
        Runnable redraw = () -> {
            statsRow.removeAll();
            buildStatsRow(statsRow, state);
            statsRow.revalidate();
            statsRow.repaint();

            gridWrapper.removeAll();
            gridWrapper.add(SlotGridPanel.buildFullGrid(state, false), BorderLayout.CENTER);
            gridWrapper.revalidate();
            gridWrapper.repaint();
        };

        // Path 1: user navigates to this screen — load from DB then redraw
        root.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                state.loadSlotDataFromDB();
                redraw.run();
            }
        });

        // Path 2: notifySlotChange() fired from AdminEntryExitScreen —
        // reload fresh data from DB first, then redraw
        state.addSlotChangeListener(() -> SwingUtilities.invokeLater(() -> {
            state.loadSlotDataFromDB();
            redraw.run();
        }));

        return root;
    }

    private static void buildStatsRow(JPanel statsRow, AppState state) {
        statsRow.add(accentStatCard("Available", String.valueOf(state.availableSlots), C_AVAILABLE));
        statsRow.add(accentStatCard("Occupied",  String.valueOf(state.occupiedSlots),  C_OCCUPIED));
        statsRow.add(accentStatCard("Reserved",  String.valueOf(state.reservedSlots),  C_RESERVED));
    }

    /**
     * Stat card with a thin colored accent bar on top matching the status color.
     * Layout: [accent bar 4px] / [value + label body]
     */
    private static JPanel accentStatCard(String label, String value, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Card background with rounded corners
                g2.setColor(C_BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                // Accent bar — top 3px, rounded only on top corners
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, getWidth(), 6, 10, 10);
                g2.fillRect(0, 3, getWidth(), 3); // square off bottom half of accent
                g2.dispose();
            }
        };
        card.setOpaque(false);

        // Body: value + label, padded below the accent bar
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 16, 14, 16));

        JLabel valLbl = UIFactory.lbl(value, Font.BOLD, 22, accentColor);
        valLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLbl = UIFactory.lbl(label.toUpperCase(), Font.BOLD, 11, C_MUTED);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        body.add(valLbl);
        body.add(Box.createVerticalStrut(4));
        body.add(nameLbl);

        card.add(Box.createVerticalStrut(4), BorderLayout.NORTH); // space for accent bar
        card.add(body, BorderLayout.CENTER);
        return card;
    }
}