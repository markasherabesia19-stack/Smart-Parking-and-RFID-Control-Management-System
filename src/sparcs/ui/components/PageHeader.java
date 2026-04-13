package sparcs.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Page header: large bold title on the left, SPARCS logo icon on the right.
 */
public class PageHeader extends JPanel {

    public PageHeader(String title) {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 0, 20, 0));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(SPARCSTheme.titleFont(36));
        titleLbl.setForeground(SPARCSTheme.TEXT_WHITE);
        add(titleLbl, BorderLayout.WEST);

        SPARCSLogo miniLogo = new SPARCSLogo(36, false);
        add(miniLogo, BorderLayout.EAST);
    }
}
