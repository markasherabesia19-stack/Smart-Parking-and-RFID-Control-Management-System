package ui.admin;

import dao.*;
import model.AppState;
import model.ParkingTransaction;
import ui.shared.SidebarPanel;
import ui.shared.SlotGridPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * SPARCS — Admin main dashboard.
 * Displays live stats and recent activity from database.
 */
public class AdminDashboardScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        // Load fresh data from database
        try {
            loadDashboardData(state);
        } catch (SQLException ex) {
            System.err.println("Error loading dashboard data: " + ex.getMessage());
        }

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
        JButton signOutBtn = new JButton("SIGN OUT");
        UIFactory.styleSmallBtn(signOutBtn);
        signOutBtn.addActionListener(e -> { state.clearSession(); cardLayout.show(rootPanel, "ROLE_PICKER"); });
        topBar.add(signOutBtn, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // Stats row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));
        statsRow.add(UIFactory.statCard("Available Slots", String.valueOf(state.availableSlots), C_AVAILABLE));
        statsRow.add(UIFactory.statCard("Occupied",        String.valueOf(state.occupiedSlots),  C_OCCUPIED));
        statsRow.add(UIFactory.statCard("Revenue Today",   "₱" + state.revenueToday,             C_ACCENT));
        statsRow.add(UIFactory.statCard("Pending Fees",    String.valueOf(state.pendingFees),   C_RESERVED));

        // Body
        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 20, 20, 20));

        JPanel zoneCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        zoneCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        zoneCard.add(UIFactory.lbl("ZONE OVERVIEW", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);
        zoneCard.add(SlotGridPanel.buildMiniGrid(state, false), BorderLayout.CENTER);
        body.add(zoneCard);

        JPanel actCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        actCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        actCard.add(UIFactory.lbl("RECENT ACTIVITY", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);
        
        // Load recent transactions from database
        JPanel actList = new JPanel();
        actList.setLayout(new BoxLayout(actList, BoxLayout.Y_AXIS));
        actList.setOpaque(false);
        
        try {
            ParkingTransactionDAO transDAO = new ParkingTransactionDAO();
            List<ParkingTransaction> recentTrans = transDAO.findRecent(10);
            for (ParkingTransaction trans : recentTrans) {
                String action = trans.getExitTime() == null ? "Entry" : "Exit";
                String time = trans.getEntryTime().toString().substring(11, 16);
                actList.add(activityRow(trans.getVehicleId() + "", action, "TX-" + trans.getTransactionId(), time));
            }
            if (recentTrans.isEmpty()) {
                actList.add(UIFactory.lbl("No recent activity", Font.PLAIN, 12, C_MUTED));
            }
        } catch (SQLException ex) {
            System.err.println("Error loading recent activity: " + ex.getMessage());
            actList.add(UIFactory.lbl("Error loading activity", Font.PLAIN, 12, C_MUTED));
        }
        
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

    private static JPanel activityRow(String plate, String action, String ref, String time) {
        JPanel row = new JPanel(new GridLayout(1, 4));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(6, 0, 6, 0));
        row.add(UIFactory.lbl(plate,  Font.BOLD,  12, C_WHITE));
        row.add(UIFactory.lbl(action, Font.PLAIN, 12, action.equals("Entry") ? C_AVAILABLE : C_OCCUPIED));
        row.add(UIFactory.lbl(ref,   Font.PLAIN, 12, C_MUTED));
        row.add(UIFactory.lbl(time,   Font.PLAIN, 11, C_MUTED));
        return row;
    }

    private static void loadDashboardData(AppState state) throws SQLException {
        ParkingSlotDAO slotDAO = new ParkingSlotDAO();
        
        // Update slot counts
        state.availableSlots = slotDAO.findByStatus("AVAILABLE").size();
        state.occupiedSlots = slotDAO.findByStatus("OCCUPIED").size();
        
        // Calculate revenue from completed transactions
        ParkingTransactionDAO transDAO = new ParkingTransactionDAO();
        BigDecimal totalRevenue = transDAO.calculateTodayRevenue();
        state.revenueToday = totalRevenue.intValue();
        
        // Count pending fees (exits without payment)
        state.pendingFees = transDAO.countPendingPayments();
        
        System.out.println("[AdminDashboardScreen] Loaded: Available=" + state.availableSlots + 
                         ", Occupied=" + state.occupiedSlots + ", Revenue=" + state.revenueToday + 
                         ", Pending=" + state.pendingFees);
    }
}
