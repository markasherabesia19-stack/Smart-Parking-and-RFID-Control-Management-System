package ui.admin;

import dao.ParkingTransactionDAO;
import dao.ParkingSlotDAO;
import dao.UserAccountDAO;
import dao.VehicleDAO;
import dao.AuditLogDAO;
import model.AppState;
import model.AuditLog;
import model.ParkingTransaction;
import model.ParkingSlot;
import model.UserAccount;
import model.Vehicle;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class AdminFeesScreen {

    private static final int FIRST_HOUR_RATE = 50;
    private static final int SUCCEEDING_RATE = 30;
    private static final int OVERNIGHT_FLAT  = 300;
    private static final int OVERNIGHT_HOURS = 12;

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_FEES"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("FEES", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        // ── Main body: left column (rate + cash-in), right column (pending) ──
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 14);
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.gridx = 0; gbc.gridy = 0;

        // ── Left column: rate card + cash-in card stacked ─────────────────────
        JPanel leftCol = new JPanel(new GridBagLayout());
        leftCol.setOpaque(false);

        GridBagConstraints lc = new GridBagConstraints();
        lc.fill = GridBagConstraints.BOTH;
        lc.weightx = 1.0;
        lc.gridx = 0;

        // Rate Schedule card
        lc.gridy = 0; lc.weighty = 0.4; lc.insets = new Insets(0, 0, 14, 0);
        JPanel rateCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        rateCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        rateCard.add(UIFactory.lbl("RATE SCHEDULE", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);

        String[] rateCols = {"Duration", "Rate"};
        Object[][] rateData = {
            {"First hour",                 "P" + FIRST_HOUR_RATE},
            {"Every succeeding hour",      "P" + SUCCEEDING_RATE + " / hr"},
            {"Overnight (12 hrs or more)", "P" + OVERNIGHT_FLAT + " flat"},
        };
        JTable rateTable = new JTable(rateData, rateCols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        styleTable(rateTable);
        JScrollPane rateScroll = new JScrollPane(rateTable);
        rateScroll.setOpaque(false);
        rateScroll.getViewport().setBackground(C_BG_CARD);
        rateScroll.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));
        rateCard.add(rateScroll, BorderLayout.CENTER);

        JLabel note = UIFactory.lbl("P50 first hr + P30/hr after · P300 flat if 12 hrs+", Font.ITALIC, 10, C_MUTED);
        note.setBorder(new EmptyBorder(6, 0, 0, 0));
        rateCard.add(note, BorderLayout.SOUTH);
        leftCol.add(rateCard, lc);

        // Cash-in card
        lc.gridy = 1; lc.weighty = 0.6; lc.insets = new Insets(0, 0, 0, 0);
        JPanel cashInCard = buildCashInCard(state);
        leftCol.add(cashInCard, lc);

        body.add(leftCol, gbc);

        // ── Right column: pending fees ─────────────────────────────────────────
        gbc.gridx = 1; gbc.insets = new Insets(0, 0, 0, 0);
        JPanel pendingCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        pendingCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        pendingCard.add(UIFactory.lbl("PENDING FEES", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);

        String[] pendingCols = {"Plate", "Slot", "Duration", "Fee", "Action"};
        DefaultTableModel pendingModel = new DefaultTableModel(new Object[0][5], pendingCols) {
            @Override public boolean isCellEditable(int r, int c) { return c == 4; }
        };

        JTable pendingTable = new JTable(pendingModel);
        styleTable(pendingTable);

        pendingTable.getColumn("Action").setCellRenderer(new ButtonRenderer());
        pendingTable.getColumn("Action").setCellEditor(new ButtonEditor(pendingModel, state));
        pendingTable.getColumn("Action").setPreferredWidth(90);
        pendingTable.getColumn("Action").setMaxWidth(90);

        JScrollPane pendingScroll = new JScrollPane(pendingTable);
        pendingScroll.setOpaque(false);
        pendingScroll.getViewport().setBackground(C_BG_CARD);
        pendingScroll.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));
        pendingCard.add(pendingScroll, BorderLayout.CENTER);

        JButton collectAllBtn = UIFactory.gradientButton("COLLECT ALL FEES");
        collectAllBtn.addActionListener(e -> collectAll(pendingModel, state));
        pendingCard.add(collectAllBtn, BorderLayout.SOUTH);
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
        card.add(UIFactory.lbl("CASH IN", Font.BOLD, 12, C_MUTED), cc);

        cc.gridy = 1; cc.insets = new Insets(0, 0, 2, 0);
        card.add(UIFactory.lbl("USERNAME", Font.BOLD, 10, C_MUTED), cc);

        cc.gridy = 2; cc.insets = new Insets(0, 0, 10, 0);
        JTextField usernameField = UIFactory.styledField("Enter username");
        card.add(usernameField, cc);

        cc.gridy = 3; cc.insets = new Insets(0, 0, 2, 0);
        card.add(UIFactory.lbl("AMOUNT (P)", Font.BOLD, 10, C_MUTED), cc);

        cc.gridy = 4; cc.insets = new Insets(0, 0, 10, 0);
        JTextField amountField = UIFactory.styledField("e.g. 100");
        card.add(amountField, cc);

        // Current balance display
        cc.gridy = 5; cc.insets = new Insets(0, 0, 14, 0);
        JLabel balanceLbl = UIFactory.lbl("Current balance: —", Font.PLAIN, 11, C_MUTED);
        card.add(balanceLbl, cc);

        // Check balance button
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
                JOptionPane.showMessageDialog(null, "Enter a username first.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                UserAccountDAO userDAO = new UserAccountDAO();
                Optional<UserAccount> userOpt = userDAO.findByUsername(username);
                if (userOpt.isEmpty()) {
                    balanceLbl.setText("User not found.");
                    balanceLbl.setForeground(new Color(220, 80, 80));
                } else {
                    BigDecimal bal = userDAO.getWalletBalance(userOpt.get().getUserId());
                    balanceLbl.setText("Current balance: P" + bal.toPlainString());
                    balanceLbl.setForeground(C_MUTED);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ── Cash-in action ────────────────────────────────────────────────────
        cashInBtn.addActionListener(e -> {
            String username  = usernameField.getText().trim();
            String amountStr = amountField.getText().trim();

            if (username.isEmpty() || amountStr.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please fill in all fields.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Enter a valid positive amount.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                UserAccountDAO userDAO = new UserAccountDAO();
                Optional<UserAccount> userOpt = userDAO.findByUsername(username);
                if (userOpt.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "User '" + username + "' not found.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                UserAccount user = userOpt.get();
                userDAO.addWalletBalance(user.getUserId(), amount);

                BigDecimal newBal = userDAO.getWalletBalance(user.getUserId());
                balanceLbl.setText("Current balance: P" + newBal.toPlainString());
                balanceLbl.setForeground(C_MUTED);

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
                JOptionPane.showMessageDialog(null,
                        "Successfully added P" + amount.toPlainString() +
                        " to " + username + "'s wallet.\nNew balance: P" + newBal.toPlainString(),
                        "Cash In Successful", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

            // Load pending (IN_PROGRESS) transactions - only those NOT yet collected
            for (ParkingTransaction tx : txDAO.findInProgress()) {
                // Skip transactions that have already been paid
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
                    "PENDING"
                });
            }

            // Load collected (PAID) transactions - show as disabled
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
                    "COLLECTED"
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void collectAll(DefaultTableModel model, AppState state) {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "No pending fees.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(null, "Mark all pending fees as PAID?",
                "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            ParkingTransactionDAO txDAO = new ParkingTransactionDAO();
            for (ParkingTransaction tx : txDAO.findInProgress()) {
                long mins = Duration.between(tx.getEntryTime(), LocalDateTime.now()).toMinutes();
                tx.setPaymentStatus("PAID");
                tx.setCalculatedFee(new java.math.BigDecimal(computeFee(mins)));
                txDAO.update(tx);
            }
            JOptionPane.showMessageDialog(null, "All fees collected.", "Success", JOptionPane.INFORMATION_MESSAGE);
            reloadPending(model);
            state.notifySlotChange();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        t.setBackground(C_BG_CARD); t.setForeground(C_WHITE);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setRowHeight(32); t.setGridColor(new Color(60, 50, 100));
        t.setSelectionBackground(C_PURPLE); t.setSelectionForeground(C_WHITE);
        JTableHeader h = t.getTableHeader();
        h.setBackground(C_BG_PANEL); h.setForeground(C_MUTED);
        h.setFont(new Font("SansSerif", Font.BOLD, 11));
        h.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));
    }

    static class ButtonRenderer extends JButton implements TableCellRenderer {
        ButtonRenderer() {
            setOpaque(true);
            setFont(new Font("SansSerif", Font.BOLD, 11));
            setBorder(new EmptyBorder(4, 8, 4, 8));
        }
        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            String status = (String) v;
            if ("COLLECTED".equals(status)) {
                setText("COLLECTED");
                setForeground(Color.WHITE);
                setBackground(new Color(100, 150, 100));
                setEnabled(false);
            } else {
                setText("COLLECT");
                setForeground(Color.WHITE);
                setBackground(new Color(130, 60, 200));
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
            btn.setOpaque(true);
            btn.setBorder(new EmptyBorder(4, 8, 4, 8));
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
                btn.setBackground(new Color(100, 150, 100));
                btn.setEnabled(false);
            } else {
                btn.setText("COLLECT");
                btn.setBackground(new Color(130, 60, 200));
                btn.setEnabled(true);
            }
            return editorComponent;
        }

        @Override
        public Object getCellEditorValue() {
            String status = (String) model.getValueAt(clickedRow, 4);
            if (!"COLLECTED".equals(status)) {
                SwingUtilities.invokeLater(() -> collectOne(clickedRow));
            }
            return model.getValueAt(clickedRow, 4);
        }

        private void collectOne(int row) {
            try {
                String plate  = (String) model.getValueAt(row, 0);
                String feeStr = ((String) model.getValueAt(row, 3)).replace("P", "");
                int    fee    = Integer.parseInt(feeStr);

                ParkingTransactionDAO txDAO = new ParkingTransactionDAO();
                VehicleDAO            vDAO  = new VehicleDAO();

                Optional<Vehicle> vOpt = vDAO.findByPlateNumber(plate);
                if (vOpt.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Vehicle not found: " + plate,
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                for (ParkingTransaction tx : txDAO.findByVehicleId(vOpt.get().getVehicleId())) {
                    if ("IN_PROGRESS".equals(tx.getTransactionStatus())) {
                        tx.setPaymentStatus("PAID");
                        tx.setCalculatedFee(new java.math.BigDecimal(fee));
                        txDAO.update(tx);
                        break;
                    }
                }
                
                // Log fee collection to audit log
                try {
                    AuditLog feeLog = new AuditLog();
                    if (state.getCurrentUserAccount() != null) {
                        feeLog.setUserId(state.getCurrentUserAccount().getUserId());
                    }
                    feeLog.setAction("FEE_COLLECTED");
                    feeLog.setEntityType("VEHICLE");
                    feeLog.setEntityId(vOpt.get().getVehicleId());
                    feeLog.setNewValue("P" + fee);
                    feeLog.setOldValue(plate);
                    new AuditLogDAO().create(feeLog);
                } catch (Exception auditEx) {
                    auditEx.printStackTrace();
                }
                
                JOptionPane.showMessageDialog(null,
                    "Fee of P" + fee + " collected for " + plate + ".",
                    "Collected", JOptionPane.INFORMATION_MESSAGE);
                model.setValueAt("COLLECTED", row, 4);
                reloadPending(model);
                state.notifySlotChange();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}