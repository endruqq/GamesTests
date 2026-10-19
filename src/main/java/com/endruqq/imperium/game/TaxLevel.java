package com.endruqq.imperium.game;

public enum TaxLevel {
    LOW(0.6, 12),
    NORMAL(1.0, 0),
    HIGH(1.5, -15);

    public final double goldMult;
    public final double happiness;

    TaxLevel(double goldMult, double happiness) {
        this.goldMult = goldMult;
        this.happiness = happiness;
    }

    public String key() {
        return "imperium.tax." + name().toLowerCase();
    }

    public static TaxLevel byOrdinal(int o) {
        TaxLevel[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, o))];
    }
}
