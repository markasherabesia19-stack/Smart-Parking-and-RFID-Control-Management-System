package ui.admin;

import dao.FeeScheduleDAO;
import dao.ParkingTransactionDAO;
import model.AppState;
import model.FeeSchedule;
import model.ParkingTransaction;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * SPARCS — Admin parking fees screen.
 * Displays fee schedule and pending fees from database.
 */
public class AdminFeesScreen {

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

        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Rate table
        JPanel rateCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        rateCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        rateCard.add(UIFactory.lbl("RATE SCHEDULE", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);

        // Load fee schedule from database
        String[] cols = {"Duration", "Rate (₱)"};
        Object[][] data = null;
        try {
            FeeScheduleDAO feeDAO = new FeeScheduleDAO();
            Optional<FeeSchedule> schedule = feeDAO.findCurrentActive();
            if (schedule.isPresent()) {
                FeeSchedule s = schedule.get();
                data = new Object[][] {
                    {"Per Hour", s.getRatePerHour().toPlainString()},
                    {"Per Day", s.getRatePerDay().toPlainString()},
                    {"Grace Period (mins)", String.valueOf(s.getGracePeriodMinutes())}
                };
            } else {
                data = new Object[][] {
                    {"No active schedule", ""}
                };
            }
        } catch (SQLException ex) {
            System.err.println("Error loading fee schedule: " + ex.getMessage());
            data = new Object[][] {
                {"Error loading", ""}
            };
        }
        JTable table = new JTable(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false); scroll.getViewport().setBackground(C_BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));
        rateCard.add(scroll, BorderLayout.CENTER);
        body.add(rateCard);

        // Pending fees panel
        JPanel pendingCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        pendingCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        pendingCard.add(UIFactory.lbl("PENDING FEES", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        
        // Load pending fees from database
        try {
            ParkingTransactionDAO transDAO = new ParkingTransactionDAO();
            List<ParkingTransaction> pending = transDAO.findPendingPayments();
            for (ParkingTransaction t : pending) {
                String plate = "VID-" + t.getVehicleId();
                String fee = "₱" + (t.getCalculatedFee() != null ? t.getCalculatedFee() : "0");
                String duration = t.getDurationMinutes() != null ? (t.getDurationMinutes() / 60) + "h" : "?";
                
                JPanel row = new JPanel(new GridLayout(1, 3));
                row.setOpaque(false); row.setBorder(new EmptyBorder(8, 0, 8, 0));
                row.add(UIFactory.lbl(plate, Font.BOLD,  12, C_WHITE));
                row.add(UIFactory.lbl(fee, Font.BOLD,  13, C_RESERVED));
                row.add(UIFactory.lbl(duration, Font.PLAIN, 11, C_MUTED));
                list.add(row);
            }
            if (pending.isEmpty()) {
                JPanel empty = new JPanel();
                empty.setOpaque(false);
                empty.add(UIFactory.lbl("No pending fees", Font.PLAIN, 12, C_MUTED));
                list.add(empty);
            }
        } catch (SQLException ex) {
            System.err.println("Error loading pending fees: " + ex.getMessage());
            JPanel error = new JPanel();
            error.setOpaque(false);
            error.add(UIFactory.lbl("Error loading fees", Font.PLAIN, 12, C_MUTED));
            list.add(error);
        }
        
        String[][] pending_old = {
            {"ABC-1234", "₱70",  "2h 20m"},
            {"LMN-9012", "₱50",  "1h 40m"},
            {"TUV-7890", "₱150", "Overnight"},
        };
        for (String[] p : pending_old) {
            JPanel row = new JPanel(new GridLayout(1, 3));
            row.setOpaque(false); row.setBorder(new EmptyBorder(8, 0, 8, 0));
            row.add(UIFactory.lbl(p[0], Font.BOLD,  12, C_WHITE));
            row.add(UIFactory.lbl(p[1], Font.BOLD,  13, C_RESERVED));
            row.add(UIFactory.lbl(p[2], Font.PLAIN, 11, C_MUTED));
            list.add(row);
        }
        pendingCard.add(list, BorderLayout.CENTER);
        JButton collectBtn = UIFactory.gradientButton("COLLECT ALL FEES");
        collectBtn.addActionListener(e ->
            JOptionPane.showMessageDialog(null, "All pending fees collected.", "Success", JOptionPane.INFORMATION_MESSAGE));
        pendingCard.add(collectBtn, BorderLayout.SOUTH);
        body.add(pendingCard);

        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
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
}
