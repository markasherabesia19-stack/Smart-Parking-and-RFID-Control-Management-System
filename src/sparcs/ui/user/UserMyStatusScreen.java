package ui.user;

import dao.ParkingSlotDAO;
import dao.ParkingTransactionDAO;
import dao.VehicleDAO;
import service.FeeCalculationService;
import java.math.BigDecimal;
import model.AppState;
import model.ParkingSlot;
import model.ParkingTransaction;
import model.Vehicle;
import ui.shared.SidebarPanel;
import util.UIFactory;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserMyStatusScreen {

    // ── Palette ────────────────────────────────────────────────────────────────
    private static final Color BG_BASE        = C_BG_DARK;                        // purple-dark base
    private static final Color BG_SURFACE     = C_BG_PANEL;                       // purple panel surface
    private static final Color BG_SURFACE_ALT = new Color(0x2A2340);              // lighter purple surface
    private static final Color BORDER_LINE    = new Color(0x3D3060);              // purple-tinted border
    private static final Color TEXT_PRIMARY   = C_WHITE;                          // main text
    private static final Color TEXT_SECONDARY = C_MUTED;                          // muted label
    private static final Color ACCENT_ACTIVE  = C_ACCENT;                         // accent — currently parked
    private static final Color ACCENT_WARN    = new Color(0xF59E0B);              // amber — elapsed time highlight
    private static final Color TAG_ACTIVE_BG  = new Color(0x251A45);              // purple accent tint

    private static final Font FONT_MONO   = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font FONT_LABEL  = new Font("SansSerif", Font.BOLD, 10);
    private static final Font FONT_VALUE  = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font FONT_TITLE  = new Font("SansSerif", Font.BOLD, 20);
    private static final Font FONT_SECTION= new Font("SansSerif", Font.BOLD, 11);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ── Build ──────────────────────────────────────────────────────────────────
    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_BASE);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "USER", "USER_MY_STATUS"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_BASE);

        content.add(buildTopBar(), BorderLayout.NORTH);

        // ── Scroll area ──────────────────────────────────────────────────────
        JPanel sectionsWrapper = new JPanel();
        sectionsWrapper.setLayout(new BoxLayout(sectionsWrapper, BoxLayout.Y_AXIS));
        sectionsWrapper.setBackground(BG_BASE);
        sectionsWrapper.setBorder(new EmptyBorder(24, 24, 24, 24));

        JScrollPane scroll = new JScrollPane(sectionsWrapper);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        content.add(scroll, BorderLayout.CENTER);

        // ── Refresh button (grabbed from topBar via name) ─────────────────────
        JButton refreshBtn = (JButton) ((JPanel) content.getComponent(0))
                .getClientProperty("refreshBtn");

        // Re-wire: build topbar with callback
        content.remove(content.getComponent(0));
        JPanel topBar = buildTopBarWithRef();
        JButton btnRef = (JButton) topBar.getClientProperty("refreshBtn");
        content.add(topBar, BorderLayout.NORTH);

        btnRef.addActionListener(e -> reloadSections(sectionsWrapper, state));

        root.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) {
                btnRef.doClick();
            }
        });

        btnRef.doClick();

        root.add(content, BorderLayout.CENTER);
        return root;
    }

    // ── Top bar ────────────────────────────────────────────────────────────────
    private static JPanel buildTopBar() { return buildTopBarWithRef(); }

    private static JPanel buildTopBarWithRef() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_SURFACE);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_LINE),
                new EmptyBorder(14, 24, 14, 24)
        ));

        // Left: icon dot + title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel dot = new JLabel("●");
        dot.setForeground(ACCENT_ACTIVE);
        dot.setFont(new Font("SansSerif", Font.PLAIN, 10));

        JLabel title = new JLabel("MY PARKING STATUS");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);

        left.add(dot);
        left.add(title);
        bar.add(left, BorderLayout.WEST);

        // Right: refresh button — flat outlined style
        JButton refreshBtn = new JButton("↻  REFRESH") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(ACCENT_ACTIVE.getRed(), ACCENT_ACTIVE.getGreen(),
                            ACCENT_ACTIVE.getBlue(), 20));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 6, 6));
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        refreshBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
        refreshBtn.setForeground(ACCENT_ACTIVE);
        refreshBtn.setBackground(new Color(0, 0, 0, 0));
        refreshBtn.setOpaque(false);
        refreshBtn.setContentAreaFilled(false);
        refreshBtn.setBorder(new CompoundBorder(
                new RoundedLineBorder(ACCENT_ACTIVE, 1, 6),
                new EmptyBorder(6, 14, 6, 14)
        ));
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setPreferredSize(new Dimension(120, 34));

        bar.add(refreshBtn, BorderLayout.EAST);
        bar.putClientProperty("refreshBtn", refreshBtn);
        return bar;
    }

    // ── Section reload ─────────────────────────────────────────────────────────
    private static void reloadSections(JPanel sectionsWrapper, AppState state) {
        sectionsWrapper.removeAll();

        List<TransactionWithDetails> all = loadUserTransactions(state);

        List<TransactionWithDetails> active = new ArrayList<>();
        for (TransactionWithDetails txn : all) {
            if (isCurrentTransaction(txn.transaction())) active.add(txn);
        }

        if (active.isEmpty()) {
            sectionsWrapper.add(buildEmptyState());
        } else {
            addSection(sectionsWrapper, "ACTIVE SESSIONS", active, ACCENT_ACTIVE, TAG_ACTIVE_BG);
        }

        sectionsWrapper.revalidate();
        sectionsWrapper.repaint();
    }

    // ── Empty state ────────────────────────────────────────────────────────────
    private static JPanel buildEmptyState() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);
        outer.setPreferredSize(new Dimension(0, 340));

        // Card container
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(BORDER_LINE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(40, 60, 40, 60));
        card.setMaximumSize(new Dimension(460, Integer.MAX_VALUE));

        // Icon circle
        JLabel icon = new JLabel("P", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TAG_ACTIVE_BG);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icon.setFont(new Font("SansSerif", Font.BOLD, 24));
        icon.setForeground(ACCENT_ACTIVE);
        icon.setPreferredSize(new Dimension(64, 64));
        icon.setMaximumSize(new Dimension(64, 64));
        icon.setOpaque(false);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Heading
        JLabel heading = new JLabel("No Active Sessions");
        heading.setFont(new Font("SansSerif", Font.BOLD, 16));
        heading.setForeground(TEXT_PRIMARY);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Sub-text
        JLabel sub = new JLabel("You are not currently parked anywhere.");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(TEXT_SECONDARY);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub2 = new JLabel("Check your history for past transactions.");
        sub2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub2.setForeground(TEXT_SECONDARY);
        sub2.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(icon);
        card.add(Box.createVerticalStrut(16));
        card.add(heading);
        card.add(Box.createVerticalStrut(8));
        card.add(sub);
        card.add(Box.createVerticalStrut(4));
        card.add(sub2);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        outer.add(card, c);
        return outer;
    }

    // ── Section builder ────────────────────────────────────────────────────────
    private static void addSection(JPanel parent,
                                   String title,
                                   List<TransactionWithDetails> group,
                                   Color accent,
                                   Color tagBg) {

        // Summary bar above cards: pulsing dot + title + count pill
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 3, 0, 0, accent),
                new EmptyBorder(0, 10, 0, 0)
        ));

        // Left: dot + title
        JPanel leftSide = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftSide.setOpaque(false);

        JLabel dot = new JLabel("●");
        dot.setFont(new Font("SansSerif", Font.PLAIN, 9));
        dot.setForeground(accent);

        JLabel sectionLabel = new JLabel(title);
        sectionLabel.setFont(FONT_SECTION);
        sectionLabel.setForeground(TEXT_PRIMARY);

        leftSide.add(dot);
        leftSide.add(sectionLabel);
        header.add(leftSide, BorderLayout.WEST);

        // Count pill
        JLabel pill = new JLabel(group.size() + " vehicle" + (group.size() == 1 ? "" : "s")) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(tagBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(new Font("SansSerif", Font.BOLD, 10));
        pill.setForeground(accent);
        pill.setBorder(new EmptyBorder(3, 10, 3, 10));
        pill.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(pill, BorderLayout.EAST);

        parent.add(header);
        parent.add(Box.createVerticalStrut(16));

        // Single-column layout — cards span full width for richer detail
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);
        col.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        for (TransactionWithDetails txn : group) {
            col.add(buildTransactionCard(txn, accent, tagBg));
            col.add(Box.createVerticalStrut(14));
        }

        parent.add(col);
        parent.add(Box.createVerticalStrut(24));
    }

    // ── Card ───────────────────────────────────────────────────────────────────
    private static JPanel buildTransactionCard(TransactionWithDetails entry,
                                               Color accent,
                                               Color tagBg) {
        ParkingTransaction txn  = entry.transaction();
        Vehicle           veh  = entry.vehicle();
        ParkingSlot       slot = entry.slot();

        // ── Guarantee fee and duration are always resolved before rendering ──
        boolean isInProgress = txn.getExitTime() == null
                && "IN_PROGRESS".equalsIgnoreCase(txn.getTransactionStatus());
        if (isInProgress || txn.getCalculatedFee() == null) {
            try {
                java.math.BigDecimal liveFee = new FeeCalculationService().calculateFee(txn);
                txn.setCalculatedFee(liveFee);
            } catch (Exception ignored) {
                txn.setCalculatedFee(java.math.BigDecimal.ZERO);
            }
        }
        if (isInProgress || (txn.getDurationMinutes() == null && txn.getEntryTime() != null)) {
            long mins = FeeCalculationService.calculateDurationMinutes(
                    txn.getEntryTime(), txn.getExitTime());
            txn.setDurationMinutes((int) mins);
        }

        // Outer card
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(0, 0, 0, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // ── Card header: accent-tinted strip ─────────────────────────────────
        JPanel cardHeader = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(tagBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + 12, 12, 12);
                g2.dispose();
            }
        };
        cardHeader.setOpaque(false);
        cardHeader.setBorder(new EmptyBorder(12, 18, 12, 18));

        // Left: live dot + status label — reflects actual transaction state
        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        headerLeft.setOpaque(false);

        JLabel liveDot = new JLabel("●");
        liveDot.setFont(new Font("SansSerif", Font.PLAIN, 8));
        liveDot.setForeground(accent);

        boolean reserved = "RESERVED".equalsIgnoreCase(txn.getTransactionStatus());
        JLabel liveLabel = new JLabel(reserved ? "SLOT RESERVED" : "CURRENTLY PARKED");
        liveLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        liveLabel.setForeground(accent);

        headerLeft.add(liveDot);
        headerLeft.add(liveLabel);

        // Right: plate number — large and prominent
        JLabel plateLabel = new JLabel(veh.getPlateNumber());
        plateLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        plateLabel.setForeground(TEXT_PRIMARY);

        cardHeader.add(headerLeft, BorderLayout.WEST);
        cardHeader.add(plateLabel, BorderLayout.EAST);
        card.add(cardHeader, BorderLayout.NORTH);

        // ── Card body — two-column detail grid ───────────────────────────────
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 18, 18, 18));

        // ─ Slot / Zone row side by side ─
        JPanel slotRow = new JPanel(new GridLayout(1, 2, 16, 0));
        slotRow.setOpaque(false);
        slotRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        slotRow.add(infoBlock("SLOT", slot.getSlotCode(), accent));
        slotRow.add(infoBlock("ZONE", slot.getZone(), TEXT_SECONDARY));
        body.add(slotRow);
        body.add(Box.createVerticalStrut(12));

        // ─ Vehicle type / Entry side by side ─
        JPanel row2 = new JPanel(new GridLayout(1, 2, 16, 0));
        row2.setOpaque(false);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        String entryStr = txn.getEntryTime() != null ? txn.getEntryTime().format(DATE_FORMAT) : "—";
        row2.add(infoBlock("VEHICLE TYPE", veh.getVehicleType(), TEXT_SECONDARY));
        row2.add(infoBlock("ENTRY TIME",   entryStr,            TEXT_SECONDARY));
        body.add(row2);
        body.add(Box.createVerticalStrut(12));

        // ─ Duration (full width, slightly highlighted) ─
        String durStr = txn.getDurationMinutes() != null
                ? formatDuration(txn.getDurationMinutes()) : "0 min";
        body.add(infoBlock("ELAPSED TIME", durStr, ACCENT_WARN));
        body.add(Box.createVerticalStrut(14));

        // ─ Divider ─
        JSeparator feeSep = new JSeparator();
        feeSep.setForeground(BORDER_LINE);
        feeSep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        body.add(feeSep);
        body.add(Box.createVerticalStrut(12));

        // ─ Accrued fee — large ─
        String feeStr = txn.getCalculatedFee() != null
                ? "₱" + String.format("%.2f", txn.getCalculatedFee().doubleValue())
                : "₱0.00";
        body.add(accrualBlock("ACCRUED FEE", feeStr));

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /** Two-line stacked label block: small grey label above, value below. */
    private static JPanel infoBlock(String label, String value, Color valueColor) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_SECONDARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel val = new JLabel(value);
        val.setFont(FONT_VALUE);
        val.setForeground(valueColor);
        val.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(lbl);
        block.add(Box.createVerticalStrut(3));
        block.add(val);
        return block;
    }

    /** Full-width fee block with a large bold value. */
    private static JPanel accrualBlock(String label, String value) {
        JPanel block = new JPanel(new BorderLayout(8, 0));
        block.setOpaque(false);
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_SECONDARY);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Monospaced", Font.BOLD, 18));
        val.setForeground(TEXT_PRIMARY);
        val.setHorizontalAlignment(SwingConstants.RIGHT);

        block.add(lbl, BorderLayout.WEST);
        block.add(val, BorderLayout.EAST);
        return block;
    }

    /** Format minutes as e.g. "2h 34m" or "45m". */
    private static String formatDuration(int totalMinutes) {
        if (totalMinutes < 60) return totalMinutes + "m";
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        return h + "h " + m + "m";
    }

    // ── Classification helpers ─────────────────────────────────────────────────
    /**
     * Active: car is physically parked and session is in progress.
     * RESERVED transactions are intentionally excluded — the vehicle has not
     * entered yet, so it must not appear in "Active Sessions".
     */
    private static boolean isCurrentTransaction(ParkingTransaction txn) {
        return txn.getExitTime() == null
                && !"RESERVED".equalsIgnoreCase(txn.getTransactionStatus());
    }

    // Completed = exited + PAID (falls through; kept here for reference only)

    // ── Data loading ───────────────────────────────────────────────────────────
    private record TransactionWithDetails(
            ParkingTransaction transaction,
            Vehicle vehicle,
            ParkingSlot slot
    ) {}

    private static List<TransactionWithDetails> loadUserTransactions(AppState state) {
        List<TransactionWithDetails> result = new ArrayList<>();
        try {
            model.UserAccount currentUser = state.getCurrentUserAccount();
            if (currentUser == null) return result;

            VehicleDAO vehicleDAO = new VehicleDAO();
            List<Vehicle> vehicles = vehicleDAO.findAllByUserId(currentUser.getUserId());

            ParkingTransactionDAO txnDAO = new ParkingTransactionDAO();
            ParkingSlotDAO slotDAO = new ParkingSlotDAO();
            FeeCalculationService feeService = new FeeCalculationService();

            for (Vehicle v : vehicles) {
                List<ParkingTransaction> txns = txnDAO.findByVehicleId(v.getVehicleId());
                for (ParkingTransaction txn : txns) {
                    boolean isInProgress = txn.getExitTime() == null
                            && "IN_PROGRESS".equalsIgnoreCase(txn.getTransactionStatus());

                    // For in-progress sessions: ALWAYS recalculate so the fee
                    // and duration reflect the current moment (not a stale cached value).
                    // For completed/reserved transactions: only compute once if missing.
                    if (isInProgress || txn.getCalculatedFee() == null) {
                        try {
                            BigDecimal liveFee = feeService.calculateFee(txn);
                            txn.setCalculatedFee(liveFee);
                        } catch (SQLException ignored) {}
                    }
                    if (isInProgress || (txn.getDurationMinutes() == null && txn.getEntryTime() != null)) {
                        long mins = FeeCalculationService.calculateDurationMinutes(
                                txn.getEntryTime(), txn.getExitTime());
                        txn.setDurationMinutes((int) mins);
                    }
                    Optional<ParkingSlot> slotOpt = slotDAO.findById(txn.getSlotId());
                    slotOpt.ifPresent(slot -> result.add(new TransactionWithDetails(txn, v, slot)));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    // ── Utility: rounded border ────────────────────────────────────────────────
    private static class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        RoundedLineBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }

        @Override public Insets getBorderInsets(Component c) {
            return new Insets(thickness + 2, thickness + 2, thickness + 2, thickness + 2);
        }
    }
}