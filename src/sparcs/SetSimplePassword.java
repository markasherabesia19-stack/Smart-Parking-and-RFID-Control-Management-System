import java.util.Scanner;

public class SetSimplePassword {
    public static void main(String[] args) {
        System.out.println("=== SPARCS - Simple Password Setter ===\n");
        
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter your admin password: ");
        String password = scanner.nextLine();
        
        System.out.print("Enter the admin username: ");
        String username = scanner.nextLine();
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SQL UPDATE COMMAND:");
        System.out.println("=".repeat(80) + "\n");
        
        System.out.println("UPDATE user_account SET password_hash = '" + password + "' WHERE username = '" + username + "';");
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PASSWORD SAVED (Plain Text - Not Hashed)");
        System.out.println("=".repeat(80) + "\n");
        System.out.println("1. Copy the SQL UPDATE command above");
        System.out.println("2. Paste it into MySQL and run it");
        System.out.println("3. Login with username '" + username + "' and password '" + password + "'");
        System.out.println("\nNote: Password is stored as PLAIN TEXT (no encryption)");
        
        scanner.close();
    }
}
