package com.endruqq.imperium.game;

import com.endruqq.imperium.game.building.Buildings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Buffs and nerfs from buildings and the state of the nation, applied to players. */
public final class TerritoryEffects {
    private TerritoryEffects() {}

    private static void give(ServerPlayer p, MobEffect e, int amp) {
        p.addEffect(new MobEffectInstance(e, 90, amp, true, false, true));
    }

    public static void tick(MinecraftServer server, GameData d) {
        if (d.phase != GamePhase.RUNNING) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator()) continue;
            Nation n = d.nationOf(p.getUUID());
            if (n == null || n.eliminated) continue;
            int here = d.nationIdAt(p.getX(), p.getZ());
            if (here == n.id) {
                if (d.hasActive(n.id, Buildings.MINE.id)) give(p, MobEffects.DIG_SPEED, 0);
                if (d.hasActive(n.id, Buildings.HOSPITAL.id) && p.getHealth() < p.getMaxHealth()) give(p, MobEffects.REGENERATION, 0);
                if (d.hasActive(n.id, Buildings.FORTRESS.id) && d.atWarWithAnyone(n.id)) give(p, MobEffects.DAMAGE_RESISTANCE, 0);
                if (d.hasActive(n.id, Buildings.TEMPLE.id)) give(p, MobEffects.LUCK, 0);
                if (n.abundant() && n.happiness > 70) give(p, MobEffects.MOVEMENT_SPEED, 0);
            }
            if (n.famine) give(p, MobEffects.HUNGER, 0);
            if (n.happiness < 20) give(p, MobEffects.WEAKNESS, 0);
        }
    }
}
