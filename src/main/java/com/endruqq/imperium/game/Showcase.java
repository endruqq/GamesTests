package com.endruqq.imperium.game;

import com.endruqq.imperium.game.building.BuildingType;
import com.endruqq.imperium.game.building.Buildings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/** Builds every ready-made building in a street next to a position: a catalogue to look at (admin command). */
public final class Showcase {
    public static final int PER_ROW = 6;
    public static final int STEP_X = 18;
    public static final int STEP_Z = 40;

    private Showcase() {}

    /** @return centres of the built buildings, in {@link Buildings#all()} order */
    public static List<BlockPos> build(ServerLevel level, BlockPos origin) {
        List<BlockPos> centres = new ArrayList<>();
        int baseY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
        int i = 0;
        for (BuildingType t : Buildings.all()) {
            if (t.prefabCost == null) continue;
            int x = origin.getX() + 12 + (i % PER_ROW) * STEP_X;
            int z = origin.getZ() + (i / PER_ROW) * STEP_Z;
            BlockPos c = new BlockPos(x, baseY, z);
            plot(level, c, t.radius + 1);
            PrefabBuilder.build(level, t, c, Direction.SOUTH, null);
            centres.add(c);
            i++;
        }
        return centres;
    }

    /** A flat lawn with a pavement and a street in front, cleared of trees and terrain. */
    private static void plot(ServerLevel level, BlockPos c, int r) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -STEP_X / 2; dx < STEP_X / 2; dx++) {
            for (int dz = -STEP_Z / 2; dz < STEP_Z / 2; dz++) {
                BlockPos ground = c.offset(dx, -1, dz);
                BlockState top = dz == r || dz == r + 4 ? Blocks.SMOOTH_STONE.defaultBlockState()
                        : dz > r && dz < r + 4 ? Blocks.GRAY_CONCRETE.defaultBlockState()
                        : dz == r + 5 || (dx == -STEP_X / 2 && dz < r) ? Blocks.STONE_BRICKS.defaultBlockState()
                        : Blocks.GRASS_BLOCK.defaultBlockState();
                level.setBlock(ground, top, 2);
                for (int y = 1; y <= 4; y++) {
                    BlockPos below = ground.below(y);
                    if (level.getBlockState(below).isSolidRender(level, below)) break;
                    level.setBlock(below, (y == 1 && top.is(Blocks.GRASS_BLOCK) ? Blocks.DIRT : Blocks.STONE).defaultBlockState(), 2);
                }
                for (int y = 0; y < 32; y++) {
                    BlockPos p = ground.above(y + 1);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, air, 2);
                }
            }
        }
    }
}
