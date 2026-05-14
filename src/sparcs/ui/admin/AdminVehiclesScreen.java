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

    // Column indices
    private static final int COL_PLATE  = 0;
    private static final int COL_OWNER  = 1;
    private static final int COL_USER   = 2;
    private static final int COL_TYPE   = 3;
    private static final int COL_COLOR  = 4;
    private static final int COL_STATUS = 5;
    private static final int COL_DELETE = 6;   // icon-only trash button
    private static final int COL_ID     = 7;   // hidden — vehicle_id

    // Row colors
    private static final Color ROW_ODD       = new Color(30, 22, 70);
    private static final Color ROW_EVEN      = new Color(36, 27, 82);
    private static final Color ROW_SELECTED  = new Color(80, 62, 165);
    private static final Color ROW_HOVER     = new Color(55, 42, 120);

    // Status pill colors
    private static final Color PARKED_BG    = new Color(29, 158, 117, 55);
    private static final Color PARKED_FG    = new Color(93, 202, 165);
    private static final Color PARKED_BD    = new Color(29, 158, 117, 100);
    private static final Color NOTPKD_BG   = new Color(255, 255, 255, 18);
    private static final Color NOTPKD_FG   = new Color(175, 169, 236, 160);
    private static final Color NOTPKD_BD   = new Color(175, 169, 236, 50);
    private static final Color SUSPND_BG   = new Color(220, 70, 90, 50);
    private static final Color SUSPND_FG   = new Color(240, 100, 110);
    private static final Color SUSPND_BD   = new Color(220, 70, 90, 100);

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.setOpaque(true);
        root.setName("ADMIN_VEHICLES");
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_VEHICLES"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);
        content.setOpaque(true);

        // ── Top Bar ──────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(C_BG_PANEL);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // subtle bottom separator line
                g2.setColor(new Color(175, 169, 236, 40));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(16, 28, 16, 32));

        // Title — plain label, no emoji
        JLabel titleLbl = UIFactory.lbl("VEHICLES", Font.BOLD, 20, C_WHITE);
        topBar.add(titleLbl, BorderLayout.WEST);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topRight.setOpaque(false);

        // ── Filter dropdown — fully custom-painted, no OS arrow box ──────────
        String[] filterOptions = {"All", "Plate", "Owner", "Username", "Type", "Color", "Parking Status"};
        JComboBox<String> filterBox = new JComboBox<>(filterOptions) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(55, 42, 115));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(new Color(175, 169, 236, 90));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                String selected = getSelectedItem() != null ? getSelectedItem().toString() : "All";
                g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g2.setColor(C_WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(selected, 12, textY);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2.setColor(new Color(175, 169, 236, 180));
                g2.drawString("\u25be", getWidth() - 18, textY);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        filterBox.setOpaque(false);
        filterBox.setBackground(new Color(55, 42, 115));
        filterBox.setForeground(C_WHITE);
        filterBox.setPreferredSize(new Dimension(130, 34));
        filterBox.setBorder(null);
        filterBox.setFocusable(false);
        for (Component comp : filterBox.getComponents()) {
            if (comp instanceof AbstractButton ab) ab.setVisible(false);
        }
        filterBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? C_PURPLE : new Color(40, 30, 90));
                setForeground(C_WHITE);
                setBorder(new EmptyBorder(6, 12, 6, 12));
                return this;
            }
        });

        // ── Search field ──────────────────────────────────────────────────────
        final String PLACEHOLDER = "  \uD83D\uDD0D  Search vehicles...";
        JTextField searchField = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(45, 35, 100));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(new Color(175, 169, 236, 90));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        searchField.setOpaque(false);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setForeground(C_MUTED);
        searchField.setCaretColor(C_WHITE);
        searchField.setPreferredSize(new Dimension(220, 34));
        searchField.setBorder(new EmptyBorder(4, 6, 4, 10));
        searchField.setText(PLACEHOLDER);

        searchField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (searchField.getText().equals(PLACEHOLDER)) {
                    searchField.setText("");
                    searchField.setForeground(C_WHITE);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (searchField.getText().isBlank()) {
                    searchField.setText(PLACEHOLDER);
                    searchField.setForeground(C_MUTED);
                }
            }
        });

        // ── Add Vehicle button ────────────────────────────────────────────────
        JButton addBtn = new JButton("+ Add Vehicle") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getModel().isRollover()
                    ? new Color(100, 80, 200)
                    : new Color(80, 60, 180);
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        addBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        addBtn.setForeground(C_WHITE);
        addBtn.setOpaque(false);
        addBtn.setContentAreaFilled(false);
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.setPreferredSize(new Dimension(120, 34));
        addBtn.addActionListener(e -> cardLayout.show(rootPanel, "ADMIN_REGISTER"));

        topRight.add(filterBox);
        topRight.add(searchField);
        topRight.add(addBtn);
        topBar.add(topRight, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // ── Table model ───────────────────────────────────────────────────────
        String[] cols = {"Plate", "Owner", "Username", "Type", "Color", "Parking Status", "", "id"};

        // Track hovered row for hover highlight
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

                // Row background — alternating + hover + selection
                if (col != COL_DELETE) {
                    if (selected) {
                        c.setBackground(ROW_SELECTED);
                    } else if (row == hoveredRow[0]) {
                        c.setBackground(ROW_HOVER);
                    } else {
                        c.setBackground(row % 2 == 0 ? ROW_ODD : ROW_EVEN);
                    }
                }

                // Foreground per column
                if (col == COL_PLATE) {
                    c.setForeground(C_PURPLE);
                    c.setFont(new Font("Monospaced", Font.BOLD, 12));
                    if (c instanceof JLabel lbl) lbl.setBorder(new EmptyBorder(0, 14, 0, 0));
                } else if (col == COL_USER) {
                    c.setForeground(C_MUTED);
                    c.setFont(new Font("SansSerif", Font.PLAIN, 12));
                } else if (col == COL_STATUS || col == COL_DELETE) {
                    // handled by custom renderers below
                } else {
                    c.setForeground(C_WHITE);
                    c.setFont(new Font("SansSerif", Font.PLAIN, 13));
                }
                return c;
            }
        };

        // Hover listener
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

        // ── Status pill renderer — extends DefaultTableCellRenderer so row bg is consistent
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                // Let the superclass handle selection/hover background tracking
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                final String status = value != null ? value.toString() : "Not Parked";
                final boolean sel = isSelected;
                final int r = row;

                return new JPanel() {
                    { setOpaque(true); }
                    @Override protected void paintComponent(Graphics g) {
                        // Match exact same bg logic as prepareRenderer
                        Color bg = sel ? ROW_SELECTED
                                       : (r == hoveredRow[0] ? ROW_HOVER
                                       : (r % 2 == 0 ? ROW_ODD : ROW_EVEN));
                        g.setColor(bg);
                        g.fillRect(0, 0, getWidth(), getHeight());

                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        Color pillBg, pillFg, pillBd;
                        if ("Parked".equals(status)) {
                            pillBg = PARKED_BG; pillFg = PARKED_FG; pillBd = PARKED_BD;
                        } else if ("Suspended".equals(status)) {
                            pillBg = SUSPND_BG; pillFg = SUSPND_FG; pillBd = SUSPND_BD;
                        } else {
                            pillBg = NOTPKD_BG; pillFg = NOTPKD_FG; pillBd = NOTPKD_BD;
                        }

                        Font pillFont = new Font("SansSerif", Font.BOLD, 11);
                        g2.setFont(pillFont);
                        FontMetrics fm = g2.getFontMetrics(pillFont);
                        int tw = fm.stringWidth(status);
                        int ph = 22, pw = tw + 24;
                        int px = 14, py = (getHeight() - ph) / 2;
                        int arc = ph;

                        g2.setColor(pillBg);
                        g2.fillRoundRect(px, py, pw, ph, arc, arc);
                        g2.setColor(pillBd);
                        g2.drawRoundRect(px, py, pw - 1, ph - 1, arc, arc);
                        g2.setColor(pillFg);
                        int textY = py + (ph - fm.getHeight()) / 2 + fm.getAscent();
                        g2.drawString(status, px + 12, textY);
                        g2.dispose();
                    }
                };
            }
        });

        // ── Hide vehicle_id column ────────────────────────────────────────────
        TableColumn idCol = table.getColumnModel().getColumn(COL_ID);
        idCol.setMinWidth(0); idCol.setMaxWidth(0); idCol.setWidth(0);
        idCol.setResizable(false);

        // ── Delete column ─────────────────────────────────────────────────────
        TableColumn delCol = table.getColumnModel().getColumn(COL_DELETE);
        delCol.setMinWidth(48); delCol.setMaxWidth(48); delCol.setWidth(48);
        delCol.setResizable(false);
        delCol.setCellRenderer(new DeleteButtonRenderer(hoveredRow));
        delCol.setCellEditor(new DeleteButtonEditor(table, tableModel, root, state));

        // ── Column widths ─────────────────────────────────────────────────────
        table.getColumnModel().getColumn(COL_PLATE).setPreferredWidth(130);
        table.getColumnModel().getColumn(COL_OWNER).setPreferredWidth(160);
        table.getColumnModel().getColumn(COL_USER).setPreferredWidth(130);
        table.getColumnModel().getColumn(COL_TYPE).setPreferredWidth(80);
        table.getColumnModel().getColumn(COL_COLOR).setPreferredWidth(90);
        table.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(150);
        // Parking Status header — match the global header style exactly, left-aligned
        table.getColumnModel().getColumn(COL_STATUS).setHeaderRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                lbl.setBackground(C_BG_PANEL);
                lbl.setForeground(new Color(175, 169, 236, 180));
                lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
                lbl.setBorder(new EmptyBorder(0, 14, 0, 14));
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                lbl.setOpaque(true);
                return lbl;
            }
        });

        // ── Sorter ────────────────────────────────────────────────────────────
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setSortable(COL_DELETE, false);
        sorter.setSortable(COL_ID, false);
        table.setRowSorter(sorter);

        // ── No results label ──────────────────────────────────────────────────
        JLabel noResultsLabel = new JLabel("No results found", SwingConstants.CENTER);
        noResultsLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        noResultsLabel.setForeground(C_MUTED);
        noResultsLabel.setVisible(false);

        // ── Scroll pane — borderless, the outer card draws the rounded border ──
        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(ROW_ODD);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setBackground(ROW_ODD);
        scroll.getVerticalScrollBar().setOpaque(true);

        // Scroll pane — normal, no clipping tricks
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Rounded card — corner masking painted LAST so it always wins
        final int ARC = 22;
        // Use a JPanel that paints children first, then masks corners on top
        JPanel tableCard = new JPanel(new BorderLayout()) {
            @Override public void paint(Graphics g) {
                // Paint everything (background + all children) first
                super.paint(g);
                // Then mask the corners by painting C_BG_DARK over the square bits
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BG_DARK);
                java.awt.geom.Area full = new java.awt.geom.Area(
                    new Rectangle(0, 0, getWidth(), getHeight()));
                java.awt.geom.Area rounded = new java.awt.geom.Area(
                    new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC, ARC));
                full.subtract(rounded);
                g2.fill(full);
                // Thick mask stroke to cover aliased corner pixels
                g2.setStroke(new BasicStroke(4f));
                g2.setColor(C_BG_DARK);
                g2.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, ARC, ARC);
                // Visible rounded border
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(new Color(175, 169, 236, 70));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, ARC, ARC);
                // Header separator + row separator lines
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(175, 169, 236, 40));
                int headerH = table.getTableHeader().getPreferredSize().height;
                int scrollY = scroll.getViewport().getViewPosition().y;
                int rowH = table.getRowHeight();
                int rowCount = table.getRowCount();
                int tableTop = headerH; // y offset where rows start inside the card
                // Header bottom line
                g2.drawLine(0, headerH, getWidth(), headerH);
                // Row separator lines
                for (int i = 1; i <= rowCount; i++) {
                    int lineY = tableTop + (i * rowH) - scrollY;
                    if (lineY > headerH && lineY < getHeight()) {
                        g2.drawLine(0, lineY, getWidth(), lineY);
                    }
                }
                g2.dispose();
            }
        };
        tableCard.setOpaque(true);
        tableCard.setBackground(ROW_ODD);
        tableCard.add(scroll, BorderLayout.CENTER);

        JPanel tableLayer = new JPanel(new BorderLayout());
        tableLayer.setOpaque(true);
        tableLayer.setBackground(C_BG_DARK);
        tableLayer.add(tableCard, BorderLayout.CENTER);

        noResultsLabel.setOpaque(false);

        // ── Search + filter logic ─────────────────────────────────────────────
        Runnable applyFilter = () -> {
            String raw       = searchField.getText();
            String filterSel = (String) filterBox.getSelectedItem();

            if (raw.equals(PLACEHOLDER) || raw.isBlank()) {
                sorter.setRowFilter(null);
                noResultsLabel.setVisible(false);
                return;
            }

            String regex = "(?i)^" + java.util.regex.Pattern.quote(raw.trim());
            RowFilter<DefaultTableModel, Object> rf;

            if ("All".equals(filterSel)) {
                rf = RowFilter.regexFilter(regex);
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
                rf = col >= 0 ? RowFilter.regexFilter(regex, col) : RowFilter.regexFilter(regex);
            }

            sorter.setRowFilter(rf);
            noResultsLabel.setVisible(table.getRowCount() == 0);
        };

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter.run(); }
        });
        filterBox.addActionListener(e -> applyFilter.run());

        root.addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) {
                reloadTable(tableModel, sorter, searchField, PLACEHOLDER, noResultsLabel);
            }
        });

        state.addSlotChangeListener(() ->
            SwingUtilities.invokeLater(() ->
                reloadTable(tableModel, sorter, searchField, PLACEHOLDER, noResultsLabel)
            )
        );

        reloadTable(tableModel, sorter, searchField, PLACEHOLDER, noResultsLabel);

        // ── Layout Assembly ───────────────────────────────────────────────────
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(C_BG_DARK);
        body.setOpaque(true);
        body.setBorder(new EmptyBorder(16, 20, 20, 20));
        body.add(tableLayer, BorderLayout.CENTER);

        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
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
                    Color bg = sel ? ROW_SELECTED : (row == hoveredRow[0] ? ROW_HOVER : (row % 2 == 0 ? ROW_ODD : ROW_EVEN));
                    g.setColor(bg);
                    g.fillRect(0, 0, getWidth(), getHeight());

                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // Draw trash icon circle button
                    int cx = getWidth() / 2, cy = getHeight() / 2, r = 13;
                    if (row == hoveredRow[0]) {
                        g2.setColor(new Color(220, 70, 90, 40));
                        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                        g2.setColor(new Color(220, 70, 90, 80));
                        g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                    }
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
                    g2.setColor(row == hoveredRow[0] ? new Color(240, 90, 100) : new Color(180, 80, 90));
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
                        // Delete in order of foreign key dependencies
                        // 1. Delete parking transactions
                        ParkingTransactionDAO transDAO = new ParkingTransactionDAO();
                        java.util.List<ParkingTransaction> transactions = transDAO.findByVehicleId(vehicleId);
                        for (ParkingTransaction trans : transactions) {
                            transDAO.delete(trans.getTransactionId());
                        }
                        
                        // 2. Delete ALL RFID mappings (regardless of status)
                        RFIDMappingDAO rfidDAO = new RFIDMappingDAO();
                        java.util.List<RFIDMapping> rfidMappings = rfidDAO.findAllByVehicleId(vehicleId);
                        for (RFIDMapping rfid : rfidMappings) {
                            rfidDAO.delete(rfid.getRfidId());
                        }
                        
                        // 3. Finally delete the vehicle
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
                                    JLabel noResultsLabel) {
        tableModel.setRowCount(0);
        sorter.setRowFilter(null);
        searchField.setText(placeholder);
        searchField.setForeground(C_MUTED);
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

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showMessageDialog(null,
                "Error loading vehicles: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Table styling ─────────────────────────────────────────────────────────
    private static void styleTable(JTable table) {
        table.setBackground(ROW_ODD);
        table.setForeground(C_WHITE);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setRowHeight(40);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(ROW_SELECTED);
        table.setSelectionForeground(C_WHITE);
        table.setOpaque(true);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setOpaque(false);
        header.setBackground(C_BG_PANEL);
        header.setBorder(new EmptyBorder(0, 0, 1, 0));
        header.setForeground(new Color(175, 169, 236, 180));
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setPreferredSize(new Dimension(0, 38));
        header.setBorder(new EmptyBorder(0, 0, 1, 0));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                lbl.setBackground(C_BG_PANEL);
                lbl.setForeground(new Color(175, 169, 236, 180));
                lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
                lbl.setBorder(new EmptyBorder(0, 14, 0, 14));
                lbl.setOpaque(true);
                return lbl;
            }
        });
    }
}