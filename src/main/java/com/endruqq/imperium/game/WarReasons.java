package com.endruqq.imperium.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Reasons to go to war. A war declared with a casus belli is "just": the people rally behind it. A war of aggression
 * (no reason) makes the own people unhappy and the whole world angry.
 */
public final class WarReasons {
    public static final long CLAIM_TICKS = 20L * 60 * 3;
    public static final int CLAIM_COST = 60;

    private WarReasons() {}

    public static void add(MinecraftServer server, GameData d, int holder, int target, CasusBelli.Type type) {
        if (holder < 0 || target < 0 || holder == target || type.computed()) return;
        long until = d.gameTick + type.minutes * 60L * 20L;
        for (CasusBelli c : d.casusBelli) {
            if (c.holder == holder && c.target == target && c.type == type) {
                c.expires = Math.max(c.expires, until);
                return;
            }
        }
        d.casusBelli.add(CasusBelli.of(holder, target, type, until));
        Nation t = d.nation(target);
        if (server != null && t != null) {
            Notify.nation(server, d, holder, Component.translatable("imperium.msg.cb_gained", t.name, type.title(), type.minutes)
                    .withStyle(ChatFormatting.GOLD), false);
        }
        d.markChanged();
    }

    /** All valid reasons {@code holder} has to attack {@code target}. */
    public static List<CasusBelli.Type> reasons(GameData d, int holder, int target) {
        List<CasusBelli.Type> l = new ArrayList<>();
        for (CasusBelli c : d.casusBelli) {
            if (c.holder == holder && c.target == target && c.expires > d.gameTick && !l.contains(c.type)) l.add(c.type);
        }
        for (Province p : d.provinces.values()) {
            if (p.originalOwner == holder && p.owner == target && p.cellCount > 0) {
                l.add(CasusBelli.Type.RECONQUEST);
                break;
            }
        }
        for (Nation x : d.nations.values()) {
            if (x.eliminated || x.id == holder || x.id == target || !d.atWar(target, x.id)) continue;
            if (d.allied(holder, x.id) && !l.contains(CasusBelli.Type.DEFEND_ALLY)) l.add(CasusBelli.Type.DEFEND_ALLY);
            Relation r = d.relations.get(Relation.key(holder, x.id));
            if (r != null && r.guarantees(holder, x.id) && !l.contains(CasusBelli.Type.GUARANTEE)) l.add(CasusBelli.Type.GUARANTEE);
        }
        return l;
    }

    public static boolean justified(GameData d, int holder, int target) {
        return !reasons(d, holder, target).isEmpty();
    }

    /** Seconds left for a stored reason, -1 if it never expires (computed) or 0 if absent. */
    public static long secondsLeft(GameData d, int holder, int target, CasusBelli.Type type) {
        if (type.computed()) return -1;
        long best = 0;
        for (CasusBelli c : d.casusBelli) {
            if (c.holder == holder && c.target == target && c.type == type) best = Math.max(best, (c.expires - d.gameTick) / 20);
        }
        return best;
    }

    /** A claim being fabricated against the target, or null. */
    public static CasusBelli claimInProgress(GameData d, int holder, int target) {
        for (CasusBelli c : d.claimsInProgress) if (c.holder == holder && c.target == target) return c;
        return null;
    }

    /** Send spies to forge a claim. @return null on success, else the reason */
    public static Component fabricate(MinecraftServer server, GameData d, Nation from, Nation to) {
        if (claimInProgress(d, from.id, to.id) != null) return Component.translatable("imperium.err.claim_in_progress");
        if (from.stock.get(ResourceType.GOLD) < CLAIM_COST) {
            return Component.translatable("imperium.err.cost", Resources.of(ResourceType.GOLD, CLAIM_COST).describe(1));
        }
        from.stock.add(ResourceType.GOLD, -CLAIM_COST);
        d.claimsInProgress.add(CasusBelli.of(from.id, to.id, CasusBelli.Type.CLAIM, d.gameTick + CLAIM_TICKS));
        Notify.nation(server, d, from.id, Component.translatable("imperium.msg.claim_started", to.name, CLAIM_TICKS / 1200)
                .withStyle(ChatFormatting.GRAY), false);
        d.markChanged();
        return null;
    }

    /** Called once per second: expire reasons, finish forged claims. */
    public static void tick(MinecraftServer server, GameData d) {
        boolean changed = d.casusBelli.removeIf(c -> c.expires <= d.gameTick);
        RandomSource rnd = server.overworld().random;
        Iterator<CasusBelli> it = d.claimsInProgress.iterator();
        List<CasusBelli> done = new ArrayList<>();
        while (it.hasNext()) {
            CasusBelli c = it.next();
            if (d.gameTick >= c.expires) {
                done.add(c);
                it.remove();
            }
        }
        for (CasusBelli c : done) {
            Nation from = d.nation(c.holder), to = d.nation(c.target);
            if (from == null || to == null || from.eliminated || to.eliminated) continue;
            if (rnd.nextDouble() < 0.25) {
                // the spies got caught
                add(server, d, to.id, from.id, CasusBelli.Type.SPY_CAUGHT);
                d.relation(to.id, from.id).addOpinion(to.id, from.id, -20);
                Notify.all(server, Component.translatable("imperium.msg.spies_caught", from.name, to.name).withStyle(ChatFormatting.DARK_RED), true);
            } else {
                add(server, d, from.id, to.id, CasusBelli.Type.CLAIM);
            }
            changed = true;
        }
        if (changed) d.markChanged();
    }
}
