package ui.admin;

import dao.VehicleDAO;
import dao.UserAccountDAO;
import dao.VehicleOwnerDAO;
import dao.RFIDMappingDAO;
import dao.ParkingTransactionDAO;
import model.AppState;
import model.Vehicle;
import model.UserAccount;
import model.VehicleOwner;
import model.RFIDMapping;
import model.ParkingTransaction;
import ui.shared.SidebarPanel;
import util.UIFactory;
import util.DialogUtil;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Optional;

public class AdminVehiclesScreen {

    // ── Column indices ────────────────────────────────────────────────────────
    private static final int COL_PLATE  = 0;
    private static final int COL_OWNER  = 1;
    private static final int COL_USER   = 2;
    private static final int COL_TYPE   = 3;
    private static final int COL_COLOR  = 4;
    private static final int COL_STATUS = 5;
    private static final int COL_DELETE = 6;
    private static final int COL_ID     = 7;   // hidden — vehicle_id

    // ── Design System: Row colors ─────────────────────────────────────────────
    // Aligned to UIConstants layer hierarchy
    private static final Color ROW_ODD      = new Color(13, 11, 31);       // Layer 0 — #0D0B1F
    private static final Color ROW_EVEN     = new Color(18, 16, 58);       // Layer 1 — #12103A
    private static final Color ROW_SELECTED = new Color(36, 30, 107);      // Layer 3 — #241E6B
    private static final Color ROW_HOVER    = new Color(26, 22, 80);       // Layer 2 — #1A1650

    // ── Design System: Status badge colors ───────────────────────────────────
    // Exact values from SPARCS Design System doc
    // Parked  → Emerald Green  #1DB954
    // Inactive / Suspended → Amber Orange  #FF8C42
    // Not Parked → muted purple
    private static final Color PARKED_BG  = new Color(29, 185, 84, 38);   // #1DB954 @ 15%
    private static final Color PARKED_FG  = new Color(29, 185, 84);        // #1DB954
    private static final Color PARKED_BD  = new Color(29, 185, 84, 80);

    private static final Color NOTPKD_BG  = new Color(124, 92, 191, 46);  // #7C5CBF @ 18%
    private static final Color NOTPKD_FG  = new Color(155, 143, 212);      // #9B8FD4
    private static final Color NOTPKD_BD  = new Color(107, 95, 160, 60);

    // "Inactive" maps to Amber Orange — not red
    private static final Color INACT_BG   = new Color(255, 140, 66, 38);  // #FF8C42 @ 15%
    private static final Color INACT_FG   = new Color(255, 140, 66);       // #FF8C42
    private static final Color INACT_BD   = new Color(255, 140, 66, 80);

    // "Suspended" maps to Crimson Red — deliberate danger signal
    private static final Color SUSPND_BG  = new Color(232, 54, 93, 38);   // #E8365D @ 15%
    private static final Color SUSPND_FG  = new Color(232, 54, 93);        // #E8365D
    private static final Color SUSPND_BD  = new Color(232, 54, 93, 80);

    // ── Design System: Plate number color ────────────────────────────────────
    // Lighter purple so it reads as a code/ID, distinct from body text
    private static final Color PLATE_FG   = new Color(167, 139, 250);      // #A78BFA

    // ── Design System: Misc ───────────────────────────────────────────────────
    private static final Color SEPARATOR   = new Color(30, 28, 69, 255);   // #1E1C45
    private static final Color CARD_BD     = new Color(45, 40, 96, 120);   // #2D2860 @ ~47%
    private static final Color HEADER_FG   = new Color(107, 95, 160);      // #6B5FA0 — muted label

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ROW_ODD);
        root.setOpaque(true);
        root.setName("ADMIN_VEHICLES");
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_VEHICLES"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(ROW_ODD);
        content.setOpaque(true);

        // ── Top Bar ───────────────────────────────────────────────────────────
        // Height fixed at 54px; background is Layer 2 (#1A1650) per design system
        JPanel topBar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(ROW_HOVER);                        // #1A1650
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(SEPARATOR);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(10, 28, 10, 24));
        topBar.setPreferredSize(new Dimension(0, 54));

        // Page title — 18px Bold ALL CAPS, color #F0ECFF
        JLabel titleLbl = UIFactory.lbl("VEHICLES", Font.BOLD, 18, new Color(240, 236, 255));
        topBar.add(titleLbl, BorderLayout.WEST);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topRight.setOpaque(false);

        // ── Search field — uses design system input spec ──────────────────────
        // bg #0D0B1F, border 1.5px #2D2860, focus border #7C5CBF, text #C4BFED
        final String PLACEHOLDER = "Search vehicles...";
        JTextField searchField = new JTextField() {
            private boolean focused = false;
            {
                addFocusListener(new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                    @Override public void focusLost(FocusEvent e)   { focused = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ROW_ODD);                           // input bg #0D0B1F
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(focused
                    ? new Color(124, 92, 191)                   // focus: #7C5CBF
                    : new Color(45, 40, 96));                   // default: #2D2860
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        searchField.setOpaque(false);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        searchField.setForeground(new Color(107, 95, 160));    // placeholder: #6B5FA0
        searchField.setCaretColor(new Color(196, 191, 237));   // caret: #C4BFED
        searchField.setPreferredSize(new Dimension(200, 34));
        searchField.setBorder(new EmptyBorder(4, 10, 4, 10));
        searchField.setText(PLACEHOLDER);

        searchField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (searchField.getText().equals(PLACEHOLDER)) {
                    searchField.setText("");
                    searchField.setForeground(new Color(196, 191, 237)); // body text: #C4BFED
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (searchField.getText().isBlank()) {
                    searchField.setText(PLACEHOLDER);
                    searchField.setForeground(new Color(107, 95, 160)); // muted: #6B5FA0
                }
            }
        });

        // ── Filter dropdown — same paint style as search field ────────────────
        String[] filterOptions = {"All", "Plate", "Owner", "Username", "Type", "Color", "Parking Status"};
        JComboBox<String> filterBox = buildFilterCombo(filterOptions);

        // ── Status filter ─────────────────────────────────────────────────────
        String[] statusOptions = {"All Status", "Parked", "Not Parked", "Inactive", "Suspended"};
        JComboBox<String> statusBox = buildFilterCombo(statusOptions);

        // ── Add Vehicle button — solid accent purple per design system ─────────
        // Uses UIFactory.gradientButton for purple→pink gradient (primary CTA)
        JButton addBtn = UIFactory.gradientButton("+ Add Vehicle");
        addBtn.setPreferredSize(new Dimension(130, 34));
        addBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        addBtn.addActionListener(e -> cardLayout.show(rootPanel, "ADMIN_REGISTER"));

        topRight.add(searchField);
        topRight.add(filterBox);
        topRight.add(statusBox);
        topRight.add(addBtn);
        topBar.add(topRight, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // ── Table model ───────────────────────────────────────────────────────
        String[] cols = {"Plate", "Owner", "Username", "Type", "Color", "Parking Status", "", "id"};

        final int[] hoveredRow = {-1};

        DefaultTableModel tableModel = new DefaultTableModel(new Object[0][8], cols) {
            @Override public boolean isCellEditable(int r, int c) { return c == COL_DELETE; }
            @Override public Class<?> getColumnClass(int c) {
                return c == COL_DELETE ? JButton.class : Object.class;
            }
        };

        JTable table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                boolean selected = isRowSelected(row);

                if (col != COL_DELETE && col != COL_STATUS) {
                    if (selected) {
                        c.setBackground(ROW_SELECTED);
                    } else if (row == hoveredRow[0]) {
                        c.setBackground(ROW_HOVER);
                    } else {
                        c.setBackground(row % 2 == 0 ? ROW_ODD : ROW_EVEN);
                    }
                }

                // Plate number — monospace, lighter purple #A78BFA
                if (col == COL_PLATE) {
                    c.setForeground(PLATE_FG);
                    c.setFont(new Font("Monospaced", Font.BOLD, 12));
                    if (c instanceof JLabel lbl) lbl.setBorder(new EmptyBorder(0, 14, 0, 0));

                // Username — muted purple #9B8FD4
                } else if (col == COL_USER) {
                    c.setForeground(new Color(155, 143, 212));
                    c.setFont(new Font("SansSerif", Font.PLAIN, 12));

                // Status and delete handled by custom renderers
                } else if (col != COL_STATUS && col != COL_DELETE) {
                    c.setForeground(new Color(196, 191, 237));  // body text #C4BFED
                    c.setFont(new Font("SansSerif", Font.PLAIN, 13));
                }
                return c;
            }
        };

        // Hover tracking
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != hoveredRow[0]) {
                    hoveredRow[0] = row;
                    table.repaint();
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                hoveredRow[0] = -1;
                table.repaint();
            }
        });

        styleTable(table);

        // ── Status badge renderer ─────────────────────────────────────────────
        // Draws a pill badge with colored bg + dot + text per design system spec.
        // Each status maps to its exact design system color — no improvisation.
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                final String status = value != null ? value.toString() : "Not Parked";

                return new JPanel() {
                    { setOpaque(true); }
                    @Override protected void paintComponent(Graphics g) {
                        // Row background — must match prepareRenderer logic exactly
                        boolean sel = t.isRowSelected(row);
                        Color bg = sel ? ROW_SELECTED
                                       : (row == hoveredRow[0] ? ROW_HOVER
                                       : (row % 2 == 0 ? ROW_ODD : ROW_EVEN));
                        g.setColor(bg);
                        g.fillRect(0, 0, getWidth(), getHeight());

                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        // Pick badge colors from design system
                        Color pillBg, pillFg, pillBd;
                        switch (status) {
                            case "Parked" -> {
                                pillBg = PARKED_BG; pillFg = PARKED_FG; pillBd = PARKED_BD;
                            }
                            case "Inactive" -> {
                                pillBg = INACT_BG; pillFg = INACT_FG; pillBd = INACT_BD;
                            }
                            case "Suspended" -> {
                                pillBg = SUSPND_BG; pillFg = SUSPND_FG; pillBd = SUSPND_BD;
                            }
                            default -> {  // "Not Parked"
                                pillBg = NOTPKD_BG; pillFg = NOTPKD_FG; pillBd = NOTPKD_BD;
                            }
                        }

                        Font pillFont = new Font("SansSerif", Font.BOLD, 11);
                        g2.setFont(pillFont);
                        FontMetrics fm = g2.getFontMetrics(pillFont);

                        // Badge geometry — 6px border-radius, 22px tall, padding 4px 10px
                        int dotDiam = 6;
                        int gap     = 5;
                        int tw      = fm.stringWidth(status);
                        int ph      = 22;
                        int pw      = dotDiam + gap + tw + 22; // 11px left pad + dot + gap + text + 11px right pad
                        int px      = 14;
                        int py      = (getHeight() - ph) / 2;
                        int arc     = 6;

                        // Pill background + border
                        g2.setColor(pillBg);
                        g2.fillRoundRect(px, py, pw, ph, arc, arc);
                        g2.setColor(pillBd);
                        g2.setStroke(new BasicStroke(1f));
                        g2.drawRoundRect(px, py, pw - 1, ph - 1, arc, arc);

                        // Dot
                        int dotX = px + 11;
                        int dotY = py + (ph - dotDiam) / 2;
                        g2.setColor(pillFg);
                        g2.fillOval(dotX, dotY, dotDiam, dotDiam);

                        // Text
                        int textX = dotX + dotDiam + gap;
                        int textY = py + (ph - fm.getHeight()) / 2 + fm.getAscent();
                        g2.setColor(pillFg);
                        g2.drawString(status, textX, textY);

                        g2.dispose();
                    }
                };
            }
        });

        // ── Color column renderer — shows a small colored dot beside the color name ──
        table.getColumnModel().getColumn(COL_COLOR).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                final String colorName = value != null ? value.toString() : "";

                return new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)) {
                    { setOpaque(true); }
                    @Override protected void paintComponent(Graphics g) {
                        boolean sel = t.isRowSelected(row);
                        Color bg = sel ? ROW_SELECTED
                                       : (row == hoveredRow[0] ? ROW_HOVER
                                       : (row % 2 == 0 ? ROW_ODD : ROW_EVEN));
                        g.setColor(bg);
                        g.fillRect(0, 0, getWidth(), getHeight());

                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        // Resolve a representational dot color from the string
                        Color dot = resolveColorDot(colorName);
                        int dotSize = 10;
                        int dotX = 14;
                        int dotY = (getHeight() - dotSize) / 2;
                        g2.setColor(dot);
                        g2.fillOval(dotX, dotY, dotSize, dotSize);
                        // Subtle ring so white/light dots are visible
                        g2.setColor(new Color(255, 255, 255, 30));
                        g2.setStroke(new BasicStroke(1f));
                        g2.drawOval(dotX, dotY, dotSize - 1, dotSize - 1);

                        // Color name text — body text #C4BFED
                        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
                        g2.setColor(new Color(196, 191, 237));
                        FontMetrics fm = g2.getFontMetrics();
                        int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                        g2.drawString(colorName, dotX + dotSize + 6, textY);

                        g2.dispose();
                    }

                    private Color resolveColorDot(String name) {
                        if (name == null) return new Color(100, 100, 120);
                        return switch (name.toLowerCase()) {
                            case "blue"   -> new Color(59, 130, 246);
                            case "red"    -> new Color(239, 68, 68);
                            case "green"  -> new Color(34, 197, 94);
                            case "yellow" -> new Color(234, 179, 8);
                            case "white"  -> new Color(229, 231, 235);
                            case "black"  -> new Color(30, 30, 40);
                            case "gray","grey"   -> new Color(107, 114, 128);
                            case "pink"   -> new Color(236, 72, 153);
                            case "orange" -> new Color(249, 115, 22);
                            case "brown"  -> new Color(120, 72, 30);
                            case "silver" -> new Color(192, 192, 200);
                            case "dark"   -> new Color(31, 41, 55);
                            default       -> new Color(124, 92, 191); // fallback purple
                        };
                    }
                };
            }
        });

        // ── Hidden vehicle_id column ──────────────────────────────────────────
        TableColumn idCol = table.getColumnModel().getColumn(COL_ID);
        idCol.setMinWidth(0); idCol.setMaxWidth(0); idCol.setWidth(0);
        idCol.setResizable(false);

        // ── Delete column — icon button, red on hover ─────────────────────────
        TableColumn delCol = table.getColumnModel().getColumn(COL_DELETE);
        delCol.setMinWidth(52); delCol.setMaxWidth(52); delCol.setWidth(52);
        delCol.setResizable(false);
        delCol.setCellRenderer(new DeleteButtonRenderer(hoveredRow));
        delCol.setCellEditor(new DeleteButtonEditor(table, tableModel, root, state));

        // ── Column widths ─────────────────────────────────────────────────────
        table.getColumnModel().getColumn(COL_PLATE).setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_OWNER).setPreferredWidth(160);
        table.getColumnModel().getColumn(COL_USER).setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_TYPE).setPreferredWidth(80);
        table.getColumnModel().getColumn(COL_COLOR).setPreferredWidth(110);
        table.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(140);

        // Parking Status header — left-aligned to match pill left edge
        table.getColumnModel().getColumn(COL_STATUS).setHeaderRenderer(buildHeaderRenderer());

        // ── Row sorter ────────────────────────────────────────────────────────
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setSortable(COL_DELETE, false);
        sorter.setSortable(COL_ID, false);
        table.setRowSorter(sorter);

        // ── No results label ──────────────────────────────────────────────────
        JLabel noResultsLabel = new JLabel("No vehicles found", SwingConstants.CENTER);
        noResultsLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        noResultsLabel.setForeground(new Color(107, 95, 160)); // #6B5FA0
        noResultsLabel.setVisible(false);

        // ── Scroll pane — custom dark scrollbar to match design system ───────
        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(ROW_ODD);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Custom scrollbar UI — thin dark thumb, transparent track
        // Track: transparent over ROW_ODD bg
        // Thumb: #2D2860 default, #7C5CBF on hover
        JScrollBar vBar = scroll.getVerticalScrollBar();
        vBar.setOpaque(false);
        vBar.setPreferredSize(new Dimension(8, 0));   // thin 8px bar
        vBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {

            @Override protected void configureScrollBarColors() {
                thumbColor          = new Color(45, 40, 96);       // #2D2860 default thumb
                thumbDarkShadowColor = new Color(0, 0, 0, 0);
                thumbHighlightColor  = new Color(0, 0, 0, 0);
                thumbLightShadowColor = new Color(0, 0, 0, 0);
                trackColor          = new Color(0, 0, 0, 0);
                trackHighlightColor = new Color(0, 0, 0, 0);
            }

            // Hide the arrow buttons entirely
            @Override protected JButton createDecreaseButton(int orientation) {
                return makeZeroButton();
            }
            @Override protected JButton createIncreaseButton(int orientation) {
                return makeZeroButton();
            }
            private JButton makeZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }

            // Transparent track
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(13, 11, 31, 180));           // near-transparent ROW_ODD
                g2.fillRect(trackBounds.x, trackBounds.y,
                            trackBounds.width, trackBounds.height);
                g2.dispose();
            }

            // Rounded pill thumb — purple, brighter on hover
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hovered = isThumbRollover();
                g2.setColor(hovered
                    ? new Color(124, 92, 191, 200)                 // #7C5CBF hover
                    : new Color(45, 40, 96, 180));                 // #2D2860 default
                int arc = thumbBounds.width;                       // full pill shape
                g2.fillRoundRect(
                    thumbBounds.x + 2,
                    thumbBounds.y + 2,
                    thumbBounds.width - 4,
                    thumbBounds.height - 4,
                    arc, arc
                );
                g2.dispose();
            }
        });

        // ── Row count footer ──────────────────────────────────────────────────
        // Shows "Showing N vehicles" at bottom of table card — consistent with mockup
        JLabel rowCountLabel = new JLabel(" ", SwingConstants.LEFT);
        rowCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        rowCountLabel.setForeground(new Color(74, 63, 138));    // #4A3F8A
        rowCountLabel.setBorder(new EmptyBorder(8, 14, 8, 14));
        rowCountLabel.setOpaque(true);
        rowCountLabel.setBackground(ROW_ODD);

        // ── Table card — JLayeredPane approach ────────────────────────────────
        // Layer DEFAULT (0)  : background + scroll pane + footer
        // Layer PALETTE (100): transparent border overlay — always on top
        //
        // Because the border overlay is a separate component in a higher layer,
        // NO repaint from the table (hover, select, scroll) can ever paint over
        // it. It is physically above the table in the Z-order.
        final int ARC = 14;

        // Content panel — holds scroll + footer, clips content to rounded shape
        JPanel contentPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ROW_ODD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
                g2.dispose();
            }
            @Override protected void paintChildren(Graphics g) {
                // Clip children 2px inside border so rows never touch card edges
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(
                    2, 2, getWidth() - 4, getHeight() - 4, ARC, ARC));
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.add(scroll, BorderLayout.CENTER);
        contentPanel.add(rowCountLabel, BorderLayout.SOUTH);

        // Border overlay — transparent except for the border + separators.
        // Sits in a higher layer so it is NEVER repainted over by the table.
        JPanel borderOverlay = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                // fully transparent background — only draws border on top
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Outer rounded border — always visible
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(new Color(58, 52, 120));            // solid #3A3478
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, ARC, ARC);

                // Header separator
                g2.setStroke(new BasicStroke(1.0f));
                g2.setColor(new Color(58, 52, 120, 200));
                int headerH = table.getTableHeader().getPreferredSize().height;
                g2.drawLine(2, headerH + 2, getWidth() - 3, headerH + 2);

                // Row separator lines
                int scrollY  = scroll.getViewport().getViewPosition().y;
                int rowH     = table.getRowHeight();
                int rowCount = table.getRowCount();
                int footerH  = rowCountLabel.getPreferredSize().height + 2;
                for (int i = 1; i <= rowCount; i++) {
                    int lineY = headerH + 2 + (i * rowH) - scrollY;
                    if (lineY > headerH + 2 && lineY < getHeight() - footerH) {
                        g2.setColor(new Color(36, 32, 80, 140));
                        g2.drawLine(2, lineY, getWidth() - 3, lineY);
                    }
                }
                g2.dispose();
            }
        };
        borderOverlay.setOpaque(false);

        // Repaint border overlay whenever the viewport scrolls
        scroll.getViewport().addChangeListener(e -> borderOverlay.repaint());

        // Layered pane — overrides doLayout() so both children always fill
        // the full pane from the very first paint. No componentListener needed,
        // no race condition on first render, no ghost controls bleeding through.
        JLayeredPane layeredCard = new JLayeredPane() {
            @Override public void doLayout() {
                int w = getWidth(), h = getHeight();
                for (Component c : getComponents()) c.setBounds(0, 0, w, h);
            }
            @Override public Dimension getPreferredSize() {
                return contentPanel.getPreferredSize();
            }
        };

        layeredCard.add(contentPanel,  JLayeredPane.DEFAULT_LAYER);
        layeredCard.add(borderOverlay, JLayeredPane.PALETTE_LAYER); // always on top

        JPanel tableLayer = new JPanel(new BorderLayout());
        tableLayer.setOpaque(true);
        tableLayer.setBackground(ROW_ODD);
        tableLayer.add(layeredCard, BorderLayout.CENTER);

        // ── Search + filter logic ─────────────────────────────────────────────
        // Leftmost prefix rule: query "ab" matches any WORD that STARTS WITH "ab".
        // e.g. searching "asher" matches "asher abesia" (word "asher" starts with it)
        // but NOT "mark asher" when field is Owner — "mark" doesn't start with "asher"
        // and "asher" is not the leftmost word... wait, it IS a word that starts with
        // "asher" so it still matches. The rule is: ANY word in the cell value may
        // match, as long as that word STARTS WITH the query token (leftmost prefix).
        // "ab" → matches "abesia", "ab" — does NOT match "besia" or "mark" alone.
        Runnable applyFilter = () -> {
            String raw       = searchField.getText();
            String filterSel = (String) filterBox.getSelectedItem();
            String statusSel = (String) statusBox.getSelectedItem();

            java.util.List<RowFilter<DefaultTableModel, Object>> filters = new java.util.ArrayList<>();

            // ── Leftmost prefix text filter ───────────────────────────────────
            if (!raw.equals(PLACEHOLDER) && !raw.isBlank()) {
                final String query = raw.trim().toLowerCase();

                // Determine which columns to search
                final int[] searchCols;
                if ("All".equals(filterSel)) {
                    searchCols = new int[]{COL_PLATE, COL_OWNER, COL_USER,
                                           COL_TYPE, COL_COLOR, COL_STATUS};
                } else {
                    int col = switch (filterSel) {
                        case "Plate"          -> COL_PLATE;
                        case "Owner"          -> COL_OWNER;
                        case "Username"       -> COL_USER;
                        case "Type"           -> COL_TYPE;
                        case "Color"          -> COL_COLOR;
                        case "Parking Status" -> COL_STATUS;
                        default               -> -1;
                    };
                    searchCols = col >= 0 ? new int[]{col}
                                          : new int[]{COL_PLATE, COL_OWNER, COL_USER,
                                                      COL_TYPE, COL_COLOR, COL_STATUS};
                }

                filters.add(new RowFilter<>() {
                    @Override
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                        for (int col : searchCols) {
                            Object val = entry.getValue(col);
                            if (val == null) continue;
                            // Split cell value into words, check if ANY word
                            // starts with the query (leftmost prefix per word)
                            String[] words = val.toString().toLowerCase().split("\\s+");
                            for (String word : words) {
                                if (word.startsWith(query)) return true;
                            }
                        }
                        return false;
                    }
                });
            }

            // ── Status filter — exact match ───────────────────────────────────
            if (statusSel != null && !"All Status".equals(statusSel)) {
                filters.add(RowFilter.regexFilter(
                    "(?i)^" + java.util.regex.Pattern.quote(statusSel) + "$", COL_STATUS));
            }

            if (filters.isEmpty()) {
                sorter.setRowFilter(null);
            } else if (filters.size() == 1) {
                sorter.setRowFilter(filters.get(0));
            } else {
                sorter.setRowFilter(RowFilter.andFilter(filters));
            }

            int visible = table.getRowCount();
            noResultsLabel.setVisible(visible == 0);
            rowCountLabel.setText(visible == 0
                ? "  No vehicles found"
                : "  Showing " + visible + " vehicle" + (visible == 1 ? "" : "s"));
        };

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter.run(); }
        });
        filterBox.addActionListener(e -> applyFilter.run());
        statusBox.addActionListener(e -> applyFilter.run());

        root.addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) {
                reloadTable(tableModel, sorter, searchField, PLACEHOLDER, noResultsLabel, rowCountLabel);
            }
        });

        state.addSlotChangeListener(() ->
            SwingUtilities.invokeLater(() ->
                reloadTable(tableModel, sorter, searchField, PLACEHOLDER, noResultsLabel, rowCountLabel)
            )
        );

        reloadTable(tableModel, sorter, searchField, PLACEHOLDER, noResultsLabel, rowCountLabel);

        // ── Layout assembly ───────────────────────────────────────────────────
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(ROW_ODD);
        body.setOpaque(true);
        body.setBorder(new EmptyBorder(16, 20, 20, 20));
        body.add(noResultsLabel, BorderLayout.NORTH);
        body.add(tableLayer, BorderLayout.CENTER);

        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    // ── Filter combo builder ──────────────────────────────────────────────────
    // Consistent input-style dropdown: bg #0D0B1F, border #2D2860, text #9B8FD4
    private static JComboBox<String> buildFilterCombo(String[] options) {
        JComboBox<String> box = new JComboBox<>(options) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ROW_ODD);                          // #0D0B1F
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(new Color(45, 40, 96));            // #2D2860
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                // Selected text
                String selected = getSelectedItem() != null ? getSelectedItem().toString() : "";
                g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g2.setColor(new Color(155, 143, 212));         // #9B8FD4
                FontMetrics fm = g2.getFontMetrics();
                int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(selected, 10, textY);
                // Chevron indicator
                g2.setColor(new Color(107, 95, 160));          // #6B5FA0
                g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                g2.drawString("\u25BE", getWidth() - 16, textY);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        box.setOpaque(false);
        box.setPreferredSize(new Dimension(130, 34));
        box.setBorder(null);
        box.setFocusable(false);
        for (Component comp : box.getComponents()) {
            if (comp instanceof AbstractButton ab) ab.setVisible(false);
        }
        box.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? new Color(36, 30, 107) : new Color(18, 16, 58));
                setForeground(new Color(196, 191, 237));
                setBorder(new EmptyBorder(6, 12, 6, 12));
                return this;
            }
        });
        return box;
    }

    // ── Header renderer — shared style across all columns ────────────────────
    private static TableCellRenderer buildHeaderRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                lbl.setBackground(ROW_ODD);
                lbl.setForeground(HEADER_FG);               // #6B5FA0
                lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
                lbl.setBorder(new EmptyBorder(0, 14, 0, 14));
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                lbl.setOpaque(true);
                return lbl;
            }
        };
    }

    // ── Delete button renderer ────────────────────────────────────────────────
    private static class DeleteButtonRenderer implements TableCellRenderer {
        private final int[] hoveredRow;
        DeleteButtonRenderer(int[] hoveredRow) { this.hoveredRow = hoveredRow; }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            return new JPanel() {
                { setOpaque(true); }
                @Override protected void paintComponent(Graphics g) {
                    boolean sel = table.isRowSelected(row);
                    Color bg = sel ? ROW_SELECTED
                                   : (row == hoveredRow[0] ? ROW_HOVER
                                   : (row % 2 == 0 ? ROW_ODD : ROW_EVEN));
                    g.setColor(bg);
                    g.fillRect(0, 0, getWidth(), getHeight());

                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int cx = getWidth() / 2, cy = getHeight() / 2, r = 13;
                    boolean hovered = row == hoveredRow[0];

                    // Red circle highlight on hover — #E8365D
                    if (hovered) {
                        g2.setColor(new Color(232, 54, 93, 35));
                        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                        g2.setColor(new Color(232, 54, 93, 70));
                        g2.setStroke(new BasicStroke(1f));
                        g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                    }

                    // Trash icon — use text unicode; muted when not hovered
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
                    g2.setColor(hovered ? new Color(232, 54, 93) : new Color(107, 95, 160));
                    FontMetrics fm = g2.getFontMetrics();
                    String icon = "\uD83D\uDDD1";
                    g2.drawString(icon, cx - fm.stringWidth(icon) / 2, cy + fm.getAscent() / 2 - 1);
                    g2.dispose();
                }
            };
        }
    }

    // ── Delete button editor ──────────────────────────────────────────────────
    private static class DeleteButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton btn;
        private final JTable table;
        private final DefaultTableModel tableModel;
        private final Component parent;
        private final AppState state;
        private int pendingModelRow = -1;

        DeleteButtonEditor(JTable table, DefaultTableModel tableModel,
                           Component parent, AppState state) {
            this.table      = table;
            this.tableModel = tableModel;
            this.parent     = parent;
            this.state      = state;

            btn = new JButton();
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            btn.addActionListener(e -> {
                int modelRow = pendingModelRow;
                fireEditingCanceled();

                if (modelRow < 0 || modelRow >= tableModel.getRowCount()) return;

                String plate     = (String) tableModel.getValueAt(modelRow, COL_PLATE);
                int    vehicleId = (int)    tableModel.getValueAt(modelRow, COL_ID);

                int choice = DialogUtil.showConfirmDialog(
                    parent,
                    "Delete vehicle with plate \"" + plate + "\"?\nThis action cannot be undone.",
                    "Delete Vehicle",
                    JOptionPane.YES_NO_OPTION
                );

                if (choice == JOptionPane.YES_OPTION) {
                    try {
                        ParkingTransactionDAO transDAO = new ParkingTransactionDAO();
                        List<ParkingTransaction> transactions = transDAO.findByVehicleId(vehicleId);
                        for (ParkingTransaction trans : transactions) {
                            transDAO.delete(trans.getTransactionId());
                        }

                        RFIDMappingDAO rfidDAO = new RFIDMappingDAO();
                        List<RFIDMapping> rfidMappings = rfidDAO.findAllByVehicleId(vehicleId);
                        for (RFIDMapping rfid : rfidMappings) {
                            rfidDAO.delete(rfid.getRfidId());
                        }

                        new VehicleDAO().delete(vehicleId);
                        tableModel.removeRow(modelRow);
                        state.notifySlotChange();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        DialogUtil.showMessageDialog(parent,
                            "Failed to delete vehicle: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int col) {
            pendingModelRow = table.convertRowIndexToModel(row);
            return btn;
        }

        @Override public Object getCellEditorValue() { return null; }
    }

    // ── Data loader ───────────────────────────────────────────────────────────
    private static void reloadTable(DefaultTableModel tableModel,
                                    TableRowSorter<DefaultTableModel> sorter,
                                    JTextField searchField,
                                    String placeholder,
                                    JLabel noResultsLabel,
                                    JLabel rowCountLabel) {
        tableModel.setRowCount(0);
        sorter.setRowFilter(null);
        searchField.setText(placeholder);
        searchField.setForeground(new Color(107, 95, 160));    // #6B5FA0
        noResultsLabel.setVisible(false);

        try {
            VehicleDAO      vehicleDAO     = new VehicleDAO();
            VehicleOwnerDAO ownerDAO       = new VehicleOwnerDAO();
            UserAccountDAO  userAccountDAO = new UserAccountDAO();
            List<Vehicle>   vehicles       = vehicleDAO.findAll();

            for (Vehicle v : vehicles) {
                String ownerName = "Unknown";
                String username  = "—";
                try {
                    Optional<VehicleOwner> owner = ownerDAO.findById(v.getOwnerId());
                    if (owner.isPresent()) {
                        VehicleOwner o = owner.get();
                        ownerName = o.getFirstName() + " " + o.getLastName();
                        if (o.getUserId() != null) {
                            Optional<UserAccount> account = userAccountDAO.findById(o.getUserId());
                            username = account.map(UserAccount::getUsername).orElse("—");
                        }
                    }
                } catch (Exception ex) {
                    // keep defaults
                }

                tableModel.addRow(new Object[]{
                    v.getPlateNumber(),
                    ownerName,
                    username,
                    v.getVehicleType()   != null ? v.getVehicleType()   : "N/A",
                    v.getColor()         != null ? v.getColor()         : "N/A",
                    v.getParkingStatus() != null ? v.getParkingStatus() : "Not Parked",
                    null,
                    v.getVehicleId()
                });
            }

            int total = tableModel.getRowCount();
            rowCountLabel.setText("  Showing " + total + " vehicle" + (total == 1 ? "" : "s"));

        } catch (Exception e) {
            e.printStackTrace();
            rowCountLabel.setText("  Error loading vehicles");
            DialogUtil.showMessageDialog(null,
                "Error loading vehicles: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Table styling ─────────────────────────────────────────────────────────
    private static void styleTable(JTable table) {
        table.setBackground(ROW_ODD);
        table.setForeground(new Color(196, 191, 237));          // #C4BFED
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setRowHeight(44);                                  // 44px restored
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(ROW_SELECTED);
        table.setSelectionForeground(new Color(240, 236, 255));
        table.setOpaque(true);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setOpaque(true);
        header.setBackground(ROW_ODD);
        header.setForeground(HEADER_FG);
        header.setFont(new Font("SansSerif", Font.BOLD, 10));
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createEmptyBorder());
        header.setDefaultRenderer(buildHeaderRenderer());
    }
}