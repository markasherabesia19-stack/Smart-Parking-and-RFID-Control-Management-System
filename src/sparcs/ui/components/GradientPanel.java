package sparcs.ui.components;

import javax.swing.*;
import java.awt.*;

public class GradientPanel extends JPanel {

    private Color left;
    private Color right;

    public GradientPanel(Color left, Color right) {
        this.left  = left;
        this.right = right;
        setOpaque(false);
    }

    public GradientPanel() {
        this(SPARCSTheme.BG_LEFT, SPARCSTheme.BG_RIGHT);
    }

    public void setColors(Color left, Color right) {
        this.left  = left;
        this.right = right;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gp = new GradientPaint(0, 0, left, getWidth(), getHeight(), right);
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
