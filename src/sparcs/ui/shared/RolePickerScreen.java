package ui.shared;

import model.AppState;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class RolePickerScreen {
    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel p = UIFactory.backgroundImagePanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;

        gc.gridy = 0;
        p.add(UIFactory.logoPanel(64), gc);

        gc.gridy = 2;
        gc.insets = new Insets(0, 0, 24, 0);
        p.add(UIFactory.lbl("SIGN IN AS", Font.PLAIN, 12, C_MUTED), gc);

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 14, 0));
        btnRow.setOpaque(false);

        JPanel adminTile = roleTile("ADMIN", "⚙", "Full system access");
        adminTile.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                state.currentRole = "ADMIN";
                cardLayout.show(rootPanel, "ADMIN_LOGIN");
            }
        });

        JPanel userTile = roleTile("USER", "👤", "Vehicle owner portal");
        userTile.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                state.currentRole = "USER";
                cardLayout.show(rootPanel, "USER_LOGIN");
            }
        });

        btnRow.add(adminTile);
        btnRow.add(userTile);

        gc.gridy = 3;
        gc.insets = new Insets(0, 0, 0, 0);
        p.add(btnRow, gc);
        return p;
    }

    private static JPanel roleTile(String role, String icon, String subtitle) {
        JPanel tile = new JPanel(new GridBagLayout()) {
            private boolean hover = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                    @Override public void mouseExited (MouseEvent e) { hover = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? C_PURPLE : C_BG_CARD);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(C_PURPLE.getRed(), C_PURPLE.getGreen(), C_PURPLE.getBlue(), hover ? 160 : 60),
                    getWidth(), getHeight(), new Color(C_PINK.getRed(), C_PINK.getGreen(), C_PINK.getBlue(), hover ? 120 : 40));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.setColor(C_ACCENT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.dispose();
            }
        };
        tile.setOpaque(false);
        tile.setPreferredSize(new Dimension(150, 130));
        tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;
        tile.add(UIFactory.lbl(icon, Font.PLAIN, 28, C_WHITE), gc);
        gc.gridy = 1; gc.insets = new Insets(8, 0, 2, 0);
        tile.add(UIFactory.lbl(role, Font.BOLD, 15, C_WHITE), gc);
        gc.gridy = 2; gc.insets = new Insets(0, 0, 0, 0);
        tile.add(UIFactory.lbl(subtitle, Font.PLAIN, 10, C_MUTED), gc);
        return tile;
    }
}