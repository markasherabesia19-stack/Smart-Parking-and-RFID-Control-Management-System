package ui.user;

import dao.ParkingSlotDAO;
import dao.ParkingTransactionDAO;
import dao.VehicleDAO;
import model.AppState;
import model.ParkingSlot;
import model.ParkingTransaction;
import model.Vehicle;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserMyStatusScreen {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "USER", "USER_MY_STATUS"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("MY PARKING STATUS", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);

        JButton refreshBtn = UIFactory.gradientButton("REFRESH");
        refreshBtn.setPreferredSize(new Dimension(100, 36));
        topBar.add(refreshBtn, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // Vertical container that holds section groups
        JPanel sectionsWrapper = new JPanel();
        sectionsWrapper.setLayout(new BoxLayout(sectionsWrapper, BoxLayout.Y_AXIS));
        sectionsWrapper.setBackground(C_BG_DARK);
        sectionsWrapper.setBorder(new EmptyBorder(28, 28, 28, 28));

        JScrollPane scroll = new JScrollPane(sectionsWrapper);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        content.add(scroll, BorderLayout.CENTER);

        // Refresh action — reload and rebuild grouped sections
        refreshBtn.addActionListener(e -> {
            sectionsWrapper.removeAll();

            List<TransactionWithDetails> all = loadUserTransactions(state);

            // Partition into three groups
            List<TransactionWithDetails> current   = new ArrayList<>();
            List<TransactionWithDetails> pending   = new ArrayList<>();
            List<TransactionWithDetails> completed = new ArrayList<>();

            for (TransactionWithDetails txn : all) {
                if (isCurrentTransaction(txn.transaction())) {
                    current.add(txn);
                } else if (isPendingTransaction(txn.transaction())) {
                    pending.add(txn);
                } else {
                    completed.add(txn);
                }
            }

            if (all.isEmpty()) {
                JPanel emptyPanel = new JPanel(new GridBagLayout());
                emptyPanel.setBackground(C_BG_DARK);
                emptyPanel.add(UIFactory.lbl("No Parking Transactions", Font.PLAIN, 14, C_MUTED));
                sectionsWrapper.add(emptyPanel);
            } else {
                addSection(sectionsWrapper, "🅿 CURRENT PARKING", current,   C_ACCENT);
                addSection(sectionsWrapper, "⏳ PENDING PAYMENT", pending,   new Color(255, 193, 7));
                addSection(sectionsWrapper, "✓ COMPLETED",        completed, C_AVAILABLE);
            }

            sectionsWrapper.revalidate();
            sectionsWrapper.repaint();
        });

        // Initial load
        refreshBtn.doClick();

        root.add(content, BorderLayout.CENTER);
        return root;
    }

    // =========================================================================
    // Transaction classification helpers
    // =========================================================================

    /**
     * Currently parked: transaction not completed and vehicle has not yet exited.
     */
    private static boolean isCurrentTransaction(ParkingTransaction txn) {
        return !txn.isCompleted() && txn.getExitTime() == null;
    }

    /**
     * Pending payment: vehicle has exited (exit time recorded) but transaction
     * is not yet marked completed / payment is not PAID.
     */
    private static boolean isPendingTransaction(ParkingTransaction txn) {
        String paymentStatus = txn.getPaymentStatus() != null ? txn.getPaymentStatus() : "PENDING";
        return !txn.isCompleted()
                && txn.getExitTime() != null
                && !paymentStatus.equalsIgnoreCase("PAID");
    }

    // =========================================================================
    // Section builder
    // =========================================================================

    /**
     * Adds a labelled section heading followed by a 2-column card grid to the
     * given parent. When the group is empty a subtle "None" placeholder is shown
     * instead of leaving the section blank.
     */
    private static void addSection(JPanel parent,
                                   String title,
                                   List<TransactionWithDetails> group,
                                   Color accentColor) {
        // Section header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        header.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel sectionLabel = UIFactory.lbl(title, Font.BOLD, 13, accentColor);
        header.add(sectionLabel, BorderLayout.WEST);

        // Count badge
        JLabel countBadge = UIFactory.lbl(String.valueOf(group.size()), Font.BOLD, 11, accentColor);
        countBadge.setHorizontalAlignment(SwingConstants.RIGHT);
        header.add(countBadge, BorderLayout.EAST);

        parent.add(header);

        // Thin accent line under header
        JSeparator accentLine = new JSeparator();
        accentLine.setForeground(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 80));
        accentLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        parent.add(accentLine);
        parent.add(Box.createVerticalStrut(12));

        if (group.isEmpty()) {
            JPanel emptyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            emptyRow.setOpaque(false);
            emptyRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            emptyRow.add(UIFactory.lbl("None", Font.PLAIN, 12, C_MUTED));
            parent.add(emptyRow);
        } else {
            // 2-column grid of cards
            JPanel grid = new JPanel(new GridLayout(0, 2, 20, 20));
            grid.setOpaque(false);
            grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            for (TransactionWithDetails txn : group) {
                grid.add(buildTransactionCard(txn, accentColor));
            }

            // If odd number of cards, fill last cell with an invisible placeholder
            if (group.size() % 2 != 0) {
                JPanel placeholder = new JPanel();
                placeholder.setOpaque(false);
                grid.add(placeholder);
            }

            parent.add(grid);
        }

        // Spacing between sections
        parent.add(Box.createVerticalStrut(32));
    }

    // =========================================================================
    // Data loading
    // =========================================================================

    private record TransactionWithDetails(
            ParkingTransaction transaction,
            Vehicle vehicle,
            ParkingSlot slot
    ) {}

    private static List<TransactionWithDetails> loadUserTransactions(AppState state) {
        List<TransactionWithDetails> result = new ArrayList<>();
        try {
            model.UserAccount currentUser = state.getCurrentUserAccount();
            if (currentUser == null) return result;

            VehicleDAO vehicleDAO = new VehicleDAO();
            List<Vehicle> vehicles = vehicleDAO.findAllByUserId(currentUser.getUserId());

            ParkingTransactionDAO txnDAO = new ParkingTransactionDAO();
            ParkingSlotDAO slotDAO = new ParkingSlotDAO();

            for (Vehicle v : vehicles) {
                List<ParkingTransaction> txns = txnDAO.findByVehicleId(v.getVehicleId());
                for (ParkingTransaction txn : txns) {
                    Optional<ParkingSlot> slotOpt = slotDAO.findById(txn.getSlotId());
                    slotOpt.ifPresent(slot -> result.add(new TransactionWithDetails(txn, v, slot)));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    // =========================================================================
    // Card rendering
    // =========================================================================

    private static JPanel buildTransactionCard(TransactionWithDetails entry, Color sectionAccent) {
        ParkingTransaction txn = entry.transaction();
        Vehicle vehicle = entry.vehicle();
        ParkingSlot slot = entry.slot();

        JPanel card = UIFactory.cardPanel(new GridBagLayout());
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0;
        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.weightx = 1.0;

        // Status badge — colour matches the section accent
        cc.gridy = 0;
        cc.insets = new Insets(0, 0, 12, 0);
        String statusText;
        if (isCurrentTransaction(txn)) {
            statusText = "🅿 CURRENTLY PARKED";
        } else if (isPendingTransaction(txn)) {
            statusText = "⏳ PENDING PAYMENT";
        } else {
            statusText = "✓ COMPLETED";
        }
        JLabel statusBadge = UIFactory.lbl(statusText, Font.BOLD, 12, sectionAccent);
        statusBadge.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(statusBadge, cc);

        // Divider
        cc.gridy = 1;
        cc.insets = new Insets(0, 0, 12, 0);
        card.add(divider(), cc);

        // Details
        cc.insets = new Insets(4, 0, 4, 0);

        cc.gridy = 2;
        card.add(infoRow("PLATE NUMBER", vehicle.getPlateNumber()), cc);

        cc.gridy = 3;
        card.add(infoRow("VEHICLE TYPE", vehicle.getVehicleType()), cc);

        cc.gridy = 4;
        card.add(infoRow("PARKING SLOT", slot.getSlotCode()), cc);

        cc.gridy = 5;
        card.add(infoRow("ZONE", slot.getZone()), cc);

        cc.gridy = 6;
        String entryTimeStr = txn.getEntryTime() != null
                ? txn.getEntryTime().format(DATE_FORMAT)
                : "—";
        card.add(infoRow("ENTRY TIME", entryTimeStr), cc);

        cc.gridy = 7;
        String exitTimeStr = txn.getExitTime() != null
                ? txn.getExitTime().format(DATE_FORMAT)
                : "—";
        card.add(infoRow("EXIT TIME", exitTimeStr), cc);

        cc.gridy = 8;
        String durationStr = txn.getDurationMinutes() != null
                ? txn.getDurationMinutes() + " min"
                : "—";
        card.add(infoRow("DURATION", durationStr), cc);

        cc.gridy = 9;
        String feeStr = txn.getCalculatedFee() != null
                ? "₱" + String.format("%.2f", txn.getCalculatedFee())
                : "—";
        card.add(infoRow("CALCULATED FEE", feeStr), cc);

        cc.gridy = 10;
        cc.insets = new Insets(4, 0, 0, 0);
        String paymentStatus = txn.getPaymentStatus() != null ? txn.getPaymentStatus() : "PENDING";
        Color paymentColor = paymentStatus.equalsIgnoreCase("PAID") ? C_AVAILABLE : C_MUTED;
        card.add(infoRowColored("PAYMENT STATUS", paymentStatus, paymentColor), cc);

        return card;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static JSeparator divider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 40));
        return sep;
    }

    private static JPanel infoRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);

        JLabel lbl = UIFactory.lbl(label, Font.BOLD, 10, C_MUTED);
        row.add(lbl, BorderLayout.WEST);

        JLabel val = UIFactory.lbl(value, Font.PLAIN, 12, C_WHITE);
        val.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(val, BorderLayout.EAST);

        return row;
    }

    private static JPanel infoRowColored(String label, String value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);

        JLabel lbl = UIFactory.lbl(label, Font.BOLD, 10, C_MUTED);
        row.add(lbl, BorderLayout.WEST);

        JLabel val = UIFactory.lbl(value, Font.PLAIN, 12, valueColor);
        val.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(val, BorderLayout.EAST);

        return row;
    }
}