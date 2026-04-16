package ui.admin;

import model.AppState;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * SPARCS - Admin Entry / Exit management screen.
 * TODO (back-end): Wire RFID scan events to the log table and update slotData.
 */
public class AdminEntryExitScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_ENTRY_EXIT"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("ENTRY / EXIT", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- Entry card ---
        JPanel entryCard = UIFactory.cardPanel(new BorderLayout(0, 12));
        entryCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        entryCard.add(UIFactory.lbl("RECORD ENTRY", Font.BOLD, 13, C_AVAILABLE), BorderLayout.NORTH);

        JPanel entryForm = new JPanel(new GridLayout(0, 1, 0, 8));
        entryForm.setOpaque(false);
        entryForm.add(UIFactory.lbl("RFID TAG / PLATE", Font.BOLD, 10, C_MUTED));
        JTextField rfidEntry = UIFactory.styledField("Scan or type RFID / plate");
        entryForm.add(rfidEntry);
        entryForm.add(UIFactory.lbl("SLOT NUMBER", Font.BOLD, 10, C_MUTED));
        JTextField slotEntry = UIFactory.styledField("e.g. B-04");
        entryForm.add(slotEntry);
        entryCard.add(entryForm, BorderLayout.CENTER);

        JButton entryBtn = UIFactory.gradientButton("RECORD ENTRY");
        entryBtn.addActionListener(e ->
            JOptionPane.showMessageDialog(null, "Entry recorded for: " + rfidEntry.getText(), "Success", JOptionPane.INFORMATION_MESSAGE));
        entryCard.add(entryBtn, BorderLayout.SOUTH);
        body.add(entryCard);

        // --- Exit card ---
        JPanel exitCard = UIFactory.cardPanel(new BorderLayout(0, 12));
        exitCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        exitCard.add(UIFactory.lbl("RECORD EXIT", Font.BOLD, 13, C_OCCUPIED), BorderLayout.NORTH);

        JPanel exitForm = new JPanel(new GridLayout(0, 1, 0, 8));
        exitForm.setOpaque(false);
        exitForm.add(UIFactory.lbl("RFID TAG / PLATE", Font.BOLD, 10, C_MUTED));
        JTextField rfidExit = UIFactory.styledField("Scan or type RFID / plate");
        exitForm.add(rfidExit);
        exitCard.add(exitForm, BorderLayout.CENTER);

        JButton exitBtn = UIFactory.gradientButton("RECORD EXIT");
        exitBtn.addActionListener(e ->
            JOptionPane.showMessageDialog(null, "Exit recorded for: " + rfidExit.getText(), "Success", JOptionPane.INFORMATION_MESSAGE));
        exitCard.add(exitBtn, BorderLayout.SOUTH);
        body.add(exitCard);

        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }
}
