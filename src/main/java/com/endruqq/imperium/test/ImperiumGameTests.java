package com.endruqq.imperium.test;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.entity.CitizenEntity;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.UnitRegistry;
import com.endruqq.imperium.entity.UnitType;
import com.endruqq.imperium.game.BorderManager;
import com.endruqq.imperium.game.BotAI;
import com.endruqq.imperium.game.CasusBelli;
import com.endruqq.imperium.game.BuildingManager;
import com.endruqq.imperium.game.DiplomacyManager;
import com.endruqq.imperium.game.Economy;
import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.GameManager;
import com.endruqq.imperium.game.GamePhase;
import com.endruqq.imperium.game.MilitaryManager;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.PeopleManager;
import com.endruqq.imperium.game.Person;
import com.endruqq.imperium.game.PrefabBuilder;
import com.endruqq.imperium.game.PrisonerManager;
import com.endruqq.imperium.game.RelationType;
import com.endruqq.imperium.game.Province;
import com.endruqq.imperium.game.TerritoryGen;
import com.endruqq.imperium.game.WarManager;
import com.endruqq.imperium.game.WarReasons;
import com.endruqq.imperium.game.building.Building;
import com.endruqq.imperium.game.building.BuildingType;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Server-side smoke tests, run in CI with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(Imperium.MODID)
@PrefixGameTestTemplate(false)
public class ImperiumGameTests {

    /** Every ready-made building must satisfy its own block requirements. */
    @GameTest(template = "empty", batch = "imperium_prefabs", timeoutTicks = 100)
    public static void prefabsSatisfyRequirements(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(24, 2, 24));
        List<String> failures = new ArrayList<>();
        for (BuildingType t : Buildings.all()) {
            if (t.prefabCost == null) continue;
            for (Direction dir : new Direction[]{Direction.SOUTH, Direction.EAST}) {
                PrefabBuilder.build(level, t, center, dir, null);
                Building b = new Building();
                b.typeId = t.id;
                b.pos = center;
                b.counts = BuildingManager.count(level, b);
                if (b.satisfied() != t.requirements.size()) {
                    failures.add(t.id + "/" + dir + " " + Arrays.toString(b.counts));
                }
            }
        }
        if (failures.isEmpty()) helper.succeed();
        else helper.fail("Prefabs missing blocks: " + failures);
    }

    /** Jobs and staffing, weddings, death in the mine and grief. */
    @GameTest(template = "empty", batch = "imperium_people", timeoutTicks = 200)
    public static void peopleLiveTheirLives(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        GameData d = GameData.get(server);
        BlockPos origin = helper.absolutePos(new BlockPos(24, 2, 24));
        GameManager.wipe(server, d);
        GameManager.setArena(d, origin.getX() + 1300, origin.getZ() + 400, 250);
        TerritoryGen.autoDivide(d, 2, 1, 11L);
        Component err = GameManager.start(server, d);
        helper.assertTrue(err == null, "start failed: " + (err == null ? "" : err.getString()));
        Nation a = d.nations.values().iterator().next();
        List<Person> people = d.peopleOf(a.id);
        helper.assertTrue(people.size() == 4, "expected 4 people, got " + people.size());

        // an (empty) farm record: without workers it gives only part of its output
        Province cap = d.provinces.get(a.capitalProvince);
        Building farm = new Building();
        farm.id = d.nextBuildingId++;
        farm.typeId = Buildings.FARM.id;
        farm.nation = a.id;
        farm.pos = cap.core.offset(30, 0, 30);
        farm.state = Building.State.ACTIVE;
        d.buildings.put(farm.id, farm);
        double before = PeopleManager.staffing(d, farm);
        helper.assertTrue(Math.abs(before - Buildings.FARM.unstaffedOutput) < 1e-6, "unstaffed output " + before);
        helper.assertTrue(PeopleManager.hire(d, a, Buildings.FARM) == null, "hiring failed");
        helper.assertTrue(farm.workers == 1, "workers " + farm.workers);
        helper.assertTrue(PeopleManager.staffing(d, farm) > before, "staffing did not improve");

        // a wedding, then the husband dies in a lava pocket
        Person husband = people.get(0), wife = people.get(1);
        husband.female = false;
        wife.female = true;
        PeopleManager.marry(level, d, a, husband, wife);
        helper.assertTrue(husband.married() && wife.married(), "not married");
        CitizenEntity he = PeopleManager.entity(husband);
        helper.assertTrue(he != null, "husband entity not loaded");
        String name = husband.name;
        he.dieOf("lava", level.damageSources().lava());
        helper.assertTrue(!d.people.containsKey(husband.id), "dead husband still has a record");
        helper.assertTrue(wife.mourning(d.gameTick) && wife.mourningFor.equals(name) && wife.mourningCause.equals("lava"), "widow is not mourning");
        helper.assertTrue(!wife.married(), "widow still married");
        helper.assertTrue(a.gossip.stream().anyMatch(g -> g.type.equals("death.lava")), "nobody talks about it");
        helper.assertTrue(PeopleManager.assign(d, wife, farm) != null, "a mourning widow went to work");
        GameManager.wipe(server, d);
        helper.succeed();
    }

    /** A war of aggression, a fallen capital, prisoners, persuasion, closed borders. */
    @GameTest(template = "empty", batch = "imperium_prisoners", timeoutTicks = 200)
    public static void prisonersAndBorders(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        GameData d = GameData.get(server);
        BlockPos origin = helper.absolutePos(new BlockPos(24, 2, 24));
        GameManager.wipe(server, d);
        GameManager.setArena(d, origin.getX() - 1300, origin.getZ() + 400, 250);
        TerritoryGen.autoDivide(d, 2, 2, 5L);
        Component err = GameManager.start(server, d);
        helper.assertTrue(err == null, "start failed: " + (err == null ? "" : err.getString()));
        d.graceTicks = 0;
        List<Nation> nations = new ArrayList<>(d.nations.values());
        Nation a = nations.get(0), b = nations.get(1);

        // no reason for war: aggression makes the own people unhappy
        helper.assertTrue(WarReasons.reasons(d, a.id, b.id).isEmpty(), "unexpected reason for war");
        double happy = a.happiness;
        helper.assertTrue(DiplomacyManager.perform(server, d, a.id, b.id, DiplomacyManager.Action.DECLARE_WAR) == null, "war refused");
        helper.assertTrue(a.happiness < happy - 10, "aggression went unpunished");

        // the capital falls: the whole army lays down arms
        Province cap = d.provinces.get(b.capitalProvince);
        WarManager.damageCore(level, cap, a.id, cap.coreHp + 10, null);
        List<SoldierEntity> army = UnitRegistry.soldiersOf(b.id);
        helper.assertTrue(!army.isEmpty(), "no soldiers of the loser loaded");
        for (SoldierEntity s : army) {
            helper.assertTrue(s.isSurrendered() && s.getCaptor() == a.id, s.getSoldierName() + " did not surrender");
            helper.assertTrue(s.getMainHandItem().isEmpty(), s.getSoldierName() + " kept the weapon in hand");
        }
        SoldierEntity freed = army.get(0);
        PrisonerManager.release(level, d, freed, true);
        helper.assertTrue(!freed.isSurrendered() && !freed.getMainHandItem().isEmpty(), "released prisoner did not rearm");
        if (army.size() > 1) {
            SoldierEntity turned = army.get(1);
            PrisonerManager.joinArmy(level, d, turned, a);
            helper.assertTrue(turned.getNationId() == a.id && !turned.isSurrendered(), "prisoner did not join");
        }

        // closed borders in peace: a foreign soldier becomes an intruder and a reason for war
        d.relation(a.id, b.id).type = RelationType.PEACE;
        a.bordersClosed = true;
        Province home = d.provinces.get(a.capitalProvince);
        SoldierEntity spy = MilitaryManager.spawnSoldier(level, d, b, UnitType.MILITIA, home.core.offset(0, 0, 6));
        helper.assertTrue(spy != null, "intruder not spawned");
        BorderManager.tick(server, d);
        d.gameTick += BorderManager.SOLDIER_GRACE + 5;
        helper.assertTrue(BorderManager.isTrespasser(d, spy, a.id), "intruder not detected");
        helper.assertTrue(com.endruqq.imperium.entity.Allegiance.isHostile(d, a.id, spy, true), "intruder is not hostile");
        helper.assertTrue(WarReasons.reasons(d, a.id, b.id).contains(CasusBelli.Type.BORDER_VIOLATION), "no reason for war after the intrusion");
        GameManager.wipe(server, d);
        BorderManager.clear();
        helper.succeed();
    }

    /** Setup, start, war, conquest, elimination, victory, economy, bots and soldier AI. */
    @GameTest(template = "empty", batch = "imperium_game", timeoutTicks = 1600)
    public static void fullGameFlow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        GameData d = GameData.get(server);
        BlockPos origin = helper.absolutePos(new BlockPos(24, 2, 24));

        GameManager.wipe(server, d);
        GameManager.setArena(d, origin.getX() + 400, origin.getZ() + 400, 250);
        TerritoryGen.autoDivide(d, 2, 2, 42L);
        helper.assertTrue(d.nations.size() == 2, "expected 2 nations, got " + d.nations.size());
        helper.assertTrue(d.provinces.size() == 4, "expected 4 provinces, got " + d.provinces.size());

        Component err = GameManager.start(server, d);
        helper.assertTrue(err == null, "start failed: " + (err == null ? "" : err.getString()));
        helper.assertTrue(d.phase == GamePhase.RUNNING, "game is not running");
        for (Province p : d.provinces.values()) {
            helper.assertTrue(p.core != null && level.getBlockState(p.core).is(ModBlocks.PROVINCE_CORE.get()), "core missing in " + p.name);
        }
        List<Nation> nations = new ArrayList<>(d.nations.values());
        for (Nation n : nations) {
            helper.assertTrue(n.bot, n.name + " should be a bot");
            helper.assertTrue(n.soldierCount == 3, n.name + " soldiers: " + n.soldierCount);
            helper.assertTrue(n.citizenCount == 4, n.name + " citizens: " + n.citizenCount);
        }
        Nation a = nations.get(0), b = nations.get(1);

        // war is impossible during the peace period
        helper.assertTrue(DiplomacyManager.perform(server, d, a.id, b.id, DiplomacyManager.Action.DECLARE_WAR) != null, "war during grace");
        d.graceTicks = 0;
        helper.assertTrue(DiplomacyManager.perform(server, d, a.id, b.id, DiplomacyManager.Action.DECLARE_WAR) == null, "war refused");
        helper.assertTrue(d.atWar(a.id, b.id), "not at war");

        // conquer everything b owns
        for (Province p : new ArrayList<>(d.provincesOf(b.id))) {
            WarManager.damageCore(level, p, a.id, p.coreHp + 10, null);
            helper.assertTrue(p.owner == a.id, "province " + p.name + " not captured");
        }
        helper.assertTrue(b.eliminated, "loser not eliminated");
        helper.assertTrue(d.phase == GamePhase.ENDED, "no victory");

        // restart and run the economy / bots
        GameManager.reset(server, d);
        err = GameManager.start(server, d);
        helper.assertTrue(err == null, "restart failed: " + (err == null ? "" : err.getString()));
        for (int i = 0; i < 3; i++) {
            Economy.cycle(level, d);
            BotAI.cycle(level, d);
        }
        helper.assertTrue(!a.lastDelta.isEmpty(), "economy did not run");

        // two hostile soldiers next to each other must fight
        d.graceTicks = 0;
        DiplomacyManager.perform(server, d, a.id, b.id, DiplomacyManager.Action.DECLARE_WAR);
        for (int x = 18; x <= 30; x++) {
            for (int z = 18; z <= 30; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        }
        SoldierEntity sa = MilitaryManager.spawnSoldier(level, d, a, UnitType.SWORDSMAN, origin);
        SoldierEntity sb = MilitaryManager.spawnSoldier(level, d, b, UnitType.MILITIA, origin.offset(3, 0, 0));
        helper.assertTrue(sa != null && sb != null, "soldiers not spawned");
        helper.runAfterDelay(300, () -> {
            boolean fought = !sa.isAlive() || !sb.isAlive() || sa.getHealth() < sa.getMaxHealth() || sb.getHealth() < sb.getMaxHealth();
            helper.assertTrue(fought, "soldiers at war did not fight");
            GameManager.wipe(server, d);
            helper.succeed();
        });
    }
}
