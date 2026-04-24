import db.DatabaseConfig;
import dao.UserAccountDAO;
import model.UserAccount;
import util.PasswordUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public class LoginDiagnostic {
    public static void main(String[] args) throws Exception {
        System.out.println("=== SPARCS Login Diagnostic Tool ===\n");

        // Test 1: Database Connection
        System.out.println("Step 1: Testing database connection...");
        try {
            Connection conn = DatabaseConfig.getInstance().getConnection();
            if (conn != null) {
                System.out.println("✓ Database connected successfully");
                conn.close();
            }
        } catch (SQLException e) {
            System.out.println("✗ Database connection failed: " + e.getMessage());
            return;
        }

        // Test 2: Check user in database
        System.out.println("\nStep 2: Checking for admin user in database...");
        UserAccountDAO userDAO = new UserAccountDAO();
        Optional<UserAccount> adminUser = userDAO.findByUsername("admin");

        if (adminUser.isPresent()) {
            UserAccount user = adminUser.get();
            System.out.println("✓ Admin user found!");
            System.out.println("  - User ID: " + user.getUserId());
            System.out.println("  - Username: " + user.getUsername());
            System.out.println("  - Role: " + user.getRole());
            System.out.println("  - Email: " + user.getEmail());
            System.out.println("  - Is Active: " + user.isActive());
            System.out.println("  - Password Hash (first 50 chars): " + user.getPasswordHash().substring(0, Math.min(50, user.getPasswordHash().length())) + "...");
        } else {
            System.out.println("✗ Admin user NOT found in database");
            System.out.println("  Possible reasons:");
            System.out.println("  1. Username is spelled differently");
            System.out.println("  2. User is_active is FALSE");
            System.out.println("  3. User doesn't exist");
            return;
        }

        // Test 3: Test password verification
        System.out.println("\nStep 3: Testing password verification...");
        
        System.out.println("  Enter the password you used: ");
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        String testPassword = scanner.nextLine();
        
        UserAccount user = adminUser.get();
        boolean passwordMatches = PasswordUtil.verifyPassword(testPassword, user.getPasswordHash());
        
        if (passwordMatches) {
            System.out.println("✓ Password verification successful!");
            System.out.println("  The password matches the hash in the database.");
        } else {
            System.out.println("✗ Password verification FAILED!");
            System.out.println("  The password does NOT match the hash in the database.");
            System.out.println("\n  This could mean:");
            System.out.println("  1. Password is incorrect");
            System.out.println("  2. Password hash was not properly generated");
            
            System.out.println("\n  Correct hash for this password:");
            String correctHash = PasswordUtil.hashPassword(testPassword);
            System.out.println("  " + correctHash);
        }

        System.out.println("\n=== End of Diagnostic ===");
    }
}
