package com.endruqq.imperium.entity;

public enum Stance {
    /** chase enemies far away from the anchor */
    AGGRESSIVE(28),
    /** engage only close enemies, keep formation */
    DEFENSIVE(12),
    /** never start a fight */
    PASSIVE(0);

    public final double leash;

    Stance(double leash) {
        this.leash = leash;
    }

    public String key() {
        return "imperium.stance." + name().toLowerCase();
    }

    public static Stance byOrdinal(int o) {
        Stance[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, o))];
    }
}
