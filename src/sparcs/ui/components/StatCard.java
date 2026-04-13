package sparcs.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Small stat card: label on top, big colored value, subtitle below.
 */
public class StatCard extends CardPanel {

    private final JLabel valueLabel;
    private final JLabel subtitleLabel;

    public StatCard(String label, String value, String subtitle,
                    Color accentBorder, Color valueColor) {
        super(accentBorder);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(SPARCSTheme.labelFont(10));
        lbl.setForeground(SPARCSTheme.TEXT_LABEL);
        lbl.setAlignmentX(LEFT_ALIGNMENT);

        valueLabel = new JLabel(value);
        valueLabel.setFont(SPARCSTheme.boldFont(40));
        valueLabel.setForeground(valueColor);
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);

        subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(SPARCSTheme.labelFont(11));
        subtitleLabel.setForeground(SPARCSTheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);

        add(lbl);
        add(Box.createVerticalStrut(6));
        add(valueLabel);
        add(Box.createVerticalStrut(4));
        add(subtitleLabel);
    }

    public void setValue(String v)    { valueLabel.setText(v); }
    public void setSubtitle(String s) { subtitleLabel.setText(s); }
}
