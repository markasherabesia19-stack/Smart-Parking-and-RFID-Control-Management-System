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
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
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

    // ── Column definitions ─────────────────────────────────────────────────────
    private static final String[] COLS = {
        "DATE", "ENTRY TIME", "EXIT TIME", "SLOT", "ZONE", "DURATION", "FEE", "STATUS"
    };

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "USER", "USER_HISTORY"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        // ── Top bar ────────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(0x3D3060)),
                new EmptyBorder(14, 24, 14, 24)
        ));

        JPanel topLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topLeft.setOpaque(false);
        JLabel dot = new JLabel("\u25CF");
        dot.setForeground(C_ACCENT);
        dot.setFont(new Font("SansSerif", Font.PLAIN, 10));
        JLabel title = new JLabel("PARKING HISTORY");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(C_WHITE);
        topLeft.add(dot);
        topLeft.add(title);
        topBar.add(topLeft, BorderLayout.WEST);

        JButton refreshBtn = buildRefreshButton();
        topBar.add(refreshBtn, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // ── Stats row (will be updated on refresh) ─────────────────────────────
        JLabel totalSessionsVal = statValue("—");
        JLabel totalHoursVal    = statValue("—");
        JLabel totalFeesVal     = statValue("—");

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 24, 12, 24));
        statsRow.add(buildStatCard("Total Sessions", totalSessionsVal, C_ACCENT));
        statsRow.add(buildStatCard("Total Hours",    totalHoursVal,    C_AVAILABLE));
        statsRow.add(buildStatCard("Total Fees Paid",totalFeesVal,     new Color(0xF59E0B)));

        // ── Table ──────────────────────────────────────────────────────────────
        DefaultTableModel tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(C_BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0x3D3060)));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 24, 24, 24));
        body.add(scroll, BorderLayout.CENTER);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body, BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);

        // ── Refresh action ─────────────────────────────────────────────────────
        refreshBtn.addActionListener(e -> {
            tableModel.setRowCount(0);

            List<HistoryRow> rows = loadHistory(state);

            // Compute summary stats
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

            // Update stat labels
            totalSessionsVal.setText(String.valueOf(totalSessions));
            long hours   = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            totalHoursVal.setText(hours + "h " + minutes + "m");
            totalFeesVal.setText("₱" + String.format("%.2f", totalFees.doubleValue()));

            if (rows.isEmpty()) {
                tableModel.addRow(new Object[]{"No history found","","","","","","",""});
            }
        });

        // ── Auto-refresh on show ───────────────────────────────────────────────
        root.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) {
                refreshBtn.doClick();
            }
        });

        refreshBtn.doClick();

        root.add(content, BorderLayout.CENTER);
        return root;
    }

    // ── Data loading ───────────────────────────────────────────────────────────
    private record HistoryRow(
            String date,
            String entryTime,
            String exitTime,
            String slotCode,
            String zone,
            Integer durationMinutes,
            BigDecimal fee,
            String paymentStatus
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

                    // Resolve fee if missing
                    BigDecimal fee = txn.getCalculatedFee();
                    if (fee == null) {
                        try { fee = feeService.calculateFee(txn); }
                        catch (Exception ex) { fee = BigDecimal.ZERO; }
                    }

                    // Resolve duration if missing
                    Integer duration = txn.getDurationMinutes();
                    if (duration == null && txn.getEntryTime() != null) {
                        duration = (int) FeeCalculationService.calculateDurationMinutes(
                                txn.getEntryTime(), txn.getExitTime());
                    }

                    // Resolve slot info
                    String slotCode = "—";
                    String zone     = "—";
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

            // Sort by entry time descending (most recent first)
            rows.sort((a, b) -> b.entryTime().compareTo(a.entryTime()));

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return rows;
    }

    // ── Table styling ──────────────────────────────────────────────────────────
    private static void styleTable(JTable t) {
        t.setBackground(C_BG_CARD);
        t.setForeground(C_WHITE);
        t.setFont(new Font("Monospaced", Font.PLAIN, 12));
        t.setRowHeight(34);
        t.setGridColor(new Color(0x2A2050));
        t.setSelectionBackground(C_PURPLE);
        t.setSelectionForeground(C_WHITE);
        t.setOpaque(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));

        // Header
        JTableHeader header = t.getTableHeader();
        header.setBackground(C_BG_PANEL);
        header.setForeground(C_MUTED);
        header.setFont(new Font("SansSerif", Font.BOLD, 10));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x3D3060)));
        header.setPreferredSize(new Dimension(0, 36));
        header.setReorderingAllowed(false);

        // Custom cell renderer for status column and alternating rows
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setOpaque(true);
                setBorder(new EmptyBorder(0, 12, 0, 12));

                // Alternating row background
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? C_BG_CARD : new Color(0x1A1535));
                }

                // Status column coloring (last column)
                if (col == COLS.length - 1 && value != null) {
                    String status = value.toString();
                    if (status.equalsIgnoreCase("PAID")) {
                        setForeground(C_AVAILABLE);
                    } else if (status.equalsIgnoreCase("PENDING")) {
                        setForeground(new Color(0xF59E0B));
                    } else {
                        setForeground(C_MUTED);
                    }
                    setFont(new Font("SansSerif", Font.BOLD, 11));
                } else {
                    setForeground(isSelected ? C_WHITE : C_WHITE);
                    setFont(new Font("Monospaced", Font.PLAIN, 12));
                }

                // Fee column — right-align
                if (col == COLS.length - 2) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }

                return this;
            }
        });

        // Column widths
        int[] widths = {110, 135, 135, 60, 50, 80, 90, 90};
        for (int i = 0; i < widths.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    // ── Stat card helpers ──────────────────────────────────────────────────────
    private static JPanel buildStatCard(String label, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(0x3D3060));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new CompoundBorder(
                new MatteBorder(0, 3, 0, 0, accent),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setForeground(C_MUTED);

        card.add(lbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private static JLabel statValue(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 22));
        lbl.setForeground(C_WHITE);
        return lbl;
    }

    // ── Refresh button ─────────────────────────────────────────────────────────
    private static JButton buildRefreshButton() {
        JButton btn = new JButton("↻  REFRESH") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(C_ACCENT.getRed(), C_ACCENT.getGreen(), C_ACCENT.getBlue(), 20));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setForeground(C_ACCENT);
        btn.setBackground(new Color(0, 0, 0, 0));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(C_ACCENT, 1),
                new EmptyBorder(6, 14, 6, 14)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(120, 34));
        return btn;
    }
}