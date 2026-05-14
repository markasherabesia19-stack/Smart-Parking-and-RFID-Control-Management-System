package ui.user;

import dao.AuditLogDAO;
import dao.ParkingSlotDAO;
import dao.ParkingTransactionDAO;
import dao.UserAccountDAO;
import dao.VehicleDAO;
import model.AuditLog;
import model.AppState;
import model.ParkingSlot;
import model.ParkingTransaction;
import model.UserAccount;
import model.Vehicle;
import ui.shared.SidebarPanel;
import ui.shared.SlotGridPanel;
import util.DialogUtil;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SPARCS — Redesigned User Dashboard.
 *
 * Top row (3 stat cards):
 *   1. Wallet Balance    — loaded from DB
 *   2. RFID Scans Today  — count of entry transactions today
 *   3. Sessions / Month  — count of parking sessions this calendar month
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
        content.add(topBar, BorderLayout.NORTH);

        // ── 3 Stat cards ─────────────────────────────────────────────────────
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));

        JPanel walletCard        = UIFactory.statCard("Wallet Balance",   fetchWalletBalance(state), C_PINK);
        JPanel rfidScansCard     = UIFactory.statCard("RFID Scans Today", "—",                       C_RESERVED);
        JPanel sessionsCard      = UIFactory.statCard("Sessions / Month", "—",                       C_AVAILABLE);

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

        // RIGHT — All RFID Cards + Fee Schedule
        JPanel rightCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        rightCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel rightContent = new JPanel();
        rightContent.setLayout(new BoxLayout(rightContent, BoxLayout.Y_AXIS));
        rightContent.setOpaque(false);

        // RFID section
        JLabel rfidCardsLbl = UIFactory.lbl("RFID CARDS", Font.BOLD, 12, C_MUTED);
        rfidCardsLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightContent.add(rfidCardsLbl);
        rightContent.add(Box.createVerticalStrut(10));

        JPanel rfidListWrapper = new JPanel();
        rfidListWrapper.setLayout(new BoxLayout(rfidListWrapper, BoxLayout.Y_AXIS));
        rfidListWrapper.setOpaque(false);
        rfidListWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        rfidListWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel rfidList = new JPanel();
        rfidList.setLayout(new BoxLayout(rfidList, BoxLayout.Y_AXIS));
        rfidList.setOpaque(false);
        rfidList.setAlignmentX(Component.LEFT_ALIGNMENT);
        rfidList.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        rfidListWrapper.add(rfidList);
        rfidListWrapper.add(Box.createVerticalGlue());
        rightContent.add(rfidListWrapper);
        rightContent.add(Box.createVerticalStrut(14));
        rightContent.add(thinDivider());
        rightContent.add(Box.createVerticalStrut(14));

        // Slot reservation section
        JLabel reserveLbl = UIFactory.lbl("RESERVE A SLOT", Font.BOLD, 11, C_MUTED);
        reserveLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightContent.add(reserveLbl);
        rightContent.add(Box.createVerticalStrut(6));

        JPanel reservationForm = new JPanel();
        reservationForm.setLayout(new BoxLayout(reservationForm, BoxLayout.Y_AXIS));
        reservationForm.setOpaque(false);
        reservationForm.setAlignmentX(Component.LEFT_ALIGNMENT);
        reservationForm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        // Plate/RFID field
        JLabel plateLbl = UIFactory.lbl("Plate / RFID", Font.PLAIN, 9, C_MUTED);
        plateLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        reservationForm.add(plateLbl);
        JTextField plateField = new JTextField();
        plateField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        plateField.setMinimumSize(new Dimension(100, 22));
        plateField.setAlignmentX(Component.LEFT_ALIGNMENT);
        plateField.setBackground(new Color(40, 35, 80));
        plateField.setForeground(C_WHITE);
        plateField.setBorder(new EmptyBorder(4, 6, 4, 6));
        reservationForm.add(plateField);
        reservationForm.add(Box.createVerticalStrut(5));

        // Slot number field
        JLabel slotLbl = UIFactory.lbl("Slot #", Font.PLAIN, 9, C_MUTED);
        slotLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        reservationForm.add(slotLbl);
        JTextField slotField = new JTextField();
        slotField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        slotField.setMinimumSize(new Dimension(100, 22));
        slotField.setAlignmentX(Component.LEFT_ALIGNMENT);
        slotField.setBackground(new Color(40, 35, 80));
        slotField.setForeground(C_WHITE);
        slotField.setBorder(new EmptyBorder(4, 6, 4, 6));
        reservationForm.add(slotField);
        reservationForm.add(Box.createVerticalStrut(5));

        // Date & Time row
        JPanel dateTimeRow = new JPanel(new GridLayout(1, 2, 6, 0));
        dateTimeRow.setOpaque(false);
        dateTimeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        dateTimeRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel datePanel = new JPanel();
        datePanel.setLayout(new BoxLayout(datePanel, BoxLayout.Y_AXIS));
        datePanel.setOpaque(false);
        datePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        datePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        JLabel dateLbl = UIFactory.lbl("Date", Font.PLAIN, 9, C_MUTED);
        dateLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        datePanel.add(dateLbl);
        JTextField dateField = new JTextField("yyyy-MM-dd");
        dateField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        dateField.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateField.setBackground(new Color(40, 35, 80));
        dateField.setForeground(C_WHITE);
        dateField.setBorder(new EmptyBorder(4, 6, 4, 6));
        datePanel.add(dateField);

        JPanel timePanel = new JPanel();
        timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.Y_AXIS));
        timePanel.setOpaque(false);
        timePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        timePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        JLabel timeLbl = UIFactory.lbl("Time", Font.PLAIN, 9, C_MUTED);
        timeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        timePanel.add(timeLbl);
        JTextField timeField = new JTextField("HH:mm");
        timeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        timeField.setAlignmentX(Component.LEFT_ALIGNMENT);
        timeField.setBackground(new Color(40, 35, 80));
        timeField.setForeground(C_WHITE);
        timeField.setBorder(new EmptyBorder(4, 6, 4, 6));
        timePanel.add(timeField);

        dateTimeRow.add(datePanel);
        dateTimeRow.add(timePanel);
        reservationForm.add(dateTimeRow);
        reservationForm.add(Box.createVerticalStrut(6));

        // Reserve button — wired to DB reservation logic
        JButton reserveBtn = UIFactory.gradientButton("RESERVE");
        reserveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        reserveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        reserveBtn.addActionListener(e -> {
            String plateOrRfid = plateField.getText().trim();
            String slotCode    = slotField.getText().trim().toUpperCase();
            String dateStr     = dateField.getText().trim();
            String timeStr     = timeField.getText().trim();

            // ── Basic validation ─────────────────────────────────────────────
            if (plateOrRfid.isEmpty() || slotCode.isEmpty()
                    || dateStr.isEmpty() || timeStr.isEmpty()
                    || "yyyy-MM-dd".equals(dateStr) || "HH:mm".equals(timeStr)) {
                DialogUtil.showMessageDialog(null,
                    "Please fill in all fields (Plate/RFID, Slot, Date, Time) before reserving.",
                    "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                VehicleDAO vehicleDAO       = new VehicleDAO();
                ParkingSlotDAO slotDAO      = new ParkingSlotDAO();
                AuditLogDAO auditDAO        = new AuditLogDAO();
                ParkingTransactionDAO txDAO = new ParkingTransactionDAO();

                // ── Resolve vehicle by plate number ──────────────────────────
                // Also accept the RFID tag format "SPARCS-<plate>" used at registration
                String lookupPlate = plateOrRfid;
                if (plateOrRfid.toUpperCase().startsWith("SPARCS-")) {
                    lookupPlate = plateOrRfid.substring(7); // strip "SPARCS-" prefix
                }
                Optional<Vehicle> vehicleOpt = vehicleDAO.findByPlateNumber(lookupPlate);
                if (vehicleOpt.isEmpty()) {
                    DialogUtil.showMessageDialog(null,
                        "No vehicle found for plate / RFID: " + plateOrRfid + ".\n"
                        + "Make sure the vehicle is registered in the system.",
                        "Vehicle Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Vehicle vehicle = vehicleOpt.get();

                // ── Check vehicle not already parked / reserved ──────────────
                List<ParkingTransaction> activeTxns = txDAO.findByVehicleId(vehicle.getVehicleId());
                boolean alreadyActive = activeTxns.stream()
                    .anyMatch(t -> "IN_PROGRESS".equals(t.getTransactionStatus())
                               || "RESERVED".equals(t.getTransactionStatus()));
                if (alreadyActive) {
                    DialogUtil.showMessageDialog(null,
                        "Vehicle " + vehicle.getPlateNumber()
                        + " already has an active parking session or pending reservation.",
                        "Already Active", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // ── Validate slot ────────────────────────────────────────────
                Optional<ParkingSlot> slotOpt = slotDAO.findBySlotCode(slotCode);
                if (slotOpt.isEmpty()) {
                    DialogUtil.showMessageDialog(null,
                        "Slot \"" + slotCode + "\" not found. Check the slot code (e.g. A-01).",
                        "Slot Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                ParkingSlot slot = slotOpt.get();
                String slotStatus = slot.getStatus() != null ? slot.getStatus().toUpperCase() : "";
                if ("OCCUPIED".equals(slotStatus) || "RESERVED".equals(slotStatus)) {
                    DialogUtil.showMessageDialog(null,
                        "Slot " + slotCode + " is currently "
                        + slotStatus.charAt(0) + slotStatus.substring(1).toLowerCase()
                        + " and cannot be reserved.",
                        "Slot Unavailable", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // ── Mark slot as RESERVED in DB ──────────────────────────────
                slotDAO.reserveSlot(slot.getSlotId());
                slotDAO.updateCurrentVehicle(slot.getSlotId(), vehicle.getVehicleId());
                vehicleDAO.updateParkingStatus(vehicle.getVehicleId(), "Reserved");

                // ── Create a RESERVED transaction record ─────────────────────
                ParkingTransaction tx = new ParkingTransaction(
                    vehicle.getVehicleId(), slot.getSlotId(), LocalDateTime.now());
                tx.setTransactionStatus("RESERVED");
                txDAO.create(tx);

                // ── Audit log — same pattern as ENTRY/EXIT ───────────────────
                AuditLog log = new AuditLog();
                log.setAction("RESERVE");
                log.setEntityType("PARKING_SLOT");
                log.setEntityId(slot.getSlotId());
                log.setNewValue(vehicle.getPlateNumber());
                log.setOldValue("plate=" + vehicle.getPlateNumber() + " slot=" + slotCode);
                log.setIpAddress("localhost");
                auditDAO.create(log);

                // ── Notify all slot-change listeners (admin map, dashboard) ──
                state.notifySlotChange();

                // ── Clear form ───────────────────────────────────────────────
                plateField.setText("");
                slotField.setText("");
                dateField.setText("yyyy-MM-dd");
                timeField.setText("HH:mm");

                DialogUtil.showMessageDialog(null,
                    "Slot " + slotCode + " has been reserved for "
                    + vehicle.getPlateNumber() + ".\n"
                    + "It will appear as RESERVED (yellow) on the parking map.",
                    "Reservation Confirmed", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                ex.printStackTrace();
                DialogUtil.showMessageDialog(null,
                    "Reservation failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        reservationForm.add(reserveBtn);

        rightContent.add(reservationForm);
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
                refreshDashboard(state, walletCard,
                        rfidScansCard, sessionsCard, actList, rfidList);
            }
        });

        // ── Real-time refresh on any slot change (reserve, cancel, entry, exit) ──
        state.addSlotChangeListener(() -> SwingUtilities.invokeLater(() ->
                refreshDashboard(state, walletCard,
                        rfidScansCard, sessionsCard, actList, rfidList)));

        // Initial load
        refreshDashboard(state, walletCard,
                rfidScansCard, sessionsCard, actList, rfidList);

        return root;
    }

    // =========================================================================
    // Refresh
    // =========================================================================

    private static void refreshDashboard(AppState state,
                                          JPanel walletCard,
                                          JPanel rfidScansCard,
                                          JPanel sessionsCard,
                                          JPanel actList,
                                          JPanel rfidList) {
        SwingUtilities.invokeLater(() -> {
            // Wallet balance
            setStatCardValue(walletCard, fetchWalletBalance(state));

            // All other live data
            DashboardData data = loadDashboardData(state);

            setStatCardValue(rfidScansCard, String.valueOf(data.rfidScansToday));
            setStatCardValue(sessionsCard,  String.valueOf(data.sessionsThisMonth));

            // Refresh callback — rebuilds the RFID list after a cancellation
            Runnable refreshRfid = () -> refreshDashboard(state, walletCard,
                    rfidScansCard, sessionsCard, actList, rfidList);

            // RFID cards list - show all vehicles
            rfidList.removeAll();
            if (data.vehicleRFIDInfo.isEmpty()) {
                JLabel empty = UIFactory.lbl("No vehicles linked", Font.PLAIN, 12, C_MUTED);
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                rfidList.add(empty);
            } else {
                for (int i = 0; i < data.vehicleRFIDInfo.size(); i++) {
                    VehicleRFIDInfo info = data.vehicleRFIDInfo.get(i);
                    rfidList.add(buildVehicleRFIDCard(info, state, refreshRfid));
                    if (i < data.vehicleRFIDInfo.size() - 1) {
                        rfidList.add(Box.createVerticalStrut(10));
                    }
                }
            }
            rfidList.revalidate();
            rfidList.repaint();

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
        int    rfidScansToday        = 0;
        int    sessionsThisMonth     = 0;
        List<VehicleRFIDInfo> vehicleRFIDInfo = new ArrayList<>();
        List<ActivityRow> recentActivity = new ArrayList<>();
    }

    private record ActivityRow(boolean isEntry, String slot, String timeLabel, String fee) {}

    /**
     * status: "PARKED" | "RESERVED" | "INACTIVE"
     */
    private record VehicleRFIDInfo(String rfidTag, String vehicleInfo, String status, String parkedSlot) {
        boolean isParked()   { return "PARKED".equals(status); }
        boolean isReserved() { return "RESERVED".equals(status); }
        boolean isActive()   { return isParked() || isReserved(); }
    }

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

            // Collect all RFID cards with parking status
            dao.RFIDMappingDAO rfidDAO = new dao.RFIDMappingDAO();
            for (Vehicle v : vehicles) {
                Optional<model.RFIDMapping> rfidOpt = rfidDAO.findByVehicleId(v.getVehicleId());
                String rfidTag = rfidOpt.map(model.RFIDMapping::getRfidTag).orElse("No card");
                String vehicleInfo = v.getPlateNumber() != null ? v.getPlateNumber() : "Vehicle";
                
                d.vehicleRFIDInfo.add(new VehicleRFIDInfo(rfidTag, vehicleInfo, "INACTIVE", ""));
            }

            ParkingTransactionDAO txnDAO  = new ParkingTransactionDAO();
            dao.ParkingSlotDAO    slotDAO = new dao.ParkingSlotDAO();
            LocalDate today      = LocalDate.now();
            YearMonth thisMonth  = YearMonth.now();
            List<ActivityRow> allRows = new ArrayList<>();

            // Update parking status for each vehicle
            for (int i = 0; i < d.vehicleRFIDInfo.size(); i++) {
                VehicleRFIDInfo info = d.vehicleRFIDInfo.get(i);
                Vehicle v = vehicles.get(i);
                List<ParkingTransaction> txns = txnDAO.findByVehicleId(v.getVehicleId());
                for (ParkingTransaction txn : txns) {

                    // Currently parked or reserved?
                    // IN_PROGRESS (parked) always takes priority over RESERVED.
                    // Once a vehicle is marked PARKED, a stale RESERVED transaction
                    // in the same list must not overwrite it.
                    if (!txn.isCompleted() && txn.getExitTime() == null) {
                        String txStatus = txn.getTransactionStatus();
                        boolean isParkedTx   = "IN_PROGRESS".equalsIgnoreCase(txStatus);
                        boolean isReservedTx = "RESERVED".equalsIgnoreCase(txStatus);
                        // Skip RESERVED if we already know the vehicle is PARKED
                        if (isReservedTx && "PARKED".equals(info.status())) continue;
                        if (isParkedTx || isReservedTx) {
                            Optional<model.ParkingSlot> slotOpt = slotDAO.findById(txn.getSlotId());
                            if (slotOpt.isPresent()) {
                                String slotCode     = slotOpt.get().getSlotCode();
                                String vehicleStatus = isParkedTx ? "PARKED" : "RESERVED";
                                d.vehicleRFIDInfo.set(i, new VehicleRFIDInfo(
                                        info.rfidTag(), info.vehicleInfo(), vehicleStatus, slotCode));
                                info = d.vehicleRFIDInfo.get(i);
                            }
                        }
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
    // Activity row builder
    // =========================================================================

    // =========================================================================
    // Vehicle RFID Card builder
    // =========================================================================

    private static JPanel buildVehicleRFIDCard(VehicleRFIDInfo info,
                                               AppState state,
                                               Runnable onCancelRefresh) {
        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setOpaque(false);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, info.isReserved() ? 62 : 52));
        card.setMinimumSize(new Dimension(200, info.isReserved() ? 62 : 52));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBorder(new EmptyBorder(6, 0, 6, 0));

        // RFID Chip — no fixed preferredSize so full tag text is always readable
        JPanel rfidChip = new JPanel(new BorderLayout(0, 2));
        rfidChip.setBackground(new Color(30, 25, 70));
        rfidChip.setBorder(new EmptyBorder(6, 10, 6, 10));
        JLabel rfidNum = UIFactory.lbl(info.rfidTag(), Font.BOLD, 11, C_ACCENT);
        JLabel vehicleLabel = UIFactory.lbl(info.vehicleInfo(), Font.PLAIN, 9, C_MUTED);
        rfidChip.add(rfidNum, BorderLayout.NORTH);
        rfidChip.add(vehicleLabel, BorderLayout.SOUTH);

        // Status and slot info
        JPanel statusPanel = new JPanel(new BorderLayout(8, 0));
        statusPanel.setOpaque(false);
        statusPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        Color statusColor;
        String statusText;
        if (info.isParked()) {
            statusColor = C_AVAILABLE;
            statusText  = "● PARKED";
        } else if (info.isReserved()) {
            statusColor = C_RESERVED;
            statusText  = "● RESERVED";
        } else {
            statusColor = C_MUTED;
            statusText  = "● INACTIVE";
        }

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        badgeRow.setOpaque(false);
        JLabel statusBadge = makeBadge(statusText, statusColor);
        badgeRow.add(statusBadge);

        // Add Cancel button only when the vehicle is RESERVED
        if (info.isReserved()) {
            JButton cancelBtn = new JButton("✕ Cancel");
            cancelBtn.setFont(new Font("Dialog", Font.BOLD, 10));
            cancelBtn.setForeground(Color.WHITE);
            cancelBtn.setBackground(new Color(180, 40, 60));
            cancelBtn.setBorder(new EmptyBorder(3, 8, 3, 8));
            cancelBtn.setFocusPainted(false);
            cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cancelBtn.setOpaque(true);
            cancelBtn.addActionListener(e -> cancelReservation(info, state, onCancelRefresh));
            badgeRow.add(cancelBtn);
        }

        JLabel slotLabel = UIFactory.lbl(
            info.isActive() ? "Slot " + info.parkedSlot() : "Not parked",
            Font.PLAIN, 11,
            info.isActive() ? new Color(197, 194, 224) : C_MUTED
        );
        slotLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        statusPanel.add(badgeRow,  BorderLayout.WEST);
        statusPanel.add(slotLabel, BorderLayout.CENTER);

        card.add(rfidChip,     BorderLayout.WEST);
        card.add(statusPanel,  BorderLayout.CENTER);

        return card;
    }

    // =========================================================================
    // Cancel reservation
    // =========================================================================

    private static void cancelReservation(VehicleRFIDInfo info,
                                          AppState state,
                                          Runnable onDone) {
        int confirm = JOptionPane.showConfirmDialog(
            null,
            "Cancel reservation for vehicle " + info.vehicleInfo()
                + " at Slot " + info.parkedSlot() + "?",
            "Confirm Cancellation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            VehicleDAO            vehicleDAO = new VehicleDAO();
            ParkingSlotDAO        slotDAO    = new ParkingSlotDAO();
            ParkingTransactionDAO txDAO      = new ParkingTransactionDAO();
            AuditLogDAO           auditDAO   = new AuditLogDAO();

            // ── Resolve vehicle ───────────────────────────────────────────────
            Optional<Vehicle> vehicleOpt = vehicleDAO.findByPlateNumber(info.vehicleInfo());
            if (vehicleOpt.isEmpty()) {
                DialogUtil.showMessageDialog(null,
                    "Could not find vehicle: " + info.vehicleInfo(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Vehicle vehicle = vehicleOpt.get();

            // ── Mark the RESERVED transaction as CANCELLED ────────────────────
            List<ParkingTransaction> txns = txDAO.findByVehicleId(vehicle.getVehicleId());
            for (ParkingTransaction txn : txns) {
                if ("RESERVED".equalsIgnoreCase(txn.getTransactionStatus())) {
                    txn.setTransactionStatus("CANCELLED");
                    txn.setExitTime(LocalDateTime.now());
                    txDAO.update(txn);
                    break;
                }
            }

            // ── Release the slot — mirrors reserveSlot() in reverse ──────────
            Optional<ParkingSlot> slotOpt = slotDAO.findBySlotCode(info.parkedSlot());
            if (slotOpt.isPresent()) {
                ParkingSlot slot = slotOpt.get();
                slotDAO.vacateSlot(slot.getSlotId());                // sets status → AVAILABLE
                slotDAO.updateCurrentVehicle(slot.getSlotId(), null); // clears vehicle link
            }

            // ── Reset vehicle parking status ──────────────────────────────────
            vehicleDAO.updateParkingStatus(vehicle.getVehicleId(), "Inactive");

            // ── Audit log ─────────────────────────────────────────────────────
            AuditLog log = new AuditLog();
            log.setAction("CANCEL_RESERVATION");
            log.setEntityType("PARKING_SLOT");
            log.setNewValue("AVAILABLE");
            log.setOldValue("plate=" + vehicle.getPlateNumber() + " slot=" + info.parkedSlot());
            log.setIpAddress("localhost");
            auditDAO.create(log);

            // ── Notify ALL slot-change listeners (slot map, admin map, etc.) ──
            // This causes UserSlotViewScreen and AdminSlotMapScreen to reload
            // from DB and redraw — the formerly-reserved slot turns green.
            state.notifySlotChange();

            // ── Refresh the dashboard RFID card list ──────────────────────────
            SwingUtilities.invokeLater(onDone);

            DialogUtil.showMessageDialog(null,
                "Reservation for " + info.vehicleInfo() + " at Slot "
                    + info.parkedSlot() + " has been cancelled.\n"
                    + "The slot is now available on the parking map.",
                "Reservation Cancelled", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            DialogUtil.showMessageDialog(null,
                "Cancellation failed: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
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