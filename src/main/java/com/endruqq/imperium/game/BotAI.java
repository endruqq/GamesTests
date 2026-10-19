package com.endruqq.imperium.game;

import com.endruqq.imperium.entity.OrderType;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.UnitRegistry;
import com.endruqq.imperium.entity.UnitType;
import com.endruqq.imperium.game.building.BuildingType;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.game.tech.Tech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Computer-controlled nations: economy, research, army, diplomacy and war. */
public final class BotAI {
    private static final Tech[] RESEARCH_ORDER = {
            Tech.AGRICULTURE, Tech.MINING, Tech.WRITING, Tech.ARCHERY, Tech.METALLURGY, Tech.TRADE, Tech.FORTIFICATION,
            Tech.ARCHITECTURE, Tech.DIPLOMACY, Tech.RELIGION, Tech.STEEL_WEAPONS, Tech.SIEGE, Tech.MEDICINE, Tech.BANKING,
            Tech.TACTICS, Tech.INDUSTRY, Tech.ADMINISTRATION, Tech.GUNPOWDER, Tech.GUILDS, Tech.MILITARY_SERVICE, Tech.ENLIGHTENMENT,
            Tech.URBANIZATION, Tech.MASS_PRODUCTION, Tech.ESPIONAGE
    };
    private static final Object[][] BUILD_ORDER = {
            {Buildings.LUMBER_MILL, 2}, {Buildings.MINE, 2}, {Buildings.ACADEMY, 1}, {Buildings.TAVERN, 1}, {Buildings.MARKET, 1},
            {Buildings.SMELTERY, 1}, {Buildings.GRANARY, 1}, {Buildings.ARCHERY_RANGE, 1}, {Buildings.TEMPLE, 1},
            {Buildings.WATCHTOWER, 1}, {Buildings.ARMORY, 1}, {Buildings.HOSPITAL, 1}, {Buildings.FORTRESS, 1},
            {Buildings.SIEGE_WORKSHOP, 1}, {Buildings.FACTORY, 1}, {Buildings.BANK, 1}, {Buildings.FARM, 4}, {Buildings.MARKET, 2},
            {Buildings.ACADEMY, 2}, {Buildings.FACTORY, 2}
    };

    private BotAI() {}

    public static double strength(GameData d, Nation n) {
        return n.soldierCount * 10.0 + n.population + d.provincesOf(n.id).size() * 6.0 + n.techs.size() * 2.0;
    }

    public static void cycle(ServerLevel level, GameData d) {
        Map<Integer, Set<Integer>> neighbours = neighbours(d);
        for (Nation n : new ArrayList<>(d.nations.values())) {
            if (!n.bot || n.eliminated) continue;
            try {
                think(level, d, n, neighbours.getOrDefault(n.id, Set.of()));
            } catch (Exception e) {
                com.endruqq.imperium.Imperium.LOGGER.error("Bot {} failed to think", n.name, e);
            }
        }
    }

    private static void think(ServerLevel level, GameData d, Nation n, Set<Integer> neighbours) {
        RandomSource rnd = level.random;
        research(n);
        if (rnd.nextInt(2) == 0) build(level, d, n, rnd);
        army(level, d, n);
        diplomacy(level, d, n, neighbours, rnd);
        war(level, d, n);
        borders(d, n, neighbours);
        PrisonerManager.botCycle(level, d, n);
        if (n.stock.get(ResourceType.FOOD) < n.population * 2 && n.tax != TaxLevel.LOW) n.tax = TaxLevel.LOW;
        else if (n.happiness > 75 && n.tax == TaxLevel.LOW) n.tax = TaxLevel.NORMAL;
    }

    // ------------------------------------------------------------------ research

    private static void research(Nation n) {
        for (Tech t : RESEARCH_ORDER) {
            if (t.available(n)) {
                if (n.research >= t.cost) {
                    n.research -= t.cost;
                    n.techs.add(t);
                }
                return;
            }
        }
    }

    // ------------------------------------------------------------------ economy

    private static void build(ServerLevel level, GameData d, Nation n, RandomSource rnd) {
        BuildingType want = null;
        int farms = d.countAll(n.id, Buildings.FARM.id);
        if (farms == 0 || (n.lastDelta.get(ResourceType.FOOD) < 2 && farms < 5)) want = Buildings.FARM;
        else if (n.population >= n.housing - 2) {
            if (Buildings.MANOR.unlocked(n) && n.stock.has(Buildings.MANOR.prefabCost)) want = Buildings.MANOR;
            else if (Buildings.STONE_HOUSE.unlocked(n) && n.stock.has(Buildings.STONE_HOUSE.prefabCost)) want = Buildings.STONE_HOUSE;
            else want = Buildings.HUT;
        } else if (d.countAll(n.id, Buildings.BARRACKS.id) == 0) want = Buildings.BARRACKS;
        else {
            for (Object[] e : BUILD_ORDER) {
                BuildingType t = (BuildingType) e[0];
                int max = (Integer) e[1];
                if (t.unlocked(n) && d.countAll(n.id, t.id) < max && n.stock.has(t.prefabCost)) {
                    want = t;
                    break;
                }
            }
        }
        if (want == null || want.prefabCost == null || !n.stock.has(want.prefabCost)) return;
        BlockPos site = findSite(level, d, n, want, rnd);
        if (site == null) return;
        Direction front = Direction.Plane.HORIZONTAL.getRandomDirection(rnd);
        BuildingManager.placeFor(level, d, n, want, site, true, front, null);
    }

    private static BlockPos findSite(ServerLevel level, GameData d, Nation n, BuildingType type, RandomSource rnd) {
        Province cap = d.provinces.get(n.capitalProvince);
        List<Province> own = d.provincesOf(n.id);
        if (own.isEmpty()) return null;
        for (int attempt = 0; attempt < 30; attempt++) {
            Province p = cap != null && cap.owner == n.id && attempt < 18 ? cap : own.get(rnd.nextInt(own.size()));
            if (p.core == null) continue;
            double radius = 14 + attempt * 2.5;
            double ang = rnd.nextDouble() * Math.PI * 2;
            int x = (int) (p.core.getX() + Math.cos(ang) * radius);
            int z = (int) (p.core.getZ() + Math.sin(ang) * radius);
            if (d.nationIdAt(x + 0.5, z + 0.5) != n.id) continue;
            level.getChunk(x >> 4, z >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos.below()).getFluidState().isEmpty()) continue;
            if (!flatEnough(level, pos, type.radius)) continue;
            if (BuildingManager.validate(d, n.id, type, pos) == null) return pos;
        }
        return null;
    }

    private static boolean flatEnough(ServerLevel level, BlockPos c, int r) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int[] o : new int[][]{{-r, -r}, {r, -r}, {-r, r}, {r, r}, {0, 0}}) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX() + o[0], c.getZ() + o[1]);
            min = Math.min(min, y);
            max = Math.max(max, y);
        }
        return max - min <= 5;
    }

    // ------------------------------------------------------------------ army

    private static void army(ServerLevel level, GameData d, Nation n) {
        boolean war = d.atWarWithAnyone(n.id);
        int desired = 3 + n.population / (war ? 3 : 5);
        if (n.soldierCount >= desired) return;
        UnitType[] prefs = war && n.soldierCount % 5 == 4 ? new UnitType[]{UnitType.SAPPER, UnitType.KNIGHT, UnitType.SWORDSMAN, UnitType.MILITIA}
                : n.soldierCount % 3 == 2 ? new UnitType[]{UnitType.ARCHER, UnitType.KNIGHT, UnitType.SWORDSMAN, UnitType.MILITIA}
                : new UnitType[]{UnitType.KNIGHT, UnitType.SWORDSMAN, UnitType.ARCHER, UnitType.MILITIA};
        for (UnitType t : prefs) {
            if (MilitaryManager.canRecruit(d, n, t) == null) {
                MilitaryManager.recruit(level, d, n, t, 1, null);
                return;
            }
        }
    }

    // ------------------------------------------------------------------ diplomacy

    /** Close the borders in war or among hostile neighbours, open them when friendly. */
    private static void borders(GameData d, Nation n, Set<Integer> neighbours) {
        boolean hostile = d.atWarWithAnyone(n.id);
        for (int id : neighbours) {
            Relation r = d.relation(n.id, id);
            if (r != null && r.opinionOf(n.id, id) < -30) hostile = true;
        }
        if (n.bordersClosed != hostile) {
            n.bordersClosed = hostile;
            d.markChanged();
        }
    }

    public static Map<Integer, Set<Integer>> neighbours(GameData d) {
        Map<Integer, Set<Integer>> m = new HashMap<>();
        int n = d.gridN;
        for (int z = 0; z < n; z++) {
            for (int x = 0; x < n; x++) {
                int a = owner(d, d.cells[z * n + x]);
                if (a < 0) continue;
                if (x + 1 < n) link(m, a, owner(d, d.cells[z * n + x + 1]));
                if (z + 1 < n) link(m, a, owner(d, d.cells[(z + 1) * n + x]));
            }
        }
        return m;
    }

    private static int owner(GameData d, int provinceId) {
        Province p = d.provinces.get(provinceId);
        return p == null ? -1 : p.owner;
    }

    private static void link(Map<Integer, Set<Integer>> m, int a, int b) {
        if (b < 0 || a == b) return;
        m.computeIfAbsent(a, k -> new HashSet<>()).add(b);
        m.computeIfAbsent(b, k -> new HashSet<>()).add(a);
    }

    private static void diplomacy(ServerLevel level, GameData d, Nation n, Set<Integer> neighbours, RandomSource rnd) {
        double mine = strength(d, n);
        // opinions drift towards 0, neighbours are a bit suspicious
        for (Nation o : d.nations.values()) {
            if (o.id == n.id || o.eliminated) continue;
            Relation r = d.relation(n.id, o.id);
            int op = r.opinionOf(n.id, o.id);
            if (op > 0 && rnd.nextInt(3) == 0) r.addOpinion(n.id, o.id, -1);
            if (op < 0 && rnd.nextInt(3) == 0) r.addOpinion(n.id, o.id, 1);
            if (neighbours.contains(o.id) && rnd.nextInt(4) == 0) r.addOpinion(n.id, o.id, -1);
        }
        List<Nation> enemies = d.enemiesOf(n.id);
        if (!enemies.isEmpty()) {
            for (Nation e : enemies) {
                double theirs = strength(d, e);
                boolean losing = theirs > mine * 1.4 || d.gameTick - n.lastProvinceLostTick < 20 * 60 * 3;
                Relation wr = d.relation(n.id, e.id);
                int lead = wr.scoreOf(n.id) - wr.scoreOf(e.id);
                if (lead >= 25 && rnd.nextInt(3) == 0) {
                    DiplomacyManager.perform(level.getServer(), d, n.id, e.id, DiplomacyManager.Action.PEACE_PROVINCE);
                } else if ((losing || n.warCycles > 45) && rnd.nextInt(4) == 0) {
                    boolean rich = n.stock.get(ResourceType.GOLD) > DiplomacyManager.PEACE_GOLD * 2;
                    DiplomacyManager.perform(level.getServer(), d, n.id, e.id, rich && losing ? DiplomacyManager.Action.PEACE_GOLD
                            : DiplomacyManager.Action.PROPOSE_PEACE);
                } else if (theirs > mine && rnd.nextInt(6) == 0) {
                    for (Nation ally : d.nations.values()) {
                        if (!ally.eliminated && d.allied(n.id, ally.id) && !d.atWar(ally.id, e.id)) {
                            DiplomacyManager.perform(level.getServer(), d, n.id, ally.id, DiplomacyManager.Action.CALL_TO_ARMS);
                            break;
                        }
                    }
                }
            }
            return;
        }
        if (d.inGracePeriod()) return;
        // aggression: attack the weakest neighbour we don't like
        Nation victim = null;
        double victimStrength = Double.MAX_VALUE;
        for (int id : neighbours) {
            Nation o = d.nation(id);
            if (o == null || o.eliminated) continue;
            Relation r = d.relation(n.id, o.id);
            if (r.type != RelationType.PEACE || d.gameTick < r.truceUntil) continue;
            if (r.opinionOf(n.id, o.id) > 15) continue;
            double s = strength(d, o);
            if (s < victimStrength) {
                victimStrength = s;
                victim = o;
            }
        }
        // a neighbour we have a good reason to fight is a much more tempting target
        Nation justified = null;
        for (int id : neighbours) {
            Nation o = d.nation(id);
            if (o == null || o.eliminated) continue;
            Relation r = d.relation(n.id, o.id);
            if (r.type != RelationType.PEACE || d.gameTick < r.truceUntil) continue;
            if (WarReasons.justified(d, n.id, o.id) && mine > strength(d, o) * 1.1) {
                justified = o;
                break;
            }
        }
        if (justified != null && n.soldierCount >= 5 && rnd.nextInt(4) == 0) {
            DiplomacyManager.perform(level.getServer(), d, n.id, justified.id, DiplomacyManager.Action.DECLARE_WAR);
            return;
        }
        if (victim != null && mine > victimStrength * 1.6 && n.soldierCount >= 6 && rnd.nextInt(10) == 0) {
            if (n.has(Tech.ESPIONAGE) && WarReasons.claimInProgress(d, n.id, victim.id) == null && n.stock.get(ResourceType.GOLD) > 120) {
                DiplomacyManager.perform(level.getServer(), d, n.id, victim.id, DiplomacyManager.Action.FABRICATE_CLAIM);
            } else if (mine > victimStrength * 2.2 && rnd.nextBoolean()) {
                DiplomacyManager.perform(level.getServer(), d, n.id, victim.id, DiplomacyManager.Action.DEMAND_PROVINCE);
            } else {
                DiplomacyManager.perform(level.getServer(), d, n.id, victim.id, DiplomacyManager.Action.DECLARE_WAR);
            }
            return;
        }
        // friendship
        for (Nation o : d.nations.values()) {
            if (o.id == n.id || o.eliminated) continue;
            Relation r = d.relation(n.id, o.id);
            int op = r.opinionOf(n.id, o.id);
            if (n.has(Tech.TRADE) && !r.trade && r.type != RelationType.WAR && op >= -5 && rnd.nextInt(8) == 0) {
                DiplomacyManager.perform(level.getServer(), d, n.id, o.id, DiplomacyManager.Action.PROPOSE_TRADE);
            } else if (n.has(Tech.DIPLOMACY) && r.type == RelationType.PEACE && op > 25 && rnd.nextInt(10) == 0) {
                DiplomacyManager.perform(level.getServer(), d, n.id, o.id, DiplomacyManager.Action.PROPOSE_ALLIANCE);
            } else if (n.has(Tech.DIPLOMACY) && r.type == RelationType.PEACE && strength(d, o) > mine * 1.3 && rnd.nextInt(10) == 0) {
                DiplomacyManager.perform(level.getServer(), d, n.id, o.id, DiplomacyManager.Action.PROPOSE_NAP);
            } else if (r.type != RelationType.WAR && !r.access && op > 15 && rnd.nextInt(12) == 0) {
                DiplomacyManager.perform(level.getServer(), d, n.id, o.id, DiplomacyManager.Action.PROPOSE_ACCESS);
            } else if (o.famine && op > 0 && n.stock.get(ResourceType.FOOD) > 250 && rnd.nextInt(6) == 0) {
                DiplomacyManager.perform(level.getServer(), d, n.id, o.id, DiplomacyManager.Action.SEND_FOOD);
            } else if (op < -40 && rnd.nextInt(25) == 0) {
                DiplomacyManager.perform(level.getServer(), d, n.id, o.id, rnd.nextBoolean() ? DiplomacyManager.Action.INSULT
                        : DiplomacyManager.Action.DENOUNCE);
            }
        }
    }

    public static boolean evaluateProposal(GameData d, Nation bot, Nation from, Proposal p) {
        double mine = strength(d, bot), theirs = strength(d, from);
        int op = d.relation(bot.id, from.id).opinionOf(bot.id, from.id);
        RandomSource rnd = RandomSource.create();
        return switch (p.type) {
            case PEACE -> theirs > mine * 0.8 || bot.warCycles > 25 || d.gameTick - bot.lastProvinceLostTick < 20 * 60 * 3
                    || rnd.nextInt(5) == 0;
            case ALLIANCE -> op > 20 || commonEnemy(d, bot, from);
            case NON_AGGRESSION -> op > -10 || theirs > mine;
            case TRADE -> op > -20;
            case TRIBUTE -> theirs > mine * 1.6 && bot.stock.get(ResourceType.GOLD) >= p.amount;
            case ACCESS -> op > 5 || d.allied(bot.id, from.id);
            case PROVINCE -> {
                Province pr = d.provinces.get(p.amount);
                boolean capital = pr != null && bot.capitalProvince == pr.id;
                yield !capital && theirs > mine * 2.2 && op > -60;
            }
            case CALL_TO_ARMS -> {
                Nation enemy = d.nation(p.amount);
                double es = enemy == null ? 0 : strength(d, enemy);
                yield op > 5 && (mine + theirs > es * 1.2 || (enemy != null && d.relation(bot.id, enemy.id).opinionOf(bot.id, enemy.id) < -20));
            }
            case PEACE_PROVINCE -> {
                Relation wr = d.relation(bot.id, from.id);
                int lead = wr.scoreOf(from.id) - wr.scoreOf(bot.id);
                Province pr = d.provinces.get(p.amount);
                boolean capital = pr != null && bot.capitalProvince == pr.id;
                yield !capital && (lead >= 20 || theirs > mine * 1.8);
            }
            case PEACE_GOLD -> {
                Relation wr = d.relation(bot.id, from.id);
                int lead = wr.scoreOf(bot.id) - wr.scoreOf(from.id);
                yield lead < 20 || bot.warCycles > 20;
            }
        };
    }

    private static boolean commonEnemy(GameData d, Nation a, Nation b) {
        for (Nation e : d.enemiesOf(a.id)) if (d.atWar(b.id, e.id)) return true;
        return false;
    }

    // ------------------------------------------------------------------ war

    private static void war(ServerLevel level, GameData d, Nation n) {
        List<SoldierEntity> army = new ArrayList<>(UnitRegistry.soldiersOf(n.id));
        army.removeIf(SoldierEntity::isSurrendered);
        if (army.isEmpty()) return;
        // defend provinces under attack
        for (Province p : d.provincesOf(n.id)) {
            if (p.core != null && p.underAttack(d.gameTick)) {
                int[] ids = army.stream().filter(s -> s.getOrder() != OrderType.ATTACK_CORE || s.distanceToSqr(Vec3.atCenterOf(p.core)) < 64 * 64)
                        .limit(Math.max(2, army.size() * 2 / 3)).mapToInt(SoldierEntity::getId).toArray();
                MilitaryManager.command(level, d, n, ids, OrderType.ATTACK_MOVE, Vec3.atBottomCenterOf(p.core.offset(0, 0, 2)), -1, -1, null);
                return;
            }
        }
        List<Nation> enemies = d.enemiesOf(n.id);
        if (enemies.isEmpty()) {
            // peace time: send units that wandered off back to barracks, keep a patrol between the capital and a border province
            for (SoldierEntity s : army) {
                if (s.getOrder() == OrderType.ATTACK_CORE) s.order(OrderType.RETURN, s.position(), null, -1);
            }
            long patrolling = army.stream().filter(s -> s.getOrder() == OrderType.PATROL).count();
            Province cap = d.provinces.get(n.capitalProvince);
            List<Province> own = d.provincesOf(n.id);
            if (patrolling < 2 && army.size() >= 4 && cap != null && cap.core != null && own.size() > 1) {
                Province far = own.get(level.random.nextInt(own.size()));
                if (far.core != null && far.id != cap.id) {
                    List<Vec3> route = List.of(Vec3.atBottomCenterOf(cap.core.offset(0, 0, 6)), Vec3.atBottomCenterOf(far.core.offset(0, 0, 6)));
                    int sent = 0;
                    for (SoldierEntity s : army) {
                        if (s.getOrder() == OrderType.HOLD && sent < 2) {
                            s.patrol(route);
                            sent++;
                        }
                    }
                }
            }
            return;
        }
        List<SoldierEntity> idle = new ArrayList<>();
        for (SoldierEntity s : army) {
            if (s.isSurrendered()) continue;
            if (s.getOrder() == OrderType.HOLD || s.getOrder() == OrderType.RETURN || s.getOrder() == OrderType.PATROL) idle.add(s);
        }
        if (idle.size() < 4) return;
        Province cap = d.provinces.get(n.capitalProvince);
        Vec3 from = cap != null && cap.core != null ? Vec3.atCenterOf(cap.core) : idle.get(0).position();
        Province target = null;
        double best = Double.MAX_VALUE;
        for (Province p : d.provinces.values()) {
            if (p.core == null || !WarManager.canAttackCore(d, n.id, p)) continue;
            double dist = from.distanceToSqr(Vec3.atCenterOf(p.core)) - (p.coreHp < p.coreMaxHp * 0.5 ? 10000 : 0);
            if (dist < best) {
                best = dist;
                target = p;
            }
        }
        if (target == null) return;
        int keep = Math.min(2, idle.size() / 4);
        int[] ids = idle.stream().skip(keep).mapToInt(SoldierEntity::getId).toArray();
        MilitaryManager.command(level, d, n, ids, OrderType.ATTACK_CORE, Vec3.atCenterOf(target.core), -1, target.id, null);
    }
}
