package ui.shared;

import model.AppState;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * SPARCS — Reusable slot grid components.
 *
 * buildMiniGrid  – compact 5×8 overview used in dashboards.
 * buildFullGrid  – labelled 5×8 grid used in slot-map screens.
 */
public class SlotGridPanel {

    /** Compact mini grid (no labels). userView hides the "reserved" state. */
    public static JPanel buildMiniGrid(AppState state, boolean userView) {
        JPanel grid = new JPanel(new GridLayout(5, 8, 4, 4));
        grid.setOpaque(false);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            Color c = slotColor(state.slotData[i], userView);
            JPanel cell = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
                    g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 5, 5);
                    g2.setColor(c);
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 5, 5);
                    g2.dispose();
                }
                @Override public Dimension getPreferredSize() { return new Dimension(16, 14); }
            };
            cell.setOpaque(false);
            grid.add(cell);
        }
        return grid;
    }

    /** Full labelled grid with tooltips. userView hides the "reserved" state. */
    public static JPanel buildFullGrid(AppState state, boolean userView) {
        JPanel grid = new JPanel(new GridLayout(5, 8, 6, 6));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(10, 0, 0, 0));
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            Color c = slotColor(state.slotData[i], userView);
            char zone = (char) ('A' + i / 8);
            int  num  = (i % 8) + 1;
            String slotName = zone + "-" + String.format("%02d", num);
            String statusStr = state.slotData[i] == 0 ? "Available"
                             : state.slotData[i] == 1 ? "Occupied"
                             : "Reserved";

            JPanel cell = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40));
                    g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                    g2.setColor(c);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                    g2.dispose();
                }
            };
            cell.setOpaque(false);
            cell.setToolTipText(slotName + " — " + statusStr);
            cell.add(UIFactory.lbl(slotName, Font.PLAIN, 9, c));
            grid.add(cell);
        }
        return grid;
    }

    private static Color slotColor(int status, boolean userView) {
        if (status == 0) return C_AVAILABLE;
        if (status == 1) return C_OCCUPIED;
        return userView ? C_OCCUPIED : C_RESERVED;
    }
}