package sparcs.ui;

import sparcs.ui.components.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

/**
 * First screen: animated GIF background + SPARCS logo + "SIGN IN AS" role cards.
 *
 * The GIF is loaded from: /sparcs/assets/splash_bg.gif  (classpath)
 * The logo PNG is loaded by SPARCSLogo from: /sparcs/assets/sparcs_logo.png
 */
public class SplashScreen extends JFrame {

    public SplashScreen() {
        setTitle("SPARCS");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
    }

    private JPanel buildContent() {
        // ── Animated GIF background panel ─────────────────────────────────────
        JPanel root = new JPanel(new GridBagLayout()) {
            private ImageIcon gifIcon;
            {
                URL gifUrl = SplashScreen.class.getResource("/sparcs/assets/splash_bg.gif");
                if (gifUrl != null) {
                    gifIcon = new ImageIcon(gifUrl);  // ImageIcon animates GIFs automatically
                } else {
                    System.err.println("splash_bg.gif not found on classpath.");
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (gifIcon != null) {
                    // Scale GIF to fill the panel
                    g.drawImage(gifIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
                } else {
                    // Fallback gradient
                    Graphics2D g2 = (Graphics2D) g;
                    GradientPaint gp = new GradientPaint(
                            0, 0, SPARCSTheme.BG_LEFT,
                            getWidth(), getHeight(), SPARCSTheme.BG_RIGHT);
                    g2.setPaint(gp);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        root.setOpaque(true);

        // ── Center content ─────────────────────────────────────────────────────
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        // Logo  (PNG, transparent)
        SPARCSLogo logo = new SPARCSLogo(100, true);
        logo.setAlignmentX(CENTER_ALIGNMENT);

        // Subtitle
        JLabel sub = new JLabel("SIGN IN AS");
        sub.setFont(SPARCSTheme.labelFont(15));
        sub.setForeground(SPARCSTheme.TEXT_WHITE);
        sub.setAlignmentX(CENTER_ALIGNMENT);

        // Role cards
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
        row.setOpaque(false);
        row.add(buildRoleCard("⚙", "ADMIN", this::openAdminLogin));
        row.add(buildRoleCard("👤", "USER",  this::openUserLogin));
        row.setAlignmentX(CENTER_ALIGNMENT);

        center.add(logo);
        center.add(Box.createVerticalStrut(22));
        center.add(sub);
        center.add(Box.createVerticalStrut(24));
        center.add(row);

        root.add(center);
        return root;
    }

    private JPanel buildRoleCard(String icon, String label, Runnable action) {
        // Semi-transparent dark card
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(13, 10, 46, 210));   // dark navy, semi-transparent
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(180, 180));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(30, 20, 20, 20));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLbl = new JLabel(icon, SwingConstants.CENTER);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 48));
        iconLbl.setForeground(SPARCSTheme.ACCENT_PURPLE);
        iconLbl.setAlignmentX(CENTER_ALIGNMENT);

        JLabel nameLbl = new JLabel(label, SwingConstants.CENTER);
        nameLbl.setFont(SPARCSTheme.boldFont(16));
        nameLbl.setForeground(SPARCSTheme.TEXT_WHITE);
        nameLbl.setAlignmentX(CENTER_ALIGNMENT);

        card.add(iconLbl);
        card.add(Box.createVerticalStrut(12));
        card.add(nameLbl);

        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { action.run(); }
        });

        return card;
    }

    private void openAdminLogin() {
        new LoginScreen(true).setVisible(true);
        dispose();
    }

    private void openUserLogin() {
        new LoginScreen(false).setVisible(true);
        dispose();
    }
}
