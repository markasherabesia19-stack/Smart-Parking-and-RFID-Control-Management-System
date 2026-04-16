package util;

import model.UserAccount;
import java.time.LocalDateTime;

/**
 * User Session Manager
 * Manages user sessions and authentication state
 */
public class UserSessionManager {
    private static UserSessionManager instance;
    private UserAccount currentUser;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivityTime;
    private static final long SESSION_TIMEOUT_MINUTES = 30;

    private UserSessionManager() {}

    public static synchronized UserSessionManager getInstance() {
        if (instance == null) {
            instance = new UserSessionManager();
        }
        return instance;
    }

    /**
     * Set current user after successful login
     */
    public void setCurrentUser(UserAccount user) {
        this.currentUser = user;
        this.loginTime = LocalDateTime.now();
        this.lastActivityTime = LocalDateTime.now();
    }

    /**
     * Get current user
     */
    public UserAccount getCurrentUser() {
        if (isSessionValid()) {
            updateLastActivityTime();
            return this.currentUser;
        }
        return null;
    }

    /**
     * Check if session is valid
     */
    public boolean isSessionValid() {
        if (this.currentUser == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        long minutesElapsed = java.time.temporal.ChronoUnit.MINUTES.between(lastActivityTime, now);

        if (minutesElapsed > SESSION_TIMEOUT_MINUTES) {
            clearSession();
            return false;
        }

        return true;
    }

    /**
     * Update last activity time
     */
    private void updateLastActivityTime() {
        this.lastActivityTime = LocalDateTime.now();
    }

    /**
     * Clear session (logout)
     */
    public void clearSession() {
        this.currentUser = null;
        this.loginTime = null;
        this.lastActivityTime = null;
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        if (isSessionValid()) {
            return "ADMIN".equals(this.currentUser.getRole());
        }
        return false;
    }

    /**
     * Check if user is regular user
     */
    public boolean isUser() {
        if (isSessionValid()) {
            return "USER".equals(this.currentUser.getRole());
        }
        return false;
    }
}
