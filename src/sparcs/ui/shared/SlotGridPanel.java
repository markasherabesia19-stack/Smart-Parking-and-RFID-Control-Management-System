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
 * buildMiniGrid        – compact 5×8 overview used in dashboards.
 * buildFullGrid        – labelled 5×8 grid used in slot-map screens.
 * buildRefreshableGrid – full grid that auto-refreshes on navigation AND
 *                        whenever state.notifySlotChange() is called
 *                        (e.g. after an entry/exit operation).
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
            String slotCode  = zone + "-" + String.format("%02d", num);
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
            cell.setToolTipText(slotCode + " — " + statusStr);
            cell.add(UIFactory.lbl(slotCode, Font.PLAIN, 9, c));
            grid.add(cell);
        }
        return grid;
    }

    /**
     * Returns a wrapper JPanel that holds a full grid and refreshes in two cases:
     *   1. When it becomes visible (navigation / addNotify).
     *   2. Immediately when state.notifySlotChange() is called from anywhere
     *      (e.g. right after a successful entry or exit in AdminEntryExitScreen).
     *
     * The listener is automatically unregistered when the panel is removed from
     * the component hierarchy to avoid memory leaks.
     *
     * @param state    shared AppState
     * @param userView true = show reserved slots as occupied (user-facing view)
     */
    public static JPanel buildRefreshableGrid(AppState state, boolean userView) {
        JPanel wrapper = new JPanel(new BorderLayout()) {

            private Runnable listener;

            @Override
            public void addNotify() {
                super.addNotify();
                // Register listener the first time this panel enters a container
                if (listener == null) {
                    listener = this::refresh;
                    state.addSlotChangeListener(listener);
                }
                refresh();
            }

            @Override
            public void removeNotify() {
                super.removeNotify();
                // Unregister when removed so there are no dangling references
                if (listener != null) {
                    state.removeSlotChangeListener(listener);
                    listener = null;
                }
            }

            @Override
            public void setVisible(boolean visible) {
                super.setVisible(visible);
                if (visible) refresh();
            }

            private void refresh() {
                // loadSlotDataFromDB() is already called by notifySlotChange();
                // only call it here for the navigation-triggered refresh path.
                if (!SwingUtilities.isEventDispatchThread()) {
                    SwingUtilities.invokeLater(this::rebuildGrid);
                } else {
                    rebuildGrid();
                }
            }

            private void rebuildGrid() {
                removeAll();
                add(buildFullGrid(state, userView), BorderLayout.CENTER);
                revalidate();
                repaint();
            }
        };
        wrapper.setOpaque(false);
        wrapper.add(buildFullGrid(state, userView), BorderLayout.CENTER);
        return wrapper;
    }

    private static Color slotColor(int status, boolean userView) {
        if (status == 0) return C_AVAILABLE;
        if (status == 1) return C_OCCUPIED;
        return userView ? C_OCCUPIED : C_RESERVED;
    }
}