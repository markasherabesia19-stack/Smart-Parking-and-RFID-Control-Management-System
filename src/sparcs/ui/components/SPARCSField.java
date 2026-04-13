package sparcs.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * A styled JTextField matching the SPARCS dark-purple field look.
 */
public class SPARCSField extends JTextField {

    public SPARCSField() {
        this(20);
    }

    public SPARCSField(int cols) {
        super(cols);
        setOpaque(true);
        setBackground(SPARCSTheme.FIELD_BG);
        setForeground(SPARCSTheme.TEXT_WHITE);
        setCaretColor(SPARCSTheme.TEXT_WHITE);
        setFont(SPARCSTheme.labelFont(14));
        setBorder(new EmptyBorder(10, 14, 10, 14));
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
