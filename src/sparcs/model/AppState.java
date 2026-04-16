package model;

import service.AuthenticationService;
import service.ParkingService;
import service.FeeCalculationService;
import static util.UIConstants.TOTAL_SLOTS;

/**
 * SPARCS - Shared application state.
 *
 * Holds the current user session, slot data, and backend services.
 */
public class AppState {

    // -- Session --────────────────────────────────────────────────────────────
    public String currentRole     = "";
    public String currentUsername = "";
    private UserAccount currentUserAccount;

    // -- Backend Services ─────────────────────────────────────────────────────
    private AuthenticationService authService;
    private ParkingService parkingService;
    private FeeCalculationService feeService;

    // ── Slot Data ─────────────────────────────────────────────────────────
    public int availableSlots = 20;
    public int occupiedSlots  = 15;
    public int reservedSlots  = 5;

    /** 0 = available, 1 = occupied, 2 = reserved */
    public final int[] slotData = new int[TOTAL_SLOTS];

    public AppState() {
        initServices();
        initSlotData();
    }

    /** Initialize backend services */
    private void initServices() {
        this.authService = new AuthenticationService();
        this.parkingService = new ParkingService();
        this.feeService = new FeeCalculationService();
    }

    /** Populates slotData array from the three count fields. */
    public void initSlotData() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if      (i < occupiedSlots)                     slotData[i] = 1;
            else if (i < occupiedSlots + reservedSlots)     slotData[i] = 2;
            else                                            slotData[i] = 0;
        }
    }

    /** Clears the active session (used on sign-out). */
    public void clearSession() {
        currentRole     = "";
        currentUsername = "";
        currentUserAccount = null;
        authService.logout();
    }

    // -- Service Getters ──────────────────────────────────────────────────────
    public AuthenticationService getAuthService() {
        return authService;
    }

    public ParkingService getParkingService() {
        return parkingService;
    }

    public FeeCalculationService getFeeService() {
        return feeService;
    }

    /**
     * Set current user after successful authentication
     */
    public void setCurrentUser(UserAccount user) {
        this.currentUserAccount = user;
        if (user != null) {
            this.currentUsername = user.getUsername();
            this.currentRole = user.getRole();
        }
    }

    /**
     * Get current user account
     */
    public UserAccount getCurrentUserAccount() {
        return currentUserAccount;
    }

    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        return currentUserAccount != null;
    }
}
