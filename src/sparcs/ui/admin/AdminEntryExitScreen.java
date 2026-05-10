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

        // ── Top bar ───────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("ENTRY / EXIT", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        topBar.add(UIFactory.lbl("Record vehicle arrivals and departures", Font.PLAIN, 11, C_MUTED), BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        VehicleDAO vehicleDAO         = new VehicleDAO();
        ParkingSlotDAO slotDAO        = new ParkingSlotDAO();
        ParkingTransactionDAO txDAO   = new ParkingTransactionDAO();
        AuditLogDAO auditDAO          = new AuditLogDAO();

        // ── Body: side by side ────────────────────────────────────────────────
        JPanel body = new JPanel(new GridLayout(1, 2, 16, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(24, 24, 24, 24));

        // ── Colors ────────────────────────────────────────────────────────────
        Color entryGreen  = new Color(15, 110, 86);
        Color entryLight  = new Color(29, 158, 117);
        Color exitRed     = new Color(180, 30, 50);
        Color exitLight   = new Color(220, 60, 80);

        // ── ENTRY card ────────────────────────────────────────────────────────
        JPanel entryCard = UIFactory.cardPanel(new BorderLayout(0, 0));

        entryCard.add(makeHeader("RECORD ENTRY", "Vehicle arrival", entryGreen, entryLight), BorderLayout.NORTH);

        JPanel entryBody = new JPanel(new GridBagLayout());
        entryBody.setOpaque(false);
        entryBody.setBorder(new EmptyBorder(16, 20, 20, 20));
        GridBagConstraints ec = new GridBagConstraints();
        ec.fill = GridBagConstraints.HORIZONTAL; ec.weightx = 1.0; ec.gridx = 0;
        ec.anchor = GridBagConstraints.NORTH;

        ec.gridy = 0; ec.insets = new Insets(0, 0, 12, 0);
        JLabel entryDesc = new JLabel("<html>Scan or type the vehicle's RFID tag or license plate to record arrival.</html>");
        entryDesc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        entryDesc.setForeground(C_MUTED);
        entryBody.add(entryDesc, ec);

        ec.gridy = 1; ec.insets = new Insets(0, 0, 4, 0);
        entryBody.add(makeFieldLabel("RFID TAG / PLATE NUMBER", C_AVAILABLE), ec);
        ec.gridy = 2; ec.insets = new Insets(0, 0, 12, 0);
        JTextField rfidEntry = UIFactory.styledField("Scan or type RFID / plate");
        entryBody.add(rfidEntry, ec);

        ec.gridy = 3; ec.insets = new Insets(0, 0, 4, 0);
        entryBody.add(makeFieldLabel("SLOT NUMBER", C_AVAILABLE), ec);
        ec.gridy = 4; ec.insets = new Insets(0, 0, 16, 0);
        JTextField slotEntry = UIFactory.styledField("e.g. B-04");
        entryBody.add(slotEntry, ec);

        ec.gridy = 5; ec.insets = new Insets(0, 0, 10, 0);
        entryBody.add(makeDivider(), ec);

        ec.gridy = 6; ec.insets = new Insets(0, 0, 10, 0);
        entryBody.add(makeHint("Vehicle will be marked as PARKED", C_AVAILABLE), ec);

        ec.gridy = 7; ec.insets = new Insets(0, 0, 0, 0);
        JButton entryBtn = makeColorButton("RECORD ENTRY", entryGreen);
        entryBody.add(entryBtn, ec);

        // Push content to top
        ec.gridy = 8; ec.weighty = 1.0; ec.insets = new Insets(0,0,0,0);
        entryBody.add(new JPanel() {{ setOpaque(false); }}, ec);

        entryCard.add(entryBody, BorderLayout.CENTER);

        // ── EXIT card ─────────────────────────────────────────────────────────
        JPanel exitCard = UIFactory.cardPanel(new BorderLayout(0, 0));

        exitCard.add(makeHeader("RECORD EXIT", "Vehicle departure", exitRed, exitLight), BorderLayout.NORTH);

        JPanel exitBody = new JPanel(new GridBagLayout());
        exitBody.setOpaque(false);
        exitBody.setBorder(new EmptyBorder(16, 20, 20, 20));
        GridBagConstraints xc = new GridBagConstraints();
        xc.fill = GridBagConstraints.HORIZONTAL; xc.weightx = 1.0; xc.gridx = 0;
        xc.anchor = GridBagConstraints.NORTH;

        xc.gridy = 0; xc.insets = new Insets(0, 0, 12, 0);
        JLabel exitDesc = new JLabel("<html>Scan or type the vehicle's RFID tag or license plate to record departure.</html>");
        exitDesc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        exitDesc.setForeground(C_MUTED);
        exitBody.add(exitDesc, xc);

        xc.gridy = 1; xc.insets = new Insets(0, 0, 4, 0);
        exitBody.add(makeFieldLabel("RFID TAG / PLATE NUMBER", C_OCCUPIED), xc);
        xc.gridy = 2; xc.insets = new Insets(0, 0, 16, 0);
        JTextField rfidExit = UIFactory.styledField("Scan or type RFID / plate");
        exitBody.add(rfidExit, xc);

        xc.gridy = 3; xc.insets = new Insets(0, 0, 10, 0);
        exitBody.add(makeDivider(), xc);

        xc.gridy = 4; xc.insets = new Insets(0, 0, 10, 0);
        exitBody.add(makeHint("Vehicle will be marked as EXITED", C_OCCUPIED), xc);

        xc.gridy = 5; xc.insets = new Insets(0, 0, 0, 0);
        JButton exitBtn = makeColorButton("RECORD EXIT", exitRed);
        exitBody.add(exitBtn, xc);

        // Push content to top
        xc.gridy = 6; xc.weighty = 1.0; xc.insets = new Insets(0,0,0,0);
        exitBody.add(new JPanel() {{ setOpaque(false); }}, xc);

        exitCard.add(exitBody, BorderLayout.CENTER);

        // ── Enter key ─────────────────────────────────────────────────────────
        java.awt.event.KeyAdapter entryEnter = new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) entryBtn.doClick();
            }
        };
        rfidEntry.addKeyListener(entryEnter);
        slotEntry.addKeyListener(entryEnter);
        rfidExit.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) exitBtn.doClick();
            }
        });

        // ── Entry action ──────────────────────────────────────────────────────
        entryBtn.addActionListener(e -> {
            String plateInput = rfidEntry.getText().trim();
            String slotInput  = slotEntry.getText().trim().toUpperCase();

            if (plateInput.isEmpty() || slotInput.isEmpty()) {
                rfidEntry.setText(""); slotEntry.setText("");
                DialogUtil.showMessageDialog(null,
                    "Please enter both a plate number and a slot number.",
                    "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                Optional<Vehicle> found = vehicleDAO.findByPlateNumber(plateInput);
                if (found.isEmpty()) {
                    rfidEntry.setText(""); slotEntry.setText("");
                    DialogUtil.showMessageDialog(null, "No active vehicle found for: " + plateInput,
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Vehicle vehicle = found.get();

                List<ParkingTransaction> activeTxList = txDAO.findByVehicleId(vehicle.getVehicleId());
                boolean alreadyParked = activeTxList.stream()
                    .anyMatch(t -> "IN_PROGRESS".equals(t.getTransactionStatus()));
                if (alreadyParked) {
                    Optional<ParkingSlot> cur = slotDAO.findByVehicleId(vehicle.getVehicleId());
                    String curCode = cur.map(ParkingSlot::getSlotCode).orElse("unknown slot");
                    rfidEntry.setText(""); slotEntry.setText("");
                    DialogUtil.showMessageDialog(null,
                        "Vehicle " + plateInput + " is already parked at slot " + curCode + ".\n"
                        + "Please record an exit first before recording a new entry.",
                        "Already Parked", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Optional<ParkingSlot> slotOpt = slotDAO.findBySlotCode(slotInput);
                if (slotOpt.isEmpty()) {
                    rfidEntry.setText(""); slotEntry.setText("");
                    DialogUtil.showMessageDialog(null,
                        "Slot \"" + slotInput + "\" not found. Check the slot code (e.g. B-04).",
                        "Slot Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                ParkingSlot slot = slotOpt.get();
                if (slot.isOccupied()) {
                    rfidEntry.setText(""); slotEntry.setText("");
                    DialogUtil.showMessageDialog(null, "Slot " + slotInput + " is already Occupied.",
                        "Slot Unavailable", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                vehicleDAO.updateParkingStatus(vehicle.getVehicleId(), "Parked");
                slotDAO.occupySlot(slot.getSlotId());
                slotDAO.updateCurrentVehicle(slot.getSlotId(), vehicle.getVehicleId());
                txDAO.create(new ParkingTransaction(vehicle.getVehicleId(), slot.getSlotId(), LocalDateTime.now()));

                AuditLog log = new AuditLog();
                log.setAction("ENTRY"); log.setEntityType("PARKING_SLOT");
                log.setEntityId(slot.getSlotId()); log.setNewValue(plateInput);
                log.setOldValue(slotInput); log.setIpAddress("localhost");
                auditDAO.create(log);

                state.notifySlotChange();
                rfidEntry.setText(""); slotEntry.setText("");
                DialogUtil.showMessageDialog(null,
                    "Entry recorded — " + plateInput + " assigned to slot " + slotInput + ".",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                rfidEntry.setText(""); slotEntry.setText("");
                ex.printStackTrace();
                DialogUtil.showMessageDialog(null, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ── Exit action ───────────────────────────────────────────────────────
        exitBtn.addActionListener(e -> {
            String plateInput = rfidExit.getText().trim();
            if (plateInput.isEmpty()) {
                rfidExit.setText("");
                DialogUtil.showMessageDialog(null, "Please enter a plate number.",
                    "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                Optional<Vehicle> found = vehicleDAO.findByPlateNumber(plateInput);
                if (found.isEmpty()) {
                    rfidExit.setText("");
                    DialogUtil.showMessageDialog(null, "No vehicle found for: " + plateInput,
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Vehicle vehicle = found.get();

                Optional<ParkingSlot> slotOpt = slotDAO.findByVehicleId(vehicle.getVehicleId());
                if (slotOpt.isEmpty()) {
                    rfidExit.setText("");
                    DialogUtil.showMessageDialog(null,
                        "Vehicle " + plateInput + " is not currently occupying any slot.\nIt may have already exited.",
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

                AuditLog log = new AuditLog();
                log.setAction("EXIT"); log.setEntityType("PARKING_SLOT");
                log.setEntityId(slot.getSlotId()); log.setNewValue(plateInput);
                log.setOldValue(resolvedSlotCode); log.setIpAddress("localhost");
                auditDAO.create(log);

                state.notifySlotChange();
                rfidExit.setText("");
                DialogUtil.showMessageDialog(null,
                    "Exit recorded — " + plateInput + " has left slot " + resolvedSlotCode + ".",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                rfidExit.setText("");
                ex.printStackTrace();
                DialogUtil.showMessageDialog(null, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        body.add(entryCard);
        body.add(exitCard);
        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    // ── Gradient header ───────────────────────────────────────────────────────
    private static JPanel makeHeader(String title, String subtitle, Color dark, Color light) {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, dark, getWidth(), 0, light);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + 20, 18, 18);
                g2.setColor(new Color(255, 255, 255, 10));
                for (int x = 8; x < getWidth(); x += 16)
                    for (int y = 8; y < getHeight() + 20; y += 16)
                        g2.fillOval(x, y, 3, 3);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        header.setPreferredSize(new Dimension(0, 72));
        header.setBorder(new EmptyBorder(0, 20, 0, 20));

        JPanel iconBox = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRoundRect(0, 0, 38, 38, 10, 10);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                boolean isEntry = title.contains("ENTRY");
                if (isEntry) {
                    g2.drawLine(8, 19, 22, 19);
                    g2.drawLine(17, 14, 22, 19); g2.drawLine(17, 24, 22, 19);
                    g2.drawLine(26, 10, 26, 28); g2.drawLine(26, 10, 32, 10);
                    g2.drawLine(26, 28, 32, 28); g2.drawLine(32, 10, 32, 28);
                } else {
                    g2.drawLine(8, 10, 8, 28); g2.drawLine(8, 10, 14, 10);
                    g2.drawLine(8, 28, 14, 28); g2.drawLine(14, 10, 14, 28);
                    g2.drawLine(18, 19, 32, 19);
                    g2.drawLine(27, 14, 32, 19); g2.drawLine(27, 24, 32, 19);
                }
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(38, 38); }
            @Override public boolean isOpaque() { return false; }
        };

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 16));
        left.setOpaque(false);
        left.add(iconBox);
        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(UIFactory.lbl(title, Font.BOLD, 14, Color.WHITE));
        titles.add(UIFactory.lbl(subtitle, Font.PLAIN, 11, new Color(255, 255, 255, 180)));
        left.add(titles);
        header.add(left, BorderLayout.WEST);
        return header;
    }

    // ── Field label with colored bar ──────────────────────────────────────────
    private static JPanel makeFieldLabel(String text, Color accent) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
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
        row.add(bar);
        row.add(UIFactory.lbl(text, Font.BOLD, 10, C_MUTED));
        return row;
    }

    // ── Divider ───────────────────────────────────────────────────────────────
    private static JSeparator makeDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(175, 169, 236, 40));
        return sep;
    }

    // ── Hint row ──────────────────────────────────────────────────────────────
    private static JPanel makeHint(String text, Color dotColor) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        row.add(UIFactory.lbl("●", Font.PLAIN, 10, dotColor));
        row.add(UIFactory.lbl(text, Font.PLAIN, 11, C_MUTED));
        return row;
    }

    // ── Solid colored button ──────────────────────────────────────────────────
    private static JButton makeColorButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(200, 42));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}