package ui.user;

import dao.UserAccountDAO;
import model.AppState;
import model.UserAccount;
import ui.shared.SidebarPanel;
import ui.shared.SlotGridPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigDecimal;
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
        content.add(topBar, BorderLayout.NORTH);

        // Stats row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));

        // TODO (back-end): Load current parking session from DB via ParkingTransactionDAO
        JPanel currentSlotCard  = UIFactory.statCard("Current Slot",   "-", C_ACCENT);
        JPanel durationCard     = UIFactory.statCard("Duration",        "-", C_AVAILABLE);
        JPanel estimatedFeeCard = UIFactory.statCard("Estimated Fee",   "-", C_RESERVED);

        // Load wallet balance from DB
        JPanel walletCard = UIFactory.statCard("Wallet Balance", fetchWalletBalance(state), C_PINK);

        statsRow.add(currentSlotCard);
        statsRow.add(durationCard);
        statsRow.add(estimatedFeeCard);
        statsRow.add(walletCard);

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
        // Activity rows will be populated dynamically from database
        actCard.add(actList, BorderLayout.CENTER);
        body.add(actCard);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body, BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        // Refresh wallet balance every time this screen becomes visible
        root.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                String balance = fetchWalletBalance(state);
                // statCard puts value in BorderLayout.CENTER
                Component centerComp = ((BorderLayout) walletCard.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                if (centerComp instanceof JLabel) {
                    ((JLabel) centerComp).setText(balance);
                }
                walletCard.revalidate();
                walletCard.repaint();
            }
        });

        return root;
    }

    //Queries the DB for the current user's wallet balance.
    private static String fetchWalletBalance(AppState state) {
        if (state.currentUsername == null || state.currentUsername.isEmpty()) {
            return "-";
        }
        try {
            UserAccountDAO userDAO = new UserAccountDAO();
            Optional<UserAccount> userOpt = userDAO.findByUsername(state.currentUsername);
            if (userOpt.isEmpty()) return "-";
            BigDecimal bal = userDAO.getWalletBalance(userOpt.get().getUserId());
            return "P" + bal.toPlainString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "-";
        }
    }


}