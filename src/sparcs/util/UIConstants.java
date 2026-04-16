package util;

import java.awt.Color;

/**
 * SPARCS — Shared UI color palette and theme constants.
 * All screens reference this class for consistent styling.
 */
public class UIConstants {

    // ── Background Colors ────────────────────────────────────────────────────
    public static final Color C_BG_DARK   = new Color(10, 8, 30);
    public static final Color C_BG_PANEL  = new Color(18, 14, 48);
    public static final Color C_BG_CARD   = new Color(25, 20, 65);
    public static final Color C_SIDEBAR   = new Color(14, 11, 38);

    // ── Accent Colors ────────────────────────────────────────────────────────
    public static final Color C_PURPLE    = new Color(105, 48, 195);
    public static final Color C_PINK      = new Color(210, 50, 140);
    public static final Color C_ACCENT    = new Color(160, 70, 230);

    // ── Text Colors ──────────────────────────────────────────────────────────
    public static final Color C_WHITE     = new Color(240, 235, 255);
    public static final Color C_MUTED     = new Color(140, 130, 180);

    // ── Status Colors ────────────────────────────────────────────────────────
    public static final Color C_AVAILABLE = new Color(60, 210, 130);
    public static final Color C_OCCUPIED  = new Color(220, 70, 90);
    public static final Color C_RESERVED  = new Color(255, 165, 50);

    // ── Input Field Colors ───────────────────────────────────────────────────
    public static final Color C_INPUT_BG  = new Color(30, 24, 75);
    public static final Color C_INPUT_BD  = new Color(80, 60, 140);

    // ── Slot Constants ───────────────────────────────────────────────────────
    public static final int TOTAL_SLOTS = 40;

    private UIConstants() {} // Utility class — no instantiation
}
