package com.endruqq.imperium.entity;

public enum OrderType {
    /** stand at the anchor point and defend it */
    HOLD,
    /** walk to a point, ignoring enemies */
    MOVE,
    /** walk to a point, fighting anything on the way */
    ATTACK_MOVE,
    /** attack a specific entity */
    ATTACK_TARGET,
    /** march to an enemy province core and destroy it */
    ATTACK_CORE,
    /** follow a player */
    FOLLOW,
    /** go back to the barracks */
    RETURN;

    public String key() {
        return "imperium.order." + name().toLowerCase();
    }

    public static OrderType byOrdinal(int o) {
        OrderType[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, o))];
    }
}
