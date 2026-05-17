package ui.shared;

import model.AppState;
import model.AuditLog;
import dao.AuditLogDAO;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class SidebarPanel {

    private static final String[][] ADMIN_ITEMS = {
        {"Monitor",               ""},
        {"  Dashboard",           "ADMIN_DASHBOARD"},
        {"  Slot Map",            "ADMIN_SLOT_MAP"},
        {"Operations",            ""},
        {"  Vehicles",            "ADMIN_VEHICLES"},
        {"  Register Vehicle",    "ADMIN_REGISTER"},
        {"Admin Only",            ""},
        {"  Fees",                "ADMIN_FEES"},
        {"  Reports",             "ADMIN_REPORTS"},
        {"  Audit Log",           "ADMIN_AUDIT_LOG"},
        {"  Manage Accounts",     "ADMIN_MANAGE_ACCOUNTS"},
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

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color C_ACTIVE_BG   = new Color(105, 48, 195);
    private static final Color C_HOVER_BG    = new Color(105, 48, 195, 55);
    private static final Color C_ACTIVE_BAR  = new Color(179, 136, 255);
    private static final Color C_SECTION     = new Color(180, 160, 230, 110);
    private static final Color C_ITEM        = new Color(200, 190, 235, 210);
    private static final Color C_ACTIVE_ITEM = Color.WHITE;
    private static final Color C_SO_BG       = new Color(200, 50, 80, 28);
    private static final Color C_SO_HOVER    = new Color(200, 50, 80, 60);
    private static final Color C_SO_BORDER   = new Color(200, 50, 80, 55);
    private static final Color C_SO_FG       = new Color(240, 120, 140);

    private static final String FONT = "Inter";

    // ── build() ───────────────────────────────────────────────────────────────
    public static JPanel build(CardLayout cardLayout, JPanel rootPanel,
                               AppState state, String role, String activeScreen) {

        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(20, 10, 50, 255));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillRect(getWidth() - 1, 0, 1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(190, 0));
        sidebar.setOpaque(false);

        sidebar.add(buildLogoZone());

        String[][] items = role.equals("ADMIN") ? ADMIN_ITEMS : USER_ITEMS;
        for (String[] item : items) {
            boolean isHeader    = !item[0].startsWith("  ");
            boolean isActive    = item[1].equals(activeScreen);
            boolean isClickable = !item[1].isEmpty();
            if (isHeader) {
                sidebar.add(buildSectionLabel(item[0]));
            } else {
                sidebar.add(buildNavRow(item, isActive, isClickable, cardLayout, rootPanel));
            }
        }

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(buildSignOutZone(state, cardLayout, rootPanel));
        return sidebar;
    }

    // ── Logo zone ─────────────────────────────────────────────────────────────
    private static JPanel buildLogoZone() {
        JPanel zone = new JPanel();
        zone.setLayout(new BoxLayout(zone, BoxLayout.Y_AXIS));
        zone.setOpaque(false);
        zone.setBorder(new EmptyBorder(16, 0, 16, 0));
        zone.setMaximumSize(new Dimension(190, 130));

        // Load logo.png using ImageIO — fully loaded, no async issues
        java.awt.image.BufferedImage logoToDraw = null;
        try {
            java.net.URL logoUrl = SidebarPanel.class.getClassLoader().getResource("assets/logo.png");
            java.awt.image.BufferedImage src = null;
            if (logoUrl != null) {
                src = javax.imageio.ImageIO.read(logoUrl);
            } else {
                java.io.File logoFile = new java.io.File("assets/logo.png");
                if (logoFile.exists()) {
                    src = javax.imageio.ImageIO.read(logoFile);
                }
            }
            if (src != null) {
                int origW = src.getWidth(), origH = src.getHeight();
                int maxW = 160, maxH = 100;
                double scale = Math.min((double) maxW / origW, (double) maxH / origH);
                int scaledW = Math.max(1, (int) (origW * scale));
                int scaledH = Math.max(1, (int) (origH * scale));
                java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(scaledW, scaledH, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D sg = scaled.createGraphics();
                sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                sg.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
                sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
                sg.drawImage(src, 0, 0, scaledW, scaledH, null);
                sg.dispose();
                logoToDraw = scaled;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        final java.awt.image.BufferedImage finalLogo = logoToDraw;

        JPanel badge = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                int cx = getWidth() / 2, cy = getHeight() / 2;

                if (finalLogo != null) {
                    int imgW = finalLogo.getWidth();
                    int imgH = finalLogo.getHeight();
                    g2.drawImage(finalLogo, cx - imgW / 2, cy - imgH / 2, imgW, imgH, null);
                } else {
                    // Fallback: purple badge with "S"
                    int s = 52;
                    g2.setColor(new Color(140, 90, 255, 50));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(cx-s/2-6, cy-s/2-6, s+12, s+12, 22, 22);
                    g2.setPaint(new GradientPaint(
                        cx-s/2, cy-s/2, new Color(120, 60, 210),
                        cx+s/2, cy+s/2, new Color(88, 32, 170)));
                    g2.fillRoundRect(cx-s/2, cy-s/2, s, s, 16, 16);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Georgia", Font.BOLD, 24));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("S", cx - fm.stringWidth("S") / 2,
                        cy + (fm.getAscent() - fm.getDescent()) / 2);
                }
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(190, 110));
        badge.setMaximumSize(new Dimension(190, 110));
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);

        zone.add(badge);
        return zone;
    }

    // ── Section label ─────────────────────────────────────────────────────────
    private static JPanel buildSectionLabel(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(190, 30));
        row.setBorder(new EmptyBorder(10, 0, 2, 0));
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setFont(new Font(FONT, Font.BOLD, 9));
        lbl.setForeground(C_SECTION);
        row.add(lbl);
        return row;
    }

    // ── Nav row ───────────────────────────────────────────────────────────────
    private static JPanel buildNavRow(String[] item, boolean isActive, boolean isClickable,
                                      CardLayout cardLayout, JPanel rootPanel) {
        String label = item[0].trim();

        JPanel row = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isActive) {
                    g2.setColor(C_ACTIVE_BG);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(C_ACTIVE_BAR);
                    g2.fillRoundRect(0, 6, 3, getHeight() - 12, 3, 3);
                } else if (Boolean.TRUE.equals(getClientProperty("hovered"))) {
                    g2.setColor(C_HOVER_BG);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                super.paintComponent(g);
                g2.dispose();
            }
        };
        row.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(190, 36));
        row.setPreferredSize(new Dimension(190, 36));

        JLabel iconLbl = new JLabel(makeNavIcon(item[1], isActive));
        iconLbl.setPreferredSize(new Dimension(16, 36));
        row.add(iconLbl);

        JLabel textLbl = new JLabel(label);
        textLbl.setFont(new Font(FONT, Font.PLAIN, 13));
        textLbl.setForeground(isActive ? C_ACTIVE_ITEM : C_ITEM);
        row.add(textLbl);

        if (isClickable) {
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            String target = item[1];
            row.addMouseListener(new MouseAdapter() {
                // FIX: Use mousePressed instead of mouseClicked.
                // mouseClicked fires only after both press AND release complete,
                // which causes a noticeable lag. mousePressed fires instantly on
                // the down-stroke, giving immediate response with no glitch.
                @Override public void mousePressed(MouseEvent e) {
                    if (!isActive) {
                        cardLayout.show(rootPanel, target);
                        rootPanel.revalidate();
                        rootPanel.repaint();
                    }
                }
                @Override public void mouseEntered(MouseEvent e) {
                    if (!isActive) { row.putClientProperty("hovered", true);  row.repaint(); }
                }
                @Override public void mouseExited(MouseEvent e) {
                    if (!isActive) { row.putClientProperty("hovered", false); row.repaint(); }
                }
            });
        }
        return row;
    }

    // ── Sign-out zone ─────────────────────────────────────────────────────────
    private static JPanel buildSignOutZone(AppState state, CardLayout cardLayout, JPanel rootPanel) {
        JPanel zone = new JPanel();
        zone.setLayout(new BoxLayout(zone, BoxLayout.Y_AXIS));
        zone.setOpaque(false);
        zone.setMaximumSize(new Dimension(190, 62));

        // separator line
        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(255, 255, 255, 18));
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        sep.setOpaque(false);
        sep.setPreferredSize(new Dimension(190, 1));
        sep.setMaximumSize(new Dimension(190, 1));
        zone.add(sep);
        zone.add(Box.createVerticalStrut(10));

        JPanel btn = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hov = Boolean.TRUE.equals(getClientProperty("hovered"));
                g2.setColor(hov ? C_SO_HOVER : C_SO_BG);
                g2.fill(new RoundRectangle2D.Float(8, 2, getWidth()-16, getHeight()-4, 10, 10));
                g2.setColor(C_SO_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(8, 2, getWidth()-16, getHeight()-4, 10, 10));
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(190, 38));
        btn.setMaximumSize(new Dimension(190, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLbl = new JLabel(makeSignOutIcon());
        iconLbl.setPreferredSize(new Dimension(16, 38));
        btn.add(iconLbl);

        JLabel lbl = new JLabel("Sign Out");
        lbl.setFont(new Font(FONT, Font.PLAIN, 13));
        lbl.setForeground(C_SO_FG);
        btn.add(lbl);

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.putClientProperty("hovered", true);  btn.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { btn.putClientProperty("hovered", false); btn.repaint(); }
            // FIX: Use mousePressed here too for instant response on Sign Out
            @Override public void mousePressed(MouseEvent e) {
                try {
                    if (state.getCurrentUserAccount() != null) {
                        AuditLog log = new AuditLog();
                        log.setUserId(state.getCurrentUserAccount().getUserId());
                        log.setAction("LOGOUT");
                        log.setEntityType("USER");
                        log.setEntityId(state.getCurrentUserAccount().getUserId());
                        log.setNewValue(state.currentUsername);
                        new AuditLogDAO().create(log);
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
                state.clearSession();
                cardLayout.show(rootPanel, "UNIFIED_LOGIN");
            }
        });

        zone.add(btn);
        zone.add(Box.createVerticalStrut(12));
        return zone;
    }

    // ── Icon factory ──────────────────────────────────────────────────────────
    private static ImageIcon makeNavIcon(String target, boolean active) {
        final int R = 32;
        BufferedImage img = new BufferedImage(R, R, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Color ic = active ? new Color(255, 255, 255, 235) : new Color(200, 190, 235, 185);
        g.setColor(ic);
        BasicStroke s = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g.setStroke(s);

        switch (target) {
            case "ADMIN_DASHBOARD": case "USER_DASHBOARD":
                g.fillRoundRect(2,  2, 12, 12, 4, 4);
                g.fillRoundRect(18, 2, 12, 12, 4, 4);
                g.fillRoundRect(2, 18, 12, 12, 4, 4);
                g.fillRoundRect(18,18, 12, 12, 4, 4);
                break;

            case "ADMIN_SLOT_MAP": case "USER_SLOT_VIEW":
                g.drawRoundRect(2, 2, 28, 20, 4, 4);
                g.drawLine(12, 22, 20, 22);
                g.drawLine(16, 22, 16, 29);
                g.drawLine(10, 29, 22, 29);
                break;

            case "ADMIN_ENTRY_EXIT":
                g.drawLine(6, 4, 6, 28);
                g.drawLine(6, 4, 14, 4);
                g.drawLine(6, 28, 14, 28);
                g.drawLine(14, 16, 28, 16);
                g.drawLine(22, 10, 28, 16);
                g.drawLine(22, 22, 28, 16);
                break;

            case "ADMIN_VEHICLES":
                g.drawRoundRect(5, 14, 22, 16, 4, 4);
                g.drawArc(9, 6, 14, 14, 0, 180);
                g.drawLine(9, 13, 9, 17);
                g.drawLine(23, 13, 23, 17);
                g.fillOval(14, 19, 4, 4);
                break;

            case "ADMIN_REGISTER":
                g.drawOval(3, 3, 26, 26);
                g.drawLine(16, 8, 16, 16);
                g.drawLine(16, 16, 22, 20);
                break;

            case "ADMIN_FEES":
                g.drawLine(3, 27, 10, 17);
                g.drawLine(10, 17, 17, 22);
                g.drawLine(17, 22, 29, 7);
                break;

            case "ADMIN_REPORTS":
                g.drawRoundRect(5, 2, 22, 28, 4, 4);
                g.drawLine(9, 10, 23, 10);
                g.drawLine(9, 16, 23, 16);
                g.drawLine(9, 22, 18, 22);
                break;

            case "ADMIN_AUDIT_LOG":
                g.drawRoundRect(5, 4, 22, 24, 4, 4);
                g.drawRoundRect(11, 1, 10, 6, 3, 3);
                g.drawLine(9, 13, 23, 13);
                g.drawLine(9, 19, 23, 19);
                g.drawLine(9, 25, 17, 25);
                break;

            case "ADMIN_MANAGE_ACCOUNTS":
                // Person head (circle)
                g.drawOval(8, 2, 10, 10);
                // Person body / shoulders arc
                g.drawArc(2, 16, 20, 18, 0, 180);
                // Gear / cog — small, bottom-right quadrant (admin/settings badge)
                // Outer circle
                g.drawOval(18, 18, 11, 11);
                // Gear teeth — 4 short lines at N/S/E/W outside the circle
                g.drawLine(23, 15, 23, 18);   // top tooth
                g.drawLine(23, 29, 23, 32);   // bottom tooth (clipped to canvas edge)
                g.drawLine(15, 23, 18, 23);   // left tooth
                g.drawLine(29, 23, 32, 23);   // right tooth (clipped)
                // Inner dot of gear
                g.fillOval(21, 21, 5, 5);
                break;

            case "USER_MY_STATUS":
                g.drawOval(11, 2, 10, 10);
                g.drawArc(3, 16, 26, 18, 0, 180);
                break;

            case "USER_HISTORY":
                g.drawOval(3, 3, 26, 26);
                g.drawLine(16, 8, 16, 16);
                g.drawLine(16, 16, 21, 20);
                break;

            case "USER_FEE_SCHEDULE":
                g.drawRoundRect(3, 5, 26, 24, 4, 4);
                g.drawLine(3, 13, 29, 13);
                g.drawLine(10, 2, 10, 9);
                g.drawLine(22, 2, 22, 9);
                g.fillOval(9, 17, 4, 4);
                g.fillOval(15, 17, 4, 4);
                g.fillOval(21, 17, 4, 4);
                break;

            case "USER_RFID_CARD":
                g.drawRoundRect(2, 8, 28, 18, 4, 4);
                g.drawLine(2, 14, 30, 14);
                g.drawRoundRect(5, 17, 8, 6, 2, 2);
                break;

            default:
                g.fillOval(12, 12, 8, 8);
        }

        g.dispose();
        return new ImageIcon(img.getScaledInstance(16, 16, Image.SCALE_SMOOTH));
    }

    private static ImageIcon makeSignOutIcon() {
        final int R = 32;
        BufferedImage img = new BufferedImage(R, R, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(C_SO_FG);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(12, 4,  5,  4);
        g.drawLine(5,  4,  5,  28);
        g.drawLine(5,  28, 12, 28);
        g.drawLine(13, 16, 28, 16);
        g.drawLine(21, 9,  28, 16);
        g.drawLine(21, 23, 28, 16);
        g.dispose();
        return new ImageIcon(img.getScaledInstance(16, 16, Image.SCALE_SMOOTH));
    }
}