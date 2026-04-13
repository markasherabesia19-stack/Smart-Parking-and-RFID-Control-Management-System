package sparcs.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SPARCSPasswordField extends JPasswordField {

    public SPARCSPasswordField() {
        this(20);
    }

    public SPARCSPasswordField(int cols) {
        super(cols);
        setOpaque(true);
        setBackground(SPARCSTheme.FIELD_BG);
        setForeground(SPARCSTheme.TEXT_WHITE);
        setCaretColor(SPARCSTheme.TEXT_WHITE);
        setFont(SPARCSTheme.labelFont(14));
        setBorder(new EmptyBorder(10, 14, 10, 14));
        setEchoChar('●');
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(SPARCSTheme.FIELD_BG);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
        g2.dispose();
        super.paintComponent(g);
    }
}
