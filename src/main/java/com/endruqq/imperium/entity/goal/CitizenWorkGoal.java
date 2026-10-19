package com.endruqq.imperium.entity.goal;

import com.endruqq.imperium.entity.CitizenEntity;
import com.endruqq.imperium.entity.Profession;
import com.endruqq.imperium.game.Game;
import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.Gossip;
import com.endruqq.imperium.game.MineWorks;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.PeopleManager;
import com.endruqq.imperium.game.Person;
import com.endruqq.imperium.game.Province;
import com.endruqq.imperium.game.ResourceType;
import com.endruqq.imperium.game.building.Building;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * During the day an employed citizen walks to the workplace and works: woodcutters fell real trees, miners dig galleries,
 * farmers harvest and replant, the others busy themselves at their workstations.
 */
public class CitizenWorkGoal extends Goal {
    private final CitizenEntity c;
    private Building building;
    private Person person;
    private int check;
    private int repath;
    private int timer;
    private BlockPos target;
    private BlockPos breaking;
    private int breakProgress;
    private int breakNeeded;
    private Vec3 lastPos = Vec3.ZERO;
    private int stuck;
    private final List<BlockPos> stations = new ArrayList<>();

    public CitizenWorkGoal(CitizenEntity c) {
        this.c = c;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean valid() {
        if (!(c.level() instanceof ServerLevel level) || !c.isWorkTime()) return false;
        GameData d = Game.data(level);
        if (!Game.running(d)) return false;
        Person p = PeopleManager.get(d, c.getUUID());
        if (p == null || p.workplace < 0 || !p.ableToWork(d.gameTick)) return false;
        Building b = d.buildings.get(p.workplace);
        if (b == null || !b.isActive() || b.nation != c.getNationId() || !level.isLoaded(b.pos)) return false;
        person = p;
        building = b;
        return true;
    }

    @Override
    public boolean canUse() {
        if (--check > 0) return false;
        check = 20 + c.getRandom().nextInt(20);
        return valid();
    }

    @Override
    public boolean canContinueToUse() {
        if (--check > 0) return building != null;
        check = 20;
        return valid();
    }

    @Override
    public void start() {
        c.setItemInHand(InteractionHand.MAIN_HAND, c.getProfession().tool());
        c.clearRestriction();
        c.setWorking(true);
        target = null;
        breaking = null;
        stations.clear();
        stuck = 0;
        timer = 0;
    }

    @Override
    public void stop() {
        if (breaking != null && c.level() instanceof ServerLevel sl) sl.destroyBlockProgress(c.getId(), breaking, -1);
        breaking = null;
        c.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        c.setWorking(false);
        c.setHome(c.getHome());
        c.getNavigation().stop();
        building = null;
        person = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (building == null || person == null || !(c.level() instanceof ServerLevel level)) return;
        GameData d = Game.data(level);
        if (d == null) return;
        switch (c.getProfession()) {
            case MINER -> mine(level, d);
            case WOODCUTTER -> chop(level, d);
            case FARMER -> farm(level, d);
            default -> craft(level, d);
        }
    }

    // ------------------------------------------------------------------ helpers

    /** @return true when standing close to the destination */
    private boolean walkTo(BlockPos dest, double reach) {
        Vec3 v = Vec3.atBottomCenterOf(dest);
        double dist = c.position().distanceToSqr(v.x, c.getY(), v.z);
        if (dist < reach * reach && Math.abs(c.getY() - dest.getY()) < 2.5) {
            c.getNavigation().stop();
            stuck = 0;
            return true;
        }
        if (--repath <= 0 || c.getNavigation().isDone()) {
            c.getNavigation().moveTo(v.x, v.y, v.z, 0.6D);
            repath = 30;
        }
        if (c.tickCount % 40 == 0) {
            if (c.position().distanceToSqr(lastPos) < 1.0) stuck += 40;
            else stuck = 0;
            lastPos = c.position();
        }
        return false;
    }

    private void swing(ServerLevel level, BlockPos at, SoundEvent sound, float volume) {
        c.getLookControl().setLookAt(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
        c.swing(InteractionHand.MAIN_HAND);
        if (sound != null) level.playSound(null, at, sound, SoundSource.NEUTRAL, volume, 0.9F + c.getRandom().nextFloat() * 0.2F);
    }

    private Nation nation(GameData d) {
        return d.nation(c.getNationId());
    }

    // ------------------------------------------------------------------ miner

    private int tunnelIndex(GameData d) {
        List<Person> crew = d.workersAt(building.id);
        int i = crew.indexOf(person);
        return Math.max(0, Math.min(2, i));
    }

    private void mine(ServerLevel level, GameData d) {
        Building.Tunnel t = MineWorks.tunnel(level, building, tunnelIndex(d));
        BlockPos face = t.cursor;
        if (!walkTo(face, 1.6)) {
            if (stuck > 600) {
                stuck = 0;
                boolean underground = c.getBlockY() < level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getBlockX(), c.getBlockZ()) - 4;
                if (underground && c.distanceToSqr(Vec3.atCenterOf(face)) > 36) {
                    Province prov = d.provinceAt(c.blockPosition());
                    PeopleManager.trap(level, d, person, c, List.of(), prov == null ? "-" : prov.name);
                } else if (c.distanceToSqr(Vec3.atCenterOf(face)) < 80 * 80) {
                    c.teleportTo(face.getX() + 0.5, face.getY(), face.getZ() + 0.5);
                }
            }
            return;
        }
        if (breaking == null) {
            BlockPos next = null;
            for (BlockPos p : MineWorks.stepBlocks(t)) {
                if (!MineWorks.passable(level, p)) {
                    next = p;
                    break;
                }
            }
            if (next == null) {
                stepDone(level, d, t);
                return;
            }
            if (!MineWorks.diggable(level, d, building, next)) {
                MineWorks.turn(level, t);
                return;
            }
            int hz = MineWorks.hazard(level, next);
            if (hz == 2) {
                Nation n = nation(d);
                double risk = n != null && n.has(com.endruqq.imperium.game.tech.Tech.INDUSTRY) ? 0.12 : 0.22;
                if (c.getRandom().nextDouble() < risk) {
                    MineWorks.lavaAccident(level, c, next);
                    return;
                }
                MineWorks.seal(level, next);
                if (n != null && d.gameTick - person.lastAccidentTick > 20 * 60 * 5) {
                    person.lastAccidentTick = d.gameTick;
                    Province prov = d.provinceAt(c.blockPosition());
                    n.addGossip(Gossip.of("lava", person.name, person.spouseName, prov == null ? "-" : prov.name, d.gameTick));
                }
            } else if (hz == 1) {
                MineWorks.seal(level, next);
            }
            breaking = next;
            breakProgress = 0;
            breakNeeded = MineWorks.breakTicks(level, next);
        }
        breakProgress++;
        c.getLookControl().setLookAt(breaking.getX() + 0.5, breaking.getY() + 0.5, breaking.getZ() + 0.5);
        if (breakProgress % 6 == 0) {
            c.swing(InteractionHand.MAIN_HAND);
            BlockState s = level.getBlockState(breaking);
            level.playSound(null, breaking, s.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.5F, 0.8F);
        }
        level.destroyBlockProgress(c.getId(), breaking, Math.min(9, breakProgress * 10 / Math.max(1, breakNeeded)));
        if (breakProgress >= breakNeeded) {
            BlockState s = level.getBlockState(breaking);
            level.destroyBlockProgress(c.getId(), breaking, -1);
            MineWorks.reward(level, d, nation(d), s, breaking);
            level.destroyBlock(breaking, false, c);
            breaking = null;
        }
    }

    private void stepDone(ServerLevel level, GameData d, Building.Tunnel t) {
        BlockPos f = MineWorks.feet(t);
        boolean cavern = MineWorks.passable(level, f.below()) && MineWorks.passable(level, f.below(2)) && MineWorks.passable(level, f.below(3));
        if (cavern && c.getRandom().nextDouble() < 0.15) {
            // the floor gives way: the miner tumbles into a cavern and may not find the way back
            c.teleportTo(f.getX() + 0.5, f.getY(), f.getZ() + 0.5);
            c.getNavigation().stop();
            MineWorks.turn(level, t);
            stuck = 0;
            return;
        }
        MineWorks.advance(level, d, building, t);
        Nation n = nation(d);
        if (c.getRandom().nextDouble() < MineWorks.caveInChance(n, t) && d.gameTick - person.lastAccidentTick > 20 * 60 * 15
                && c.blockPosition().closerThan(t.cursor, 2.5)) {
            List<BlockPos> rubble = MineWorks.collapse(level, t);
            if (!rubble.isEmpty()) {
                Province prov = d.provinceAt(c.blockPosition());
                PeopleManager.trap(level, d, person, c, rubble, prov == null ? "-" : prov.name);
            }
        }
        d.markChanged();
    }

    // ------------------------------------------------------------------ woodcutter

    private void chop(ServerLevel level, GameData d) {
        if (timer > 0) {
            timer--;
            if (timer % 40 == 0 && !walkTo(building.pos, 3)) return;
            return;
        }
        if (target == null || !level.getBlockState(target).is(BlockTags.LOGS)) {
            target = findTree(level, d);
            breakProgress = 0;
            if (target == null) {
                timer = 200;
                return;
            }
        }
        if (!walkTo(target, 2.4)) {
            if (stuck > 400) {
                target = null;
                stuck = 0;
            }
            return;
        }
        breakProgress++;
        if (breakProgress % 10 == 0) swing(level, target, SoundEvents.WOOD_HIT, 0.8F);
        level.destroyBlockProgress(c.getId(), target, Math.min(9, breakProgress / 8));
        if (breakProgress >= 80) {
            level.destroyBlockProgress(c.getId(), target, -1);
            fell(level, d, target);
            target = null;
            timer = 160;
        }
    }

    private BlockPos findTree(ServerLevel level, GameData d) {
        BlockPos center = building.pos;
        for (int i = 0; i < 24; i++) {
            int x = center.getX() + c.getRandom().nextInt(49) - 24;
            int z = center.getZ() + c.getRandom().nextInt(49) - 24;
            if (d.nationIdAt(x + 0.5, z + 0.5) != building.nation) continue;
            if (!level.isLoaded(new BlockPos(x, center.getY(), z))) continue;
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos p = new BlockPos(x, top, z);
            if (!level.getBlockState(p).is(BlockTags.LOGS)) continue;
            if (d.buildingContaining(p) != null) continue;
            boolean leaves = false;
            for (BlockPos q : BlockPos.betweenClosed(p.offset(-2, -1, -2), p.offset(2, 2, 2))) {
                if (level.getBlockState(q).is(BlockTags.LEAVES)) {
                    leaves = true;
                    break;
                }
            }
            if (!leaves) continue;
            while (level.getBlockState(p.below()).is(BlockTags.LOGS) && p.getY() > top - 30) p = p.below();
            if (d.buildingContaining(p) != null) continue;
            return p.immutable();
        }
        return null;
    }

    private void fell(ServerLevel level, GameData d, BlockPos base) {
        Block log = level.getBlockState(base).getBlock();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(base);
        List<BlockPos> logs = new ArrayList<>();
        while (!queue.isEmpty() && logs.size() < 64) {
            BlockPos p = queue.poll();
            if (!seen.add(p)) continue;
            if (!level.getBlockState(p).is(BlockTags.LOGS)) continue;
            if (Math.abs(p.getX() - base.getX()) > 5 || Math.abs(p.getZ() - base.getZ()) > 5 || p.getY() < base.getY()) continue;
            logs.add(p);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx != 0 || dy != 0 || dz != 0) queue.add(p.offset(dx, dy, dz));
                    }
                }
            }
        }
        for (BlockPos p : logs) level.destroyBlock(p, false, c);
        Nation n = nation(d);
        if (n != null) n.stock.add(ResourceType.WOOD, Math.min(40, logs.size() * 2));
        BlockState ground = level.getBlockState(base.below());
        if (ground.is(BlockTags.DIRT) && level.getBlockState(base).isAir()) {
            level.setBlock(base, sapling(log).defaultBlockState(), 3);
        }
        level.playSound(null, base, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.2F, 0.6F);
        d.markChanged();
    }

    private static Block sapling(Block log) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(log);
        String path = id.getPath().replace("stripped_", "").replace("_log", "_sapling").replace("_wood", "_sapling");
        if (path.startsWith("mangrove")) return Blocks.MANGROVE_PROPAGULE;
        Block b = BuiltInRegistries.BLOCK.get(new ResourceLocation(id.getNamespace(), path));
        return b == Blocks.AIR ? Blocks.OAK_SAPLING : b;
    }

    // ------------------------------------------------------------------ farmer

    private void farm(ServerLevel level, GameData d) {
        if (timer > 0) {
            timer--;
            return;
        }
        if (target == null) {
            target = findCrop(level);
            if (target == null) {
                if (!walkTo(building.pos, 3)) return;
                timer = 100;
                // tend the field: help a random crop grow
                BlockPos p = randomInPlot(0);
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof CropBlock crop && !crop.isMaxAge(s)) {
                    crop.growCrops(level, p, s);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0);
                    swing(level, p, SoundEvents.CROP_PLANTED, 0.6F);
                }
                return;
            }
        }
        if (!walkTo(target, 1.8)) {
            if (stuck > 300) {
                target = null;
                stuck = 0;
            }
            return;
        }
        BlockState s = level.getBlockState(target);
        if (s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s)) {
            swing(level, target, SoundEvents.CROP_BREAK, 0.8F);
            level.destroyBlock(target, false, c);
            level.setBlock(target, crop.getStateForAge(0), 3);
            Nation n = nation(d);
            if (n != null) n.stock.add(ResourceType.FOOD, 1);
            d.markChanged();
        }
        target = null;
        timer = 30;
    }

    private BlockPos randomInPlot(int dy) {
        int r = building.radius();
        return building.pos.offset(c.getRandom().nextInt(r * 2 + 1) - r, dy, c.getRandom().nextInt(r * 2 + 1) - r);
    }

    private BlockPos findCrop(ServerLevel level) {
        for (int i = 0; i < 12; i++) {
            BlockPos p = randomInPlot(0);
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos q = p.above(dy);
                BlockState s = level.getBlockState(q);
                if (s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s)) return q;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ everybody else

    private void scanStations(ServerLevel level) {
        stations.clear();
        for (BlockPos p : BlockPos.betweenClosed(building.minX(), building.minY(), building.minZ(), building.maxX(), building.maxY(), building.maxZ())) {
            BlockState s = level.getBlockState(p);
            if (isStation(s)) stations.add(p.immutable());
            if (stations.size() > 40) break;
        }
    }

    private static boolean isStation(BlockState s) {
        return s.is(Blocks.FURNACE) || s.is(Blocks.BLAST_FURNACE) || s.is(Blocks.SMOKER) || s.is(BlockTags.ANVIL) || s.is(Blocks.CRAFTING_TABLE)
                || s.is(Blocks.LECTERN) || s.is(Blocks.BOOKSHELF) || s.is(Blocks.BARREL) || s.is(Blocks.BREWING_STAND) || s.is(BlockTags.CAULDRONS)
                || s.is(Blocks.BELL) || s.is(Blocks.SMITHING_TABLE) || s.is(Blocks.GRINDSTONE) || s.is(Blocks.LOOM) || s.is(Blocks.STONECUTTER)
                || s.is(Blocks.CARTOGRAPHY_TABLE) || s.is(Blocks.PISTON) || s.is(Blocks.CHEST) || s.is(BlockTags.BEDS) || s.is(Blocks.GOLD_BLOCK)
                || s.is(BlockTags.CANDLES) || s.is(Blocks.COMPOSTER) || s.is(Blocks.FLETCHING_TABLE);
    }

    private void craft(ServerLevel level, GameData d) {
        if (stations.isEmpty()) {
            scanStations(level);
            if (stations.isEmpty()) {
                walkTo(building.pos, 3);
                return;
            }
        }
        if (target == null) target = stations.get(c.getRandom().nextInt(stations.size()));
        if (!walkTo(target, 2.2)) {
            if (stuck > 300) {
                target = null;
                stuck = 0;
            }
            return;
        }
        timer++;
        if (timer % 25 == 0) {
            BlockState s = level.getBlockState(target);
            Profession prof = c.getProfession();
            SoundEvent sound = null;
            ParticleOptions particle = null;
            if (s.is(BlockTags.ANVIL)) sound = SoundEvents.ANVIL_USE;
            else if (s.is(Blocks.FURNACE) || s.is(Blocks.BLAST_FURNACE) || s.is(Blocks.SMOKER)) {
                sound = SoundEvents.FURNACE_FIRE_CRACKLE;
                particle = ParticleTypes.LARGE_SMOKE;
            } else if (s.is(Blocks.PISTON)) sound = SoundEvents.PISTON_EXTEND;
            else if (s.is(Blocks.LECTERN) || s.is(Blocks.BOOKSHELF)) {
                sound = SoundEvents.BOOK_PAGE_TURN;
                particle = ParticleTypes.ENCHANT;
            } else if (s.is(Blocks.BREWING_STAND) || s.is(BlockTags.CAULDRONS)) {
                sound = SoundEvents.BREWING_STAND_BREW;
                particle = ParticleTypes.EFFECT;
            } else if (s.is(Blocks.GRINDSTONE)) sound = SoundEvents.GRINDSTONE_USE;
            else if (s.is(Blocks.SMITHING_TABLE)) sound = SoundEvents.SMITHING_TABLE_USE;
            else if (s.is(Blocks.CRAFTING_TABLE) || s.is(Blocks.LOOM) || s.is(Blocks.CARTOGRAPHY_TABLE)) sound = SoundEvents.VILLAGER_WORK_FLETCHER;
            else if (s.is(Blocks.BARREL) || s.is(Blocks.CHEST)) sound = SoundEvents.BARREL_OPEN;
            if (prof == Profession.PRIEST || prof == Profession.DOCTOR) particle = ParticleTypes.HAPPY_VILLAGER;
            if (prof == Profession.INNKEEPER) particle = ParticleTypes.NOTE;
            swing(level, target, sound, 0.35F);
            if (particle != null) {
                level.sendParticles(particle, target.getX() + 0.5, target.getY() + 1.1, target.getZ() + 0.5, 3, 0.25, 0.2, 0.25, 0.01);
            }
        }
        if (timer > 160 + c.getRandom().nextInt(200)) {
            timer = 0;
            target = null;
        }
    }
}
