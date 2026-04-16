package util;

/**
 * Password Utility Class
 * Simple password storage and verification (plain text)
 */
public class PasswordUtil {

    /**
     * Store password as plain text (no hashing)
     */
    public static String hashPassword(String password) {
        return password;
    }

    /**
     * Verify password by simple comparison
     */
    public static boolean verifyPassword(String password, String storedPassword) {
        return password != null && password.equals(storedPassword);
    }
}
