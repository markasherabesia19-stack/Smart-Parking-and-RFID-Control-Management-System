package ui.shared;

import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import java.awt.*;

public class SplashPanel {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel) {
        JPanel p = UIFactory.backgroundImagePanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;

        gc.gridy = 0;
        p.add(UIFactory.logoPanel(90), gc);

        gc.gridy = 2;
        gc.insets = new Insets(4, 0, 30, 0);
        p.add(UIFactory.lbl("Smart Parking & RFID Control Management System",
                Font.PLAIN, 13, C_MUTED), gc);

        gc.gridy = 3;
        gc.insets = new Insets(0, 0, 0, 0);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setPreferredSize(new Dimension(280, 6));
        bar.setBorderPainted(false);
        bar.setBackground(new Color(50, 35, 90, 160));
        bar.setForeground(C_ACCENT);
        bar.setOpaque(true);
        p.add(bar, gc);

        // Animate progress bar then switch to animation screen
        Timer fill = new Timer(10, null);
        fill.addActionListener(e -> {
            bar.setValue(bar.getValue() + 1);
            if (bar.getValue() >= 100) {
                fill.stop();
                cardLayout.show(rootPanel, "ANIMATION");
            }
        });
        fill.start();

        return p;
    }
}