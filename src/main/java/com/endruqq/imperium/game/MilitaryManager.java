package com.endruqq.imperium.game;

import com.endruqq.imperium.entity.CitizenEntity;
import com.endruqq.imperium.entity.OrderType;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.Stance;
import com.endruqq.imperium.entity.UnitRegistry;
import com.endruqq.imperium.entity.UnitType;
import com.endruqq.imperium.game.building.Building;
import com.endruqq.imperium.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Conscription and unit orders. */
public final class MilitaryManager {
    private MilitaryManager() {}

    public static int maxArmy(Nation n) {
        return Math.max(4, n.population / 2 + 4);
    }

    /** @return null if recruiting is possible */
    public static Component canRecruit(GameData d, Nation n, UnitType type) {
        if (d.phase != GamePhase.RUNNING) return Component.translatable("imperium.err.not_running");
        if (!type.unlocked(n)) return Component.translatable("imperium.err.locked", type.tech.title());
        if (!d.hasActive(n.id, type.building.id)) return Component.translatable("imperium.err.needs_building", type.building.name());
        if (n.population <= 2) return Component.translatable("imperium.err.no_people");
        if (n.soldierCount >= maxArmy(n)) return Component.translatable("imperium.err.army_limit", maxArmy(n));
        if (!n.stock.has(type.cost, type.costMult(n))) return Component.translatable("imperium.err.cost", type.cost.describe(type.costMult(n)));
        return null;
    }

    public static int recruit(ServerLevel level, GameData d, Nation n, UnitType type, int count, ServerPlayer feedback) {
        int made = 0;
        for (int i = 0; i < count; i++) {
            Component err = canRecruit(d, n, type);
            if (err != null) {
                if (feedback != null && made == 0) feedback.displayClientMessage(err.copy().withStyle(ChatFormatting.RED), true);
                break;
            }
            Building barracks = pickBuilding(level, d, n, type);
            if (barracks == null) {
                if (feedback != null && made == 0)
                    feedback.displayClientMessage(Component.translatable("imperium.err.building_unloaded").withStyle(ChatFormatting.RED), true);
                break;
            }
            n.stock.subtract(type.cost, type.costMult(n));
            n.population--;
            spawnSoldier(level, d, n, type, barracks.pos);
            made++;
        }
        if (made > 0) {
            n.addLog(Component.translatable("imperium.log.recruited", made, type.title()));
            if (feedback != null) feedback.displayClientMessage(Component.translatable("imperium.msg.recruited", made, type.title())
                    .withStyle(ChatFormatting.GREEN), true);
            d.markChanged();
        }
        return made;
    }

    private static Building pickBuilding(ServerLevel level, GameData d, Nation n, UnitType type) {
        List<Building> list = new ArrayList<>();
        for (Building b : d.buildings.values()) {
            if (b.nation == n.id && b.isActive() && b.typeId.equals(type.building.id) && level.isLoaded(b.pos)) list.add(b);
        }
        if (list.isEmpty()) return null;
        return list.get(level.random.nextInt(list.size()));
    }

    public static SoldierEntity spawnSoldier(ServerLevel level, GameData d, Nation n, UnitType type, BlockPos near) {
        SoldierEntity s = ModEntities.SOLDIER.get().create(level);
        if (s == null) return null;
        BlockPos at = Economy.findStandPos(level, near, 3);
        s.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360F, 0);
        s.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
        s.setup(type, n, d, near);
        level.addFreshEntity(s);
        n.soldierCount++;
        return s;
    }

    /** A citizen volunteers and becomes a militiaman on the spot. */
    public static SoldierEntity conscriptCitizen(ServerLevel level, GameData d, Nation n, CitizenEntity c) {
        BlockPos home = null;
        for (Building b : d.buildings.values()) {
            if (b.nation == n.id && b.isActive() && b.typeId.equals(UnitType.MILITIA.building.id)) {
                home = b.pos;
                break;
            }
        }
        SoldierEntity s = ModEntities.SOLDIER.get().create(level);
        if (s == null) return null;
        s.moveTo(c.getX(), c.getY(), c.getZ(), c.getYRot(), 0);
        s.setup(UnitType.MILITIA, n, d, home != null ? home : c.blockPosition());
        level.addFreshEntity(s);
        n.soldierCount++;
        n.population = Math.max(1, n.population - 1);
        c.leave();
        d.markChanged();
        return s;
    }

    private static List<SoldierEntity> ownUnits(ServerLevel level, Nation n, int[] ids) {
        List<SoldierEntity> list = new ArrayList<>();
        for (int id : ids) {
            Entity e = level.getEntity(id);
            if (e instanceof SoldierEntity s && s.isAlive() && s.getNationId() == n.id && !s.isSurrendered()) list.add(s);
        }
        return list;
    }

    /** Issue an order to a group, arranging the units in a formation around the target point. */
    public static void command(ServerLevel level, GameData d, Nation n, int[] ids, OrderType order, Vec3 target, int targetEntity,
                               int province, ServerPlayer player) {
        List<SoldierEntity> units = ownUnits(level, n, ids);
        if (units.isEmpty()) return;
        switch (order) {
            case ATTACK_TARGET -> {
                Entity e = level.getEntity(targetEntity);
                if (!(e instanceof LivingEntity le) || !le.isAlive()) return;
                int enemyNation = com.endruqq.imperium.entity.Allegiance.nationOf(e, d);
                boolean monster = e instanceof net.minecraft.world.entity.monster.Enemy && !(e instanceof SoldierEntity);
                if (!monster && !d.atWar(n.id, enemyNation)) {
                    if (player != null) player.displayClientMessage(Component.translatable("imperium.err.not_at_war").withStyle(ChatFormatting.RED), true);
                    return;
                }
                for (SoldierEntity s : units) {
                    s.order(OrderType.ATTACK_TARGET, s.position(), e.getUUID(), -1);
                    s.setTarget(le);
                }
            }
            case ATTACK_CORE -> {
                Province p = d.provinces.get(province);
                if (p == null || p.core == null || !WarManager.canAttackCore(d, n.id, p)) {
                    if (player != null) player.displayClientMessage(Component.translatable("imperium.err.not_at_war").withStyle(ChatFormatting.RED), true);
                    return;
                }
                for (SoldierEntity s : units) s.order(OrderType.ATTACK_CORE, Vec3.atCenterOf(p.core), null, p.id);
            }
            case FOLLOW -> {
                if (player == null) return;
                for (SoldierEntity s : units) s.order(OrderType.FOLLOW, s.position(), player.getUUID(), -1);
            }
            case RETURN -> {
                for (SoldierEntity s : units) s.order(OrderType.RETURN, s.position(), null, -1);
            }
            case HOLD -> {
                for (SoldierEntity s : units) s.hold(s.position());
            }
            case MOVE, ATTACK_MOVE -> {
                List<Vec3> slots = formation(units, target, level);
                for (int i = 0; i < units.size(); i++) units.get(i).order(order, slots.get(i), null, -1);
            }
        }
    }

    /**
     * Grid formation facing the direction of travel. Melee units take the front rows, archers the back.
     * Returns one slot per unit, in the unit list order.
     */
    public static List<Vec3> formation(List<SoldierEntity> units, Vec3 target, ServerLevel level) {
        Vec3 centroid = Vec3.ZERO;
        for (SoldierEntity s : units) centroid = centroid.add(s.position());
        centroid = centroid.scale(1.0 / units.size());
        Vec3 dir = new Vec3(target.x - centroid.x, 0, target.z - centroid.z);
        if (dir.lengthSqr() < 1e-3) dir = new Vec3(0, 0, 1);
        dir = dir.normalize();
        Vec3 right = new Vec3(-dir.z, 0, dir.x);
        int n = units.size();
        int cols = Math.max(1, (int) Math.ceil(Math.sqrt(n * 2.0)));
        double spacing = 1.7;
        // order units: melee first (front), ranged last (back)
        List<SoldierEntity> sorted = new ArrayList<>(units);
        sorted.sort(Comparator.comparingInt(s -> s.isRangedUnitArmed() ? 1 : 0));
        List<Vec3> result = new ArrayList<>();
        for (int i = 0; i < n; i++) result.add(target);
        for (int i = 0; i < n; i++) {
            int row = i / cols, col = i % cols;
            int inRow = Math.min(cols, n - row * cols);
            double offX = (col - (inRow - 1) / 2.0) * spacing;
            double offZ = -row * spacing;
            Vec3 p = target.add(right.scale(offX)).add(dir.scale(offZ));
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(p.x), (int) Math.floor(p.z));
            boolean unknownY = target.y < -500;
            if (unknownY || Math.abs(y - target.y) < 6) p = new Vec3(p.x, y, p.z);
            else p = new Vec3(p.x, target.y, p.z);
            result.set(units.indexOf(sorted.get(i)), p);
        }
        return result;
    }

    /** Patrol: every unit walks from where it stands through the waypoints and back, keeping its place in the formation. */
    public static void patrol(ServerLevel level, GameData d, Nation n, int[] ids, List<Vec3> waypoints) {
        List<SoldierEntity> units = ownUnits(level, n, ids);
        if (units.isEmpty() || waypoints.isEmpty()) return;
        Vec3 first = waypoints.get(0);
        List<Vec3> slots = formation(units, first, level);
        for (int i = 0; i < units.size(); i++) {
            SoldierEntity s = units.get(i);
            Vec3 off = slots.get(i).subtract(first);
            List<Vec3> route = new ArrayList<>();
            route.add(s.position());
            for (Vec3 w : waypoints) {
                Vec3 p = w.add(off.x, 0, off.z);
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(p.x), (int) Math.floor(p.z));
                route.add(w.y < -500 || Math.abs(y - w.y) < 6 ? new Vec3(p.x, y, p.z) : p);
            }
            s.patrol(route);
        }
    }

    public static void setStance(ServerLevel level, Nation n, int[] ids, Stance stance) {
        for (SoldierEntity s : ownUnits(level, n, ids)) s.setStance(stance);
    }

    public static void dismiss(ServerLevel level, GameData d, Nation n, int[] ids) {
        for (SoldierEntity s : ownUnits(level, n, ids)) {
            n.population++;
            s.dismiss();
        }
        d.markChanged();
    }

    /** All loaded soldiers of a nation. */
    public static List<SoldierEntity> army(Nation n) {
        return UnitRegistry.soldiersOf(n.id);
    }
}
