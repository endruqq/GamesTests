package com.endruqq.imperium.game;

import com.endruqq.imperium.ImperiumConfig;
import com.endruqq.imperium.entity.CitizenEntity;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.UnitRegistry;
import com.endruqq.imperium.game.building.Building;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.game.tech.Tech;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Province cores, conquest, elimination and victory. */
public final class WarManager {
    private static final Map<UUID, Long> HIT_COOLDOWN = new HashMap<>();

    private WarManager() {}

    public static Province provinceWithCore(GameData d, BlockPos pos) {
        for (Province p : d.provinces.values()) if (pos.equals(p.core)) return p;
        return null;
    }

    /** Neutral provinces can be taken by anyone, owned ones only by nations at war with the owner. */
    public static boolean canAttackCore(GameData d, int attacker, Province p) {
        if (attacker < 0 || p == null || p.owner == attacker) return false;
        return p.owner < 0 || d.atWar(attacker, p.owner);
    }

    public static double maxCoreHp(GameData d, Province p) {
        double hp = ImperiumConfig.CORE_BASE_HP.get();
        Nation owner = d.nation(p.owner);
        if (owner != null) {
            if (owner.capitalProvince == p.id) hp *= 1.5;
            if (owner.has(Tech.FORTIFICATION)) hp *= 1.5;
            if (d.hasActive(owner.id, Buildings.FORTRESS.id)) hp *= 2.0;
        }
        return hp;
    }

    public static void playerHitsCore(ServerLevel level, ServerPlayer player, BlockPos pos) {
        GameData d = GameData.get(level.getServer());
        Province p = provinceWithCore(d, pos);
        if (p == null) return;
        int attacker = d.nationIdOf(player.getUUID());
        if (d.phase != GamePhase.RUNNING) return;
        if (!canAttackCore(d, attacker, p)) {
            player.displayClientMessage(Component.translatable(attacker == p.owner ? "imperium.msg.own_core" : "imperium.msg.core_not_at_war")
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        long now = level.getGameTime();
        Long last = HIT_COOLDOWN.get(player.getUUID());
        if (last != null && now - last < 8) return;
        HIT_COOLDOWN.put(player.getUUID(), now);
        double dmg = 1.0 + player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        damageCore(level, p, attacker, dmg, player);
    }

    /** Deals damage to a province core; conquers the province when it reaches zero. */
    public static void damageCore(ServerLevel level, Province p, int attackerNation, double amount, Entity source) {
        GameData d = GameData.get(level.getServer());
        if (d.phase != GamePhase.RUNNING || p.core == null || !canAttackCore(d, attackerNation, p)) return;
        Nation attacker = d.nation(attackerNation);
        if (attacker != null && attacker.has(Tech.GUNPOWDER) && source instanceof SoldierEntity s
                && s.getUnitType() == com.endruqq.imperium.entity.UnitType.SAPPER) amount *= 2;
        boolean firstHit = !p.underAttack(d.gameTick);
        p.coreMaxHp = maxCoreHp(d, p);
        p.coreHp -= amount;
        p.lastAttackTick = d.gameTick;
        p.lastAttacker = attackerNation;
        level.sendParticles(ParticleTypes.CRIT, p.core.getX() + 0.5, p.core.getY() + 0.7, p.core.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.2);
        level.playSound(null, p.core, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4F, 1.6F);
        if (firstHit) {
            Notify.nation(level.getServer(), d, p.owner, Component.translatable("imperium.msg.core_under_attack", p.name,
                    attacker == null ? "?" : attacker.name).withStyle(ChatFormatting.RED), true);
        }
        if (source instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("imperium.msg.core_hp", p.name, (int) Math.max(0, p.coreHp), (int) p.coreMaxHp)
                    .withStyle(ChatFormatting.GOLD), true);
        }
        if (p.coreHp <= 0) capture(level, d, p, attackerNation);
        d.markChanged();
    }

    /** Transfers a province to a new owner. */
    public static void capture(ServerLevel level, GameData d, Province p, int newOwner) {
        int oldOwner = p.owner;
        Nation loser = d.nation(oldOwner);
        Nation winner = d.nation(newOwner);
        p.owner = newOwner;
        p.coreMaxHp = maxCoreHp(d, p);
        p.coreHp = p.coreMaxHp * 0.5;
        p.lastAttackTick = -100000;
        boolean wasCapital = loser != null && loser.capitalProvince == p.id;

        // buildings inside the province change hands
        for (Building b : d.buildings.values()) {
            Province bp = d.provinceAt(b.pos);
            if (bp != null && bp.id == p.id) b.nation = newOwner;
        }
        // citizens and population
        if (loser != null && winner != null) {
            int lostCells = Math.max(1, p.cellCount);
            int totalCells = Math.max(lostCells, d.cellsOf(oldOwner) + lostCells);
            int moved = (int) Math.round(loser.population * (lostCells / (double) totalCells));
            moved = Math.min(moved, Math.max(0, loser.population - 1));
            loser.population -= moved;
            winner.population += moved;
            loser.happiness = Math.max(0, loser.happiness - 10);
            loser.lastProvinceLostTick = d.gameTick;
            if (p.originalOwner == newOwner) winner.happiness = Math.min(100, winner.happiness + 10);
        }
        for (CitizenEntity c : UnitRegistry.citizens()) {
            if (c.getNationId() == oldOwner) {
                Province cp = d.provinceAt(c.blockPosition());
                if (cp != null && cp.id == p.id) c.setNationId(newOwner);
            }
        }
        // the people living in the province now belong to the conqueror (and keep working in its buildings)
        for (Person person : d.people.values()) {
            if (person.nation != oldOwner) continue;
            CitizenEntity c = PeopleManager.entity(person);
            Province hp = c != null ? d.provinceAt(c.blockPosition()) : person.home == null ? null : d.provinceAt(person.home);
            if (hp != null && hp.id == p.id) person.nation = newOwner;
        }
        PeopleManager.recountWorkers(d);
        // spoils of war
        if (loser != null && winner != null) {
            Resources loot = new Resources();
            for (ResourceType t : new ResourceType[]{ResourceType.GOLD, ResourceType.STEEL, ResourceType.GOODS, ResourceType.FOOD}) {
                double take = Math.floor(loser.stock.get(t) * 0.15);
                loser.stock.add(t, -take);
                loot.add(t, take);
            }
            winner.stock.add(loot, 1);
            if (!loot.isEmpty()) {
                Notify.nation(level.getServer(), d, winner.id, Component.translatable("imperium.msg.loot", p.name, loot.describe(1))
                        .withStyle(ChatFormatting.GOLD), false);
            }
            Relation wr = d.relation(winner.id, loser.id);
            if (wr != null) wr.addScore(winner.id, 15);
            // the garrison lays down arms; when the capital falls, the whole army does
            int surrendered = wasCapital ? PrisonerManager.surrenderArmy(level, d, loser, winner.id)
                    : PrisonerManager.surrenderGarrison(level, d, p, loser.id, winner.id);
            if (surrendered > 0) {
                Notify.all(level.getServer(), Component.translatable(wasCapital ? "imperium.msg.army_surrenders" : "imperium.msg.garrison_surrenders",
                        loser.name, surrendered, winner.name).withStyle(ChatFormatting.YELLOW), true);
                winner.addGossip(Gossip.of("surrender", loser.name, String.valueOf(surrendered), p.name, d.gameTick));
            }
        }
        if (level.isLoaded(p.core)) {
            TowerBuilder.paintFlags(level, p.core, winner, winner != null && winner.capitalProvince == p.id);
            level.playSound(null, p.core, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 4.0F, 1.0F);
        }
        Component msg = Component.translatable("imperium.msg.province_captured", p.name,
                winner == null ? "?" : winner.name, loser == null ? "?" : loser.name).withStyle(ChatFormatting.GOLD);
        Notify.all(level.getServer(), msg, true);
        if (winner != null) winner.addLog(Component.translatable("imperium.log.captured", p.name));
        if (loser != null) loser.addLog(Component.translatable("imperium.log.lost", p.name, winner == null ? "?" : winner.name));

        if (loser != null && wasCapital) relocateCapital(level, d, loser);
        if (loser != null && d.provincesOf(loser.id).isEmpty()) eliminate(level, d, loser, winner);
        d.markTerritoryChanged();
        d.markChanged();
        checkVictory(level, d);
    }

    /** A province changes hands by treaty: no plunder, no prisoners. */
    public static void cede(ServerLevel level, GameData d, Province p, int newOwner) {
        int oldOwner = p.owner;
        Nation loser = d.nation(oldOwner);
        Nation winner = d.nation(newOwner);
        if (winner == null || oldOwner == newOwner) return;
        boolean wasCapital = loser != null && loser.capitalProvince == p.id;
        p.owner = newOwner;
        p.coreMaxHp = maxCoreHp(d, p);
        p.coreHp = Math.min(p.coreHp, p.coreMaxHp);
        p.lastAttackTick = -100000;
        for (Building b : d.buildings.values()) {
            Province bp = d.provinceAt(b.pos);
            if (bp != null && bp.id == p.id) b.nation = newOwner;
        }
        for (CitizenEntity c : UnitRegistry.citizens()) {
            Province cp = d.provinceAt(c.blockPosition());
            if (c.getNationId() == oldOwner && cp != null && cp.id == p.id) c.setNationId(newOwner);
        }
        for (Person person : d.people.values()) {
            if (person.nation != oldOwner) continue;
            CitizenEntity c = PeopleManager.entity(person);
            Province hp = c != null ? d.provinceAt(c.blockPosition()) : person.home == null ? null : d.provinceAt(person.home);
            if (hp != null && hp.id == p.id) person.nation = newOwner;
        }
        PeopleManager.recountWorkers(d);
        if (loser != null) {
            int lostCells = Math.max(1, p.cellCount);
            int totalCells = Math.max(lostCells, d.cellsOf(oldOwner) + lostCells);
            int moved = Math.min((int) Math.round(loser.population * (lostCells / (double) totalCells)), Math.max(0, loser.population - 1));
            loser.population -= moved;
            winner.population += moved;
            loser.lastProvinceLostTick = d.gameTick;
            loser.addLog(Component.translatable("imperium.log.ceded", p.name, winner.name));
        }
        winner.addLog(Component.translatable("imperium.log.received", p.name));
        if (p.core != null && level.isLoaded(p.core)) TowerBuilder.paintFlags(level, p.core, winner, winner.capitalProvince == p.id);
        Notify.all(level.getServer(), Component.translatable("imperium.msg.province_ceded", p.name, loser == null ? "?" : loser.name, winner.name)
                .withStyle(ChatFormatting.GOLD), true);
        if (loser != null && wasCapital) relocateCapital(level, d, loser);
        if (loser != null && d.provincesOf(loser.id).isEmpty()) eliminate(level, d, loser, winner);
        d.markTerritoryChanged();
        d.markChanged();
        checkVictory(level, d);
    }

    private static void relocateCapital(ServerLevel level, GameData d, Nation n) {
        Province best = null;
        for (Province p : d.provincesOf(n.id)) if (best == null || p.cellCount > best.cellCount) best = p;
        n.capitalProvince = best == null ? -1 : best.id;
        if (best != null) {
            n.happiness = Math.max(0, n.happiness - 15);
            Notify.nation(level.getServer(), d, n.id, Component.translatable("imperium.msg.capital_moved", best.name)
                    .withStyle(ChatFormatting.RED), true);
            GameManager.updateRespawns(level, d, n);
        }
    }

    public static void eliminate(ServerLevel level, GameData d, Nation loser, Nation by) {
        loser.eliminated = true;
        for (Nation other : d.nations.values()) {
            if (other.id == loser.id) continue;
            Relation r = d.relation(loser.id, other.id);
            if (r != null) {
                r.type = RelationType.PEACE;
                r.trade = false;
            }
        }
        d.proposals.removeIf(pr -> pr.from == loser.id || pr.to == loser.id);
        List<SoldierEntity> soldiers = new ArrayList<>(UnitRegistry.soldiersOf(loser.id));
        for (SoldierEntity s : soldiers) {
            if (by != null) {
                if (!s.isSurrendered()) s.surrender(by.id, d.gameTick);
            } else {
                s.discard();
            }
        }
        if (by == null) loser.soldierCount = 0;
        for (Person p : d.people.values()) {
            if (p.nation == loser.id && by != null) p.nation = by.id;
        }
        Notify.all(level.getServer(), Component.translatable("imperium.msg.eliminated", loser.name,
                by == null ? "?" : by.name).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        d.markChanged();
    }

    public static void checkVictory(ServerLevel level, GameData d) {
        if (d.phase != GamePhase.RUNNING) return;
        List<Nation> alive = new ArrayList<>(d.activeNations());
        if (alive.isEmpty()) return;
        boolean allAllied = true;
        for (int i = 0; i < alive.size() && allAllied; i++) {
            for (int j = i + 1; j < alive.size(); j++) {
                if (!d.allied(alive.get(i).id, alive.get(j).id)) {
                    allAllied = false;
                    break;
                }
            }
        }
        if (alive.size() == 1 || allAllied) {
            StringBuilder sb = new StringBuilder();
            for (Nation n : alive) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(n.name);
            }
            d.winner = sb.toString();
            d.phase = GamePhase.ENDED;
            Notify.all(level.getServer(), Component.translatable("imperium.msg.victory", d.winner)
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                        Component.translatable("imperium.title.victory").withStyle(ChatFormatting.GOLD)));
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal(d.winner)));
            }
            d.markChanged();
        }
    }

    /** Cores slowly regenerate when nobody attacks them. Called once per second. */
    public static void regenerate(GameData d) {
        for (Province p : d.provinces.values()) {
            if (p.core == null) continue;
            p.coreMaxHp = maxCoreHp(d, p);
            if (p.coreHp < p.coreMaxHp && d.gameTick - p.lastAttackTick > 600) {
                p.coreHp = Math.min(p.coreMaxHp, p.coreHp + Math.max(1, p.coreMaxHp / 200));
            } else if (p.coreHp > p.coreMaxHp) {
                p.coreHp = p.coreMaxHp;
            }
        }
    }

    public static void describeCore(ServerLevel level, ServerPlayer player, BlockPos pos) {
        GameData d = GameData.get(level.getServer());
        Province p = provinceWithCore(d, pos);
        if (p == null) {
            player.sendSystemMessage(Component.translatable("imperium.msg.core_unbound").withStyle(ChatFormatting.GRAY));
            return;
        }
        Nation owner = d.nation(p.owner);
        player.sendSystemMessage(Component.translatable("imperium.msg.core_info", p.name, owner == null ? "-" : owner.name,
                (int) p.coreHp, (int) maxCoreHp(d, p)).withStyle(ChatFormatting.GOLD));
    }

    public static void clear() {
        HIT_COOLDOWN.clear();
    }
}
