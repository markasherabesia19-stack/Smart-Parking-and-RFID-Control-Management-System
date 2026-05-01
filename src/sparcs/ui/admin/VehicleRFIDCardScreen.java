package ui.admin;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

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

/**
 * Displays the RFID card for a single Vehicle.
 * The barcode renders the rfid_tag stored in rfid_mapping — the DB is the source of truth.
 */
public class VehicleRFIDCardScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state,
                               Vehicle vehicle, RFIDMapping mapping) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.setName("VEHICLE_RFID");
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_REGISTER"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("VEHICLE RFID CARD", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(C_BG_DARK);
        content.add(center, BorderLayout.CENTER);

        JPanel card = UIFactory.cardPanel(new GridBagLayout());
        card.setPreferredSize(new Dimension(420, 500));
        card.setBorder(new EmptyBorder(32, 36, 32, 36));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0; cc.fill = GridBagConstraints.HORIZONTAL;
        cc.insets = new Insets(4, 0, 4, 0);

        // Title
        cc.gridy = 0;
        JLabel titleLbl = UIFactory.lbl("SPARCS", Font.BOLD, 22, C_ACCENT);
        titleLbl.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(titleLbl, cc);

        cc.gridy = 1; cc.insets = new Insets(0, 0, 20, 0);
        JLabel subLbl = UIFactory.lbl("Smart Parking & RFID Control System", Font.PLAIN, 11, C_MUTED);
        subLbl.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(subLbl, cc);

        cc.gridy = 2; cc.insets = new Insets(0, 0, 16, 0);
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 40));
        card.add(sep, cc);

        // Vehicle info
        cc.insets = new Insets(4, 0, 4, 0);
        cc.gridy = 3;
        card.add(infoRow("LICENSE PLATE", vehicle.getPlateNumber()), cc);
        cc.gridy = 4;
        card.add(infoRow("VEHICLE TYPE",  vehicle.getVehicleType()), cc);
        cc.gridy = 5;
        String color = vehicle.getColor() != null && !vehicle.getColor().isBlank()
                       ? vehicle.getColor() : "N/A";
        card.add(infoRow("COLOR", color), cc);

        cc.gridy = 6; cc.insets = new Insets(16, 0, 16, 0);
        JSeparator sep2 = new JSeparator();
        sep2.setForeground(new Color(255, 255, 255, 40));
        card.add(sep2, cc);

        // Barcode — use the rfid_tag from the DB record, not a re-computed string
        String rfidTag = mapping.getRfidTag();

        cc.gridy = 7; cc.insets = new Insets(0, 0, 8, 0);
        JLabel barcodeLbl = new JLabel();
        barcodeLbl.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            barcodeLbl.setIcon(new ImageIcon(generateBarcode(rfidTag, 340, 80)));
        } catch (WriterException ex) {
            barcodeLbl.setText("Barcode generation failed.");
            barcodeLbl.setForeground(C_MUTED);
        }
        card.add(barcodeLbl, cc);

        cc.gridy = 8; cc.insets = new Insets(0, 0, 16, 0);
        JLabel barcodeText = UIFactory.lbl(rfidTag, Font.PLAIN, 9, C_MUTED);
        barcodeText.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(barcodeText, cc);

        cc.gridy = 9; cc.insets = new Insets(8, 0, 6, 0);
        JButton saveBtn = UIFactory.gradientButton("SAVE CARD AS IMAGE");
        saveBtn.addActionListener(e -> saveCardAsImage(card, vehicle.getPlateNumber()));
        card.add(saveBtn, cc);

        cc.gridy = 10; cc.insets = new Insets(0, 0, 0, 0);
        JButton backBtn = UIFactory.outlineButton("BACK TO VEHICLES");
        backBtn.addActionListener(e ->
            cardLayout.show(rootPanel, "ADMIN_VEHICLES")
        );
        card.add(backBtn, cc);

        center.add(card);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private static JPanel infoRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        JLabel lbl = UIFactory.lbl(label, Font.BOLD, 10, C_MUTED);
        lbl.setPreferredSize(new Dimension(100, 20));
        row.add(lbl, BorderLayout.WEST);
        row.add(UIFactory.lbl(value, Font.PLAIN, 13, C_WHITE), BorderLayout.CENTER);
        return row;
    }

    private static BufferedImage generateBarcode(String data, int width, int height) throws WriterException {
        BitMatrix matrix = new Code128Writer().encode(data, BarcodeFormat.CODE_128, width, height);
        BufferedImage raw = MatrixToImageWriter.toBufferedImage(matrix);
        BufferedImage inverted = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < raw.getWidth(); x++)
            for (int y = 0; y < raw.getHeight(); y++) {
                int rgb = raw.getRGB(x, y);
                inverted.setRGB(x, y, rgb == 0xFF000000 ? 0xFFFFFFFF : 0xFF1A1040);
            }
        return inverted;
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
                JOptionPane.showMessageDialog(null, "RFID card saved successfully!", "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Failed to save: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}