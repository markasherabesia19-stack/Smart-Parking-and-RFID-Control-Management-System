package ui.admin;

import dao.VehicleDAO;
import dao.VehicleOwnerDAO;
import dao.UserAccountDAO;
import dao.ParkingSlotDAO;
import dao.AuditLogDAO;
import dao.ParkingTransactionDAO;
import model.AppState;
import model.Vehicle;
import model.VehicleOwner;
import model.UserAccount;
import model.ParkingSlot;
import model.AuditLog;
import java.time.format.DateTimeFormatter;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import model.ParkingTransaction;

public class AdminDashboardScreen {

    private static DefaultTableModel vehiclesModel;
    private static Runnable registeredListener = null;

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.setOpaque(true);
        root.setName("ADMIN_DASHBOARD");
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_DASHBOARD"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);
        content.setOpaque(true);

        // Top bar — Layer 1 (#12103A) with bottom separator, 54px tall
        JPanel topBar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(18, 16, 58));                    // #12103A Layer 1
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(30, 28, 69));                    // #1E1C45 separator
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        topBar.setOpaque(false);
        topBar.setPreferredSize(new Dimension(0, 54));
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        // Page Title: 22px 700 ALL CAPS #F0ECFF per design system §2
        topBar.add(UIFactory.lbl("DASHBOARD", Font.BOLD, 22, new Color(240, 236, 255)), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        // Stats row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(20, 20, 12, 20));
        buildStatsRow(statsRow, state);

        // Body
        JPanel body = new JPanel(new GridLayout(1, 2, 16, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 20, 20, 20));

        // Vehicles Overview 
        JPanel vehiclesCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        vehiclesCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        vehiclesCard.add(UIFactory.lbl("VEHICLES OVERVIEW", Font.BOLD, 14, new Color(232, 228, 255)), BorderLayout.NORTH);
        if (vehiclesModel == null) {
            String[] vCols = {"Plate", "Username", "Slot"};
            vehiclesModel = new DefaultTableModel(new Object[0][3], vCols) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
        }

        JTable vehiclesTable = new JTable(vehiclesModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                c.setBackground(isRowSelected(row)
                    ? new Color(36, 30, 107)                            // #241E6B selected
                    : new Color(26, 22, 80));                           // #1A1650 base
                if (col == 0) {
                    // Plate — monospace #7C5CBF per §3.8
                    c.setFont(new Font("Monospaced", Font.BOLD, 12));
                    c.setForeground(new Color(124, 92, 191));
                } else {
                    c.setFont(new Font("SansSerif", Font.PLAIN, 13));
                    c.setForeground(new Color(196, 191, 237));          // #C4BFED
                }
                return c;
            }
        };
        styleOverviewTable(vehiclesTable);

        JScrollPane vehiclesScroll = new JScrollPane(vehiclesTable);
        vehiclesScroll.setOpaque(false);
        vehiclesScroll.setViewportBorder(null);
        vehiclesScroll.getViewport().setBackground(new Color(26, 22, 80));   // #1A1650
        vehiclesScroll.setBorder(BorderFactory.createLineBorder(new Color(45, 40, 96))); // #2D2860 C_INPUT_BD

        vehiclesCard.add(vehiclesScroll, BorderLayout.CENTER);
        body.add(vehiclesCard);
        reloadParkedVehicles(vehiclesModel, state);
        
        if (registeredListener != null) {
            state.removeSlotChangeListener(registeredListener);
        }
        registeredListener = () -> SwingUtilities.invokeLater(() ->
            reloadParkedVehicles(vehiclesModel, state));
        state.addSlotChangeListener(registeredListener);

        JPanel actCard = UIFactory.cardPanel(new BorderLayout(0, 10));
        actCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        actCard.add(UIFactory.lbl("RECENT ACTIVITY", Font.BOLD, 14, new Color(232, 228, 255)), BorderLayout.NORTH);

        // Use a JTable for proper column/row structure
        String[] actCols = {"Plate", "Action", "Slot", "Time"};
        DefaultTableModel actModel = new DefaultTableModel(new Object[0][4], actCols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable actTable = new JTable(actModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                c.setBackground(isRowSelected(row)
                    ? new Color(36, 30, 107)                            // #241E6B selected
                    : new Color(26, 22, 80));                           // #1A1650 base
                if (col == 0) {
                    // Plate — monospace #7C5CBF per §3.8
                    c.setFont(new Font("Monospaced", Font.BOLD, 12));
                    c.setForeground(new Color(124, 92, 191));
                } else if (col != 1) {
                    // Slot + Time — body text
                    c.setFont(new Font("SansSerif", Font.PLAIN, 13));
                    c.setForeground(new Color(196, 191, 237));          // #C4BFED
                }
                return c;
            }
        };
        // Action column — proper badge component per §3.2, replaces plain colored text
        actTable.getColumnModel().getColumn(1).setCellRenderer((t, value, isSelected, hasFocus, row, col) -> {
            final String action = value != null ? value.toString() : "";
            return new JPanel() {
                { setOpaque(true); }
                @Override protected void paintComponent(Graphics g) {
                    // Row bg
                    g.setColor(t.isRowSelected(row)
                        ? new Color(36, 30, 107)
                        : new Color(26, 22, 80));
                    g.fillRect(0, 0, getWidth(), getHeight());

                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Badge colors per §3.2
                    Color bg, fg;
                    switch (action) {
                        case "ENTRY"   -> { bg = new Color(29,185,84,31);  fg = new Color(29,185,84);  }
                        case "EXIT"    -> { bg = new Color(232,54,93,31);  fg = new Color(232,54,93);  }
                        case "RESERVE" -> { bg = new Color(245,197,24,31); fg = new Color(245,197,24); }
                        default        -> { bg = new Color(79,142,247,31); fg = new Color(79,142,247); }
                    }

                    Font f = new Font("SansSerif", Font.BOLD, 11);
                    g2.setFont(f);
                    FontMetrics fm = g2.getFontMetrics(f);
                    int tw = fm.stringWidth(action);
                    int ph = 20, pw = tw + 20;
                    int px = 10, py = (getHeight() - ph) / 2;

                    g2.setColor(bg);
                    g2.fillRoundRect(px, py, pw, ph, 6, 6);
                    g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 80));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(px, py, pw - 1, ph - 1, 6, 6);
                    g2.setColor(fg);
                    g2.drawString(action, px + (pw - tw) / 2, py + (ph - fm.getHeight()) / 2 + fm.getAscent());
                    g2.dispose();
                }
            };
        });
        styleOverviewTable(actTable);

        JScrollPane actScroll = new JScrollPane(actTable);
        actScroll.setOpaque(false);
        actScroll.setViewportBorder(null);
        actScroll.getViewport().setBackground(new Color(26, 22, 80));        // #1A1650
        actScroll.setBorder(BorderFactory.createLineBorder(new Color(45, 40, 96))); // #2D2860 C_INPUT_BD
        actCard.add(actScroll, BorderLayout.CENTER);
        body.add(actCard);

        // Initial load of recent activity
        reloadRecentActivity(actModel);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(statsRow, BorderLayout.NORTH);
        main.add(body, BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        // ── Auto-refresh stats (mirrors AdminSlotMapScreen pattern) ──────────
        Runnable refreshStats = () -> {
            statsRow.removeAll();
            buildStatsRow(statsRow, state);
            statsRow.revalidate();
            statsRow.repaint();
        };

        // Path 1: user navigates to this screen
        root.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                state.loadSlotDataFromDB();
                refreshStats.run();
                reloadParkedVehicles(vehiclesModel, state);
                reloadRecentActivity(actModel);
            }
        });

        // Path 2: entry/exit fires notifySlotChange()
        state.addSlotChangeListener(() -> SwingUtilities.invokeLater(() -> {
            state.loadSlotDataFromDB();
            refreshStats.run();
            reloadRecentActivity(actModel);
        }));

        return root;
    }

    private static void buildStatsRow(JPanel statsRow, AppState state) {
        // §3.5 Metric Cards with top accent bar — mirroring AdminSlotMapScreen.accentStatCard()
        statsRow.add(accentStatCard("AVAILABLE SLOTS", String.valueOf(state.availableSlots), new Color(29, 185, 84)));   // #1DB954
        statsRow.add(accentStatCard("OCCUPIED",        String.valueOf(state.occupiedSlots),  new Color(232, 54, 93)));   // #E8365D
        statsRow.add(accentStatCard("REVENUE TODAY",   calculateRevenue(),                    new Color(168, 85, 247)));  // #A855F7
        statsRow.add(accentStatCard("PENDING FEES",    calculatePendingFees(),                new Color(255, 140, 66)));  // #FF8C42
    }

    /**
     * Stat card with a thin colored accent bar on top — copied from AdminSlotMapScreen.
     * Layout: [accent bar 4px] / [value + label body]
     */
    private static JPanel accentStatCard(String label, String value, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Card background
                g2.setColor(C_BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                // Accent bar — top 3px, rounded only on top corners
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, getWidth(), 6, 10, 10);
                g2.fillRect(0, 3, getWidth(), 3); // square off bottom half of accent
                g2.dispose();
            }
        };
        card.setOpaque(false);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 16, 14, 16));

        JLabel valLbl = UIFactory.lbl(value, Font.BOLD, 22, accentColor);
        valLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLbl = UIFactory.lbl(label.toUpperCase(), Font.BOLD, 11, C_MUTED);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        body.add(valLbl);
        body.add(Box.createVerticalStrut(4));
        body.add(nameLbl);

        card.add(Box.createVerticalStrut(4), BorderLayout.NORTH); // space for accent bar
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private static String calculateRevenue() {
        try {
            ParkingTransactionDAO txDAO = new ParkingTransactionDAO();
            List<ParkingTransaction> paidTxs = txDAO.findByPaymentStatus("PAID");
            int totalRevenue = 0;
            for (ParkingTransaction tx : paidTxs) {
                if (tx.getCalculatedFee() != null) {
                    totalRevenue += tx.getCalculatedFee().intValue();
                }
            }
            return "P" + totalRevenue;
        } catch (Exception e) {
            e.printStackTrace();
            return "0";
        }
    }

    private static String calculatePendingFees() {
        try {
            ParkingTransactionDAO txDAO = new ParkingTransactionDAO();
            List<ParkingTransaction> pendingTxs = txDAO.findInProgress();
            return String.valueOf(pendingTxs.size());
        } catch (Exception e) {
            e.printStackTrace();
            return "0";
        }
    }

    private static void reloadParkedVehicles(DefaultTableModel model, AppState state) {
        model.setRowCount(0);
        try {
            VehicleDAO vehicleDAO         = new VehicleDAO();
            VehicleOwnerDAO ownerDAO      = new VehicleOwnerDAO();
            UserAccountDAO userAccountDAO = new UserAccountDAO();
            ParkingSlotDAO slotDAO        = new ParkingSlotDAO();

            List<Vehicle> vehicles = vehicleDAO.findAll();
            for (Vehicle v : vehicles) {
                if (!"Parked".equalsIgnoreCase(v.getParkingStatus())) continue;

                String username = "—";
                try {
                    Optional<VehicleOwner> owner = ownerDAO.findById(v.getOwnerId());
                    if (owner.isPresent() && owner.get().getUserId() != null) {
                        Optional<UserAccount> account = userAccountDAO.findById(owner.get().getUserId());
                        username = account.map(UserAccount::getUsername).orElse("—");
                    }
                } catch (Exception ex) { /* keep default */ }

                // Look up slot directly via current_vehicle_id column
                String slotCode = "—";
                try {
                    Optional<ParkingSlot> slot = slotDAO.findByVehicleId(v.getVehicleId());
                    if (slot.isPresent()) slotCode = slot.get().getSlotCode();
                } catch (Exception ex) { /* keep default */ }

                model.addRow(new Object[]{ v.getPlateNumber(), username, slotCode });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void styleOverviewTable(JTable table) {
        // Mirror exactly how AdminFeesScreen.styleTable() works:
        // horizontal lines only via setGridColor — no custom cell border renderer needed
        table.setBackground(new Color(26, 22, 80));              // #1A1650 C_BG_ROW
        table.setForeground(new Color(196, 191, 237));           // #C4BFED C_BODY
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setRowHeight(44);
        table.setGridColor(new Color(30, 28, 69));               // #1E1C45 C_BORDER_SUBTLE
        table.setShowHorizontalLines(true);                      // subtle row separators only
        table.setShowVerticalLines(false);                       // no vertical column lines
        table.setSelectionBackground(new Color(36, 30, 107));    // #241E6B
        table.setSelectionForeground(new Color(240, 236, 255));  // #F0ECFF
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setOpaque(true);
        table.setFillsViewportHeight(true);

        // Body cell renderer — padding + bg, same as Fees bodyRenderer
        DefaultTableCellRenderer bodyRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                if (!isSelected) {
                    setBackground(new Color(26, 22, 80));        // #1A1650
                    setForeground(new Color(196, 191, 237));     // #C4BFED
                }
                return this;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(bodyRenderer);
        }

        // Header — same bg as card so it blends flat, bright bold text, 1px bottom only
        JTableHeader header = table.getTableHeader();
        header.setOpaque(true);
        header.setBackground(new Color(18, 16, 58));             // #12103A C_BG_PANEL
        header.setForeground(new Color(232, 228, 255));          // #E8E4FF C_TEXT_SECONDARY
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(45, 40, 96))); // #2D2860 C_INPUT_BD
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                setText(value != null ? value.toString().toUpperCase() : "");
                setFont(new Font("SansSerif", Font.BOLD, 13));
                setForeground(new Color(232, 228, 255));         // #E8E4FF
                setBackground(new Color(18, 16, 58));            // #12103A
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setHorizontalAlignment(SwingConstants.LEFT);
                return this;
            }
        });
    }

    private static void reloadRecentActivity(DefaultTableModel model) {
        model.setRowCount(0);
        try {
            AuditLogDAO auditDAO = new AuditLogDAO();
            List<AuditLog> logs = auditDAO.findAll();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd HH:mm");
            int count = 0;
            for (AuditLog log : logs) {
                if (!"ENTRY".equals(log.getAction()) && !"EXIT".equals(log.getAction())
                        && !"RESERVE".equals(log.getAction())) continue;
                if (count++ >= 10) break;

                // changes_log format: "plate=X slot=Y"
                String plate = "—", slot = "—";
                String raw = log.getOldValue(); // mapped from changes_log in DAO
                if (raw != null) {
                    for (String part : raw.split(" ")) {
                        if (part.startsWith("plate=")) plate = part.substring(6);
                        if (part.startsWith("slot="))  slot  = part.substring(5);
                    }
                }

                String time = log.getCreatedAt() != null ? log.getCreatedAt().format(fmt) : "—";
                model.addRow(new Object[]{ plate, log.getAction(), slot, time });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}