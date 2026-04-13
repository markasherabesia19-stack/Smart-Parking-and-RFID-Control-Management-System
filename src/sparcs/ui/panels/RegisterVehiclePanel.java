package sparcs.ui.panels;

import sparcs.ui.components.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RegisterVehiclePanel extends JPanel {

    public RegisterVehiclePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        GradientPanel bg = new GradientPanel();
        bg.setLayout(new BorderLayout());
        bg.setBorder(new EmptyBorder(20, 24, 20, 24));
        add(bg, BorderLayout.CENTER);

        JPanel inner = new JPanel(new BorderLayout(0, 16));
        inner.setOpaque(false);
        bg.add(inner, BorderLayout.CENTER);

        inner.add(new PageHeader("REGISTER VEHICLE"), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(2, 1, 0, 14));
        body.setOpaque(false);

        // --- Owner Information Card ---
        CardPanel ownerCard = new CardPanel();
        ownerCard.setLayout(new BorderLayout(0, 10));
        ownerCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel ownerTitle = sectionLabel("OWNER INFORMATION");
        ownerCard.add(ownerTitle, BorderLayout.NORTH);

        JPanel ownerGrid = new JPanel(new GridLayout(3, 2, 14, 10));
        ownerGrid.setOpaque(false);
        ownerGrid.add(labeledField("FIRST NAME",           new SPARCSField()));
        ownerGrid.add(labeledField("LAST NAME",            new SPARCSField()));
        ownerGrid.add(labeledField("STUDENT / EMPLOYEE ID",new SPARCSField()));
        ownerGrid.add(labeledField("RFID TAG UID",         new SPARCSField()));
        ownerGrid.add(labeledField("CONTACT NUMBER",       new SPARCSField()));
        ownerGrid.add(labeledField("INITIAL BALANCE (₱)", new SPARCSField()));
        ownerCard.add(ownerGrid, BorderLayout.CENTER);

        // --- Vehicle Details Card ---
        CardPanel vehicleCard = new CardPanel();
        vehicleCard.setLayout(new BorderLayout(0, 10));
        vehicleCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel vehicleTitle = sectionLabel("VEHICLE DETAILS");
        vehicleCard.add(vehicleTitle, BorderLayout.NORTH);

        JPanel vehicleGrid = new JPanel(new GridLayout(2, 2, 14, 10));
        vehicleGrid.setOpaque(false);
        vehicleGrid.add(labeledField("PLATE NUMBER",  new SPARCSField()));
        vehicleGrid.add(labeledField("VEHICLE TYPE",  new SPARCSField()));
        vehicleGrid.add(labeledField("MODEL",         new SPARCSField()));
        vehicleGrid.add(labeledField("COLOR",         new SPARCSField()));
        vehicleCard.add(vehicleGrid, BorderLayout.CENTER);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);
        SPARCSButton registerBtn = new SPARCSButton("REGISTER VEHICLE");
        registerBtn.setPreferredSize(new Dimension(200, 42));
        registerBtn.addActionListener(e -> handleRegister());

        SPARCSButton resetBtn = new SPARCSButton("RESET",
                new Color(0x3A2870), new Color(0x4A3880));
        resetBtn.setPreferredSize(new Dimension(120, 42));
        resetBtn.addActionListener(e -> handleReset());

        btnRow.add(registerBtn);
        btnRow.add(resetBtn);
        vehicleCard.add(btnRow, BorderLayout.SOUTH);

        body.add(ownerCard);
        body.add(vehicleCard);
        inner.add(body, BorderLayout.CENTER);
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(SPARCSTheme.boldFont(16));
        l.setForeground(SPARCSTheme.ACCENT_PINK);
        return l;
    }

    private JPanel labeledField(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(SPARCSTheme.labelFont(10));
        lbl.setForeground(SPARCSTheme.TEXT_LABEL);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        p.add(lbl,   BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private void handleRegister() {
        // TODO: read field values and insert into SQL database
    }

    private void handleReset() {
        // TODO: clear all fields
    }
}
