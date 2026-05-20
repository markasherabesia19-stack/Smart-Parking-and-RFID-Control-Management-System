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
import util.DialogUtil;
import static util.UIConstants.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
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

        // ── Top bar ───────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("MY RFID CARDS", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        JLabel subLbl = UIFactory.lbl("Your registered vehicle access cards", Font.PLAIN, 11, C_MUTED);
        topBar.add(subLbl, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // ── Scroll pane ref so we can replace it on reload ────────────────────
        JScrollPane[] scrollRef = new JScrollPane[1];

        Runnable reload = () -> {
            if (scrollRef[0] != null) content.remove(scrollRef[0]);

            List<VehicleWithRFID> entries = loadVehiclesWithRFID(state);

        // ── Centered scrollable wrapper ────────────────────────────────────────
            JPanel cardsContainer = new JPanel();
            cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
            cardsContainer.setOpaque(false);
            cardsContainer.setBorder(new EmptyBorder(32, 0, 32, 0));

        if (entries.isEmpty()) {
            JPanel emptyWrap = new JPanel(new GridBagLayout());
            emptyWrap.setOpaque(false);

            JPanel emptyCard = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(30, 22, 70, 200));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                    g2.setColor(new Color(175, 169, 236, 40));
                    g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 18, 18));
                    g2.dispose();
                }
                @Override public boolean isOpaque() { return false; }
            };
            emptyCard.setLayout(new BoxLayout(emptyCard, BoxLayout.Y_AXIS));
            emptyCard.setPreferredSize(new Dimension(420, 200));
            emptyCard.setBorder(new EmptyBorder(40, 40, 40, 40));

            JLabel icon = UIFactory.lbl("⊘", Font.PLAIN, 40, new Color(127, 119, 221, 100));
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel msg = UIFactory.lbl("No vehicles registered to your account.", Font.PLAIN, 13, C_MUTED);
            msg.setAlignmentX(Component.CENTER_ALIGNMENT);

            emptyCard.add(Box.createVerticalGlue());
            emptyCard.add(icon);
            emptyCard.add(Box.createVerticalStrut(12));
            emptyCard.add(msg);
            emptyCard.add(Box.createVerticalGlue());

            emptyWrap.add(emptyCard);
            cardsContainer.add(emptyWrap);
        } else {
            for (VehicleWithRFID entry : entries) {
                JPanel vehicleCard = buildVehicleCard(entry);
                vehicleCard.setAlignmentX(Component.CENTER_ALIGNMENT);
                cardsContainer.add(vehicleCard);
                cardsContainer.add(Box.createVerticalStrut(28));
            }
        }

        // Center the cards horizontally
            JPanel centerWrapper = new JPanel(new GridBagLayout());
            centerWrapper.setOpaque(false);
            centerWrapper.add(cardsContainer, new GridBagConstraints());

            JScrollPane scroll = new JScrollPane(centerWrapper);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getViewport().setBackground(C_BG_DARK);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            scroll.getVerticalScrollBar().setBackground(C_BG_DARK);

            scrollRef[0] = scroll;
            content.add(scroll, BorderLayout.CENTER);
            content.revalidate();
            content.repaint();
        };

        // ── Reload every time this screen becomes visible ─────────────────────
        root.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) {
                reload.run();
            }
        });

        // Initial load
        reload.run();

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

        // Outer card with gradient header
        JPanel card = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Card background
                g2.setColor(new Color(30, 22, 70, 255));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                // Card border
                g2.setColor(new Color(175, 169, 236, 50));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 20, 20));
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        card.setPreferredSize(new Dimension(500, 520));
        card.setMaximumSize(new Dimension(500, 520));

        // ── Gradient header strip ─────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(105, 48, 195, 230),
                        getWidth(), 0, new Color(210, 50, 140, 200));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + 20, 20, 20);

                // Subtle dot pattern on header
                g2.setColor(new Color(255, 255, 255, 12));
                for (int x = 10; x < getWidth(); x += 18)
                    for (int y = 10; y < getHeight() + 20; y += 18)
                        g2.fillOval(x, y, 3, 3);

                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        header.setPreferredSize(new Dimension(500, 72));
        header.setBorder(new EmptyBorder(0, 24, 0, 24));

        // SPARCS logo area in header
        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 16));
        headerLeft.setOpaque(false);

        // Chip icon
        JPanel chipIcon = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRoundRect(0, 0, 38, 38, 8, 8);
                g2.setColor(new Color(255, 255, 255, 200));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // Chip body
                g2.drawRoundRect(8, 8, 22, 22, 4, 4);
                // Chip pins
                g2.drawLine(4, 13, 8, 13); g2.drawLine(4, 19, 8, 19); g2.drawLine(4, 25, 8, 25);
                g2.drawLine(30, 13, 34, 13); g2.drawLine(30, 19, 34, 19); g2.drawLine(30, 25, 34, 25);
                g2.drawLine(13, 4, 13, 8); g2.drawLine(19, 4, 19, 8); g2.drawLine(25, 4, 25, 8);
                g2.drawLine(13, 30, 13, 34); g2.drawLine(19, 30, 19, 34); g2.drawLine(25, 30, 25, 34);
                // Inner grid
                g2.drawLine(14, 14, 24, 14); g2.drawLine(14, 19, 24, 19); g2.drawLine(14, 24, 24, 24);
                g2.drawLine(14, 14, 14, 24); g2.drawLine(19, 14, 19, 24); g2.drawLine(24, 14, 24, 24);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(38, 38); }
            @Override public boolean isOpaque() { return false; }
        };

        JPanel headerTitles = new JPanel(new GridLayout(2, 1, 0, 2));
        headerTitles.setOpaque(false);
        JLabel sparcsLbl = UIFactory.lbl("SPARCS", Font.BOLD, 18, Color.WHITE);
        JLabel systemLbl = UIFactory.lbl("Smart Parking & RFID Control System", Font.PLAIN, 10,
                new Color(255, 255, 255, 180));
        headerTitles.add(sparcsLbl);
        headerTitles.add(systemLbl);

        headerLeft.add(chipIcon);
        headerLeft.add(headerTitles);

        // Status badge top right
        JPanel statusBadge = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(60, 210, 130, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(60, 210, 130, 120));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        JLabel dot = UIFactory.lbl("\u25CF", Font.PLAIN, 8, C_AVAILABLE);
        JLabel activeLbl = UIFactory.lbl("ACTIVE", Font.BOLD, 10, C_AVAILABLE);
        statusBadge.setPreferredSize(new Dimension(78, 26));
        statusBadge.setLayout(new BorderLayout());
        statusBadge.setBorder(new EmptyBorder(4, 10, 4, 10));
        JPanel dotRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        dotRow.setOpaque(false);
        dotRow.add(dot);
        dotRow.add(activeLbl);
        statusBadge.add(dotRow, BorderLayout.CENTER);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 22));
        headerRight.setOpaque(false);
        headerRight.add(statusBadge);

        header.add(headerLeft, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // ── Card body ─────────────────────────────────────────────────────────
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 28, 24, 28));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0; cc.fill = GridBagConstraints.HORIZONTAL; cc.weightx = 1.0;

        // Vehicle info section label
        cc.gridy = 0; cc.insets = new Insets(0, 0, 10, 0);
        body.add(makeSectionLabel("VEHICLE INFORMATION", C_PURPLE), cc);

        // Info rows in a styled panel
        JPanel infoBox = new JPanel(new GridLayout(0, 1, 0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 6));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(new Color(175, 169, 236, 25));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        infoBox.setBorder(new EmptyBorder(4, 14, 4, 14));
        infoBox.add(infoRow("LICENSE PLATE", vehicle.getPlateNumber(), C_ACCENT));
        infoBox.add(infoRow("VEHICLE TYPE",  vehicle.getVehicleType(), C_WHITE));
        infoBox.add(infoRow("COLOR",
                (vehicle.getColor() != null && !vehicle.getColor().isBlank())
                        ? vehicle.getColor() : "N/A", C_WHITE));
        infoBox.add(infoRow("RFID TAG",      rfidTag, C_PINK));

        cc.gridy = 1; cc.insets = new Insets(0, 0, 18, 0);
        body.add(infoBox, cc);

        // Barcode section label
        cc.gridy = 2; cc.insets = new Insets(0, 0, 10, 0);
        body.add(makeSectionLabel("ACCESS BARCODE", C_PINK), cc);

        // Barcode in styled container
        JPanel barcodeBox = new JPanel(new BorderLayout(0, 6)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 6));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(new Color(175, 169, 236, 25));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        barcodeBox.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel barcodeLbl = new JLabel();
        barcodeLbl.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            barcodeLbl.setIcon(new ImageIcon(generateBarcode(rfidTag, 400, 80)));
        } catch (WriterException ex) {
            barcodeLbl.setText("Barcode generation failed.");
            barcodeLbl.setForeground(C_MUTED);
        }

        JLabel barcodeText = UIFactory.lbl(rfidTag, Font.PLAIN, 9, C_MUTED);
        barcodeText.setHorizontalAlignment(SwingConstants.CENTER);

        barcodeBox.add(barcodeLbl, BorderLayout.CENTER);
        barcodeBox.add(barcodeText, BorderLayout.SOUTH);

        cc.gridy = 3; cc.insets = new Insets(0, 0, 20, 0);
        body.add(barcodeBox, cc);

        // Save button
        cc.gridy = 4; cc.insets = new Insets(0, 0, 0, 0);
        JButton saveBtn = UIFactory.gradientButton("SAVE CARD AS IMAGE");
        body.add(saveBtn, cc);
        saveBtn.addActionListener(e -> saveCardAsImage(card, vehicle.getPlateNumber()));

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

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
        JPanel line = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(175, 169, 236, 30));
                g.fillRect(0, getHeight() / 2, getWidth(), 1);
            }
            @Override public boolean isOpaque() { return false; }
        };
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(bar);
        left.add(UIFactory.lbl(text, Font.BOLD, 10, C_MUTED));
        row.add(left, BorderLayout.WEST);
        row.add(line, BorderLayout.CENTER);
        return row;
    }

    private static JPanel infoRow(String label, String value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 0, 8, 0));

        JLabel lbl = UIFactory.lbl(label, Font.BOLD, 10, C_MUTED);
        lbl.setPreferredSize(new Dimension(110, 20));

        JLabel val = UIFactory.lbl(value, Font.BOLD, 13, valueColor);

        // Subtle separator line between rows
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(lbl, BorderLayout.WEST);
        wrapper.add(val, BorderLayout.CENTER);

        row.add(wrapper, BorderLayout.CENTER);
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
                DialogUtil.showMessageDialog(null, "RFID card saved!", "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                DialogUtil.showMessageDialog(null, "Failed to save: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}