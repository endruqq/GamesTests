package com.endruqq.imperium.game;

import com.endruqq.imperium.game.building.BuildingType;
import com.endruqq.imperium.game.building.Buildings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/** Builds every ready-made building in a street next to a position: a catalogue to look at (admin command). */
public final class Showcase {
    public static final int PER_ROW = 6;
    public static final int STEP_X = 18;
    public static final int STEP_Z = 24;

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
            // a street in front of every row
            int r = t.radius + 1;
            for (int dx = -STEP_X / 2; dx <= STEP_X / 2; dx++) {
                for (int dz = r; dz <= r + 3; dz++) {
                    BlockPos p = c.offset(dx, -1, dz);
                    level.setBlock(p, (dz == r + 3 ? Blocks.STONE_BRICKS : dz == r ? Blocks.SMOOTH_STONE : Blocks.GRAY_CONCRETE).defaultBlockState(), 2);
                    for (int y = 0; y < 6; y++) level.setBlock(p.above(y + 1), Blocks.AIR.defaultBlockState(), 2);
                }
            }
            PrefabBuilder.build(level, t, c, Direction.SOUTH, null);
            centres.add(c);
            i++;
        }
        return centres;
    }
}
