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

    private static final Color ROW_EVEN   = new Color(28, 27, 51);
    private static final Color ROW_ODD    = new Color(22, 21, 43);
    private static final Color GRID_COLOR = new Color(48, 44, 78);

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.setOpaque(true);
        root.setName("ADMIN_AUDIT_LOG");
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_AUDIT_LOG"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);
        content.setOpaque(true);

        // ── Top bar ───────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout(0, 8));
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(12, 24, 12, 24));

        // Title row — "AUDIT LOG" uppercase, bold, no icon, no refresh button
        JLabel titleLbl = UIFactory.lbl("AUDIT LOG", Font.BOLD, 20, C_WHITE);
        topBar.add(titleLbl, BorderLayout.NORTH);

        // Filter row — search + dropdowns
        JPanel filterRow = new JPanel(new BorderLayout(8, 0));
        filterRow.setOpaque(false);

        JTextField searchField = new JTextField();
        searchField.setBackground(C_BG_CARD);
        searchField.setForeground(C_MUTED);
        searchField.setCaretColor(C_WHITE);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_INPUT_BD),
                new EmptyBorder(5, 10, 5, 10)));
        searchField.setText("Search by user, action, IP\u2026");
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals("Search by user, action, IP\u2026")) {
                    searchField.setText("");
                    searchField.setForeground(C_WHITE);
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Search by user, action, IP\u2026");
                    searchField.setForeground(C_MUTED);
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
                    c.setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                    c.setForeground(C_WHITE);
                }
                return c;
            }
        };

        table.setBackground(ROW_EVEN);
        table.setForeground(C_WHITE);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setRowHeight(34);
        table.setGridColor(GRID_COLOR);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(C_PURPLE);
        table.setSelectionForeground(C_WHITE);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setBackground(C_BG_PANEL);
        header.setForeground(C_MUTED);
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GRID_COLOR));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 30));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(C_BG_PANEL);
        headerRenderer.setForeground(C_MUTED);
        headerRenderer.setFont(new Font("SansSerif", Font.BOLD, 11));
        headerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        headerRenderer.setBorder(new EmptyBorder(0, 14, 0, 14));
        for (int i = 0; i < cols.length; i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        // Cell renderers
        table.getColumnModel().getColumn(0).setCellRenderer(new TimestampRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new BadgeRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new EntityChipRenderer());

        DefaultTableCellRenderer plain = new DefaultTableCellRenderer();
        plain.setBorder(new EmptyBorder(0, 14, 0, 14));
        plain.setForeground(C_WHITE);
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
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JScrollBar vBar = scroll.getVerticalScrollBar();
        vBar.setPreferredSize(new Dimension(6, 0));
        vBar.setBackground(ROW_EVEN);
        vBar.setUI(new BasicScrollBarUI() {
            private final Color THUMB = new Color(90, 80, 160);
            private final Color TRACK = ROW_EVEN;
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

        // Rounded wrapper
        JPanel roundedWrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ROW_EVEN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(GRID_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        roundedWrap.setOpaque(false);
        roundedWrap.add(scroll, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(true);
        body.setBackground(C_BG_DARK);
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
                        g.setColor(C_BG_CARD);
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
                g.setColor(C_BG_CARD);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });

        cb.setBackground(C_BG_CARD);
        cb.setForeground(C_WHITE);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cb.setPreferredSize(new Dimension(128, 28));
        cb.setBorder(BorderFactory.createLineBorder(GRID_COLOR));
        cb.setFocusable(false);

        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, hasFocus);
                lbl.setBackground(isSelected ? C_PURPLE : C_BG_CARD);
                lbl.setForeground(C_WHITE);
                lbl.setBorder(new EmptyBorder(5, 10, 5, 10));
                lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                lbl.setOpaque(true);
                return lbl;
            }
        });

        return cb;
    }

    // ── Timestamp renderer ────────────────────────────────────────────────────
    static class TimestampRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object value, boolean sel, boolean foc, int row, int col) {
            String raw = value != null ? value.toString() : "—";
            String timePart = raw, datePart = "";
            if (raw.length() >= 19) {
                timePart = raw.substring(11, 19);
                datePart = raw.substring(0, 10);
            }
            JLabel timeLbl = new JLabel(timePart);
            timeLbl.setFont(new Font("Monospaced", Font.BOLD, 13));
            timeLbl.setForeground(C_WHITE);

            JLabel dateLbl = new JLabel(datePart);
            dateLbl.setFont(new Font("Monospaced", Font.PLAIN, 10));
            dateLbl.setForeground(C_MUTED);

            JPanel cell = new JPanel();
            cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
            cell.setBorder(new EmptyBorder(4, 14, 4, 8));
            cell.add(timeLbl);
            cell.add(dateLbl);
            cell.setBackground(sel ? C_PURPLE : (row % 2 == 0 ? ROW_EVEN : ROW_ODD));
            cell.setOpaque(true);
            return cell;
        }
    }

    // ── Action badge renderer ─────────────────────────────────────────────────
    static class BadgeRenderer implements TableCellRenderer {

        private static final Color[] LOGIN   = { new Color(30, 58, 138),  new Color(147, 197, 253) };
        private static final Color[] LOGOUT  = { new Color(55, 55, 75),   new Color(180, 180, 200) };
        private static final Color[] ENTRY   = { new Color(20, 83, 45),   new Color(134, 239, 172) };
        private static final Color[] EXIT    = { new Color(124, 45, 18),  new Color(253, 186, 116) };
        private static final Color[] FEE     = { new Color(113, 63, 18),  new Color(253, 224, 71)  };
        private static final Color[] DEFAULT = { new Color(55, 48, 90),   new Color(196, 181, 253) };

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

            JPanel left = new JPanel(new GridBagLayout());
            left.setOpaque(false);
            left.setBorder(new EmptyBorder(0, 14, 0, 0));
            left.add(badge);

            JPanel stretch = new JPanel(new BorderLayout());
            stretch.setOpaque(true);
            stretch.setBackground(sel ? C_PURPLE : (row % 2 == 0 ? ROW_EVEN : ROW_ODD));
            stretch.add(left, BorderLayout.WEST);
            return stretch;
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

        private static final Color CHIP_BG = new Color(45, 42, 75);
        private static final Color CHIP_BD = new Color(70, 65, 110);
        private static final Color CHIP_FG = new Color(180, 175, 210);

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

            JPanel left = new JPanel(new GridBagLayout());
            left.setOpaque(false);
            left.setBorder(new EmptyBorder(0, 14, 0, 0));
            left.add(chip);

            JPanel stretch = new JPanel(new BorderLayout());
            stretch.setOpaque(true);
            stretch.setBackground(sel ? C_PURPLE : (row % 2 == 0 ? ROW_EVEN : ROW_ODD));
            stretch.add(left, BorderLayout.WEST);
            return stretch;
        }
    }
}