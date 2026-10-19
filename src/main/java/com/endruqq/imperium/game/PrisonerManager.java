package com.endruqq.imperium.game;

import com.endruqq.imperium.entity.CitizenEntity;
import com.endruqq.imperium.entity.OrderType;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.UnitRegistry;
import com.endruqq.imperium.entity.UnitType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Prisoners of war. When a province falls, its garrison lays down arms; when a capital falls, the whole army does.
 * The captors can talk prisoners into serving them (as soldiers or as workers) - or let them go home. Killing a prisoner is
 * a war crime the whole world remembers.
 */
public final class PrisonerManager {
    /** prisoners nobody deals with are released after this time */
    public static final long CAPTIVITY_TICKS = 20L * 60 * 10;
    public static final int BRIBE_GOLD = 20;

    public enum Offer { ARMY, WORK, BRIBE, RELEASE }

    private PrisonerManager() {}

    /** Soldiers of the loser inside a captured province (or near its core) surrender. */
    public static int surrenderGarrison(ServerLevel level, GameData d, Province p, int loser, int winner) {
        int c = 0;
        for (SoldierEntity s : UnitRegistry.soldiersOf(loser)) {
            if (s.isSurrendered()) continue;
            Province at = d.provinceAt(s.blockPosition());
            boolean near = p.core != null && s.distanceToSqr(Vec3.atCenterOf(p.core)) < 48 * 48;
            if ((at != null && at.id == p.id) || near) {
                surrender(level, d, s, winner);
                c++;
            }
        }
        return c;
    }

    /** The whole army lays down arms (the capital fell or the nation collapsed). */
    public static int surrenderArmy(ServerLevel level, GameData d, Nation loser, int winner) {
        int c = 0;
        for (SoldierEntity s : new ArrayList<>(UnitRegistry.soldiersOf(loser.id))) {
            if (!s.isSurrendered()) {
                surrender(level, d, s, winner);
                c++;
            }
        }
        return c;
    }

    private static void surrender(ServerLevel level, GameData d, SoldierEntity s, int winner) {
        s.surrender(winner, d.gameTick);
        level.sendParticles(ParticleTypes.POOF, s.getX(), s.getY() + 1.2, s.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
        Relation r = d.relation(winner, s.getNationId());
        if (r != null) r.addScore(winner, 1);
    }

    /** Chance that a prisoner accepts an offer. */
    public static double chance(GameData d, SoldierEntity s, Offer offer) {
        Nation own = d.nation(s.getNationId());
        Nation captor = d.nation(s.getCaptor());
        double c = 0.35;
        if (own == null || own.eliminated) c += 0.35;
        else {
            if (own.famine || own.unpaid) c += 0.2;
            c -= (own.happiness - 50) / 200.0;
        }
        if (captor != null) c += (captor.happiness - 50) / 200.0;
        if (offer == Offer.WORK) c += 0.1;
        if (offer == Offer.BRIBE) c += 0.25;
        if (s.getUnitType() == UnitType.KNIGHT) c -= 0.15;
        if (s.getUnitType() == UnitType.MILITIA) c += 0.1;
        c -= s.getPersuadeAttempts() * 0.12;
        return Math.max(0.03, Math.min(0.95, c));
    }

    /**
     * The captor makes an offer.
     *
     * @return dialog result key suffix: "yes", "no", "flee", "released", "full", "poor"
     */
    public static String offer(ServerLevel level, GameData d, SoldierEntity s, Nation captor, Offer offer, RandomSource rnd) {
        if (!s.isSurrendered() || captor == null || s.getCaptor() != captor.id) return "no";
        Nation own = d.nation(s.getNationId());
        if (offer == Offer.RELEASE) {
            release(level, d, s, true);
            return "released";
        }
        if ((offer == Offer.ARMY || offer == Offer.BRIBE) && captor.soldierCount >= MilitaryManager.maxArmy(captor)) return "full";
        if (offer == Offer.BRIBE) {
            if (captor.stock.get(ResourceType.GOLD) < BRIBE_GOLD) return "poor";
            captor.stock.add(ResourceType.GOLD, -BRIBE_GOLD);
        }
        double c = chance(d, s, offer);
        s.addPersuadeAttempt();
        if (rnd.nextDouble() < c) {
            if (offer == Offer.WORK) toCitizen(level, d, s, captor);
            else joinArmy(level, d, s, captor);
            return "yes";
        }
        if (own != null && !own.eliminated && rnd.nextDouble() < 0.15) {
            // runs off home at the first chance
            release(level, d, s, false);
            return "flee";
        }
        return "no";
    }

    public static void joinArmy(ServerLevel level, GameData d, SoldierEntity s, Nation captor) {
        Nation own = d.nation(s.getNationId());
        if (own != null) own.soldierCount = Math.max(0, own.soldierCount - 1);
        s.rearm();
        s.setNationId(captor.id);
        s.hold(s.position());
        captor.soldierCount++;
        captor.addLog(Component.translatable("imperium.log.prisoner_joined", s.getSoldierName()));
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, s.getX(), s.getY() + 1.5, s.getZ(), 8, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, s.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        d.markChanged();
    }

    public static void toCitizen(ServerLevel level, GameData d, SoldierEntity s, Nation captor) {
        Nation own = d.nation(s.getNationId());
        if (own != null) own.soldierCount = Math.max(0, own.soldierCount - 1);
        captor.population++;
        CitizenEntity c = Economy.spawnCitizen(level, d, captor, s.blockPosition());
        if (c != null) {
            Person p = PeopleManager.get(d, c.getUUID());
            c.setCitizenName(s.getSoldierName());
            if (p != null) p.name = s.getSoldierName();
            Province cap = d.provinces.get(captor.capitalProvince);
            if (cap != null && cap.core != null) {
                c.setHome(cap.core);
                if (p != null) p.home = cap.core;
            }
        }
        captor.addLog(Component.translatable("imperium.log.prisoner_settled", s.getSoldierName()));
        s.discard();
        d.markChanged();
    }

    /** Let a prisoner go: back home with his weapons. */
    public static void release(ServerLevel level, GameData d, SoldierEntity s, boolean mercy) {
        int captor = s.getCaptor();
        s.rearm();
        s.order(OrderType.RETURN, s.position(), null, -1);
        if (mercy && captor >= 0) {
            Relation r = d.relation(captor, s.getNationId());
            if (r != null) r.addOpinion(s.getNationId(), captor, 4);
            Nation c = d.nation(captor);
            Nation own = d.nation(s.getNationId());
            if (c != null && own != null) c.addGossip(Gossip.of("mercy", s.getSoldierName(), own.name, "", d.gameTick));
        }
        d.markChanged();
    }

    /** Killing a prisoner. */
    public static void warCrime(ServerLevel level, GameData d, SoldierEntity victim, int killer) {
        Nation k = d.nation(killer);
        Nation v = d.nation(victim.getNationId());
        if (k == null) return;
        for (Nation o : d.nations.values()) {
            if (o.id == killer || o.eliminated) continue;
            Relation r = d.relation(o.id, killer);
            if (r != null) r.addOpinion(o.id, killer, o.id == victim.getNationId() ? -25 : -10);
        }
        k.happiness = Math.max(0, k.happiness - 4);
        if (v != null && !v.eliminated) WarReasons.add(level.getServer(), d, v.id, killer, CasusBelli.Type.ATROCITY);
        for (Nation o : d.nations.values()) {
            if (o.id != killer && !o.eliminated && o.id != victim.getNationId()) o.addGossip(Gossip.of("execution", victim.getSoldierName(), k.name, "", d.gameTick));
        }
        Notify.all(level.getServer(), Component.translatable("imperium.msg.war_crime", k.name, victim.getSoldierName())
                .withStyle(ChatFormatting.DARK_RED), true);
        d.markChanged();
    }

    /** Once per second: forgotten prisoners go home (or settle with the captor if their nation is gone). */
    public static void tick(ServerLevel level, GameData d) {
        for (SoldierEntity s : UnitRegistry.soldiers()) {
            if (!s.isSurrendered() || d.gameTick - s.getSurrenderTick() < CAPTIVITY_TICKS) continue;
            Nation own = d.nation(s.getNationId());
            Nation captor = d.nation(s.getCaptor());
            if ((own == null || own.eliminated) && captor != null && !captor.eliminated) toCitizen(level, d, s, captor);
            else release(level, d, s, false);
        }
    }

    /** Bots try to win over their prisoners. */
    public static void botCycle(ServerLevel level, GameData d, Nation bot) {
        RandomSource rnd = level.random;
        List<SoldierEntity> prisoners = new ArrayList<>();
        for (SoldierEntity s : UnitRegistry.soldiers()) if (s.isSurrendered() && s.getCaptor() == bot.id) prisoners.add(s);
        for (SoldierEntity s : prisoners) {
            if (rnd.nextInt(3) != 0) continue;
            Offer o = bot.soldierCount < MilitaryManager.maxArmy(bot) ? Offer.ARMY : Offer.WORK;
            if (s.getPersuadeAttempts() >= 3) o = Offer.RELEASE;
            offer(level, d, s, bot, o, rnd);
        }
    }

    public static int prisonersOf(GameData d, int captor) {
        int c = 0;
        for (SoldierEntity s : UnitRegistry.soldiers()) if (s.isSurrendered() && s.getCaptor() == captor) c++;
        return c;
    }
}
