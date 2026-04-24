import util.PasswordUtil;

public class HashGenerator {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java sparcs.HashGenerator <password>");
            System.out.println("\nExample passwords to hash:");
            System.out.println("  admin123");
            System.out.println("  user12345");
            return;
        }

        String password = args[0];
        String hash = PasswordUtil.hashPassword(password);
        
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("\nUse this hash in your SQL INSERT statement:");
        System.out.println("INSERT INTO user_account (username, password_hash, role, email) VALUES");
        System.out.println("('" + args[0] + "', '" + hash + "', 'USER', '" + args[0] + "@sparcs.com');");
    }
}
