package sparcs.ui.components;

import javax.swing.*;
import java.awt.*;

public class CardPanel extends JPanel {

    private final Color bg;
    private Color accentColor;   // top-border accent, null = none
    private final int arc;

    public CardPanel(Color bg, int arc) {
        this.bg  = bg;
        this.arc = arc;
        setOpaque(false);
        setLayout(new BorderLayout());
    }

    public CardPanel() {
        this(SPARCSTheme.CARD_BG, 20);
    }

    public CardPanel(Color accentBorder) {
        this(SPARCSTheme.CARD_BG, 20);
        this.accentColor = accentBorder;
    }

    public void setAccentColor(Color c) {
        this.accentColor = c;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill card
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

        // Accent border on top
        if (accentColor != null) {
            g2.setColor(accentColor);
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(arc / 2, 0, getWidth() - arc / 2, 0);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
