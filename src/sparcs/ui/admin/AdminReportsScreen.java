package ui.admin;

import model.AppState;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * SPARCS — Admin reports screen.
 * TODO (back-end): Generate reports from aggregated DB data; add export to CSV/PDF.
 */
public class AdminReportsScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_REPORTS"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("REPORTS", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(2, 2, 14, 14));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 20, 20, 20));

        // TODO (back-end): Load report metrics from DB aggregated queries
        body.add(reportCard("Daily Revenue",  "0", "from database", C_ACCENT));
        body.add(reportCard("Weekly Revenue", "0", "from database", C_AVAILABLE));
        body.add(reportCard("Total Vehicles", "0", "registered in system", C_PURPLE));
        body.add(reportCard("Avg. Duration",  "-", "per parking session", C_RESERVED));

        JPanel exportRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        exportRow.setOpaque(false);
        exportRow.setBorder(new EmptyBorder(0, 20, 16, 20));
        JButton exportBtn = UIFactory.gradientButton("EXPORT REPORT");
        exportBtn.addActionListener(e ->
            JOptionPane.showMessageDialog(null, "Report exported. (TODO: implement file export)", "Export", JOptionPane.INFORMATION_MESSAGE));
        exportRow.add(exportBtn);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(body, BorderLayout.CENTER);
        main.add(exportRow, BorderLayout.SOUTH);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private static JPanel reportCard(String label, String value, String sub, Color valueColor) {
        JPanel card = UIFactory.cardPanel(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));
        card.add(UIFactory.lbl(value, Font.BOLD, 30, valueColor), BorderLayout.CENTER);
        card.add(UIFactory.lbl(label.toUpperCase(), Font.BOLD, 11, C_WHITE), BorderLayout.NORTH);
        card.add(UIFactory.lbl(sub, Font.PLAIN, 11, C_MUTED), BorderLayout.SOUTH);
        return card;
    }
}
