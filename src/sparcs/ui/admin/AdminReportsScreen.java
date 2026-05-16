package ui.admin;

import model.AppState;
import model.ParkingTransaction;
import model.Vehicle;
import ui.shared.SidebarPanel;
import util.UIFactory;
import util.DialogUtil;
import static util.UIConstants.*;
import dao.ParkingTransactionDAO;
import dao.VehicleDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

/**
 * SPARCS — Admin reports screen.
 * Displays parking analytics and provides CSV export functionality.
 */
public class AdminReportsScreen {
    private static final ParkingTransactionDAO transactionDAO = new ParkingTransactionDAO();
    private static final VehicleDAO vehicleDAO = new VehicleDAO();

    private static double dailyRevenue = 0;
    private static double weeklyRevenue = 0;
    private static int totalVehicles = 0;
    private static double avgDuration = 0;

    // Mon=0 … Sun=6 breakdown for the bar chart
    private static final double[] weeklyDayRevenue = new double[7];

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_REPORTS"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        // ── Top bar ──────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("REPORTS", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);

        JButton refreshBtn = UIFactory.outlineButton("↻  REFRESH");
        refreshBtn.setPreferredSize(new Dimension(130, 34));
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topRight.setOpaque(false);
        topRight.add(refreshBtn);
        topBar.add(topRight, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // ── Load metrics ─────────────────────────────────────────────────────
        try {
            loadReportMetrics();
        } catch (Exception e) {
            System.err.println("[AdminReportsScreen] Error loading metrics: " + e.getMessage());
            e.printStackTrace();
        }

        // ── Main body ────────────────────────────────────────────────────────
        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(18, 20, 14, 20));

        // ROW 1 — 4 metric cards
        JPanel metricsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        metricsRow.setOpaque(false);
        metricsRow.add(accentMetricCard("Daily Revenue",  String.format("P%.2f", dailyRevenue),  "today",                C_AVAILABLE));
        metricsRow.add(accentMetricCard("Weekly Revenue", String.format("P%.2f", weeklyRevenue), "this week",            C_AVAILABLE));
        metricsRow.add(accentMetricCard("Total Vehicles", String.valueOf(totalVehicles),          "registered in system", C_PURPLE));
        metricsRow.add(accentMetricCard("Avg. Duration",  formatDuration(avgDuration),            "per parking session",  C_RESERVED));

        // ROW 2 — bar chart + donut, fixed height wrapper so CENTER doesn't stretch
        JPanel row2 = new JPanel(new BorderLayout(14, 0));
        row2.setOpaque(false);
        row2.add(buildBarChartCard(), BorderLayout.CENTER);

        // Build donut card and keep reference to inner donut panel for live repainting
        JPanel[] donutRef = new JPanel[1];
        JPanel donutCard = buildDonutCard(state, donutRef);
        row2.add(donutCard, BorderLayout.EAST);

        // Wrap row2 in a fixed-height container so BorderLayout.CENTER can't expand it
        JPanel row2Wrap = new JPanel(new BorderLayout());
        row2Wrap.setOpaque(false);
        row2Wrap.setPreferredSize(new Dimension(Integer.MAX_VALUE, 250));
        row2Wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        row2Wrap.add(row2, BorderLayout.CENTER);

        // ── Live update: repaint entire donut card on every slot change ───────
        state.addSlotChangeListener(() -> SwingUtilities.invokeLater(() -> {
            donutCard.repaint();
        }));

        // ROW 3 — Export CSV bottom-right (ghost button, same as before)
        JPanel exportRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        exportRow.setOpaque(false);
        JButton exportCSVBtn = UIFactory.outlineButton("EXPORT CSV");
        exportCSVBtn.setPreferredSize(new Dimension(160, 38));
        exportCSVBtn.addActionListener(e -> exportToCSV());
        exportRow.add(exportCSVBtn);

        main.add(metricsRow, BorderLayout.NORTH);
        main.add(row2Wrap,   BorderLayout.CENTER);
        main.add(exportRow,  BorderLayout.SOUTH);

        refreshBtn.addActionListener(e -> {
            try { loadReportMetrics(); } catch (Exception ex) { ex.printStackTrace(); }
            metricsRow.removeAll();
            metricsRow.add(accentMetricCard("Daily Revenue",  String.format("P%.2f", dailyRevenue),  "today",                C_AVAILABLE));
            metricsRow.add(accentMetricCard("Weekly Revenue", String.format("P%.2f", weeklyRevenue), "this week",            C_AVAILABLE));
            metricsRow.add(accentMetricCard("Total Vehicles", String.valueOf(totalVehicles),          "registered in system", C_PURPLE));
            metricsRow.add(accentMetricCard("Avg. Duration",  formatDuration(avgDuration),            "per parking session",  C_RESERVED));
            metricsRow.revalidate();
            metricsRow.repaint();
        });

        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    // ── Accent metric card (3px colored top bar) ─────────────────────────────
    private static JPanel accentMetricCard(String label, String value, String sub, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // 3px accent — rounded top, square bottom
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, getWidth(), 6, 12, 12);
                g2.fillRect(0, 3, getWidth(), 3);
                // border
                g2.setColor(new Color(0x2D2860));
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(12, 16, 14, 16));

        JLabel lblLabel = UIFactory.lbl(label.toUpperCase(), Font.BOLD, 11, C_MUTED);
        lblLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblValue = UIFactory.lbl(value, Font.BOLD, 22, accentColor);
        lblValue.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = UIFactory.lbl(sub, Font.PLAIN, 11, C_MUTED);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        body.add(lblLabel);
        body.add(Box.createVerticalStrut(6));
        body.add(lblValue);
        body.add(Box.createVerticalStrut(4));
        body.add(lblSub);

        card.add(Box.createVerticalStrut(3), BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    // ── Weekly revenue bar chart ──────────────────────────────────────────────
    private static JPanel buildBarChartCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(0x2D2860));
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(WeekFields.of(Locale.US).dayOfWeek(), 1);
        String range = weekStart.getMonthValue() + "/" + weekStart.getDayOfMonth()
                + " \u2013 " + today.getMonthValue() + "/" + today.getDayOfMonth()
                + ", " + today.getYear();
        header.add(UIFactory.lbl("REVENUE THIS WEEK", Font.BOLD, 12, C_MUTED), BorderLayout.WEST);
        header.add(UIFactory.lbl(range, Font.PLAIN, 11, C_MUTED), BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Bar chart drawn with paintComponent
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        JPanel chart = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();
                int padLeft = 38, padBottom = 24, padTop = 14, padRight = 8;
                int chartW = w - padLeft - padRight;
                int chartH = h - padBottom - padTop;

                // Max value for scale
                double max = 0;
                for (double v : weeklyDayRevenue) if (v > max) max = v;
                if (max == 0) max = 150;

                // Gridlines + Y labels
                g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                int steps = 3;
                for (int i = 0; i <= steps; i++) {
                    int y = padTop + chartH - (int)(chartH * i / (double) steps);
                    // gridline
                    g2.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                            0, new float[]{3, 3}, 0));
                    g2.setColor(new Color(0x2D2860));
                    g2.drawLine(padLeft, y, padLeft + chartW, y);
                    // label
                    g2.setStroke(new BasicStroke(1f));
                    g2.setColor(new Color(0x6B5FA0));
                    String lbl = "P" + (int)(max * i / steps);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(lbl, padLeft - fm.stringWidth(lbl) - 4, y + 4);
                }

                // Bars
                int barGap = 5;
                int barW = (chartW - barGap * 6) / 7;
                FontMetrics fm9  = g2.getFontMetrics(new Font("SansSerif", Font.PLAIN, 9));
                FontMetrics fm10 = g2.getFontMetrics(new Font("SansSerif", Font.PLAIN, 10));

                for (int i = 0; i < 7; i++) {
                    double val = weeklyDayRevenue[i];
                    int barH = (val > 0 && max > 0) ? Math.max(3, (int)(chartH * val / max)) : 0;
                    int x = padLeft + i * (barW + barGap);
                    int y = padTop + chartH - barH;

                    // Fill
                    g2.setColor(new Color(C_AVAILABLE.getRed(), C_AVAILABLE.getGreen(),
                            C_AVAILABLE.getBlue(), barH > 0 ? 110 : 40));
                    g2.fillRoundRect(x, y, barW, barH > 0 ? barH : 3, 4, 4);
                    // Stroke
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.setColor(barH > 0 ? C_AVAILABLE
                            : new Color(C_AVAILABLE.getRed(), C_AVAILABLE.getGreen(), C_AVAILABLE.getBlue(), 60));
                    g2.drawRoundRect(x, y, barW, barH > 0 ? barH : 3, 4, 4);

                    // Value label
                    if (val > 0) {
                        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                        g2.setColor(new Color(0x9B8FD4));
                        String vs = "P" + (int)val;
                        g2.drawString(vs, x + (barW - fm9.stringWidth(vs)) / 2, y - 3);
                    }

                    // Day label
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    g2.setColor(new Color(0x6B5FA0));
                    g2.drawString(days[i], x + (barW - fm10.stringWidth(days[i])) / 2,
                            padTop + chartH + padBottom - 6);
                }

                // Baseline
                g2.setStroke(new BasicStroke(0.5f));
                g2.setColor(new Color(0x2D2860));
                g2.drawLine(padLeft, padTop + chartH, padLeft + chartW, padTop + chartH);

                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0, 140); }
        };
        chart.setOpaque(false);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    // ── Slot usage donut ──────────────────────────────────────────────────────
    private static JPanel buildDonutCard(AppState state, JPanel[] donutRef) {
        int available = state.availableSlots;
        int occupied  = state.occupiedSlots;
        int reserved  = state.reservedSlots;
        int total     = Math.max(1, available + occupied + reserved);

        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(0x2D2860));
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 12, 14, 12));
        card.setPreferredSize(new Dimension(310, 0));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIFactory.lbl("SLOT USAGE", Font.BOLD, 12, C_MUTED), BorderLayout.WEST);
        header.add(UIFactory.lbl("live", Font.PLAIN, 11, C_MUTED), BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Donut — reads state.availableSlots etc. live on every repaint
        double[] vals  = {available, occupied, reserved};
        Color[]  cols  = {C_AVAILABLE, C_OCCUPIED, C_RESERVED};

        JPanel donut = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int liveAvail    = state.availableSlots;
                int liveOccupied = state.occupiedSlots;
                int liveReserved = state.reservedSlots;
                int liveTotal    = Math.max(1, liveAvail + liveOccupied + liveReserved);
                double[] liveVals = {liveAvail, liveOccupied, liveReserved};

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int sw = 18;
                // Reserve half-stroke on every edge so ring never clips
                int margin = sw / 2 + 4;
                int size   = Math.min(getWidth(), getHeight()) - margin * 2;
                if (size < 10) { g2.dispose(); return; }
                int cx = getWidth()  / 2;
                int cy = getHeight() / 2;
                int r  = size / 2;

                // Track
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
                g2.setColor(new Color(0x1E1C45));
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);

                // Segments
                double startAngle = 90;
                for (int i = 0; i < liveVals.length; i++) {
                    if (liveVals[i] == 0) continue;
                    double sweep = 360.0 * liveVals[i] / liveTotal;
                    g2.setColor(new Color(cols[i].getRed(), cols[i].getGreen(), cols[i].getBlue(), 220));
                    g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2,
                            startAngle, -sweep, Arc2D.OPEN));
                    startAngle -= sweep;
                }

                // Center text
                g2.setFont(new Font("SansSerif", Font.BOLD, 20));
                FontMetrics fm = g2.getFontMetrics();
                String tot = String.valueOf(liveTotal);
                g2.setColor(new Color(0xF0ECFF));
                g2.drawString(tot, cx - fm.stringWidth(tot) / 2, cy + 7);

                g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                fm = g2.getFontMetrics();
                String sub = "total slots";
                g2.setColor(new Color(0x6B5FA0));
                g2.drawString(sub, cx - fm.stringWidth(sub) / 2, cy + 20);

                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(140, 140); }
        };
        donut.setOpaque(false);
        donutRef[0] = donut;

        // GridBagLayout centers the donut both horizontally and vertically
        JPanel donutWrap = new JPanel(new GridBagLayout());
        donutWrap.setOpaque(false);
        donutWrap.add(donut, new GridBagConstraints());

        // Live legend — repaints itself reading state directly
        JPanel legend = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int liveAvail    = state.availableSlots;
                int liveOccupied = state.occupiedSlots;
                int liveReserved = state.reservedSlots;
                int liveTotal    = Math.max(1, liveAvail + liveOccupied + liveReserved);

                String[][] rows = {
                    {"Available", String.valueOf(liveAvail),    String.valueOf((int)Math.round(100.0*liveAvail/liveTotal))    + "%"},
                    {"Occupied",  String.valueOf(liveOccupied), String.valueOf((int)Math.round(100.0*liveOccupied/liveTotal)) + "%"},
                    {"Reserved",  String.valueOf(liveReserved), String.valueOf((int)Math.round(100.0*liveReserved/liveTotal)) + "%"},
                };
                Color[] rowCols = {C_AVAILABLE, C_OCCUPIED, C_RESERVED};

                int rowH = 22;
                int startY = 14;
                for (int i = 0; i < rows.length; i++) {
                    int y = startY + i * rowH;
                    // Dot
                    g2.setColor(rowCols[i]);
                    g2.fillOval(0, y + 5, 8, 8);
                    // Label
                    g2.setColor(new Color(0xAFA9EC));
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    g2.drawString(rows[i][0], 14, y + 14);
                    // Count + pct
                    String val = rows[i][1] + "  " + rows[i][2];
                    g2.setColor(rowCols[i]);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(val, getWidth() - fm.stringWidth(val), y + 14);
                }
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0, 80); }
        };
        legend.setOpaque(false);
        donutRef[0] = donut;

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(donutWrap, BorderLayout.CENTER);
        center.add(legend,    BorderLayout.SOUTH);

        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private static JPanel legendRow(String label, int count, int total, Color color) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        left.setOpaque(false);

        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 2, 8, 8);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(8, 12); }
            @Override public boolean isOpaque() { return false; }
        };

        left.add(dot);
        left.add(UIFactory.lbl(label, Font.PLAIN, 12, C_MUTED));

        int pct = (int) Math.round(100.0 * count / total);
        JLabel valLbl = UIFactory.lbl(count + "  " + pct + "%", Font.BOLD, 12, color);

        row.add(left,   BorderLayout.WEST);
        row.add(valLbl, BorderLayout.EAST);
        return row;
    }

    // ── Data loading (unchanged) ──────────────────────────────────────────────
    private static void loadReportMetrics() {
        try {
            LocalDate today     = LocalDate.now();
            LocalDate weekStart = today.with(WeekFields.of(Locale.US).dayOfWeek(), 1);
            LocalDate oneMonthAgo = today.minusMonths(1);

            dailyRevenue = 0;
            weeklyRevenue = 0;
            double totalDuration = 0;
            int completedTransactions = 0;
            for (int i = 0; i < 7; i++) weeklyDayRevenue[i] = 0;

            try {
                List<ParkingTransaction> allTransactions = transactionDAO.findByDateRange(oneMonthAgo, today);
                for (ParkingTransaction tx : allTransactions) {
                    if (tx.getExitTime() != null && "COMPLETED".equals(tx.getTransactionStatus())) {
                        try {
                            double amount = tx.getCalculatedFee() != null ? tx.getCalculatedFee().doubleValue() : 0;
                            LocalDate txDate = tx.getExitTime().toLocalDate();

                            if (txDate.equals(today)) dailyRevenue += amount;

                            if (!txDate.isBefore(weekStart) && !txDate.isAfter(today)) {
                                weeklyRevenue += amount;
                                int dayIdx = txDate.getDayOfWeek().getValue() - 1; // Mon=0…Sun=6
                                weeklyDayRevenue[dayIdx] += amount;
                            }

                            if (tx.getDurationMinutes() != null && tx.getDurationMinutes() > 0) {
                                totalDuration += tx.getDurationMinutes();
                                completedTransactions++;
                            }
                        } catch (Exception e) {
                            System.err.println("[AdminReportsScreen] Error processing tx: " + e.getMessage());
                        }
                    }
                }
                avgDuration = completedTransactions > 0 ? totalDuration / completedTransactions : 0;
            } catch (SQLException e) {
                System.err.println("[AdminReportsScreen] Error loading transactions: " + e.getMessage());
            }

            try {
                List<Vehicle> vehicles = vehicleDAO.findAll();
                totalVehicles = vehicles.size();
            } catch (SQLException e) {
                System.err.println("[AdminReportsScreen] Error loading vehicles: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("[AdminReportsScreen] Unexpected error: " + e.getMessage());
        }
    }

    private static String formatDuration(double minutes) {
        if (minutes == 0) return "-";
        int hours = (int)(minutes / 60);
        int mins  = (int)(minutes % 60);
        return hours > 0 ? String.format("%dh %dm", hours, mins) : String.format("%dm", mins);
    }

    // ── CSV export (unchanged logic, currency fixed to P) ─────────────────────
    private static void exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override public boolean accept(File f) { return f.isDirectory() || f.getName().endsWith(".csv"); }
            @Override public String getDescription() { return "CSV Files (*.csv)"; }
        });
        fileChooser.setSelectedFile(new File("SPARCS_Report_" + LocalDate.now() + ".csv"));

        int result = fileChooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                writer.println("SPARCS - Smart Parking & RFID Control Management System");
                writer.println("Report Generated: " + LocalDateTime.now());
                writer.println();
                writer.println("METRICS");
                writer.println("Daily Revenue,"   + String.format("P%.2f", dailyRevenue));
                writer.println("Weekly Revenue,"  + String.format("P%.2f", weeklyRevenue));
                writer.println("Total Vehicles,"  + totalVehicles);
                writer.println("Average Duration," + formatDuration(avgDuration));
                writer.println();
                writer.println("TRANSACTION SUMMARY");
                writer.println("Date,Transaction ID,Vehicle,Entry Time,Exit Time,Amount,Status");

                try {
                    LocalDate today = LocalDate.now();
                    List<ParkingTransaction> transactions = transactionDAO.findByDateRange(today.minusMonths(1), today);
                    for (ParkingTransaction tx : transactions) {
                        if (tx.getExitTime() != null && "COMPLETED".equals(tx.getTransactionStatus())) {
                            double fee = tx.getCalculatedFee() != null ? tx.getCalculatedFee().doubleValue() : 0;
                            writer.printf("%s,%d,%s,%s,%s,P%.2f,%s%n",
                                    tx.getExitTime().toLocalDate(),
                                    tx.getTransactionId(),
                                    "Vehicle #" + tx.getVehicleId(),
                                    tx.getEntryTime(),
                                    tx.getExitTime(),
                                    fee,
                                    tx.getTransactionStatus());
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error reading transactions: " + e.getMessage());
                }

                DialogUtil.showMessageDialog(null,
                        "Report exported successfully to:\n" + fileChooser.getSelectedFile().getAbsolutePath(),
                        "Export Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                DialogUtil.showMessageDialog(null,
                        "Error exporting report: " + e.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}