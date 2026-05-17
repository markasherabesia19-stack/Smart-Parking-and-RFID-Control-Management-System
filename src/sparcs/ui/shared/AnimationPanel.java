package ui.shared;

import util.UIFactory;
import javax.swing.*;
import java.awt.*;

public class AnimationPanel {
    private static final int ANIMATION_DURATION_MS = 10000; 

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel) {
        JPanel backgroundPanel = UIFactory.backgroundImagePanel(new BorderLayout());
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        
        ImageIcon gif = UIFactory.getAnimationGif();
        if (gif != null) {
            int width = gif.getIconWidth();
            int height = gif.getIconHeight();
            System.out.println("[AnimationPanel] GIF loaded: " + width + "x" + height);

            if (width > 800 || height > 600) {
                double scale = Math.min(800.0 / width, 600.0 / height);
                Image scaled = gif.getImage().getScaledInstance(
                    (int)(width * scale), (int)(height * scale), Image.SCALE_FAST);
                gif = new ImageIcon(scaled);
                System.out.println("[AnimationPanel] GIF scaled to: " + (int)(width * scale) + "x" + (int)(height * scale));
            }
            
            JLabel gifLabel = new JLabel(gif);
            gifLabel.setOpaque(false);
            gifLabel.setHorizontalAlignment(SwingConstants.CENTER);
            gifLabel.setVerticalAlignment(SwingConstants.CENTER);
            
            centerPanel.add(gifLabel, BorderLayout.CENTER);
            System.out.println("[AnimationPanel] GIF label added to center panel");
        } else {
            System.err.println("[AnimationPanel] Failed to load animation GIF");
        }
        
        backgroundPanel.add(centerPanel, BorderLayout.CENTER);
        
        Timer transition = new Timer(ANIMATION_DURATION_MS, e -> {
            System.out.println("[AnimationPanel] Transitioning to UNIFIED_LOGIN");
            cardLayout.show(rootPanel, "UNIFIED_LOGIN");
        });
        transition.setRepeats(false);
        transition.start();
        
        return backgroundPanel;
    }
}
