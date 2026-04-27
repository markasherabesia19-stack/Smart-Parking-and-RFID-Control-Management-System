package ui.shared;
 
import model.AppState;
import util.UIFactory;
import static util.UIConstants.*;
 
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
 
public class SidebarPanel {
 
    private static final String[][] ADMIN_ITEMS = {
        {"Monitor",               ""},
        {"  Dashboard",           "ADMIN_DASHBOARD"},
        {"  Slot Map",            "ADMIN_SLOT_MAP"},
        {"Operations",            ""},
        {"  Entry / Exit",        "ADMIN_ENTRY_EXIT"},
        {"  Vehicles",            "ADMIN_VEHICLES"},
        {"  Register Vehicle",    "ADMIN_REGISTER"},
        {"Admin Only",            ""},
        {"  Fees",                "ADMIN_FEES"},
        {"  Reports",             "ADMIN_REPORTS"},
        {"  Audit Log",           "ADMIN_AUDIT_LOG"},
    };
 
    private static final String[][] USER_ITEMS = {
        {"My Account",            ""},
        {"  Dashboard",           "USER_DASHBOARD"},
        {"  My Status",           "USER_MY_STATUS"},
        {"  Slot View",           "USER_SLOT_VIEW"},
        {"Records",               ""},
        {"  History",             "USER_HISTORY"},
        {"  Fee Schedule",        "USER_FEE_SCHEDULE"},
        {"  RFID Card",           "USER_RFID_CARD"},
    };
 
    // Fix 1: Define solid, visible colors as constants so alpha issues don't occur
    private static final Color COLOR_ACTIVE_BG = new Color(105, 48, 195);   // solid purple
    private static final Color COLOR_HOVER_BG  = new Color(80,  50, 150);   // solid darker purple
 
    public static JPanel build(CardLayout cardLayout, JPanel rootPanel,
                               AppState state, String role, String activeScreen) {
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // Semi-transparent dark overlay so nav text stays readable
                // over the shared background image.
                g2.setColor(new Color(20, 10, 50, 170));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // right-edge separator
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRect(getWidth() - 1, 0, 1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(185, 0));
        sidebar.setOpaque(false);
 
        // Logo + brand
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        logoRow.setOpaque(false);
        logoRow.setBorder(new EmptyBorder(18, 0, 4, 0));
        logoRow.add(UIFactory.logoPanel(36));
        sidebar.add(logoRow);
 
        JLabel brand = UIFactory.lbl("SPARCS", Font.BOLD, 15, C_WHITE);
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);
        brand.setBorder(new EmptyBorder(0, 0, 20, 0));
        sidebar.add(brand);
 
        // Nav items
        String[][] items = role.equals("ADMIN") ? ADMIN_ITEMS : USER_ITEMS;
        for (String[] item : items) {
            boolean isHeader   = !item[0].startsWith("  ");
            boolean isActive   = item[1].equals(activeScreen);
            boolean isClickable = !item[1].isEmpty();
 
            // Fix 2: Use a custom-painted panel so background alpha works correctly
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 5)) {
                @Override protected void paintComponent(Graphics g) {
                    if (isActive) {
                        g.setColor(COLOR_ACTIVE_BG);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    } else if (Boolean.TRUE.equals(getClientProperty("hovered"))) {
                        g.setColor(COLOR_HOVER_BG);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                    super.paintComponent(g);
                }
            };
            // Fix 3: Always non-opaque so our custom paintComponent controls the background
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(185, 32));
 
            Color textColor = isHeader ? C_MUTED
                            : isActive  ? C_WHITE
                            : new Color(200, 190, 230);
 
            row.add(UIFactory.lbl(item[0],
                    isHeader ? Font.BOLD : Font.PLAIN,
                    isHeader ? 10        : 13,
                    textColor));
 
            if (isClickable) {
                row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                String target = item[1];
                row.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        cardLayout.show(rootPanel, target);
                    }
                    @Override public void mouseEntered(MouseEvent e) {
                        if (!isActive) {
                            row.putClientProperty("hovered", true);
                            row.repaint();
                        }
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        if (!isActive) {
                            row.putClientProperty("hovered", false);
                            row.repaint();
                        }
                    }
                });
            }
            sidebar.add(row);
        }
 
        sidebar.add(Box.createVerticalGlue());
 
        // Sign-out button
        JButton signOut = new JButton("  Sign Out") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                if (getModel().isRollover()) {
                    g2.setColor(new Color(210, 50, 90));   // Fix 4: solid rollover color
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.setColor(new Color(220, 80, 100));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), 14, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        signOut.setFont(new Font("SansSerif", Font.PLAIN, 13));
        signOut.setBorderPainted(false);
        signOut.setContentAreaFilled(false);
        signOut.setFocusPainted(false);
        signOut.setMaximumSize(new Dimension(185, 40));
        signOut.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signOut.addActionListener(e -> {
            state.clearSession();
            cardLayout.show(rootPanel, "ROLE_PICKER");
        });
        sidebar.add(signOut);
        sidebar.add(Box.createVerticalStrut(10));
 
        return sidebar;
    }
}