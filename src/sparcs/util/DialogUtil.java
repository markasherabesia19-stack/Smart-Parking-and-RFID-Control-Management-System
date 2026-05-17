package util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import static util.UIConstants.*;

public class DialogUtil {

    public static void showMessageDialog(Component parent, String message, String title, int messageType) {
        JDialog dialog = createDialog(parent, title, message, messageType);
        dialog.setVisible(true);
    }

    public static int showConfirmDialog(Component parent, String message, String title, int optionType) {
        int[] result = {JOptionPane.NO_OPTION};
        JDialog dialog = createDialog(parent, title, message, JOptionPane.QUESTION_MESSAGE);

        // Replace OK button with Yes / No
        JPanel card    = (JPanel) dialog.getContentPane();
        JPanel btnPanel = (JPanel) card.getComponent(2);
        btnPanel.removeAll();

        JButton noBtn  = styledButton("No",  false);
        JButton yesBtn = styledButton("Yes", true);
        noBtn .addActionListener(e -> { result[0] = JOptionPane.NO_OPTION;  dialog.dispose(); });
        yesBtn.addActionListener(e -> { result[0] = JOptionPane.YES_OPTION; dialog.dispose(); });
        btnPanel.add(noBtn);
        btnPanel.add(yesBtn);

        dialog.setVisible(true);
        return result[0];
    }

    // ── Core builder ──────────────────────────────────────────────────────────

    private static JDialog createDialog(Component parent, String title, String message, int messageType) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        JDialog dialog = new JDialog(owner instanceof Frame ? (Frame) owner : null, title, true);
        dialog.setUndecorated(true);
        dialog.setSize(380, 210);
        dialog.setLocationRelativeTo(parent);
        dialog.getRootPane().setOpaque(false);
        dialog.getRootPane().setBackground(new Color(0, 0, 0, 0));
        dialog.setBackground(new Color(0, 0, 0, 0));

        // ── Card ──────────────────────────────────────────────────────────────
        JPanel card = new JPanel(new BorderLayout(0, 12)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(22, 14, 56));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(new Color(80, 60, 160, 180));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(24, 28, 20, 28));

        // ── Icon + title row ──────────────────────────────────────────────────
        Color  accent   = accentFor(messageType);
        JLabel iconLbl  = new JLabel(glyphFor(messageType));
        iconLbl.setFont(new Font("SansSerif", Font.BOLD, 18));
        iconLbl.setForeground(accent);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLbl.setForeground(new Color(240, 235, 255));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topRow.setOpaque(false);
        topRow.add(iconLbl);
        topRow.add(titleLbl);
        card.add(topRow, BorderLayout.NORTH);   // component index 0

        // ── Message ───────────────────────────────────────────────────────────
        JTextArea msgArea = new JTextArea(message);
        msgArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        msgArea.setForeground(new Color(160, 150, 200));
        msgArea.setOpaque(false);
        msgArea.setEditable(false);
        msgArea.setFocusable(false);
        msgArea.setLineWrap(true);
        msgArea.setWrapStyleWord(true);
        msgArea.setBorder(null);
        card.add(msgArea, BorderLayout.CENTER);  // component index 1

        // ── Buttons ───────────────────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        JButton okBtn = styledButton("OK", true);
        okBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(okBtn);
        card.add(btnPanel, BorderLayout.SOUTH);  // component index 2

        dialog.setContentPane(card);
        return dialog;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static JButton styledButton(String text, boolean primary) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(primary ? new Color(98, 70, 210) : new Color(38, 28, 80));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                if (!primary) {
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.setColor(new Color(80, 60, 160));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(new Color(240, 235, 255));
        btn.setPreferredSize(new Dimension(80, 32));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private static Color accentFor(int messageType) {
        return switch (messageType) {
            case JOptionPane.WARNING_MESSAGE     -> new Color(255, 193,  60);
            case JOptionPane.ERROR_MESSAGE       -> new Color(255,  80,  80);
            case JOptionPane.INFORMATION_MESSAGE -> new Color( 80, 220, 120);
            default                              -> new Color(130, 100, 255);
        };
    }

    private static String glyphFor(int messageType) {
        return switch (messageType) {
            case JOptionPane.WARNING_MESSAGE     -> "⚠";
            case JOptionPane.ERROR_MESSAGE       -> "✕";
            case JOptionPane.INFORMATION_MESSAGE -> "✓";
            default                              -> "?";
        };
    }
}