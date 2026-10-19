package com.endruqq.imperium.entity.goal;

import com.endruqq.imperium.entity.OrderType;
import com.endruqq.imperium.entity.SoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Executes movement orders: move, attack-move, return, hold position, follow. */
public class SoldierOrderGoal extends Goal {
    private final SoldierEntity soldier;
    private int repath;

    public SoldierOrderGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (soldier.isRetreating() || soldier.isSurrendered()) return false;
        OrderType o = soldier.getOrder();
        if (o == OrderType.ATTACK_CORE) return false;
        if (o != OrderType.MOVE && o != OrderType.RETURN && soldier.getTarget() != null) return false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        OrderType o = soldier.getOrder();
        switch (o) {
            case MOVE, ATTACK_MOVE -> moveTo(soldier.getOrderPos(), 1.15D, true);
            case RETURN -> {
                BlockPos home = soldier.getHome();
                Vec3 dest = home != null ? Vec3.atBottomCenterOf(home) : soldier.getOrderPos();
                moveTo(dest, 1.15D, true);
            }
            case HOLD -> {
                Vec3 anchor = soldier.getOrderPos();
                if (soldier.position().distanceToSqr(anchor) > 2.5 * 2.5) moveTo(anchor, 1.0D, false);
            }
            case FOLLOW -> follow();
            case PATROL -> {
                Vec3 p = soldier.patrolPoint();
                if (p == null) {
                    soldier.hold(soldier.position());
                } else if (moveTo(p, 0.95D, false)) {
                    soldier.advancePatrol();
                    repath = 0;
                }
            }
            case ATTACK_TARGET -> {
                Entity e = findEntity();
                if (e instanceof LivingEntity le && le.isAlive()) {
                    soldier.setTarget(le);
                } else {
                    soldier.hold(soldier.position());
                }
            }
            default -> {
            }
        }
    }

    private Entity findEntity() {
        if (soldier.getOrderEntity() == null || !(soldier.level() instanceof ServerLevel sl)) return null;
        return sl.getEntity(soldier.getOrderEntity());
    }

    /** @return true when the destination is reached */
    private boolean moveTo(Vec3 dest, double speed, boolean finishToHold) {
        double distSq = soldier.position().distanceToSqr(dest.x, soldier.getY(), dest.z);
        if (distSq < 1.6 * 1.6 && Math.abs(soldier.getY() - dest.y) < 3) {
            soldier.getNavigation().stop();
            if (finishToHold) soldier.hold(dest);
            return true;
        }
        if (--repath <= 0 || soldier.getNavigation().isDone()) {
            boolean ok = soldier.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
            repath = ok ? 30 : 15;
            if (!ok && distSq < 4 * 4) {
                if (finishToHold) soldier.hold(soldier.position());
                return true;
            }
        }
        return false;
    }

    private void follow() {
        Entity leader = findEntity();
        if (leader == null || !leader.isAlive()) {
            soldier.hold(soldier.position());
            return;
        }
        double d = soldier.distanceToSqr(leader);
        if (d > 64 * 64) {
            soldier.hold(soldier.position());
            return;
        }
        if (d > 5 * 5) {
            if (--repath <= 0 || soldier.getNavigation().isDone()) {
                soldier.getNavigation().moveTo(leader, d > 12 * 12 ? 1.3D : 1.05D);
                repath = 10;
            }
        } else if (d < 3 * 3) {
            soldier.getNavigation().stop();
        }
    }
}
