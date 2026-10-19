package com.endruqq.imperium.game;

import com.endruqq.imperium.ImperiumConfig;
import com.endruqq.imperium.entity.CitizenEntity;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.UnitRegistry;
import com.endruqq.imperium.entity.UnitType;
import com.endruqq.imperium.network.ImperiumNet;
import com.endruqq.imperium.network.StatePacket;
import com.endruqq.imperium.network.TerritoryPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Game setup (arena, nations, provinces), start/stop and player membership. */
public final class GameManager {
    public static final int[] ARENA_SIZES = {250, 500, 1000, 1500, 2000, 2500, 3000};

    private GameManager() {}

    // ------------------------------------------------------------------ setup

    public static void setArena(GameData d, int x, int z, int size) {
        size = Math.max(100, Math.min(3000, size));
        d.setArena(x, z, size);
        d.provinces.values().forEach(p -> {
            p.core = null;
            p.coreBuilt = false;
        });
        d.markChanged();
    }

    public static Nation createNation(GameData d, String name, int color) {
        Nation n = new Nation();
        n.id = d.nextNationId++;
        n.name = name == null || name.isBlank() ? NameGen.nation(net.minecraft.util.RandomSource.create(), namesOf(d)) : name;
        n.colorIndex = color >= 0 ? color : firstFreeColor(d);
        d.nations.put(n.id, n);
        d.markChanged();
        return n;
    }

    private static Set<String> namesOf(GameData d) {
        Set<String> s = new HashSet<>();
        for (Nation n : d.nations.values()) s.add(n.name);
        return s;
    }

    public static int firstFreeColor(GameData d) {
        for (int c = 0; c < NationColor.values().length; c++) {
            final int cc = c;
            if (d.nations.values().stream().noneMatch(n -> n.colorIndex == cc)) return c;
        }
        return d.nations.size();
    }

    public static void deleteNation(GameData d, int id) {
        Nation n = d.nations.remove(id);
        if (n == null) return;
        for (Province p : d.provinces.values()) {
            if (p.owner == id) p.owner = -1;
            if (p.originalOwner == id) p.originalOwner = -1;
        }
        d.buildings.values().removeIf(b -> b.nation == id);
        d.relations.values().removeIf(r -> r.a == id || r.b == id);
        d.proposals.removeIf(p -> p.from == id || p.to == id);
        d.markTerritoryChanged();
        d.markChanged();
    }

    public static Province createProvince(GameData d, int owner) {
        Province p = new Province();
        p.id = d.nextProvinceId++;
        Set<String> taken = new HashSet<>();
        for (Province q : d.provinces.values()) taken.add(q.name);
        p.name = NameGen.province(net.minecraft.util.RandomSource.create(), taken);
        p.owner = owner;
        p.originalOwner = owner;
        d.provinces.put(p.id, p);
        Nation n = d.nation(owner);
        if (n != null && n.capitalProvince < 0) n.capitalProvince = p.id;
        d.markChanged();
        return p;
    }

    public static void deleteProvince(GameData d, int id) {
        if (d.provinces.remove(id) == null) return;
        for (int i = 0; i < d.cells.length; i++) if (d.cells[i] == id) d.cells[i] = -1;
        for (Nation n : d.nations.values()) if (n.capitalProvince == id) n.capitalProvince = -1;
        d.recomputeGeometry();
        d.markTerritoryChanged();
    }

    // ------------------------------------------------------------------ membership

    public static void join(MinecraftServer server, GameData d, ServerPlayer player, int nationId) {
        Nation target = d.nation(nationId);
        if (target == null) return;
        if (target.eliminated) {
            player.displayClientMessage(Component.translatable("imperium.err.eliminated").withStyle(ChatFormatting.RED), true);
            return;
        }
        Nation current = d.nationOf(player.getUUID());
        if (current == target) return;
        if (current != null && d.phase == GamePhase.RUNNING && !current.eliminated && !player.hasPermissions(2)) {
            player.displayClientMessage(Component.translatable("imperium.err.cannot_switch").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (current != null) removeMember(current, player.getUUID(), player.getGameProfile().getName());
        target.members.add(player.getUUID());
        target.memberNames.add(player.getGameProfile().getName());
        if (target.leader == null) target.leader = player.getUUID();
        if (target.bot) {
            target.bot = false;
            target.leader = player.getUUID();
        }
        d.markChanged();
        Notify.all(server, Component.translatable("imperium.msg.joined", player.getDisplayName(), target.name).withStyle(ChatFormatting.AQUA), false);
        if (d.phase == GamePhase.RUNNING) {
            teleportToCapital(player, d, target);
            updateRespawns(player.serverLevel(), d, target);
        }
    }

    public static void leave(GameData d, ServerPlayer player) {
        Nation current = d.nationOf(player.getUUID());
        if (current == null) return;
        removeMember(current, player.getUUID(), player.getGameProfile().getName());
        d.markChanged();
    }

    private static void removeMember(Nation n, UUID id, String name) {
        n.members.remove(id);
        n.memberNames.remove(name);
        if (id.equals(n.leader)) n.leader = n.members.isEmpty() ? null : n.members.iterator().next();
    }

    public static BlockPos capitalSpawn(GameData d, Nation n) {
        Province cap = d.provinces.get(n.capitalProvince);
        if (cap == null || cap.core == null) return null;
        int half = 4;
        return cap.core.offset(0, 0, half + 2);
    }

    public static void teleportToCapital(ServerPlayer p, GameData d, Nation n) {
        BlockPos spawn = capitalSpawn(d, n);
        if (spawn == null) return;
        ServerLevel level = p.server.overworld();
        BlockPos safe = Economy.findStandPos(level, spawn, 2);
        p.teleportTo(level, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, p.getYRot(), p.getXRot());
    }

    public static void updateRespawns(ServerLevel level, GameData d, Nation n) {
        BlockPos spawn = capitalSpawn(d, n);
        if (spawn == null) return;
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (n.isMember(p.getUUID())) p.setRespawnPosition(level.dimension(), spawn, 0, true, false);
        }
    }

    // ------------------------------------------------------------------ start / stop

    /** @return null if the game started, else the reason */
    public static Component start(MinecraftServer server, GameData d) {
        if (d.phase == GamePhase.RUNNING) return Component.translatable("imperium.err.already_running");
        if (!d.arenaSet) return Component.translatable("imperium.err.no_arena");
        d.recomputeGeometry();
        List<Nation> playable = new ArrayList<>();
        for (Nation n : d.nations.values()) if (d.cellsOf(n.id) > 0) playable.add(n);
        if (playable.size() < 2) return Component.translatable("imperium.err.need_nations");
        // nations without land are removed
        for (Nation n : new ArrayList<>(d.nations.values())) if (!playable.contains(n)) deleteNation(d, n.id);
        ServerLevel level = server.overworld();

        // assign players
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (d.nationOf(p.getUUID()) != null || p.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) continue;
            Nation smallest = playable.stream().filter(n -> !n.bot).min(Comparator.comparingInt(n -> n.members.size())).orElse(null);
            if (smallest == null) smallest = playable.get(0);
            smallest.members.add(p.getUUID());
            smallest.memberNames.add(p.getGameProfile().getName());
            if (smallest.leader == null) smallest.leader = p.getUUID();
            d.markChanged();
        }

        d.relations.clear();
        d.proposals.clear();
        d.buildings.clear();
        BuildingManager.clearCaches();
        for (Nation n : d.nations.values()) {
            if (n.members.isEmpty() && ImperiumConfig.AUTO_BOTS.get()) n.bot = true;
            if (!n.members.isEmpty()) n.bot = false;
            n.stock = Resources.of(ResourceType.WOOD, 150, ResourceType.STONE, 120, ResourceType.IRON, 20,
                    ResourceType.GOLD, 60, ResourceType.FOOD, 200);
            n.population = ImperiumConfig.STARTING_POPULATION.get();
            n.happiness = 60;
            n.research = 10;
            n.techs.clear();
            n.tax = TaxLevel.NORMAL;
            n.eliminated = false;
            n.warCycles = 0;
            n.soldierCount = 0;
            n.citizenCount = 0;
            n.hardshipCycles = 0;
            n.famine = false;
            n.unpaid = false;
            n.log.clear();
            n.lastProvinceLostTick = -100000;
            Province cap = d.provinces.get(n.capitalProvince);
            if (cap == null || cap.owner != n.id || cap.cellCount == 0) {
                n.capitalProvince = d.provincesOf(n.id).stream().filter(p -> p.cellCount > 0)
                        .max(Comparator.comparingInt(p -> p.cellCount)).map(p -> p.id).orElse(-1);
            }
        }

        // towers & cores
        for (Province p : d.provinces.values()) {
            if (p.cellCount == 0) {
                p.core = null;
                continue;
            }
            p.originalOwner = p.owner;
            Nation owner = d.nation(p.owner);
            boolean capital = owner != null && owner.capitalProvince == p.id;
            BlockPos base = pickCoreSite(level, d, p);
            TowerBuilder.build(level, base, owner, capital);
            p.core = base;
            p.coreBuilt = true;
            p.coreMaxHp = WarManager.maxCoreHp(d, p);
            p.coreHp = p.coreMaxHp;
            p.lastAttackTick = -100000;
        }

        d.phase = GamePhase.RUNNING;
        d.startTick = d.gameTick;
        d.graceTicks = ImperiumConfig.GRACE_PERIOD_MINUTES.get() * 60L * 20L;
        d.winner = "";

        // starting people and militia
        for (Nation n : d.nations.values()) {
            Province cap = d.provinces.get(n.capitalProvince);
            if (cap == null || cap.core == null) continue;
            for (int i = 0; i < 4; i++) Economy.spawnCitizen(level, d, n, cap.core);
            for (int i = 0; i < 3; i++) {
                SoldierEntity s = MilitaryManager.spawnSoldier(level, d, n, UnitType.MILITIA, cap.core.offset(0, 0, 6));
                if (s != null) s.hold(s.position());
            }
            n.addLog(Component.translatable("imperium.log.founded", n.name));
        }

        if (ImperiumConfig.SET_WORLD_BORDER.get()) {
            level.getWorldBorder().setCenter(d.centerX, d.centerZ);
            level.getWorldBorder().setSize(d.size);
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Nation n = d.nationOf(p.getUUID());
            if (n != null) teleportToCapital(p, d, n);
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("imperium.title.start").withStyle(ChatFormatting.GOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(n == null ? Component.translatable("imperium.title.spectator")
                    : Component.translatable("imperium.title.your_nation", n.name)));
        }
        for (Nation n : d.nations.values()) updateRespawns(level, d, n);
        Notify.all(server, Component.translatable("imperium.msg.game_started", ImperiumConfig.GRACE_PERIOD_MINUTES.get())
                .withStyle(ChatFormatting.GOLD), true);
        d.markTerritoryChanged();
        d.markChanged();
        return null;
    }

    private static BlockPos pickCoreSite(ServerLevel level, GameData d, Province p) {
        List<int[]> cells = new ArrayList<>();
        for (int i = 0; i < d.cells.length; i++) {
            if (d.cells[i] == p.id) cells.add(new int[]{i % d.gridN, i / d.gridN});
        }
        double cx = p.core != null ? p.core.getX() : p.centroidX;
        double cz = p.core != null ? p.core.getZ() : p.centroidZ;
        cells.sort(Comparator.comparingDouble(c -> {
            double x = d.cellCenterX(c[0]) - cx, z = d.cellCenterZ(c[1]) - cz;
            return x * x + z * z;
        }));
        BlockPos fallback = null;
        int tries = 0;
        // nearest cells first, then a coarse sweep over the whole province looking for dry land
        int stride = Math.max(1, cells.size() / 30);
        for (int i = 0; i < cells.size(); i += (i < 12 ? 1 : stride)) {
            int[] c = cells.get(i);
            int x = (int) Math.floor(d.cellCenterX(c[0]));
            int z = (int) Math.floor(d.cellCenterZ(c[1]));
            BlockPos ground = TowerBuilder.groundAt(level, x, z);
            if (fallback == null) fallback = ground;
            if (!TowerBuilder.isWater(level, ground) && !TowerBuilder.isWater(level, ground.offset(4, 0, 4))
                    && !TowerBuilder.isWater(level, ground.offset(-4, 0, -4))) return ground;
            if (++tries > 45) break;
        }
        return fallback != null ? fallback : TowerBuilder.groundAt(level, (int) cx, (int) cz);
    }

    /** Ends the running game (keeps the map). */
    public static void stop(MinecraftServer server, GameData d) {
        d.phase = GamePhase.ENDED;
        Notify.all(server, Component.translatable("imperium.msg.game_stopped").withStyle(ChatFormatting.YELLOW), true);
        d.markChanged();
    }

    /** Back to setup: removes units, buildings and diplomacy, keeps arena, nations and provinces. */
    public static void reset(MinecraftServer server, GameData d) {
        for (SoldierEntity s : UnitRegistry.soldiers()) s.discard();
        for (CitizenEntity c : UnitRegistry.citizens()) c.discard();
        d.phase = GamePhase.SETUP;
        d.buildings.clear();
        d.relations.clear();
        d.proposals.clear();
        d.winner = "";
        for (Nation n : d.nations.values()) {
            n.eliminated = false;
            n.soldierCount = 0;
            n.citizenCount = 0;
            n.log.clear();
        }
        for (Province p : d.provinces.values()) {
            p.owner = p.originalOwner >= 0 ? p.originalOwner : p.owner;
            p.coreBuilt = false;
        }
        BuildingManager.clearCaches();
        d.markTerritoryChanged();
        d.markChanged();
        Notify.all(server, Component.translatable("imperium.msg.game_reset").withStyle(ChatFormatting.YELLOW), false);
    }

    /** Wipes absolutely everything. */
    public static void wipe(MinecraftServer server, GameData d) {
        reset(server, d);
        d.nations.clear();
        d.provinces.clear();
        java.util.Arrays.fill(d.cells, -1);
        d.recomputeGeometry();
        d.markTerritoryChanged();
        d.markChanged();
    }

    // ------------------------------------------------------------------ sync

    public static void sendFullState(ServerPlayer p, GameData d) {
        ImperiumNet.sendTo(p, new StatePacket(d.write(new CompoundTag(), false)));
        ImperiumNet.sendTo(p, new TerritoryPacket(d.gridN, d.cells));
    }
}
