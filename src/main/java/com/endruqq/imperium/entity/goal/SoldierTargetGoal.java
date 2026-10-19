package com.endruqq.imperium.entity.goal;

import com.endruqq.imperium.entity.Allegiance;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.game.Game;
import com.endruqq.imperium.game.GameData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

/** Picks the most dangerous enemy nearby: closest, wounded, or attacking our comrades. */
public class SoldierTargetGoal extends Goal {
    private final SoldierEntity soldier;
    private LivingEntity candidate;
    private int cooldown;

    public SoldierTargetGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    private double range() {
        return soldier.isRangedUnitArmed() ? 22.0 : 16.0;
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0) return false;
        cooldown = 10 + soldier.getRandom().nextInt(5);
        if (soldier.getTarget() != null || !soldier.canAcquireTargets()) return false;
        GameData d = Game.data(soldier.level());
        if (d == null) return false;
        int me = soldier.getNationId();
        boolean archer = soldier.isRangedUnitArmed();
        List<LivingEntity> list = soldier.level().getEntitiesOfClass(LivingEntity.class,
                soldier.getBoundingBox().inflate(range(), 6.0, range()),
                e -> e != soldier && Allegiance.isHostile(d, me, e, archer));
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : list) {
            if (!soldier.getSensing().hasLineOfSight(e) && soldier.distanceToSqr(e) > 36) continue;
            double score = soldier.distanceTo(e);
            if (e.getHealth() < e.getMaxHealth() * 0.4) score -= 3;
            LivingEntity victim = e.getLastHurtMob();
            if (victim instanceof SoldierEntity s && s.getNationId() == me) score -= 4;
            if (e instanceof Player) score -= 1;
            if (e instanceof SoldierEntity s && s.isRangedUnitArmed() && !archer) score -= 1.5;
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        candidate = best;
        return best != null;
    }

    @Override
    public void start() {
        soldier.setTarget(candidate);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = soldier.getTarget();
        if (t == null || !t.isAlive()) return false;
        if (!soldier.canFight()) return false;
        GameData d = Game.data(soldier.level());
        if (d == null) return false;
        if (!Allegiance.isHostile(d, soldier.getNationId(), t, true)) return false;
        return soldier.distanceToSqr(t) < 40 * 40;
    }

    @Override
    public void stop() {
        LivingEntity t = soldier.getTarget();
        if (t == null || !t.isAlive() || t == candidate) soldier.setTarget(null);
        candidate = null;
    }
}
