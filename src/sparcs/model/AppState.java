package model;

import dao.ParkingSlotDAO;
import service.AuthenticationService;
import service.ParkingService;
import service.FeeCalculationService;
import static util.UIConstants.TOTAL_SLOTS;

import java.util.ArrayList;
import java.util.List;

/**
 * SPARCS - Shared application state.
 *
 * Holds the current user session, slot data, and backend services.
 */
public class AppState {

    // ── Session ───────────────────────────────────────────────────────────────
    public String currentRole        = "";
    public String currentUsername    = "";
    private UserAccount currentUserAccount;

    // ── Backend Services ──────────────────────────────────────────────────────
    private AuthenticationService authService;
    private ParkingService        parkingService;
    private FeeCalculationService feeService;

    // ── Slot Data ─────────────────────────────────────────────────────────────
    public int availableSlots = TOTAL_SLOTS;
    public int occupiedSlots  = 0;
    public int reservedSlots  = 0;

    /** 0 = available, 1 = occupied, 2 = reserved */
    public final int[] slotData = new int[TOTAL_SLOTS];

    // ── Slot Map Refresh Listeners ────────────────────────────────────────────
    /** Any panel that shows slot data registers a Runnable here to be notified on changes. */
    private final List<Runnable> slotChangeListeners = new ArrayList<>();

    public void addSlotChangeListener(Runnable listener) {
        slotChangeListeners.add(listener);
    }

    public void removeSlotChangeListener(Runnable listener) {
        slotChangeListeners.remove(listener);
    }

    /**
     * Call this after any entry/exit operation to push the change to all
     * registered slot-map panels immediately, without waiting for navigation.
     */
    public void notifySlotChange() {
        loadSlotDataFromDB();
        for (Runnable listener : slotChangeListeners) {
            listener.run();
        }
    }

    public AppState() {
        initServices();
        loadSlotDataFromDB();
    }

    /** Initialize backend services. */
    private void initServices() {
        this.authService    = new AuthenticationService();
        this.parkingService = new ParkingService();
        this.feeService     = new FeeCalculationService();
    }

    /**
     * Loads every slot's actual status from the parking_slot table into
     * slotData[] and recalculates the three summary counts.
     */
    public void loadSlotDataFromDB() {
        for (int i = 0; i < TOTAL_SLOTS; i++) slotData[i] = 0;
        availableSlots = TOTAL_SLOTS;
        occupiedSlots  = 0;
        reservedSlots  = 0;

        try {
            ParkingSlotDAO slotDAO = new ParkingSlotDAO();
            List<ParkingSlot> slots = slotDAO.findAll();

            int available = 0, occupied = 0, reserved = 0;

            for (ParkingSlot slot : slots) {
                int idx = slot.getSlotIndex();
                if (idx < 0 || idx >= TOTAL_SLOTS) continue;

                switch (slot.getStatus()) {
                    case ParkingSlot.OCCUPIED -> { slotData[idx] = 1; occupied++; }
                    case ParkingSlot.RESERVED -> { slotData[idx] = 2; reserved++; }
                    default                   -> { slotData[idx] = 0; available++; }
                }
            }

            if (slots.size() < TOTAL_SLOTS) available += (TOTAL_SLOTS - slots.size());

            availableSlots = available;
            occupiedSlots  = occupied;
            reservedSlots  = reserved;

        } catch (Exception e) {
            System.err.println("[AppState] Failed to load slot data from DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Legacy alias — delegates to loadSlotDataFromDB(). */
    public void initSlotData() {
        loadSlotDataFromDB();
    }

    /** Clears the active session (used on sign-out). */
    public void clearSession() {
        currentRole        = "";
        currentUsername    = "";
        currentUserAccount = null;
        authService.logout();
    }

    // ── Service Getters ───────────────────────────────────────────────────────
    public AuthenticationService  getAuthService()    { return authService;    }
    public ParkingService         getParkingService() { return parkingService; }
    public FeeCalculationService  getFeeService()     { return feeService;     }

    /** Set current user after successful authentication. */
    public void setCurrentUser(UserAccount user) {
        this.currentUserAccount = user;
        if (user != null) {
            this.currentUsername = user.getUsername();
            this.currentRole     = user.getRole();
        }
    }

    public UserAccount getCurrentUserAccount() { return currentUserAccount; }

    public boolean isAuthenticated() { return currentUserAccount != null; }
}