package sparcs.ui;

import sparcs.ui.components.*;
import sparcs.ui.panels.*;
import javax.swing.*;
import java.awt.*;

/**
 * Main application window after login.
 * Holds: sidebar (left) + content area (right, card-swapped on nav).
 */
public class DashboardFrame extends JFrame {

    private final boolean isAdmin;
    private final String  username;
    private JPanel contentArea;
    private SidebarPanel sidebar;

    public DashboardFrame(boolean isAdmin, String username) {
        this.isAdmin  = isAdmin;
        this.username = username;

        setTitle("SPARCS");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setContentPane(buildRoot());

        // Show default page
        String defaultPage = isAdmin ? "dashboard" : "mystatus";
        sidebar.setActivePage(defaultPage);
        showPage(defaultPage);
    }

    private JPanel buildRoot() {
        JPanel root = new JPanel(new BorderLayout());

        sidebar = new SidebarPanel(isAdmin);
        sidebar.setNavListener(this::showPage);
        root.add(sidebar, BorderLayout.WEST);

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(SPARCSTheme.BG_LEFT);
        root.add(contentArea, BorderLayout.CENTER);

        return root;
    }

    private void showPage(String page) {
        contentArea.removeAll();
        JPanel panel;

        switch (page) {
            // Admin pages
            case "dashboard":  panel = new DashboardPanel(isAdmin);    break;
            case "slotmap":    panel = new SlotMapPanel(true);          break;
            case "entryexit":  panel = new EntryExitPanel();            break;
            case "vehicles":   panel = new VehiclesPanel();             break;
            case "register":   panel = new RegisterVehiclePanel();      break;
            case "fees":       panel = new FeeManagementPanel(true);    break;
            case "reports":    panel = new ReportPanel();               break;
            case "auditlog":   panel = new AuditLogPanel();             break;

            // User pages
            case "mystatus":   panel = new MyStatusPanel(username);     break;
            case "slotview":   panel = new SlotMapPanel(false);         break;
            case "history":    panel = new HistoryPanel();              break;
            case "feeschedule":panel = new FeeManagementPanel(false);   break;

            default:           panel = new DashboardPanel(isAdmin);
        }

        contentArea.add(panel, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }
}
