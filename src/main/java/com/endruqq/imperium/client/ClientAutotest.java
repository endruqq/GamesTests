package com.endruqq.imperium.client;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.client.screen.DialogScreen;
import com.endruqq.imperium.client.screen.NationScreen;
import com.endruqq.imperium.client.screen.RtsScreen;
import com.endruqq.imperium.client.screen.TechTreeScreen;
import com.endruqq.imperium.entity.CitizenEntity;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.game.GamePhase;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.Province;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.network.ActionPacket;
import com.endruqq.imperium.network.ImperiumNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Development-only scripted client run used by CI ({@code -Dimperium.autotest=true}): creates a world, starts a game,
 * opens every screen and takes screenshots, then quits. Does nothing in normal play.
 */
@Mod.EventBusSubscriber(modid = Imperium.MODID, value = Dist.CLIENT)
public final class ClientAutotest {
    private static final boolean ENABLED = Boolean.getBoolean("imperium.autotest");
    private static int tick;
    private static int step;
    private static int wait;
    private static boolean worldRequested;
    private static final List<String> LOG = new ArrayList<>();

    private ClientAutotest() {}

    private static void log(String s) {
        Imperium.LOGGER.info("[AUTOTEST] {}", s);
        LOG.add(s);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "imperium_" + name + ".png", mc.getMainRenderTarget(), c -> log("screenshot " + name));
    }

    private static void cmd(Minecraft mc, String c) {
        if (mc.player != null) mc.player.connection.sendCommand(c);
        log("/" + c);
    }

    private static void finish(Minecraft mc, boolean ok, String msg) {
        log((ok ? "RESULT OK " : "RESULT FAIL ") + msg);
        try {
            Files.write(new File(mc.gameDirectory, "imperium_autotest.txt").toPath(), LOG);
        } catch (Exception e) {
            Imperium.LOGGER.error("autotest write failed", e);
        }
        mc.stop();
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        tick++;
        try {
            run(mc);
        } catch (Throwable t) {
            Imperium.LOGGER.error("[AUTOTEST] failure", t);
            finish(mc, false, t.toString());
        }
        if (tick > 20 * 60 * 16) finish(mc, false, "timeout at step " + step);
    }

    private static void run(Minecraft mc) {
        if (wait > 0) {
            wait--;
            return;
        }
        if (mc.level == null || mc.player == null) {
            if (!worldRequested && tick > 100 && mc.getOverlay() == null) {
                worldRequested = true;
                log("creating world");
                String name = "imperium_autotest_" + System.currentTimeMillis();
                LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(),
                        WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(20251019L, false, false),
                        access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions());
            }
            return;
        }
        switch (step) {
            case 0 -> {
                log("world loaded");
                wait = 100;
                step++;
            }
            case 1 -> {
                cmd(mc, "gamerule doDaylightCycle false");
                cmd(mc, "time set 1000");
                cmd(mc, "gamerule doWeatherCycle false");
                cmd(mc, "imperium arena 250");
                cmd(mc, "imperium autodivide 3 3");
                wait = 60;
                step++;
            }
            case 2 -> {
                cmd(mc, "imperium start");
                wait = 200;
                step++;
            }
            case 3 -> {
                if (ClientState.data.phase != GamePhase.RUNNING) {
                    finish(mc, false, "game did not start");
                    return;
                }
                Nation n = ClientState.myNation();
                log("my nation: " + (n == null ? "none" : n.name));
                cmd(mc, "imperium give wood 2000");
                cmd(mc, "imperium give stone 2000");
                cmd(mc, "imperium give gold 500");
                cmd(mc, "imperium give iron 300");
                cmd(mc, "imperium research all");
                wait = 60;
                step++;
            }
            case 4 -> {
                mc.player.setXRot(10);
                wait = 40;
                step++;
            }
            case 5 -> {
                shot(mc, "01_first_person");
                mc.setScreen(new RtsScreen());
                wait = 80;
                step++;
            }
            case 6 -> {
                if (!(mc.screen instanceof RtsScreen)) {
                    finish(mc, false, "rts screen did not open");
                    return;
                }
                for (Entity e : mc.level.entitiesForRendering()) {
                    if (e instanceof SoldierEntity s && s.getNationId() == ClientState.myNationId()) RtsScreen.SELECTION.add(s.getId());
                }
                RtsScreen.Placement p = new RtsScreen.Placement();
                p.type = Buildings.FARM;
                p.prefab = true;
                RtsScreen.placement = p;
                wait = 40;
                step++;
            }
            case 7 -> {
                shot(mc, "02_rts_view");
                RtsScreen.placement = null;
                placePrefabs(mc);
                wait = 80;
                step++;
            }
            case 8 -> {
                CompoundTag jobs = new CompoundTag();
                jobs.putString("op", "auto");
                ImperiumNet.sendToServer(new ActionPacket("jobs", jobs));
                if (!PLACED.isEmpty()) {
                    BlockPos first = PLACED.get(0);
                    RtsCamera.lookAt(first.getX(), first.getZ() + 6);
                    RtsCamera.height = 30;
                }
                wait = 60;
                step = 80;
            }
            case 80 -> {
                shot(mc, "03_rts_buildings");
                mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_M, 0, 0);
                wait = 60;
                step = 81;
            }
            case 81 -> {
                shot(mc, "04_strategic_map");
                cmd(mc, "imperium grace 0");
                Nation me = ClientState.myNation();
                for (Nation o : ClientState.data.nations.values()) {
                    if (me != null && o.id != me.id) {
                        CompoundTag t = new CompoundTag();
                        t.putInt("target", o.id);
                        t.putInt("action", com.endruqq.imperium.game.DiplomacyManager.Action.DECLARE_WAR.ordinal());
                        ImperiumNet.sendToServer(new ActionPacket("diplomacy", t));
                        break;
                    }
                }
                wait = 60;
                step = 82;
            }
            case 82 -> {
                shot(mc, "05_war_map");
                mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_M, 0, 0);
                mc.setScreen(new TechTreeScreen(mc.screen));
                wait = 30;
                step = 9;
            }
            case 9 -> {
                shot(mc, "06_tech_tree");
                mc.setScreen(new NationScreen(null, 0));
                wait = 30;
                step++;
            }
            case 10 -> {
                shot(mc, "07_nation");
                mc.setScreen(new NationScreen(null, NationScreen.PEOPLE));
                wait = 40;
                step = 100;
            }
            case 100 -> {
                shot(mc, "07b_people");
                mc.setScreen(new NationScreen(null, NationScreen.DIPLOMACY));
                wait = 30;
                step = 11;
            }
            case 11 -> {
                shot(mc, "08_diplomacy");
                mc.setScreen(null);
                CitizenEntity c = nearest(mc, CitizenEntity.class);
                if (c != null) {
                    cmd(mc, String.format("tp @s %.1f %.1f %.1f", c.getX() + 1.5, c.getY(), c.getZ()));
                    wait = 20;
                    step++;
                } else {
                    log("no citizen found");
                    step = 14;
                }
            }
            case 12 -> {
                CitizenEntity c = nearest(mc, CitizenEntity.class);
                if (c != null) {
                    mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, c.getEyePosition());
                    mc.gameMode.interact(mc.player, c, InteractionHand.MAIN_HAND);
                }
                wait = 30;
                step++;
            }
            case 13 -> {
                shot(mc, mc.screen instanceof DialogScreen ? "09_dialog" : "09_no_dialog");
                if (mc.screen instanceof DialogScreen) {
                    CompoundTag t = new CompoundTag();
                    Entity c = nearest(mc, CitizenEntity.class);
                    t.putInt("entity", c == null ? -1 : c.getId());
                    t.putString("option", "joke");
                    ImperiumNet.sendToServer(new ActionPacket("dialog", t));
                }
                wait = 40;
                step++;
            }
            case 14 -> {
                shot(mc, "10_dialog_joke");
                mc.setScreen(null);
                // a construction site next to the capital, then stand inside it
                Nation n = ClientState.myNation();
                Province cap = n == null ? null : ClientState.data.provinces.get(n.capitalProvince);
                if (cap == null || cap.core == null) {
                    finish(mc, false, "no capital");
                    return;
                }
                BlockPos best = null;
                for (int r = 16; r < 120 && best == null; r += 4) {
                    for (int a = 0; a < 24 && best == null; a++) {
                        int x = cap.core.getX() + (int) (Math.cos(a * Math.PI / 12) * r);
                        int z = cap.core.getZ() + (int) (Math.sin(a * Math.PI / 12) * r);
                        if (!mc.level.hasChunkAt(new BlockPos(x, 64, z))) continue;
                        int y = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                        if (!mc.level.getBlockState(new BlockPos(x, y - 1, z)).getFluidState().isEmpty()) continue;
                        BlockPos pos = new BlockPos(x, y, z);
                        if (com.endruqq.imperium.game.BuildingManager.validate(ClientState.data, n.id, Buildings.SMELTERY, pos) == null) best = pos;
                    }
                }
                if (best == null) {
                    log("no valid construction site found");
                    step = 17;
                    return;
                }
                CompoundTag t = new CompoundTag();
                t.putString("type", Buildings.SMELTERY.id);
                t.putLong("pos", best.asLong());
                t.putBoolean("prefab", false);
                t.putInt("facing", 0);
                ImperiumNet.sendToServer(new ActionPacket("place_building", t));
                cmd(mc, String.format("tp @s %d %d %d", best.getX() + 2, best.getY(), best.getZ() + 2));
                log("site at " + best);
                wait = 60;
                step++;
            }
            case 15 -> {
                shot(mc, "11_construction_hud");
                SoldierEntity s = nearest(mc, SoldierEntity.class);
                if (s != null) {
                    cmd(mc, String.format("tp @s %.1f %.1f %.1f", s.getX() + 1.5, s.getY(), s.getZ()));
                    wait = 20;
                    step++;
                } else {
                    step = 17;
                }
            }
            case 16 -> {
                SoldierEntity s = nearest(mc, SoldierEntity.class);
                if (s != null) {
                    CompoundTag t = new CompoundTag();
                    t.putInt("entity", s.getId());
                    t.putString("option", "equipment");
                    mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, s.getEyePosition());
                    ImperiumNet.sendToServer(new ActionPacket("dialog", t));
                }
                wait = 40;
                step++;
            }
            case 17 -> {
                shot(mc, "12_soldier_equipment");
                mc.setScreen(null);
                cmd(mc, "imperium info");
                wait = 40;
                step++;
            }
            case 18 -> {
                // a catalogue of all ready-made buildings, away from the arena
                cmd(mc, "worldborder set 8000");
                cmd(mc, "tp @s ~420 ~ ~");
                wait = 120;
                step = 200;
            }
            case 200 -> {
                // the server builds at the ground level under the player, whatever height the player is at
                showcaseOrigin = mc.player.blockPosition();
                cmd(mc, "imperium showcase");
                wait = 300;
                step = 201;
            }
            case 201 -> {
                // clean shots: spectator camera, no HUD
                mc.setScreen(null);
                RtsCamera.exit();
                // the lawn behind the first building lies at the level the catalogue was built on
                int lx = showcaseOrigin.getX() + 4, lz = showcaseOrigin.getZ() - com.endruqq.imperium.game.Showcase.STEP_Z / 2 + 2;
                showcaseOrigin = new BlockPos(showcaseOrigin.getX(), mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, lx, lz), showcaseOrigin.getZ());
                log("showcase ground at y=" + showcaseOrigin.getY());
                cmd(mc, "gamemode spectator");
                mc.options.hideGui = true;
                showcaseIndex = 0;
                wait = 40;
                step = 202;
            }
            case 202 -> {
                // two buildings per picture, seen from the south-east, facing their entrances
                int count = 0;
                for (com.endruqq.imperium.game.building.BuildingType t : Buildings.all()) if (t.prefabCost != null) count++;
                if (showcaseIndex >= count) {
                    mc.options.hideGui = false;
                    step = 210;
                    return;
                }
                int i = showcaseIndex;
                int col = i % com.endruqq.imperium.game.Showcase.PER_ROW, row = i / com.endruqq.imperium.game.Showcase.PER_ROW;
                double x = showcaseOrigin.getX() + 12 + col * com.endruqq.imperium.game.Showcase.STEP_X
                        + (i + 1 < count ? com.endruqq.imperium.game.Showcase.STEP_X / 2.0 : 0);
                double z = showcaseOrigin.getZ() + row * com.endruqq.imperium.game.Showcase.STEP_Z + 2;
                double y = showcaseOrigin.getY() + 7;
                double yaw = Math.toRadians(160), pitch = Math.toRadians(20), dist = 28;
                double cx = x + Math.sin(yaw) * Math.cos(pitch) * dist;
                double cz = z - Math.cos(yaw) * Math.cos(pitch) * dist;
                double cy = y + Math.sin(pitch) * dist;
                cmd(mc, String.format(java.util.Locale.ROOT, "tp @s %.1f %.1f %.1f 160 20", cx, cy, cz));
                wait = 60;
                step = 203;
            }
            case 203 -> {
                shot(mc, String.format("p%02d_prefabs", showcaseIndex / 2 + 1));
                showcaseIndex += 2;
                wait = 10;
                step = 202;
            }
            case 210 -> finish(mc, true, "all steps done");
            default -> {
            }
        }
    }

    private static final List<BlockPos> PLACED = new ArrayList<>();
    private static BlockPos showcaseOrigin = BlockPos.ZERO;
    private static int showcaseIndex;

    /** Buys a few ready-made buildings around the capital. */
    private static void placePrefabs(Minecraft mc) {
        Nation n = ClientState.myNation();
        Province cap = n == null ? null : ClientState.data.provinces.get(n.capitalProvince);
        if (cap == null || cap.core == null) return;
        com.endruqq.imperium.game.building.BuildingType[] types = {Buildings.HUT, Buildings.FARM, Buildings.BARRACKS, Buildings.LUMBER_MILL,
                Buildings.MARKET, Buildings.TAVERN, Buildings.STONE_HOUSE, Buildings.MINE};
        for (com.endruqq.imperium.game.building.BuildingType type : types) {
            BlockPos found = null;
            for (int r = 18; r < 70 && found == null; r += 3) {
                for (int a = 0; a < 24 && found == null; a++) {
                    int x = cap.core.getX() + (int) (Math.cos(a * Math.PI / 12) * r);
                    int z = cap.core.getZ() + (int) (Math.sin(a * Math.PI / 12) * r);
                    if (!mc.level.hasChunkAt(new BlockPos(x, 64, z))) continue;
                    int y = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    BlockPos pos = new BlockPos(x, y, z);
                    if (com.endruqq.imperium.game.BuildingManager.validate(ClientState.data, n.id, type, pos) != null) continue;
                    boolean clash = false;
                    for (BlockPos q : PLACED) if (Math.abs(q.getX() - x) < 14 && Math.abs(q.getZ() - z) < 14) clash = true;
                    if (!clash) found = pos;
                }
            }
            if (found == null) {
                log("no spot for " + type.id);
                continue;
            }
            PLACED.add(found);
            CompoundTag t = new CompoundTag();
            t.putString("type", type.id);
            t.putLong("pos", found.asLong());
            t.putBoolean("prefab", true);
            t.putInt("facing", 2);
            ImperiumNet.sendToServer(new ActionPacket("place_building", t));
            log("prefab " + type.id + " at " + found);
        }
    }

    private static <T extends Entity> T nearest(Minecraft mc, Class<T> cls) {
        T best = null;
        double bd = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!cls.isInstance(e)) continue;
            int nation = e instanceof CitizenEntity c ? c.getNationId() : e instanceof SoldierEntity s ? s.getNationId() : -1;
            if (nation != ClientState.myNationId()) continue;
            double d = e.distanceToSqr(mc.player);
            if (d < bd) {
                bd = d;
                best = cls.cast(e);
            }
        }
        return best;
    }
}
