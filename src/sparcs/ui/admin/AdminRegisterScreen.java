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
import util.DialogUtil;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;

public class AdminRegisterScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.setOpaque(true);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_REGISTER"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);
        content.setOpaque(true);

        // ── Top bar ───────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("REGISTER VEHICLE", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        topBar.add(UIFactory.lbl("Vehicles  /  Register New", Font.PLAIN, 11, C_MUTED), BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // ── Body ──────────────────────────────────────────────────────────────
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(28, 28, 28, 28));

        // ── LEFT info card ────────────────────────────────────────────────────
        JPanel leftCol = UIFactory.cardPanel(new GridBagLayout());
        leftCol.setPreferredSize(new Dimension(240, 0));

        JPanel leftInner = new JPanel();
        leftInner.setLayout(new BoxLayout(leftInner, BoxLayout.Y_AXIS));
        leftInner.setOpaque(false);
        leftInner.setBorder(new EmptyBorder(0, 12, 0, 12));

        // Car illustration
        JPanel carIllustration = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                // Outer soft glow rings
                g2.setColor(new Color(127, 119, 221, 10));
                g2.fillOval(cx - 80, cy - 80, 160, 160);
                g2.setColor(new Color(127, 119, 221, 18));
                g2.fillOval(cx - 65, cy - 65, 130, 130);
                g2.setColor(new Color(160, 100, 255, 28));
                g2.fillOval(cx - 50, cy - 50, 100, 100);

                // Glowing circle rings (stroke)
                g2.setStroke(new BasicStroke(1.2f));
                g2.setColor(new Color(127, 119, 221, 55));
                g2.drawOval(cx - 62, cy - 62, 124, 124);
                g2.setColor(new Color(210, 50, 140, 35));
                g2.drawOval(cx - 72, cy - 72, 144, 144);

                // Floating sparkle dots around the car
                int[][] dots = {{cx - 55, cy - 48}, {cx + 56, cy - 40},
                                {cx - 46, cy + 50}, {cx + 50, cy + 42},
                                {cx,      cy - 70}, {cx + 28, cy - 60}};
                Color[] dotColors = {
                    new Color(127, 119, 221, 140), new Color(210, 50, 140, 120),
                    new Color(60,  210, 130, 100), new Color(127, 119, 221, 100),
                    new Color(210, 50, 140,  90),  new Color(255, 200, 80,  80)
                };
                int[] dotSizes = {5, 4, 3, 5, 3, 4};
                for (int d = 0; d < dots.length; d++) {
                    g2.setColor(dotColors[d]);
                    g2.fillOval(dots[d][0], dots[d][1], dotSizes[d], dotSizes[d]);
                }

                // Ground shadow beneath car
                g2.setColor(new Color(80, 60, 180, 40));
                g2.fillOval(cx - 38, cy + 40, 76, 12);
                // Car body
                g2.setColor(new Color(200, 190, 255, 210));
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int bx = cx - 44, by = cy, bw = 88, bh = 28;
                g2.drawRoundRect(bx, by, bw, bh, 8, 8);
                // Cabin roof
                g2.drawLine(bx + 16, by, bx + 24, by - 20);
                g2.drawLine(bx + bw - 16, by, bx + bw - 24, by - 20);
                g2.drawLine(bx + 24, by - 20, bx + bw - 24, by - 20);
                // Window divider
                g2.drawLine(bx + (bw / 2), by, bx + (bw / 2), by - 18);
                // Wheels
                g2.setColor(new Color(127, 119, 221, 170));
                g2.fillOval(bx + 8, by + bh - 10, 24, 24);
                g2.fillOval(bx + bw - 32, by + bh - 10, 24, 24);
                g2.setColor(new Color(200, 190, 255, 200));
                g2.drawOval(bx + 8, by + bh - 10, 24, 24);
                g2.drawOval(bx + bw - 32, by + bh - 10, 24, 24);
                // Hub dots
                g2.fillOval(bx + 17, by + bh - 1, 6, 6);
                g2.fillOval(bx + bw - 23, by + bh - 1, 6, 6);
                // Headlight accent
                g2.setColor(new Color(255, 210, 80, 180));
                g2.fillOval(bx + bw - 4, by + 7, 8, 6);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(180, 110); }
            @Override public boolean isOpaque() { return false; }
        };
        carIllustration.setAlignmentX(Component.CENTER_ALIGNMENT);

        leftInner.add(Box.createVerticalGlue());
        leftInner.add(Box.createVerticalGlue());
        leftInner.add(carIllustration);
        leftInner.add(Box.createVerticalStrut(14));

        JLabel sideTitle = UIFactory.lbl("Add a Vehicle", Font.BOLD, 15, C_WHITE);
        sideTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftInner.add(sideTitle);
        leftInner.add(Box.createVerticalStrut(8));

        JLabel sideDesc = new JLabel("<html><div style='text-align:center;width:165px'>"
                + "Register a new vehicle to the SPARCS parking system.</div></html>");
        sideDesc.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sideDesc.setForeground(C_MUTED);
        sideDesc.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftInner.add(sideDesc);
        leftInner.add(Box.createVerticalStrut(26));

        // Steps
        String[] steps = {"Enter owner details", "Enter vehicle info", "Submit to register"};
        Color[]  stepColors = {
            new Color(29, 185, 84),    // #1DB954 Emerald Green  — step 1
            new Color(255, 140, 66),   // #FF8C42 Amber Orange   — step 2
            new Color(167, 139, 250)}; // #A78BFA Light Purple   — step 3
        for (int i = 0; i < steps.length; i++) {
            leftInner.add(makeStepRow(i + 1, steps[i], stepColors[i]));
            if (i < steps.length - 1) leftInner.add(Box.createVerticalStrut(10));
        }
        leftInner.add(Box.createVerticalStrut(20));
        leftInner.add(Box.createVerticalGlue());
        leftInner.add(Box.createVerticalGlue());
        leftInner.add(Box.createVerticalGlue());

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.CENTER;
        lc.fill = GridBagConstraints.BOTH;
        lc.weightx = 1.0;
        lc.weighty = 1.0;
        leftCol.add(leftInner, lc);

        // ── RIGHT form card ───────────────────────────────────────────────────
        JPanel rightCol = UIFactory.cardPanel(new BorderLayout());
        rightCol.setBorder(new EmptyBorder(28, 32, 28, 32));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        // Owner section header
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
        gc.insets = new Insets(0, 0, 14, 0);
        form.add(makeSectionDivider("OWNER INFORMATION", C_PURPLE), gc);

        // Username
        gc.gridy = 1; gc.insets = new Insets(0, 0, 3, 0);
        form.add(makeFieldLabel("USERNAME"), gc);
        gc.gridy = 2; gc.insets = new Insets(0, 0, 14, 0);
        JTextField usernameField = UIFactory.styledField("e.g. juan_dela_cruz");
        form.add(usernameField, gc);

        // First / Last
        gc.gridwidth = 1; gc.weightx = 0.5;
        gc.gridy = 3; gc.gridx = 0; gc.insets = new Insets(0, 0, 3, 10);
        form.add(makeFieldLabel("FIRST NAME"), gc);
        gc.gridx = 1; gc.insets = new Insets(0, 0, 3, 0);
        form.add(makeFieldLabel("LAST NAME"), gc);

        gc.gridy = 4; gc.gridx = 0; gc.insets = new Insets(0, 0, 14, 10);
        JTextField firstNameField = UIFactory.styledField("e.g. Juan");
        form.add(firstNameField, gc);
        gc.gridx = 1; gc.insets = new Insets(0, 0, 14, 0);
        JTextField lastNameField = UIFactory.styledField("e.g. Dela Cruz");
        form.add(lastNameField, gc);

        // Contact
        gc.gridy = 5; gc.gridx = 0; gc.gridwidth = 2; gc.weightx = 1.0;
        gc.insets = new Insets(0, 0, 3, 0);
        form.add(makeFieldLabel("CONTACT NUMBER"), gc);
        gc.gridy = 6; gc.insets = new Insets(0, 0, 22, 0);
        JTextField contactField = UIFactory.styledField("e.g. 09171234567");
        form.add(contactField, gc);

        // Vehicle section header
        gc.gridy = 7; gc.insets = new Insets(0, 0, 14, 0);
        form.add(makeSectionDivider("VEHICLE DETAILS", C_PINK), gc);

        // Plate
        gc.gridy = 8; gc.insets = new Insets(0, 0, 3, 0);
        form.add(makeFieldLabel("LICENSE PLATE"), gc);
        gc.gridy = 9; gc.insets = new Insets(0, 0, 14, 0);
        JTextField plateField = UIFactory.styledField("e.g. ABC 1234");
        form.add(plateField, gc);

        // Type / Color
        gc.gridwidth = 1; gc.weightx = 0.5;
        gc.gridy = 10; gc.gridx = 0; gc.insets = new Insets(0, 0, 3, 10);
        form.add(makeFieldLabel("VEHICLE TYPE"), gc);
        gc.gridx = 1; gc.insets = new Insets(0, 0, 3, 0);
        form.add(makeFieldLabel("COLOR"), gc);

        gc.gridy = 11; gc.gridx = 0; gc.insets = new Insets(0, 0, 28, 10);
        JTextField vehicleTypeField = UIFactory.styledField("e.g. Sedan, SUV");
        form.add(vehicleTypeField, gc);
        gc.gridx = 1; gc.insets = new Insets(0, 0, 28, 0);
        JTextField colorField = UIFactory.styledField("e.g. Silver");
        form.add(colorField, gc);

        // Buttons
        gc.gridy = 12; gc.gridx = 0; gc.gridwidth = 1; gc.weightx = 0.5;
        gc.insets = new Insets(0, 0, 0, 10);
        JButton backBtn = UIFactory.outlineButton("BACK TO VEHICLES");
        backBtn.setPreferredSize(new Dimension(160, 42));
        form.add(backBtn, gc);

        gc.gridx = 1; gc.insets = new Insets(0, 0, 0, 0);
        JButton registerBtn = UIFactory.gradientButton("REGISTER VEHICLE");
        registerBtn.setPreferredSize(new Dimension(160, 42));
        form.add(registerBtn, gc);

        rightCol.add(form, BorderLayout.CENTER);

        body.add(leftCol, BorderLayout.WEST);
        body.add(rightCol, BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        // ── Action logic (unchanged) ──────────────────────────────────────────
        registerBtn.addActionListener(e -> {
            String username    = usernameField.getText().trim();
            String firstName   = firstNameField.getText().trim();
            String lastName    = lastNameField.getText().trim();
            String plate       = plateField.getText().trim();
            String vehicleType = vehicleTypeField.getText().trim();
            String color       = colorField.getText().trim();
            String contactNum  = contactField.getText().trim();

            if (username.isEmpty() || firstName.isEmpty() || lastName.isEmpty()
                    || plate.isEmpty() || vehicleType.isEmpty()) {
                DialogUtil.showMessageDialog(null, "Please fill in all required fields.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                UserAccountDAO userDAO = new UserAccountDAO();
                Optional<UserAccount> userOpt = userDAO.findByUsername(username);
                if (userOpt.isEmpty()) {
                    DialogUtil.showMessageDialog(null, "User '" + username + "' not found!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                UserAccount user = userOpt.get();

                VehicleDAO vehicleDAO = new VehicleDAO();
                if (vehicleDAO.findByPlateNumber(plate).isPresent()) {
                    DialogUtil.showMessageDialog(null, "License plate '" + plate + "' is already registered!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                VehicleOwner owner = new VehicleOwner();
                owner.setUserId(user.getUserId());
                owner.setFirstName(firstName);
                owner.setLastName(lastName);
                owner.setContactNumber(contactNum);
                owner.setAddress("");

                VehicleOwnerDAO ownerDAO = new VehicleOwnerDAO();
                owner = ownerDAO.createOrFind(owner);

                Vehicle vehicle = new Vehicle();
                vehicle.setOwnerId(owner.getOwnerId());
                vehicle.setPlateNumber(plate);
                vehicle.setVehicleType(vehicleType);
                vehicle.setColor(color);
                vehicle.setActive(true);
                vehicleDAO.create(vehicle);

                String rfidTag = "SPARCS-" + plate.replaceAll("[^\\x00-\\x7F]", "");
                RFIDMapping mapping = new RFIDMapping();
                mapping.setRfidTag(rfidTag);
                mapping.setVehicleId(vehicle.getVehicleId());
                mapping.setActive(true);

                RFIDMappingDAO rfidDAO = new RFIDMappingDAO();
                rfidDAO.create(mapping);

                int choice = DialogUtil.showConfirmDialog(null,
                        "Vehicle '" + plate + "' registered successfully for "
                                + firstName + " " + lastName + "!\n\nView RFID card for this vehicle?",
                        "Success", JOptionPane.YES_NO_OPTION);

                usernameField.setText(""); firstNameField.setText(""); lastNameField.setText("");
                plateField.setText(""); vehicleTypeField.setText("");
                colorField.setText(""); contactField.setText("");

                if (choice == JOptionPane.YES_OPTION) {
                    String panelKey = "VEHICLE_RFID_" + plate;
                    for (Component c : rootPanel.getComponents()) {
                        if (panelKey.equals(c.getName())) { rootPanel.remove(c); break; }
                    }
                    JPanel rfidPanel = VehicleRFIDCardScreen.build(cardLayout, rootPanel, state, vehicle, mapping);
                    rfidPanel.setName(panelKey);
                    rootPanel.add(rfidPanel, panelKey);
                    cardLayout.show(rootPanel, panelKey);
                } else {
                    cardLayout.show(rootPanel, "ADMIN_VEHICLES");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                DialogUtil.showMessageDialog(null, "Error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "ADMIN_VEHICLES"));

        return root;
    }

    private static JPanel makeSectionDivider(String text, Color accent) {
        JPanel row = new JPanel(new BorderLayout(8, 0)) {
            @Override public boolean isOpaque() { return false; }
        };
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 2, 2);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(3, 14); }
            @Override public boolean isOpaque() { return false; }
        };
        JPanel linePanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(175, 169, 236, 35));
                g.fillRect(0, getHeight() / 2, getWidth(), 1);
            }
            @Override public boolean isOpaque() { return false; }
        };
        JPanel leftPart = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftPart.setOpaque(false);
        leftPart.add(bar);
        leftPart.add(UIFactory.lbl(text, Font.BOLD, 10, C_MUTED));
        row.add(leftPart, BorderLayout.WEST);
        row.add(linePanel, BorderLayout.CENTER);
        return row;
    }

    private static JLabel makeFieldLabel(String text) {
        return UIFactory.lbl(text, Font.BOLD, 10, C_MUTED);
    }

    private static JPanel makeStepRow(int num, String label, Color color) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(200, 30));
        JPanel circle = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
                g2.fillOval(0, 0, 22, 22);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(0, 0, 22, 22);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                String s = String.valueOf(num);
                g2.drawString(s, (22 - fm.stringWidth(s)) / 2,
                        (22 + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(22, 22); }
            @Override public boolean isOpaque() { return false; }
        };
        row.add(circle);
        row.add(UIFactory.lbl(label, Font.PLAIN, 11, new Color(155, 143, 212))); // #9B8FD4
        return row;
    }
}