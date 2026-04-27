package ui.user;

import dao.FeeScheduleDAO;
import model.AppState;
import model.FeeSchedule;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

/**
 * SPARCS — User fee schedule screen.
 * Shows the current parking rate schedule for reference.
 * Loads fee schedule from the database.
 */ 

public class UserFeeScheduleScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "USER", "USER_FEE_SCHEDULE"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("FEE SCHEDULE", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        JPanel card = UIFactory.cardPanel(new BorderLayout(0, 16));
        card.setPreferredSize(new Dimension(500, 420));
        card.setBorder(new EmptyBorder(28, 32, 28, 32));

        card.add(UIFactory.lbl("CURRENT PARKING RATES", Font.BOLD, 14, C_WHITE), BorderLayout.NORTH);

        // Rate rows
        JPanel rateList = new JPanel();
        rateList.setLayout(new BoxLayout(rateList, BoxLayout.Y_AXIS));
        rateList.setOpaque(false);

        // Load fee schedule from database
        FeeSchedule schedule = null;
        try {
            FeeScheduleDAO feeDAO = new FeeScheduleDAO();
            Optional<FeeSchedule> activeSchedule = feeDAO.findCurrentActive();
            if (activeSchedule.isPresent()) {
                schedule = activeSchedule.get();
                System.out.println("[UserFeeScheduleScreen] Loaded fee schedule: " + schedule);
            } else {
                System.out.println("[UserFeeScheduleScreen] No active fee schedule found in database");
            }
        } catch (SQLException ex) {
            System.err.println("Error loading fee schedule: " + ex.getMessage());
            ex.printStackTrace();
        }

        // Build rate display
        String[][] rates = buildRateArray(schedule);

        for (String[] r : rates) {
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(12, 0, 12, 0));

            // Left: label + subtitle
            JPanel left = new JPanel(new BorderLayout(0, 2));
            left.setOpaque(false);
            left.add(UIFactory.lbl(r[0], Font.BOLD,  13, C_WHITE), BorderLayout.NORTH);
            left.add(UIFactory.lbl(r[2], Font.PLAIN, 11, C_MUTED), BorderLayout.SOUTH);
            row.add(left, BorderLayout.CENTER);

            // Right: rate
            JLabel rateLabel = UIFactory.lbl(r[1], Font.BOLD, 16, C_ACCENT);
            rateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            row.add(rateLabel, BorderLayout.EAST);

            // Separator line
            JPanel separator = new JPanel();
            separator.setBackground(new Color(60, 50, 100));
            separator.setPreferredSize(new Dimension(0, 1));
            separator.setOpaque(true);

            JPanel wrapped = new JPanel(new BorderLayout());
            wrapped.setOpaque(false);
            wrapped.add(row, BorderLayout.CENTER);
            wrapped.add(separator, BorderLayout.SOUTH);
            rateList.add(wrapped);
        }
        card.add(rateList, BorderLayout.CENTER);

        // Note at bottom
        JLabel note = UIFactory.lbl(
            "* Rates are subject to change. Check with the parking office for the latest schedule.",
            Font.PLAIN, 10, C_MUTED);
        note.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(note, BorderLayout.SOUTH);

        center.add(card, new GridBagConstraints());
        content.add(center, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    /**
     * Build the rates array from the fee schedule model.
     * Falls back to default values if no schedule is loaded.
     */
    private static String[][] buildRateArray(FeeSchedule schedule) {
        if (schedule != null) {
            String ratePerHour = "₱" + schedule.getRatePerHour().setScale(0, java.math.RoundingMode.HALF_UP);
            String ratePerDay = "₱" + schedule.getRatePerDay().setScale(0, java.math.RoundingMode.HALF_UP);
            String gracePeriod = schedule.getGracePeriodMinutes() + " min grace period";

            return new String[][] {
                {"Per Hour",      ratePerHour, "Hourly parking rate"},
                {"Per Day",       ratePerDay,  "Full day flat rate"},
                {"Grace Period",  gracePeriod, "No charge period"},
            };
        } else {
            // Fallback to default rates
            return new String[][] {
                {"First hour",           "₱30",  "Minimum charge per session"},
                {"Succeeding hours",     "₱20",  "Per additional hour"},
                {"Overnight (8 hrs+)",   "₱150", "Single overnight flat rate"},
                {"Lost ticket fee",      "₱500", "Charged if ticket is lost"},
                {"Reservation hold",     "₱20",  "Non-refundable slot hold fee"},
            };
        }
    }
}
