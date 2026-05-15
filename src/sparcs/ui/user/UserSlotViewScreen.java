package ui.user;

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
 * SPARCS — User slot view screen.
 * Shows the full parking map with available / occupied / reserved colours.
 * Reloads slot data from DB every time the screen becomes visible,
 * AND immediately whenever state.notifySlotChange() is called.
 */
public class UserSlotViewScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "USER", "USER_SLOT_VIEW"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        // ── Top bar ──────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));

        // LEFT: "SLOT VIEW"
        topBar.add(UIFactory.lbl("SLOT VIEW", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);

        // RIGHT: ● Live
        JPanel topBarRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topBarRight.setOpaque(false);
        topBarRight.add(UIFactory.lbl("\u25CF Live", Font.PLAIN, 12, C_AVAILABLE));
        topBar.add(topBarRight, BorderLayout.EAST);

        content.add(topBar, BorderLayout.NORTH);

        // ── Stats row (mutable — rebuilt on each reload) ─────────────────────
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));

        // ── Map card (grid swapped out on each reload) ───────────────────────
        JPanel mapCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        mapCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header row: title left, legend right
        JPanel zonesHeader = new JPanel(new BorderLayout());
        zonesHeader.setOpaque(false);
        zonesHeader.add(UIFactory.lbl("PARKING ZONES", Font.BOLD, 12, C_MUTED), BorderLayout.WEST);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        legend.setOpaque(false);
        legend.add(UIFactory.legendDot(C_AVAILABLE, "Available"));
        legend.add(UIFactory.legendDot(C_OCCUPIED,  "Occupied"));
        legend.add(UIFactory.legendDot(C_RESERVED,  "Reserved"));
        zonesHeader.add(legend, BorderLayout.EAST);

        mapCard.add(zonesHeader, BorderLayout.NORTH);

        // south holds grid (replaced on reload)
        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        mapCard.add(south, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 20, 20, 20));
        body.add(mapCard, BorderLayout.CENTER);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body,     BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        // ── Shared redraw logic ───────────────────────────────────────────────
        Runnable redraw = () -> {
            statsRow.removeAll();
            statsRow.add(accentStatCard("Available", String.valueOf(state.availableSlots), C_AVAILABLE));
            statsRow.add(accentStatCard("Occupied",  String.valueOf(state.occupiedSlots),  C_OCCUPIED));
            statsRow.add(accentStatCard("Reserved",  String.valueOf(state.reservedSlots),  C_RESERVED));

            if (south.getComponentCount() > 0) {
                south.remove(0);
            }
            south.add(SlotGridPanel.buildFullGrid(state, false), BorderLayout.CENTER);

            statsRow.revalidate();
            statsRow.repaint();
            south.revalidate();
            south.repaint();
        };

        root.addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) {
                state.loadSlotDataFromDB();
                redraw.run();
            }
        });

        state.addSlotChangeListener(() -> SwingUtilities.invokeLater(() -> {
            state.loadSlotDataFromDB();
            redraw.run();
        }));

        state.loadSlotDataFromDB();
        redraw.run();

        return root;
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
                g2.setColor(C_BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, getWidth(), 6, 10, 10);
                g2.fillRect(0, 3, getWidth(), 3);
                g2.dispose();
            }
        };
        card.setOpaque(false);

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

        card.add(Box.createVerticalStrut(4), BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }
}