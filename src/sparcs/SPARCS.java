/* control shift b to run */
import javax.swing.*;
import java.awt.*;

import model.AppState;
import ui.admin.*;
import ui.user.*;
import ui.shared.*;
import static util.UIConstants.*;

public class SPARCS extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     rootPanel  = new JPanel(cardLayout);
    private final AppState   state      = new AppState();

    public SPARCS() {
        super("SPARCS - Smart Parking & RFID Control Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG_DARK);
    
        registerScreens();

        add(rootPanel);
        cardLayout.show(rootPanel, "SPLASH");
        setVisible(true);
    }

    private void registerScreens() {
        //Shared/Auth
        rootPanel.add(SplashPanel.build(cardLayout, rootPanel),                        "SPLASH");
        rootPanel.add(RolePickerScreen.build(cardLayout, rootPanel, state),             "ROLE_PICKER");

        //Admin 
        rootPanel.add(AdminLoginScreen.build(cardLayout, rootPanel, state),             "ADMIN_LOGIN");
        rootPanel.add(AdminDashboardScreen.build(cardLayout, rootPanel, state),         "ADMIN_DASHBOARD");
        rootPanel.add(AdminSlotMapScreen.build(cardLayout, rootPanel, state),           "ADMIN_SLOT_MAP");
        rootPanel.add(AdminEntryExitScreen.build(cardLayout, rootPanel, state),         "ADMIN_ENTRY_EXIT");
        rootPanel.add(AdminVehiclesScreen.build(cardLayout, rootPanel, state),          "ADMIN_VEHICLES");
        rootPanel.add(AdminRegisterScreen.build(cardLayout, rootPanel, state),          "ADMIN_REGISTER");
        rootPanel.add(AdminFeesScreen.build(cardLayout, rootPanel, state),              "ADMIN_FEES");
        rootPanel.add(AdminReportsScreen.build(cardLayout, rootPanel, state),           "ADMIN_REPORTS");
        rootPanel.add(AdminAuditLogScreen.build(cardLayout, rootPanel, state),          "ADMIN_AUDIT_LOG");
        
        //User
        rootPanel.add(UserLoginScreen.build(cardLayout, rootPanel, state),              "USER_LOGIN");
        rootPanel.add(UserRegisterScreen.build(cardLayout, rootPanel),                  "USER_REGISTER");
        rootPanel.add(UserDashboardScreen.build(cardLayout, rootPanel, state),          "USER_DASHBOARD");
        rootPanel.add(UserMyStatusScreen.build(cardLayout, rootPanel, state),           "USER_MY_STATUS");
        rootPanel.add(UserSlotViewScreen.build(cardLayout, rootPanel, state),           "USER_SLOT_VIEW");
        rootPanel.add(UserHistoryScreen.build(cardLayout, rootPanel, state),            "USER_HISTORY");
        rootPanel.add(UserFeeScheduleScreen.build(cardLayout, rootPanel, state),        "USER_FEE_SCHEDULE");
        rootPanel.add(UserRFIDCardScreen.build(cardLayout, rootPanel, state),           "USER_RFID_CARD");
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(SPARCS::new);
    }
}
