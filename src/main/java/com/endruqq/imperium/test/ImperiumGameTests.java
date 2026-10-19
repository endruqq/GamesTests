package com.endruqq.imperium.test;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.UnitType;
import com.endruqq.imperium.game.BotAI;
import com.endruqq.imperium.game.BuildingManager;
import com.endruqq.imperium.game.DiplomacyManager;
import com.endruqq.imperium.game.Economy;
import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.GameManager;
import com.endruqq.imperium.game.GamePhase;
import com.endruqq.imperium.game.MilitaryManager;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.PrefabBuilder;
import com.endruqq.imperium.game.Province;
import com.endruqq.imperium.game.TerritoryGen;
import com.endruqq.imperium.game.WarManager;
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
