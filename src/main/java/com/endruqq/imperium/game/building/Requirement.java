package com.endruqq.imperium.game.building;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.function.Predicate;

/** "N blocks of X must be present inside the plot". */
public class Requirement {
    /** How the prefab generator places this requirement. */
    public enum Mode {
        /** wall material (first structure requirement) or roof/floor material (second) */
        STRUCTURE,
        /** single blocks standing on the floor along the walls */
        INTERIOR,
        /** two-block beds */
        BED,
        /** the entrance door */
        DOOR,
        /** window openings */
        WINDOW,
        /** ladders climbing a wall */
        VERTICAL,
        /** crenellations on the roof edge */
        ROOF_EDGE,
        /** farmland field (farm style) */
        FIELD,
        /** water source blocks (farm style) */
        WATER
    }

    public final String id;
    public final int count;
    public final Mode mode;
    private final Predicate<BlockState> test;
    private final Block placeBlock;
    private final Block nameBlock;

    private Requirement(String id, int count, Mode mode, Predicate<BlockState> test, Block placeBlock, Block nameBlock) {
        this.id = id;
        this.count = count;
        this.mode = mode;
        this.test = test;
        this.placeBlock = placeBlock;
        this.nameBlock = nameBlock;
    }

    public static Requirement block(Block block, int count, Mode mode) {
        return new Requirement(blockId(block), count, mode, s -> s.is(block), block, block);
    }

    public static Requirement blocks(String id, int count, Mode mode, Block... blocks) {
        return new Requirement(id, count, mode, s -> {
            for (Block b : blocks) if (s.is(b)) return true;
            return false;
        }, blocks[0], null);
    }

    public static Requirement tag(String id, TagKey<Block> tag, Block place, int count, Mode mode) {
        return new Requirement(id, count, mode, s -> s.is(tag), place, null);
    }

    public static Requirement water(int count) {
        return new Requirement("water", count, Mode.WATER,
                s -> s.getFluidState().is(FluidTags.WATER) && s.getFluidState().isSource(), Blocks.WATER, null);
    }

    private static String blockId(Block b) {
        String s = b.getDescriptionId();
        int i = s.lastIndexOf('.');
        return i >= 0 ? s.substring(i + 1) : s;
    }

    /** Counts a block state, ignoring the second half of multi-block blocks so that a door counts once. */
    public boolean matches(BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) return false;
        if (state.hasProperty(BlockStateProperties.BED_PART)
                && state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT) return false;
        return test.test(state);
    }

    public Block placeBlock() {
        return placeBlock;
    }

    public ItemStack icon() {
        if (placeBlock == Blocks.WATER) return new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET);
        if (placeBlock == Blocks.FARMLAND) return new ItemStack(net.minecraft.world.item.Items.DIRT);
        ItemStack s = new ItemStack(placeBlock.asItem());
        return s.isEmpty() ? new ItemStack(net.minecraft.world.item.Items.FLOWER_POT) : s;
    }

    public Component name() {
        if (nameBlock != null) return nameBlock.getName();
        return Component.translatable("imperium.req." + id);
    }
}
