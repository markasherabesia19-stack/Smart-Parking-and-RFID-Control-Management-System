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
        AuditLogDAO auditDAO = new AuditLogDAO();

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
                DialogUtil.showMessageDialog(null,
                    "Please enter both a plate number and a slot number.",
                    "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                // 1. Look up the vehicle
                Optional<Vehicle> found = vehicleDAO.findByPlateNumber(plateInput);
                if (found.isEmpty()) {
                    DialogUtil.showMessageDialog(null,
                        "No active vehicle found for: " + plateInput,
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Vehicle vehicle = found.get();

                // Guard against double-entry
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

                // 2. Look up the slot
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

                // 3. Update vehicle parking status
                vehicleDAO.updateParkingStatus(vehicle.getVehicleId(), "Parked");

                // 4. Mark slot OCCUPIED and link vehicle
                slotDAO.occupySlot(slot.getSlotId());
                slotDAO.updateCurrentVehicle(slot.getSlotId(), vehicle.getVehicleId());

                // 5. Create IN_PROGRESS transaction
                ParkingTransaction tx = new ParkingTransaction(
                    vehicle.getVehicleId(), slot.getSlotId(), LocalDateTime.now());
                txDAO.create(tx);

                // 6. Write audit log so Recent Activity updates
                // AuditLogDAO.create() builds: "plate=" + newValue + " slot=" + oldValue
                AuditLog entryLog = new AuditLog();
                entryLog.setAction("ENTRY");
                entryLog.setEntityType("PARKING_SLOT");
                entryLog.setEntityId(slot.getSlotId());
                entryLog.setNewValue(plateInput);   // → plate=ABC123
                entryLog.setOldValue(slotInput);    // → slot=B-04
                entryLog.setIpAddress("localhost");
                auditDAO.create(entryLog);

                // 7. Notify slot-map panels
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
        entryCard.add(entryBtn, BorderLayout.SOUTH);
        body.add(entryCard);

        // ── Exit card ─────────────────────────────────────────────────────────────
        JPanel exitCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        exitCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        exitCard.add(UIFactory.lbl("RECORD EXIT", Font.BOLD, 12, C_OCCUPIED), BorderLayout.NORTH);

        JPanel exitForm = new JPanel(new BorderLayout(0, 6));
        exitForm.setOpaque(false);
        exitForm.add(UIFactory.lbl("RFID TAG / PLATE", Font.BOLD, 9, C_MUTED), BorderLayout.NORTH);
        JTextField rfidExit = UIFactory.styledField("Scan or type RFID / plate");
        exitForm.add(rfidExit, BorderLayout.CENTER);
        JTextField slotExit = new JTextField();
        exitCard.add(exitForm, BorderLayout.CENTER);

        JButton exitBtn = UIFactory.gradientButton("RECORD EXIT");
        exitBtn.addActionListener(e -> {
            String plateInput = rfidExit.getText().trim();

            if (plateInput.isEmpty()) {
                DialogUtil.showMessageDialog(null,
                    "Please enter a plate number.",
                    "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                // 1. Look up the vehicle
                Optional<Vehicle> found = vehicleDAO.findByPlateNumber(plateInput);
                if (found.isEmpty()) {
                    DialogUtil.showMessageDialog(null,
                        "No vehicle found for: " + plateInput,
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Vehicle vehicle = found.get();

                // 2. Resolve slot from DB
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

                // 3. Update vehicle parking status
                vehicleDAO.updateParkingStatus(vehicle.getVehicleId(), "Not Parked");

                // 4. Vacate the slot
                slotDAO.vacateSlot(slot.getSlotId());
                slotDAO.updateCurrentVehicle(slot.getSlotId(), null);

                // 5. Complete the active IN_PROGRESS transaction
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

                // 6. Write audit log so Recent Activity updates
                // AuditLogDAO.create() builds: "plate=" + newValue + " slot=" + oldValue
                AuditLog exitLog = new AuditLog();
                exitLog.setAction("EXIT");
                exitLog.setEntityType("PARKING_SLOT");
                exitLog.setEntityId(slot.getSlotId());
                exitLog.setNewValue(plateInput);        // → plate=ABC123
                exitLog.setOldValue(resolvedSlotCode);  // → slot=B-04
                exitLog.setIpAddress("localhost");
                auditDAO.create(exitLog);

                // 7. Notify slot-map panels
                state.notifySlotChange();

                rfidExit.setText("");
                slotExit.setText("");
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
        exitCard.add(exitBtn, BorderLayout.SOUTH);
        body.add(exitCard);

        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }
}