package com.endruqq.imperium.game;

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
import net.minecraft.core.particles.ParticleTypes;
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
        if (n.has(Tech.URBANIZATION)) h *= 1.15;
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

    /** Provinces that were ours at the start and are held by somebody else now. */
    public static int lostLands(GameData d, Nation n) {
        int c = 0;
        for (Province p : d.provinces.values()) if (p.originalOwner == n.id && p.owner != n.id && p.cellCount > 0) c++;
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
        PeopleManager.cycle(level, d, n);
        double farms = PeopleManager.effective(d, id, Buildings.FARM.id);
        double lumber = PeopleManager.effective(d, id, Buildings.LUMBER_MILL.id);
        double mines = PeopleManager.effective(d, id, Buildings.MINE.id);
        double smelteries = PeopleManager.effective(d, id, Buildings.SMELTERY.id);
        double markets = PeopleManager.effective(d, id, Buildings.MARKET.id);
        double banks = PeopleManager.effective(d, id, Buildings.BANK.id);
        double taverns = PeopleManager.effective(d, id, Buildings.TAVERN.id);
        double academies = PeopleManager.effective(d, id, Buildings.ACADEMY.id);
        double temples = PeopleManager.effective(d, id, Buildings.TEMPLE.id);
        double factories = PeopleManager.effective(d, id, Buildings.FACTORY.id);
        int smelterySites = d.countActive(id, Buildings.SMELTERY.id);
        int factorySites = d.countActive(id, Buildings.FACTORY.id);
        int granaries = d.countActive(id, Buildings.GRANARY.id);
        int bankSites = d.countActive(id, Buildings.BANK.id);
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
        // natural wealth of the provinces
        for (Province p : d.provincesOf(id)) {
            if (p.resource != ProvinceResource.NONE && p.resource.resource != null) delta.add(p.resource.resource, p.resource.bonus * prod);
        }
        double gold = n.population * 0.25 * n.tax.goldMult + markets * 6 + trades * (3 + markets * 2);
        gold *= (1 + 0.15 * banks) * (n.has(Tech.ADMINISTRATION) ? 1.1 : 1.0) * (n.has(Tech.GUILDS) ? 1.2 : 1.0) * prod;
        delta.add(ResourceType.GOLD, gold);
        double steelMade = 0;
        for (int i = 0; i < Math.round(smelteries); i++) {
            if (n.stock.get(ResourceType.IRON) + delta.get(ResourceType.IRON) >= 2) {
                delta.add(ResourceType.IRON, -2);
                delta.add(ResourceType.STEEL, 1);
                steelMade++;
            }
        }
        // factories turn wood and iron into goods
        double goodsCap = factories * 4 * (n.has(Tech.MASS_PRODUCTION) ? 1.5 : 1.0);
        double canMake = Math.min(goodsCap, Math.min((n.stock.get(ResourceType.WOOD) + delta.get(ResourceType.WOOD)),
                (n.stock.get(ResourceType.IRON) + delta.get(ResourceType.IRON)) * 2));
        double goods = Math.max(0, canMake);
        if (goods > 0) {
            double inputs = n.has(Tech.MASS_PRODUCTION) ? goods / 1.5 : goods;
            delta.add(ResourceType.WOOD, -inputs);
            delta.add(ResourceType.IRON, -inputs * 0.5);
            delta.add(ResourceType.GOODS, goods);
        }
        // people buy goods: prosperity
        double goodsNeed = Math.max(1, n.population / 15.0);
        double goodsHave = n.stock.get(ResourceType.GOODS) + delta.get(ResourceType.GOODS);
        n.prosperity = factorySites > 0 && goodsHave >= goodsNeed;
        if (n.prosperity) {
            delta.add(ResourceType.GOODS, -goodsNeed);
            delta.add(ResourceType.GOLD, goodsNeed * 0.8);
            goodsHave -= goodsNeed;
        }
        // trade partners buy our goods
        double export = Math.min(trades, Math.max(0, goodsHave));
        if (export > 0) {
            delta.add(ResourceType.GOODS, -export);
            delta.add(ResourceType.GOLD, export * 5);
        }

        // food: married couples share the table and eat less
        int couples = PeopleManager.happyCouples(d, n);
        double foodUse = n.population * 0.4 + n.soldierCount * 0.6 + taverns * 2 - couples * 0.15;
        if (granaries > 0) foodUse *= 0.9;
        delta.add(ResourceType.FOOD, -Math.max(0, foodUse));
        delta.add(ResourceType.GOLD, -n.soldierCount * 0.3);

        n.stock.add(delta, 1);
        n.famine = n.stock.get(ResourceType.FOOD) < 0;
        n.unpaid = n.stock.get(ResourceType.GOLD) < 0;
        n.stock.clampMin(0);
        double foodCap = 400 + 600 * granaries;
        double cap = 1500 + 1000 * bankSites;
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
        n.lostLands = lostLands(d, n);

        double target = 55 + taverns * 6 + temples * 10 + markets * 4 + Math.min(2, watchtowers) * 2 - smelterySites * 4 - factorySites * 3 + n.tax.happiness;
        if (n.famine) target -= 35;
        if (n.unpaid) target -= 10;
        if (n.population > housing) target -= 15;
        target -= Math.min(20, n.warCycles * 0.5);
        if (n.abundant()) target += 8;
        if (n.prosperity) target += 6;
        if (n.has(Tech.ADMINISTRATION)) target += 5;
        if (n.has(Tech.ENLIGHTENMENT)) target += 5;
        target += Math.min(6, couples * 0.5);
        target -= Math.min(8, n.mourningCount * 1.5);
        target -= Math.min(9, n.lostLands * 3);
        if (d.gameTick - n.lastProvinceLostTick < 20 * 60 * 5) target -= 10;
        target = Math.max(0, Math.min(100, target));
        n.happinessTarget = target;
        double diff = target - n.happiness;
        n.happiness = Math.max(0, Math.min(100, n.happiness + Math.max(-6, Math.min(4, diff))));

        // population
        if (n.famine) {
            int dead = Math.max(1, n.population / 15);
            // hunger takes real people too
            List<CitizenEntity> loaded = UnitRegistry.citizensOf(id);
            if (!loaded.isEmpty() && level.random.nextInt(3) == 0) {
                loaded.get(level.random.nextInt(loaded.size())).dieOf("starvation", level.damageSources().starve());
                dead--;
            }
            n.population = Math.max(1, n.population - Math.max(0, dead));
            n.addLog(Component.translatable("imperium.log.starvation", Math.max(1, dead)));
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
        factoryLife(level, d, n);
        maintainCitizens(level, d, n);
        PeopleManager.stats(d, n);
    }

    /** Factory chimneys smoke while staffed; now and then somebody gets hurt at the machines. */
    private static void factoryLife(ServerLevel level, GameData d, Nation n) {
        for (Building b : d.buildings.values()) {
            if (b.nation != n.id || !b.isActive()) continue;
            boolean smoky = b.typeId.equals(Buildings.FACTORY.id) || b.typeId.equals(Buildings.SMELTERY.id);
            if (!smoky || !level.isLoaded(b.pos)) continue;
            if (b.workers > 0 || b.typeId.equals(Buildings.SMELTERY.id)) {
                for (BlockPos top : PrefabBuilder.smokePoints(b)) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, top.getX() + 0.5, top.getY() + 0.8, top.getZ() + 0.5, 3, 0.15, 0.3, 0.15, 0.01);
                }
            }
            if (!b.typeId.equals(Buildings.FACTORY.id) || b.workers == 0 || level.random.nextDouble() > 0.012) continue;
            List<Person> crew = d.workersAt(b.id);
            if (crew.isEmpty()) continue;
            Person victim = crew.get(level.random.nextInt(crew.size()));
            CitizenEntity c = PeopleManager.entity(victim);
            if (c != null && c.hasState(CitizenEntity.WORKING)) c.dieOf("accident", level.damageSources().generic());
        }
    }

    /** Keeps the number of citizen NPCs at the nation's citizen limit. */
    private static void maintainCitizens(ServerLevel level, GameData d, Nation n) {
        int target = PeopleManager.citizenCap(d, n);
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
            CitizenEntity pick = null;
            for (CitizenEntity c : list) if (!c.getProfession().employed()) pick = c;
            if (pick == null && !list.isEmpty()) pick = list.get(0);
            if (pick != null) pick.leave();
        }
    }

    public static CitizenEntity spawnCitizen(ServerLevel level, GameData d, Nation n, BlockPos home) {
        CitizenEntity c = ModEntities.CITIZEN.get().create(level);
        if (c == null) return null;
        BlockPos at = findStandPos(level, home, 4);
        c.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360F, 0);
        c.setup(n, home, Profession.PEASANT);
        c.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(c);
        n.citizenCount++;
        return c;
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
