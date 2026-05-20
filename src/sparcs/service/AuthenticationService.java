package service;

import dao.UserAccountDAO;
import model.UserAccount;
import util.PasswordUtil;
import java.sql.SQLException;
import java.util.Optional;

// Authentication Service
// Handles user authentication and session management

public class AuthenticationService {
    private UserAccountDAO userAccountDAO;
    private UserAccount currentUser;

    public AuthenticationService() {
        this.userAccountDAO = new UserAccountDAO();
    }

    // Authenticate user with username and password
    //  Includes checking inactive accounts to provide proper feedback

    public boolean authenticate(String username, String password) throws SQLException {
        Optional<UserAccount> user = userAccountDAO.findByUsernameAny(username);

        if (user.isPresent()) {
            UserAccount account = user.get();
            if (PasswordUtil.verifyPassword(password, account.getPasswordHash())) {
                this.currentUser = account;
                return true;
            }
        }
        return false;
    }

    // Register a new user account
    
    public boolean register(String username, String password, String email, String role) throws SQLException {
        // Check if username already exists
        Optional<UserAccount> existing = userAccountDAO.findByUsername(username);
        if (existing.isPresent()) {
            return false;
        }

        UserAccount newUser = new UserAccount();
        newUser.setUsername(username);
        newUser.setPasswordHash(PasswordUtil.hashPassword(password));
        newUser.setEmail(email);
        newUser.setRole(role);
        newUser.setActive(true);

        try {
            userAccountDAO.create(newUser);
            return true;
        } catch (SQLException e) {
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    // Logout current user
    
    public void logout() {
        this.currentUser = null;
    }

    // Get current authenticated user
     
    public UserAccount getCurrentUser() {
        return this.currentUser;
    }

    // Check if user is authenticated
    public boolean isAuthenticated() {
        return this.currentUser != null;
    }

    // Check if current user is admin
    public boolean isAdmin() {
        return isAuthenticated() && "ADMIN".equals(currentUser.getRole());
    }

    // Check if current user is regular user
    public boolean isUser() {
        return isAuthenticated() && "USER".equals(currentUser.getRole());
    }
}
