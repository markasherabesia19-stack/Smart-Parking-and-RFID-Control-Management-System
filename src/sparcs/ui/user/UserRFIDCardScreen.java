package ui.user;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import dao.VehicleOwnerDAO;
import model.AppState;
import model.VehicleOwner;
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
import java.util.Optional;

public class UserRFIDCardScreen {

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "USER", "USER_RFID_CARD"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("MY RFID CARD", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        // Center: card display
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(C_BG_DARK);
        content.add(center, BorderLayout.CENTER);

        // Load owner info using the logged-in user's ID from AppState
        VehicleOwner owner = null;
        try {
            VehicleOwnerDAO ownerDAO = new VehicleOwnerDAO();
            model.UserAccount currentUser = state.getCurrentUserAccount();
            if (currentUser != null) {
                int loggedInUserId = currentUser.getUserId();
                Optional<VehicleOwner> opt = ownerDAO.findAll().stream()
                        .filter(o -> o.getUserId() != null && o.getUserId() == loggedInUserId)
                        .findFirst();
                if (opt.isPresent()) owner = opt.get();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        // Card panel
        JPanel card = UIFactory.cardPanel(new GridBagLayout());
        card.setPreferredSize(new Dimension(420, 480));
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

        // Divider
        cc.gridy = 2; cc.insets = new Insets(0, 0, 16, 0);
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 40));
        card.add(sep, cc);

        // User info
        model.UserAccount currentUser = state.getCurrentUserAccount();
        String username = currentUser != null ? currentUser.getUsername() : state.currentUsername;
        String email    = currentUser != null && currentUser.getEmail() != null
                          ? currentUser.getEmail() : "N/A";

        // Full name: prefer user_account.full_name, fallback to username
        String fullName = (currentUser != null
                          && currentUser.getFullName() != null
                          && !currentUser.getFullName().isBlank())
                          ? currentUser.getFullName()
                          : username;

        cc.insets = new Insets(4, 0, 4, 0);

        cc.gridy = 3;
        card.add(infoRow("FULL NAME",  fullName),  cc);
        cc.gridy = 4;
        card.add(infoRow("USERNAME",   username),  cc);
        cc.gridy = 5;
        card.add(infoRow("EMAIL",      email),     cc);

        // Divider
        cc.gridy = 6; cc.insets = new Insets(16, 0, 16, 0);
        JSeparator sep2 = new JSeparator();
        sep2.setForeground(new Color(255, 255, 255, 40));
        card.add(sep2, cc);

        // Barcode — encodes: SPARCS - <username>
        cc.gridy = 7; cc.insets = new Insets(0, 0, 8, 0);
        String rawData = "SPARCS - " + username;
        String barcodeData = rawData.replaceAll("[^\\x00-\\x7F]", "");
        JLabel barcodeLbl = new JLabel();
        barcodeLbl.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            BufferedImage barcodeImg = generateBarcode(barcodeData, 340, 80);
            barcodeLbl.setIcon(new ImageIcon(barcodeImg));
        } catch (WriterException ex) {
            barcodeLbl.setText("Barcode generation failed.");
            barcodeLbl.setForeground(C_MUTED);
        }
        card.add(barcodeLbl, cc);

        // Barcode text
        cc.gridy = 8; cc.insets = new Insets(0, 0, 16, 0);
        JLabel barcodeText = UIFactory.lbl(barcodeData, Font.PLAIN, 9, C_MUTED);
        barcodeText.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(barcodeText, cc);

        // Save button
        cc.gridy = 9; cc.insets = new Insets(8, 0, 0, 0);
        JButton saveBtn = UIFactory.gradientButton("SAVE CARD AS IMAGE");
        saveBtn.addActionListener(e -> saveCardAsImage(card, username));
        card.add(saveBtn, cc);

        center.add(card);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    /** Creates a labeled info row */
    private static JPanel infoRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        JLabel lbl = UIFactory.lbl(label, Font.BOLD, 10, C_MUTED);
        lbl.setPreferredSize(new Dimension(90, 20));
        JLabel val = UIFactory.lbl(value, Font.PLAIN, 13, C_WHITE);
        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    /** Generates a CODE_128 barcode image */
    private static BufferedImage generateBarcode(String data, int width, int height) throws WriterException {
        Code128Writer writer = new Code128Writer();
        BitMatrix matrix = writer.encode(data, BarcodeFormat.CODE_128, width, height);
        BufferedImage raw = MatrixToImageWriter.toBufferedImage(matrix);

        // Invert colors to match dark theme (white bars on dark background)
        BufferedImage inverted = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < raw.getWidth(); x++) {
            for (int y = 0; y < raw.getHeight(); y++) {
                int rgb = raw.getRGB(x, y);
                inverted.setRGB(x, y, rgb == 0xFF000000 ? 0xFFFFFFFF : 0xFF1A1040);
            }
        }
        return inverted;
    }

    /** Saves the card panel as a PNG image */
    private static void saveCardAsImage(JPanel card, String username) {
        BufferedImage img = new BufferedImage(card.getWidth(), card.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        card.paint(g2);
        g2.dispose();

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("SPARCS_Card_" + username + ".png"));
        int result = chooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(img, "PNG", chooser.getSelectedFile());
                JOptionPane.showMessageDialog(null, "Card saved successfully!",
                        "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Failed to save: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}