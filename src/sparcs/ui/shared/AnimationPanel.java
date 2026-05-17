package ui.shared;

import util.UIFactory;
import javax.swing.*;
import java.awt.*;

public class AnimationPanel {
    private static final int ANIMATION_DURATION_MS = 10000; 

    /**
     * Builds an animation screen with the background image and centered GIF animation.
     * Automatically transitions to the role picker after the animation duration.
     */
    public static JPanel build(CardLayout cardLayout, JPanel rootPanel) {
        // Use UIFactory's cached background instead of reloading
        JPanel backgroundPanel = UIFactory.backgroundImagePanel(new BorderLayout());
        
        // Create center panel for GIF with BorderLayout
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        
        ImageIcon gif = UIFactory.getAnimationGif();
        if (gif != null) {
            int width = gif.getIconWidth();
            int height = gif.getIconHeight();
            System.out.println("[AnimationPanel] GIF loaded: " + width + "x" + height);
            
            // Only scale if absolutely necessary, and avoid SCALE_SMOOTH which breaks animation
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
        
        // Auto-transition to unified login after animation completes
        Timer transition = new Timer(ANIMATION_DURATION_MS, e -> {
            System.out.println("[AnimationPanel] Transitioning to UNIFIED_LOGIN");
            cardLayout.show(rootPanel, "UNIFIED_LOGIN");
        });
        transition.setRepeats(false);
        transition.start();
        
        return backgroundPanel;
    }
}
