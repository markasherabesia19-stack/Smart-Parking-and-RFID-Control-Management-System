package util;

import java.awt.Color;

/**
 * SPARCS — Shared UI color palette and theme constants.
 * All screens reference this class for consistent styling.
 */
public class UIConstants {

    // ── Background Colors ────────────────────────────────────────────────────
    public static final Color C_BG_DARK   = new Color(26, 16, 64);      // outer gradient dark
    public static final Color C_BG_PANEL  = new Color(45, 27, 110);     // outer gradient mid
    public static final Color C_BG_CARD   = new Color(30, 22, 70, 255); // frosted card (with alpha)
    public static final Color C_SIDEBAR   = new Color(14, 11, 38);

    // ── Accent Colors ────────────────────────────────────────────────────────
    public static final Color C_PURPLE    = new Color(127, 119, 221);   // primary button / badge
    public static final Color C_PINK      = new Color(210, 50, 140);
    public static final Color C_ACCENT    = new Color(127, 119, 221);   // matches C_PURPLE now

    // ── Text Colors ──────────────────────────────────────────────────────────
    public static final Color C_WHITE     = new Color(240, 235, 255);
    public static final Color C_MUTED     = new Color(175, 169, 236);   // softer purple-white

    // ── Status Colors ────────────────────────────────────────────────────────
    public static final Color C_AVAILABLE = new Color(60, 210, 130);
    public static final Color C_OCCUPIED  = new Color(220, 70, 90);
    public static final Color C_RESERVED  = new Color(255, 165, 50);

    // ── Input Field Colors ───────────────────────────────────────────────────
    public static final Color C_INPUT_BG  = new Color(255, 255, 255, 18);  // subtle white tint
    public static final Color C_INPUT_BD  = new Color(175, 169, 236, 80);  // muted purple border

    // ── Card Border ──────────────────────────────────────────────────────────
    public static final Color C_CARD_BD   = new Color(175, 169, 236, 64);  // frosted card border

    // ── Slot Constants ───────────────────────────────────────────────────────
    public static final int TOTAL_SLOTS = 40;

    private UIConstants() {} // Utility class — no instantiation
}