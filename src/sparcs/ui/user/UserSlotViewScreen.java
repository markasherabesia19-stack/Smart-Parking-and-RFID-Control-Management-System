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

        // LEFT: ⊞ icon + "SLOT VIEW"
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(UIFactory.lbl("\u229E", Font.BOLD, 20, C_ACCENT));
        titlePanel.add(UIFactory.lbl("SLOT VIEW", Font.BOLD, 20, C_WHITE));
        topBar.add(titlePanel, BorderLayout.WEST);

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
        mapCard.add(UIFactory.lbl("PARKING ZONES", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);

        // Legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        legend.setOpaque(false);
        legend.add(UIFactory.legendDot(C_AVAILABLE, "Available"));
        legend.add(UIFactory.legendDot(C_OCCUPIED,  "Occupied"));
        legend.add(UIFactory.legendDot(C_RESERVED,  "Reserved"));

        // south holds legend (fixed) + grid (replaced on reload)
        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.add(legend, BorderLayout.NORTH);
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
            statsRow.add(UIFactory.statCard("Available", String.valueOf(state.availableSlots), C_AVAILABLE));
            statsRow.add(UIFactory.statCard("Occupied",  String.valueOf(state.occupiedSlots),  C_OCCUPIED));
            statsRow.add(UIFactory.statCard("Reserved",  String.valueOf(state.reservedSlots),  C_RESERVED));

            if (south.getComponentCount() > 1) {
                south.remove(1);
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
}