package com.endruqq.imperium.game;

import com.endruqq.imperium.entity.CitizenEntity;
import com.endruqq.imperium.game.building.Building;
import com.endruqq.imperium.game.tech.Tech;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

/**
 * Underground work of miners: every miner digs his own gallery from the mine entrance - first a staircase down, then a
 * straight gallery, then deeper again. Deeper means richer ore, but also lava pockets, cave-ins and caverns.
 */
public final class MineWorks {
    private static final int GALLERY_LENGTH = 20;
    private static final int STEP_DOWN = 6;

    private MineWorks() {}

    public static Building.Tunnel tunnel(ServerLevel level, Building b, int index) {
        while (b.tunnels.size() <= index) b.tunnels.add(newTunnel(level, b, b.tunnels.size()));
        return b.tunnels.get(index);
    }

    private static Building.Tunnel newTunnel(ServerLevel level, Building b, int i) {
        BlockPos start = null;
        Direction dir = null;
        PrefabTemplates.Meta m = b.facing == null ? null : PrefabTemplates.get(b.typeId);
        if (m != null && m.tunnel() != null) {
            int[] t = m.tunnel();
            start = PrefabBuilder.templateToWorld(b.pos, b.facing, b.radius(), m.floor(), t[0], t[1], t[2]);
            dir = PrefabBuilder.rotate(b.facing, Direction.from2DDataValue(t[3]));
        }
        if (start == null) {
            // hand-built mine: start next to the construction site, where there is room to stand
            dir = Direction.EAST;
            start = b.pos.relative(dir);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos p = b.pos.relative(d);
                if (level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                        && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) {
                    start = p;
                    dir = d;
                    break;
                }
            }
        }
        Building.Tunnel t = new Building.Tunnel();
        t.cursor = start;
        t.dir = i == 0 ? dir : i == 1 ? dir.getCounterClockWise() : dir.getClockWise();
        t.mode = 0;
        t.targetY = Math.max(level.getMinBuildHeight() + 8, start.getY() - 12);
        return t;
    }

    /** feet position of the next step */
    public static BlockPos feet(Building.Tunnel t) {
        BlockPos f = t.cursor.relative(t.dir);
        return t.mode == 0 ? f.below() : f;
    }

    /** blocks that must be cleared for the next step, top first */
    public static List<BlockPos> stepBlocks(Building.Tunnel t) {
        BlockPos f = feet(t);
        List<BlockPos> l = new ArrayList<>();
        if (t.mode == 0) l.add(f.above(2));
        l.add(f.above());
        l.add(f);
        return l;
    }

    public static boolean passable(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.getCollisionShape(level, p).isEmpty() && s.getFluidState().isEmpty();
    }

    /** May a miner dig here at all? */
    public static boolean diggable(ServerLevel level, GameData d, Building b, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (s.getDestroySpeed(level, p) < 0 || s.hasBlockEntity()) return false;
        if (s.is(Blocks.OBSIDIAN) || s.is(Blocks.CRYING_OBSIDIAN)) return false;
        if (p.getY() <= level.getMinBuildHeight() + 4) return false;
        int owner = d.nationIdAt(p.getX() + 0.5, p.getZ() + 0.5);
        if (owner != b.nation) return false;
        Building other = d.buildingContaining(p);
        return other == null || other.id == b.id;
    }

    /** 0 = safe, 1 = water nearby, 2 = lava nearby */
    public static int hazard(ServerLevel level, BlockPos p) {
        int h = 0;
        for (Direction dir : Direction.values()) {
            FluidState f = level.getFluidState(p.relative(dir));
            if (f.is(FluidTags.LAVA)) return 2;
            if (f.is(FluidTags.WATER)) h = 1;
        }
        FluidState own = level.getFluidState(p);
        if (own.is(FluidTags.LAVA)) return 2;
        if (own.is(FluidTags.WATER)) h = 1;
        return h;
    }

    /** Walls off liquids around a block with cobblestone. */
    public static void seal(ServerLevel level, BlockPos p) {
        for (Direction dir : Direction.values()) {
            BlockPos q = p.relative(dir);
            if (!level.getFluidState(q).isEmpty()) level.setBlock(q, Blocks.COBBLESTONE.defaultBlockState(), 3);
        }
        if (!level.getFluidState(p).isEmpty()) level.setBlock(p, Blocks.COBBLESTONE.defaultBlockState(), 3);
    }

    /** Hardness of a block for a miner, in ticks. */
    public static int breakTicks(ServerLevel level, BlockPos p) {
        float h = level.getBlockState(p).getDestroySpeed(level, p);
        return Math.max(6, (int) (h * 14));
    }

    /** What the nation earns for a dug block. */
    public static void reward(ServerLevel level, GameData d, Nation n, BlockState s, BlockPos at) {
        if (n == null) return;
        if (s.is(Tags.Blocks.ORES_IRON)) n.stock.add(ResourceType.IRON, 3);
        else if (s.is(Tags.Blocks.ORES_GOLD)) n.stock.add(ResourceType.GOLD, 3);
        else if (s.is(Tags.Blocks.ORES_COAL)) n.stock.add(ResourceType.GOLD, 1);
        else if (s.is(Tags.Blocks.ORES_COPPER)) n.stock.add(ResourceType.IRON, 1);
        else if (s.is(Tags.Blocks.ORES_REDSTONE) || s.is(Tags.Blocks.ORES_LAPIS)) n.research += 2;
        else if (s.is(Tags.Blocks.ORES_DIAMOND) || s.is(Tags.Blocks.ORES_EMERALD)) {
            n.stock.add(ResourceType.GOLD, 12);
            Notify.nation(level.getServer(), d, n.id, Component.translatable("imperium.msg.gems_found", at.getY()).withStyle(ChatFormatting.AQUA), true);
        } else if (s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Tags.Blocks.STONE)) n.stock.add(ResourceType.STONE, 0.3);
    }

    /** The step is clear: move the cursor forward, lay the floor, light the gallery, maybe turn. */
    public static void advance(ServerLevel level, GameData d, Building b, Building.Tunnel t) {
        BlockPos f = feet(t);
        BlockPos below = f.below();
        if (passable(level, below) || !level.getFluidState(below).isEmpty()) {
            level.setBlock(below, Blocks.COBBLESTONE.defaultBlockState(), 3);
        }
        t.cursor = f;
        t.steps++;
        if (t.mode == 1) {
            if (level.getBlockState(f).isAir()) level.setBlock(f, Blocks.RAIL.defaultBlockState(), 3);
            if (t.steps % 6 == 3) torch(level, t);
        } else if (t.steps % 5 == 0) {
            torch(level, t);
        }
        if (t.mode == 0 && t.cursor.getY() <= t.targetY) {
            t.mode = 1;
            t.steps = 0;
        } else if (t.mode == 1 && t.steps >= GALLERY_LENGTH) {
            turn(level, t);
        }
    }

    /** Ends a gallery: turn and dig deeper. */
    public static void turn(ServerLevel level, Building.Tunnel t) {
        t.branch++;
        t.dir = t.branch % 2 == 0 ? t.dir.getClockWise() : t.dir.getCounterClockWise();
        t.steps = 0;
        int deeper = t.cursor.getY() - STEP_DOWN;
        if (deeper > level.getMinBuildHeight() + 8) {
            t.mode = 0;
            t.targetY = deeper;
        } else {
            t.mode = 1;
        }
    }

    private static void torch(ServerLevel level, Building.Tunnel t) {
        BlockPos at = t.cursor.above();
        Direction wallSide = t.dir.getClockWise();
        BlockPos wall = at.relative(wallSide);
        if (!level.getBlockState(at).isAir() || !level.getBlockState(wall).isFaceSturdy(level, wall, wallSide.getOpposite())) return;
        level.setBlock(at, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, wallSide.getOpposite()), 3);
    }

    /** Depth of the deepest gallery below the entrance. */
    public static int depth(Building b) {
        int best = 0;
        for (Building.Tunnel t : b.tunnels) best = Math.max(best, b.pos.getY() - t.cursor.getY());
        return best;
    }

    // ------------------------------------------------------------------ accidents

    /** Chance that the gallery collapses behind a miner after a step. */
    public static double caveInChance(Nation n, Building.Tunnel t) {
        if (t.steps < 5) return 0;
        double c = 0.0025 + 0.0005 * t.branch;
        if (n != null && n.has(Tech.INDUSTRY)) c *= 0.6;
        return Math.min(0.006, c);
    }

    /** Fills the gallery a few steps behind the miner. @return rubble positions */
    public static List<BlockPos> collapse(ServerLevel level, Building.Tunnel t) {
        List<BlockPos> rubble = new ArrayList<>();
        Direction back = t.dir.getOpposite();
        for (int k = 3; k <= 4; k++) {
            BlockPos p = t.cursor.relative(back, k);
            if (t.mode == 0) p = p.above(k);
            for (int y = 0; y <= 2; y++) {
                BlockPos q = p.above(y);
                BlockState s = level.getBlockState(q);
                if (s.getCollisionShape(level, q).isEmpty() && s.getFluidState().isEmpty()) {
                    level.setBlock(q, (y == 2 ? Blocks.GRAVEL : Blocks.COBBLESTONE).defaultBlockState(), 3);
                    rubble.add(q.immutable());
                }
            }
        }
        BlockPos c = t.cursor.relative(back, 3);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, 20, 0.6, 0.6, 0.6, 0.02);
        level.playSound(null, c, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.7F, 0.6F);
        level.playSound(null, c, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.0F, 0.5F);
        return rubble;
    }

    /** The miner broke into a lava pocket. */
    public static void lavaAccident(ServerLevel level, CitizenEntity miner, BlockPos at) {
        level.sendParticles(ParticleTypes.LAVA, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 25, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(ParticleTypes.FLAME, miner.getX(), miner.getY() + 1, miner.getZ(), 30, 0.3, 0.8, 0.3, 0.05);
        level.playSound(null, at, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.5F, 0.8F);
        level.playSound(null, at, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.6F);
        miner.setSecondsOnFire(8);
        // the pocket cools down where it touched the gallery
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-2, -2, -2), at.offset(2, 2, 2))) {
            if (level.getFluidState(p).is(FluidTags.LAVA)) level.setBlock(p, Blocks.OBSIDIAN.defaultBlockState(), 3);
        }
        miner.dieOf("lava", level.damageSources().lava());
    }
}
