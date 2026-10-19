package com.endruqq.imperium.entity;

import net.minecraft.network.chat.Component;

public enum Profession {
    PEASANT, FARMER, WOODCUTTER, MINER, SMITH, MERCHANT, SCHOLAR, PRIEST;

    public String id() {
        return name().toLowerCase();
    }

    public Component title() {
        return Component.translatable("imperium.profession." + id());
    }

    public static Profession byOrdinal(int o) {
        Profession[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, o))];
    }
}
