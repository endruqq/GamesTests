package com.endruqq.imperium.entity.goal;

import com.endruqq.imperium.entity.OrderType;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.game.Game;
import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.Province;
import com.endruqq.imperium.game.WarManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** March to an enemy province core and hammer it until the province falls. */
public class SoldierAttackCoreGoal extends Goal {
    private final SoldierEntity soldier;
    private int repath;
    private int swing;

    public SoldierAttackCoreGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private Province province() {
        GameData d = Game.data(soldier.level());
        if (d == null) return null;
        Province p = d.provinces.get(soldier.getOrderProvince());
        if (p == null || p.core == null || !WarManager.canAttackCore(d, soldier.getNationId(), p)) return null;
        return p;
    }

    @Override
    public boolean canUse() {
        return soldier.getOrder() == OrderType.ATTACK_CORE && soldier.getTarget() == null && !soldier.isRetreating();
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
        Province p = province();
        if (p == null) {
            soldier.hold(soldier.position());
            return;
        }
        BlockPos core = p.core;
        Vec3 c = Vec3.atCenterOf(core);
        double distSq = soldier.distanceToSqr(c);
        soldier.getLookControl().setLookAt(c.x, c.y, c.z);
        if (distSq > 3.2 * 3.2) {
            if (--repath <= 0 || soldier.getNavigation().isDone()) {
                soldier.getNavigation().moveTo(c.x, core.getY(), c.z, 1.1D);
                repath = 20;
            }
            return;
        }
        soldier.getNavigation().stop();
        if (--swing <= 0) {
            swing = 20;
            soldier.swing(InteractionHand.MAIN_HAND);
            double dmg = (2.0 + soldier.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE))
                    * soldier.getUnitType().coreDamageMult;
            if (soldier.level() instanceof ServerLevel sl) {
                WarManager.damageCore(sl, p, soldier.getNationId(), dmg, soldier);
            }
        }
    }
}
