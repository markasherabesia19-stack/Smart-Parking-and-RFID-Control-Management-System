package sparcs.ui.components;

import java.awt.*;

public class SPARCSTheme {
    // Background gradient colors
    public static final Color BG_LEFT   = new Color(0x2B20B0);
    public static final Color BG_RIGHT  = new Color(0xC43A9B);

    // Sidebar
    public static final Color SIDEBAR_BG   = new Color(0x0D0A2E);
    public static final Color SIDEBAR_ACTIVE = new Color(0x2A1F6E);

    // Card backgrounds
    public static final Color CARD_BG      = new Color(0x100D38);
    public static final Color CARD_BG2     = new Color(0x160E45);
    public static final Color FIELD_BG     = new Color(0x2C2468);

    // Text colors
    public static final Color TEXT_WHITE   = Color.WHITE;
    public static final Color TEXT_LABEL   = new Color(0xCCCCCC);
    public static final Color TEXT_MUTED   = new Color(0x9988CC);

    // Accent colors
    public static final Color ACCENT_GREEN  = new Color(0x00E676);
    public static final Color ACCENT_CYAN   = new Color(0x00BCD4);
    public static final Color ACCENT_YELLOW = new Color(0xFFC107);
    public static final Color ACCENT_RED    = new Color(0xFF3D3D);
    public static final Color ACCENT_PINK   = new Color(0xE91E8C);
    public static final Color ACCENT_PURPLE = new Color(0x9C27B0);

    // Button
    public static final Color BTN_PRIMARY  = new Color(0x6A3DB8);
    public static final Color BTN_HOVER    = new Color(0x7E4FCC);

    // Card border accent colors
    public static final Color BORDER_GREEN  = ACCENT_GREEN;
    public static final Color BORDER_YELLOW = ACCENT_YELLOW;
    public static final Color BORDER_RED    = ACCENT_RED;
    public static final Color BORDER_BLUE   = ACCENT_CYAN;

    // Fonts
    public static Font titleFont(float size) {
        return new Font("SansSerif", Font.BOLD, (int) size);
    }
    public static Font labelFont(float size) {
        return new Font("SansSerif", Font.PLAIN, (int) size);
    }
    public static Font boldFont(float size) {
        return new Font("SansSerif", Font.BOLD, (int) size);
    }
}
