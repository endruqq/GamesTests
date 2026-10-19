package com.endruqq.imperium.game;

import com.endruqq.imperium.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum ResourceType {
    WOOD(0xA0763A),
    STONE(0x9A9A9A),
    IRON(0xD8D8D8),
    GOLD(0xF5D442),
    STEEL(0x7E8FA3),
    FOOD(0xE08A3C);

    public final int color;

    ResourceType(int color) {
        this.color = color;
    }

    public String id() {
        return name().toLowerCase();
    }

    public Component label() {
        return Component.translatable("imperium.resource." + id());
    }

    public ItemStack icon() {
        return switch (this) {
            case WOOD -> new ItemStack(Items.OAK_LOG);
            case STONE -> new ItemStack(Items.COBBLESTONE);
            case IRON -> new ItemStack(Items.IRON_INGOT);
            case GOLD -> new ItemStack(Items.GOLD_INGOT);
            case STEEL -> new ItemStack(ModItems.STEEL_INGOT.get());
            case FOOD -> new ItemStack(Items.BREAD);
        };
    }

    public static ResourceType byId(String id) {
        for (ResourceType t : values()) if (t.id().equals(id)) return t;
        return null;
    }
}
