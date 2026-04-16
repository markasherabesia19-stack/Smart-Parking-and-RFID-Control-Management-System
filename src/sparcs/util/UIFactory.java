package util;
 
import javax.swing.*;
import java.awt.*;
import java.net.URL;
import static util.UIConstants.*;
import javax.swing.border.EmptyBorder; 
import javax.imageio.ImageIO;

import java.io.File;
import java.io.IOException;
 
public class UIFactory {
    private static Image bgImage = null;
 
    private static Image getBgImage() {
        if (bgImage == null) {
            URL url = UIFactory.class.getResource("/Users/marrianebalano/Smart-Parking-and-RFID-Control-Management-System/src/sparcs/ui/resources/gradientbg.png");
            if (url != null) {
                bgImage = new ImageIcon(url).getImage();
            } else {
                bgImage = new ImageIcon("/Users/marrianebalano/Smart-Parking-and-RFID-Control-Management-System/src/sparcs/ui/resources/gradientbg.png").getImage();
            }
        }
        return bgImage;
    }
 
    public static JPanel backgroundImagePanel(LayoutManager layout) {
        JPanel p = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image img = getBgImage();
                if (img != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    // Scale-to-fill: cover the whole panel
                    int pw = getWidth(), ph = getHeight();
                    int iw = img.getWidth(null), ih = img.getHeight(null);
                    if (iw > 0 && ih > 0) {
                        double scale = Math.max((double) pw / iw, (double) ph / ih);
                        int dw = (int) (iw * scale);
                        int dh = (int) (ih * scale);
                        int dx = (pw - dw) / 2;
                        int dy = (ph - dh) / 2;
                        g2.drawImage(img, dx, dy, dw, dh, null);
                    }
                    g2.dispose();
                }
            }
        };
        p.setOpaque(true);   
        return p;
    }

    public static JPanel gradientPanel(LayoutManager lm) {
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

    /** Dark card panel with rounded corners and subtle border. */
    public static JPanel cardPanel(LayoutManager lm) {
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

    // ── Button Factories ─────────────────────────────────────────────────────

    /** Purple-to-pink gradient primary button. */
    public static JButton gradientButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = getModel().isRollover()
                    ? new GradientPaint(0, 0, C_PINK, getWidth(), 0, C_PURPLE)
                    : new GradientPaint(0, 0, C_PURPLE, getWidth(), 0, C_PINK);
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

    /** Outline / ghost secondary button. */
    public static JButton outlineButton(String text) {
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

    /** Small utility button (e.g. Sign Out in top bar). */
    public static void styleSmallBtn(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setForeground(C_WHITE);
        btn.setBackground(new Color(80, 50, 150));
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // ── Input Field Factories ────────────────────────────────────────────────

    /** Styled rounded text field. */
    public static JTextField styledField(String placeholder) {
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

    /** Styled rounded password field. */
    public static JPasswordField styledPasswordField(String placeholder) {
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

    // ── Label / Misc Factories ───────────────────────────────────────────────

    /** Quick label helper with font style, size, and color. */
    public static JLabel lbl(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        return l;
    }

    /** SPARCS logo + title component. */
    public static JPanel logoPanel(int iconSize) {
        Image logoImg = null;
        try {
            File logoFile = new File("src/sparcs/ui/resources/logo.png");
            if (!logoFile.exists()) {
                // Fallback for running from compiled output
                logoFile = new File("../src/sparcs/ui/resources/logo.png");
            }
            logoImg = ImageIO.read(logoFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final Image finalImg = logoImg;

        // Image panel
        JPanel imgPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                if (finalImg != null) {
                    int panelW = getWidth();
                    int panelH = getHeight();
                    int imgW = finalImg.getWidth(null);
                    int imgH = finalImg.getHeight(null);
                    double scale = Math.min((double) panelW / imgW, (double) panelH / imgH);
                    int drawW = (int) (imgW * scale);
                    int drawH = (int) (imgH * scale);
                    int drawX = (panelW - drawW) / 2;
                    int drawY = (panelH - drawH) / 2;
                    g2.drawImage(finalImg, drawX, drawY, drawW, drawH, null);
                } else {
                    g2.setColor(Color.GRAY);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(Color.WHITE);
                    g2.drawString("Logo not found", 5, getHeight() / 2);
                }
                g2.dispose();
            }

            @Override public Dimension getPreferredSize() {
                return new Dimension(iconSize * 3, iconSize * 3);
            }
        };
        imgPanel.setOpaque(false);
        
        return imgPanel;
    }

    /** Stat card with a large colored value and a muted label. */
    public static JPanel statCard(String label, String value, Color valueColor) {
        JPanel card = cardPanel(new BorderLayout(0, 4));
        card.setBorder(new EmptyBorder(14, 18, 14, 18));
        JLabel valLbl = lbl(value, Font.BOLD, 26, valueColor);
        JLabel lblLbl = lbl(label.toUpperCase(), Font.PLAIN, 10, C_MUTED);
        card.add(valLbl, BorderLayout.CENTER);
        card.add(lblLbl, BorderLayout.SOUTH);
        return card;
    }

    /** Colored dot + label legend item. */
    public static JPanel legendDot(Color color, String label) {
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
}
