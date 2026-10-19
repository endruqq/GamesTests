package com.endruqq.imperium.registry;

import com.endruqq.imperium.Imperium;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final TagKey<Block> MASONRY = block("masonry");
    public static final TagKey<Block> BUILDING_MATERIALS = block("building_materials");
    public static final TagKey<Block> STONE_BLOCKS = block("stone_blocks");

    public static final TagKey<Item> WOOD_RESOURCE = item("resource/wood");
    public static final TagKey<Item> STONE_RESOURCE = item("resource/stone");

    private static TagKey<Block> block(String name) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation(Imperium.MODID, name));
    }

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(Imperium.MODID, name));
    }

    private ModTags() {}
}
