package ui.user;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import dao.RFIDMappingDAO;
import dao.VehicleDAO;
import model.AppState;
import model.RFIDMapping;
import model.Vehicle;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRFIDCardScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "USER", "USER_RFID_CARD"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        // ── Top bar ──────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("MY RFID CARDS", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        // ── Load data ────────────────────────────────────────────────────────
        List<VehicleWithRFID> entries = loadVehiclesWithRFID(state);

        // ── Cards container ──────────────────────────────────────────────────
        JPanel cardsContainer = new JPanel();
        cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
        cardsContainer.setBackground(C_BG_DARK);
        cardsContainer.setBorder(new EmptyBorder(28, 0, 28, 0));

        if (entries.isEmpty()) {
            JPanel emptyWrap = new JPanel(new GridBagLayout());
            emptyWrap.setOpaque(false);
            JLabel msg = UIFactory.lbl("No vehicles registered to your account.", Font.PLAIN, 14, C_MUTED);
            emptyWrap.add(msg);
            cardsContainer.add(emptyWrap);
        } else {
            for (VehicleWithRFID entry : entries) {
                JPanel vehicleCard = buildVehicleCard(entry);
                vehicleCard.setAlignmentX(Component.CENTER_ALIGNMENT);
                cardsContainer.add(vehicleCard);
                cardsContainer.add(Box.createVerticalStrut(28));
            }
        }

        JScrollPane scroll = new JScrollPane(cardsContainer);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        content.add(scroll, BorderLayout.CENTER);

        root.add(content, BorderLayout.CENTER);
        return root;
    }

    // =========================================================================
    // Data loading
    // =========================================================================

    private record VehicleWithRFID(Vehicle vehicle, RFIDMapping mapping) {}

    private static List<VehicleWithRFID> loadVehiclesWithRFID(AppState state) {
        List<VehicleWithRFID> result = new ArrayList<>();
        try {
            model.UserAccount currentUser = state.getCurrentUserAccount();
            if (currentUser == null) return result;

            // Single JOIN query — fetches all vehicles for this user regardless
            // of how many owner rows exist (UNIQUE constraint now prevents duplicates anyway)
            VehicleDAO vehicleDAO = new VehicleDAO();
            List<Vehicle> vehicles = vehicleDAO.findAllByUserId(currentUser.getUserId());

            RFIDMappingDAO rfidDAO = new RFIDMappingDAO();
            for (Vehicle v : vehicles) {
                Optional<RFIDMapping> rfidOpt = rfidDAO.findByVehicleId(v.getVehicleId());
                rfidOpt.ifPresent(rfid -> result.add(new VehicleWithRFID(v, rfid)));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    // =========================================================================
    // Card rendering
    // =========================================================================

    private static JPanel buildVehicleCard(VehicleWithRFID entry) {
        Vehicle     vehicle = entry.vehicle();
        RFIDMapping mapping = entry.mapping();
        String      rfidTag = mapping.getRfidTag();

        JPanel card = UIFactory.cardPanel(new GridBagLayout());
        card.setPreferredSize(new Dimension(480, 520));
        card.setMaximumSize(new Dimension(480, 520));
        card.setBorder(new EmptyBorder(28, 36, 28, 36));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0;
        cc.fill  = GridBagConstraints.HORIZONTAL;
        cc.weightx = 1.0;

        cc.gridy = 0; cc.insets = new Insets(0, 0, 2, 0);
        JLabel titleLbl = UIFactory.lbl("SPARCS", Font.BOLD, 22, C_ACCENT);
        titleLbl.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(titleLbl, cc);

        cc.gridy = 1; cc.insets = new Insets(0, 0, 18, 0);
        JLabel subLbl = UIFactory.lbl("Smart Parking & RFID Control System", Font.PLAIN, 11, C_MUTED);
        subLbl.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(subLbl, cc);

        cc.gridy = 2; cc.insets = new Insets(0, 0, 16, 0);
        card.add(divider(), cc);

        cc.insets = new Insets(5, 0, 5, 0);

        cc.gridy = 3;
        card.add(infoRow("LICENSE PLATE", vehicle.getPlateNumber()), cc);

        cc.gridy = 4;
        card.add(infoRow("VEHICLE TYPE", vehicle.getVehicleType()), cc);

        cc.gridy = 5;
        card.add(infoRow("COLOR",
                (vehicle.getColor() != null && !vehicle.getColor().isBlank())
                        ? vehicle.getColor() : "N/A"), cc);

        cc.gridy = 6;
        card.add(infoRow("RFID TAG", rfidTag), cc);

        cc.gridy = 7; cc.insets = new Insets(14, 0, 14, 0);
        card.add(divider(), cc);

        cc.gridy = 8; cc.insets = new Insets(0, 0, 6, 0);
        JLabel barcodeLbl = new JLabel();
        barcodeLbl.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            barcodeLbl.setIcon(new ImageIcon(generateBarcode(rfidTag, 360, 80)));
        } catch (WriterException ex) {
            barcodeLbl.setText("Barcode generation failed.");
            barcodeLbl.setForeground(C_MUTED);
        }
        card.add(barcodeLbl, cc);

        cc.gridy = 9; cc.insets = new Insets(0, 0, 20, 0);
        JLabel barcodeText = UIFactory.lbl(rfidTag, Font.PLAIN, 9, C_MUTED);
        barcodeText.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(barcodeText, cc);

        cc.gridy = 10; cc.insets = new Insets(0, 0, 0, 0);
        JButton saveBtn = UIFactory.gradientButton("SAVE CARD AS IMAGE");
        saveBtn.addActionListener(e -> saveCardAsImage(card, vehicle.getPlateNumber()));
        card.add(saveBtn, cc);

        return card;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static JSeparator divider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 40));
        return sep;
    }

    private static JPanel infoRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);

        JLabel lbl = UIFactory.lbl(label, Font.BOLD, 10, C_MUTED);
        lbl.setPreferredSize(new Dimension(110, 20));

        JLabel val = UIFactory.lbl(value, Font.PLAIN, 13, C_WHITE);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    private static BufferedImage generateBarcode(String data, int width, int height) throws WriterException {
        BitMatrix matrix = new Code128Writer().encode(data, BarcodeFormat.CODE_128, width, height);
        BufferedImage raw = MatrixToImageWriter.toBufferedImage(matrix);
        BufferedImage inv = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < raw.getWidth(); x++)
            for (int y = 0; y < raw.getHeight(); y++)
                inv.setRGB(x, y, raw.getRGB(x, y) == 0xFF000000 ? 0xFFFFFFFF : 0xFF1A1040);
        return inv;
    }

    private static void saveCardAsImage(JPanel card, String plateNumber) {
        BufferedImage img = new BufferedImage(card.getWidth(), card.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        card.paint(g2);
        g2.dispose();

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("SPARCS_RFID_" + plateNumber + ".png"));
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(img, "PNG", chooser.getSelectedFile());
                JOptionPane.showMessageDialog(null, "RFID card saved!", "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Failed to save: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}