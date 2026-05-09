package ui.admin;

import dao.AuditLogDAO;
import dao.VehicleDAO;
import dao.ParkingSlotDAO;
import dao.ParkingTransactionDAO;
import model.AppState;
import model.AuditLog;
import model.ParkingSlot;
import model.ParkingTransaction;
import model.Vehicle;
import ui.shared.SidebarPanel;
import util.UIFactory;
import util.DialogUtil;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AdminEntryExitScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.setOpaque(true);
        root.setName("ADMIN_ENTRY_EXIT");
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_ENTRY_EXIT"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);
        content.setOpaque(true);

        // ── Top bar ───────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("ENTRY / EXIT", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        topBar.add(UIFactory.lbl("Record vehicle arrivals and departures", Font.PLAIN, 11, C_MUTED), BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // ── Body: side-by-side cards ──────────────────────────────────────────
        JPanel body = new JPanel(new GridLayout(1, 2, 16, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(28, 28, 28, 28));

        VehicleDAO vehicleDAO         = new VehicleDAO();
        ParkingSlotDAO slotDAO        = new ParkingSlotDAO();
        ParkingTransactionDAO txDAO   = new ParkingTransactionDAO();
        AuditLogDAO auditDAO          = new AuditLogDAO();

        // ── ENTRY card ────────────────────────────────────────────────────────
        JPanel entryCard = buildActionCard(
            "RECORD ENTRY",
            "Scan or type the vehicle's RFID tag or license plate to record arrival.",
            C_AVAILABLE,
            new int[][]{ // Arrow-in icon points
                {18,10}, {26,18}, {18,26}, {18,21}, {6,21}, {6,15}, {18,15}, {18,10}
            },
            true
        );

        JTextField rfidEntry = (JTextField) entryCard.getClientProperty("field1");
        JTextField slotEntry = (JTextField) entryCard.getClientProperty("field2");
        JButton entryBtn     = (JButton)    entryCard.getClientProperty("btn");

        entryBtn.addActionListener(e -> {
            String plateInput = rfidEntry.getText().trim();
            String slotInput  = slotEntry.getText().trim().toUpperCase();

            if (plateInput.isEmpty() || slotInput.isEmpty()) {
                DialogUtil.showMessageDialog(null,
                    "Please enter both a plate number and a slot number.",
                    "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                Optional<Vehicle> found = vehicleDAO.findByPlateNumber(plateInput);
                if (found.isEmpty()) {
                    DialogUtil.showMessageDialog(null,
                        "No active vehicle found for: " + plateInput,
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Vehicle vehicle = found.get();

                List<ParkingTransaction> activeTxList = txDAO.findByVehicleId(vehicle.getVehicleId());
                boolean alreadyParked = activeTxList.stream()
                    .anyMatch(t -> "IN_PROGRESS".equals(t.getTransactionStatus()));
                if (alreadyParked) {
                    Optional<ParkingSlot> currentSlot = slotDAO.findByVehicleId(vehicle.getVehicleId());
                    String currentSlotCode = currentSlot.map(ParkingSlot::getSlotCode).orElse("unknown slot");
                    DialogUtil.showMessageDialog(null,
                        "Vehicle " + plateInput + " is already parked at slot " + currentSlotCode + ".\n"
                        + "Please record an exit first before recording a new entry.",
                        "Already Parked", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Optional<ParkingSlot> slotOpt = slotDAO.findBySlotCode(slotInput);
                if (slotOpt.isEmpty()) {
                    DialogUtil.showMessageDialog(null,
                        "Slot \"" + slotInput + "\" not found. Check the slot code (e.g. B-04).",
                        "Slot Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                ParkingSlot slot = slotOpt.get();
                if (slot.isOccupied()) {
                    DialogUtil.showMessageDialog(null,
                        "Slot " + slotInput + " is already Occupied.",
                        "Slot Unavailable", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                vehicleDAO.updateParkingStatus(vehicle.getVehicleId(), "Parked");
                slotDAO.occupySlot(slot.getSlotId());
                slotDAO.updateCurrentVehicle(slot.getSlotId(), vehicle.getVehicleId());

                ParkingTransaction tx = new ParkingTransaction(
                    vehicle.getVehicleId(), slot.getSlotId(), LocalDateTime.now());
                txDAO.create(tx);

                AuditLog entryLog = new AuditLog();
                entryLog.setAction("ENTRY");
                entryLog.setEntityType("PARKING_SLOT");
                entryLog.setEntityId(slot.getSlotId());
                entryLog.setNewValue(plateInput);
                entryLog.setOldValue(slotInput);
                entryLog.setIpAddress("localhost");
                auditDAO.create(entryLog);

                state.notifySlotChange();
                rfidEntry.setText("");
                slotEntry.setText("");
                DialogUtil.showMessageDialog(null,
                    "Entry recorded — " + plateInput + " assigned to slot " + slotInput + ".",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                ex.printStackTrace();
                DialogUtil.showMessageDialog(null,
                    "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ── EXIT card ─────────────────────────────────────────────────────────
        JPanel exitCard = buildActionCard(
            "RECORD EXIT",
            "Scan or type the vehicle's RFID tag or license plate to record departure.",
            C_OCCUPIED,
            new int[][]{ // Arrow-out icon
                {14,10}, {14,15}, {6,15}, {6,21}, {14,21}, {14,26}, {26,18}, {14,10}
            },
            false
        );

        JTextField rfidExit = (JTextField) exitCard.getClientProperty("field1");
        JButton exitBtn     = (JButton)    exitCard.getClientProperty("btn");

        exitBtn.addActionListener(e -> {
            String plateInput = rfidExit.getText().trim();

            if (plateInput.isEmpty()) {
                DialogUtil.showMessageDialog(null,
                    "Please enter a plate number.",
                    "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                Optional<Vehicle> found = vehicleDAO.findByPlateNumber(plateInput);
                if (found.isEmpty()) {
                    DialogUtil.showMessageDialog(null,
                        "No vehicle found for: " + plateInput,
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Vehicle vehicle = found.get();

                Optional<ParkingSlot> slotOpt = slotDAO.findByVehicleId(vehicle.getVehicleId());
                if (slotOpt.isEmpty()) {
                    DialogUtil.showMessageDialog(null,
                        "Vehicle " + plateInput + " is not currently occupying any slot.\n"
                        + "It may have already exited.",
                        "Not Parked", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                ParkingSlot slot = slotOpt.get();
                String resolvedSlotCode = slot.getSlotCode();

                vehicleDAO.updateParkingStatus(vehicle.getVehicleId(), "Not Parked");
                slotDAO.vacateSlot(slot.getSlotId());
                slotDAO.updateCurrentVehicle(slot.getSlotId(), null);

                List<ParkingTransaction> txList = txDAO.findByVehicleId(vehicle.getVehicleId());
                for (ParkingTransaction tx : txList) {
                    if ("IN_PROGRESS".equals(tx.getTransactionStatus())) {
                        LocalDateTime exitTime = LocalDateTime.now();
                        long minutes = java.time.Duration.between(tx.getEntryTime(), exitTime).toMinutes();
                        tx.setExitTime(exitTime);
                        tx.setDurationMinutes((int) minutes);
                        tx.setTransactionStatus("COMPLETED");
                        txDAO.update(tx);
                        break;
                    }
                }

                AuditLog exitLog = new AuditLog();
                exitLog.setAction("EXIT");
                exitLog.setEntityType("PARKING_SLOT");
                exitLog.setEntityId(slot.getSlotId());
                exitLog.setNewValue(plateInput);
                exitLog.setOldValue(resolvedSlotCode);
                exitLog.setIpAddress("localhost");
                auditDAO.create(exitLog);

                state.notifySlotChange();
                rfidExit.setText("");
                DialogUtil.showMessageDialog(null,
                    "Exit recorded — " + plateInput + " has left slot " + resolvedSlotCode + ".",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                ex.printStackTrace();
                DialogUtil.showMessageDialog(null,
                    "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        body.add(entryCard);
        body.add(exitCard);
        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    // ── Card builder ──────────────────────────────────────────────────────────
    private static JPanel buildActionCard(String title, String description,
                                          Color accentColor, int[][] iconPoints,
                                          boolean twoFields) {
        JPanel card = UIFactory.cardPanel(new BorderLayout(0, 0));

        // ── Colored header strip ──────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c1 = twoFields
                    ? new Color(30, 140, 80, 220)
                    : new Color(180, 40, 60, 220);
                Color c2 = twoFields
                    ? new Color(20, 100, 60, 180)
                    : new Color(140, 20, 40, 180);
                GradientPaint gp = new GradientPaint(0, 0, c1, getWidth(), 0, c2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + 20, 18, 18);

                // Dot pattern
                g2.setColor(new Color(255, 255, 255, 10));
                for (int x = 8; x < getWidth(); x += 16)
                    for (int y = 8; y < getHeight() + 20; y += 16)
                        g2.fillOval(x, y, 3, 3);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        header.setPreferredSize(new Dimension(0, 80));
        header.setBorder(new EmptyBorder(0, 22, 0, 22));

        // Icon panel
        JPanel iconPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 35));
                g2.fillRoundRect(0, 0, 38, 38, 10, 10);
                g2.setColor(new Color(255, 255, 255, 220));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // Draw arrow polygon
                int[] xs = new int[iconPoints.length];
                int[] ys = new int[iconPoints.length];
                for (int i = 0; i < iconPoints.length; i++) {
                    xs[i] = iconPoints[i][0];
                    ys[i] = iconPoints[i][1];
                }
                g2.fillPolygon(xs, ys, iconPoints.length);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(38, 38); }
            @Override public boolean isOpaque() { return false; }
        };

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 20));
        headerLeft.setOpaque(false);
        headerLeft.add(iconPanel);

        JPanel headerTitles = new JPanel(new GridLayout(2, 1, 0, 2));
        headerTitles.setOpaque(false);
        headerTitles.add(UIFactory.lbl(title, Font.BOLD, 14, Color.WHITE));
        headerTitles.add(UIFactory.lbl(twoFields ? "Vehicle arrival" : "Vehicle departure",
                Font.PLAIN, 11, new Color(255, 255, 255, 180)));
        headerLeft.add(headerTitles);
        header.add(headerLeft, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);

        // ── Form body ─────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(24, 24, 24, 24));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridx = 0;

        // Description
        gc.gridy = 0; gc.insets = new Insets(0, 0, 20, 0);
        JLabel descLbl = new JLabel("<html><div style='width:280px'>" + description + "</div></html>");
        descLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        descLbl.setForeground(C_MUTED);
        form.add(descLbl, gc);

        // Field 1 — RFID / Plate
        gc.gridy = 1; gc.insets = new Insets(0, 0, 4, 0);
        form.add(makeSectionLabel("RFID TAG / PLATE NUMBER", accentColor), gc);
        gc.gridy = 2; gc.insets = new Insets(0, 0, twoFields ? 16 : 0, 0);
        JTextField field1 = UIFactory.styledField("Scan or type RFID / plate");
        form.add(field1, gc);

        JTextField field2 = null;
        if (twoFields) {
            gc.gridy = 3; gc.insets = new Insets(0, 0, 4, 0);
            form.add(makeSectionLabel("SLOT NUMBER", accentColor), gc);
            gc.gridy = 4; gc.insets = new Insets(0, 0, 0, 0);
            field2 = UIFactory.styledField("e.g. B-04");
            form.add(field2, gc);
        }

        // Spacer to push button to bottom
        gc.gridy = 5; gc.weighty = 1.0;
        gc.insets = new Insets(0, 0, 0, 0);
        form.add(Box.createVerticalGlue(), gc);
        gc.weighty = 0;

        // Status hint row
        gc.gridy = 6; gc.insets = new Insets(0, 0, 12, 0);
        JPanel hintRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        hintRow.setOpaque(false);
        JLabel hintDot = UIFactory.lbl("●", Font.PLAIN, 10, accentColor);
        JLabel hintTxt = UIFactory.lbl(
            twoFields ? "Vehicle will be marked as PARKED" : "Vehicle will be marked as EXITED",
            Font.PLAIN, 11, C_MUTED);
        hintRow.add(hintDot);
        hintRow.add(hintTxt);
        form.add(hintRow, gc);

        // Button
        gc.gridy = 7; gc.insets = new Insets(0, 0, 0, 0);
        JButton btn = UIFactory.gradientButton(title);
        form.add(btn, gc);

        card.add(form, BorderLayout.CENTER);

        // Store references for caller to wire up logic
        card.putClientProperty("field1", field1);
        card.putClientProperty("field2", field2);
        card.putClientProperty("btn",    btn);

        return card;
    }

    // ── Section label with colored left bar ───────────────────────────────────
    private static JPanel makeSectionLabel(String text, Color accent) {
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
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(bar);
        left.add(UIFactory.lbl(text, Font.BOLD, 10, C_MUTED));
        row.add(left, BorderLayout.WEST);
        return row;
    }
}