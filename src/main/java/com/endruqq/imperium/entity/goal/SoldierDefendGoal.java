package com.endruqq.imperium.entity.goal;

import com.endruqq.imperium.entity.Allegiance;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.game.Game;
import com.endruqq.imperium.game.GameData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Strike back at whoever hurt us and call nearby comrades for help. */
public class SoldierDefendGoal extends Goal {
    private final SoldierEntity soldier;
    private int lastStamp;

    public SoldierDefendGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        int stamp = soldier.getLastHurtByMobTimestamp();
        LivingEntity attacker = soldier.getLastHurtByMob();
        if (stamp == lastStamp || attacker == null || soldier.isRetreating() || soldier.isSurrendered()) return false;
        GameData d = Game.data(soldier.level());
        return d != null && Allegiance.isHostileTo(d, soldier, attacker, true);
    }

    @Override
    public void start() {
        lastStamp = soldier.getLastHurtByMobTimestamp();
        LivingEntity attacker = soldier.getLastHurtByMob();
        soldier.setTarget(attacker);
        for (SoldierEntity other : soldier.level().getEntitiesOfClass(SoldierEntity.class, soldier.getBoundingBox().inflate(12.0, 4.0, 12.0),
                s -> s != soldier && s.getNationId() == soldier.getNationId() && s.getTarget() == null && s.canFight() && !s.isSurrendered())) {
            other.setTarget(attacker);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
}
