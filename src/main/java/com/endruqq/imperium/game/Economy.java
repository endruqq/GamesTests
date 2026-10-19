package com.endruqq.imperium.game;

import com.endruqq.imperium.ImperiumConfig;
import com.endruqq.imperium.entity.CitizenEntity;
import com.endruqq.imperium.entity.Profession;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.UnitRegistry;
import com.endruqq.imperium.game.building.Building;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.game.tech.Tech;
import com.endruqq.imperium.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/** The economy cycle: production, consumption, research, happiness and population. */
public final class Economy {
    private Economy() {}

    public static int housing(GameData d, Nation n) {
        double h = 10;
        for (Building b : d.buildings.values()) if (b.nation == n.id && b.isActive()) h += b.housing;
        if (n.has(Tech.ARCHITECTURE)) h *= 1.2;
        return (int) h;
    }

    public static int tradeAgreements(GameData d, Nation n) {
        int c = 0;
        for (Relation r : d.relations.values()) {
            if (!r.trade || (r.a != n.id && r.b != n.id)) continue;
            Nation other = d.nation(r.a == n.id ? r.b : r.a);
            if (other != null && !other.eliminated) c++;
        }
        return c;
    }

    public static void cycle(ServerLevel level, GameData d) {
        for (Nation n : d.nations.values()) {
            if (n.eliminated) continue;
            cycle(level, d, n);
        }
        d.markChanged();
    }

    private static void cycle(ServerLevel level, GameData d, Nation n) {
        int id = n.id;
        int farms = d.countActive(id, Buildings.FARM.id);
        int lumber = d.countActive(id, Buildings.LUMBER_MILL.id);
        int mines = d.countActive(id, Buildings.MINE.id);
        int smelteries = d.countActive(id, Buildings.SMELTERY.id);
        int markets = d.countActive(id, Buildings.MARKET.id);
        int granaries = d.countActive(id, Buildings.GRANARY.id);
        int banks = d.countActive(id, Buildings.BANK.id);
        int taverns = d.countActive(id, Buildings.TAVERN.id);
        int academies = d.countActive(id, Buildings.ACADEMY.id);
        int temples = d.countActive(id, Buildings.TEMPLE.id);
        int hospitals = d.countActive(id, Buildings.HOSPITAL.id);
        int watchtowers = d.countActive(id, Buildings.WATCHTOWER.id);
        int trades = tradeAgreements(d, n);

        double prod = 0.6 + n.happiness / 125.0;
        if (n.famine) prod *= 0.8;
        double industry = (n.has(Tech.INDUSTRY) ? 1.25 : 1.0) * (n.has(Tech.GUILDS) ? 1.2 : 1.0);

        Resources delta = new Resources();
        delta.add(ResourceType.WOOD, (2 + 10 * lumber) * prod * industry);
        delta.add(ResourceType.STONE, (2 + 8 * mines) * prod * industry);
        delta.add(ResourceType.IRON, (0.5 + 2.5 * mines) * prod * industry);
        delta.add(ResourceType.FOOD, (4 + farms * (8 + (n.has(Tech.AGRICULTURE) ? 3 : 0))) * prod);
        double gold = n.population * 0.25 * n.tax.goldMult + markets * 6 + trades * (3 + markets * 2);
        gold *= (1 + 0.15 * banks) * (n.has(Tech.ADMINISTRATION) ? 1.1 : 1.0) * (n.has(Tech.GUILDS) ? 1.2 : 1.0) * prod;
        delta.add(ResourceType.GOLD, gold);
        for (int i = 0; i < smelteries; i++) {
            if (n.stock.get(ResourceType.IRON) + delta.get(ResourceType.IRON) >= 2) {
                delta.add(ResourceType.IRON, -2);
                delta.add(ResourceType.STEEL, 1);
            }
        }
        double foodUse = n.population * 0.4 + n.soldierCount * 0.6 + taverns * 2;
        if (granaries > 0) foodUse *= 0.9;
        delta.add(ResourceType.FOOD, -foodUse);
        delta.add(ResourceType.GOLD, -n.soldierCount * 0.3);

        n.stock.add(delta, 1);
        n.famine = n.stock.get(ResourceType.FOOD) < 0;
        n.unpaid = n.stock.get(ResourceType.GOLD) < 0;
        n.stock.clampMin(0);
        double foodCap = 400 + 600 * granaries;
        double cap = 1500 + 1000 * banks;
        for (ResourceType t : ResourceType.values()) {
            double c = t == ResourceType.FOOD ? foodCap : cap;
            if (n.stock.get(t) > c) n.stock.set(t, c);
        }
        n.lastDelta = delta;

        double research = (1 + n.population / 15.0 + academies * 4 + (n.has(Tech.WRITING) ? 1 : 0)) * (n.has(Tech.ENLIGHTENMENT) ? 1.5 : 1.0);
        n.research += research;
        n.researchDelta = research;

        int housing = housing(d, n);
        n.housing = housing;
        boolean atWar = d.atWarWithAnyone(id);
        n.warCycles = atWar ? n.warCycles + 1 : Math.max(0, n.warCycles - 2);

        double target = 55 + taverns * 6 + temples * 10 + markets * 4 + Math.min(2, watchtowers) * 2 - smelteries * 4 + n.tax.happiness;
        if (n.famine) target -= 35;
        if (n.unpaid) target -= 10;
        if (n.population > housing) target -= 15;
        target -= Math.min(20, n.warCycles * 0.5);
        if (n.abundant()) target += 8;
        if (n.has(Tech.ADMINISTRATION)) target += 5;
        if (n.has(Tech.ENLIGHTENMENT)) target += 5;
        if (d.gameTick - n.lastProvinceLostTick < 20 * 60 * 5) target -= 10;
        target = Math.max(0, Math.min(100, target));
        n.happinessTarget = target;
        double diff = target - n.happiness;
        n.happiness = Math.max(0, Math.min(100, n.happiness + Math.max(-6, Math.min(4, diff))));

        // population
        if (n.famine) {
            int dead = Math.max(1, n.population / 15);
            n.population = Math.max(1, n.population - dead);
            n.addLog(Component.translatable("imperium.log.starvation", dead));
            Notify.nation(level.getServer(), d, id, Component.translatable("imperium.msg.famine").withStyle(ChatFormatting.RED), false);
        } else if (n.happiness >= 35 && n.population < housing) {
            int growth = 1 + (hospitals > 0 ? 1 : 0) + (n.happiness > 70 ? 1 : 0);
            n.population = Math.min(housing, n.population + growth);
        } else if (n.population > housing + 4) {
            n.population--;
            n.addLog(Component.translatable("imperium.log.emigration"));
        }

        // hardship: unpaid or starving soldiers may desert
        if (n.famine || n.unpaid) n.hardshipCycles++;
        else n.hardshipCycles = 0;
        if (n.hardshipCycles >= 3 && n.morale() < 25) {
            List<SoldierEntity> soldiers = UnitRegistry.soldiersOf(id);
            if (!soldiers.isEmpty()) {
                SoldierEntity s = soldiers.get(level.random.nextInt(soldiers.size()));
                n.addLog(Component.translatable("imperium.log.deserted", s.getDisplayName()));
                s.dismiss();
            }
        }
        maintainCitizens(level, d, n);
    }

    /** Keeps a number of visible citizen NPCs proportional to the population. */
    private static void maintainCitizens(ServerLevel level, GameData d, Nation n) {
        int target = Math.min(n.population, ImperiumConfig.MAX_CITIZENS_PER_NATION.get());
        if (n.citizenCount < target) {
            List<BlockPos> homes = new ArrayList<>();
            for (Building b : d.buildings.values()) {
                if (b.nation == n.id && b.isActive() && b.type() != null && b.type().isHousing()) homes.add(b.pos);
            }
            Province cap = d.provinces.get(n.capitalProvince);
            if (cap != null && cap.core != null) homes.add(cap.core);
            if (homes.isEmpty()) return;
            for (int i = 0; i < 2 && n.citizenCount < target; i++) {
                BlockPos home = homes.get(level.random.nextInt(homes.size()));
                if (!level.isLoaded(home)) continue;
                spawnCitizen(level, d, n, home);
            }
        } else if (n.citizenCount > n.population + 2) {
            List<CitizenEntity> list = UnitRegistry.citizensOf(n.id);
            if (!list.isEmpty()) list.get(0).leave();
        }
    }

    public static CitizenEntity spawnCitizen(ServerLevel level, GameData d, Nation n, BlockPos home) {
        CitizenEntity c = ModEntities.CITIZEN.get().create(level);
        if (c == null) return null;
        BlockPos at = findStandPos(level, home, 4);
        c.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360F, 0);
        Profession prof = pickProfession(level, d, n);
        c.setup(n, home, prof);
        c.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(c);
        n.citizenCount++;
        return c;
    }

    private static Profession pickProfession(ServerLevel level, GameData d, Nation n) {
        List<Profession> options = new ArrayList<>();
        options.add(Profession.PEASANT);
        if (d.hasActive(n.id, Buildings.FARM.id)) options.add(Profession.FARMER);
        if (d.hasActive(n.id, Buildings.LUMBER_MILL.id)) options.add(Profession.WOODCUTTER);
        if (d.hasActive(n.id, Buildings.MINE.id)) options.add(Profession.MINER);
        if (d.hasActive(n.id, Buildings.SMELTERY.id) || d.hasActive(n.id, Buildings.ARMORY.id)) options.add(Profession.SMITH);
        if (d.hasActive(n.id, Buildings.MARKET.id)) options.add(Profession.MERCHANT);
        if (d.hasActive(n.id, Buildings.ACADEMY.id)) options.add(Profession.SCHOLAR);
        if (d.hasActive(n.id, Buildings.TEMPLE.id)) options.add(Profession.PRIEST);
        return options.get(level.random.nextInt(options.size()));
    }

    /** A free spot to stand on near a position. */
    public static BlockPos findStandPos(ServerLevel level, BlockPos near, int radius) {
        for (int i = 0; i < 16; i++) {
            int x = near.getX() + level.random.nextInt(radius * 2 + 1) - radius;
            int z = near.getZ() + level.random.nextInt(radius * 2 + 1) - radius;
            for (int y = near.getY() + 3; y >= near.getY() - 4; y--) {
                BlockPos p = new BlockPos(x, y, z);
                if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                        && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)) {
                    return p;
                }
            }
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near.getX() + 2, near.getZ() + 2);
        return new BlockPos(near.getX() + 2, y, near.getZ() + 2);
    }
}
