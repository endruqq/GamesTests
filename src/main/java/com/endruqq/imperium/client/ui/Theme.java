package com.endruqq.imperium.client.ui;

/** Colours of the Imperium interface: a dark slate theme with a gold accent. */
public final class Theme {
    public static final int SCRIM = 0xA0080A0E;
    public static final int PANEL = 0xEC151A21;
    public static final int PANEL_SOLID = 0xFF151A21;
    public static final int CARD = 0xFF1C232C;
    public static final int CARD_HOVER = 0xFF253040;
    public static final int CARD_SELECTED = 0xFF2C3A4E;
    public static final int HEADER = 0xFF1A2029;
    public static final int BORDER = 0xFF2E3946;
    public static final int BORDER_LIGHT = 0xFF465568;
    public static final int HIGHLIGHT = 0x18FFFFFF;
    public static final int SHADOW = 0x60000000;

    public static final int ACCENT = 0xFFE3B341;
    public static final int ACCENT_DARK = 0xFF7A5E1E;
    public static final int TEXT = 0xFFE8ECF1;
    public static final int TEXT_DIM = 0xFFA3ADBA;
    public static final int TEXT_MUTED = 0xFF6E7887;

    public static final int GOOD = 0xFF52C98A;
    public static final int BAD = 0xFFE5534B;
    public static final int WARN = 0xFFE8A33D;
    public static final int INFO = 0xFF5AA9E6;
    public static final int PURPLE = 0xFFB07CE8;

    public static final int BTN = 0xFF27313D;
    public static final int BTN_HOVER = 0xFF33414F;
    public static final int BTN_DISABLED = 0xFF1A2028;
    public static final int BTN_PRIMARY = 0xFF8A6A1F;
    public static final int BTN_PRIMARY_HOVER = 0xFFA77F24;
    public static final int BTN_DANGER = 0xFF7A2A2A;
    public static final int BTN_DANGER_HOVER = 0xFF963434;
    public static final int BTN_SUCCESS = 0xFF24603F;
    public static final int BTN_SUCCESS_HOVER = 0xFF2E7A50;

    private Theme() {}

    /** Same colour with another alpha (0..255). */
    public static int alpha(int argb, int a) {
        return (a & 255) << 24 | (argb & 0xFFFFFF);
    }

    public static int mix(int a, int b, double t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        int r = (int) (ar + (br - ar) * t), g = (int) (ag + (bg - ag) * t), bl = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
}
