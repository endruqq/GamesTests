package com.endruqq.imperium.game;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.ImperiumConfig;
import com.endruqq.imperium.entity.OrderType;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.UnitRegistry;
import com.endruqq.imperium.game.building.Building;
import com.endruqq.imperium.game.building.Buildings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps the important parts of the world loaded so that wars also happen far away from players:
 * bot capitals and military buildings, and the ground under marching armies.
 */
public final class ChunkLoading {
    private static final int MAX_PER_NATION = 24;
    private static final Map<UUID, Set<Long>> ACTIVE = new HashMap<>();

    private ChunkLoading() {}

    public static void registerCallback() {
        // tickets are recomputed every few seconds, so stale ones from a previous session are dropped
        ForgeChunkManager.setForcedChunkLoadingCallback(Imperium.MODID, (level, helper) -> {
            for (UUID id : new ArrayList<>(helper.getEntityTickets().keySet())) helper.removeAllTickets(id);
            for (BlockPos pos : new ArrayList<>(helper.getBlockTickets().keySet())) helper.removeAllTickets(pos);
        });
    }

    private static UUID owner(int nation) {
        return UUID.nameUUIDFromBytes(("imperium:nation:" + nation).getBytes(StandardCharsets.UTF_8));
    }

    private static void add(Set<Long> set, BlockPos pos) {
        if (set.size() < MAX_PER_NATION) set.add(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
    }

    private static boolean military(Building b) {
        return b.typeId.equals(Buildings.BARRACKS.id) || b.typeId.equals(Buildings.ARCHERY_RANGE.id)
                || b.typeId.equals(Buildings.ARMORY.id) || b.typeId.equals(Buildings.SIEGE_WORKSHOP.id);
    }

    /** Called every 100 ticks. */
    public static void tick(ServerLevel level, GameData d) {
        Map<UUID, Set<Long>> want = new HashMap<>();
        if (d.phase == GamePhase.RUNNING && ImperiumConfig.KEEP_ARMIES_LOADED.get()) {
            for (Nation n : d.nations.values()) {
                if (n.eliminated) continue;
                Set<Long> set = new HashSet<>();
                if (n.bot) {
                    Province cap = d.provinces.get(n.capitalProvince);
                    if (cap != null && cap.core != null) add(set, cap.core);
                    for (Building b : d.buildings.values()) {
                        if (b.nation == n.id && b.isActive() && military(b)) add(set, b.pos);
                    }
                }
                for (SoldierEntity s : UnitRegistry.soldiersOf(n.id)) {
                    OrderType o = s.getOrder();
                    if (o != OrderType.MOVE && o != OrderType.ATTACK_MOVE && o != OrderType.ATTACK_CORE && o != OrderType.RETURN) continue;
                    add(set, s.blockPosition());
                    Vec3 dest = s.getOrderPos();
                    Vec3 dir = dest.subtract(s.position());
                    if (dir.lengthSqr() > 16 * 16) {
                        Vec3 ahead = s.position().add(dir.normalize().scale(16));
                        add(set, BlockPos.containing(ahead));
                    }
                }
                if (!set.isEmpty()) want.put(owner(n.id), set);
            }
        }
        for (Map.Entry<UUID, Set<Long>> e : ACTIVE.entrySet()) {
            Set<Long> keep = want.getOrDefault(e.getKey(), Set.of());
            for (long c : e.getValue()) {
                if (!keep.contains(c)) {
                    ForgeChunkManager.forceChunk(level, Imperium.MODID, e.getKey(), ChunkPos.getX(c), ChunkPos.getZ(c), false, true);
                }
            }
        }
        for (Map.Entry<UUID, Set<Long>> e : want.entrySet()) {
            Set<Long> had = ACTIVE.getOrDefault(e.getKey(), Set.of());
            for (long c : e.getValue()) {
                if (!had.contains(c)) {
                    ForgeChunkManager.forceChunk(level, Imperium.MODID, e.getKey(), ChunkPos.getX(c), ChunkPos.getZ(c), true, true);
                }
            }
        }
        ACTIVE.clear();
        ACTIVE.putAll(want);
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
