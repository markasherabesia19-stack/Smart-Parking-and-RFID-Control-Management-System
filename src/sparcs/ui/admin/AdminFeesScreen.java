package ui.admin;

import dao.ParkingTransactionDAO;
import dao.ParkingSlotDAO;
import dao.UserAccountDAO;
import dao.VehicleDAO;
import dao.VehicleOwnerDAO;
import dao.AuditLogDAO;
import model.AppState;
import model.AuditLog;
import model.ParkingTransaction;
import model.ParkingSlot;
import model.UserAccount;
import model.Vehicle;
import model.VehicleOwner;
import ui.shared.SidebarPanel;
import util.UIFactory;
import util.DialogUtil;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class AdminFeesScreen {

    // ── SPARCS design-system color aliases ────────────────────────────────────
    // These supplement UIConstants with the full token set from the design spec.
    // If UIConstants already defines any of these, remove the duplicate here.
    private static final Color C_TEXT_PRIMARY   = new Color(0xF0ECFF);  // Page titles
    private static final Color C_TEXT_SECONDARY = new Color(0xE8E4FF);  // Section headings
    private static final Color C_BODY           = new Color(0xC4BFED);  // Table data
    private static final Color C_BG_ROW         = new Color(0x1A1650);  // Table row base (Layer 2)
    private static final Color C_ROW_SELECTED   = new Color(0x241E6B);  // Row hover/selected (Layer 3)
    private static final Color C_BORDER_SUBTLE  = new Color(0x1E1C45);  // Inner separators
    private static final Color C_SUCCESS        = new Color(0x1DB954);  // Emerald green
    private static final Color C_DANGER         = new Color(0xE8365D);  // Crimson red
    private static final Color C_WARNING        = new Color(0xFF8C42);  // Amber orange
    private static final Color C_INFO_BLUE      = new Color(0x4F8EF7);  // Cobalt blue

    private static final int FIRST_HOUR_RATE = 30;
    private static final int SUCCEEDING_RATE = 20;
    private static final int OVERNIGHT_FLAT  = 150;
    private static final int OVERNIGHT_HOURS = 12;

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.setOpaque(true);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_FEES"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);
        content.setOpaque(true);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("FEES", Font.BOLD, 22, C_TEXT_PRIMARY), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        // ── Main body: left column (rate + cash-in), right column (pending) ──
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(true);
        body.setBackground(C_BG_DARK);
        body.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 14);
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.gridx = 0; gbc.gridy = 0;

        // ── Left column: rate card + cash-in card stacked ─────────────────────
        JPanel leftCol = new JPanel(new GridBagLayout());
        leftCol.setOpaque(true);
        leftCol.setBackground(C_BG_DARK);

        GridBagConstraints lc = new GridBagConstraints();
        lc.fill = GridBagConstraints.BOTH;
        lc.weightx = 1.0;
        lc.gridx = 0;

        // Rate Schedule card — no scroll, all 3 rows always visible
        lc.gridy = 0; lc.weighty = 0.0; lc.insets = new Insets(0, 0, 14, 0);
        JPanel rateCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        rateCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel rateTitle = UIFactory.lbl("RATE SCHEDULE", Font.BOLD, 14, C_TEXT_SECONDARY);
        rateTitle.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER_SUBTLE),
            new EmptyBorder(0, 0, 10, 0)
        ));
        rateCard.add(rateTitle, BorderLayout.NORTH);

        String[] rateCols = {"Duration", "Rate"};
        Object[][] rateData = {
            {"First hour",                 "P" + FIRST_HOUR_RATE},
            {"Every succeeding hour",      "P" + SUCCEEDING_RATE + " / hr"},
            {"Overnight (12 hrs or more)", "P" + OVERNIGHT_FLAT + " flat"},
        };
        JTable rateTable = new JTable(rateData, rateCols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Dimension getPreferredScrollableViewportSize() {
                return getPreferredSize();
            }
        };
        styleTable(rateTable);
        // 3 rows × 44px + 38px header = 170px — no scrollbar needed
        rateTable.setPreferredScrollableViewportSize(
            new Dimension(rateTable.getPreferredSize().width, 44 * 3 + 38));

        JPanel rateTableWrap = new JPanel(new BorderLayout());
        rateTableWrap.setOpaque(false);
        rateTableWrap.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));
        rateTableWrap.add(rateTable.getTableHeader(), BorderLayout.NORTH);
        rateTableWrap.add(rateTable, BorderLayout.CENTER);
        rateCard.add(rateTableWrap, BorderLayout.CENTER);

        JLabel note = UIFactory.lbl("P30 first hr + P20/hr after · P150 flat if 12 hrs+", Font.ITALIC, 10, C_MUTED);
        note.setBorder(new EmptyBorder(6, 0, 0, 0));
        rateCard.add(note, BorderLayout.SOUTH);
        leftCol.add(rateCard, lc);

        // Cash-in card — takes all remaining vertical space
        lc.gridy = 1; lc.weighty = 1.0; lc.insets = new Insets(0, 0, 0, 0);
        JPanel cashInCard = buildCashInCard(state);
        leftCol.add(cashInCard, lc);

        body.add(leftCol, gbc);

        // ── Right column: pending fees ─────────────────────────────────────────
        gbc.gridx = 1; gbc.insets = new Insets(0, 0, 0, 0);
        JPanel pendingCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        pendingCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel pendingTitle = UIFactory.lbl("PENDING FEES", Font.BOLD, 14, C_TEXT_SECONDARY);
        pendingTitle.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER_SUBTLE),
            new EmptyBorder(0, 0, 10, 0)
        ));
        pendingCard.add(pendingTitle, BorderLayout.NORTH);

        String[] pendingCols = {"Plate", "Slot", "Duration", "Fee", "Status", "Action"};
        DefaultTableModel pendingModel = new DefaultTableModel(new Object[0][6], pendingCols) {
            @Override public boolean isCellEditable(int r, int c) { return c == 5; }
        };

        JTable pendingTable = new JTable(pendingModel);
        styleTable(pendingTable);

        // Plate column — monospace purple
        pendingTable.getColumn("Plate").setCellRenderer(new PlateRenderer());
        pendingTable.getColumn("Plate").setPreferredWidth(90);

        // Status badge column
        pendingTable.getColumn("Status").setCellRenderer(new StatusBadgeRenderer());
        pendingTable.getColumn("Status").setPreferredWidth(100);
        pendingTable.getColumn("Status").setMaxWidth(110);

        // Action collect button column
        pendingTable.getColumn("Action").setCellRenderer(new ButtonRenderer());
        pendingTable.getColumn("Action").setCellEditor(new ButtonEditor(pendingModel, state));
        pendingTable.getColumn("Action").setPreferredWidth(90);
        pendingTable.getColumn("Action").setMaxWidth(90);

        JScrollPane pendingScroll = new JScrollPane(pendingTable);
        pendingScroll.setOpaque(false);
        pendingScroll.getViewport().setBackground(C_BG_CARD);
        pendingScroll.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));
        pendingCard.add(pendingScroll, BorderLayout.CENTER);

        // ── Footer: collect all ───────────────────────────────────────────────
        JPanel pendingFooter = new JPanel(new GridBagLayout());
        pendingFooter.setOpaque(false);
        pendingFooter.setBorder(new EmptyBorder(10, 0, 0, 0));

        GridBagConstraints fc = new GridBagConstraints();
        fc.gridy = 0; fc.fill = GridBagConstraints.HORIZONTAL; fc.insets = new Insets(0, 0, 0, 0);
        fc.weightx = 1.0; fc.gridx = 0;

        JButton collectAllBtn = UIFactory.gradientButton("COLLECT ALL FEES");
        collectAllBtn.addActionListener(e -> collectAll(pendingModel, state));
        pendingFooter.add(collectAllBtn, fc);

        pendingCard.add(pendingFooter, BorderLayout.SOUTH);
        body.add(pendingCard, gbc);

        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        root.addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { reloadPending(pendingModel); }
        });
        state.addSlotChangeListener(() -> SwingUtilities.invokeLater(() -> reloadPending(pendingModel)));

        reloadPending(pendingModel);
        return root;
    }

    // =========================================================================
    // Cash-in card
    // =========================================================================

    private static JPanel buildCashInCard(AppState state) {
        JPanel card = UIFactory.cardPanel(new GridBagLayout());
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0; cc.fill = GridBagConstraints.HORIZONTAL; cc.weightx = 1.0;

        cc.gridy = 0; cc.insets = new Insets(0, 0, 14, 0);
        JLabel cashInTitle = UIFactory.lbl("CASH IN", Font.BOLD, 14, C_TEXT_SECONDARY);
        cashInTitle.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER_SUBTLE),
            new EmptyBorder(0, 0, 10, 0)
        ));
        card.add(cashInTitle, cc);

        cc.gridy = 1; cc.insets = new Insets(0, 0, 4, 0);
        card.add(UIFactory.lbl("USERNAME", Font.BOLD, 11, C_MUTED), cc);

        cc.gridy = 2; cc.insets = new Insets(0, 0, 12, 0);
        JTextField usernameField = UIFactory.styledField("Enter username");
        card.add(usernameField, cc);

        cc.gridy = 3; cc.insets = new Insets(0, 0, 4, 0);
        card.add(UIFactory.lbl("AMOUNT (P)", Font.BOLD, 11, C_MUTED), cc);

        cc.gridy = 4; cc.insets = new Insets(0, 0, 12, 0);
        JTextField amountField = UIFactory.styledField("e.g. 100");
        card.add(amountField, cc);

        // Current balance display
        cc.gridy = 5; cc.insets = new Insets(0, 0, 14, 0);
        JLabel balanceLbl = UIFactory.lbl("Current balance: —", Font.PLAIN, 13, C_MUTED);
        card.add(balanceLbl, cc);

        // Check balance button — Ghost style
        cc.gridy = 6; cc.insets = new Insets(0, 0, 8, 0);
        JButton checkBtn = UIFactory.outlineButton("CHECK BALANCE");
        card.add(checkBtn, cc);

        // Cash in button
        cc.gridy = 7; cc.insets = new Insets(0, 0, 0, 0);
        JButton cashInBtn = UIFactory.gradientButton("CASH IN");
        card.add(cashInBtn, cc);

        // ── Check balance action ──────────────────────────────────────────────
        checkBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            if (username.isEmpty()) {
                DialogUtil.showMessageDialog(null, "Enter a username first.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                UserAccountDAO userDAO = new UserAccountDAO();
                Optional<UserAccount> userOpt = userDAO.findByUsername(username);
                if (userOpt.isEmpty()) {
                    balanceLbl.setText("User not found.");
                    balanceLbl.setForeground(C_DANGER);
                } else {
                    BigDecimal bal = userDAO.getWalletBalance(userOpt.get().getUserId());
                    balanceLbl.setText("Current balance: P" + bal.toPlainString());
                    balanceLbl.setForeground(C_INFO_BLUE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                DialogUtil.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ── Cash-in action ────────────────────────────────────────────────────
        cashInBtn.addActionListener(e -> {
            String username  = usernameField.getText().trim();
            String amountStr = amountField.getText().trim();

            if (username.isEmpty() || amountStr.isEmpty()) {
                DialogUtil.showMessageDialog(null, "Please fill in all fields.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                DialogUtil.showMessageDialog(null, "Enter a valid positive amount.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                UserAccountDAO userDAO = new UserAccountDAO();
                Optional<UserAccount> userOpt = userDAO.findByUsername(username);
                if (userOpt.isEmpty()) {
                    DialogUtil.showMessageDialog(null, "User '" + username + "' not found.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                UserAccount user = userOpt.get();
                userDAO.addWalletBalance(user.getUserId(), amount);

                BigDecimal newBal = userDAO.getWalletBalance(user.getUserId());
                balanceLbl.setText("Current balance: P" + newBal.toPlainString());
                balanceLbl.setForeground(C_INFO_BLUE);

                // Log cash-in to audit log
                try {
                    AuditLog cashInLog = new AuditLog();
                    if (state.getCurrentUserAccount() != null) {
                        cashInLog.setUserId(state.getCurrentUserAccount().getUserId());
                    }
                    cashInLog.setAction("CASH_IN");
                    cashInLog.setEntityType("USER_WALLET");
                    cashInLog.setEntityId(user.getUserId());
                    cashInLog.setNewValue("P" + amount.toPlainString());
                    cashInLog.setOldValue(username);
                    new AuditLogDAO().create(cashInLog);
                } catch (Exception auditEx) {
                    auditEx.printStackTrace();
                }

                amountField.setText("");
                DialogUtil.showMessageDialog(null,
                        "Successfully added P" + amount.toPlainString() +
                        " to " + username + "'s wallet.\nNew balance: P" + newBal.toPlainString(),
                        "Cash In Successful", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                ex.printStackTrace();
                DialogUtil.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return card;
    }

    // =========================================================================
    // Pending fees helpers
    // =========================================================================

    static void reloadPending(DefaultTableModel model) {
        model.setRowCount(0);
        try {
            ParkingTransactionDAO txDAO   = new ParkingTransactionDAO();
            ParkingSlotDAO        slotDAO = new ParkingSlotDAO();
            VehicleDAO            vDAO    = new VehicleDAO();

            // Load pending (IN_PROGRESS) transactions
            for (ParkingTransaction tx : txDAO.findInProgress()) {
                if ("PAID".equals(tx.getPaymentStatus())) continue;

                String plate = "—", slotCode = "—";
                Optional<Vehicle>     v = vDAO.findById(tx.getVehicleId());
                if (v.isPresent()) plate = v.get().getPlateNumber();
                Optional<ParkingSlot> s = slotDAO.findById(tx.getSlotId());
                if (s.isPresent()) slotCode = s.get().getSlotCode();

                long totalMinutes = Duration.between(tx.getEntryTime(), LocalDateTime.now()).toMinutes();
                model.addRow(new Object[]{
                    plate, slotCode,
                    formatDuration(totalMinutes),
                    "P" + computeFee(totalMinutes),
                    "PENDING",
                    "COLLECT"
                });
            }

            // Load collected (PAID) transactions
            for (ParkingTransaction tx : txDAO.findByPaymentStatus("PAID")) {
                String plate = "—", slotCode = "—";
                Optional<Vehicle>     v = vDAO.findById(tx.getVehicleId());
                if (v.isPresent()) plate = v.get().getPlateNumber();
                Optional<ParkingSlot> s = slotDAO.findById(tx.getSlotId());
                if (s.isPresent()) slotCode = s.get().getSlotCode();

                long totalMinutes;
                if ("COMPLETED".equals(tx.getTransactionStatus()) && tx.getExitTime() != null) {
                    totalMinutes = Duration.between(tx.getEntryTime(), tx.getExitTime()).toMinutes();
                } else {
                    totalMinutes = Duration.between(tx.getEntryTime(), LocalDateTime.now()).toMinutes();
                }

                model.addRow(new Object[]{
                    plate, slotCode,
                    formatDuration(totalMinutes),
                    "P" + tx.getCalculatedFee().intValue(),
                    "COLLECTED",
                    "COLLECTED"
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Resolves the UserAccount that owns the given vehicle.
     * Returns empty if the chain vehicle → owner → user cannot be completed.
     */
    private static Optional<UserAccount> resolveUserForVehicle(Vehicle vehicle) {
        try {
            VehicleOwnerDAO ownerDAO = new VehicleOwnerDAO();
            UserAccountDAO  userDAO  = new UserAccountDAO();

            Optional<VehicleOwner> ownerOpt = ownerDAO.findById(vehicle.getOwnerId());
            if (ownerOpt.isEmpty() || ownerOpt.get().getUserId() == null) return Optional.empty();

            return userDAO.findById(ownerOpt.get().getUserId());
        } catch (Exception ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    private static void collectAll(DefaultTableModel model, AppState state) {
        if (model.getRowCount() == 0) {
            DialogUtil.showMessageDialog(null, "No pending fees.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (DialogUtil.showConfirmDialog(null, "Mark all pending fees as PAID?\nThe fee will be deducted from each vehicle owner's wallet.",
                "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        try {
            ParkingTransactionDAO txDAO   = new ParkingTransactionDAO();
            VehicleDAO            vDAO    = new VehicleDAO();
            UserAccountDAO        userDAO = new UserAccountDAO();

            int collected = 0;
            int skipped   = 0;
            StringBuilder skippedPlates = new StringBuilder();

            for (ParkingTransaction tx : txDAO.findInProgress()) {
                if ("PAID".equals(tx.getPaymentStatus())) continue;

                long mins = Duration.between(tx.getEntryTime(), LocalDateTime.now()).toMinutes();
                int  fee  = computeFee(mins);
                BigDecimal feeDec = new BigDecimal(fee);

                // ── Resolve vehicle owner and deduct wallet ───────────────────
                Optional<Vehicle> vOpt = vDAO.findById(tx.getVehicleId());
                String plate = vOpt.map(Vehicle::getPlateNumber).orElse("Vehicle#" + tx.getVehicleId());

                Optional<UserAccount> userOpt = vOpt.isPresent()
                    ? resolveUserForVehicle(vOpt.get()) : Optional.empty();

                if (userOpt.isPresent()) {
                    BigDecimal balance = userDAO.getWalletBalance(userOpt.get().getUserId());
                    if (balance.compareTo(feeDec) < 0) {
                        // Skip vehicles whose owner has insufficient balance
                        skipped++;
                        skippedPlates.append("\n  • ").append(plate)
                            .append(" (balance: P").append(balance.toPlainString())
                            .append(", fee: P").append(fee).append(")");
                        continue;
                    }
                    userDAO.deductWalletBalance(userOpt.get().getUserId(), feeDec);
                }
                // If owner/user cannot be resolved, still mark PAID (cash payment scenario)

                tx.setPaymentStatus("PAID");
                tx.setCalculatedFee(feeDec);
                txDAO.update(tx);
                collected++;

                // Audit log
                try {
                    AuditLog feeLog = new AuditLog();
                    if (state.getCurrentUserAccount() != null) {
                        feeLog.setUserId(state.getCurrentUserAccount().getUserId());
                    }
                    feeLog.setAction("FEE_COLLECTED");
                    feeLog.setEntityType("VEHICLE");
                    if (vOpt.isPresent()) feeLog.setEntityId(vOpt.get().getVehicleId());
                    feeLog.setNewValue("P" + fee);
                    feeLog.setOldValue(plate);
                    new AuditLogDAO().create(feeLog);
                } catch (Exception auditEx) {
                    auditEx.printStackTrace();
                }
            }

            StringBuilder msg = new StringBuilder();
            msg.append(collected).append(" fee(s) collected successfully.");
            if (skipped > 0) {
                msg.append("\n\n").append(skipped)
                   .append(" vehicle(s) skipped (insufficient balance):").append(skippedPlates);
            }
            DialogUtil.showMessageDialog(null, msg.toString(),
                skipped > 0 ? "Partially Collected" : "Success",
                skipped > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);

            reloadPending(model);
            state.notifySlotChange();

        } catch (Exception ex) {
            ex.printStackTrace();
            DialogUtil.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static int computeFee(long totalMinutes) {
        long hours = (long) Math.ceil(totalMinutes / 60.0);
        if (hours == 0) hours = 1;
        if (hours >= OVERNIGHT_HOURS) return OVERNIGHT_FLAT;
        if (hours == 1) return FIRST_HOUR_RATE;
        return FIRST_HOUR_RATE + (int)(hours - 1) * SUCCEEDING_RATE;
    }

    static String formatDuration(long totalMinutes) {
        long h = totalMinutes / 60, m = totalMinutes % 60;
        return h > 0 ? h + "h " + m + "m" : m + "m";
    }

    private static void styleTable(JTable t) {
        t.setBackground(C_BG_ROW);
        t.setForeground(C_BODY);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setRowHeight(44);
        t.setGridColor(C_BORDER_SUBTLE);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setSelectionBackground(C_ROW_SELECTED);
        t.setSelectionForeground(C_WHITE);
        t.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader h = t.getTableHeader();
        h.setBackground(C_BG_PANEL);
        h.setForeground(C_TEXT_SECONDARY);
        h.setFont(new Font("SansSerif", Font.BOLD, 13));
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_INPUT_BD));
        h.setPreferredSize(new Dimension(h.getPreferredSize().width, 38));

        // Default cell renderer — body color + padding
        DefaultTableCellRenderer bodyRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? C_BG_ROW : C_BG_ROW);
                    setForeground(C_BODY);
                }
                return this;
            }
        };

        // Apply to all columns by default
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(bodyRenderer);
        }

        // Header renderer — uppercase, letter-spaced
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setText(value != null ? value.toString().toUpperCase() : "");
                setFont(new Font("SansSerif", Font.BOLD, 13));
                setForeground(C_TEXT_SECONDARY);
                setBackground(C_BG_PANEL);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setHorizontalAlignment(SwingConstants.LEFT);
                return this;
            }
        };
        t.getTableHeader().setDefaultRenderer(headerRenderer);
    }

    // ── Plate number renderer — monospace purple ──────────────────────────────
    static class PlateRenderer extends DefaultTableCellRenderer {
        PlateRenderer() {
            setFont(new Font("Consolas", Font.PLAIN, 12));
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            if (!sel) {
                setForeground(C_PURPLE);
                setBackground(C_BG_ROW);
            }
            return this;
        }
    }

    // ── Status badge renderer ─────────────────────────────────────────────────
    static class StatusBadgeRenderer extends JPanel implements TableCellRenderer {
        private final JLabel badge = new JLabel();

        StatusBadgeRenderer() {
            setLayout(new GridBagLayout());
            setOpaque(true);
            badge.setFont(new Font("SansSerif", Font.BOLD, 11));
            badge.setOpaque(false);
            badge.setBorder(new EmptyBorder(3, 8, 3, 8));
            add(badge);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            setBackground(sel ? C_ROW_SELECTED : C_BG_ROW);
            String status = v != null ? v.toString() : "";
            if ("COLLECTED".equals(status)) {
                badge.setText("● COLLECTED");
                badge.setForeground(C_SUCCESS);
            } else {
                badge.setText("● PENDING");
                badge.setForeground(C_WARNING);
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Badge background pill
            Rectangle b = badge.getBounds();
            if (b.width > 0) {
                String status = badge.getText();
                Color bg = status.contains("COLLECTED")
                    ? new Color(29, 185, 84, 38)
                    : new Color(255, 140, 66, 38);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(b.x, b.y, b.width, b.height, 6, 6));
            }
            g2.dispose();
        }
    }

    static class ButtonRenderer extends JButton implements TableCellRenderer {
        ButtonRenderer() {
            setOpaque(true);
            setFont(new Font("SansSerif", Font.BOLD, 11));
            setBorder(new EmptyBorder(4, 10, 4, 10));
            setFocusPainted(false);
        }
        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            String status = (String) v;
            if ("COLLECTED".equals(status)) {
                setText("COLLECTED");
                setForeground(C_SUCCESS);
                setBackground(new Color(29, 185, 84, 38));
                setEnabled(false);
            } else {
                setText("COLLECT");
                setForeground(Color.WHITE);
                setBackground(C_PURPLE);
                setEnabled(true);
            }
            return this;
        }
    }

    static class ButtonEditor extends DefaultCellEditor {
        private final DefaultTableModel model;
        private final AppState          state;
        private       int               clickedRow;

        ButtonEditor(DefaultTableModel model, AppState state) {
            super(new JCheckBox());
            this.model = model;
            this.state = state;
            JButton btn = new JButton("COLLECT");
            btn.setFont(new Font("SansSerif", Font.BOLD, 11));
            btn.setForeground(Color.WHITE);
            btn.setBackground(C_PURPLE);
            btn.setOpaque(true);
            btn.setFocusPainted(false);
            btn.setBorder(new EmptyBorder(4, 10, 4, 10));
            btn.addActionListener(e -> fireEditingStopped());
            editorComponent = btn;
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable t, Object v, boolean sel, int row, int col) {
            clickedRow = row;
            String status = (String) v;
            JButton btn = (JButton) editorComponent;

            if ("COLLECTED".equals(status)) {
                btn.setText("COLLECTED");
                btn.setForeground(C_SUCCESS);
                btn.setBackground(new Color(29, 185, 84, 38));
                btn.setEnabled(false);
            } else {
                btn.setText("COLLECT");
                btn.setForeground(Color.WHITE);
                btn.setBackground(C_PURPLE);
                btn.setEnabled(true);
            }
            return editorComponent;
        }

        @Override
        public Object getCellEditorValue() {
            String status = (String) model.getValueAt(clickedRow, 5);
            if (!"COLLECTED".equals(status)) {
                SwingUtilities.invokeLater(() -> collectOne(clickedRow));
            }
            return model.getValueAt(clickedRow, 5);
        }

        private void collectOne(int row) {
            try {
                String plate  = (String) model.getValueAt(row, 0);
                String feeStr = ((String) model.getValueAt(row, 3)).replace("P", "");
                int    fee    = Integer.parseInt(feeStr);
                BigDecimal feeDec = new BigDecimal(fee);

                ParkingTransactionDAO txDAO   = new ParkingTransactionDAO();
                VehicleDAO            vDAO    = new VehicleDAO();
                UserAccountDAO        userDAO = new UserAccountDAO();

                Optional<Vehicle> vOpt = vDAO.findByPlateNumber(plate);
                if (vOpt.isEmpty()) {
                    DialogUtil.showMessageDialog(null, "Vehicle not found: " + plate,
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Vehicle vehicle = vOpt.get();

                // ── Deduct fee from vehicle owner's wallet ────────────────────
                Optional<UserAccount> userOpt = resolveUserForVehicle(vehicle);
                if (userOpt.isPresent()) {
                    BigDecimal balance = userDAO.getWalletBalance(userOpt.get().getUserId());
                    if (balance.compareTo(feeDec) < 0) {
                        DialogUtil.showMessageDialog(null,
                            "Insufficient wallet balance for " + plate + ".\n"
                            + "Current balance: P" + balance.toPlainString()
                            + "  |  Fee: P" + fee,
                            "Insufficient Balance", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    userDAO.deductWalletBalance(userOpt.get().getUserId(), feeDec);
                }

                // ── Mark transaction as PAID ──────────────────────────────────
                for (ParkingTransaction tx : txDAO.findByVehicleId(vehicle.getVehicleId())) {
                    if ("IN_PROGRESS".equals(tx.getTransactionStatus())) {
                        tx.setPaymentStatus("PAID");
                        tx.setCalculatedFee(feeDec);
                        txDAO.update(tx);
                        break;
                    }
                }

                // ── Audit log ─────────────────────────────────────────────────
                try {
                    AuditLog feeLog = new AuditLog();
                    if (state.getCurrentUserAccount() != null) {
                        feeLog.setUserId(state.getCurrentUserAccount().getUserId());
                    }
                    feeLog.setAction("FEE_COLLECTED");
                    feeLog.setEntityType("VEHICLE");
                    feeLog.setEntityId(vehicle.getVehicleId());
                    feeLog.setNewValue("P" + fee);
                    feeLog.setOldValue(plate);
                    new AuditLogDAO().create(feeLog);
                } catch (Exception auditEx) {
                    auditEx.printStackTrace();
                }

                String successMsg = "Fee of P" + fee + " collected for " + plate + ".";
                if (userOpt.isPresent()) {
                    BigDecimal newBal = userDAO.getWalletBalance(userOpt.get().getUserId());
                    successMsg += "\nNew wallet balance: P" + newBal.toPlainString();
                }

                DialogUtil.showMessageDialog(null, successMsg, "Collected", JOptionPane.INFORMATION_MESSAGE);
                model.setValueAt("COLLECTED", row, 4);
                model.setValueAt("COLLECTED", row, 5);
                reloadPending(model);
                state.notifySlotChange();

            } catch (Exception ex) {
                ex.printStackTrace();
                DialogUtil.showMessageDialog(null, "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}