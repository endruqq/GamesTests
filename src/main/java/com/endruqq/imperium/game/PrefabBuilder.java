package com.endruqq.imperium.game;

import com.endruqq.imperium.game.building.BuildingType;
import com.endruqq.imperium.game.building.Requirement;
import com.endruqq.imperium.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates ready-made ("prefab") buildings bought in RTS mode. The generator places exactly the blocks the
 * building's requirements ask for, so a prefab passes the same check as a hand-made building.
 */
public final class PrefabBuilder {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private PrefabBuilder() {}

    private record Ctx(ServerLevel level, BlockPos c, Direction front, Direction right, List<BlockPos> fix) {
        BlockPos at(int lx, int y, int lz) {
            return c.offset(right.getStepX() * lx + front.getStepX() * lz, y, right.getStepZ() * lx + front.getStepZ() * lz);
        }

        void set(int lx, int y, int lz, BlockState s) {
            BlockPos p = at(lx, y, lz);
            level.setBlock(p, s, FLAGS);
            if (s.hasProperty(BlockStateProperties.NORTH) || s.hasProperty(BlockStateProperties.NORTH_WALL)) fix.add(p);
        }

        BlockState get(int lx, int y, int lz) {
            return level.getBlockState(at(lx, y, lz));
        }

        /** local direction (dx, dz) to world direction */
        Direction dir(int dx, int dz) {
            if (dx > 0) return right;
            if (dx < 0) return right.getOpposite();
            if (dz > 0) return front;
            return front.getOpposite();
        }
    }

    public static void build(ServerLevel level, BuildingType type, BlockPos center, Direction front, Nation nation) {
        if (front == null || front.getAxis() == Direction.Axis.Y) front = Direction.SOUTH;
        Ctx ctx = new Ctx(level, center, front, front.getClockWise(), new ArrayList<>());
        prepareGround(ctx, type);
        if (type.style == BuildingType.Style.FARM) buildFarm(ctx, type);
        else buildShell(ctx, type);
        for (BlockPos p : ctx.fix) {
            BlockState s = level.getBlockState(p);
            BlockState u = Block.updateFromNeighbourShapes(s, level, p);
            if (u != s) level.setBlock(p, u, FLAGS);
        }
        level.setBlock(center, ModBlocks.CONSTRUCTION_SITE.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    private static boolean soft(BlockState s) {
        return s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty() || s.is(net.minecraft.tags.BlockTags.LEAVES)
                || s.is(net.minecraft.tags.BlockTags.LOGS);
    }

    private static void prepareGround(Ctx ctx, BuildingType type) {
        int r = type.radius;
        int top = type.height + 3;
        for (int lx = -r; lx <= r; lx++) {
            for (int lz = -r; lz <= r; lz++) {
                for (int y = 0; y <= top; y++) ctx.set(lx, y, lz, Blocks.AIR.defaultBlockState());
                ctx.set(lx, -1, lz, Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = -2; y >= -8; y--) {
                    if (!soft(ctx.get(lx, y, lz))) break;
                    ctx.set(lx, y, lz, (y == -2 ? Blocks.DIRT : Blocks.COBBLESTONE).defaultBlockState());
                }
            }
        }
    }

    private static BlockState fixState(BlockState s, Direction facing) {
        if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) s = s.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        if (s.hasProperty(BlockStateProperties.ATTACH_FACE)) s = s.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
        if (s.hasProperty(BlockStateProperties.LIT) && s.is(Blocks.FURNACE)) s = s.setValue(BlockStateProperties.LIT, false);
        return s;
    }

    // ------------------------------------------------------------------ shell buildings

    private static void buildShell(Ctx ctx, BuildingType type) {
        int r = type.radius;
        int b = r - 1;
        int wallH = Math.max(3, Math.min(11, type.height - 3));
        Block wall = Blocks.OAK_PLANKS;
        Block second = null;
        Block door = Blocks.OAK_DOOR;
        Requirement window = null;
        Requirement vertical = null;
        Requirement roofEdge = null;
        List<Requirement> interior = new ArrayList<>();
        List<Requirement> beds = new ArrayList<>();
        boolean firstStructure = true;
        for (Requirement req : type.requirements) {
            switch (req.mode) {
                case STRUCTURE -> {
                    if (firstStructure) {
                        wall = req.placeBlock();
                        firstStructure = false;
                    } else if (second == null) {
                        second = req.placeBlock();
                    }
                }
                case DOOR -> door = req.placeBlock();
                case WINDOW -> window = req;
                case VERTICAL -> vertical = req;
                case ROOF_EDGE -> roofEdge = req;
                case BED -> beds.add(req);
                default -> interior.add(req);
            }
        }
        Block roof = second != null ? second : wall;
        Block pillar = second != null ? second : wall;

        // path ring outside the walls
        for (int lx = -r; lx <= r; lx++) {
            for (int lz = -r; lz <= r; lz++) {
                if (Math.max(Math.abs(lx), Math.abs(lz)) > b) ctx.set(lx, -1, lz, Blocks.DIRT_PATH.defaultBlockState());
            }
        }
        // floor + walls + roof
        for (int lx = -b; lx <= b; lx++) {
            for (int lz = -b; lz <= b; lz++) {
                ctx.set(lx, -1, lz, roof.defaultBlockState());
                boolean edge = Math.abs(lx) == b || Math.abs(lz) == b;
                boolean corner = Math.abs(lx) == b && Math.abs(lz) == b;
                if (edge) {
                    for (int y = 0; y < wallH; y++) ctx.set(lx, y, lz, (corner ? pillar : wall).defaultBlockState());
                }
                ctx.set(lx, wallH, lz, roof.defaultBlockState());
            }
        }
        // door
        BlockState lower = door.defaultBlockState();
        if (lower.hasProperty(DoorBlock.FACING)) {
            lower = lower.setValue(DoorBlock.FACING, ctx.front.getOpposite()).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
            ctx.set(0, 0, b, lower);
            ctx.set(0, 1, b, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        } else {
            ctx.set(0, 0, b, Blocks.AIR.defaultBlockState());
            ctx.set(0, 1, b, Blocks.AIR.defaultBlockState());
        }
        // windows
        int windowsLeft = window == null ? 0 : window.count;
        List<int[]> windowSpots = new ArrayList<>();
        for (int y = 1; y <= Math.min(2, wallH - 2); y++) {
            for (int off : new int[]{-2, 2, 0}) {
                if (Math.abs(off) >= b) continue;
                windowSpots.add(new int[]{off, y, -b});
                if (vertical == null || off != 0) windowSpots.add(new int[]{b, y, off});
                windowSpots.add(new int[]{-b, y, off});
                if (off != 0) windowSpots.add(new int[]{off, y, b});
            }
        }
        int decorativeWindows = 0;
        for (int[] w : windowSpots) {
            if (windowsLeft > 0) {
                ctx.set(w[0], w[1], w[2], window.placeBlock().defaultBlockState());
                windowsLeft--;
            } else if (decorativeWindows < 4 && w[1] == 1) {
                ctx.set(w[0], w[1], w[2], Blocks.GLASS_PANE.defaultBlockState());
                decorativeWindows++;
            }
        }
        // ladder column next to the right wall
        Set<Long> used = new HashSet<>();
        if (vertical != null && b >= 2) {
            int lx = b - 1;
            Direction facing = ctx.dir(-1, 0);
            BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing);
            for (int y = 0; y <= wallH; y++) ctx.set(lx, y, 0, ladder);
            used.add(key(lx, 0));
        }
        // crenellations
        if (roofEdge != null) {
            int left = roofEdge.count;
            for (int lx = -b; lx <= b && left > 0; lx++) {
                for (int lz = -b; lz <= b && left > 0; lz++) {
                    boolean edge = Math.abs(lx) == b || Math.abs(lz) == b;
                    if (edge && ((lx + lz) & 1) == 0) {
                        ctx.set(lx, wallH + 1, lz, roofEdge.placeBlock().defaultBlockState());
                        left--;
                    }
                }
            }
        }
        // interior slots: rings from the walls inwards, skipping the corridor from the door to the center
        List<int[]> slots = new ArrayList<>();
        for (int ring = b - 1; ring >= 1; ring--) {
            for (int lx = -ring; lx <= ring; lx++) {
                for (int lz = -ring; lz <= ring; lz++) {
                    if (Math.max(Math.abs(lx), Math.abs(lz)) != ring) continue;
                    if (lx == 0 && lz > 0) continue;
                    if (used.contains(key(lx, lz))) continue;
                    slots.add(new int[]{lx, lz, ring});
                }
            }
        }
        // beds: head against a wall, foot one step inwards
        for (Requirement req : beds) {
            int placed = 0;
            for (int[] s : slots) {
                if (placed >= req.count) break;
                int lx = s[0], lz = s[1];
                if (s[2] != b - 1 || used.contains(key(lx, lz))) continue;
                int dx = 0, dz = 0;
                if (Math.abs(lx) == b - 1 && Math.abs(lz) != b - 1) dx = Integer.signum(lx);
                else if (Math.abs(lz) == b - 1 && Math.abs(lx) != b - 1) dz = Integer.signum(lz);
                else continue;
                int fx = lx - dx, fz = lz - dz;
                if ((fx == 0 && fz == 0) || (fx == 0 && fz > 0) || used.contains(key(fx, fz))) continue;
                Direction facing = ctx.dir(dx, dz);
                BlockState bed = req.placeBlock().defaultBlockState();
                if (!bed.hasProperty(BedBlock.PART)) bed = Blocks.RED_BED.defaultBlockState();
                ctx.set(fx, 0, fz, bed.setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.FOOT));
                ctx.set(lx, 0, lz, bed.setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.HEAD));
                used.add(key(lx, lz));
                used.add(key(fx, fz));
                placed++;
            }
        }
        // other interior blocks, stacking full blocks when space runs out
        for (int layer = 0; layer < 3; layer++) {
            List<Requirement> remaining = new ArrayList<>();
            for (Requirement req : interior) {
                int already = 0;
                for (int[] s : slots) {
                    for (int y = 0; y < 3; y++) if (req.matches(ctx.get(s[0], y, s[1]))) already++;
                }
                int need = req.count - already;
                for (int[] s : slots) {
                    if (need <= 0) break;
                    int lx = s[0], lz = s[1];
                    if (layer == 0 && used.contains(key(lx, lz))) continue;
                    if (layer > 0) {
                        BlockState below = ctx.get(lx, layer - 1, lz);
                        if (below.isAir() || !below.isCollisionShapeFullBlock(ctx.level, ctx.at(lx, layer - 1, lz))) continue;
                        if (!ctx.get(lx, layer, lz).isAir() || layer >= wallH - 1) continue;
                    }
                    BlockState st = fixState(req.placeBlock().defaultBlockState(), ctx.dir(-Integer.signum(lx), -Integer.signum(lz)));
                    if (layer > 0 && !st.isCollisionShapeFullBlock(ctx.level, ctx.at(lx, layer, lz))) continue;
                    ctx.set(lx, layer, lz, st);
                    if (layer == 0) used.add(key(lx, lz));
                    need--;
                }
                if (need > 0) remaining.add(req);
            }
            if (remaining.isEmpty()) break;
            interior = remaining;
        }
        // a hanging lantern so the building is lit
        if (ctx.get(0, wallH - 1, 0).isAir()) {
            ctx.set(0, wallH - 1, 0, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    // ------------------------------------------------------------------ farm

    private static void buildFarm(Ctx ctx, BuildingType type) {
        int r = type.radius;
        int in = r - 1;
        BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
        for (int lx = -r; lx <= r; lx++) {
            for (int lz = -r; lz <= r; lz++) {
                if (Math.max(Math.abs(lx), Math.abs(lz)) == r) {
                    if (lx == 0 && lz == r) {
                        ctx.set(lx, 0, lz, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, ctx.front));
                    } else {
                        ctx.set(lx, 0, lz, fence);
                        if (Math.abs(lx) == r && Math.abs(lz) == r) ctx.set(lx, 1, lz, Blocks.TORCH.defaultBlockState());
                    }
                }
            }
        }
        for (int lx = -in; lx <= in; lx++) {
            for (int lz = -in; lz <= in; lz++) {
                if (lx == 0) {
                    if (lz == 0 || lz == in) ctx.set(lx, -1, lz, Blocks.COBBLESTONE.defaultBlockState());
                    else ctx.set(lx, -1, lz, Blocks.WATER.defaultBlockState());
                } else if (lx == in && lz == -in) {
                    ctx.set(lx, -1, lz, Blocks.COBBLESTONE.defaultBlockState());
                    ctx.set(lx, 0, lz, Blocks.COMPOSTER.defaultBlockState());
                } else {
                    ctx.set(lx, -1, lz, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                    int age = ctx.level.random.nextInt(4);
                    BlockState crop = switch (Math.abs(lx) % 3) {
                        case 0 -> Blocks.CARROTS.defaultBlockState();
                        case 1 -> Blocks.WHEAT.defaultBlockState();
                        default -> Blocks.POTATOES.defaultBlockState();
                    };
                    if (crop.hasProperty(CropBlock.AGE)) crop = crop.setValue(CropBlock.AGE, age);
                    ctx.set(lx, 0, lz, crop);
                }
            }
        }
    }
}
