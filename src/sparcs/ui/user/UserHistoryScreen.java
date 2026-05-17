package ui.user;

import dao.ParkingSlotDAO;
import dao.ParkingTransactionDAO;
import dao.VehicleDAO;
import model.AppState;
import model.ParkingSlot;
import model.ParkingTransaction;
import model.Vehicle;
import service.FeeCalculationService;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserHistoryScreen {

    private static final DateTimeFormatter DT_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private static final String[] COLS = {
        "DATE", "ENTRY TIME", "EXIT TIME", "SLOT", "ZONE", "DURATION", "FEE", "STATUS"
    };

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "USER", "USER_HISTORY"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        // ── Top bar — Layer 1 (#12103A), 22px title, #1E1C45 bottom separator ──
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);                        // matches Slot Map / My Parking Status topbar
        topBar.setOpaque(true);
        topBar.setPreferredSize(new Dimension(0, 54));
        topBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(30, 28, 69)), // #1E1C45 separator
            new EmptyBorder(14, 24, 14, 24)
        ));
        // Page Title: 22px 700 ALL CAPS #F0ECFF per design system §2
        topBar.add(UIFactory.lbl("PARKING HISTORY", Font.BOLD, 22, new Color(240, 236, 255)), BorderLayout.WEST);

        // Refresh — Ghost button style per §3.1
        JButton refreshBtn = buildRefreshButton();
        topBar.add(refreshBtn, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // ── Stat cards — top accent bar style matching AdminSlotMapScreen ──────
        JLabel totalSessionsVal = statValue("—", new Color(29, 185, 84));     // #1DB954 green — Available
        JLabel totalHoursVal    = statValue("—", new Color(232, 54, 93));     // #E8365D red — Occupied
        JLabel totalFeesVal     = statValue("—", new Color(255, 140, 66));    // #FF8C42 orange — Reserved

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 12, 20));
        statsRow.add(accentStatCard("TOTAL SESSIONS", totalSessionsVal, new Color(29, 185, 84)));    // #1DB954 green
        statsRow.add(accentStatCard("TOTAL HOURS",    totalHoursVal,    new Color(232, 54, 93)));    // #E8365D red
        statsRow.add(accentStatCard("TOTAL FEES PAID",totalFeesVal,     new Color(255, 140, 66)));   // #FF8C42 orange

        // ── Table ──────────────────────────────────────────────────────────────
        DefaultTableModel tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.setViewportBorder(null);
        scroll.getViewport().setBackground(new Color(18, 16, 58));  // #12103A — matches row base
        scroll.setBorder(BorderFactory.createLineBorder(new Color(45, 40, 96))); // #2D2860
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 20, 20, 20));
        body.add(scroll, BorderLayout.CENTER);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body, BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);

        // ── Refresh logic ──────────────────────────────────────────────────────
        refreshBtn.addActionListener(e -> {
            tableModel.setRowCount(0);
            List<HistoryRow> rows = loadHistory(state);

            int totalSessions = rows.size();
            long totalMinutes = 0;
            BigDecimal totalFees = BigDecimal.ZERO;

            for (HistoryRow row : rows) {
                if (row.durationMinutes != null) totalMinutes += row.durationMinutes;
                if (row.fee != null)             totalFees = totalFees.add(row.fee);

                tableModel.addRow(new Object[]{
                    row.date,
                    row.entryTime,
                    row.exitTime,
                    row.slotCode,
                    row.zone,
                    row.durationMinutes != null ? row.durationMinutes + " min" : "—",
                    row.fee != null ? "₱" + String.format("%.2f", row.fee.doubleValue()) : "₱0.00",
                    row.paymentStatus
                });
            }

            totalSessionsVal.setText(String.valueOf(totalSessions));
            long hours = totalMinutes / 60, mins = totalMinutes % 60;
            totalHoursVal.setText(hours + "h " + mins + "m");
            totalFeesVal.setText("₱" + String.format("%.2f", totalFees.doubleValue()));

            if (rows.isEmpty())
                tableModel.addRow(new Object[]{"No history found","","","","","","",""});
        });

        root.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) {
                refreshBtn.doClick();
            }
        });

        refreshBtn.doClick();
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    // ── Table styling — mirrors AdminFeesScreen.styleTable() ──────────────────
    private static void styleTable(JTable t) {
        t.setBackground(new Color(18, 16, 58));                  // #12103A Layer 1 row base
        t.setForeground(new Color(196, 191, 237));               // #C4BFED body text
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));        // body font — not Monospaced
        t.setRowHeight(44);                                       // 44px per design system §5
        t.setGridColor(new Color(30, 28, 69));                   // #1E1C45 subtle border
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(new Color(36, 30, 107));        // #241E6B Layer 3
        t.setSelectionForeground(new Color(240, 236, 255));      // #F0ECFF
        t.setOpaque(true);
        t.setFillsViewportHeight(true);

        // Header — #12103A bg, #E8E4FF text 13px 600, #2D2860 bottom border
        JTableHeader header = t.getTableHeader();
        header.setOpaque(true);
        header.setBackground(new Color(18, 16, 58));             // #12103A
        header.setForeground(new Color(232, 228, 255));          // #E8E4FF
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(45, 40, 96))); // #2D2860
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(tbl, value, sel, focus, row, col);
                setFont(new Font("SansSerif", Font.BOLD, 13));
                setForeground(new Color(232, 228, 255));         // #E8E4FF
                setBackground(new Color(18, 16, 58));            // #12103A
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setHorizontalAlignment(SwingConstants.LEFT);
                setOpaque(true);
                return this;
            }
        });

        // Body cell renderer — status badges, fee right-align, body text colors
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                setOpaque(true);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setFont(new Font("SansSerif", Font.PLAIN, 13));
                setHorizontalAlignment(SwingConstants.LEFT);

                if (!isSelected) {
                    setBackground(new Color(18, 16, 58));        // #12103A — all rows same base
                    setForeground(new Color(196, 191, 237));     // #C4BFED body text
                } else {
                    setBackground(new Color(36, 30, 107));       // #241E6B selected
                    setForeground(new Color(240, 236, 255));     // #F0ECFF
                }

                // Status column — badge colors per §3.2
                if (col == COLS.length - 1 && value != null) {
                    String status = value.toString();
                    if (status.equalsIgnoreCase("PAID")) {
                        setForeground(new Color(29, 185, 84));  // #1DB954
                        setFont(new Font("SansSerif", Font.BOLD, 11));
                    } else if (status.equalsIgnoreCase("PENDING")) {
                        setForeground(new Color(255, 140, 66)); // #FF8C42
                        setFont(new Font("SansSerif", Font.BOLD, 11));
                    } else {
                        setForeground(new Color(107, 95, 160)); // #6B5FA0 muted
                        setFont(new Font("SansSerif", Font.BOLD, 11));
                    }
                }

                // Fee column — left-align
                if (col == COLS.length - 2)
                    setHorizontalAlignment(SwingConstants.LEFT);


                return this;
            }
        });

        // Column widths
        int[] widths = {110, 135, 135, 60, 50, 80, 90, 90};
        for (int i = 0; i < widths.length && i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }

    // ── Accent stat card — top colored bar, mirrors AdminSlotMapScreen ─────────
    private static JPanel accentStatCard(String label, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(18, 16, 58));              // #12103A C_BG_PANEL
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                // Accent bar — top 6px, rounded on top corners only
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), 6, 10, 10);
                g2.fillRect(0, 3, getWidth(), 3);
                // Card border — #2D2860
                g2.setColor(new Color(45, 40, 96));
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 16, 14, 16));

        // Section label — 11px 600 ALL CAPS #6B5FA0 per §2
        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(new Color(107, 95, 160));              // #6B5FA0
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        body.add(valueLabel);
        body.add(Box.createVerticalStrut(4));
        body.add(lbl);

        card.add(Box.createVerticalStrut(4), BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private static JLabel statValue(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 22));      // 22px per §2 Page Title/Value
        lbl.setForeground(color);
        return lbl;
    }

    // ── Refresh button — Ghost style per §3.1 ─────────────────────────────────
    private static JButton buildRefreshButton() {
        JButton btn = new JButton("↻  REFRESH") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover()
                    ? new Color(255, 255, 255, 10)
                    : new Color(255, 255, 255, 4));              // rgba(255,255,255,0.04) per §3.1 Ghost
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setForeground(new Color(155, 143, 212));             // #9B8FD4 Ghost text
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(new EmptyBorder(0, 14, 0, 0));             // right-side padding only
        // Ghost border — 0.5px #2D2860 per §3.1
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(45, 40, 96, 180), 1),
            new EmptyBorder(6, 14, 6, 14)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(120, 34));
        return btn;
    }

    // ── Data model ─────────────────────────────────────────────────────────────
    private record HistoryRow(
            String date, String entryTime, String exitTime,
            String slotCode, String zone,
            Integer durationMinutes, BigDecimal fee, String paymentStatus
    ) {}

    private static List<HistoryRow> loadHistory(AppState state) {
        List<HistoryRow> rows = new ArrayList<>();
        try {
            model.UserAccount currentUser = state.getCurrentUserAccount();
            if (currentUser == null) return rows;

            VehicleDAO vehicleDAO = new VehicleDAO();
            List<Vehicle> vehicles = vehicleDAO.findAllByUserId(currentUser.getUserId());

            ParkingTransactionDAO txnDAO = new ParkingTransactionDAO();
            ParkingSlotDAO slotDAO       = new ParkingSlotDAO();
            FeeCalculationService feeService = new FeeCalculationService();

            for (Vehicle v : vehicles) {
                List<ParkingTransaction> txns = txnDAO.findByVehicleId(v.getVehicleId());
                for (ParkingTransaction txn : txns) {
                    BigDecimal fee = txn.getCalculatedFee();
                    if (fee == null) {
                        try { fee = feeService.calculateFee(txn); }
                        catch (Exception ex) { fee = BigDecimal.ZERO; }
                    }
                    Integer duration = txn.getDurationMinutes();
                    if (duration == null && txn.getEntryTime() != null)
                        duration = (int) FeeCalculationService.calculateDurationMinutes(
                                txn.getEntryTime(), txn.getExitTime());

                    String slotCode = "—", zone = "—";
                    Optional<ParkingSlot> slotOpt = slotDAO.findById(txn.getSlotId());
                    if (slotOpt.isPresent()) {
                        slotCode = slotOpt.get().getSlotCode();
                        zone     = slotOpt.get().getZone();
                    }

                    String date      = txn.getEntryTime() != null ? txn.getEntryTime().format(DAY_FMT) : "—";
                    String entryTime = txn.getEntryTime() != null ? txn.getEntryTime().format(DT_FMT)  : "—";
                    String exitTime  = txn.getExitTime()  != null ? txn.getExitTime().format(DT_FMT)   : "Still Parked";
                    String payStatus = txn.getPaymentStatus() != null ? txn.getPaymentStatus() : "PENDING";

                    rows.add(new HistoryRow(date, entryTime, exitTime, slotCode, zone, duration, fee, payStatus));
                }
            }
            rows.sort((a, b) -> b.entryTime().compareTo(a.entryTime()));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return rows;
    }
}