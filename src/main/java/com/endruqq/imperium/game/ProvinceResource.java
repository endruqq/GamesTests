package com.endruqq.imperium.game;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Natural wealth of a province: a reason to want it. */
public enum ProvinceResource {
    NONE(null, 0),
    FERTILE(ResourceType.FOOD, 5),
    FOREST(ResourceType.WOOD, 6),
    QUARRY(ResourceType.STONE, 6),
    IRON(ResourceType.IRON, 2.5),
    GOLD(ResourceType.GOLD, 4);

    public final ResourceType resource;
    public final double bonus;

    ProvinceResource(ResourceType resource, double bonus) {
        this.resource = resource;
        this.bonus = bonus;
    }

    public String id() {
        return name().toLowerCase();
    }

    public Component title() {
        return Component.translatable("imperium.presource." + id());
    }

    public ItemStack icon() {
        return switch (this) {
            case FERTILE -> new ItemStack(Items.WHEAT);
            case FOREST -> new ItemStack(Items.SPRUCE_SAPLING);
            case QUARRY -> new ItemStack(Items.STONE_BRICKS);
            case IRON -> new ItemStack(Items.RAW_IRON);
            case GOLD -> new ItemStack(Items.RAW_GOLD);
            default -> ItemStack.EMPTY;
        };
    }

    public static ProvinceResource byOrdinal(int o) {
        ProvinceResource[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, o))];
    }
}
