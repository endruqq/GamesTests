package com.endruqq.imperium.game;

public enum RelationType {
    PEACE(0xBBBBBB),
    WAR(0xFF3030),
    ALLIANCE(0x40D060),
    NON_AGGRESSION(0x60A0FF);

    public final int color;

    RelationType(int color) {
        this.color = color;
    }

    public String key() {
        return "imperium.relation." + name().toLowerCase();
    }
}
