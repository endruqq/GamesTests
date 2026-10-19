package com.endruqq.imperium.entity.goal;

import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.Stance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Wounded soldiers fall back to their barracks (or away from danger) and heal. */
public class SoldierRetreatGoal extends Goal {
    private final SoldierEntity soldier;
    private int repath;

    public SoldierRetreatGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (soldier.getStance() == Stance.AGGRESSIVE && soldier.getHealth() > soldier.getMaxHealth() * 0.15) return false;
        return soldier.getHealth() < soldier.getMaxHealth() * soldier.retreatThreshold() && soldier.inCombat();
    }

    @Override
    public boolean canContinueToUse() {
        return soldier.getHealth() < soldier.getMaxHealth() * 0.6;
    }

    @Override
    public void start() {
        soldier.setRetreating(true);
        repath = 0;
    }

    @Override
    public void stop() {
        soldier.setRetreating(false);
    }

    @Override
    public void tick() {
        if (--repath > 0) return;
        repath = 20;
        BlockPos home = soldier.getHome();
        if (home != null && soldier.distanceToSqr(Vec3.atCenterOf(home)) > 36) {
            soldier.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.3D);
            return;
        }
        LivingEntity threat = soldier.getLastHurtByMob();
        if (threat != null) {
            Vec3 away = DefaultRandomPos.getPosAway(soldier, 14, 6, threat.position());
            if (away != null) soldier.getNavigation().moveTo(away.x, away.y, away.z, 1.3D);
        }
    }
}
