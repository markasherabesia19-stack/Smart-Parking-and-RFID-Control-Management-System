package ui.admin;

import dao.VehicleDAO;
import dao.ParkingSlotDAO;
import dao.ParkingTransactionDAO;
import dao.AuditLogDAO;
import model.AppState;
import model.ParkingSlot;
import model.ParkingTransaction;
import model.Vehicle;
import model.AuditLog;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

        JPanel body = new JPanel(new GridLayout(2, 1, 0, 12));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 20, 20, 20));

        VehicleDAO vehicleDAO = new VehicleDAO();
        ParkingSlotDAO slotDAO = new ParkingSlotDAO();
        ParkingTransactionDAO txDAO = new ParkingTransactionDAO();

        // ── Entry card ────────────────────────────────────────────────────────────
        JPanel entryCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        entryCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        entryCard.add(UIFactory.lbl("RECORD ENTRY", Font.BOLD, 12, C_AVAILABLE), BorderLayout.NORTH);

        JPanel entryForm = new JPanel(new GridLayout(0, 1, 0, 6));
        entryForm.setOpaque(false);
        entryForm.add(UIFactory.lbl("RFID TAG / PLATE", Font.BOLD, 9, C_MUTED));
        JTextField rfidEntry = UIFactory.styledField("Scan or type RFID / plate");
        entryForm.add(rfidEntry);
        entryForm.add(UIFactory.lbl("SLOT NUMBER", Font.BOLD, 9, C_MUTED));
        JTextField slotEntry = UIFactory.styledField("e.g. B-04");
        entryForm.add(slotEntry);
        entryCard.add(entryForm, BorderLayout.CENTER);

        JButton entryBtn = UIFactory.gradientButton("RECORD ENTRY");
        entryBtn.addActionListener(e -> {
            String plateInput = rfidEntry.getText().trim();
            String slotInput  = slotEntry.getText().trim().toUpperCase();

            if (plateInput.isEmpty() || slotInput.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                    "Please enter both a plate number and a slot number.",
                    "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                // 1. Look up the vehicle
                Optional<Vehicle> found = vehicleDAO.findByPlateNumber(plateInput);
                if (found.isEmpty()) {
                    JOptionPane.showMessageDialog(null,
                        "No active vehicle found for: " + plateInput,
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Vehicle vehicle = found.get();
                if ("Parked".equals(vehicle.getParkingStatus())) {
                    JOptionPane.showMessageDialog(null,
                        "Vehicle " + plateInput + " is already marked as Parked.",
                        "Already Parked", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // 2. Look up the slot
                Optional<ParkingSlot> slotOpt = slotDAO.findBySlotCode(slotInput);
                if (slotOpt.isEmpty()) {
                    JOptionPane.showMessageDialog(null,
                        "Slot \"" + slotInput + "\" not found. Check the slot code (e.g. B-04).",
                        "Slot Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                ParkingSlot slot = slotOpt.get();
                if (slot.isOccupied()) {
                    JOptionPane.showMessageDialog(null,
                        "Slot " + slotInput + " is already Occupied.",
                        "Slot Unavailable", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // 3. Update vehicle parking status in DB
                vehicleDAO.updateParkingStatus(vehicle.getVehicleId(), "Parked");

                // 4. Mark slot OCCUPIED in DB and link vehicle to slot
                slotDAO.occupySlot(slot.getSlotId());
                slotDAO.updateCurrentVehicle(slot.getSlotId(), vehicle.getVehicleId());

                // 5. Create a ParkingTransaction (IN_PROGRESS) so the dashboard
                //    can resolve which slot the vehicle is in
                ParkingTransaction tx = new ParkingTransaction(
                    vehicle.getVehicleId(), slot.getSlotId(), LocalDateTime.now());
                txDAO.create(tx);

                // 6. Create audit log entry for this action
                AuditLogDAO auditDAO = new AuditLogDAO();
                AuditLog entryLog = new AuditLog();
                entryLog.setAction("ENTRY");
                entryLog.setEntityType("VEHICLE");
                entryLog.setEntityId(vehicle.getVehicleId());
                entryLog.setNewValue(plateInput);  // Store plate number
                entryLog.setOldValue(slotInput);   // Store slot number
                auditDAO.create(entryLog);

                // 7. Notify all slot-map panels — reloads DB + redraws grids immediately
                state.notifySlotChange();

                // 7. Clear inputs and confirm
                rfidEntry.setText("");
                slotEntry.setText("");
                JOptionPane.showMessageDialog(null,
                    "Entry recorded — " + plateInput + " assigned to slot " + slotInput + ".",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                    "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        entryCard.add(entryBtn, BorderLayout.SOUTH);
        body.add(entryCard);

        // ── Exit card ─────────────────────────────────────────────────────────────
        JPanel exitCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        exitCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        exitCard.add(UIFactory.lbl("RECORD EXIT", Font.BOLD, 12, C_OCCUPIED), BorderLayout.NORTH);

        JPanel exitForm = new JPanel(new GridLayout(0, 1, 0, 6));
        exitForm.setOpaque(false);
        exitForm.add(UIFactory.lbl("RFID TAG / PLATE", Font.BOLD, 9, C_MUTED));
        JTextField rfidExit = UIFactory.styledField("Scan or type RFID / plate");
        exitForm.add(rfidExit);
        exitForm.add(UIFactory.lbl("SLOT NUMBER", Font.BOLD, 9, C_MUTED));
        JTextField slotExit = UIFactory.styledField("e.g. B-04");
        exitForm.add(slotExit);
        exitCard.add(exitForm, BorderLayout.CENTER);

        JButton exitBtn = UIFactory.gradientButton("RECORD EXIT");
        exitBtn.addActionListener(e -> {
            String plateInput = rfidExit.getText().trim();
            String slotInput  = slotExit.getText().trim().toUpperCase();

            if (plateInput.isEmpty() || slotInput.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                    "Please enter both a plate number and a slot number.",
                    "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                // 1. Look up the vehicle
                Optional<Vehicle> found = vehicleDAO.findByPlateNumber(plateInput);
                if (found.isEmpty()) {
                    JOptionPane.showMessageDialog(null,
                        "No active vehicle found for: " + plateInput,
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Vehicle vehicle = found.get();
                if ("Not Parked".equals(vehicle.getParkingStatus())) {
                    JOptionPane.showMessageDialog(null,
                        "Vehicle " + plateInput + " is already marked as Not Parked.",
                        "Already Exited", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // 2. Look up the slot
                Optional<ParkingSlot> slotOpt = slotDAO.findBySlotCode(slotInput);
                if (slotOpt.isEmpty()) {
                    JOptionPane.showMessageDialog(null,
                        "Slot \"" + slotInput + "\" not found. Check the slot code (e.g. B-04).",
                        "Slot Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                ParkingSlot slot = slotOpt.get();

                // 3. Update vehicle parking status to Not Parked
                vehicleDAO.updateParkingStatus(vehicle.getVehicleId(), "Not Parked");

                // 4. Mark slot AVAILABLE in DB and unlink vehicle from slot
                slotDAO.vacateSlot(slot.getSlotId());
                slotDAO.updateCurrentVehicle(slot.getSlotId(), null);

                // 5. Complete the active ParkingTransaction for this vehicle
                List<ParkingTransaction> txList = txDAO.findByVehicleId(vehicle.getVehicleId());
                for (ParkingTransaction tx : txList) {
                    if ("IN_PROGRESS".equals(tx.getTransactionStatus())) {
                        LocalDateTime exitTime = LocalDateTime.now();
                        long minutes = java.time.Duration.between(tx.getEntryTime(), exitTime).toMinutes();
                        tx.setExitTime(exitTime);
                        tx.setDurationMinutes((int) minutes);
                        tx.setTransactionStatus("COMPLETED");
                        txDAO.update(tx);
                        break; // only complete the most recent one
                    }
                }

                // 5. Create audit log entry for this action
                AuditLogDAO auditDAO = new AuditLogDAO();
                AuditLog exitLog = new AuditLog();
                exitLog.setAction("EXIT");
                exitLog.setEntityType("VEHICLE");
                exitLog.setEntityId(vehicle.getVehicleId());
                exitLog.setNewValue(plateInput);   // Store plate number
                exitLog.setOldValue(slotInput);    // Store slot number
                auditDAO.create(exitLog);

                // 6. Notify all slot-map panels to redraw
                state.notifySlotChange();

                rfidExit.setText("");
                slotExit.setText("");
                JOptionPane.showMessageDialog(null,
                    "Exit recorded — " + plateInput + " has left slot " + slotInput + ".",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                    "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        exitCard.add(exitBtn, BorderLayout.SOUTH);
        body.add(exitCard);

        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }
}