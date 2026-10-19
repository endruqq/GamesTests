package com.endruqq.imperium.game;

import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.UnitRegistry;
import com.endruqq.imperium.game.building.Building;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.network.ImperiumNet;
import com.endruqq.imperium.network.StatePacket;
import com.endruqq.imperium.network.TerritoryPacket;
import com.endruqq.imperium.network.UnitsPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Keeps clients up to date: state, territory and (fog-of-war filtered) unit positions. */
public final class Sync {
    private static final Set<UUID> RTS_PLAYERS = new HashSet<>();

    private Sync() {}

    public static void setRtsMode(ServerPlayer p, boolean active) {
        if (active) RTS_PLAYERS.add(p.getUUID());
        else RTS_PLAYERS.remove(p.getUUID());
    }

    public static boolean inRts(ServerPlayer p) {
        return RTS_PLAYERS.contains(p.getUUID());
    }

    public static void clear() {
        RTS_PLAYERS.clear();
    }

    public static void tick(MinecraftServer server, GameData d) {
        long t = server.getTickCount();
        if (t % 10 == 0 && d.territoryDirty) {
            d.territoryDirty = false;
            ImperiumNet.sendToAll(new TerritoryPacket(d.gridN, d.cells));
        }
        if (t % 10 == 5 && d.stateDirty) {
            d.stateDirty = false;
            ImperiumNet.sendToAll(new StatePacket(d.write(new CompoundTag(), false)));
        }
        if (t % 10 == 2) sendUnits(server, d, true);
        else if (t % 40 == 22) sendUnits(server, d, false);
    }

    private record Eye(double x, double z, double r) {
    }

    private static void sendUnits(MinecraftServer server, GameData d, boolean rtsOnly) {
        List<ServerPlayer> targets = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (RTS_PLAYERS.contains(p.getUUID()) == rtsOnly) targets.add(p);
        }
        if (targets.isEmpty()) return;
        List<SoldierEntity> soldiers = UnitRegistry.soldiers();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        Map<Integer, List<Eye>> eyes = new HashMap<>();
        Map<Integer, List<UnitsPacket.Unit>> cache = new HashMap<>();
        for (ServerPlayer p : targets) {
            Nation n = d.nationOf(p.getUUID());
            int nid = n == null ? -1 : n.id;
            boolean seeAll = n == null || d.phase != GamePhase.RUNNING;
            List<UnitsPacket.Unit> list = cache.get(seeAll ? Integer.MIN_VALUE : nid);
            if (list == null) {
                List<Eye> vision = seeAll ? null : eyes.computeIfAbsent(nid, k -> vision(d, k, soldiers, players));
                list = new ArrayList<>();
                for (SoldierEntity s : soldiers) {
                    if (!seeAll && !visible(d, nid, s.getNationId(), s.getX(), s.getZ(), vision)) continue;
                    UnitsPacket.Unit u = new UnitsPacket.Unit();
                    u.entityId = s.getId();
                    u.nation = s.getNationId();
                    u.type = s.getUnitType().ordinal();
                    u.x = (float) s.getX();
                    u.y = (float) s.getY();
                    u.z = (float) s.getZ();
                    u.hp = s.getHealth() / Math.max(1, s.getMaxHealth());
                    u.order = s.getOrder().ordinal();
                    list.add(u);
                }
                for (ServerPlayer o : players) {
                    if (o.isSpectator()) continue;
                    int on = d.nationIdOf(o.getUUID());
                    if (!seeAll && !visible(d, nid, on, o.getX(), o.getZ(), vision)) continue;
                    UnitsPacket.Unit u = new UnitsPacket.Unit();
                    u.entityId = o.getId();
                    u.nation = on;
                    u.type = -1;
                    u.x = (float) o.getX();
                    u.y = (float) o.getY();
                    u.z = (float) o.getZ();
                    u.hp = o.getHealth() / Math.max(1, o.getMaxHealth());
                    u.name = o.getGameProfile().getName();
                    list.add(u);
                }
                cache.put(seeAll ? Integer.MIN_VALUE : nid, list);
            }
            ImperiumNet.sendTo(p, new UnitsPacket(list));
        }
    }

    private static List<Eye> vision(GameData d, int nation, List<SoldierEntity> soldiers, List<ServerPlayer> players) {
        List<Eye> l = new ArrayList<>();
        for (SoldierEntity s : soldiers) if (d.friendly(nation, s.getNationId())) l.add(new Eye(s.getX(), s.getZ(), 40));
        for (ServerPlayer p : players) if (d.friendly(nation, d.nationIdOf(p.getUUID()))) l.add(new Eye(p.getX(), p.getZ(), 48));
        for (Province p : d.provinces.values()) {
            if (p.core != null && d.friendly(nation, p.owner)) l.add(new Eye(p.core.getX(), p.core.getZ(), 40));
        }
        for (Building b : d.buildings.values()) {
            if (b.isActive() && d.friendly(nation, b.nation) && b.typeId.equals(Buildings.WATCHTOWER.id)) {
                l.add(new Eye(b.pos.getX(), b.pos.getZ(), 96));
            }
        }
        return l;
    }

    private static boolean visible(GameData d, int viewer, int owner, double x, double z, List<Eye> vision) {
        if (vision == null) return true;
        if (d.friendly(viewer, owner)) return true;
        int here = d.nationIdAt(x, z);
        if (here >= 0 && d.friendly(viewer, here)) return true;
        for (Eye e : vision) {
            double dx = e.x - x, dz = e.z - z;
            if (dx * dx + dz * dz <= e.r * e.r) return true;
        }
        return false;
    }
}
