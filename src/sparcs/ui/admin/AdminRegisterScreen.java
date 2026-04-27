package ui.admin;

import dao.UserAccountDAO;
import dao.VehicleDAO;
import dao.VehicleOwnerDAO;
import model.AppState;
import model.UserAccount;
import model.Vehicle;
import model.VehicleOwner;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.Optional;

/**
 * SPARCS — Admin register vehicle screen.
 */
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
        card.setPreferredSize(new Dimension(420, 480));
        card.setBorder(new EmptyBorder(28, 32, 28, 32));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0; cc.fill = GridBagConstraints.HORIZONTAL;
        cc.insets = new Insets(6, 0, 2, 0);

        String[] labels = {"OWNER USERNAME", "PLATE NUMBER", "RFID TAG ID", "VEHICLE TYPE", "CONTACT NUMBER"};
        JTextField[] fields = new JTextField[labels.length - 1];
        JComboBox<String> ownerDropdown = new JComboBox<>();
        
        // Load usernames from database
        try {
            UserAccountDAO userAccountDAO = new UserAccountDAO();
            java.util.List<UserAccount> users = userAccountDAO.findByRole("USER");
            for (UserAccount user : users) {
                ownerDropdown.addItem(user.getUsername());
            }
            if (users.isEmpty()) {
                ownerDropdown.addItem("(No users found)");
                ownerDropdown.setEnabled(false);
            }
        } catch (SQLException ex) {
            System.err.println("Error loading users: " + ex.getMessage());
            ownerDropdown.addItem("(Error loading users)");
            ownerDropdown.setEnabled(false);
        }


        for (int i = 0; i < labels.length; i++) {
            cc.gridy = i * 2;
            card.add(UIFactory.lbl(labels[i], Font.BOLD, 10, C_MUTED), cc);
            cc.gridy = i * 2 + 1; cc.insets = new Insets(0, 0, 4, 0);
            
            if (i == 0) {
                card.add(ownerDropdown, cc);
            } else {
                fields[i - 1] = UIFactory.styledField("");
                card.add(fields[i - 1], cc);
            }
            cc.insets = new Insets(6, 0, 2, 0);
        }

        cc.gridy = labels.length * 2; cc.insets = new Insets(18, 0, 6, 0);
        JButton registerBtn = UIFactory.gradientButton("REGISTER VEHICLE");
        card.add(registerBtn, cc);

        cc.gridy++; cc.insets = new Insets(0, 0, 0, 0);
        JButton backBtn = UIFactory.outlineButton("BACK TO VEHICLES");
        card.add(backBtn, cc);

        registerBtn.addActionListener(e -> {
            String ownerUsername = (String) ownerDropdown.getSelectedItem();
            String plateNumber = fields[0].getText().trim();
            String rfidTagStr = fields[1].getText().trim();
            String vehicleType = fields[2].getText().trim();
            String contactNumber = fields[3].getText().trim();

            // Validate required fields
            if (ownerUsername == null || ownerUsername.isEmpty() || ownerUsername.equals("(No users found)") || 
                plateNumber.isEmpty() || vehicleType.isEmpty()) {
                JOptionPane.showMessageDialog(null, 
                    "Please select an owner, fill in Plate Number, and Vehicle Type.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                UserAccountDAO userAccountDAO = new UserAccountDAO();
                VehicleOwnerDAO ownerDAO = new VehicleOwnerDAO();
                VehicleDAO vehicleDAO = new VehicleDAO();

                Optional<UserAccount> userOpt = userAccountDAO.findByUsername(ownerUsername);
                if (userOpt.isEmpty()) {
                    JOptionPane.showMessageDialog(null,
                        "Owner username not found.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (vehicleDAO.findByPlateNumber(plateNumber).isPresent()) {
                    JOptionPane.showMessageDialog(null,
                        "Plate number already exists.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                UserAccount ownerUser = userOpt.get();
                VehicleOwner owner;

                Optional<VehicleOwner> existingOwner = ownerDAO.findByUserId(ownerUser.getUserId());
                if (existingOwner.isPresent()) {
                    owner = existingOwner.get();
                    if (!contactNumber.isEmpty()) {
                        owner.setContactNumber(contactNumber);
                        ownerDAO.update(owner);
                    }
                } else {
                    owner = new VehicleOwner();
                    owner.setUserId(ownerUser.getUserId());
                    owner.setContactNumber(contactNumber);
                    ownerDAO.create(owner);
                }

                // Create new vehicle
                Vehicle vehicle = new Vehicle(owner.getOwnerId(), plateNumber, vehicleType);
                vehicleDAO.create(vehicle);

                if (!rfidTagStr.isEmpty()) {
                    System.out.println("[AdminRegisterScreen] RFID tag captured but mapping table integration is pending: " + rfidTagStr);
                }

                JOptionPane.showMessageDialog(null, 
                    "Vehicle registered successfully!\nOwner: " + ownerUsername + "\nVehicle ID: " + vehicle.getVehicleId(), 
                    "Success", JOptionPane.INFORMATION_MESSAGE);

                // Clear fields
                for (JTextField field : fields) {
                    field.setText("");
                }
                ownerDropdown.setSelectedIndex(0);

                cardLayout.show(rootPanel, "ADMIN_VEHICLES");

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, 
                    "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });
        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "ADMIN_VEHICLES"));

        center.add(card, new GridBagConstraints());
        content.add(center, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }
}
