package com.endruqq.imperium.game;

import com.endruqq.imperium.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Builds the defensive tower that protects a province core. */
public final class TowerBuilder {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private TowerBuilder() {}

    /** Finds solid ground for a tower near the given column. */
    public static BlockPos groundAt(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, Math.max(level.getMinBuildHeight() + 5, y), z);
    }

    public static boolean isWater(ServerLevel level, BlockPos ground) {
        return !level.getBlockState(ground.below()).getFluidState().isEmpty();
    }

    private static boolean soft(BlockState s) {
        return s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS);
    }

    /**
     * @param base the block above the ground at the tower's center; the core is placed there
     */
    public static void build(ServerLevel level, BlockPos base, Nation owner, boolean capital) {
        int half = capital ? 4 : 3;
        int height = capital ? 13 : 10;
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState cracked = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        BlockState floor = Blocks.POLISHED_ANDESITE.defaultBlockState();
        var rnd = level.random;
        // clear & foundation (a platform three blocks wider than the tower)
        for (int dx = -half - 3; dx <= half + 3; dx++) {
            for (int dz = -half - 3; dz <= half + 3; dz++) {
                boolean inner = Math.max(Math.abs(dx), Math.abs(dz)) <= half + 1;
                for (int y = 0; y <= (inner ? height + 4 : 3); y++) level.setBlock(base.offset(dx, y, dz), Blocks.AIR.defaultBlockState(), FLAGS);
                level.setBlock(base.offset(dx, -1, dz), (Math.max(Math.abs(dx), Math.abs(dz)) > half ? Blocks.COBBLESTONE : Blocks.STONE_BRICKS)
                        .defaultBlockState(), FLAGS);
                for (int y = -2; y >= -40; y--) {
                    BlockPos p = base.offset(dx, y, dz);
                    if (!soft(level.getBlockState(p))) break;
                    level.setBlock(p, Blocks.COBBLESTONE.defaultBlockState(), FLAGS);
                }
            }
        }
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                boolean edge = Math.abs(dx) == half || Math.abs(dz) == half;
                boolean corner = Math.abs(dx) == half && Math.abs(dz) == half;
                if (!edge) {
                    level.setBlock(base.offset(dx, -1, dz), floor, FLAGS);
                    level.setBlock(base.offset(dx, height, dz), wall, FLAGS);
                    continue;
                }
                for (int y = 0; y <= height; y++) {
                    BlockState s = corner ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
                            : rnd.nextInt(8) == 0 ? mossy : rnd.nextInt(10) == 0 ? cracked : wall;
                    level.setBlock(base.offset(dx, y, dz), s, FLAGS);
                }
                // crenellations
                if (((dx + dz) & 1) == 0) level.setBlock(base.offset(dx, height + 1, dz), wall, FLAGS);
            }
        }
        // entrances on all four sides, two blocks high
        for (int[] d : new int[][]{{0, half}, {0, -half}, {half, 0}, {-half, 0}}) {
            level.setBlock(base.offset(d[0], 0, d[1]), Blocks.AIR.defaultBlockState(), FLAGS);
            level.setBlock(base.offset(d[0], 1, d[1]), Blocks.AIR.defaultBlockState(), FLAGS);
            // arrow slits
            level.setBlock(base.offset(d[0], 5, d[1]), Blocks.AIR.defaultBlockState(), FLAGS);
        }
        // pedestal + core
        level.setBlock(base.below(), Blocks.GOLD_BLOCK.defaultBlockState(), FLAGS);
        level.setBlock(base, ModBlocks.PROVINCE_CORE.get().defaultBlockState(), Block.UPDATE_ALL);
        // lanterns
        for (int[] d : new int[][]{{1, 1}, {-1, 1}, {1, -1}, {-1, -1}}) {
            level.setBlock(base.offset(d[0] * (half - 1), height - 1, d[1] * (half - 1)),
                    Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), FLAGS);
        }
        if (capital) {
            level.setBlock(base.offset(2, 0, 0), ModBlocks.TREASURY.get().defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(base.offset(-2, 0, 0), Blocks.CRAFTING_TABLE.defaultBlockState(), FLAGS);
            level.setBlock(base.offset(0, 0, -2), Blocks.CHEST.defaultBlockState(), FLAGS);
        }
        paintFlags(level, base, owner, capital);
    }

    /** The coloured flag on top of the tower shows the current owner. */
    public static void paintFlags(ServerLevel level, BlockPos base, Nation owner, boolean capital) {
        if (owner == null || !level.isLoaded(base)) return;
        int height = capital ? 13 : 10;
        BlockState wool = owner.color().wool().defaultBlockState();
        BlockPos pole = base.above(height + 1);
        for (int y = 0; y < 4; y++) level.setBlock(pole.above(y), Blocks.OAK_FENCE.defaultBlockState(), FLAGS);
        for (int y = 2; y < 4; y++) {
            for (int dx = 1; dx <= 3; dx++) level.setBlock(pole.offset(dx, y, 0), wool, FLAGS);
        }
        // coloured ring of carpet around the core
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            BlockPos p = base.offset(d[0], 0, d[1]);
            BlockState s = level.getBlockState(p);
            if (s.isAir() || s.is(BlockTags.WOOL_CARPETS)) {
                level.setBlock(p, carpet(owner).defaultBlockState(), FLAGS);
            }
        }
    }

    private static Block carpet(Nation n) {
        return switch (n.color().dye) {
            case WHITE -> Blocks.WHITE_CARPET;
            case ORANGE -> Blocks.ORANGE_CARPET;
            case MAGENTA -> Blocks.MAGENTA_CARPET;
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_CARPET;
            case YELLOW -> Blocks.YELLOW_CARPET;
            case LIME -> Blocks.LIME_CARPET;
            case PINK -> Blocks.PINK_CARPET;
            case GRAY -> Blocks.GRAY_CARPET;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CARPET;
            case CYAN -> Blocks.CYAN_CARPET;
            case PURPLE -> Blocks.PURPLE_CARPET;
            case BLUE -> Blocks.BLUE_CARPET;
            case BROWN -> Blocks.BROWN_CARPET;
            case GREEN -> Blocks.GREEN_CARPET;
            case RED -> Blocks.RED_CARPET;
            case BLACK -> Blocks.BLACK_CARPET;
        };
    }
}
