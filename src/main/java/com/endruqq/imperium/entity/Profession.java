package com.endruqq.imperium.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** What a citizen does for a living. PEASANT means "no job yet" - such people can be given work. */
public enum Profession {
    PEASANT, FARMER, WOODCUTTER, MINER, SMITH, MERCHANT, SCHOLAR, PRIEST, WORKER, INNKEEPER, DOCTOR;

    public String id() {
        return name().toLowerCase();
    }

    public Component title() {
        return Component.translatable("imperium.profession." + id());
    }

    public boolean employed() {
        return this != PEASANT;
    }

    /** The tool a worker carries while at work. */
    public ItemStack tool() {
        return switch (this) {
            case FARMER -> new ItemStack(Items.IRON_HOE);
            case WOODCUTTER -> new ItemStack(Items.IRON_AXE);
            case MINER -> new ItemStack(Items.IRON_PICKAXE);
            case SMITH -> new ItemStack(Items.IRON_SHOVEL);
            case MERCHANT -> new ItemStack(Items.EMERALD);
            case SCHOLAR -> new ItemStack(Items.BOOK);
            case PRIEST -> new ItemStack(Items.CANDLE);
            case WORKER -> new ItemStack(Items.IRON_SHOVEL);
            case INNKEEPER -> new ItemStack(Items.HONEY_BOTTLE);
            case DOCTOR -> new ItemStack(Items.GLASS_BOTTLE);
            default -> ItemStack.EMPTY;
        };
    }

    public static Profession byOrdinal(int o) {
        Profession[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, o))];
    }
}
