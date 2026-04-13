package sparcs.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SPARCSButton extends JButton {

    private Color base;
    private Color hover;
    private boolean hovered = false;

    public SPARCSButton(String text) {
        this(text, SPARCSTheme.BTN_PRIMARY, SPARCSTheme.BTN_HOVER);
    }

    public SPARCSButton(String text, Color base, Color hover) {
        super(text);
        this.base  = base;
        this.hover = hover;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setForeground(SPARCSTheme.TEXT_WHITE);
        setFont(SPARCSTheme.boldFont(14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(getPreferredSize().width, 44));

        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
            public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(hovered ? hover : base);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
        g2.dispose();
        super.paintComponent(g);
    }
}
