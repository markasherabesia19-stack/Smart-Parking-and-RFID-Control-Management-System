
import util.PasswordUtil;
import java.util.Scanner;

/**
 * Helper tool to generate password hashes for database updates
 */
public class UpdatePasswordHash {
    public static void main(String[] args) {
        System.out.println("=== SPARCS Password Hash Generator ===\n");
        
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter the admin password you want to use: ");
        String password = scanner.nextLine();
        
        System.out.print("Enter the admin username: ");
        String username = scanner.nextLine();
        
        // Generate hash
        String hash = PasswordUtil.hashPassword(password);
        
        System.out.println("\n✓ Password hash generated successfully!\n");
        System.out.println("Generated Hash:");
        System.out.println(hash);
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SQL UPDATE COMMAND:");
        System.out.println("=".repeat(80) + "\n");
        
        System.out.println("UPDATE user_account SET password_hash = '" + hash + "' WHERE username = '" + username + "';");
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("INSTRUCTIONS:");
        System.out.println("=".repeat(80) + "\n");
        System.out.println("1. Open MySQL and run the SQL UPDATE command above");
        System.out.println("2. Or copy the hash value above and paste it into your MySQL GUI tool");
        System.out.println("3. Then try logging in with the username '" + username + "' and password '" + password + "'");
        
        scanner.close();
    }
}
