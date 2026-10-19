package com.endruqq.imperium.game.building;

import com.endruqq.imperium.game.ResourceType;
import com.endruqq.imperium.game.Resources;
import com.endruqq.imperium.game.tech.Tech;
import com.endruqq.imperium.registry.ModTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.endruqq.imperium.game.building.BuildingCategory.*;
import static com.endruqq.imperium.game.building.Requirement.Mode.*;

/** All functional buildings of the game. */
public final class Buildings {
    private static final Map<String, BuildingType> TYPES = new LinkedHashMap<>();

    private static Resources cost(Object... pairs) {
        return Resources.of(pairs);
    }

    private static Requirement walls(int n) {
        return Requirement.tag("building_materials", ModTags.BUILDING_MATERIALS, Blocks.OAK_PLANKS, n, STRUCTURE);
    }

    private static Requirement masonry(int n) {
        return Requirement.tag("masonry", ModTags.MASONRY, Blocks.STONE_BRICKS, n, STRUCTURE);
    }

    private static Requirement beds(int n) {
        return Requirement.tag("beds", BlockTags.BEDS, Blocks.RED_BED, n, BED);
    }

    private static Requirement door() {
        return Requirement.tag("doors", BlockTags.DOORS, Blocks.SPRUCE_DOOR, 1, DOOR);
    }

    private static Requirement lanterns(int n) {
        return Requirement.blocks("lanterns", n, INTERIOR, Blocks.LANTERN, Blocks.SOUL_LANTERN);
    }

    private static Requirement chests(int n) {
        return Requirement.blocks("chests", n, INTERIOR, Blocks.CHEST, Blocks.TRAPPED_CHEST);
    }

    // ---------------------------------------------------------------- housing
    public static final BuildingType HOUSE = reg(new BuildingType.Builder("house", HOUSING, 4, 8, Items.RED_BED)
            .req(beds(1)).req(door()).req(walls(30)).housing(10).build());

    public static final BuildingType HUT = reg(new BuildingType.Builder("hut", HOUSING, 4, 7, Items.OAK_PLANKS)
            .req(Requirement.tag("planks", BlockTags.PLANKS, Blocks.OAK_PLANKS, 40, STRUCTURE))
            .req(Requirement.tag("logs", BlockTags.LOGS, Blocks.OAK_LOG, 8, STRUCTURE))
            .req(beds(2)).req(door()).req(Requirement.blocks("torches", 2, INTERIOR, Blocks.TORCH, Blocks.WALL_TORCH))
            .housing(4).prefab(cost(ResourceType.WOOD, 50)).build());

    public static final BuildingType STONE_HOUSE = reg(new BuildingType.Builder("stone_house", HOUSING, 4, 8, Items.STONE_BRICKS)
            .req(masonry(50)).req(Requirement.tag("planks", BlockTags.PLANKS, Blocks.SPRUCE_PLANKS, 20, STRUCTURE))
            .req(beds(3)).req(door()).req(Requirement.tag("glass_panes", Tags.Blocks.GLASS_PANES, Blocks.GLASS_PANE, 4, WINDOW))
            .req(lanterns(2))
            .housing(6).tech(Tech.ARCHITECTURE).prefab(cost(ResourceType.WOOD, 30, ResourceType.STONE, 70)).build());

    public static final BuildingType MANOR = reg(new BuildingType.Builder("manor", HOUSING, 5, 9, Items.BRICKS)
            .req(Requirement.block(Blocks.BRICKS, 60, STRUCTURE)).req(Requirement.tag("planks", BlockTags.PLANKS, Blocks.DARK_OAK_PLANKS, 40, STRUCTURE))
            .req(beds(5)).req(door()).req(Requirement.tag("glass_panes", Tags.Blocks.GLASS_PANES, Blocks.GLASS_PANE, 6, WINDOW))
            .req(lanterns(3)).req(Requirement.tag("flower_pots", BlockTags.FLOWER_POTS, Blocks.POTTED_POPPY, 2, INTERIOR))
            .housing(10).tech(Tech.ARCHITECTURE).prefab(cost(ResourceType.WOOD, 90, ResourceType.STONE, 120, ResourceType.GOLD, 20)).build());

    // ---------------------------------------------------------------- economy
    public static final BuildingType FARM = reg(new BuildingType.Builder("farm", ECONOMY, 5, 5, Items.WHEAT)
            .req(Requirement.block(Blocks.FARMLAND, 24, FIELD)).req(Requirement.water(2))
            .req(Requirement.block(Blocks.COMPOSTER, 1, INTERIOR))
            .style(BuildingType.Style.FARM).prefab(cost(ResourceType.WOOD, 40, ResourceType.STONE, 10, ResourceType.FOOD, 10)).build());

    public static final BuildingType LUMBER_MILL = reg(new BuildingType.Builder("lumber_mill", ECONOMY, 4, 8, Items.IRON_AXE)
            .req(Requirement.tag("logs", BlockTags.LOGS, Blocks.SPRUCE_LOG, 24, STRUCTURE)).req(Requirement.tag("planks", BlockTags.PLANKS, Blocks.SPRUCE_PLANKS, 16, STRUCTURE))
            .req(door()).req(Requirement.block(Blocks.CRAFTING_TABLE, 1, INTERIOR)).req(Requirement.block(Blocks.STONECUTTER, 1, INTERIOR))
            .req(chests(1))
            .prefab(cost(ResourceType.WOOD, 40, ResourceType.STONE, 20)).build());

    public static final BuildingType MINE = reg(new BuildingType.Builder("mine", ECONOMY, 4, 8, Items.IRON_PICKAXE)
            .req(Requirement.tag("stone_blocks", ModTags.STONE_BLOCKS, Blocks.COBBLESTONE, 40, STRUCTURE))
            .req(door()).req(Requirement.tag("rails", BlockTags.RAILS, Blocks.RAIL, 6, INTERIOR))
            .req(chests(1)).req(Requirement.blocks("torches", 4, INTERIOR, Blocks.TORCH, Blocks.WALL_TORCH))
            .tech(Tech.MINING).prefab(cost(ResourceType.WOOD, 40, ResourceType.STONE, 40)).build());

    public static final BuildingType SMELTERY = reg(new BuildingType.Builder("smeltery", ECONOMY, 4, 9, Items.BLAST_FURNACE)
            .req(masonry(40)).req(Requirement.tag("stone_blocks", ModTags.STONE_BLOCKS, Blocks.COBBLESTONE, 20, STRUCTURE))
            .req(door()).req(Requirement.block(Blocks.FURNACE, 3, INTERIOR)).req(Requirement.block(Blocks.BLAST_FURNACE, 1, INTERIOR))
            .req(Requirement.tag("anvils", BlockTags.ANVIL, Blocks.ANVIL, 1, INTERIOR))
            .req(Requirement.tag("cauldrons", BlockTags.CAULDRONS, Blocks.CAULDRON, 1, INTERIOR))
            .tech(Tech.METALLURGY).prefab(cost(ResourceType.STONE, 150, ResourceType.IRON, 20, ResourceType.WOOD, 30)).build());

    public static final BuildingType MARKET = reg(new BuildingType.Builder("market", ECONOMY, 5, 7, Items.EMERALD)
            .req(walls(30)).req(door()).req(Requirement.block(Blocks.BARREL, 4, INTERIOR)).req(chests(2))
            .req(Requirement.tag("wool", BlockTags.WOOL, Blocks.RED_WOOL, 6, INTERIOR)).req(lanterns(2))
            .tech(Tech.TRADE).prefab(cost(ResourceType.WOOD, 60, ResourceType.GOLD, 15)).build());

    public static final BuildingType GRANARY = reg(new BuildingType.Builder("granary", ECONOMY, 4, 8, Items.HAY_BLOCK)
            .req(walls(30)).req(door()).req(Requirement.block(Blocks.HAY_BLOCK, 12, INTERIOR)).req(Requirement.block(Blocks.BARREL, 4, INTERIOR))
            .tech(Tech.AGRICULTURE).prefab(cost(ResourceType.WOOD, 60, ResourceType.FOOD, 20)).build());

    public static final BuildingType BANK = reg(new BuildingType.Builder("bank", ECONOMY, 4, 8, Items.GOLD_BLOCK)
            .req(masonry(40)).req(door()).req(Requirement.block(Blocks.GOLD_BLOCK, 1, INTERIOR))
            .req(Requirement.block(Blocks.IRON_BARS, 6, WINDOW)).req(chests(4))
            .tech(Tech.BANKING).max(3).prefab(cost(ResourceType.STONE, 100, ResourceType.GOLD, 60, ResourceType.IRON, 20)).build());

    // ---------------------------------------------------------------- society
    public static final BuildingType TAVERN = reg(new BuildingType.Builder("tavern", SOCIETY, 4, 8, Items.HONEY_BOTTLE)
            .req(walls(30)).req(door()).req(Requirement.block(Blocks.BARREL, 4, INTERIOR))
            .req(Requirement.tag("campfires", BlockTags.CAMPFIRES, Blocks.CAMPFIRE, 1, INTERIOR)).req(lanterns(2))
            .prefab(cost(ResourceType.WOOD, 70, ResourceType.STONE, 20, ResourceType.FOOD, 20)).build());

    public static final BuildingType ACADEMY = reg(new BuildingType.Builder("academy", SOCIETY, 4, 8, Items.BOOKSHELF)
            .req(walls(30)).req(door()).req(Requirement.block(Blocks.BOOKSHELF, 10, INTERIOR)).req(Requirement.block(Blocks.LECTERN, 2, INTERIOR))
            .req(Requirement.block(Blocks.CARTOGRAPHY_TABLE, 1, INTERIOR))
            .tech(Tech.WRITING).prefab(cost(ResourceType.WOOD, 80, ResourceType.STONE, 40, ResourceType.GOLD, 10)).build());

    public static final BuildingType TEMPLE = reg(new BuildingType.Builder("temple", SOCIETY, 5, 10, Items.BELL)
            .req(masonry(60)).req(door()).req(Requirement.tag("candles", BlockTags.CANDLES, Blocks.CANDLE, 4, INTERIOR))
            .req(Requirement.block(Blocks.BELL, 1, INTERIOR)).req(Requirement.tag("glass_panes", Tags.Blocks.GLASS_PANES, Blocks.WHITE_STAINED_GLASS_PANE, 8, WINDOW))
            .tech(Tech.RELIGION).prefab(cost(ResourceType.STONE, 160, ResourceType.GOLD, 20)).build());

    public static final BuildingType HOSPITAL = reg(new BuildingType.Builder("hospital", SOCIETY, 4, 8, Items.GLISTERING_MELON_SLICE)
            .req(walls(30)).req(door()).req(beds(3)).req(Requirement.block(Blocks.BREWING_STAND, 1, INTERIOR))
            .req(Requirement.tag("cauldrons", BlockTags.CAULDRONS, Blocks.CAULDRON, 1, INTERIOR))
            .req(Requirement.tag("flower_pots", BlockTags.FLOWER_POTS, Blocks.POTTED_DANDELION, 2, INTERIOR))
            .tech(Tech.MEDICINE).prefab(cost(ResourceType.WOOD, 50, ResourceType.STONE, 50, ResourceType.GOLD, 15)).build());

    // ---------------------------------------------------------------- military
    public static final BuildingType BARRACKS = reg(new BuildingType.Builder("barracks", MILITARY, 4, 8, Items.IRON_SWORD)
            .req(walls(30)).req(door()).req(beds(3)).req(chests(1)).req(Requirement.block(Blocks.CRAFTING_TABLE, 1, INTERIOR))
            .req(Requirement.block(Blocks.HAY_BLOCK, 2, INTERIOR))
            .prefab(cost(ResourceType.WOOD, 60, ResourceType.STONE, 40)).build());

    public static final BuildingType ARCHERY_RANGE = reg(new BuildingType.Builder("archery_range", MILITARY, 4, 7, Items.BOW)
            .req(walls(24)).req(door()).req(Requirement.block(Blocks.TARGET, 2, INTERIOR)).req(Requirement.block(Blocks.FLETCHING_TABLE, 1, INTERIOR))
            .req(Requirement.block(Blocks.HAY_BLOCK, 4, INTERIOR))
            .tech(Tech.ARCHERY).prefab(cost(ResourceType.WOOD, 60)).build());

    public static final BuildingType ARMORY = reg(new BuildingType.Builder("armory", MILITARY, 4, 8, Items.SMITHING_TABLE)
            .req(masonry(40)).req(door()).req(Requirement.block(Blocks.SMITHING_TABLE, 1, INTERIOR))
            .req(Requirement.tag("anvils", BlockTags.ANVIL, Blocks.ANVIL, 1, INTERIOR)).req(Requirement.block(Blocks.GRINDSTONE, 1, INTERIOR))
            .req(Requirement.block(Blocks.IRON_BLOCK, 1, INTERIOR))
            .tech(Tech.STEEL_WEAPONS).prefab(cost(ResourceType.STONE, 80, ResourceType.IRON, 30, ResourceType.WOOD, 20)).build());

    public static final BuildingType SIEGE_WORKSHOP = reg(new BuildingType.Builder("siege_workshop", MILITARY, 4, 8, Items.TNT)
            .req(Requirement.tag("logs", BlockTags.LOGS, Blocks.DARK_OAK_LOG, 24, STRUCTURE)).req(door())
            .req(Requirement.block(Blocks.CRAFTING_TABLE, 1, INTERIOR)).req(Requirement.tag("anvils", BlockTags.ANVIL, Blocks.ANVIL, 1, INTERIOR))
            .req(Requirement.block(Blocks.TNT, 2, INTERIOR))
            .tech(Tech.SIEGE).prefab(cost(ResourceType.WOOD, 80, ResourceType.IRON, 20, ResourceType.STONE, 20)).build());

    public static final BuildingType WATCHTOWER = reg(new BuildingType.Builder("watchtower", MILITARY, 3, 14, Items.SPYGLASS)
            .req(walls(40)).req(door()).req(Requirement.block(Blocks.LADDER, 6, VERTICAL)).req(lanterns(1))
            .prefab(cost(ResourceType.WOOD, 30, ResourceType.STONE, 40)).build());

    public static final BuildingType FORTRESS = reg(new BuildingType.Builder("fortress", MILITARY, 5, 10, Items.STONE_BRICK_WALL)
            .req(masonry(80)).req(door()).req(Requirement.block(Blocks.IRON_BARS, 8, WINDOW))
            .req(Requirement.tag("walls", BlockTags.WALLS, Blocks.STONE_BRICK_WALL, 10, ROOF_EDGE))
            .tech(Tech.FORTIFICATION).max(3).prefab(cost(ResourceType.STONE, 300, ResourceType.IRON, 30)).build());

    private static BuildingType reg(BuildingType t) {
        TYPES.put(t.id, t);
        return t;
    }

    public static BuildingType get(String id) {
        return TYPES.get(id);
    }

    public static Collection<BuildingType> all() {
        return Collections.unmodifiableCollection(TYPES.values());
    }

    private Buildings() {}
}
