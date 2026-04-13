package sparcs.ui.panels;

import sparcs.ui.components.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class FeeManagementPanel extends JPanel {

    private final boolean isAdmin;

    public FeeManagementPanel(boolean isAdmin) {
        this.isAdmin = isAdmin;
        setLayout(new BorderLayout());
        setOpaque(false);

        GradientPanel bg = new GradientPanel();
        bg.setLayout(new BorderLayout());
        bg.setBorder(new EmptyBorder(20, 24, 20, 24));
        add(bg, BorderLayout.CENTER);

        JPanel inner = new JPanel(new BorderLayout(0, 16));
        inner.setOpaque(false);
        bg.add(inner, BorderLayout.CENTER);

        String title = isAdmin ? "FEE MANAGEMENT" : "FEE SCHEDULE";
        inner.add(new PageHeader(title), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(2, 1, 0, 14));
        body.setOpaque(false);

        // --- Top card: Fee Schedule / Parking Rates ---
        CardPanel topCard = new CardPanel();
        topCard.setLayout(new BorderLayout(0, 10));
        topCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        String topTitle = isAdmin ? "FEE SCHEDULE" : "PARKING RATES";
        JLabel tl = sectionLabel(topTitle);
        topCard.add(tl, BorderLayout.NORTH);

        // TODO: populate fee schedule table from SQL
        JPanel topBody = new JPanel();
        topBody.setOpaque(false);
        topCard.add(topBody, BorderLayout.CENTER);

        // --- Bottom card ---
        CardPanel botCard = new CardPanel();
        botCard.setLayout(new BorderLayout(0, 10));
        botCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        String botTitle = isAdmin ? "OUTSTANDING FEES" : "HOW FEES WORK";
        JLabel bl = sectionLabel(botTitle);
        botCard.add(bl, BorderLayout.NORTH);

        // TODO: populate outstanding fees / explanation from SQL / config
        JPanel botBody = new JPanel();
        botBody.setOpaque(false);
        botCard.add(botBody, BorderLayout.CENTER);

        body.add(topCard);
        body.add(botCard);
        inner.add(body, BorderLayout.CENTER);
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(SPARCSTheme.boldFont(16));
        l.setForeground(SPARCSTheme.ACCENT_PINK);
        return l;
    }
}
