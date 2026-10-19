package com.endruqq.imperium.game.tech;

import com.endruqq.imperium.game.Nation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * The technology ("skill") tree. Column/row give the position in the tree screen.
 * Branches: economy (columns 0-1), society (2-3), military (4-5).
 */
public enum Tech {
    AGRICULTURE(0, 0, 15, Items.WHEAT),
    MINING(1, 0, 15, Items.IRON_PICKAXE),
    WRITING(2, 0, 15, Items.WRITABLE_BOOK),
    ARCHITECTURE(3, 0, 25, Items.BRICKS),
    ARCHERY(4, 0, 20, Items.BOW),
    FORTIFICATION(5, 0, 30, Items.STONE_BRICK_WALL),

    TRADE(0, 1, 30, Items.EMERALD, AGRICULTURE),
    METALLURGY(1, 1, 40, Items.BLAST_FURNACE, MINING),
    RELIGION(2, 1, 30, Items.BELL, WRITING),
    DIPLOMACY(3, 1, 30, Items.WRITTEN_BOOK, WRITING),
    STEEL_WEAPONS(4, 1, 60, Items.IRON_SWORD, METALLURGY),
    SIEGE(5, 1, 60, Items.TNT, FORTIFICATION),

    BANKING(0, 2, 60, Items.GOLD_BLOCK, TRADE),
    INDUSTRY(1, 2, 90, Items.PISTON, METALLURGY),
    MEDICINE(2, 2, 50, Items.BREWING_STAND, RELIGION),
    ADMINISTRATION(3, 2, 70, Items.LECTERN, DIPLOMACY, ARCHITECTURE),
    TACTICS(4, 2, 80, Items.SHIELD, STEEL_WEAPONS, ARCHERY),
    GUNPOWDER(5, 2, 90, Items.GUNPOWDER, SIEGE),

    GUILDS(0, 3, 120, Items.SMITHING_TABLE, BANKING, INDUSTRY),
    ENLIGHTENMENT(2, 3, 120, Items.ENCHANTED_BOOK, MEDICINE, ADMINISTRATION),
    MILITARY_SERVICE(4, 3, 100, Items.IRON_CHESTPLATE, TACTICS, ADMINISTRATION),

    MASS_PRODUCTION(1, 3, 110, Items.HOPPER, INDUSTRY),
    URBANIZATION(3, 3, 110, Items.LANTERN, ADMINISTRATION),
    ESPIONAGE(5, 3, 100, Items.ENDER_EYE, DIPLOMACY, TACTICS);

    public final int column;
    public final int row;
    public final int cost;
    private final Item icon;
    private final Tech[] prerequisites;

    Tech(int column, int row, int cost, Item icon, Tech... prerequisites) {
        this.column = column;
        this.row = row;
        this.cost = cost;
        this.icon = icon;
        this.prerequisites = prerequisites;
    }

    public List<Tech> prerequisites() {
        return List.of(prerequisites);
    }

    public ItemStack icon() {
        return new ItemStack(icon);
    }

    public String id() {
        return name().toLowerCase();
    }

    public Component title() {
        return Component.translatable("imperium.tech." + id());
    }

    public Component description() {
        return Component.translatable("imperium.tech." + id() + ".desc");
    }

    public boolean available(Nation n) {
        if (n.has(this)) return false;
        for (Tech p : prerequisites) if (!n.has(p)) return false;
        return true;
    }

    public static Tech byId(String id) {
        for (Tech t : values()) if (t.id().equals(id) || t.name().equals(id)) return t;
        return null;
    }
}
