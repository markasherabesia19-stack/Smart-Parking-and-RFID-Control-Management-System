package sparcs.ui.panels;

import sparcs.ui.components.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class EntryExitPanel extends JPanel {

    public EntryExitPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        GradientPanel bg = new GradientPanel();
        bg.setLayout(new BorderLayout());
        bg.setBorder(new EmptyBorder(20, 24, 20, 24));
        add(bg, BorderLayout.CENTER);

        JPanel inner = new JPanel(new BorderLayout(0, 16));
        inner.setOpaque(false);
        bg.add(inner, BorderLayout.CENTER);

        inner.add(new PageHeader("ENTRY / EXIT"), BorderLayout.NORTH);

        // RFID card
        CardPanel rfidCard = new CardPanel();
        rfidCard.setLayout(new BorderLayout());
        rfidCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel rfidTitle = new JLabel("RFID");
        rfidTitle.setFont(SPARCSTheme.boldFont(16));
        rfidTitle.setForeground(SPARCSTheme.ACCENT_PINK);

        // TODO: populate with live RFID scan data from SQL / hardware
        JPanel rfidBody = new JPanel();
        rfidBody.setOpaque(false);

        rfidCard.add(rfidTitle, BorderLayout.NORTH);
        rfidCard.add(rfidBody, BorderLayout.CENTER);

        inner.add(rfidCard, BorderLayout.CENTER);
    }
}
