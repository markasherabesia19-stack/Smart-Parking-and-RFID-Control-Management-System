package ui.admin;

import dao.AuditLogDAO;
import model.AppState;
import model.AuditLog;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AdminAuditLogScreen {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // Row colors — match Dashboard & Fees exactly
    private static final Color ROW_BASE   = new Color(26, 22, 80);    // #1A1650  Layer 2 — same as Dashboard/Fees
    private static final Color ROW_EVEN   = new Color(26, 22, 80);    // #1A1650  even rows
    private static final Color ROW_ODD    = new Color(22, 18, 70);    // slightly darker alt
    private static final Color ROW_HOVER  = new Color(36, 30, 107);   // #241E6B  hover
    private static final Color ROW_SELECT = new Color(36, 30, 107);   // #241E6B  selected
    private static final Color GRID_COLOR = new Color(30, 28, 69);    // #1E1C45  separator

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.BLACK);
        root.setOpaque(true);
        root.setName("ADMIN_AUDIT_LOG");
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_AUDIT_LOG"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.BLACK);
        content.setOpaque(true);

        // ── Top bar — #1A1650 bg, 54px, bottom separator per design system ──
        JPanel topBar = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(C_BG_PANEL); // matches Slot Map / Register Vehicle topbar purple-blue
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(30, 28, 69)); // #1E1C45 separator
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(12, 24, 12, 24));

        // Title row — "AUDIT LOG" uppercase, bold, no icon, no refresh button
        JLabel titleLbl = UIFactory.lbl("AUDIT LOG", Font.BOLD, 18, new Color(240, 236, 255));
        topBar.add(titleLbl, BorderLayout.NORTH);

        // Filter row — search + dropdowns
        JPanel filterRow = new JPanel(new BorderLayout(8, 0));
        filterRow.setOpaque(false);

        // Search field — bg #0D0B1F, border 1.5px #2D2860, focus #7C5CBF
        JTextField searchField = new JTextField() {
            private boolean focused = false;
            {
                addFocusListener(new java.awt.event.FocusAdapter() {
                    @Override public void focusGained(java.awt.event.FocusEvent e) { focused = true; repaint(); }
                    @Override public void focusLost(java.awt.event.FocusEvent e)   { focused = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(18, 16, 58)); // #12103A input bg in topbar context
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(focused ? new Color(124, 92, 191) : new Color(45, 40, 96));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        searchField.setOpaque(false);
        searchField.setForeground(new Color(107, 95, 160));
        searchField.setCaretColor(new Color(196, 191, 237));
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        searchField.setBorder(new EmptyBorder(5, 12, 5, 12));
        searchField.setText("Search by user, action, IP\u2026");
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals("Search by user, action, IP\u2026")) {
                    searchField.setText("");
                    searchField.setForeground(new Color(196, 191, 237));
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Search by user, action, IP\u2026");
                    searchField.setForeground(new Color(107, 95, 160));
                }
            }
        });

        JComboBox<String> actionFilter = styledCombo(
                new String[]{"All Actions", "LOGIN", "LOGOUT", "ENTRY", "EXIT", "FEE_COLLECTED"});
        JComboBox<String> entityFilter = styledCombo(
                new String[]{"All Entities", "USER", "PARKING_SLOT", "VEHICLE"});

        JPanel combos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        combos.setOpaque(false);
        combos.add(actionFilter);
        combos.add(entityFilter);

        filterRow.add(searchField, BorderLayout.CENTER);
        filterRow.add(combos, BorderLayout.EAST);
        topBar.add(filterRow, BorderLayout.SOUTH);

        content.add(topBar, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────────────────────
        String[] cols = {"Timestamp", "User ID", "Action", "Entity Type", "Entity ID", "IP Address"};
        DefaultTableModel tableModel = new DefaultTableModel(new Object[0][cols.length], cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(new Color(26, 22, 80)); // C_BG_ROW #1A1650
                    c.setForeground(C_WHITE);
                }
                return c;
            }
        };

        table.setBackground(C_BG_CARD); // matches Fees pendingScroll viewport
        table.setForeground(new Color(196, 191, 237)); // #C4BFED
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setRowHeight(44);
        table.setGridColor(GRID_COLOR);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(36, 30, 107)); // #241E6B
        table.setSelectionForeground(C_WHITE);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setBackground(C_BG_PANEL); // matches Fees styleTable header
        header.setForeground(C_MUTED); // matches Fees styleTable header fg
        header.setFont(new Font("SansSerif", Font.BOLD, 10));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GRID_COLOR));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 38));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(C_BG_PANEL); // matches Fees styleTable
        headerRenderer.setForeground(C_MUTED); // matches Fees
        headerRenderer.setFont(new Font("SansSerif", Font.BOLD, 10));
        headerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        headerRenderer.setBorder(new EmptyBorder(0, 14, 0, 14));
        for (int i = 0; i < cols.length; i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        // Cell renderers
        table.getColumnModel().getColumn(0).setCellRenderer(new TimestampRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new BadgeRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new EntityChipRenderer());

        // Action + Entity Type headers — extra left padding to align with badge/chip content
        DefaultTableCellRenderer paddedHeader = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, foc, row, col);
                setText(value != null ? value.toString().toUpperCase() : "");
                setFont(new Font("SansSerif", Font.BOLD, 10));
                setForeground(C_MUTED);
                setBackground(C_BG_PANEL);
                setBorder(new EmptyBorder(0, 14, 0, 0)); // 14px — aligns closer to badge, matches other headers
                setHorizontalAlignment(SwingConstants.LEFT);
                setOpaque(true);
                return this;
            }
        };
        table.getColumnModel().getColumn(2).setHeaderRenderer(paddedHeader); // Action
        table.getColumnModel().getColumn(3).setHeaderRenderer(paddedHeader); // Entity Type

        // Timestamp header — 24px left to align with cell content padding
        DefaultTableCellRenderer timestampHeader = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, foc, row, col);
                setText(value != null ? value.toString() : "");
                setFont(new Font("SansSerif", Font.BOLD, 10));
                setForeground(C_MUTED);
                setBackground(C_BG_PANEL);
                setBorder(new EmptyBorder(0, 24, 0, 0)); // matches cell's 24px left padding
                setHorizontalAlignment(SwingConstants.LEFT);
                setOpaque(true);
                return this;
            }
        };
        table.getColumnModel().getColumn(0).setHeaderRenderer(timestampHeader);

        DefaultTableCellRenderer plain = new DefaultTableCellRenderer();
        plain.setBorder(new EmptyBorder(0, 14, 0, 14));
        plain.setForeground(new Color(196, 191, 237)); // #C4BFED
        for (int i : new int[]{1, 4, 5}) {
            table.getColumnModel().getColumn(i).setCellRenderer(plain);
        }

        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(1).setPreferredWidth(65);
        table.getColumnModel().getColumn(2).setPreferredWidth(115);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(65);
        table.getColumnModel().getColumn(5).setPreferredWidth(105);

        // ── Scroll pane ───────────────────────────────────────────────────────
        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(C_BG_CARD); // darker fill for empty rows — matches Fees
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JScrollBar vBar = scroll.getVerticalScrollBar();
        vBar.setPreferredSize(new Dimension(6, 0));
        vBar.setBackground(new Color(26, 22, 80));
        vBar.setUI(new BasicScrollBarUI() {
            private final Color THUMB = new Color(124, 92, 191); // #7C5CBF thumb — accent purple
            private final Color TRACK = C_BG_CARD.darker(); // darker track under thumb
            @Override protected void configureScrollBarColors() {
                thumbColor = THUMB; trackColor = TRACK;
                thumbHighlightColor = THUMB; thumbDarkShadowColor = THUMB;
                thumbLightShadowColor = THUMB;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(THUMB);
                g2.fillRoundRect(r.x + 1, r.y + 2, r.width - 2, r.height - 4, 6, 6);
                g2.dispose();
            }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                g.setColor(TRACK); g.fillRect(r.x, r.y, r.width, r.height);
            }
        });

        // Rounded wrapper — same JLayeredPane technique as Vehicles screen
        // so the border is always on top and corners are always curved
        final int ARC = 14;

        JPanel contentPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
                g2.dispose();
            }
            @Override protected void paintChildren(Graphics g) {
                // Clip children 2px inside so scroll never covers the rounded corners
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(
                    2, 2, getWidth() - 4, getHeight() - 4, ARC, ARC));
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.add(scroll, BorderLayout.CENTER);

        // Border overlay — always drawn on top, so nothing covers the curved corners
        JPanel borderOverlay = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(new Color(45, 40, 96)); // #2D2860
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, ARC, ARC);
                g2.dispose();
            }
        };
        borderOverlay.setOpaque(false);

        // Repaint overlay when viewport scrolls
        scroll.getViewport().addChangeListener(e -> borderOverlay.repaint());

        JLayeredPane roundedWrap = new JLayeredPane() {
            @Override public void doLayout() {
                int w = getWidth(), h = getHeight();
                for (Component c : getComponents()) c.setBounds(0, 0, w, h);
            }
            @Override public Dimension getPreferredSize() {
                return contentPanel.getPreferredSize();
            }
        };
        roundedWrap.add(contentPanel,  JLayeredPane.DEFAULT_LAYER);
        roundedWrap.add(borderOverlay, JLayeredPane.PALETTE_LAYER);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(true);
        body.setBackground(Color.BLACK);
        body.setBorder(new EmptyBorder(14, 20, 20, 20));
        body.add(roundedWrap, BorderLayout.CENTER);

        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        // ── Filter logic ──────────────────────────────────────────────────────
        final List<Object[]>[] allRows = new List[]{new ArrayList<>()};

        Runnable applyFilter = () -> {
            String query = searchField.getText().toLowerCase().trim();
            boolean emptyQ = query.equals("search by user, action, ip\u2026") || query.isEmpty();
            String selAction = (String) actionFilter.getSelectedItem();
            String selEntity = (String) entityFilter.getSelectedItem();
            tableModel.setRowCount(0);
            for (Object[] row : allRows[0]) {
                boolean okAction = "All Actions".equals(selAction) || row[2].toString().equalsIgnoreCase(selAction);
                boolean okEntity = "All Entities".equals(selEntity) || row[3].toString().equalsIgnoreCase(selEntity);
                boolean okSearch = emptyQ
                        || row[0].toString().toLowerCase().contains(query)
                        || row[1].toString().toLowerCase().contains(query)
                        || row[2].toString().toLowerCase().contains(query)
                        || row[5].toString().toLowerCase().contains(query);
                if (okAction && okEntity && okSearch) tableModel.addRow(row);
            }
        };

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter.run(); }
        });
        actionFilter.addActionListener(e -> applyFilter.run());
        entityFilter.addActionListener(e -> applyFilter.run());

        // Auto-load
        Runnable loader = () -> loadLogs(tableModel, allRows);
        root.addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { loader.run(); }
        });
        loader.run();
        return root;
    }

    // ── Data loader ───────────────────────────────────────────────────────────
    private static void loadLogs(DefaultTableModel model, List<Object[]>[] allRows) {
        model.setRowCount(0);
        allRows[0] = new ArrayList<>();
        try {
            AuditLogDAO dao = new AuditLogDAO();
            List<AuditLog> logs = dao.findAll();
            if (logs.isEmpty()) {
                model.addRow(new Object[]{"No records found.", "", "", "", "", ""});
                return;
            }
            for (AuditLog log : logs) {
                String timestamp = log.getCreatedAt() != null ? log.getCreatedAt().format(DT_FMT) : "—";
                String userId    = log.getUserId()     != null ? String.valueOf(log.getUserId())    : "—";
                String action    = log.getAction()     != null ? log.getAction()                    : "—";
                String entity    = log.getEntityType() != null ? log.getEntityType()                : "—";
                String entityId  = log.getEntityId()   != null ? String.valueOf(log.getEntityId())  : "—";
                String ip        = log.getIpAddress()  != null ? log.getIpAddress()                 : "—";
                Object[] row = {timestamp, userId, action, entity, entityId, ip};
                allRows[0].add(row);
                model.addRow(row);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            model.addRow(new Object[]{"Error: " + ex.getMessage(), "", "", "", "", ""});
        }
    }

    // ── Styled combo — fully custom painted, no system border/arrow issues ────
    private static JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);

        cb.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton btn = new JButton("\u25BE") { // small downward triangle ▾
                    @Override protected void paintComponent(Graphics g) {
                        g.setColor(new Color(26, 22, 80));
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.setColor(C_MUTED);
                        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                        FontMetrics fm = g.getFontMetrics();
                        String t = "\u25BE";
                        int x = (getWidth()  - fm.stringWidth(t)) / 2;
                        int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                        g.drawString(t, x, y);
                    }
                };
                btn.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, GRID_COLOR));
                btn.setContentAreaFilled(false);
                btn.setFocusPainted(false);
                btn.setPreferredSize(new Dimension(22, 0));
                return btn;
            }

            @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(new Color(26, 22, 80));
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });

        cb.setBackground(new Color(26, 22, 80)); // #1A1650
        cb.setForeground(new Color(155, 143, 212));
        cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cb.setPreferredSize(new Dimension(140, 34));
        cb.setBorder(BorderFactory.createLineBorder(new Color(45, 40, 96)));
        cb.setFocusable(false);

        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, hasFocus);
                lbl.setBackground(isSelected ? new Color(36, 30, 107) : new Color(26, 22, 80));
                lbl.setForeground(C_WHITE);
                lbl.setBorder(new EmptyBorder(5, 10, 5, 10));
                lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                lbl.setOpaque(true);
                return lbl;
            }
        });

        return cb;
    }

    // ── Timestamp renderer — human readable: "May 17, 2026  00:31:56" ────────
    static class TimestampRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object value, boolean sel, boolean foc, int row, int col) {
            String raw = value != null ? value.toString() : "—";
            String datePart = "—", timePart = "";
            try {
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(raw, DT_FMT);
                datePart = ldt.format(DATE_FMT); // e.g. "May 17, 2026"
                timePart = ldt.format(TIME_FMT); // e.g. "00:31:56"
            } catch (Exception ex) {
                datePart = raw; // fallback — show raw if unparseable
            }

            // Two-line layout: date (muted, small) on top, time (bold) below
            JLabel dateLbl = new JLabel(datePart);
            dateLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
            dateLbl.setForeground(new Color(107, 95, 160)); // #6B5FA0 muted

            JLabel timeLbl = new JLabel(timePart);
            timeLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
            timeLbl.setForeground(new Color(196, 191, 237)); // #C4BFED

            JPanel cell = new JPanel();
            cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
            cell.setBorder(new EmptyBorder(6, 24, 6, 8)); // 24px left — shifted right
            cell.add(dateLbl);  // date on top — more familiar reading order
            cell.add(timeLbl);  // time below
            cell.setBackground(sel ? new Color(36, 30, 107) : new Color(26, 22, 80)); // C_BG_ROW
            cell.setOpaque(true);
            return cell;
        }
    }

    // ── Action badge renderer ─────────────────────────────────────────────────
    static class BadgeRenderer implements TableCellRenderer {

        // Badge colors — exact design system status colors
        private static final Color[] LOGIN   = { new Color(79, 142, 247, 38),  new Color(79, 142, 247)  };  // #4F8EF7
        private static final Color[] LOGOUT  = { new Color(107, 95, 160, 46),  new Color(155, 143, 212) };  // muted purple
        private static final Color[] ENTRY   = { new Color(29, 185, 84, 38),   new Color(29, 185, 84)   };  // #1DB954
        private static final Color[] EXIT    = { new Color(232, 54, 93, 38),   new Color(232, 54, 93)   };  // #E8365D
        private static final Color[] FEE     = { new Color(245, 197, 24, 38),  new Color(245, 197, 24)  };  // #F5C518
        private static final Color[] DEFAULT = { new Color(124, 92, 191, 38),  new Color(167, 139, 250) };  // purple

        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object value, boolean sel, boolean foc, int row, int col) {
            String text = value != null ? value.toString() : "—";
            Color[] clr = colors(text);

            JLabel badge = new JLabel(text, SwingConstants.CENTER) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setOpaque(false);
            badge.setBackground(clr[0]);
            badge.setForeground(clr[1]);
            badge.setFont(new Font("SansSerif", Font.BOLD, 10));
            badge.setBorder(new EmptyBorder(2, 9, 2, 9));

            // Center badge vertically + left-align with consistent 14px padding
            JPanel cell = new JPanel(new GridBagLayout());
            cell.setOpaque(true);
            cell.setBackground(sel ? new Color(36, 30, 107) : new Color(26, 22, 80)); // C_BG_ROW
            GridBagConstraints bgc = new GridBagConstraints();
            bgc.anchor = GridBagConstraints.WEST;
            bgc.insets = new Insets(0, 14, 0, 0);
            bgc.weightx = 1.0;
            bgc.fill = GridBagConstraints.NONE;
            cell.add(badge, bgc);
            return cell;
        }

        private Color[] colors(String action) {
            switch (action.toUpperCase()) {
                case "LOGIN":         return LOGIN;
                case "LOGOUT":        return LOGOUT;
                case "ENTRY":         return ENTRY;
                case "EXIT":          return EXIT;
                case "FEE_COLLECTED": return FEE;
                default:              return DEFAULT;
            }
        }
    }

    // ── Entity chip renderer ──────────────────────────────────────────────────
    static class EntityChipRenderer implements TableCellRenderer {

        private static final Color CHIP_BG = new Color(124, 92, 191, 46);  // #7C5CBF @18%
        private static final Color CHIP_BD = new Color(107, 95, 160, 60);  // #6B5FA0
        private static final Color CHIP_FG = new Color(155, 143, 212);     // #9B8FD4

        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object value, boolean sel, boolean foc, int row, int col) {
            String text = value != null ? value.toString().replace("_", " ") : "—";

            JLabel chip = new JLabel(text) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(CHIP_BD);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            chip.setOpaque(false);
            chip.setBackground(CHIP_BG);
            chip.setForeground(CHIP_FG);
            chip.setFont(new Font("SansSerif", Font.PLAIN, 10));
            chip.setBorder(new EmptyBorder(2, 8, 2, 8));

            JPanel cell = new JPanel(new GridBagLayout());
            cell.setOpaque(true);
            cell.setBackground(sel ? new Color(36, 30, 107) : new Color(26, 22, 80)); // C_BG_ROW
            GridBagConstraints cgc = new GridBagConstraints();
            cgc.anchor = GridBagConstraints.WEST;
            cgc.insets = new Insets(0, 14, 0, 0);
            cgc.weightx = 1.0;
            cgc.fill = GridBagConstraints.NONE;
            cell.add(chip, cgc);
            return cell;
        }
    }
}