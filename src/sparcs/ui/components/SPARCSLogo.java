package sparcs.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 * Renders the SPARCS logo from the bundled PNG asset (transparent background).
 * Falls back to text-only "S" if the image cannot be loaded.
 *
 * PNG is loaded from classpath: /sparcs/assets/sparcs_logo.png
 */
public class SPARCSLogo extends JComponent {

    private static BufferedImage LOGO_IMAGE = null;

    static {
        try {
            URL url = SPARCSLogo.class.getResource("/sparcs/assets/sparcs_logo.png");
            if (url != null) {
                LOGO_IMAGE = ImageIO.read(url);
            } else {
                System.err.println("sparcs_logo.png not found on classpath.");
            }
        } catch (IOException e) {
            System.err.println("Could not load sparcs_logo.png: " + e.getMessage());
        }
    }

    private final int size;
    private final boolean showText;

    public SPARCSLogo(int size, boolean showText) {
        this.size     = size;
        this.showText = showText;
        int totalH = size + (showText ? 26 : 0);
        setPreferredSize(new Dimension(size, totalH));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);

        int cx = getWidth() / 2;

        if (LOGO_IMAGE != null) {
            int srcW  = LOGO_IMAGE.getWidth();
            int srcH  = LOGO_IMAGE.getHeight();
            int drawH = size;
            int drawW = (int) ((double) srcW / srcH * drawH);
            int x     = cx - drawW / 2;
            g2.drawImage(LOGO_IMAGE, x, 0, drawW, drawH, null);
        } else {
            // Fallback text
            g2.setFont(SPARCSTheme.boldFont(size));
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            int sw = fm.stringWidth("S");
            g2.drawString("S", cx - sw / 2, size - 4);
            if (showText) {
                g2.setFont(SPARCSTheme.boldFont(18));
                fm = g2.getFontMetrics();
                int tw = fm.stringWidth("SPARCS");
                g2.drawString("SPARCS", cx - tw / 2, size + 20);
            }
        }
        g2.dispose();
    }
}
