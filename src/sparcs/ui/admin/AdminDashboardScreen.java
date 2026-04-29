package ui.admin;

import dao.VehicleDAO;
import dao.VehicleOwnerDAO;
import dao.UserAccountDAO;
import dao.ParkingSlotDAO;
import model.AppState;
import model.Vehicle;
import model.VehicleOwner;
import model.UserAccount;
import model.ParkingSlot;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * SPARCS — Admin main dashboard.
 * TODO (back-end): Replace mock stat values and activity rows with live DB queries.
 */
public class AdminDashboardScreen {

    // Persisted across navigations — created once, never rebuilt
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
        statsRow.add(UIFactory.statCard("Available Slots", String.valueOf(state.availableSlots), C_AVAILABLE));
        statsRow.add(UIFactory.statCard("Occupied",        String.valueOf(state.occupiedSlots),  C_OCCUPIED));
        // TODO (back-end): Load revenue and pending fees from DB
        statsRow.add(UIFactory.statCard("Revenue Today",   "0",                                  C_ACCENT));
        statsRow.add(UIFactory.statCard("Pending Fees",    "0",                                  C_RESERVED));

        // Body
        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 20, 20, 20));

        // ── Vehicles Overview (currently parked) ──────────────────────────────
        JPanel vehiclesCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        vehiclesCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        vehiclesCard.add(UIFactory.lbl("VEHICLES OVERVIEW", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);

        // Create the model only once; reuse it on subsequent navigations so
        // the table data persists while the user is on other screens.
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

        // Always do a fresh load when build() is called (every navigation).
        reloadParkedVehicles(vehiclesModel, state);

        // Register slot-change listener once. If a previous one exists, remove
        // it first so we never stack duplicate listeners across navigations.
        if (registeredListener != null) {
            state.removeSlotChangeListener(registeredListener);
        }
        registeredListener = () -> SwingUtilities.invokeLater(() ->
            reloadParkedVehicles(vehiclesModel, state));
        state.addSlotChangeListener(registeredListener);

        JPanel actCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        actCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        actCard.add(UIFactory.lbl("RECENT ACTIVITY", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);
        // TODO (back-end): Load live entry/exit log from DB via AuditLogDAO
        JPanel actList = new JPanel();
        actList.setLayout(new BoxLayout(actList, BoxLayout.Y_AXIS));
        actList.setOpaque(false);
        // Activity rows will be populated dynamically from database
        JScrollPane actScroll = new JScrollPane(actList);
        actScroll.setBorder(null); actScroll.setOpaque(false); actScroll.getViewport().setOpaque(false);
        actCard.add(actScroll, BorderLayout.CENTER);
        body.add(actCard);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body, BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
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

    private static JPanel activityRow(String plate, String action, String slot, String time) {
        JPanel row = new JPanel(new GridLayout(1, 4));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(6, 0, 6, 0));
        row.add(UIFactory.lbl(plate,  Font.BOLD,  12, C_WHITE));
        row.add(UIFactory.lbl(action, Font.PLAIN, 12, action.equals("Entry") ? C_AVAILABLE : C_OCCUPIED));
        row.add(UIFactory.lbl(slot,   Font.PLAIN, 12, C_MUTED));
        row.add(UIFactory.lbl(time,   Font.PLAIN, 11, C_MUTED));
        return row;
    }
}