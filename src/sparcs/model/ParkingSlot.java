package model;

// Represents a single parking slot.
// status: "AVAILABLE" | "OCCUPIED" | "RESERVED"
// slotCode format: "A-01" … "E-08"  (5 zones × 8 slots = 40 total)
// slotIndex: 0-based position in AppState.slotData[]
 
public class ParkingSlot {

    public static final String AVAILABLE = "AVAILABLE";
    public static final String OCCUPIED  = "OCCUPIED";
    public static final String RESERVED  = "RESERVED";

    private int    slotId;
    private String slotCode;   // e.g. "B-04"
    private String status;     // "AVAILABLE" | "OCCUPIED" | "RESERVED"
    private String zone;       // "A" … "E"

    public ParkingSlot() {}

    public ParkingSlot(int slotId, String slotCode, String status, String zone) {
        this.slotId   = slotId;
        this.slotCode = slotCode;
        this.status   = status;
        this.zone     = zone;
    }

    public int    getSlotId()           { return slotId; }
    public void   setSlotId(int slotId) { this.slotId = slotId; }

    public String getSlotCode()                { return slotCode; }
    public void   setSlotCode(String slotCode) { this.slotCode = slotCode; }

    public String getStatus()                  { return status; }
    public void   setStatus(String status)     { this.status = status; }

    public String getZone()                    { return zone; }
    public void   setZone(String zone)         { this.zone = zone; }

    public boolean isAvailable() { return AVAILABLE.equals(status); }
    public boolean isOccupied()  { return OCCUPIED.equals(status);  }
    public boolean isReserved()  { return RESERVED.equals(status);  }

    // Converts slotCode (e.g. "B-04") to a 0-based index into AppState.slotData[].
    // Zone A=0, B=1, C=2, D=3, E=4. Slot numbers are 1-based.
    
    public int getSlotIndex() {
        if (slotCode == null || slotCode.length() < 4) return -1;
        char zoneChar = Character.toUpperCase(slotCode.charAt(0));
        int  zoneRow  = zoneChar - 'A';
        int  slotNum;
        try {
            slotNum = Integer.parseInt(slotCode.substring(2));
        } catch (NumberFormatException ex) {
            return -1;
        }
        return zoneRow * 8 + (slotNum - 1);
    }

    @Override
    public String toString() {
        return "ParkingSlot{slotId=" + slotId +
               ", slotCode='" + slotCode + '\'' +
               ", status='" + status + '\'' +
               ", zone='" + zone + '\'' + '}';
    }
}