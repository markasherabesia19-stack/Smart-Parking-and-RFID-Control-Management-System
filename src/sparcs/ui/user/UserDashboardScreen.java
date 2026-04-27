package ui.user;

import dao.*;
import model.AppState;
import model.ParkingTransaction;
import model.VehicleOwner;
import ui.shared.SidebarPanel;
import ui.shared.SlotGridPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UserDashboardScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "USER", "USER_DASHBOARD"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        String greeting = "HELLO, " + (state.currentUsername.isEmpty() ? "USER" : state.currentUsername.toUpperCase()) + "!";
        topBar.add(UIFactory.lbl(greeting, Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        JButton signOutBtn = new JButton("SIGN OUT");
        UIFactory.styleSmallBtn(signOutBtn);
        signOutBtn.addActionListener(e -> { state.clearSession(); cardLayout.show(rootPanel, "ROLE_PICKER"); });
        topBar.add(signOutBtn, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // Load user's parking history from database
        ParkingTransaction currentTransaction = null;
        try {
            if (state.getCurrentUserAccount() != null) {
                // Get current/latest parking transaction for this user
                currentTransaction = loadCurrentParking(state);
            }
        } catch (SQLException ex) {
            System.err.println("Error loading user parking history: " + ex.getMessage());
        }

        // Stats row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));
        
        String currentSlot = currentTransaction != null ? "Slot " + currentTransaction.getSlotId() : "None";
        String durationStr = currentTransaction != null ? formatDuration(currentTransaction.getDurationMinutes()) : "-";
        String estimatedFee = currentTransaction != null ? "₱" + currentTransaction.getCalculatedFee() : "₱0";
        String walletBalance = "₱0"; // TODO: Implement wallet feature
        
        statsRow.add(UIFactory.statCard("Current Slot",   currentSlot,  C_ACCENT));
        statsRow.add(UIFactory.statCard("Duration",       durationStr, C_AVAILABLE));
        statsRow.add(UIFactory.statCard("Estimated Fee",  estimatedFee,   C_RESERVED));
        statsRow.add(UIFactory.statCard("Wallet Balance", walletBalance,  C_PINK));

        // Body
        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 20, 20, 20));

        JPanel mapCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        mapCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        mapCard.add(UIFactory.lbl("PARKING MAP", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);
        mapCard.add(SlotGridPanel.buildMiniGrid(state, true), BorderLayout.CENTER);
        body.add(mapCard);

        JPanel actCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        actCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        actCard.add(UIFactory.lbl("RECENT ACTIVITY", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);

        JPanel actList = new JPanel();
        actList.setLayout(new BoxLayout(actList, BoxLayout.Y_AXIS));
        actList.setOpaque(false);
        
        // Load user's parking history from database
        try {
            if (state.getCurrentUserAccount() != null) {
                ParkingTransactionDAO transDAO = new ParkingTransactionDAO();
                List<ParkingTransaction> userTransactions = transDAO.findByUserId(state.getCurrentUserAccount().getUserId());
                
                for (ParkingTransaction trans : userTransactions) {
                    String action = trans.getExitTime() == null ? "Entry" : "Exit";
                    String time = trans.getEntryTime().toString().substring(11, 16);
                    String transDuration = formatDuration(trans.getDurationMinutes());
                    
                    JPanel row = new JPanel(new GridLayout(1, 3));
                    row.setOpaque(false);
                    row.setBorder(new EmptyBorder(6, 0, 6, 0));
                    Color ac = action.equals("Entry") ? C_AVAILABLE : C_OCCUPIED;
                    row.add(UIFactory.lbl(action, Font.BOLD,  12, ac));
                    row.add(UIFactory.lbl(time, Font.PLAIN, 12, C_WHITE));
                    row.add(UIFactory.lbl(transDuration, Font.PLAIN, 11, C_MUTED));
                    actList.add(row);
                }
                
                if (userTransactions.isEmpty()) {
                    JPanel empty = new JPanel();
                    empty.setOpaque(false);
                    empty.add(UIFactory.lbl("No parking history", Font.PLAIN, 12, C_MUTED));
                    actList.add(empty);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error loading user activity: " + ex.getMessage());
            JPanel error = new JPanel();
            error.setOpaque(false);
            error.add(UIFactory.lbl("Error loading activity", Font.PLAIN, 12, C_MUTED));
            actList.add(error);
        }
        
        actCard.add(actList, BorderLayout.CENTER);
        body.add(actCard);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body, BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private static ParkingTransaction loadCurrentParking(AppState state) throws SQLException {
        // Try to find user's vehicle owner record
        VehicleOwnerDAO ownerDAO = new VehicleOwnerDAO();
        Optional<VehicleOwner> ownerOpt = ownerDAO.findByUserId(state.getCurrentUserAccount().getUserId());
        
        if (ownerOpt.isPresent()) {
            // Get the user's latest parking transaction
            ParkingTransactionDAO transDAO = new ParkingTransactionDAO();
            List<ParkingTransaction> transactions = transDAO.findByUserId(state.getCurrentUserAccount().getUserId());
            
            if (!transactions.isEmpty()) {
                return transactions.get(0); // Most recent
            }
        }
        return null;
    }

    private static String formatDuration(Integer minutes) {
        if (minutes == null || minutes <= 0) return "-";
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (hours > 0) {
            return hours + "h " + mins + "m";
        } else {
            return mins + "m";
        }
    }
}
