package sparcs.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Left sidebar navigation panel.
 * isAdmin controls which menu sections are visible.
 */
public class SidebarPanel extends JPanel {

    public interface NavListener {
        void onNavigate(String page);
    }

    private NavListener listener;
    private String activePage = "";
    private final List<NavItem> items = new ArrayList<>();
    private final boolean isAdmin;

    public SidebarPanel(boolean isAdmin) {
        this.isAdmin = isAdmin;
        setBackground(SPARCSTheme.SIDEBAR_BG);
        setPreferredSize(new Dimension(220, 0));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(10, 0, 10, 0));
        buildMenu();
    }

    public void setNavListener(NavListener l) { this.listener = l; }

    public void setActivePage(String page) {
        this.activePage = page;
        repaint();
        for (NavItem ni : items) ni.updateStyle();
    }

    private void buildMenu() {
        // Logo area
        JPanel logoArea = new JPanel();
        logoArea.setOpaque(false);
        logoArea.setLayout(new BoxLayout(logoArea, BoxLayout.Y_AXIS));
        logoArea.setBorder(new EmptyBorder(10, 0, 20, 0));
        SPARCSLogo logo = new SPARCSLogo(60, true);
        logo.setAlignmentX(CENTER_ALIGNMENT);
        logoArea.add(logo);
        add(logoArea);

        if (isAdmin) {
            addSection("Monitor");
            addNavItem("📊", "Dashboard",        "dashboard");
            addNavItem("📍", "Slot Map",         "slotmap");
            addSection("Operations");
            addNavItem("🔄", "Entry / Exit",     "entryexit");
            addNavItem("🚗", "Vehicles",         "vehicles");
            addNavItem("📝", "Register Vehicle", "register");
            addSection("Admin Only");
            addNavItem("💰", "Fees",             "fees");
            addNavItem("📋", "Reports",          "reports");
            addNavItem("🗂", "Audit Log",        "auditlog");
        } else {
            addSection("My Account");
            addNavItem("👤", "My Status",        "mystatus");
            addNavItem("📍", "Slot View",        "slotview");
            addSection("Records");
            addNavItem("🔄", "History",          "history");
            addNavItem("💰", "Fee Schedule",     "feeschedule");
        }

        add(Box.createVerticalGlue());
    }

    private void addSection(String title) {
        JLabel lbl = new JLabel(title.toUpperCase());
        lbl.setForeground(new Color(0x6B5E9A));
        lbl.setFont(SPARCSTheme.labelFont(10));
        lbl.setBorder(new EmptyBorder(14, 16, 4, 0));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        add(lbl);
    }

    private void addNavItem(String icon, String label, String page) {
        NavItem ni = new NavItem(icon, label, page);
        items.add(ni);
        add(ni);
    }

    // ---- Inner row ----
    private class NavItem extends JPanel {
        private final String page;
        private boolean hovered = false;

        NavItem(String icon, String label, String page) {
            this.page = page;
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setLayout(new FlowLayout(FlowLayout.LEFT, 12, 6));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel iconLbl  = new JLabel(icon);
            JLabel labelLbl = new JLabel(label);
            iconLbl.setForeground(SPARCSTheme.TEXT_WHITE);
            labelLbl.setForeground(SPARCSTheme.TEXT_WHITE);
            iconLbl.setFont(SPARCSTheme.labelFont(14));
            labelLbl.setFont(SPARCSTheme.labelFont(14));
            add(iconLbl);
            add(labelLbl);

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
                public void mouseClicked(MouseEvent e) {
                    setActivePage(page);
                    if (listener != null) listener.onNavigate(page);
                }
            });
        }

        void updateStyle() { repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean active = activePage.equals(page);
            if (active) {
                g2.setColor(SPARCSTheme.SIDEBAR_ACTIVE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(SPARCSTheme.ACCENT_PINK);
                g2.fillRect(0, 0, 3, getHeight());
            } else if (hovered) {
                g2.setColor(new Color(0x1E1550));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
