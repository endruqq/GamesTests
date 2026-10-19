package com.endruqq.imperium.game;

import com.endruqq.imperium.block.ConstructionSiteBlock;
import com.endruqq.imperium.game.building.Building;
import com.endruqq.imperium.game.building.BuildingType;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.game.building.Requirement;
import com.endruqq.imperium.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Construction sites, requirement checks and building effects that touch the world. */
public final class BuildingManager {
    private static final Map<Integer, List<BlockPos>> FURNACES = new HashMap<>();
    private static int checkCursor;

    private BuildingManager() {}

    // ------------------------------------------------------------------ validation (shared with client)

    /** @return null if the plot is valid, otherwise the reason */
    public static Component validate(GameData d, int nationId, BuildingType type, BlockPos pos) {
        if (d.phase != GamePhase.RUNNING) return Component.translatable("imperium.err.not_running");
        Nation n = d.nation(nationId);
        if (n == null) return Component.translatable("imperium.err.no_nation");
        if (!type.unlocked(n)) return Component.translatable("imperium.err.locked", type.tech.title());
        if (type.maxPerNation > 0 && d.countAll(nationId, type.id) >= type.maxPerNation)
            return Component.translatable("imperium.err.max_buildings", type.maxPerNation);
        int r = type.radius;
        int[][] pts = {{0, 0}, {-r, -r}, {r, -r}, {-r, r}, {r, r}, {0, -r}, {0, r}, {-r, 0}, {r, 0}};
        for (int[] p : pts) {
            if (d.nationIdAt(pos.getX() + p[0] + 0.5, pos.getZ() + p[1] + 0.5) != nationId)
                return Component.translatable("imperium.err.not_own_territory");
        }
        for (Building b : d.buildings.values()) {
            if (pos.getX() - r <= b.maxX() + 1 && pos.getX() + r >= b.minX() - 1
                    && pos.getZ() - r <= b.maxZ() + 1 && pos.getZ() + r >= b.minZ() - 1
                    && Math.abs(pos.getY() - b.pos.getY()) < 24) {
                return Component.translatable("imperium.err.overlap", b.type() == null ? "?" : b.type().name());
            }
        }
        for (Province p : d.provinces.values()) {
            if (p.core == null) continue;
            if (Math.abs(p.core.getX() - pos.getX()) <= r + 5 && Math.abs(p.core.getZ() - pos.getZ()) <= r + 5
                    && Math.abs(p.core.getY() - pos.getY()) < 24) {
                return Component.translatable("imperium.err.near_core");
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ placing

    public static boolean place(ServerLevel level, ServerPlayer player, BuildingType type, BlockPos pos, boolean prefab, Direction front) {
        GameData d = GameData.get(level.getServer());
        Nation n = d.nationOf(player.getUUID());
        if (n == null) {
            player.displayClientMessage(Component.translatable("imperium.err.no_nation").withStyle(ChatFormatting.RED), true);
            return false;
        }
        return placeFor(level, d, n, type, pos, prefab, front, player);
    }

    public static boolean placeFor(ServerLevel level, GameData d, Nation n, BuildingType type, BlockPos pos, boolean prefab,
                                   Direction front, ServerPlayer feedback) {
        Component err = validate(d, n.id, type, pos);
        if (err == null && prefab && type.prefabCost == null) err = Component.translatable("imperium.err.no_prefab");
        if (err == null && prefab && !n.stock.has(type.prefabCost)) {
            err = Component.translatable("imperium.err.cost", type.prefabCost.describe(1));
        }
        if (err != null) {
            if (feedback != null) feedback.displayClientMessage(err.copy().withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!prefab && !level.getBlockState(pos.below()).getFluidState().isEmpty()) {
            if (feedback != null) feedback.displayClientMessage(Component.translatable("imperium.err.water").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (prefab) n.stock.subtract(type.prefabCost);

        Building b = new Building();
        b.id = d.nextBuildingId++;
        b.typeId = type.id;
        b.nation = n.id;
        b.pos = pos;
        b.createdTick = d.gameTick;
        b.counts = new int[type.requirements.size()];
        b.state = prefab ? Building.State.ACTIVE : Building.State.CONSTRUCTION;
        d.buildings.put(b.id, b);

        if (prefab) {
            PrefabBuilder.build(level, type, pos, front, n);
        } else {
            level.setBlock(pos, ModBlocks.CONSTRUCTION_SITE.get().defaultBlockState(), 3);
        }
        check(level, d, b, true);
        n.addLog(Component.translatable(prefab ? "imperium.log.prefab_built" : "imperium.log.site_placed", type.name()));
        d.markChanged();
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        if (feedback != null) {
            feedback.displayClientMessage(Component.translatable(prefab ? "imperium.msg.prefab_built" : "imperium.msg.site_placed",
                    type.name()).withStyle(ChatFormatting.GREEN), true);
        }
        return true;
    }

    // ------------------------------------------------------------------ checks

    public static int[] count(net.minecraft.world.level.BlockGetter level, Building b) {
        BuildingType type = b.type();
        if (type == null) return new int[0];
        int[] counts = new int[type.requirements.size()];
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = b.minX(); x <= b.maxX(); x++) {
            for (int z = b.minZ(); z <= b.maxZ(); z++) {
                for (int y = b.minY(); y <= b.maxY(); y++) {
                    m.set(x, y, z);
                    BlockState s = level.getBlockState(m);
                    if (s.isAir()) continue;
                    for (int i = 0; i < counts.length; i++) {
                        if (type.requirements.get(i).matches(s)) counts[i]++;
                    }
                }
            }
        }
        return counts;
    }

    /** Re-count requirements and update the building state. */
    public static void check(ServerLevel level, GameData d, Building b, boolean announce) {
        BuildingType type = b.type();
        if (type == null) return;
        if (!level.isLoaded(b.pos)) return;
        BlockState site = level.getBlockState(b.pos);
        if (!site.is(ModBlocks.CONSTRUCTION_SITE.get())) {
            level.setBlock(b.pos, ModBlocks.CONSTRUCTION_SITE.get().defaultBlockState(), 3);
            site = level.getBlockState(b.pos);
        }
        b.counts = count(level, b);
        boolean ok = b.satisfied() == type.requirements.size();
        Building.State old = b.state;
        if (ok) b.state = Building.State.ACTIVE;
        else if (old != Building.State.CONSTRUCTION) b.state = Building.State.DAMAGED;
        int beds = 0;
        for (int i = 0; i < type.requirements.size(); i++) {
            if (type.requirements.get(i).id.equals("beds")) beds = b.counts[i];
        }
        b.housing = type.isHousing() && b.state == Building.State.ACTIVE ? Math.min(type.maxHousing, Math.max(2, beds * 2)) : 0;
        boolean active = b.state == Building.State.ACTIVE;
        if (site.getValue(ConstructionSiteBlock.ACTIVE) != active) {
            level.setBlock(b.pos, site.setValue(ConstructionSiteBlock.ACTIVE, active), 3);
        }
        if (old != b.state) {
            Nation n = d.nation(b.nation);
            if (n != null) {
                if (b.state == Building.State.ACTIVE && old == Building.State.CONSTRUCTION) {
                    n.addLog(Component.translatable("imperium.log.building_done", type.name()));
                    Notify.nation(level.getServer(), d, n.id, Component.translatable("imperium.msg.building_done", type.name())
                            .withStyle(ChatFormatting.GOLD), true);
                    level.playSound(null, b.pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.0F);
                } else if (b.state == Building.State.DAMAGED) {
                    n.addLog(Component.translatable("imperium.log.building_damaged", type.name()));
                    if (announce) Notify.nation(level.getServer(), d, n.id, Component.translatable("imperium.msg.building_damaged",
                            type.name()).withStyle(ChatFormatting.RED), false);
                } else if (b.state == Building.State.ACTIVE) {
                    n.addLog(Component.translatable("imperium.log.building_repaired", type.name()));
                }
            }
            FURNACES.remove(b.id);
        }
        d.markChanged();
    }

    /** Called every server tick. */
    public static void tick(ServerLevel level, GameData d) {
        if (d.phase != GamePhase.RUNNING) return;
        if (d.gameTick % 20 == 0 && !d.buildings.isEmpty()) {
            List<Building> list = new ArrayList<>(d.buildings.values());
            // check a few buildings per second, round-robin
            int n = Math.max(1, list.size() / 5 + 1);
            for (int i = 0; i < n; i++) {
                checkCursor = (checkCursor + 1) % list.size();
                Building b = list.get(checkCursor);
                if (level.isLoaded(b.pos)) check(level, d, b, true);
            }
        }
        tickFurnaces(level, d);
    }

    /** Furnaces in and around an active smeltery work three times as fast. */
    private static void tickFurnaces(ServerLevel level, GameData d) {
        for (Building b : d.buildings.values()) {
            if (!b.isActive() || !b.typeId.equals(Buildings.SMELTERY.id) || !level.isLoaded(b.pos)) continue;
            List<BlockPos> furnaces = FURNACES.get(b.id);
            if (furnaces == null || d.gameTick % 100 == 0) {
                furnaces = new ArrayList<>();
                for (BlockPos p : BlockPos.betweenClosed(b.minX() - 8, b.minY(), b.minZ() - 8, b.maxX() + 8, b.maxY(), b.maxZ() + 8)) {
                    if (level.getBlockEntity(p) instanceof AbstractFurnaceBlockEntity) furnaces.add(p.immutable());
                }
                FURNACES.put(b.id, furnaces);
            }
            for (BlockPos p : furnaces) {
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof AbstractFurnaceBlockEntity f) {
                    BlockState st = level.getBlockState(p);
                    AbstractFurnaceBlockEntity.serverTick(level, p, st, f);
                    AbstractFurnaceBlockEntity.serverTick(level, p, level.getBlockState(p), f);
                }
            }
        }
    }

    public static boolean isInActiveSmeltery(ServerLevel level, BlockPos pos) {
        GameData d = GameData.get(level.getServer());
        for (Building b : d.buildings.values()) {
            if (b.isActive() && b.typeId.equals(Buildings.SMELTERY.id) && b.containsXZ(pos.getX() + 0.5, pos.getZ() + 0.5, 8)
                    && Math.abs(pos.getY() - b.pos.getY()) < 16) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ site interaction

    public static void onSiteUsed(ServerLevel level, ServerPlayer player, BlockPos pos) {
        GameData d = GameData.get(level.getServer());
        Building b = d.buildingAtSite(pos);
        if (b == null || b.type() == null) {
            player.sendSystemMessage(Component.translatable("imperium.msg.orphan_site").withStyle(ChatFormatting.GRAY));
            return;
        }
        check(level, d, b, false);
        BuildingType type = b.type();
        Nation owner = d.nation(b.nation);
        player.sendSystemMessage(Component.literal("== ").append(type.name()).append(" ==").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("imperium.msg.site_owner", owner == null ? "-" : owner.name,
                Component.translatable(b.state.key())).withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < type.requirements.size(); i++) {
            Requirement r = type.requirements.get(i);
            int have = i < b.counts.length ? b.counts[i] : 0;
            boolean ok = have >= r.count;
            MutableComponent line = Component.literal(ok ? " ✔ " : " ✘ ").append(r.name()).append(": " + Math.min(have, r.count) + "/" + r.count);
            player.sendSystemMessage(line.withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        player.sendSystemMessage(type.description().copy().withStyle(ChatFormatting.AQUA));
    }

    public static void onSiteRemoved(ServerLevel level, BlockPos pos) {
        GameData d = GameData.get(level.getServer());
        Building b = d.buildingAtSite(pos);
        if (b == null) return;
        remove(level, d, b, true);
    }

    public static void remove(ServerLevel level, GameData d, Building b, boolean destroyed) {
        d.buildings.remove(b.id);
        FURNACES.remove(b.id);
        Nation n = d.nation(b.nation);
        if (n != null && b.type() != null) {
            n.addLog(Component.translatable(destroyed ? "imperium.log.building_destroyed" : "imperium.log.building_demolished", b.type().name()));
            if (destroyed && b.isActive()) {
                Notify.nation(level.getServer(), d, n.id, Component.translatable("imperium.msg.building_destroyed", b.type().name())
                        .withStyle(ChatFormatting.RED), true);
            }
        }
        d.markChanged();
    }

    public static void demolish(ServerLevel level, ServerPlayer player, int buildingId) {
        GameData d = GameData.get(level.getServer());
        Building b = d.buildings.get(buildingId);
        Nation n = d.nationOf(player.getUUID());
        if (b == null || n == null || b.nation != n.id) return;
        // refund part of the prefab cost of unfinished sites is not possible, just remove the marker
        d.buildings.remove(b.id);
        if (level.isLoaded(b.pos) && level.getBlockState(b.pos).is(ModBlocks.CONSTRUCTION_SITE.get())) {
            level.removeBlock(b.pos, false);
        }
        n.addLog(Component.translatable("imperium.log.building_demolished", b.type() == null ? "?" : b.type().name()));
        d.markChanged();
    }

    public static void clearCaches() {
        FURNACES.clear();
    }
}
