package ui.user;

import dao.ParkingTransactionDAO;
import dao.UserAccountDAO;
import dao.VehicleDAO;
import dao.FeeScheduleDAO;
import model.FeeSchedule;
import model.AppState;
import model.ParkingTransaction;
import model.UserAccount;
import model.Vehicle;
import ui.shared.SidebarPanel;
import ui.shared.SlotGridPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SPARCS — Redesigned User Dashboard.
 *
 * Top row (4 stat cards):
 *   1. Parking Status    — current slot or "Not Parked"
 *   2. Wallet Balance    — loaded from DB
 *   3. RFID Scans Today  — count of entry transactions today
 *   4. Sessions / Month  — count of parking sessions this calendar month
 *
 * Bottom row (2 panels):
 *   LEFT  — Recent Activity  (last 5 entry/exit events)
 *   RIGHT — RFID Card info + Fee Schedule (replaces standalone Fee Schedule screen on dashboard)
 */
public class UserDashboardScreen {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    // =========================================================================
    // Build
    // =========================================================================

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "USER", "USER_DASHBOARD"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        // ── Top bar ──────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        String greeting = "HELLO, " + (state.currentUsername.isEmpty() ? "USER" : state.currentUsername.toUpperCase()) + "!";
        topBar.add(UIFactory.lbl(greeting, Font.BOLD, 20, C_WHITE), BorderLayout.WEST);

        JLabel statusBadge = makeBadge("● NOT PARKED", C_MUTED);
        topBar.add(statusBadge, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // ── 4 Stat cards ─────────────────────────────────────────────────────
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));

        JPanel parkingStatusCard = UIFactory.statCard("Parking Status",   "—",                       C_ACCENT);
        JPanel walletCard        = UIFactory.statCard("Wallet Balance",   fetchWalletBalance(state), C_PINK);
        JPanel rfidScansCard     = UIFactory.statCard("RFID Scans Today", "—",                       C_RESERVED);
        JPanel sessionsCard      = UIFactory.statCard("Sessions / Month", "—",                       C_AVAILABLE);

        statsRow.add(parkingStatusCard);
        statsRow.add(walletCard);
        statsRow.add(rfidScansCard);
        statsRow.add(sessionsCard);

        // ── Bottom body ───────────────────────────────────────────────────────
        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 20, 20, 20));

        // LEFT — Recent Activity
        JPanel actCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        actCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        actCard.add(UIFactory.lbl("RECENT ACTIVITY", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);
        JPanel actList = new JPanel();
        actList.setLayout(new BoxLayout(actList, BoxLayout.Y_AXIS));
        actList.setOpaque(false);
        // Activity rows will be populated dynamically from database
        actCard.add(actList, BorderLayout.CENTER);
        body.add(actCard);

        // RIGHT — RFID Card + Fee Schedule
        JPanel rightCard = UIFactory.cardPanel(new BorderLayout());
        rightCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel rightContent = new JPanel();
        rightContent.setLayout(new BoxLayout(rightContent, BoxLayout.Y_AXIS));
        rightContent.setOpaque(false);

        // RFID section
        rightContent.add(UIFactory.lbl("RFID CARD", Font.BOLD, 12, C_MUTED));
        rightContent.add(Box.createVerticalStrut(10));

        JPanel rfidRow = new JPanel(new BorderLayout(10, 0));
        rfidRow.setOpaque(false);
        rfidRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JPanel rfidChip = new JPanel(new BorderLayout(0, 2));
        rfidChip.setBackground(new Color(30, 25, 70));
        rfidChip.setBorder(new EmptyBorder(8, 12, 8, 12));
        JLabel rfidNum   = UIFactory.lbl("—",        Font.BOLD,  12, C_ACCENT);
        JLabel rfidLabel = UIFactory.lbl("Card UID", Font.PLAIN, 10, C_MUTED);
        rfidChip.add(rfidNum,   BorderLayout.NORTH);
        rfidChip.add(rfidLabel, BorderLayout.SOUTH);

        JLabel rfidStatus = makeBadge("● ACTIVE", C_AVAILABLE);
        rfidStatus.setHorizontalAlignment(SwingConstants.RIGHT);
        rfidStatus.setVerticalAlignment(SwingConstants.CENTER);

        rfidRow.add(rfidChip,   BorderLayout.CENTER);
        rfidRow.add(rfidStatus, BorderLayout.EAST);
        rightContent.add(rfidRow);

        rightContent.add(Box.createVerticalStrut(14));
        rightContent.add(thinDivider());
        rightContent.add(Box.createVerticalStrut(14));

        // Fee schedule section
        rightContent.add(UIFactory.lbl("FEE SCHEDULE", Font.BOLD, 12, C_MUTED));
        rightContent.add(Box.createVerticalStrut(10));

        JPanel feeList = new JPanel();
        feeList.setLayout(new BoxLayout(feeList, BoxLayout.Y_AXIS));
        feeList.setOpaque(false);
        addFeeRows(feeList, state);
        rightContent.add(feeList);

        rightCard.add(rightContent, BorderLayout.CENTER);
        body.add(rightCard);

        // ── Assemble ─────────────────────────────────────────────────────────
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body,     BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        // ── Auto-refresh when screen becomes visible ──────────────────────────
        root.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                refreshDashboard(state, walletCard, parkingStatusCard,
                        rfidScansCard, sessionsCard, actList,
                        rfidNum, rfidStatus, statusBadge);
            }
        });

        // Initial load
        refreshDashboard(state, walletCard, parkingStatusCard,
                rfidScansCard, sessionsCard, actList,
                rfidNum, rfidStatus, statusBadge);

        return root;
    }

    // =========================================================================
    // Refresh
    // =========================================================================

    private static void refreshDashboard(AppState state,
                                          JPanel walletCard,
                                          JPanel parkingStatusCard,
                                          JPanel rfidScansCard,
                                          JPanel sessionsCard,
                                          JPanel actList,
                                          JLabel rfidNum,
                                          JLabel rfidStatus,
                                          JLabel statusBadge) {
        SwingUtilities.invokeLater(() -> {
            // Wallet balance
            setStatCardValue(walletCard, fetchWalletBalance(state));

            // All other live data
            DashboardData data = loadDashboardData(state);

            // Parking status
            if (data.currentSlot != null) {
                setStatCardValue(parkingStatusCard, "Slot " + data.currentSlot);
                statusBadge.setText("● PARKED");
                statusBadge.setForeground(C_AVAILABLE);
            } else {
                setStatCardValue(parkingStatusCard, "Not Parked");
                statusBadge.setText("● NOT PARKED");
                statusBadge.setForeground(C_MUTED);
            }

            setStatCardValue(rfidScansCard, String.valueOf(data.rfidScansToday));
            setStatCardValue(sessionsCard,  String.valueOf(data.sessionsThisMonth));

            // RFID card
            if (data.rfidTag != null && !data.rfidTag.isBlank()) {
                rfidNum.setText(data.rfidTag);
                rfidStatus.setText("● ACTIVE");
                rfidStatus.setForeground(C_AVAILABLE);
            } else {
                rfidNum.setText("No card linked");
                rfidStatus.setText("● INACTIVE");
                rfidStatus.setForeground(C_MUTED);
            }

            // Recent activity
            actList.removeAll();
            if (data.recentActivity.isEmpty()) {
                JLabel empty = UIFactory.lbl("No recent activity", Font.PLAIN, 12, C_MUTED);
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                actList.add(empty);
            } else {
                for (ActivityRow row : data.recentActivity) {
                    actList.add(buildActivityRow(row));
                    actList.add(Box.createVerticalStrut(2));
                }
            }
            actList.revalidate();
            actList.repaint();
        });
    }

    // =========================================================================
    // Data loading
    // =========================================================================

    private static class DashboardData {
        String currentSlot       = null;
        String rfidTag           = null;
        int    rfidScansToday    = 0;
        int    sessionsThisMonth = 0;
        List<ActivityRow> recentActivity = new ArrayList<>();
    }

    private record ActivityRow(boolean isEntry, String slot, String timeLabel, String fee) {}

    private static DashboardData loadDashboardData(AppState state) {
        DashboardData d = new DashboardData();
        if (state.currentUsername == null || state.currentUsername.isEmpty()) return d;

        try {
            UserAccountDAO userDAO = new UserAccountDAO();
            Optional<UserAccount> userOpt = userDAO.findByUsername(state.currentUsername);
            if (userOpt.isEmpty()) return d;
            UserAccount user = userOpt.get();

            VehicleDAO vehicleDAO = new VehicleDAO();
            List<Vehicle> vehicles = vehicleDAO.findAllByUserId(user.getUserId());
            if (vehicles.isEmpty()) return d;

            // RFID tag — first linked vehicle
            dao.RFIDMappingDAO rfidDAO = new dao.RFIDMappingDAO();
            for (Vehicle v : vehicles) {
                Optional<model.RFIDMapping> rfidOpt = rfidDAO.findByVehicleId(v.getVehicleId());
                if (rfidOpt.isPresent()) { d.rfidTag = rfidOpt.get().getRfidTag(); break; }
            }

            ParkingTransactionDAO txnDAO  = new ParkingTransactionDAO();
            dao.ParkingSlotDAO    slotDAO = new dao.ParkingSlotDAO();
            LocalDate today      = LocalDate.now();
            YearMonth thisMonth  = YearMonth.now();
            List<ActivityRow> allRows = new ArrayList<>();

            for (Vehicle v : vehicles) {
                List<ParkingTransaction> txns = txnDAO.findByVehicleId(v.getVehicleId());
                for (ParkingTransaction txn : txns) {

                    // Currently parked?
                    if (!txn.isCompleted() && txn.getExitTime() == null) {
                        Optional<model.ParkingSlot> slotOpt = slotDAO.findById(txn.getSlotId());
                        d.currentSlot = slotOpt.map(model.ParkingSlot::getSlotCode).orElse("Active");
                    }

                    // RFID scans today (entry == today)
                    if (txn.getEntryTime() != null &&
                            txn.getEntryTime().toLocalDate().equals(today)) {
                        d.rfidScansToday++;
                    }

                    // Sessions this month
                    if (txn.getEntryTime() != null &&
                            YearMonth.from(txn.getEntryTime()).equals(thisMonth)) {
                        d.sessionsThisMonth++;
                    }

                    // Activity rows
                    Optional<model.ParkingSlot> slotOpt = slotDAO.findById(txn.getSlotId());
                    String slotCode = slotOpt.map(model.ParkingSlot::getSlotCode).orElse("—");

                    if (txn.getEntryTime() != null) {
                        String label = formatTimeLabel(txn.getEntryTime().toLocalDate(), today,
                                txn.getEntryTime().format(TIME_FMT));
                        allRows.add(new ActivityRow(true, slotCode, label, "—"));
                    }
                    if (txn.getExitTime() != null) {
                        String fee = txn.getCalculatedFee() != null
                                ? "₱" + String.format("%.2f", txn.getCalculatedFee()) : "—";
                        String label = formatTimeLabel(txn.getExitTime().toLocalDate(), today,
                                txn.getExitTime().format(TIME_FMT));
                        allRows.add(new ActivityRow(false, slotCode, label, fee));
                    }
                }
            }

            // Newest first, cap at 5
            allRows.sort((a, b) -> b.timeLabel().compareTo(a.timeLabel()));
            d.recentActivity = allRows.subList(0, Math.min(5, allRows.size()));

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return d;
    }

    // =========================================================================
    // Fee schedule
    // =========================================================================

    /**
     * Loads the current active fee schedule from FeeScheduleDAO.
     * Falls back to hardcoded defaults if no active schedule is found in DB.
     */
    private static void addFeeRows(JPanel feeList, AppState state) {
        String ratePerHour = "₱30";
        String ratePerDay  = "₱150";
        int    graceMins   = 0;

        try {
            FeeScheduleDAO feeDAO = new FeeScheduleDAO();
            Optional<FeeSchedule> activeOpt = feeDAO.findCurrentActive();
            if (activeOpt.isPresent()) {
                FeeSchedule fs = activeOpt.get();
                ratePerHour = "₱" + fs.getRatePerHour().toPlainString();
                ratePerDay  = "₱" + fs.getRatePerDay().toPlainString();
                graceMins   = fs.getGracePeriodMinutes();
            }
        } catch (Exception ignored) {}

        feeList.add(buildFeeRow("Rate per hour", ratePerHour));
        feeList.add(Box.createVerticalStrut(7));
        feeList.add(buildFeeRow("Rate per day",  ratePerDay));
        feeList.add(Box.createVerticalStrut(7));
        feeList.add(buildFeeRow("Grace period",  graceMins + " min"));
        feeList.add(Box.createVerticalStrut(7));
    }

    private static JPanel buildFeeRow(String label, String rate) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JLabel lbl = UIFactory.lbl(label, Font.PLAIN, 12, new Color(160, 157, 192));
        JLabel val = UIFactory.lbl(rate,  Font.BOLD,  12, C_WHITE);
        val.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    // =========================================================================
    // Activity row builder
    // =========================================================================

    private static JPanel buildActivityRow(ActivityRow row) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(new EmptyBorder(6, 0, 6, 0));

        Color iconBg  = row.isEntry() ? new Color(27, 58, 45)  : new Color(58, 27, 27);
        Color iconClr = row.isEntry() ? C_AVAILABLE             : new Color(255, 107, 107);
        JLabel icon = new JLabel(row.isEntry() ? "▲" : "▼", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(iconBg);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icon.setForeground(iconClr);
        icon.setFont(new Font("Dialog", Font.BOLD, 10));
        icon.setPreferredSize(new Dimension(28, 28));
        icon.setMinimumSize(new Dimension(28, 28));
        icon.setMaximumSize(new Dimension(28, 28));
        icon.setOpaque(false);

        // Wrap icon in a fixed panel so BorderLayout.WEST doesn't stretch it vertically
        JPanel iconWrap = new JPanel(new GridBagLayout());
        iconWrap.setOpaque(false);
        iconWrap.setPreferredSize(new Dimension(36, 36));
        iconWrap.setMinimumSize(new Dimension(36, 36));
        iconWrap.setMaximumSize(new Dimension(36, 36));
        iconWrap.add(icon);

        JPanel info = new JPanel(new BorderLayout(0, 2));
        info.setOpaque(false);
        String title = (row.isEntry() ? "Entry" : "Exit") + " — Slot " + row.slot();
        info.add(UIFactory.lbl(title,          Font.BOLD,  12, new Color(197, 194, 224)), BorderLayout.NORTH);
        info.add(UIFactory.lbl(row.timeLabel(), Font.PLAIN, 11, C_MUTED),                 BorderLayout.SOUTH);

        JLabel feeLbl = UIFactory.lbl(row.fee(), Font.BOLD, 12, C_ACCENT);
        feeLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        p.add(iconWrap, BorderLayout.WEST);
        p.add(info,     BorderLayout.CENTER);
        p.add(feeLbl,   BorderLayout.EAST);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(p,             BorderLayout.CENTER);
        wrap.add(thinDivider(), BorderLayout.SOUTH);
        return wrap;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Queries the DB for the current user's wallet balance. */
    private static String fetchWalletBalance(AppState state) {
        if (state.currentUsername == null || state.currentUsername.isEmpty()) return "—";
        try {
            UserAccountDAO userDAO = new UserAccountDAO();
            Optional<UserAccount> userOpt = userDAO.findByUsername(state.currentUsername);
            if (userOpt.isEmpty()) return "—";
            BigDecimal bal = userDAO.getWalletBalance(userOpt.get().getUserId());
            return "₱" + bal.toPlainString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "—";
        }
    }

    /** Updates the CENTER label of a UIFactory.statCard(). */
    private static void setStatCardValue(JPanel card, String value) {
        Component c = ((BorderLayout) card.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (c instanceof JLabel lbl) lbl.setText(value);
        card.revalidate();
        card.repaint();
    }

    private static String formatTimeLabel(LocalDate date, LocalDate today, String timeStr) {
        if (date.equals(today))              return "Today, "     + timeStr;
        if (date.equals(today.minusDays(1))) return "Yesterday, " + timeStr;
        return date.format(DateTimeFormatter.ofPattern("MMM d")) + ", " + timeStr;
    }

    private static JLabel makeBadge(String text, Color color) {
        JLabel lbl = UIFactory.lbl(text, Font.PLAIN, 11, color);
        lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
        return lbl;
    }

    private static JSeparator thinDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(60, 50, 100));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }
}