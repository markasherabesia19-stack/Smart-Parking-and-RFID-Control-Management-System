package ui.admin;

import model.AppState;
import ui.shared.SidebarPanel;
import ui.shared.SlotGridPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * SPARCS — Admin slot map screen.
 * TODO (back-end): Reload slotData from DB on screen entry.
 */
public class AdminSlotMapScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_SLOT_MAP"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("SLOT MAP", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));
        statsRow.add(UIFactory.statCard("Available", String.valueOf(state.availableSlots), C_AVAILABLE));
        statsRow.add(UIFactory.statCard("Occupied",  String.valueOf(state.occupiedSlots),  C_OCCUPIED));
        statsRow.add(UIFactory.statCard("Reserved",  String.valueOf(state.reservedSlots),  C_RESERVED));

        JPanel mapCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        mapCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        mapCard.add(UIFactory.lbl("SLOTS", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        legend.setOpaque(false);
        legend.add(UIFactory.legendDot(C_AVAILABLE, "Available"));
        legend.add(UIFactory.legendDot(C_OCCUPIED,  "Occupied"));
        legend.add(UIFactory.legendDot(C_RESERVED,  "Reserved"));

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(legend, BorderLayout.NORTH);
        south.add(SlotGridPanel.buildFullGrid(state, false), BorderLayout.CENTER);
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
        return root;
    }
}
