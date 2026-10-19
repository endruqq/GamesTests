package com.endruqq.imperium.entity.goal;

import com.endruqq.imperium.entity.SoldierEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class SoldierMeleeGoal extends MeleeAttackGoal {
    private final SoldierEntity soldier;

    public SoldierMeleeGoal(SoldierEntity soldier) {
        super(soldier, 1.2D, true);
        this.soldier = soldier;
    }

    @Override
    public boolean canUse() {
        return !soldier.isRangedUnitArmed() && soldier.canFight() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !soldier.isRangedUnitArmed() && soldier.canFight() && soldier.withinLeash() && super.canContinueToUse();
    }

    @Override
    public void stop() {
        boolean leashBroken = !soldier.withinLeash();
        super.stop();
        if (leashBroken || !soldier.canFight()) soldier.setTarget(null);
    }
}
