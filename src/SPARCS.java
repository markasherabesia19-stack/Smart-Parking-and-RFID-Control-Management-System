import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public class SPARCS extends JFrame {

    //Color Palette
    static final Color C_BG_DARK   = new Color(10, 8, 30);
    static final Color C_BG_PANEL  = new Color(18, 14, 48);
    static final Color C_BG_CARD   = new Color(25, 20, 65);
    static final Color C_SIDEBAR   = new Color(14, 11, 38);
    static final Color C_PURPLE    = new Color(105, 48, 195);
    static final Color C_PINK      = new Color(210, 50, 140);
    static final Color C_ACCENT    = new Color(160, 70, 230);
    static final Color C_WHITE     = new Color(240, 235, 255);
    static final Color C_MUTED     = new Color(140, 130, 180);
    static final Color C_AVAILABLE = new Color(60, 210, 130);
    static final Color C_OCCUPIED  = new Color(220, 70, 90);
    static final Color C_RESERVED  = new Color(255, 165, 50);
    static final Color C_INPUT_BG  = new Color(30, 24, 75);
    static final Color C_INPUT_BD  = new Color(80, 60, 140);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     rootPanel  = new JPanel(cardLayout);

    // Shared state mock
    private String currentRole     = "";
    private String currentUsername = "";
    private final int TOTAL_SLOTS   = 40;
    private int availableSlots      = 20;
    private int occupiedSlots       = 15;
    private int reservedSlots       = 5;

    // Slot Data
    private final int[] slotData = new int[TOTAL_SLOTS];

    public SPARCS() {
        super("SPARCS — Smart Parking & RFID Control Management System");
        initSlotData();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG_DARK);

        // build all screens
        rootPanel.add(buildSplashScreen(),     "SPLASH");
        rootPanel.add(buildRolePicker(),       "ROLE_PICKER");
        rootPanel.add(buildAdminLogin(),       "ADMIN_LOGIN");
        rootPanel.add(buildUserLogin(),        "USER_LOGIN");
        rootPanel.add(buildUserRegister(),     "USER_REGISTER");
        rootPanel.add(buildAdminDashboard(),   "ADMIN_DASHBOARD");
        rootPanel.add(buildAdminSlotMap(),     "ADMIN_SLOT_MAP");
        rootPanel.add(buildUserDashboard(),    "USER_DASHBOARD");
        rootPanel.add(buildUserSlotView(),     "USER_SLOT_VIEW");

        add(rootPanel);
        cardLayout.show(rootPanel, "SPLASH");
        setVisible(true);

        // auto-advance splash after 2.5s
        Timer t = new Timer(2500, e -> cardLayout.show(rootPanel, "ROLE_PICKER"));
        t.setRepeats(false);
        t.start();
    }

    private void initSlotData() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (i < occupiedSlots)       slotData[i] = 1;
            else if (i < occupiedSlots + reservedSlots) slotData[i] = 2;
            else                         slotData[i] = 0;
        }
    }

    /** Panel with a purple→pink diagonal gradient background */
    private JPanel gradientPanel(LayoutManager lm) {
        return new JPanel(lm) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                    0, 0, C_BG_DARK,
                    getWidth(), getHeight(), new Color(55, 15, 90));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
    }

    /** Dark card panel with rounded corners */
    private JPanel cardPanel(LayoutManager lm) {
        return new JPanel(lm) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BG_CARD);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 18, 18);
                g2.setColor(C_INPUT_BD);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 18, 18);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
    }

    /** Gradient button */
    private JButton gradientButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = getModel().isRollover()
                    ? new GradientPaint(0,0,C_PINK,getWidth(),0,C_PURPLE)
                    : new GradientPaint(0,0,C_PURPLE,getWidth(),0,C_PINK);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(C_WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(C_WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(220, 40));
        return btn;
    }

    /** Outline / ghost button */
    private JButton outlineButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(105, 48, 195, 60));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                g2.setColor(C_ACCENT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 10, 10);
                g2.setColor(C_WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setForeground(C_WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(220, 38));
        return btn;
    }

    /** Styled text field */
    private JTextField styledField(String placeholder) {
        JTextField tf = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_INPUT_BG);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.setColor(C_INPUT_BD);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        tf.setOpaque(false);
        tf.setForeground(C_WHITE);
        tf.setCaretColor(C_ACCENT);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setBorder(new EmptyBorder(6, 12, 6, 12));
        tf.setPreferredSize(new Dimension(260, 38));
        tf.putClientProperty("placeholder", placeholder);
        return tf;
    }

    /** Styled password field */
    private JPasswordField styledPasswordField(String placeholder) {
        JPasswordField pf = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(C_INPUT_BG);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.setColor(C_INPUT_BD);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        pf.setOpaque(false);
        pf.setForeground(C_WHITE);
        pf.setCaretColor(C_ACCENT);
        pf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        pf.setBorder(new EmptyBorder(6, 12, 6, 12));
        pf.setPreferredSize(new Dimension(260, 38));
        return pf;
    }

    /** Label helper */
    private JLabel lbl(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        return l;
    }

    /** SPARCS logo component */
    private JPanel logoPanel(int iconSize) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth()/2, cy = getHeight()/2;
                int r = iconSize/2;
                // gradient circle
                RadialGradientPaint rg = new RadialGradientPaint(cx, cy, r,
                    new float[]{0f, 1f}, new Color[]{C_PURPLE, C_PINK});
                g2.setPaint(rg);
                g2.fillOval(cx-r, cy-r, iconSize, iconSize);
                // S letter
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, (int)(iconSize*0.55)));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("S", cx - fm.stringWidth("S")/2, cy + fm.getAscent()/2 - 2);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() {
                return new Dimension(iconSize + 20, iconSize + 20);
            }
        };
        p.setOpaque(false);
        return p;
    }

    /** Sidebar for admin/user */
    private JPanel buildSidebar(String role, String activeScreen) {
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(C_SIDEBAR);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(60, 45, 100));
                g2.fillRect(getWidth()-1, 0, 1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(185, 0));
        sidebar.setOpaque(false);

        // logo
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        logoRow.setOpaque(false);
        logoRow.setBorder(new EmptyBorder(18, 0, 4, 0));
        logoRow.add(logoPanel(36));
        sidebar.add(logoRow);
        JLabel brand = lbl("SPARCS", Font.BOLD, 15, C_WHITE);
        brand.setAlignmentX(CENTER_ALIGNMENT);
        brand.setBorder(new EmptyBorder(0, 0, 20, 0));
        sidebar.add(brand);

        String[][] adminItems = {
            {"Monitor",""},
            {"  Dashboard","ADMIN_DASHBOARD"},
            {"  Slot Map","ADMIN_SLOT_MAP"},
            {"Operations",""},
            {"  Entry/Exit",""},
            {"  Vehicles",""},
            {"  Register Vehicle",""},
            {"Admin Only",""},
            {"  Fees",""},
            {"  Reports",""},
            {"  Audit Log",""}
        };
        String[][] userItems = {
            {"My Account",""},
            {"  Dashboard","USER_DASHBOARD"},
            {"  My Status",""},
            {"  Slot View","USER_SLOT_VIEW"},
            {"Records",""},
            {"  History",""},
            {"  Fee Schedule",""}
        };

        String[][] items = role.equals("ADMIN") ? adminItems : userItems;
        for (String[] item : items) {
            boolean isHeader = !item[0].startsWith("  ");
            boolean isActive = item[1].equals(activeScreen);
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 5));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(185, 32));

            if (isActive) {
                row.setOpaque(true);
                row.setBackground(new Color(105, 48, 195, 80));
            }

            JLabel itemLbl = lbl(item[0],
                isHeader ? Font.BOLD : Font.PLAIN,
                isHeader ? 10 : 13,
                isHeader ? C_MUTED : (isActive ? C_WHITE : new Color(200, 190, 230)));
            row.add(itemLbl);

            if (!item[1].isEmpty()) {
                row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                String target = item[1];
                row.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        cardLayout.show(rootPanel, target);
                    }
                    @Override public void mouseEntered(MouseEvent e) {
                        if (!isActive) row.setBackground(new Color(80, 50, 150, 60));
                        row.setOpaque(true); row.repaint();
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        if (!isActive) { row.setOpaque(false); row.repaint(); }
                    }
                });
            }
            sidebar.add(row);
        }

        sidebar.add(Box.createVerticalGlue());

        // sign out
        JButton signOut = new JButton("  Sign Out") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                if (getModel().isRollover()) {
                    g2.setColor(new Color(210, 50, 90, 60));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.setColor(new Color(220, 80, 100));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), 14, (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        signOut.setFont(new Font("SansSerif", Font.PLAIN, 13));
        signOut.setBorderPainted(false); signOut.setContentAreaFilled(false);
        signOut.setFocusPainted(false);
        signOut.setMaximumSize(new Dimension(185, 40));
        signOut.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signOut.addActionListener(e -> {
            currentRole = ""; currentUsername = "";
            cardLayout.show(rootPanel, "ROLE_PICKER");
        });
        sidebar.add(signOut);
        sidebar.add(Box.createVerticalStrut(10));
        return sidebar;
    }

    /** Stat card */
    private JPanel statCard(String label, String value, Color valueColor) {
        JPanel card = cardPanel(new BorderLayout(0, 4));
        card.setBorder(new EmptyBorder(14, 18, 14, 18));
        JLabel valLbl = lbl(value, Font.BOLD, 26, valueColor);
        JLabel lblLbl = lbl(label.toUpperCase(), Font.PLAIN, 10, C_MUTED);
        card.add(valLbl, BorderLayout.CENTER);
        card.add(lblLbl, BorderLayout.SOUTH);
        return card;
    }

    //Screen components

    // Splash Intro Interface
    private JPanel buildSplashScreen() {
        JPanel p = gradientPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;

        p.add(logoPanel(90), gc);
        gc.gridy = 1;
        JLabel title = lbl("SPARCS", Font.BOLD, 32, C_WHITE);
        p.add(title, gc);
        gc.gridy = 2;
        gc.insets = new Insets(4, 0, 30, 0);
        p.add(lbl("Smart Parking & RFID Control Management System", Font.PLAIN, 13, C_MUTED), gc);

        // loading bar
        gc.gridy = 3; gc.insets = new Insets(0,0,0,0);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setPreferredSize(new Dimension(280, 6));
        bar.setBorderPainted(false);
        bar.setBackground(new Color(50, 35, 90));
        bar.setForeground(C_ACCENT);
        bar.setOpaque(true);
        p.add(bar, gc);

        Timer t = new Timer(25, null);
        t.addActionListener(e -> {
            bar.setValue(bar.getValue() + 1);
            if (bar.getValue() >= 100) t.stop();
        });
        t.start();
        return p;
    }

    // Role Picker Interface
    private JPanel buildRolePicker() {
        JPanel p = gradientPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;

        p.add(logoPanel(64), gc);
        gc.gridy = 1; gc.insets = new Insets(2,0,6,0);
        p.add(lbl("SPARCS", Font.BOLD, 28, C_WHITE), gc);
        gc.gridy = 2; gc.insets = new Insets(0,0,24,0);
        p.add(lbl("SIGN IN AS", Font.PLAIN, 12, C_MUTED), gc);

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 14, 0));
        btnRow.setOpaque(false);

        // Admin tile
        JPanel adminTile = roleTile("ADMIN", "⚙", "Full system access");
        adminTile.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                currentRole = "ADMIN";
                cardLayout.show(rootPanel, "ADMIN_LOGIN");
            }
        });

        // User tile
        JPanel userTile = roleTile("USER", "👤", "Vehicle owner portal");
        userTile.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                currentRole = "USER";
                cardLayout.show(rootPanel, "USER_LOGIN");
            }
        });

        btnRow.add(adminTile);
        btnRow.add(userTile);
        gc.gridy = 3; gc.insets = new Insets(0,0,0,0);
        p.add(btnRow, gc);
        return p;
    }

    private JPanel roleTile(String role, String icon, String subtitle) {
        JPanel tile = new JPanel(new GridBagLayout()) {
            boolean hover = false;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = hover ? C_PURPLE : C_BG_CARD;
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                GradientPaint gp = new GradientPaint(0,0, new Color(C_PURPLE.getRed(), C_PURPLE.getGreen(), C_PURPLE.getBlue(), hover?160:60),
                    getWidth(), getHeight(), new Color(C_PINK.getRed(), C_PINK.getGreen(), C_PINK.getBlue(), hover?120:40));
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
        tile.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                ((JPanel)tile).putClientProperty("hover", true); tile.repaint();
                // set hover field via reflection-free trick
            }
        });

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;
        tile.add(lbl(icon, Font.PLAIN, 28, C_WHITE), gc);
        gc.gridy = 1; gc.insets = new Insets(8,0,2,0);
        tile.add(lbl(role, Font.BOLD, 15, C_WHITE), gc);
        gc.gridy = 2; gc.insets = new Insets(0,0,0,0);
        tile.add(lbl(subtitle, Font.PLAIN, 10, C_MUTED), gc);
        return tile;
    }

    //Admin Login Interface
    private JPanel buildAdminLogin() {
        JPanel p = gradientPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;

        JPanel card = cardPanel(new GridBagLayout());
        card.setPreferredSize(new Dimension(340, 420));
        card.setBorder(new EmptyBorder(32, 32, 32, 32));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0; cc.gridy = 0; cc.fill = GridBagConstraints.HORIZONTAL;
        cc.insets = new Insets(4, 0, 4, 0);

        card.add(logoPanel(44), cc);
        cc.gridy++;
        JLabel title = lbl("SPARCS", Font.BOLD, 22, C_WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(title, cc);
        cc.gridy++; cc.insets = new Insets(0,0,18,0);
        JLabel sub = lbl("Admin Portal", Font.PLAIN, 12, C_MUTED);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(sub, cc);

        cc.insets = new Insets(4,0,2,0); cc.gridy++;
        card.add(lbl("USERNAME", Font.BOLD, 10, C_MUTED), cc);
        cc.gridy++;
        JTextField usernameField = styledField("Enter username");
        card.add(usernameField, cc);
        cc.gridy++;
        card.add(lbl("PASSWORD", Font.BOLD, 10, C_MUTED), cc);
        cc.gridy++;
        JPasswordField passwordField = styledPasswordField("Enter password");
        card.add(passwordField, cc);

        cc.gridy++; cc.insets = new Insets(18,0,8,0);
        JButton signInBtn = gradientButton("SIGN IN AS ADMIN");
        card.add(signInBtn, cc);

        cc.gridy++; cc.insets = new Insets(0,0,0,0);
        JLabel policyTitle = lbl("ADMIN ACCESS POLICY", Font.BOLD, 9, C_MUTED);
        card.add(policyTitle, cc);
        cc.gridy++;
        JLabel policyText = lbl("Admin accounts are created in the MySQL database.", Font.PLAIN, 9, new Color(100, 90, 140));
        card.add(policyText, cc);
        cc.gridy++;
        JLabel policyText2 = lbl("Self-registration is disabled for admins.", Font.PLAIN, 9, new Color(100, 90, 140));
        card.add(policyText2, cc);

        signInBtn.addActionListener(e -> {
            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword());
            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter both username and password.", "Login Error", JOptionPane.ERROR_MESSAGE);
            } else {
                // mock: accept any non-empty credentials
                currentUsername = user;
                cardLayout.show(rootPanel, "ADMIN_DASHBOARD");
            }
        });

        JButton backBtn = new JButton("← Back to Role Picker");
        backBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        backBtn.setForeground(C_MUTED);
        backBtn.setBorderPainted(false); backBtn.setContentAreaFilled(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "ROLE_PICKER"));
        cc.gridy++; cc.insets = new Insets(8,0,0,0);
        card.add(backBtn, cc);

        p.add(card, gc);
        return p;
    }

    //User Login Interface
    private JPanel buildUserLogin() {
        JPanel p = gradientPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;

        JPanel card = cardPanel(new GridBagLayout());
        card.setPreferredSize(new Dimension(340, 460));
        card.setBorder(new EmptyBorder(30, 32, 30, 32));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0; cc.gridy = 0; cc.fill = GridBagConstraints.HORIZONTAL;
        cc.insets = new Insets(4, 0, 4, 0);

        card.add(logoPanel(44), cc);
        cc.gridy++;
        JLabel title = lbl("SPARCS", Font.BOLD, 22, C_WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(title, cc);
        cc.gridy++; cc.insets = new Insets(0,0,18,0);
        JLabel sub = lbl("Vehicle Owner Portal", Font.PLAIN, 12, C_MUTED);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(sub, cc);

        cc.insets = new Insets(4,0,2,0); cc.gridy++;
        card.add(lbl("USERNAME", Font.BOLD, 10, C_MUTED), cc);
        cc.gridy++;
        JTextField usernameField = styledField("Enter username");
        card.add(usernameField, cc);
        cc.gridy++;
        card.add(lbl("PASSWORD", Font.BOLD, 10, C_MUTED), cc);
        cc.gridy++;
        JPasswordField passwordField = styledPasswordField("Enter password");
        card.add(passwordField, cc);

        cc.gridy++; cc.insets = new Insets(18,0,6,0);
        JButton signInBtn = gradientButton("SIGN IN");
        card.add(signInBtn, cc);
        cc.gridy++; cc.insets = new Insets(0,0,4,0);
        JButton registerBtn = outlineButton("CREATE NEW ACCOUNT");
        card.add(registerBtn, cc);
        cc.gridy++;
        JButton guestBtn = outlineButton("CONTINUE AS GUEST");
        card.add(guestBtn, cc);

        signInBtn.addActionListener(e -> {
            String user = usernameField.getText().trim();
            if (user.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a username.", "Login Error", JOptionPane.ERROR_MESSAGE);
            } else {
                currentUsername = user;
                cardLayout.show(rootPanel, "USER_DASHBOARD");
            }
        });
        registerBtn.addActionListener(e -> cardLayout.show(rootPanel, "USER_REGISTER"));
        guestBtn.addActionListener(e -> {
            currentUsername = "Guest";
            cardLayout.show(rootPanel, "USER_DASHBOARD");
        });

        JButton backBtn = new JButton("← Back to Role Picker");
        backBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        backBtn.setForeground(C_MUTED);
        backBtn.setBorderPainted(false); backBtn.setContentAreaFilled(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "ROLE_PICKER"));
        cc.gridy++; cc.insets = new Insets(8,0,0,0);
        card.add(backBtn, cc);

        p.add(card, gc);
        return p;
    }

    //User Registration Interface
    private JPanel buildUserRegister() {
        JPanel p = gradientPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;

        JPanel card = cardPanel(new GridBagLayout());
        card.setPreferredSize(new Dimension(360, 500));
        card.setBorder(new EmptyBorder(28, 32, 28, 32));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0; cc.gridy = 0; cc.fill = GridBagConstraints.HORIZONTAL;
        cc.insets = new Insets(4,0,2,0);

        JLabel title = lbl("Create Account", Font.BOLD, 20, C_WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(title, cc);
        cc.gridy++; cc.insets = new Insets(0,0,14,0);
        JLabel sub = lbl("Register as a vehicle owner", Font.PLAIN, 12, C_MUTED);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(sub, cc);

        String[] labels = {"FIRST NAME","LAST NAME","USERNAME","EMAIL","PASSWORD","CONFIRM PASSWORD"};
        JTextField[] fields = new JTextField[labels.length - 2];
        JPasswordField[] pfields = new JPasswordField[2];

        for (int i = 0; i < labels.length; i++) {
            cc.gridy++; cc.insets = new Insets(6,0,2,0);
            card.add(lbl(labels[i], Font.BOLD, 10, C_MUTED), cc);
            cc.gridy++; cc.insets = new Insets(0,0,0,0);
            if (i < labels.length - 2) {
                fields[i] = styledField("");
                card.add(fields[i], cc);
            } else {
                pfields[i - (labels.length - 2)] = styledPasswordField("");
                card.add(pfields[i - (labels.length - 2)], cc);
            }
        }

        cc.gridy++; cc.insets = new Insets(18,0,6,0);
        JButton regBtn = gradientButton("REGISTER");
        card.add(regBtn, cc);
        cc.gridy++; cc.insets = new Insets(0,0,0,0);
        JButton backBtn = outlineButton("BACK TO LOGIN");
        card.add(backBtn, cc);

        regBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Account created successfully! Please sign in.", "Success", JOptionPane.INFORMATION_MESSAGE);
            cardLayout.show(rootPanel, "USER_LOGIN");
        });
        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "USER_LOGIN"));

        JScrollPane scroll = new JScrollPane(card);
        scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        p.add(card, gc);
        return p;
    }

    //Admin Dashboard Interface
    private JPanel buildAdminDashboard() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(buildSidebar("ADMIN", "ADMIN_DASHBOARD"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBackground(C_BG_DARK);

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(lbl("DASHBOARD", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        JButton signOutTop = new JButton("SIGN OUT");
        styleSmallBtn(signOutTop);
        signOutTop.addActionListener(e -> { currentRole=""; cardLayout.show(rootPanel,"ROLE_PICKER"); });
        topBar.add(signOutTop, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // Stat row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));
        statsRow.add(statCard("Available Slots", String.valueOf(availableSlots), C_AVAILABLE));
        statsRow.add(statCard("Occupied", String.valueOf(occupiedSlots), C_OCCUPIED));
        statsRow.add(statCard("Revenue Today", "₱1,000", C_ACCENT));
        statsRow.add(statCard("Pending Fees", "3", C_RESERVED));

        // Body
        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 20, 20, 20));

        // Zone overview
        JPanel zonePanel = cardPanel(new BorderLayout(0, 10));
        zonePanel.setBorder(new EmptyBorder(16, 16, 16, 16));
        zonePanel.add(lbl("ZONE OVERVIEW", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);
        zonePanel.add(buildMiniSlotGrid(false), BorderLayout.CENTER);
        body.add(zonePanel);

        // Recent activity
        JPanel activityPanel = cardPanel(new BorderLayout(0, 10));
        activityPanel.setBorder(new EmptyBorder(16, 16, 16, 16));
        activityPanel.add(lbl("RECENT ACTIVITY", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);
        JPanel actList = new JPanel();
        actList.setLayout(new BoxLayout(actList, BoxLayout.Y_AXIS));
        actList.setOpaque(false);
        String[][] acts = {
            {"ABC-1234","Entry","B-04","Just now"},
            {"XYZ-5678","Exit","A-12","2 min ago"},
            {"LMN-9012","Entry","C-07","5 min ago"},
            {"QRS-3456","Exit","B-19","8 min ago"},
        };
        for (String[] a : acts) actList.add(activityRow(a[0], a[1], a[2], a[3]));
        activityPanel.add(new JScrollPane(actList) {{ setBorder(null); setOpaque(false); getViewport().setOpaque(false); }}, BorderLayout.CENTER);
        body.add(activityPanel);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false);
        mainPanel.add(statsRow, BorderLayout.NORTH);
        mainPanel.add(body, BorderLayout.CENTER);
        content.add(mainPanel, BorderLayout.CENTER);

        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private JPanel activityRow(String plate, String action, String slot, String time) {
        JPanel row = new JPanel(new GridLayout(1, 4));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(6, 0, 6, 0));
        row.add(lbl(plate, Font.BOLD, 12, C_WHITE));
        Color ac = action.equals("Entry") ? C_AVAILABLE : C_OCCUPIED;
        row.add(lbl(action, Font.PLAIN, 12, ac));
        row.add(lbl(slot, Font.PLAIN, 12, C_MUTED));
        row.add(lbl(time, Font.PLAIN, 11, C_MUTED));
        return row;
    }

    private void styleSmallBtn(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setForeground(C_WHITE);
        btn.setBackground(new Color(80, 50, 150));
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    //Admin Slot Map Interface
    private JPanel buildAdminSlotMap() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(buildSidebar("ADMIN", "ADMIN_SLOT_MAP"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(lbl("SLOT MAP", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));
        statsRow.add(statCard("Available", String.valueOf(availableSlots), C_AVAILABLE));
        statsRow.add(statCard("Occupied", String.valueOf(occupiedSlots), C_OCCUPIED));
        statsRow.add(statCard("Reserved", String.valueOf(reservedSlots), C_RESERVED));

        JPanel mapCard = cardPanel(new BorderLayout(0, 10));
        mapCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        mapCard.add(lbl("SLOTS", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);

        // legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        legend.setOpaque(false);
        legend.add(legendDot(C_AVAILABLE, "Available"));
        legend.add(legendDot(C_OCCUPIED, "Occupied"));
        legend.add(legendDot(C_RESERVED, "Reserved"));

        JPanel slotGrid = buildFullSlotGrid();
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(legend, BorderLayout.NORTH);
        south.add(slotGrid, BorderLayout.CENTER);
        mapCard.add(south, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 20, 20, 20));
        body.add(mapCard, BorderLayout.CENTER);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body, BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private JPanel legendDot(Color color, String label) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setOpaque(false);
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(color);
                g.fillOval(0, 2, 10, 10);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(12, 14); }
        };
        dot.setOpaque(false);
        p.add(dot);
        p.add(lbl(label, Font.PLAIN, 11, C_MUTED));
        return p;
    }

    private JPanel buildFullSlotGrid() {
        JPanel grid = new JPanel(new GridLayout(5, 8, 6, 6));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(10, 0, 0, 0));
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            int idx = i;
            Color slotColor = slotData[i] == 0 ? C_AVAILABLE : (slotData[i] == 1 ? C_OCCUPIED : C_RESERVED);
            JPanel slot = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(slotColor.getRed(), slotColor.getGreen(), slotColor.getBlue(), 40));
                    g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                    g2.setColor(slotColor);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                    g2.dispose();
                }
            };
            slot.setOpaque(false);
            char zone = (char)('A' + idx / 8);
            int num = (idx % 8) + 1;
            String slotName = zone + "-" + String.format("%02d", num);
            slot.setToolTipText(slotName + " — " + (slotData[i] == 0 ? "Available" : slotData[i] == 1 ? "Occupied" : "Reserved"));
            JLabel l = lbl(slotName, Font.PLAIN, 9, slotColor);
            slot.add(l);
            grid.add(slot);
        }
        return grid;
    }

    private JPanel buildMiniSlotGrid(boolean userView) {
        JPanel grid = new JPanel(new GridLayout(5, 8, 4, 4));
        grid.setOpaque(false);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            Color slotColor;
            if (userView) {
                slotColor = slotData[i] == 0 ? C_AVAILABLE : C_OCCUPIED;
            } else {
                slotColor = slotData[i] == 0 ? C_AVAILABLE : (slotData[i] == 1 ? C_OCCUPIED : C_RESERVED);
            }
            JPanel slot = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(slotColor.getRed(), slotColor.getGreen(), slotColor.getBlue(), 80));
                    g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,5,5);
                    g2.setColor(slotColor);
                    g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,5,5);
                    g2.dispose();
                }
                @Override public Dimension getPreferredSize() { return new Dimension(16, 14); }
            };
            slot.setOpaque(false);
            grid.add(slot);
        }
        return grid;
    }

    //User Dashboard Interface
    private JPanel buildUserDashboard() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(buildSidebar("USER", "USER_DASHBOARD"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        String greeting = "HELLO, " + (currentUsername.isEmpty() ? "User" : currentUsername.toUpperCase()) + "!";
        JLabel titleLbl = lbl(greeting, Font.BOLD, 20, C_WHITE);
        topBar.add(titleLbl, BorderLayout.WEST);
        JButton signOutTop = new JButton("SIGN OUT");
        styleSmallBtn(signOutTop);
        signOutTop.addActionListener(e -> { currentRole=""; cardLayout.show(rootPanel,"ROLE_PICKER"); });
        topBar.add(signOutTop, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));
        statsRow.add(statCard("Current Slot", "B-12", C_ACCENT));
        statsRow.add(statCard("Duration", "01:45", C_AVAILABLE));
        statsRow.add(statCard("Estimated Fee", "₱50", C_RESERVED));
        statsRow.add(statCard("Wallet Balance", "₱250", C_PINK));

        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 20, 20, 20));

        JPanel mapCard = cardPanel(new BorderLayout(0, 10));
        mapCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        mapCard.add(lbl("PARKING MAP", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);
        mapCard.add(buildMiniSlotGrid(true), BorderLayout.CENTER);
        body.add(mapCard);

        JPanel actCard = cardPanel(new BorderLayout(0, 10));
        actCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        actCard.add(lbl("RECENT ACTIVITY", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);
        JPanel actList = new JPanel();
        actList.setLayout(new BoxLayout(actList, BoxLayout.Y_AXIS));
        actList.setOpaque(false);
        String[][] acts = {
            {"Entry","B-12","Today 09:00 AM"},
            {"Exit","A-05","Yesterday 06:30 PM"},
            {"Entry","C-11","Apr 9"},
        };
        for (String[] a : acts) {
            JPanel row = new JPanel(new GridLayout(1, 3));
            row.setOpaque(false); row.setBorder(new EmptyBorder(6,0,6,0));
            Color ac = a[0].equals("Entry") ? C_AVAILABLE : C_OCCUPIED;
            row.add(lbl(a[0], Font.BOLD, 12, ac));
            row.add(lbl(a[1], Font.PLAIN, 12, C_WHITE));
            row.add(lbl(a[2], Font.PLAIN, 11, C_MUTED));
            actList.add(row);
        }
        actCard.add(actList, BorderLayout.CENTER);
        body.add(actCard);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body, BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    //User Slot View Interface
    private JPanel buildUserSlotView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(buildSidebar("USER", "USER_SLOT_VIEW"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(lbl("SLOT VIEW", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        JPanel statsRow = new JPanel(new GridLayout(1, 2, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 10, 20));
        statsRow.add(statCard("Available", String.valueOf(availableSlots), C_AVAILABLE));
        statsRow.add(statCard("Occupied", String.valueOf(occupiedSlots), C_OCCUPIED));

        JPanel mapCard = cardPanel(new BorderLayout(0, 10));
        mapCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        mapCard.add(lbl("PARKING ZONES", Font.BOLD, 12, C_MUTED), BorderLayout.NORTH);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        legend.setOpaque(false);
        legend.add(legendDot(C_AVAILABLE, "Available"));
        legend.add(legendDot(C_OCCUPIED, "Occupied"));
        south.add(legend, BorderLayout.NORTH);
        south.add(buildFullSlotGrid(), BorderLayout.CENTER);
        mapCard.add(south, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 20, 20, 20));
        body.add(mapCard, BorderLayout.CENTER);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body, BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    public static void main(String[] args) {
        // Enable antialiasing hints globally
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(SPARCS::new);
    }
}
