package ui.admin;

import dao.VehicleDAO;
import dao.VehicleOwnerDAO;
import dao.UserAccountDAO;
import dao.ParkingSlotDAO;
import dao.AuditLogDAO;
import dao.ParkingTransactionDAO;
import model.AppState;
import model.Vehicle;
import model.VehicleOwner;
import model.UserAccount;
import model.ParkingSlot;
import model.AuditLog;
import java.time.format.DateTimeFormatter;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import model.ParkingTransaction;

public class AdminDashboardScreen {

    private static DefaultTableModel vehiclesModel;
    private static Runnable registeredListener = null;

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_DASHBOARD"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("DASHBOARD", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        // Stats row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));
        buildStatsRow(statsRow, state);

        // Body
        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 20, 20, 20));

        // Vehicles Overview 
        JPanel vehiclesCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        vehiclesCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        vehiclesCard.add(UIFactory.lbl("VEHICLES OVERVIEW", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);
        if (vehiclesModel == null) {
            String[] vCols = {"Plate", "Username", "Slot"};
            vehiclesModel = new DefaultTableModel(new Object[0][3], vCols) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
        }

        JTable vehiclesTable = new JTable(vehiclesModel);
        styleOverviewTable(vehiclesTable);

        JScrollPane vehiclesScroll = new JScrollPane(vehiclesTable);
        vehiclesScroll.setBorder(null);
        vehiclesScroll.setOpaque(false);
        vehiclesScroll.getViewport().setBackground(C_BG_CARD);

        vehiclesCard.add(vehiclesScroll, BorderLayout.CENTER);
        body.add(vehiclesCard);
        reloadParkedVehicles(vehiclesModel, state);
        
        if (registeredListener != null) {
            state.removeSlotChangeListener(registeredListener);
        }
        registeredListener = () -> SwingUtilities.invokeLater(() ->
            reloadParkedVehicles(vehiclesModel, state));
        state.addSlotChangeListener(registeredListener);

        JPanel actCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        actCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        actCard.add(UIFactory.lbl("RECENT ACTIVITY", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);

        // Use a JTable for proper column/row structure
        String[] actCols = {"Plate", "Action", "Slot", "Time"};
        DefaultTableModel actModel = new DefaultTableModel(new Object[0][4], actCols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable actTable = new JTable(actModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                String action = (String) getModel().getValueAt(row, 1);
                if (col == 1) {
                    c.setForeground("ENTRY".equals(action) ? C_AVAILABLE : C_OCCUPIED);
                } else {
                    c.setForeground(C_WHITE);
                }
                c.setBackground(C_BG_CARD);
                return c;
            }
        };
        styleOverviewTable(actTable);

        JScrollPane actScroll = new JScrollPane(actTable);
        actScroll.setBorder(null);
        actScroll.setOpaque(false);
        actScroll.getViewport().setBackground(C_BG_CARD);
        actCard.add(actScroll, BorderLayout.CENTER);
        body.add(actCard);

        // Initial load of recent activity
        reloadRecentActivity(actModel);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body, BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        // ── Auto-refresh stats (mirrors AdminSlotMapScreen pattern) ──────────
        Runnable refreshStats = () -> {
            statsRow.removeAll();
            buildStatsRow(statsRow, state);
            statsRow.revalidate();
            statsRow.repaint();
        };

        // Path 1: user navigates to this screen
        root.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                state.loadSlotDataFromDB();
                refreshStats.run();
                reloadParkedVehicles(vehiclesModel, state);
                reloadRecentActivity(actModel);
            }
        });

        // Path 2: entry/exit fires notifySlotChange()
        state.addSlotChangeListener(() -> SwingUtilities.invokeLater(() -> {
            state.loadSlotDataFromDB();
            refreshStats.run();
            reloadRecentActivity(actModel);
        }));

        return root;
    }

    private static void buildStatsRow(JPanel statsRow, AppState state) {
        statsRow.add(UIFactory.statCard("Available Slots", String.valueOf(state.availableSlots), C_AVAILABLE));
        statsRow.add(UIFactory.statCard("Occupied",        String.valueOf(state.occupiedSlots),  C_OCCUPIED));
        statsRow.add(UIFactory.statCard("Revenue Today",   calculateRevenue(),                    C_ACCENT));
        statsRow.add(UIFactory.statCard("Pending Fees",    calculatePendingFees(),                C_RESERVED));
    }

    private static String calculateRevenue() {
        try {
            ParkingTransactionDAO txDAO = new ParkingTransactionDAO();
            List<ParkingTransaction> paidTxs = txDAO.findByPaymentStatus("PAID");
            int totalRevenue = 0;
            for (ParkingTransaction tx : paidTxs) {
                if (tx.getCalculatedFee() != null) {
                    totalRevenue += tx.getCalculatedFee().intValue();
                }
            }
            return "P" + totalRevenue;
        } catch (Exception e) {
            e.printStackTrace();
            return "0";
        }
    }

    private static String calculatePendingFees() {
        try {
            ParkingTransactionDAO txDAO = new ParkingTransactionDAO();
            List<ParkingTransaction> pendingTxs = txDAO.findInProgress();
            return String.valueOf(pendingTxs.size());
        } catch (Exception e) {
            e.printStackTrace();
            return "0";
        }
    }

    private static void reloadParkedVehicles(DefaultTableModel model, AppState state) {
        model.setRowCount(0);
        try {
            VehicleDAO vehicleDAO         = new VehicleDAO();
            VehicleOwnerDAO ownerDAO      = new VehicleOwnerDAO();
            UserAccountDAO userAccountDAO = new UserAccountDAO();
            ParkingSlotDAO slotDAO        = new ParkingSlotDAO();

            List<Vehicle> vehicles = vehicleDAO.findAll();
            for (Vehicle v : vehicles) {
                if (!"Parked".equalsIgnoreCase(v.getParkingStatus())) continue;

                String username = "—";
                try {
                    Optional<VehicleOwner> owner = ownerDAO.findById(v.getOwnerId());
                    if (owner.isPresent() && owner.get().getUserId() != null) {
                        Optional<UserAccount> account = userAccountDAO.findById(owner.get().getUserId());
                        username = account.map(UserAccount::getUsername).orElse("—");
                    }
                } catch (Exception ex) { /* keep default */ }

                // Look up slot directly via current_vehicle_id column
                String slotCode = "—";
                try {
                    Optional<ParkingSlot> slot = slotDAO.findByVehicleId(v.getVehicleId());
                    if (slot.isPresent()) slotCode = slot.get().getSlotCode();
                } catch (Exception ex) { /* keep default */ }

                model.addRow(new Object[]{ v.getPlateNumber(), username, slotCode });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void styleOverviewTable(JTable table) {
        table.setBackground(C_BG_CARD);
        table.setForeground(C_WHITE);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setRowHeight(28);
        table.setGridColor(new Color(60, 50, 100));
        table.setSelectionBackground(C_PURPLE);
        table.setSelectionForeground(C_WHITE);
        table.setOpaque(true);
        JTableHeader header = table.getTableHeader();
        header.setBackground(C_BG_PANEL);
        header.setForeground(C_MUTED);
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));
    }

    private static void reloadRecentActivity(DefaultTableModel model) {
        model.setRowCount(0);
        try {
            AuditLogDAO auditDAO = new AuditLogDAO();
            List<AuditLog> logs = auditDAO.findAll();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd HH:mm");
            int count = 0;
            for (AuditLog log : logs) {
                if (!"ENTRY".equals(log.getAction()) && !"EXIT".equals(log.getAction())) continue;
                if (count++ >= 10) break;

                // changes_log format: "plate=X slot=Y"
                String plate = "—", slot = "—";
                String raw = log.getOldValue(); // mapped from changes_log in DAO
                if (raw != null) {
                    for (String part : raw.split(" ")) {
                        if (part.startsWith("plate=")) plate = part.substring(6);
                        if (part.startsWith("slot="))  slot  = part.substring(5);
                    }
                }

                String time = log.getCreatedAt() != null ? log.getCreatedAt().format(fmt) : "—";
                model.addRow(new Object[]{ plate, log.getAction(), slot, time });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}