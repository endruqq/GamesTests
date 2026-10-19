package com.endruqq.imperium.game;

import com.endruqq.imperium.entity.CitizenEntity;
import com.endruqq.imperium.entity.Profession;
import com.endruqq.imperium.entity.UnitRegistry;
import com.endruqq.imperium.game.building.Building;
import com.endruqq.imperium.game.building.BuildingType;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.game.tech.Tech;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The lives of citizens: jobs, weddings, children, grief, accidents underground. Works on {@link Person} records so that it
 * keeps going while citizens are unloaded.
 */
public final class PeopleManager {
    /** how long a widow(er) mourns */
    public static final long MOURNING_TICKS = 20L * 60 * 12;
    /** how long a miner survives trapped behind rubble */
    public static final long TRAPPED_TICKS = 20L * 60 * 8;
    private static final int DECEASED_LIMIT = 400;

    private PeopleManager() {}

    // ------------------------------------------------------------------ records

    public static Person get(GameData d, UUID id) {
        return d == null || id == null ? null : d.people.get(id);
    }

    /** The record of a citizen, created on first use. */
    public static Person ensure(GameData d, CitizenEntity c) {
        Person p = d.people.get(c.getUUID());
        if (p == null) {
            p = new Person();
            p.id = c.getUUID();
            p.name = c.getCitizenName();
            p.female = c.isFemale();
            p.nation = c.getNationId();
            p.personality = c.getPersonality();
            p.home = c.getHome();
            p.bornTick = d.gameTick;
            d.people.put(p.id, p);
        }
        return p;
    }

    public static CitizenEntity entity(Person p) {
        return p == null ? null : UnitRegistry.citizen(p.id);
    }

    private static void bury(GameData d, UUID id) {
        d.deceased.add(id);
        Iterator<UUID> it = d.deceased.iterator();
        while (d.deceased.size() > DECEASED_LIMIT && it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    // ------------------------------------------------------------------ death

    public static String causeOf(DamageSource src, Person p) {
        if (p != null && p.trapped) return "trapped";
        if (src.is(DamageTypes.LAVA) || src.is(DamageTypeTags.IS_FIRE)) return "lava";
        if (src.is(DamageTypeTags.IS_FALL)) return "fall";
        if (src.is(DamageTypeTags.IS_DROWNING)) return "drowned";
        if (src.is(DamageTypes.STARVE)) return "starvation";
        if (src.is(DamageTypes.IN_WALL)) return "accident";
        if (src.getEntity() != null) return "killed";
        return "other";
    }

    /** Called by a dying citizen entity. */
    public static void onDeath(ServerLevel level, GameData d, CitizenEntity c, DamageSource src, String forcedCause) {
        Person p = d.people.remove(c.getUUID());
        String cause = forcedCause != null ? forcedCause : causeOf(src, p);
        Province prov = d.provinceAt(c.blockPosition());
        Nation n = d.nation(c.getNationId());
        bury(d, c.getUUID());
        level.destroyBlockProgress(c.getId(), c.blockPosition(), -1);
        afterDeath(level, d, n, p, p == null ? c.getCitizenName() : p.name, cause, prov == null ? "-" : prov.name);
    }

    /** A citizen dies while nobody is around (unloaded chunk). */
    public static void dieUnseen(ServerLevel level, GameData d, Person p, String cause) {
        d.people.remove(p.id);
        bury(d, p.id);
        Province prov = p.home == null ? null : d.provinceAt(p.home);
        afterDeath(level, d, d.nation(p.nation), p, p.name, cause, prov == null ? "-" : prov.name);
    }

    private static void afterDeath(ServerLevel level, GameData d, Nation n, Person p, String name, String cause, String place) {
        if (n == null) return;
        long now = d.gameTick;
        n.population = Math.max(1, n.population - 1);
        n.citizenCount = Math.max(0, n.citizenCount - 1);
        n.happiness = Math.max(0, n.happiness - 2);
        n.addLog(Component.translatable("imperium.log.died." + cause, name));
        String widow = "";
        if (p != null && p.spouse != null) {
            Person s = d.people.get(p.spouse);
            if (s != null) {
                s.spouse = null;
                s.spouseName = name;
                s.mourningUntil = now + MOURNING_TICKS;
                s.mourningFor = name;
                s.mourningCause = cause;
                widow = s.name;
                n.addLog(Component.translatable(s.female ? "imperium.log.widow" : "imperium.log.widower", s.name, name));
                CitizenEntity se = entity(s);
                if (se != null) se.syncFromRecord(s, now);
            }
        }
        n.addGossip(Gossip.of("death." + cause, name, widow, place, now));
        boolean dramatic = cause.equals("lava") || cause.equals("trapped") || cause.equals("accident");
        Notify.nation(level.getServer(), d, n.id, Component.translatable("imperium.msg.died." + cause, name, place)
                .withStyle(dramatic ? ChatFormatting.RED : ChatFormatting.GRAY), dramatic);
        d.markChanged();
    }

    /** Removes a citizen without a death (emigration, conscription, becoming a soldier). */
    public static void onLeave(GameData d, UUID id) {
        Person p = d.people.remove(id);
        if (p == null) return;
        if (p.spouse != null) {
            Person s = d.people.get(p.spouse);
            if (s != null) {
                s.spouse = null;
                s.spouseName = "";
            }
        }
    }

    // ------------------------------------------------------------------ jobs

    public static int freeSlots(GameData d, Building b) {
        BuildingType t = b.type();
        if (t == null || !t.isWorkplace()) return 0;
        int used = 0;
        for (Person p : d.people.values()) if (p.workplace == b.id) used++;
        return Math.max(0, t.workerSlots - used);
    }

    /** @return null on success, else the reason */
    public static Component assign(GameData d, Person p, Building b) {
        BuildingType t = b.type();
        if (t == null || !t.isWorkplace()) return Component.translatable("imperium.err.not_workplace");
        if (b.nation != p.nation) return Component.translatable("imperium.err.not_own_territory");
        if (!b.isActive()) return Component.translatable("imperium.err.building_inactive");
        if (p.trapped) return Component.translatable("imperium.err.person_trapped");
        if (p.mourning(d.gameTick)) return Component.translatable("imperium.err.person_mourning");
        if (p.workplace == b.id) return null;
        if (freeSlots(d, b) <= 0) return Component.translatable("imperium.err.no_free_slots");
        p.workplace = b.id;
        p.job = t.profession;
        recountWorkers(d);
        CitizenEntity c = entity(p);
        if (c != null) c.syncFromRecord(p, d.gameTick);
        d.markChanged();
        return null;
    }

    public static void unassign(GameData d, Person p) {
        p.workplace = -1;
        p.job = Profession.PEASANT;
        CitizenEntity c = entity(p);
        if (c != null) c.syncFromRecord(p, d.gameTick);
    }

    /** The best building of a type for a person: free slots, nearest to home. */
    public static Building findWorkplace(GameData d, Person p, BuildingType type) {
        Building best = null;
        double bestScore = Double.MAX_VALUE;
        for (Building b : d.buildings.values()) {
            if (b.nation != p.nation || !b.isActive() || (type != null && !b.typeId.equals(type.id))) continue;
            BuildingType t = b.type();
            if (t == null || !t.isWorkplace() || freeSlots(d, b) <= 0) continue;
            double score = p.home == null ? 0 : p.home.distSqr(b.pos);
            score += (b.workers / (double) Math.max(1, t.workerSlots)) * 10000;
            if (score < bestScore) {
                bestScore = score;
                best = b;
            }
        }
        return best;
    }

    /** Hire one unemployed citizen for a building type. @return null on success, else the reason */
    public static Component hire(GameData d, Nation n, BuildingType type) {
        List<Person> idle = unemployed(d, n);
        if (idle.isEmpty()) return Component.translatable("imperium.err.no_unemployed");
        boolean anyBuilding = false;
        for (Building b : d.buildings.values()) if (b.nation == n.id && b.isActive() && b.typeId.equals(type.id)) anyBuilding = true;
        if (!anyBuilding) return Component.translatable("imperium.err.needs_building", type.name());
        for (Person p : idle) {
            Building b = findWorkplace(d, p, type);
            if (b != null) return assign(d, p, b);
        }
        return Component.translatable("imperium.err.no_free_slots");
    }

    public static boolean fire(GameData d, Nation n, BuildingType type) {
        Person last = null;
        for (Person p : d.people.values()) {
            Building b = p.workplace < 0 ? null : d.buildings.get(p.workplace);
            if (p.nation == n.id && b != null && b.typeId.equals(type.id)) last = p;
        }
        if (last == null) return false;
        unassign(d, last);
        recountWorkers(d);
        d.markChanged();
        return true;
    }

    public static List<Person> unemployed(GameData d, Nation n) {
        List<Person> l = new ArrayList<>();
        for (Person p : d.people.values()) {
            if (p.nation == n.id && p.workplace < 0 && p.ableToWork(d.gameTick)) l.add(p);
        }
        return l;
    }

    /** Gives every unemployed citizen the most needed job. @return how many got work */
    public static int autoAssign(GameData d, Nation n) {
        int hired = 0;
        List<Person> idle = unemployed(d, n);
        boolean hungry = n.lastDelta.get(ResourceType.FOOD) < 0;
        for (Person p : idle) {
            Building b = hungry ? findWorkplace(d, p, Buildings.FARM) : null;
            if (b == null) b = findWorkplace(d, p, null);
            if (b == null) break;
            if (assign(d, p, b) == null) hired++;
        }
        return hired;
    }

    public static void recountWorkers(GameData d) {
        for (Building b : d.buildings.values()) b.workers = 0;
        for (Person p : d.people.values()) {
            if (p.workplace < 0) continue;
            Building b = d.buildings.get(p.workplace);
            BuildingType t = b == null ? null : b.type();
            if (b == null || t == null || !t.isWorkplace() || b.nation != p.nation || b.workers >= t.workerSlots) {
                unassign(d, p);
                continue;
            }
            p.job = t.profession;
            b.workers++;
        }
    }

    /** 0..1: how well a building is staffed, counting only people who can work today. */
    public static double staffing(GameData d, Building b) {
        BuildingType t = b.type();
        if (t == null || !t.isWorkplace()) return 1.0;
        int able = 0;
        for (Person p : d.people.values()) if (p.workplace == b.id && p.ableToWork(d.gameTick)) able++;
        double share = Math.min(1.0, able / (double) t.workerSlots);
        return t.unstaffedOutput + (1 - t.unstaffedOutput) * share;
    }

    /** Sum of staffing of all active buildings of a type: "how many fully working buildings". */
    public static double effective(GameData d, int nationId, String typeId) {
        double e = 0;
        for (Building b : d.buildings.values()) {
            if (b.nation == nationId && b.isActive() && b.typeId.equals(typeId)) e += staffing(d, b);
        }
        return e;
    }

    // ------------------------------------------------------------------ citizen limit

    /** How many visible citizens a nation may have: grows with time and technologies. */
    public static int citizenCap(GameData d, Nation n) {
        return Math.min(n.population, citizenCapRaw(d, n));
    }

    public static int citizenCapRaw(GameData d, Nation n) {
        int cap = 4 + timeBonus(d) + techBonus(n) + buildingBonus(d, n);
        return Math.min(com.endruqq.imperium.ImperiumConfig.MAX_CITIZENS_PER_NATION.get(), cap);
    }

    public static int timeBonus(GameData d) {
        long minutes = Math.max(0, d.gameTick - d.startTick) / 1200;
        return (int) Math.min(16, minutes / 3);
    }

    public static int techBonus(Nation n) {
        int b = 0;
        if (n.has(Tech.AGRICULTURE)) b += 1;
        if (n.has(Tech.ARCHITECTURE)) b += 2;
        if (n.has(Tech.MEDICINE)) b += 2;
        if (n.has(Tech.INDUSTRY)) b += 2;
        if (n.has(Tech.ADMINISTRATION)) b += 2;
        if (n.has(Tech.ENLIGHTENMENT)) b += 3;
        if (n.has(Tech.MASS_PRODUCTION)) b += 2;
        if (n.has(Tech.URBANIZATION)) b += 6;
        return b;
    }

    public static int buildingBonus(GameData d, Nation n) {
        int h = 0;
        for (Building b : d.buildings.values()) {
            if (b.nation == n.id && b.isActive() && b.type() != null && b.type().isHousing()) h++;
        }
        return Math.min(10, h);
    }

    // ------------------------------------------------------------------ economy cycle

    public static void cycle(ServerLevel level, GameData d, Nation n) {
        RandomSource rnd = level.random;
        long now = d.gameTick;
        List<Person> list = d.peopleOf(n.id);
        for (Person p : list) {
            if (p.mourningUntil > 0 && now >= p.mourningUntil) {
                p.mourningUntil = 0;
                CitizenEntity c = entity(p);
                if (c != null) c.syncFromRecord(p, now);
            }
        }
        marriages(level, d, n, list, rnd);
        births(d, n, list, rnd);
        recountWorkers(d);
        if (n.bot || n.autoJobs) autoAssign(d, n);
        stats(d, n);
    }

    public static void stats(GameData d, Nation n) {
        int employed = 0, idle = 0, married = 0, mourning = 0, trapped = 0, count = 0;
        for (Person p : d.people.values()) {
            if (p.nation != n.id) continue;
            count++;
            if (p.workplace >= 0) employed++;
            else idle++;
            if (p.married()) married++;
            if (p.mourning(d.gameTick)) mourning++;
            if (p.trapped) trapped++;
        }
        n.citizenCount = count;
        n.employed = employed;
        n.unemployed = idle;
        n.marriedCount = married;
        n.mourningCount = mourning;
        n.trappedCount = trapped;
        n.citizenCap = citizenCapRaw(d, n);
    }

    private static void marriages(ServerLevel level, GameData d, Nation n, List<Person> list, RandomSource rnd) {
        double chance = 0.12 + 0.05 * Math.min(2, d.countActive(n.id, Buildings.TAVERN.id)) + 0.05 * Math.min(2, d.countActive(n.id, Buildings.TEMPLE.id));
        if (n.happiness < 40) chance *= 0.3;
        if (n.famine) chance *= 0.2;
        if (rnd.nextDouble() >= chance) return;
        List<Person> men = new ArrayList<>(), women = new ArrayList<>();
        for (Person p : list) {
            if (p.married() || p.trapped || p.mourning(d.gameTick)) continue;
            if (d.gameTick - p.bornTick < 20 * 60 * 2) continue;
            (p.female ? women : men).add(p);
        }
        if (men.isEmpty() || women.isEmpty()) return;
        Person m = men.get(rnd.nextInt(men.size()));
        Person w = women.get(rnd.nextInt(women.size()));
        marry(level, d, n, m, w);
    }

    public static void marry(ServerLevel level, GameData d, Nation n, Person m, Person w) {
        long now = d.gameTick;
        String maiden = w.name;
        m.spouse = w.id;
        w.spouse = m.id;
        m.marriedSince = now;
        w.marriedSince = now;
        if (!m.surname().isEmpty()) w.name = w.firstName() + " " + feminine(m.surname());
        m.spouseName = w.name;
        w.spouseName = m.name;
        if (m.home != null) w.home = m.home;
        n.addLog(Component.translatable("imperium.log.wedding", m.name, maiden));
        n.addGossip(Gossip.of("marriage", m.name, w.name, "", now));
        Notify.nation(level.getServer(), d, n.id, Component.translatable("imperium.msg.wedding", m.name, w.name).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        for (Person p : new Person[]{m, w}) {
            CitizenEntity c = entity(p);
            if (c != null) {
                c.syncFromRecord(p, now);
                level.sendParticles(ParticleTypes.HEART, c.getX(), c.getY() + 2.1, c.getZ(), 6, 0.4, 0.3, 0.4, 0.05);
            }
        }
        d.markChanged();
    }

    /** Kowalski -> Kowalska */
    public static String feminine(String surname) {
        if (surname.endsWith("ski") || surname.endsWith("cki") || surname.endsWith("dzki")) return surname.substring(0, surname.length() - 1) + "a";
        return surname;
    }

    private static void births(GameData d, Nation n, List<Person> list, RandomSource rnd) {
        if (n.famine || n.population >= n.housing) return;
        for (Person p : list) {
            if (p.female || !p.married()) continue;
            Person w = d.people.get(p.spouse);
            if (w == null || p.mourning(d.gameTick) || w.trapped || p.trapped) continue;
            if (rnd.nextDouble() < 0.04) {
                n.population++;
                p.children++;
                w.children++;
                n.addLog(Component.translatable("imperium.log.birth", p.name, w.name));
                n.addGossip(Gossip.of("birth", p.name, w.name, "", d.gameTick));
                return;
            }
        }
    }

    /** Number of married people whose spouse is alive and well: they eat less. */
    public static int happyCouples(GameData d, Nation n) {
        int c = 0;
        for (Person p : d.people.values()) {
            if (p.nation == n.id && p.married() && !p.mourning(d.gameTick) && !p.trapped) c++;
        }
        return c;
    }

    // ------------------------------------------------------------------ per second: trapped miners

    public static void tick(ServerLevel level, GameData d) {
        long now = d.gameTick;
        for (Person p : new ArrayList<>(d.people.values())) {
            if (!p.trapped) continue;
            CitizenEntity c = entity(p);
            if (c != null && (p.trapKind == 0 ? rubbleCleared(level, p) : found(level, d, p, c))) {
                if (p.trapKind == 1 && p.home != null) c.teleportTo(p.home.getX() + 0.5, p.home.getY(), p.home.getZ() + 0.5);
                rescue(level, d, p, c);
                continue;
            }
            long left = TRAPPED_TICKS - (now - p.trappedSince);
            if (left <= 0) {
                if (c != null) c.dieOf("trapped", level.damageSources().starve());
                else dieUnseen(level, d, p, "trapped");
                continue;
            }
            if (c != null && (now - p.trappedSince) % 300 < 20) {
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, c.getX(), c.getY() + 2.0, c.getZ(), 1, 0.2, 0.1, 0.2, 0);
                level.playSound(null, c.blockPosition(), SoundEvents.STONE_HIT, SoundSource.NEUTRAL, 1.0F, 0.7F);
            }
            if ((now - p.trappedSince) % 2400 < 20 && now > p.trappedSince + 40) {
                BlockPos at = c != null ? c.blockPosition() : p.rubble.isEmpty() ? p.home : p.rubble.get(0);
                if (at != null) {
                    Notify.nation(level.getServer(), d, p.nation, Component.translatable("imperium.msg.still_trapped", p.name,
                            at.getX(), at.getY(), at.getZ(), UiTime.format(left / 20)).withStyle(ChatFormatting.RED), true);
                }
            }
        }
    }

    /** a member of the nation came close to a citizen lost underground */
    private static boolean found(ServerLevel level, GameData d, Person p, CitizenEntity c) {
        for (net.minecraft.server.level.ServerPlayer pl : level.getServer().getPlayerList().getPlayers()) {
            if (pl.level() == level && pl.distanceToSqr(c) < 16 && d.nationIdOf(pl.getUUID()) == p.nation) return true;
        }
        return false;
    }

    private static boolean rubbleCleared(ServerLevel level, Person p) {
        if (p.rubble.isEmpty()) return true;
        for (BlockPos pos : p.rubble) {
            if (!level.isLoaded(pos)) return false;
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return false;
        }
        return true;
    }

    public static void trap(ServerLevel level, GameData d, Person p, CitizenEntity c, List<BlockPos> rubble, String place) {
        p.trapped = true;
        p.trapKind = rubble.isEmpty() ? 1 : 0;
        p.trappedSince = d.gameTick;
        p.lastAccidentTick = d.gameTick;
        p.rubble.clear();
        p.rubble.addAll(rubble);
        Nation n = d.nation(p.nation);
        if (n != null) {
            n.addLog(Component.translatable(p.trapKind == 0 ? "imperium.log.cave_in" : "imperium.log.lost", p.name, place));
            n.addGossip(Gossip.of(p.trapKind == 0 ? "trapped" : "lost", p.name, p.spouseName, place, d.gameTick));
            BlockPos at = c.blockPosition();
            Notify.nation(level.getServer(), d, n.id, Component.translatable(p.trapKind == 0 ? "imperium.msg.cave_in" : "imperium.msg.lost", p.name, place,
                    at.getX(), at.getY(), at.getZ(), TRAPPED_TICKS / 1200).withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
        }
        c.syncFromRecord(p, d.gameTick);
        d.markChanged();
    }

    private static void rescue(ServerLevel level, GameData d, Person p, CitizenEntity c) {
        p.trapped = false;
        p.rubble.clear();
        Nation n = d.nation(p.nation);
        if (n != null) {
            n.happiness = Math.min(100, n.happiness + 3);
            n.addLog(Component.translatable("imperium.log.rescued", p.name));
            n.addGossip(Gossip.of("rescued", p.name, p.spouseName, "", d.gameTick));
            Notify.nation(level.getServer(), d, n.id, Component.translatable("imperium.msg.rescued", p.name).withStyle(ChatFormatting.GREEN), true);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.getX(), c.getY() + 1.5, c.getZ(), 12, 0.4, 0.5, 0.4, 0.1);
        c.syncFromRecord(p, d.gameTick);
        d.markChanged();
    }

    // ------------------------------------------------------------------ client list

    /** The citizens of a nation, for the People tab. */
    public static CompoundTag listFor(GameData d, Nation n) {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        long now = d.gameTick;
        List<Person> list = d.peopleOf(n.id);
        list.sort(Comparator.comparing((Person p) -> p.workplace < 0 ? 1 : 0).thenComparing(p -> p.job.ordinal()).thenComparing(p -> p.name));
        for (Person p : list) {
            CompoundTag e = new CompoundTag();
            e.putUUID("id", p.id);
            e.putString("name", p.name);
            e.putBoolean("female", p.female);
            e.putInt("job", p.job.ordinal());
            Building b = p.workplace < 0 ? null : d.buildings.get(p.workplace);
            e.putString("work", b == null ? "" : b.typeId);
            e.putString("spouse", p.married() ? p.spouseName : "");
            e.putInt("children", p.children);
            e.putLong("mourning", p.mourning(now) ? (p.mourningUntil - now) / 20 : 0);
            e.putString("mourningFor", p.mourningFor);
            e.putLong("trapped", p.trapped ? Math.max(0, TRAPPED_TICKS - (now - p.trappedSince)) / 20 : -1);
            l.add(e);
        }
        t.put("people", l);
        return t;
    }

    /** mm:ss without client classes */
    public static final class UiTime {
        private UiTime() {}

        public static String format(long seconds) {
            return String.format("%d:%02d", seconds / 60, seconds % 60);
        }
    }
}
