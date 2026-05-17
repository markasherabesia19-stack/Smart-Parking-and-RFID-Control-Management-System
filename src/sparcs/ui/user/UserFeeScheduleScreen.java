package ui.user;

import model.AppState;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

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

        // ── Content — card centered in screen ────────────────────────────────
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        // Card — custom painted with accent bar + design system border
        JPanel card = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // 3px accent bar — purple/info
                g2.setColor(new Color(0x7C5CBF));
                g2.fillRoundRect(0, 0, getWidth(), 6, 12, 12);
                g2.fillRect(0, 3, getWidth(), 3);
                // border
                g2.setColor(new Color(0x2D2860));
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(500, 420));

        // Card header
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);
        cardHeader.setBorder(new EmptyBorder(18, 24, 14, 24));
        cardHeader.add(UIFactory.lbl("CURRENT PARKING RATES", Font.BOLD, 13, new Color(0xF0ECFF)), BorderLayout.WEST);

        JPanel headerDivider = new JPanel();
        headerDivider.setBackground(new Color(0x2D2860));
        headerDivider.setPreferredSize(new Dimension(0, 1));
        headerDivider.setOpaque(true);
        cardHeader.add(headerDivider, BorderLayout.SOUTH);
        card.add(cardHeader, BorderLayout.NORTH);

        // Rate rows
        String[][] rates = {
            {"First Hour",          "₱30",  "Minimum charge per session"},
            {"Succeeding Hours",    "₱20",  "Per additional hour after the first"},
            {"Overnight (12 hrs+)", "₱150", "Single overnight flat rate"},
        };

        JPanel rateList = new JPanel();
        rateList.setLayout(new BoxLayout(rateList, BoxLayout.Y_AXIS));
        rateList.setOpaque(false);
        rateList.setBorder(new EmptyBorder(6, 24, 16, 24));

        for (int i = 0; i < rates.length; i++) {
            String[] r = rates[i];

            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(14, 0, 14, 0));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

            // Left: name + subtitle
            JPanel left = new JPanel();
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            left.setOpaque(false);
            JLabel name = UIFactory.lbl(r[0], Font.BOLD, 13, new Color(0xF0ECFF));
            name.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel sub = UIFactory.lbl(r[2], Font.PLAIN, 11, new Color(0x9B8FD4));
            sub.setAlignmentX(Component.LEFT_ALIGNMENT);
            left.add(name);
            left.add(Box.createVerticalStrut(3));
            left.add(sub);

            // Right: green rate badge
            JPanel rightWrap = new JPanel(new GridBagLayout());
            rightWrap.setOpaque(false);

            JPanel badge = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(29, 185, 84, 25));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(new Color(0x1DB954));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                    g2.dispose();
                }
            };
            badge.setOpaque(false);
            badge.setBorder(new EmptyBorder(5, 14, 5, 14));

            JLabel rateLabel = new JLabel(r[1]);
            rateLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            rateLabel.setForeground(new Color(0x1DB954));
            rateLabel.setOpaque(false);
            badge.add(rateLabel, new GridBagConstraints());
            rightWrap.add(badge, new GridBagConstraints());

            row.add(left, BorderLayout.CENTER);
            row.add(rightWrap, BorderLayout.EAST);

            JPanel wrapped = new JPanel(new BorderLayout());
            wrapped.setOpaque(false);
            wrapped.add(row, BorderLayout.CENTER);
            if (i < rates.length - 1) {
                JPanel sep = new JPanel();
                sep.setBackground(new Color(0x1E1C45));
                sep.setPreferredSize(new Dimension(0, 1));
                sep.setOpaque(true);
                wrapped.add(sep, BorderLayout.SOUTH);
            }
            rateList.add(wrapped);
        }
        card.add(rateList, BorderLayout.CENTER);

        // Note at bottom
        JLabel note = UIFactory.lbl(
            "* Rates are subject to change. Check with the parking office for the latest schedule.",
            Font.PLAIN, 10, new Color(0x9B8FD4));
        note.setHorizontalAlignment(SwingConstants.CENTER);
        note.setBorder(new EmptyBorder(0, 0, 16, 0));
        card.add(note, BorderLayout.SOUTH);

        center.add(card, new GridBagConstraints());
        content.add(center, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }
}