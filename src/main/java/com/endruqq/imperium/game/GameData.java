package com.endruqq.imperium.game;

import com.endruqq.imperium.game.building.Building;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The whole state of a strategy game: arena, territory grid, nations, provinces,
 * buildings, diplomacy. Stored in the overworld and mirrored to clients.
 */
public class GameData extends SavedData {
    public static final String NAME = "imperium_game";

    public GamePhase phase = GamePhase.SETUP;
    public long gameTick;
    public long startTick;
    public long graceTicks;
    /** bumped at every start/reset: citizens from an older game are removed when their chunk loads */
    public int session;

    // ---- arena ----
    public boolean arenaSet;
    public int centerX;
    public int centerZ;
    public int size = 500;
    public int cellSize = 8;
    public int gridN;
    /** province id per cell, -1 = wilderness */
    public int[] cells = new int[0];

    public final Map<Integer, Nation> nations = new LinkedHashMap<>();
    public final Map<Integer, Province> provinces = new LinkedHashMap<>();
    public final Map<Integer, Building> buildings = new LinkedHashMap<>();
    public final Map<Long, Relation> relations = new HashMap<>();
    public final List<Proposal> proposals = new ArrayList<>();
    /** citizens' lives, by entity UUID (server only, not sent to clients) */
    public final Map<UUID, Person> people = new LinkedHashMap<>();
    /** citizens that died while their chunk was unloaded; their entities are removed when loaded */
    public final Set<UUID> deceased = new LinkedHashSet<>();
    public final List<CasusBelli> casusBelli = new ArrayList<>();
    /** claims being fabricated by spies; {@link CasusBelli#expires} is the tick they are ready */
    public final List<CasusBelli> claimsInProgress = new ArrayList<>();
    public int nextNationId = 1;
    public int nextProvinceId = 1;
    public int nextBuildingId = 1;
    public int nextProposalId = 1;
    public String winner = "";

    // ---- transient ----
    public transient boolean stateDirty = true;
    public transient boolean territoryDirty = true;
    private transient Map<UUID, Integer> memberIndex;

    public GameData() {}

    public static GameData get(MinecraftServer server) {
        ServerLevel level = server.overworld();
        return level.getDataStorage().computeIfAbsent(GameData::loadFrom, GameData::new, NAME);
    }

    public void markChanged() {
        stateDirty = true;
        memberIndex = null;
        setDirty();
    }

    public void markTerritoryChanged() {
        territoryDirty = true;
        stateDirty = true;
        setDirty();
    }

    // ------------------------------------------------------------------ arena / territory

    public void setArena(int cx, int cz, int newSize) {
        this.arenaSet = true;
        this.centerX = cx;
        this.centerZ = cz;
        this.size = newSize;
        this.cellSize = Math.max(8, (int) Math.ceil(newSize / 125.0 / 4.0) * 4);
        this.gridN = (int) Math.ceil(newSize / (double) cellSize);
        this.cells = new int[gridN * gridN];
        Arrays.fill(cells, -1);
        recomputeGeometry();
        markTerritoryChanged();
    }

    public int minX() {
        return centerX - size / 2;
    }

    public int minZ() {
        return centerZ - size / 2;
    }

    public int maxX() {
        return minX() + size;
    }

    public int maxZ() {
        return minZ() + size;
    }

    public boolean inArena(double x, double z) {
        return arenaSet && x >= minX() && x < maxX() && z >= minZ() && z < maxZ();
    }

    public int cellX(double x) {
        return (int) Math.floor((x - minX()) / cellSize);
    }

    public int cellZ(double z) {
        return (int) Math.floor((z - minZ()) / cellSize);
    }

    public int cellIndex(double x, double z) {
        if (!arenaSet) return -1;
        int cx = cellX(x), cz = cellZ(z);
        if (cx < 0 || cz < 0 || cx >= gridN || cz >= gridN) return -1;
        return cz * gridN + cx;
    }

    public int provinceIdAt(double x, double z) {
        int i = cellIndex(x, z);
        return i < 0 || i >= cells.length ? -1 : cells[i];
    }

    public Province provinceAt(double x, double z) {
        int id = provinceIdAt(x, z);
        return id < 0 ? null : provinces.get(id);
    }

    public Province provinceAt(BlockPos pos) {
        return provinceAt(pos.getX() + 0.5, pos.getZ() + 0.5);
    }

    public int nationIdAt(double x, double z) {
        Province p = provinceAt(x, z);
        return p == null ? -1 : p.owner;
    }

    public int nationIdAt(BlockPos pos) {
        return nationIdAt(pos.getX() + 0.5, pos.getZ() + 0.5);
    }

    public Nation nationAt(BlockPos pos) {
        return nations.get(nationIdAt(pos));
    }

    /** cell center in world coordinates */
    public double cellCenterX(int cx) {
        return minX() + (cx + 0.5) * cellSize;
    }

    public double cellCenterZ(int cz) {
        return minZ() + (cz + 0.5) * cellSize;
    }

    public void recomputeGeometry() {
        Map<Integer, double[]> acc = new HashMap<>();
        for (int i = 0; i < cells.length; i++) {
            int pid = cells[i];
            if (pid < 0) continue;
            double[] a = acc.computeIfAbsent(pid, k -> new double[3]);
            a[0] += i % gridN;
            a[1] += i / gridN;
            a[2]++;
        }
        for (Province p : provinces.values()) {
            double[] a = acc.get(p.id);
            if (a == null || a[2] == 0) {
                p.cellCount = 0;
                continue;
            }
            p.cellCount = (int) a[2];
            p.centroidX = cellCenterX(0) + a[0] / a[2] * cellSize;
            p.centroidZ = cellCenterZ(0) + a[1] / a[2] * cellSize;
        }
    }

    // ------------------------------------------------------------------ nations

    public Nation nation(int id) {
        return nations.get(id);
    }

    public Collection<Nation> activeNations() {
        List<Nation> l = new ArrayList<>();
        for (Nation n : nations.values()) if (!n.eliminated) l.add(n);
        return l;
    }

    public Nation nationOf(UUID player) {
        if (memberIndex == null) {
            memberIndex = new HashMap<>();
            for (Nation n : nations.values()) for (UUID u : n.members) memberIndex.put(u, n.id);
        }
        Integer id = memberIndex.get(player);
        return id == null ? null : nations.get(id);
    }

    public int nationIdOf(UUID player) {
        Nation n = nationOf(player);
        return n == null ? -1 : n.id;
    }

    public List<Province> provincesOf(int nationId) {
        List<Province> l = new ArrayList<>();
        for (Province p : provinces.values()) if (p.owner == nationId) l.add(p);
        return l;
    }

    public int cellsOf(int nationId) {
        int c = 0;
        for (Province p : provinces.values()) if (p.owner == nationId) c += p.cellCount;
        return c;
    }

    // ------------------------------------------------------------------ relations

    public Relation relation(int a, int b) {
        if (a == b || a < 0 || b < 0) return null;
        return relations.computeIfAbsent(Relation.key(a, b), k -> new Relation(a, b));
    }

    public RelationType relationType(int a, int b) {
        if (a == b || a < 0 || b < 0) return RelationType.PEACE;
        Relation r = relations.get(Relation.key(a, b));
        return r == null ? RelationType.PEACE : r.type;
    }

    public boolean atWar(int a, int b) {
        return a >= 0 && b >= 0 && a != b && relationType(a, b) == RelationType.WAR;
    }

    public boolean allied(int a, int b) {
        return a >= 0 && b >= 0 && a != b && relationType(a, b) == RelationType.ALLIANCE;
    }

    /** same nation or allied */
    public boolean friendly(int a, int b) {
        return a >= 0 && (a == b || allied(a, b));
    }

    public boolean atWarWithAnyone(int a) {
        for (Nation n : nations.values()) if (n.id != a && !n.eliminated && atWar(a, n.id)) return true;
        return false;
    }

    public List<Nation> enemiesOf(int a) {
        List<Nation> l = new ArrayList<>();
        for (Nation n : nations.values()) if (n.id != a && !n.eliminated && atWar(a, n.id)) l.add(n);
        return l;
    }

    // ------------------------------------------------------------------ buildings

    public List<Building> buildingsOf(int nationId) {
        List<Building> l = new ArrayList<>();
        for (Building b : buildings.values()) if (b.nation == nationId) l.add(b);
        return l;
    }

    public int countActive(int nationId, String typeId) {
        int c = 0;
        for (Building b : buildings.values()) if (b.nation == nationId && b.isActive() && b.typeId.equals(typeId)) c++;
        return c;
    }

    public int countAll(int nationId, String typeId) {
        int c = 0;
        for (Building b : buildings.values()) if (b.nation == nationId && b.typeId.equals(typeId)) c++;
        return c;
    }

    public boolean hasActive(int nationId, String typeId) {
        return countActive(nationId, typeId) > 0;
    }

    public Building buildingAtSite(BlockPos pos) {
        for (Building b : buildings.values()) if (b.pos.equals(pos)) return b;
        return null;
    }

    public Building buildingContaining(BlockPos pos) {
        for (Building b : buildings.values()) if (b.contains(pos)) return b;
        return null;
    }

    public Building buildingContainingXZ(double x, double z) {
        for (Building b : buildings.values()) if (b.containsXZ(x, z, 0)) return b;
        return null;
    }

    public List<Person> peopleOf(int nationId) {
        List<Person> l = new ArrayList<>();
        for (Person p : people.values()) if (p.nation == nationId) l.add(p);
        return l;
    }

    public List<Person> workersAt(int buildingId) {
        List<Person> l = new ArrayList<>();
        for (Person p : people.values()) if (p.workplace == buildingId) l.add(p);
        return l;
    }

    // ------------------------------------------------------------------ proposals

    public Proposal proposal(int id) {
        for (Proposal p : proposals) if (p.id == id) return p;
        return null;
    }

    // ------------------------------------------------------------------ time

    public boolean inGracePeriod() {
        return phase == GamePhase.RUNNING && gameTick - startTick < graceTicks;
    }

    public long graceRemainingTicks() {
        return Math.max(0, graceTicks - (gameTick - startTick));
    }

    // ------------------------------------------------------------------ persistence

    public static GameData loadFrom(CompoundTag tag) {
        GameData d = new GameData();
        d.load(tag, true);
        return d;
    }

    public void load(CompoundTag tag, boolean includeCells) {
        phase = GamePhase.values()[Math.min(GamePhase.values().length - 1, tag.getInt("phase"))];
        gameTick = tag.getLong("tick");
        startTick = tag.getLong("start");
        graceTicks = tag.getLong("grace");
        session = tag.getInt("session");
        arenaSet = tag.getBoolean("arena");
        centerX = tag.getInt("cx");
        centerZ = tag.getInt("cz");
        size = tag.contains("size") ? tag.getInt("size") : 500;
        cellSize = Math.max(1, tag.contains("cellSize") ? tag.getInt("cellSize") : 8);
        gridN = tag.getInt("gridN");
        if (includeCells) {
            cells = tag.getIntArray("cells");
            if (cells.length != gridN * gridN) {
                cells = new int[gridN * gridN];
                Arrays.fill(cells, -1);
            }
        }
        nations.clear();
        ListTag nl = tag.getList("nations", Tag.TAG_COMPOUND);
        for (int i = 0; i < nl.size(); i++) {
            Nation n = Nation.load(nl.getCompound(i));
            nations.put(n.id, n);
        }
        provinces.clear();
        ListTag pl = tag.getList("provinces", Tag.TAG_COMPOUND);
        for (int i = 0; i < pl.size(); i++) {
            Province p = Province.load(pl.getCompound(i));
            provinces.put(p.id, p);
        }
        buildings.clear();
        ListTag bl = tag.getList("buildings", Tag.TAG_COMPOUND);
        for (int i = 0; i < bl.size(); i++) {
            Building b = Building.load(bl.getCompound(i));
            buildings.put(b.id, b);
        }
        relations.clear();
        ListTag rl = tag.getList("relations", Tag.TAG_COMPOUND);
        for (int i = 0; i < rl.size(); i++) {
            Relation r = Relation.load(rl.getCompound(i));
            relations.put(Relation.key(r.a, r.b), r);
        }
        proposals.clear();
        ListTag prl = tag.getList("proposals", Tag.TAG_COMPOUND);
        for (int i = 0; i < prl.size(); i++) proposals.add(Proposal.load(prl.getCompound(i)));
        casusBelli.clear();
        ListTag cbl = tag.getList("cb", Tag.TAG_COMPOUND);
        for (int i = 0; i < cbl.size(); i++) casusBelli.add(CasusBelli.load(cbl.getCompound(i)));
        claimsInProgress.clear();
        ListTag cll = tag.getList("claims", Tag.TAG_COMPOUND);
        for (int i = 0; i < cll.size(); i++) claimsInProgress.add(CasusBelli.load(cll.getCompound(i)));
        if (includeCells) {
            people.clear();
            ListTag pel = tag.getList("people", Tag.TAG_COMPOUND);
            for (int i = 0; i < pel.size(); i++) {
                Person p = Person.load(pel.getCompound(i));
                people.put(p.id, p);
            }
            deceased.clear();
            ListTag del = tag.getList("deceased", Tag.TAG_INT_ARRAY);
            for (int i = 0; i < del.size(); i++) deceased.add(net.minecraft.nbt.NbtUtils.loadUUID(del.get(i)));
        }
        nextNationId = Math.max(1, tag.getInt("nextNation"));
        nextProvinceId = Math.max(1, tag.getInt("nextProvince"));
        nextBuildingId = Math.max(1, tag.getInt("nextBuilding"));
        nextProposalId = Math.max(1, tag.getInt("nextProposal"));
        winner = tag.getString("winner");
        memberIndex = null;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        write(tag, true);
        return tag;
    }

    public CompoundTag write(CompoundTag tag, boolean includeCells) {
        tag.putInt("phase", phase.ordinal());
        tag.putLong("tick", gameTick);
        tag.putLong("start", startTick);
        tag.putLong("grace", graceTicks);
        tag.putInt("session", session);
        tag.putBoolean("arena", arenaSet);
        tag.putInt("cx", centerX);
        tag.putInt("cz", centerZ);
        tag.putInt("size", size);
        tag.putInt("cellSize", cellSize);
        tag.putInt("gridN", gridN);
        if (includeCells) tag.putIntArray("cells", cells);
        ListTag nl = new ListTag();
        for (Nation n : nations.values()) nl.add(n.save());
        tag.put("nations", nl);
        ListTag pl = new ListTag();
        for (Province p : provinces.values()) pl.add(p.save());
        tag.put("provinces", pl);
        ListTag bl = new ListTag();
        for (Building b : buildings.values()) bl.add(b.save());
        tag.put("buildings", bl);
        ListTag rl = new ListTag();
        for (Relation r : relations.values()) rl.add(r.save());
        tag.put("relations", rl);
        ListTag prl = new ListTag();
        for (Proposal p : proposals) prl.add(p.save());
        tag.put("proposals", prl);
        ListTag cbl = new ListTag();
        for (CasusBelli c : casusBelli) cbl.add(c.save());
        tag.put("cb", cbl);
        ListTag cll = new ListTag();
        for (CasusBelli c : claimsInProgress) cll.add(c.save());
        tag.put("claims", cll);
        if (includeCells) {
            ListTag pel = new ListTag();
            for (Person p : people.values()) pel.add(p.save());
            tag.put("people", pel);
            ListTag del = new ListTag();
            for (UUID u : deceased) del.add(net.minecraft.nbt.NbtUtils.createUUID(u));
            tag.put("deceased", del);
        }
        tag.putInt("nextNation", nextNationId);
        tag.putInt("nextProvince", nextProvinceId);
        tag.putInt("nextBuilding", nextBuildingId);
        tag.putInt("nextProposal", nextProposalId);
        tag.putString("winner", winner);
        return tag;
    }
}
