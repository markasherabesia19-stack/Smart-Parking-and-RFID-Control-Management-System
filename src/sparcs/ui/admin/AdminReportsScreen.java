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
 * Displays parking analytics and provides CSV/PDF export functionality.
 */
public class AdminReportsScreen {
    private static final ParkingTransactionDAO transactionDAO = new ParkingTransactionDAO();
    private static final VehicleDAO vehicleDAO = new VehicleDAO();
    
    private static double dailyRevenue = 0;
    private static double weeklyRevenue = 0;
    private static int totalVehicles = 0;
    private static double avgDuration = 0;

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_REPORTS"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("REPORTS", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);
        content.add(topBar, BorderLayout.NORTH);

        // Load metrics from database (with error handling)
        try {
            loadReportMetrics();
        } catch (Exception e) {
            System.err.println("[AdminReportsScreen] Error loading metrics: " + e.getMessage());
            e.printStackTrace();
            // Use default values if load fails
            dailyRevenue = 0;
            weeklyRevenue = 0;
            totalVehicles = 0;
            avgDuration = 0;
        }

        JPanel body = new JPanel(new GridLayout(2, 2, 14, 14));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 20, 20, 20));

        body.add(reportCard("Daily Revenue", String.format("$%.2f", dailyRevenue), "today", C_ACCENT));
        body.add(reportCard("Weekly Revenue", String.format("$%.2f", weeklyRevenue), "this week", C_AVAILABLE));
        body.add(reportCard("Total Vehicles", String.valueOf(totalVehicles), "registered in system", C_PURPLE));
        body.add(reportCard("Avg. Duration", formatDuration(avgDuration), "per parking session", C_RESERVED));

        JPanel exportRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        exportRow.setOpaque(false);
        exportRow.setBorder(new EmptyBorder(0, 20, 16, 20));
        
        JButton exportCSVBtn = UIFactory.gradientButton("EXPORT CSV");
        exportCSVBtn.addActionListener(e -> exportToCSV());
        
        JButton exportPDFBtn = UIFactory.gradientButton("EXPORT PDF");
        exportPDFBtn.addActionListener(e -> exportToPDF());
        
        exportRow.add(exportCSVBtn);
        exportRow.add(exportPDFBtn);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(body, BorderLayout.CENTER);
        main.add(exportRow, BorderLayout.SOUTH);
        content.add(main, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private static void loadReportMetrics() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.with(WeekFields.of(Locale.US).dayOfWeek(), 1);
            LocalDate oneMonthAgo = today.minusMonths(1);
            
            dailyRevenue = 0;
            weeklyRevenue = 0;
            double totalDuration = 0;
            int completedTransactions = 0;
            
            try {
                // Get completed transactions from the last month
                List<ParkingTransaction> allTransactions = transactionDAO.findByDateRange(oneMonthAgo, today);
                
                for (ParkingTransaction tx : allTransactions) {
                    if (tx.getExitTime() != null && "COMPLETED".equals(tx.getTransactionStatus())) {
                        try {
                            double amount = tx.getCalculatedFee() != null ? tx.getCalculatedFee().doubleValue() : 0;
                            LocalDate txDate = tx.getExitTime().toLocalDate();
                            
                            // Calculate daily revenue
                            if (txDate.equals(today)) {
                                dailyRevenue += amount;
                            }
                            
                            // Calculate weekly revenue
                            if (!txDate.isBefore(weekStart) && !txDate.isAfter(today)) {
                                weeklyRevenue += amount;
                            }
                            
                            // Calculate average duration
                            if (tx.getDurationMinutes() != null && tx.getDurationMinutes() > 0) {
                                totalDuration += tx.getDurationMinutes();
                                completedTransactions++;
                            }
                        } catch (Exception e) {
                            System.err.println("[AdminReportsScreen] Error processing transaction: " + e.getMessage());
                        }
                    }
                }
                
                avgDuration = completedTransactions > 0 ? totalDuration / completedTransactions : 0;
            } catch (SQLException e) {
                System.err.println("[AdminReportsScreen] Error loading transactions: " + e.getMessage());
            }
            
            try {
                // Load total vehicles
                List<Vehicle> vehicles = vehicleDAO.findAll();
                totalVehicles = vehicles.size();
            } catch (SQLException e) {
                System.err.println("[AdminReportsScreen] Error loading vehicles: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("[AdminReportsScreen] Unexpected error in loadReportMetrics: " + e.getMessage());
        }
    }

    private static String formatDuration(double minutes) {
        if (minutes == 0) return "-";
        int hours = (int) (minutes / 60);
        int mins = (int) (minutes % 60);
        if (hours > 0) {
            return String.format("%dh %dm", hours, mins);
        }
        return String.format("%dm", mins);
    }

    private static void exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".csv");
            }
            @Override
            public String getDescription() {
                return "CSV Files (*.csv)";
            }
        });
        fileChooser.setSelectedFile(new File("SPARCS_Report_" + LocalDate.now() + ".csv"));
        
        int result = fileChooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                writer.println("SPARCS - Smart Parking & RFID Control Management System");
                writer.println("Report Generated: " + LocalDateTime.now());
                writer.println();
                writer.println("METRICS");
                writer.println("Daily Revenue," + String.format("$%.2f", dailyRevenue));
                writer.println("Weekly Revenue," + String.format("$%.2f", weeklyRevenue));
                writer.println("Total Vehicles," + totalVehicles);
                writer.println("Average Duration," + formatDuration(avgDuration));
                writer.println();
                writer.println("TRANSACTION SUMMARY");
                writer.println("Date,Transaction ID,Vehicle,Entry Time,Exit Time,Amount,Status");
                
                try {
                    LocalDate today = LocalDate.now();
                    LocalDate oneMonthAgo = today.minusMonths(1);
                    List<ParkingTransaction> transactions = transactionDAO.findByDateRange(oneMonthAgo, today);
                    
                    for (ParkingTransaction tx : transactions) {
                        if (tx.getExitTime() != null && "COMPLETED".equals(tx.getTransactionStatus())) {
                            String vehicleInfo = "Vehicle #" + tx.getVehicleId();
                            double fee = tx.getCalculatedFee() != null ? tx.getCalculatedFee().doubleValue() : 0;
                            writer.printf("%s,%d,%s,%s,%s,$%.2f,%s%n",
                                tx.getExitTime().toLocalDate(),
                                tx.getTransactionId(),
                                vehicleInfo,
                                tx.getEntryTime(),
                                tx.getExitTime(),
                                fee,
                                tx.getTransactionStatus()
                            );
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

    private static void exportToPDF() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".txt");
            }
            @Override
            public String getDescription() {
                return "Text Files (*.txt)";
            }
        });
        fileChooser.setSelectedFile(new File("SPARCS_Report_" + LocalDate.now() + ".txt"));
        
        int result = fileChooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                writer.println("╔════════════════════════════════════════════════════════════════╗");
                writer.println("║  SPARCS - Smart Parking & RFID Control Management System        ║");
                writer.println("║  ADMINISTRATIVE REPORT                                         ║");
                writer.println("╚════════════════════════════════════════════════════════════════╝");
                writer.println();
                writer.println("Report Generated: " + LocalDateTime.now());
                writer.println();
                writer.println("─────────────────────────────────────────────────────────────────");
                writer.println("FINANCIAL METRICS");
                writer.println("─────────────────────────────────────────────────────────────────");
                writer.printf("Daily Revenue (Today):        $%.2f%n", dailyRevenue);
                writer.printf("Weekly Revenue (This Week):   $%.2f%n", weeklyRevenue);
                writer.println();
                writer.println("─────────────────────────────────────────────────────────────────");
                writer.println("SYSTEM METRICS");
                writer.println("─────────────────────────────────────────────────────────────────");
                writer.printf("Total Vehicles Registered:    %d%n", totalVehicles);
                writer.printf("Average Parking Duration:     %s%n", formatDuration(avgDuration));
                writer.println();
                writer.println("─────────────────────────────────────────────────────────────────");
                writer.println("END OF REPORT");
                writer.println("─────────────────────────────────────────────────────────────────");
                
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

    private static JPanel reportCard(String label, String value, String sub, Color valueColor) {
        JPanel card = UIFactory.cardPanel(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));
        card.add(UIFactory.lbl(value, Font.BOLD, 30, valueColor), BorderLayout.CENTER);
        card.add(UIFactory.lbl(label.toUpperCase(), Font.BOLD, 11, C_WHITE), BorderLayout.NORTH);
        card.add(UIFactory.lbl(sub, Font.PLAIN, 11, C_MUTED), BorderLayout.SOUTH);
        return card;
    }
}
