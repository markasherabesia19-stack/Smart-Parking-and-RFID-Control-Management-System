package ui.admin;

import model.AppState;
import model.RFIDMapping;
import model.UserAccount;
import model.Vehicle;
import model.VehicleOwner;
import dao.RFIDMappingDAO;
import dao.UserAccountDAO;
import dao.VehicleDAO;
import dao.VehicleOwnerDAO;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;

public class AdminRegisterScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_REGISTER"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("REGISTER VEHICLE", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        JPanel card = UIFactory.cardPanel(new GridBagLayout());
        card.setPreferredSize(new Dimension(420, 540));
        card.setBorder(new EmptyBorder(28, 32, 28, 32));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0; cc.fill = GridBagConstraints.HORIZONTAL;
        cc.insets = new Insets(6, 0, 2, 0);

        String[] labels = {"USERNAME", "FIRST NAME", "LAST NAME", "LICENSE PLATE", "VEHICLE TYPE", "COLOR", "CONTACT NUMBER"};
        JTextField[] fields = new JTextField[labels.length];

        for (int i = 0; i < labels.length; i++) {
            cc.gridy = i * 2;
            card.add(UIFactory.lbl(labels[i], Font.BOLD, 10, C_MUTED), cc);
            cc.gridy = i * 2 + 1; cc.insets = new Insets(0, 0, 4, 0);
            fields[i] = UIFactory.styledField(i == 0 ? "Existing username" : "");
            card.add(fields[i], cc);
            cc.insets = new Insets(6, 0, 2, 0);
        }

        cc.gridy = labels.length * 2; cc.insets = new Insets(18, 0, 6, 0);
        JButton registerBtn = UIFactory.gradientButton("REGISTER VEHICLE");
        card.add(registerBtn, cc);

        cc.gridy++; cc.insets = new Insets(0, 0, 0, 0);
        JButton backBtn = UIFactory.outlineButton("BACK TO VEHICLES");
        card.add(backBtn, cc);

        registerBtn.addActionListener(e -> {
            String username    = fields[0].getText().trim();
            String firstName   = fields[1].getText().trim();
            String lastName    = fields[2].getText().trim();
            String plate       = fields[3].getText().trim();
            String vehicleType = fields[4].getText().trim();
            String color       = fields[5].getText().trim();
            String contactNum  = fields[6].getText().trim();

            if (username.isEmpty() || firstName.isEmpty() || lastName.isEmpty()
                    || plate.isEmpty() || vehicleType.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please fill in all required fields.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // 1. Verify user exists
                UserAccountDAO userDAO = new UserAccountDAO();
                Optional<UserAccount> userOpt = userDAO.findByUsername(username);
                if (userOpt.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "User '" + username + "' not found!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                UserAccount user = userOpt.get();

                // 2. Check if plate already exists
                VehicleDAO vehicleDAO = new VehicleDAO();
                if (vehicleDAO.findByPlateNumber(plate).isPresent()) {
                    JOptionPane.showMessageDialog(null, "License plate '" + plate + "' is already registered!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // 3. Create VehicleOwner
                VehicleOwner owner = new VehicleOwner();
                owner.setUserId(user.getUserId());
                owner.setFirstName(firstName);
                owner.setLastName(lastName);
                owner.setContactNumber(contactNum);
                owner.setAddress("");

                VehicleOwnerDAO ownerDAO = new VehicleOwnerDAO();
                ownerDAO.createOrFind(owner);

                // 4. Create Vehicle linked to owner
                Vehicle vehicle = new Vehicle();
                vehicle.setOwnerId(owner.getOwnerId());
                vehicle.setPlateNumber(plate);
                vehicle.setVehicleType(vehicleType);
                vehicle.setColor(color);
                vehicle.setActive(true);

                vehicleDAO.create(vehicle);

                // 5. Auto-generate and persist a unique RFID mapping for this vehicle.
                //    Tag format: "SPARCS-<PLATE>" — unique because plate numbers are unique.
                String rfidTag = "SPARCS-" + plate.replaceAll("[^\\x00-\\x7F]", "");

                RFIDMapping mapping = new RFIDMapping();
                mapping.setRfidTag(rfidTag);
                mapping.setVehicleId(vehicle.getVehicleId());
                mapping.setActive(true);

                RFIDMappingDAO rfidDAO = new RFIDMappingDAO();
                rfidDAO.create(mapping);

                // 6. Offer to view the RFID card immediately
                int choice = JOptionPane.showConfirmDialog(null,
                        "Vehicle '" + plate + "' registered successfully for "
                                + firstName + " " + lastName + "!\n\nView RFID card for this vehicle?",
                        "Success", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

                for (JTextField field : fields) field.setText("");

                if (choice == JOptionPane.YES_OPTION) {
                    String panelKey = "VEHICLE_RFID_" + plate;
                    rootPanel.add(VehicleRFIDCardScreen.build(cardLayout, rootPanel, state, vehicle, mapping), panelKey);
                    cardLayout.show(rootPanel, panelKey);
                } else {
                    rootPanel.add(AdminVehiclesScreen.build(cardLayout, rootPanel, state), "ADMIN_VEHICLES");
                    cardLayout.show(rootPanel, "ADMIN_VEHICLES");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "ADMIN_VEHICLES"));

        center.add(card, new GridBagConstraints());
        content.add(center, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }
}