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
import util.UIFactory;
import util.DialogUtil;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Modal overlay dialog for quick entry/exit actions on a selected parking slot.
// Appears when a slot is clicked from the slot map.

public class SlotActionDialog extends JDialog {

    public SlotActionDialog(JFrame parent, String slotCode, AppState state) {
        super(parent, "Slot Action: " + slotCode, true);
        
        VehicleDAO vehicleDAO = new VehicleDAO();
        ParkingSlotDAO slotDAO = new ParkingSlotDAO();
        ParkingTransactionDAO txDAO = new ParkingTransactionDAO();
        AuditLogDAO auditDAO = new AuditLogDAO();

        // ── Check slot status ─────────────────────────────────────────────────
        Optional<ParkingSlot> slotOpt;
        ParkingSlot slot;
        boolean isSlotOccupied;
        String occupiedVehiclePlate = null;
        
        try {
            slotOpt = slotDAO.findBySlotCode(slotCode);
            if (slotOpt.isEmpty()) {
                DialogUtil.showMessageDialog(parent, "Slot not found: " + slotCode, 
                    "Error", JOptionPane.ERROR_MESSAGE);
                dispose();
                return;
            }
            slot = slotOpt.get();
            isSlotOccupied = slot.isOccupied();
            
            // If slot is occupied, get the vehicle's plate number
            if (isSlotOccupied) {
                try {
                    // Find IN_PROGRESS transaction in this slot
                    List<ParkingTransaction> inProgressTx = txDAO.findInProgress();
                    for (ParkingTransaction tx : inProgressTx) {
                        if (tx.getSlotId() == slot.getSlotId()) {
                            Optional<Vehicle> vehicleOpt = vehicleDAO.findById(tx.getVehicleId());
                            if (vehicleOpt.isPresent()) {
                                occupiedVehiclePlate = vehicleOpt.get().getPlateNumber();
                            }
                            break;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            DialogUtil.showMessageDialog(parent, "Database error: " + ex.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        // ── Dialog styling ────────────────────────────────────────────────────
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        setUndecorated(true);
        
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.setOpaque(true);
        root.setBorder(new EmptyBorder(0, 0, 0, 0));

        // ── Header with close button ──────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(C_BG_PANEL);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel titleLbl = UIFactory.lbl("SLOT: " + slotCode, Font.BOLD, 18, C_WHITE);
        header.add(titleLbl, BorderLayout.WEST);

        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        closeBtn.setForeground(C_MUTED);
        closeBtn.setBackground(C_BG_PANEL);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.addActionListener(e -> dispose());
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        header.add(closeBtn, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        // ── Body: Input field + Dynamic buttons ────────────────────────────────
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 20, 16, 20));

        // Label for input field
        String labelText = isSlotOccupied ? "Enter Vehicle/RFID Number (to exit)" : "Enter Vehicle/RFID Number (to enter)";
        JLabel inputLabel = UIFactory.lbl(labelText, Font.PLAIN, 11, C_MUTED);
        
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        labelPanel.setOpaque(false);
        labelPanel.add(inputLabel);
        body.add(labelPanel, BorderLayout.NORTH);

        // Input field for RFID/Plate
        JTextField rfidField = UIFactory.styledField("Scan or type RFID / plate");
        if (isSlotOccupied && occupiedVehiclePlate != null) {
            rfidField.setText(occupiedVehiclePlate);
            rfidField.setEditable(false); // Read-only for occupied slots
            rfidField.setForeground(C_OCCUPIED);
        }
        body.add(rfidField, BorderLayout.CENTER);

        Color entryGreen = new Color(15, 110, 86);
        Color exitRed = new Color(180, 30, 50);

        // Buttons container — FlowLayout CENTER so the single visible button
        // is always perfectly centered regardless of which one is shown
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonContainer.setOpaque(false);

        // ── ENTRY button ──────────────────────────────────────────────────────
        JButton entryBtn = new JButton("RECORD ENTRY") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? entryGreen.darker() : entryGreen);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        entryBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        entryBtn.setForeground(Color.WHITE);
        entryBtn.setContentAreaFilled(false);
        entryBtn.setBorderPainted(false);
        entryBtn.setFocusPainted(false);
        entryBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        entryBtn.setPreferredSize(new Dimension(320, 42));
        entryBtn.setVisible(!isSlotOccupied);

        entryBtn.addActionListener(e -> {
            String plateInput = rfidField.getText().trim();

            if (plateInput.isEmpty()) {
                DialogUtil.showMessageDialog(this,
                    "Please enter a RFID tag or plate number.",
                    "Input Required", JOptionPane.WARNING_MESSAGE);
                rfidField.requestFocus();
                return;
            }

            try {
                Optional<Vehicle> found = vehicleDAO.findByPlateNumber(plateInput);
                if (found.isEmpty()) {
                    DialogUtil.showMessageDialog(this, "No active vehicle found for: " + plateInput,
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                    rfidField.setText("");
                    rfidField.requestFocus();
                    return;
                }
                Vehicle vehicle = found.get();

                List<ParkingTransaction> activeTxList = txDAO.findByVehicleId(vehicle.getVehicleId());
                boolean alreadyParked = activeTxList.stream()
                    .anyMatch(t -> "IN_PROGRESS".equals(t.getTransactionStatus()));
                if (alreadyParked) {
                    Optional<ParkingSlot> cur = slotDAO.findByVehicleId(vehicle.getVehicleId());
                    String curCode = cur.map(ParkingSlot::getSlotCode).orElse("unknown slot");
                    DialogUtil.showMessageDialog(this,
                        "Vehicle " + plateInput + " is already parked at slot " + curCode + ".\n"
                        + "Please record an exit first before recording a new entry.",
                        "Already Parked", JOptionPane.WARNING_MESSAGE);
                    rfidField.setText("");
                    rfidField.requestFocus();
                    return;
                }

                if (slot.isOccupied()) {
                    DialogUtil.showMessageDialog(this, "Slot " + slotCode + " is already Occupied.",
                        "Slot Unavailable", JOptionPane.WARNING_MESSAGE);
                    rfidField.setText("");
                    rfidField.requestFocus();
                    return;
                }

                vehicleDAO.updateParkingStatus(vehicle.getVehicleId(), "Parked");
                slotDAO.occupySlot(slot.getSlotId());
                slotDAO.updateCurrentVehicle(slot.getSlotId(), vehicle.getVehicleId());
                txDAO.create(new ParkingTransaction(vehicle.getVehicleId(), slot.getSlotId(), LocalDateTime.now()));

                AuditLog log = new AuditLog();
                log.setAction("ENTRY"); log.setEntityType("PARKING_SLOT");
                log.setEntityId(slot.getSlotId()); log.setNewValue(plateInput);
                log.setOldValue(slotCode); log.setIpAddress("localhost");
                auditDAO.create(log);

                state.notifySlotChange();
                DialogUtil.showMessageDialog(this,
                    "Entry recorded — " + plateInput + " assigned to slot " + slotCode + ".",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (Exception ex) {
                ex.printStackTrace();
                DialogUtil.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ── EXIT button ───────────────────────────────────────────────────────
        JButton exitBtn = new JButton("RECORD EXIT") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? exitRed.darker() : exitRed);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        exitBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        exitBtn.setForeground(Color.WHITE);
        exitBtn.setContentAreaFilled(false);
        exitBtn.setBorderPainted(false);
        exitBtn.setFocusPainted(false);
        exitBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exitBtn.setPreferredSize(new Dimension(320, 42));
        exitBtn.setVisible(isSlotOccupied);

        exitBtn.addActionListener(e -> {
            String plateInput = rfidField.getText().trim();

            if (plateInput.isEmpty()) {
                DialogUtil.showMessageDialog(this,
                    "Please enter a RFID tag or plate number.",
                    "Input Required", JOptionPane.WARNING_MESSAGE);
                rfidField.requestFocus();
                return;
            }

            try {
                Optional<Vehicle> found = vehicleDAO.findByPlateNumber(plateInput);
                if (found.isEmpty()) {
                    DialogUtil.showMessageDialog(this, "No vehicle found for: " + plateInput,
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                    rfidField.setText("");
                    rfidField.requestFocus();
                    return;
                }
                Vehicle vehicle = found.get();

                // ── KEY FIX: Verify vehicle is in the clicked slot ──────────────
                Optional<ParkingSlot> vehicleSlotOpt = slotDAO.findByVehicleId(vehicle.getVehicleId());
                if (vehicleSlotOpt.isEmpty() || vehicleSlotOpt.get().getSlotId() != slot.getSlotId()) {
                    DialogUtil.showMessageDialog(this,
                        "Vehicle " + plateInput + " is not parked in slot " + slotCode + ".\n"
                        + "The vehicle in this slot is different.",
                        "Wrong Vehicle", JOptionPane.WARNING_MESSAGE);
                    rfidField.setText("");
                    rfidField.requestFocus();
                    return;
                }

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

                AuditLog log = new AuditLog();
                log.setAction("EXIT"); log.setEntityType("PARKING_SLOT");
                log.setEntityId(slot.getSlotId()); log.setNewValue(plateInput);
                log.setOldValue(slotCode); log.setIpAddress("localhost");
                auditDAO.create(log);

                state.notifySlotChange();
                DialogUtil.showMessageDialog(this,
                    "Exit recorded — " + plateInput + " has left slot " + slotCode + ".",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (Exception ex) {
                ex.printStackTrace();
                DialogUtil.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonContainer.add(entryBtn);
        buttonContainer.add(exitBtn);
        body.add(buttonContainer, BorderLayout.SOUTH);

        // ── Enter key listener for quick submit ───────────────────────────────
        rfidField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (isSlotOccupied) {
                        exitBtn.doClick();
                    } else {
                        entryBtn.doClick();
                    }
                }
            }
        });

        root.add(body, BorderLayout.CENTER);
        setContentPane(root);
        pack();
        setSize(420, 220);
        setLocationRelativeTo(parent);
    }
}
