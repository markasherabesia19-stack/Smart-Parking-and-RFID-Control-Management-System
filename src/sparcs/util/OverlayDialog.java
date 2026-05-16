package util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import static util.UIConstants.*;

/**
 * OverlayDialog — Beautiful custom modal dialog with frosted glass effect.
 * Replaces standard JOptionPane with a modern overlay panel matching the SPARCS GUI.
 */
public class OverlayDialog extends JPanel {
    private JDialog dialog;
    private int result = -1;
    private JTextField inputField;
    private static final int BORDER_RADIUS = 24;
    private static final int SHADOW_SIZE = 32;

    private OverlayDialog() {
        setOpaque(false);
    }

    /**
     * Shows a message dialog overlay.
     */
    public static void showMessage(Component parent, String message, String title, int messageType) {
        OverlayDialog overlay = new OverlayDialog();
        overlay.createMessageDialog(parent, message, title, messageType);
    }

    /**
     * Shows a confirmation dialog overlay. Returns YES_OPTION or NO_OPTION.
     */
    public static int showConfirm(Component parent, String message, String title) {
        OverlayDialog overlay = new OverlayDialog();
        return overlay.createConfirmDialog(parent, message, title);
    }

    /**
     * Shows an input dialog overlay. Returns the input string or null if cancelled.
     */
    public static String showInput(Component parent, String message, String title) {
        OverlayDialog overlay = new OverlayDialog();
        return overlay.createInputDialog(parent, message, title, "");
    }

    /**
     * Shows an input dialog with initial value.
     */
    public static String showInput(Component parent, String message, String title, String initialValue) {
        OverlayDialog overlay = new OverlayDialog();
        return overlay.createInputDialog(parent, message, title, initialValue != null ? initialValue : "");
    }

    private void createMessageDialog(Component parent, String message, String title, int messageType) {
        JPanel contentPanel = buildMessagePanel(message, messageType);
        
        JButton okBtn = buildGradientButton("OK", e -> {
            dialog.dispose();
        });

        showDialog(parent, title, contentPanel, okBtn, null, null);
    }

    private int createConfirmDialog(Component parent, String message, String title) {
        JPanel contentPanel = buildMessagePanel(message, JOptionPane.QUESTION_MESSAGE);
        
        JButton yesBtn = buildGradientButton("YES", e -> {
            result = JOptionPane.YES_OPTION;
            dialog.dispose();
        });

        JButton noBtn = buildOutlineButton("NO", e -> {
            result = JOptionPane.NO_OPTION;
            dialog.dispose();
        });

        showDialog(parent, title, contentPanel, yesBtn, noBtn, null);
        return result;
    }

    private String createInputDialog(Component parent, String message, String title, String initialValue) {
        JPanel contentPanel = new JPanel(new BorderLayout(0, 14));
        contentPanel.setOpaque(false);

        JLabel msgLabel = new JLabel(message);
        msgLabel.setForeground(C_WHITE);
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentPanel.add(msgLabel, BorderLayout.NORTH);

        inputField = new JTextField(initialValue);
        inputField.setBackground(new Color(255, 255, 255, 12));
        inputField.setForeground(C_WHITE);
        inputField.setCaretColor(C_MUTED);
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_INPUT_BD, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        inputField.setPreferredSize(new Dimension(300, 36));
        contentPanel.add(inputField, BorderLayout.CENTER);

        JButton okBtn = buildGradientButton("OK", e -> {
            result = JOptionPane.OK_OPTION;
            dialog.dispose();
        });

        JButton cancelBtn = buildOutlineButton("CANCEL", e -> {
            result = JOptionPane.CANCEL_OPTION;
            dialog.dispose();
        });

        showDialog(parent, title, contentPanel, okBtn, cancelBtn, null);
        return result == JOptionPane.OK_OPTION ? inputField.getText() : null;
    }

    private JPanel buildMessagePanel(String message, int messageType) {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setOpaque(false);

        // Icon based on message type
        JLabel iconLabel = getIconLabel(messageType);
        if (iconLabel != null) {
            panel.add(iconLabel, BorderLayout.WEST);
        }

        JLabel msgLabel = new JLabel("<html><p style=\"font-family: Segoe UI; width: 280px;\">" 
            + message.replace("\n", "<br>") + "</p></html>");
        msgLabel.setForeground(C_WHITE);
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msgLabel.setVerticalAlignment(JLabel.TOP);
        panel.add(msgLabel, BorderLayout.CENTER);

        return panel;
    }

    private JLabel getIconLabel(int messageType) {
        String icon = null;
        Color color = C_WHITE;
        
        switch (messageType) {
            case JOptionPane.INFORMATION_MESSAGE:
                icon = "\u25CF"; color = C_AVAILABLE; break;
            case JOptionPane.WARNING_MESSAGE:
                icon = "\u26A0"; color = new Color(255, 165, 50); break;
            case JOptionPane.ERROR_MESSAGE:
                icon = "\u25CF"; color = new Color(220, 70, 90); break;
            case JOptionPane.QUESTION_MESSAGE:
                icon = "?"; color = C_PURPLE; break;
        }
        
        if (icon != null) {
            JLabel lbl = new JLabel(icon);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 28));
            lbl.setForeground(color);
            lbl.setPreferredSize(new Dimension(40, 40));
            lbl.setHorizontalAlignment(JLabel.CENTER);
            lbl.setVerticalAlignment(JLabel.TOP);
            return lbl;
        }
        return null;
    }

    private JButton buildGradientButton(String text, ActionListener action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2.setColor(new Color(107, 99, 201));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(147, 139, 241));
                } else {
                    g2.setColor(C_PURPLE);
                }
                
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                super.paintComponent(g);
            }
            
            @Override
            protected void paintBorder(Graphics g) {
                // No border
            }
        };
        
        btn.setForeground(C_WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 28, 10, 28));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(false);
        btn.addActionListener(action);
        
        return btn;
    }

    private JButton buildOutlineButton(String text, ActionListener action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isRollover()) {
                    g2.setColor(new Color(127, 119, 221, 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(C_PURPLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                
                super.paintComponent(g);
            }
            
            @Override
            protected void paintBorder(Graphics g) {
                // Border painted in paintComponent
            }
        };
        
        btn.setForeground(C_PURPLE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 28, 10, 28));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(false);
        btn.addActionListener(action);
        
        return btn;
    }

    private void showDialog(Component parent, String title, JPanel contentPanel, 
                           JButton btn1, JButton btn2, JButton btn3) {
        dialog = new JDialog();
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModal(true);
        dialog.setBackground(new Color(0, 0, 0, 0)); // Transparent window

        // Semi-transparent dark backdrop
        JPanel backdropPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backdropPanel.setLayout(new GridBagLayout());

        // Create main dialog panel with gradient and frosted effect
        JPanel mainPanel = new JPanel(new BorderLayout(0, 18)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw rounded background with gradient
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(45, 27, 110),
                    0, getHeight(), new Color(35, 20, 85)
                );
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, BORDER_RADIUS, BORDER_RADIUS);
                
                // Draw subtle border with purple glow
                g2.setColor(new Color(175, 169, 236, 80));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, BORDER_RADIUS, BORDER_RADIUS);
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        // Title with better styling
        if (title != null && !title.isEmpty()) {
            JLabel titleLabel = new JLabel(title);
            titleLabel.setForeground(C_WHITE);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            mainPanel.add(titleLabel, BorderLayout.NORTH);
        }

        // Content
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Buttons with better layout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        if (btn3 != null) buttonPanel.add(btn3);
        if (btn2 != null) buttonPanel.add(btn2);
        if (btn1 != null) buttonPanel.add(btn1);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Add main panel to backdrop
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        backdropPanel.add(mainPanel, gbc);

        dialog.add(backdropPanel);
        dialog.setSize(460, 220);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
