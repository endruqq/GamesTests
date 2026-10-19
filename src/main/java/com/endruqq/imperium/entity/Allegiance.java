package com.endruqq.imperium.entity;

import com.endruqq.imperium.game.GameData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

/** Which nation an entity fights for and whom it should attack. */
public final class Allegiance {
    private Allegiance() {}

    public static int nationOf(Entity e, GameData data) {
        if (e == null || data == null) return -1;
        if (e instanceof SoldierEntity s) return s.getNationId();
        if (e instanceof CitizenEntity c) return c.getNationId();
        if (e instanceof Player p) return data.nationIdOf(p.getUUID());
        if (e instanceof Projectile pr) {
            Entity owner = pr.getOwner();
            return owner == pr ? -1 : nationOf(owner, data);
        }
        if (e instanceof TamableAnimal t) {
            LivingEntity owner = t.getOwner();
            return owner == null ? -1 : nationOf(owner, data);
        }
        return -1;
    }

    /** Should a soldier of {@code myNation} attack {@code e} on sight? */
    public static boolean isHostile(GameData data, int myNation, LivingEntity e, boolean allowCreepers) {
        if (e == null || !e.isAlive()) return false;
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if (e instanceof Enemy && !(e instanceof SoldierEntity)) {
            return allowCreepers || !(e instanceof Creeper);
        }
        if (!(e instanceof SoldierEntity) && !(e instanceof Player)) return false;
        if (e instanceof SoldierEntity s && s.isSurrendered()) return false;
        // strangers on our land while the borders are closed
        if (myNation >= 0 && com.endruqq.imperium.game.BorderManager.isTrespasser(data, e, myNation)) return true;
        int other = nationOf(e, data);
        return other >= 0 && myNation >= 0 && data.atWar(myNation, other);
    }

    /** Like {@link #isHostile}, plus self-defence of a soldier that is itself trespassing. */
    public static boolean isHostileTo(GameData data, SoldierEntity me, LivingEntity e, boolean allowCreepers) {
        if (isHostile(data, me.getNationId(), e, allowCreepers)) return true;
        if (e == null || !e.isAlive() || !(e instanceof SoldierEntity || e instanceof Player)) return false;
        if (e instanceof SoldierEntity s && s.isSurrendered()) return false;
        int other = nationOf(e, data);
        return other >= 0 && com.endruqq.imperium.game.BorderManager.isTrespasser(data, me, other);
    }

    /** May {@code attacker} damage {@code victim} at all? */
    public static boolean mayHarm(GameData data, Entity attacker, Entity victim) {
        int a = nationOf(attacker, data);
        int v = nationOf(victim, data);
        if (a < 0 || v < 0) return true;
        if (data.friendly(a, v)) return false;
        return data.atWar(a, v);
    }
}
