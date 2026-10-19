package com.endruqq.imperium.game;

import com.endruqq.imperium.entity.Allegiance;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.UnitRegistry;
import com.endruqq.imperium.game.building.Building;
import com.endruqq.imperium.game.building.Buildings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Open and closed borders. When a nation closes its borders, foreign soldiers and strangers (players of other nations or
 * of none) found on its land are warned, reported and - after a short grace period - attacked and chased by its army.
 * Allies and nations with military access may pass.
 */
public final class BorderManager {
    private record Intrusion(int nation, long since) {
    }

    private static final Map<UUID, Intrusion> INTRUDERS = new HashMap<>();
    private static final Map<Long, Long> REPORTED = new HashMap<>();
    /** players get 10 seconds to turn back */
    public static final long PLAYER_GRACE = 200;
    public static final long SOLDIER_GRACE = 60;

    private BorderManager() {}

    /** May somebody of nation {@code who} (-1: nobody) stay on the land of {@code owner}? */
    public static boolean welcome(GameData d, int who, int owner) {
        if (owner < 0 || who == owner) return true;
        Nation o = d.nation(owner);
        if (o == null || o.eliminated || !o.bordersClosed) return true;
        if (who >= 0) {
            if (d.allied(who, owner) || d.atWar(who, owner)) return true;
            Relation r = d.relations.get(Relation.key(who, owner));
            if (r != null && r.access) return true;
        }
        return false;
    }

    /** Called once per second. */
    public static void tick(MinecraftServer server, GameData d) {
        if (d.phase != GamePhase.RUNNING) {
            INTRUDERS.clear();
            return;
        }
        long now = d.gameTick;
        Set<UUID> seen = new HashSet<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator() || p.isCreative()) continue;
            check(server, d, p, d.nationIdOf(p.getUUID()), seen, now);
        }
        for (SoldierEntity s : UnitRegistry.soldiers()) {
            if (!s.isSurrendered()) check(server, d, s, s.getNationId(), seen, now);
        }
        INTRUDERS.keySet().retainAll(seen);
        REPORTED.values().removeIf(t -> now - t > 20 * 60);
    }

    private static void check(MinecraftServer server, GameData d, Entity e, int who, Set<UUID> seen, long now) {
        int owner = d.nationIdAt(e.getX(), e.getZ());
        if (owner < 0 || welcome(d, who, owner)) return;
        seen.add(e.getUUID());
        Intrusion in = INTRUDERS.get(e.getUUID());
        if (in != null && in.nation() == owner) return;
        INTRUDERS.put(e.getUUID(), new Intrusion(owner, now));
        Nation o = d.nation(owner);
        Nation intruderNation = d.nation(who);
        Province prov = d.provinceAt(e.getX(), e.getZ());
        String place = prov == null ? "-" : prov.name;
        if (e instanceof ServerPlayer p) {
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("imperium.title.closed_border").withStyle(ChatFormatting.RED)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("imperium.title.closed_border_sub", o.name, PLAYER_GRACE / 20)));
            Notify.player(p, Component.translatable("imperium.msg.closed_border_warning", o.name, PLAYER_GRACE / 20).withStyle(ChatFormatting.RED), true);
        }
        long key = ((long) owner << 32) ^ (who >= 0 ? who : e.getUUID().hashCode());
        Long last = REPORTED.get(key);
        if (last == null || now - last > 20 * 30) {
            REPORTED.put(key, now);
            Component name = e instanceof Player ? e.getDisplayName()
                    : Component.translatable("imperium.msg.foreign_troops", intruderNation == null ? "?" : intruderNation.name);
            Notify.nation(server, d, owner, Component.translatable("imperium.msg.intruder", name, place).withStyle(ChatFormatting.GOLD), true);
            if (intruderNation != null) {
                WarReasons.add(server, d, owner, who, CasusBelli.Type.BORDER_VIOLATION);
                d.relation(owner, who).addOpinion(owner, who, -3);
                Notify.nation(server, d, who, Component.translatable("imperium.msg.border_crossed", o.name).withStyle(ChatFormatting.YELLOW), false);
            }
        }
    }

    /** Is {@code e} an intruder on the closed land of {@code against}, past the grace period? */
    public static boolean isTrespasser(GameData d, Entity e, int against) {
        if (e == null || against < 0 || d == null) return false;
        Intrusion in = INTRUDERS.get(e.getUUID());
        if (in == null || in.nation() != against) return false;
        long grace = e instanceof Player ? PLAYER_GRACE : SOLDIER_GRACE;
        if (d.gameTick - in.since() < grace) return false;
        return d.nationIdAt(e.getX(), e.getZ()) == against;
    }

    public static List<LivingEntity> intrudersOf(Level level, GameData d, int nation) {
        List<LivingEntity> l = new ArrayList<>();
        if (!(level instanceof ServerLevel sl)) return l;
        for (Map.Entry<UUID, Intrusion> en : INTRUDERS.entrySet()) {
            if (en.getValue().nation() != nation) continue;
            Entity e = sl.getEntity(en.getKey());
            if (e instanceof LivingEntity le && le.isAlive() && isTrespasser(d, le, nation)) l.add(le);
        }
        return l;
    }

    /** How far our soldiers react to intruders (0: borders open or we are abroad). */
    public static double pursuitRange(GameData d, SoldierEntity s) {
        Nation n = d.nation(s.getNationId());
        if (n == null || !n.bordersClosed || d.nationIdAt(s.getX(), s.getZ()) != n.id) return 0;
        for (Building b : d.buildings.values()) {
            if (b.nation == n.id && b.isActive() && b.typeId.equals(Buildings.WATCHTOWER.id) && s.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(b.pos)) < 96 * 96) return 64;
        }
        return 32;
    }

    /** Fights between intruders and the owners of the land are allowed even in peace. */
    public static boolean borderConflict(GameData d, Entity a, Entity b) {
        int na = Allegiance.nationOf(a, d), nb = Allegiance.nationOf(b, d);
        return (na >= 0 && isTrespasser(d, b, na)) || (nb >= 0 && isTrespasser(d, a, nb));
    }

    public static int intruderCount(int nation) {
        int c = 0;
        for (Intrusion in : INTRUDERS.values()) if (in.nation() == nation) c++;
        return c;
    }

    public static void clear() {
        INTRUDERS.clear();
        REPORTED.clear();
    }
}
