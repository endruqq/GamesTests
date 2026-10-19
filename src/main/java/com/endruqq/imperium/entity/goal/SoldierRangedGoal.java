package com.endruqq.imperium.entity.goal;

import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.Stance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Archers keep their distance, kite melee attackers and shoot when they have line of sight. */
public class SoldierRangedGoal extends Goal {
    private final SoldierEntity soldier;
    private int repath;
    private int kiteCooldown;
    private int seeTime;

    public SoldierRangedGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean valid() {
        LivingEntity t = soldier.getTarget();
        return t != null && t.isAlive() && soldier.isRangedUnitArmed() && soldier.canFight();
    }

    @Override
    public boolean canUse() {
        return valid();
    }

    @Override
    public boolean canContinueToUse() {
        return valid() && soldier.withinLeash();
    }

    @Override
    public void start() {
        soldier.setAggressive(true);
        seeTime = 0;
    }

    @Override
    public void stop() {
        soldier.setAggressive(false);
        if (soldier.isUsingItem()) soldier.stopUsingItem();
        if (!soldier.withinLeash() || !soldier.canFight()) soldier.setTarget(null);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = soldier.getTarget();
        if (target == null) return;
        double dist = soldier.distanceTo(target);
        boolean canSee = soldier.getSensing().hasLineOfSight(target);
        seeTime = canSee ? seeTime + 1 : 0;
        soldier.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (kiteCooldown > 0) kiteCooldown--;

        if (dist < 5.0 && soldier.getStance() != Stance.PASSIVE && kiteCooldown <= 0) {
            Vec3 away = DefaultRandomPos.getPosAway(soldier, 10, 5, target.position());
            if (away != null) {
                soldier.getNavigation().moveTo(away.x, away.y, away.z, 1.3D);
                kiteCooldown = 30;
            }
        } else if (dist > 16.0 || !canSee) {
            if (--repath <= 0) {
                soldier.getNavigation().moveTo(target, 1.1D);
                repath = 10;
            }
        } else if (kiteCooldown <= 0) {
            soldier.getNavigation().stop();
        }

        if (canSee && dist <= 22.0 && seeTime > 5) {
            if (!soldier.isUsingItem()) {
                soldier.startUsingItem(InteractionHand.MAIN_HAND);
            } else if (soldier.getTicksUsingItem() >= 20) {
                int used = soldier.getTicksUsingItem();
                soldier.stopUsingItem();
                soldier.performRangedAttack(target, BowItem.getPowerForTime(used));
            }
        } else if (soldier.isUsingItem() && !canSee) {
            soldier.stopUsingItem();
        }
    }
}
