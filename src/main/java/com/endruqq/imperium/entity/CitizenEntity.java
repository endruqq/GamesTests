package com.endruqq.imperium.entity;

import com.endruqq.imperium.entity.goal.CitizenWorkGoal;
import com.endruqq.imperium.game.DialogManager;
import com.endruqq.imperium.game.Game;
import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.GamePhase;
import com.endruqq.imperium.game.NameGen;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.PeopleManager;
import com.endruqq.imperium.game.Person;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class CitizenEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_NATION = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PROFESSION = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SKIN = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FEMALE = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.BOOLEAN);
    /** bit flags: 1 mourning, 2 trapped, 4 married, 8 working */
    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);

    public static final int SKINS = 6;
    public static final int MOURNING = 1, TRAPPED = 2, MARRIED = 4, WORKING = 8;

    private BlockPos home;
    private int personality;
    private String citizenName = "";
    private int session;
    private boolean checked;
    /** cause of death forced by game events (lava, trapped...) */
    private String pendingCause;

    public CitizenEntity(EntityType<? extends CitizenEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation nav) nav.setCanOpenDoors(true);
        setPersistenceRequired();
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_NATION, -1);
        entityData.define(DATA_PROFESSION, 0);
        entityData.define(DATA_SKIN, 0);
        entityData.define(DATA_FEMALE, false);
        entityData.define(DATA_STATE, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 0.75D));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, LivingEntity.class, 10.0F, 0.6D, 0.8D, this::isThreat));
        goalSelector.addGoal(3, new GoHomeGoal(this));
        goalSelector.addGoal(4, new CitizenWorkGoal(this));
        goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.5D));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.45D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    private boolean isThreat(LivingEntity e) {
        if (e instanceof net.minecraft.world.entity.monster.Enemy && !(e instanceof SoldierEntity)) return true;
        GameData d = Game.data(level());
        if (d == null) return false;
        if (e instanceof SoldierEntity s && s.isSurrendered()) return false;
        int other = Allegiance.nationOf(e, d);
        return (e instanceof SoldierEntity || e instanceof Player) && d.atWar(getNationId(), other);
    }

    public void setup(Nation nation, BlockPos home, Profession profession) {
        setNationId(nation == null ? -1 : nation.id);
        boolean female = random.nextBoolean();
        entityData.set(DATA_FEMALE, female);
        entityData.set(DATA_PROFESSION, profession.ordinal());
        entityData.set(DATA_SKIN, random.nextInt(SKINS));
        this.personality = random.nextInt(4);
        setCitizenName(NameGen.person(random, female));
        setHome(home);
        GameData d = Game.data(level());
        if (d != null) {
            session = d.session;
            checked = true;
            if (nation != null) PeopleManager.ensure(d, this);
        }
    }

    public void setCitizenName(String name) {
        this.citizenName = name;
        setCustomName(Component.literal(name));
        setCustomNameVisible(false);
    }

    public void setHome(BlockPos home) {
        this.home = home;
        if (home != null) restrictTo(home, 28);
    }

    @Nullable
    public BlockPos getHome() {
        return home;
    }

    public int getNationId() {
        return entityData.get(DATA_NATION);
    }

    public void setNationId(int id) {
        entityData.set(DATA_NATION, id);
    }

    public Profession getProfession() {
        return Profession.byOrdinal(entityData.get(DATA_PROFESSION));
    }

    public int getSkin() {
        return entityData.get(DATA_SKIN);
    }

    public boolean isFemale() {
        return entityData.get(DATA_FEMALE);
    }

    public int getState() {
        return entityData.get(DATA_STATE);
    }

    public boolean hasState(int flag) {
        return (getState() & flag) != 0;
    }

    private void setState(int flag, boolean on) {
        int s = getState();
        int n = on ? s | flag : s & ~flag;
        if (n != s) entityData.set(DATA_STATE, n);
    }

    public void setWorking(boolean w) {
        setState(WORKING, w);
    }

    /** 0 = cheerful, 1 = grumpy, 2 = dramatic, 3 = philosopher */
    public int getPersonality() {
        return personality;
    }

    public String getCitizenName() {
        return citizenName.isEmpty() ? getName().getString() : citizenName;
    }

    /** Working hours: from morning to dusk. */
    public boolean isWorkTime() {
        long t = level().getDayTime() % 24000L;
        return t >= 600 && t < 11000;
    }

    /** Copies the life record onto the entity (job, name after a wedding, grief...). */
    public void syncFromRecord(Person p, long now) {
        if (p.nation != getNationId()) setNationId(p.nation);
        if (p.job.ordinal() != entityData.get(DATA_PROFESSION)) entityData.set(DATA_PROFESSION, p.job.ordinal());
        if (!p.name.isEmpty() && !p.name.equals(citizenName)) setCitizenName(p.name);
        if (p.home != null && !p.home.equals(home)) setHome(p.home);
        setState(MOURNING, p.mourning(now));
        setState(TRAPPED, p.trapped);
        setState(MARRIED, p.married());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            getNavigation().stop();
            getLookControl().setLookAt(player);
            DialogManager.openCitizenDialog(sp, this);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void aiStep() {
        updateSwingTime();
        super.aiStep();
        if (!level().isClientSide && isAlive() && (tickCount % 20 == 3 || !checked)) {
            GameData d = Game.data(level());
            if (d == null) return;
            if (!checked) {
                checked = true;
                if (d.deceased.contains(getUUID()) || (d.phase == GamePhase.RUNNING && session != d.session && getNationId() >= 0)) {
                    discard();
                    return;
                }
            }
            if (getNationId() >= 0) syncFromRecord(PeopleManager.ensure(d, this), d.gameTick);
        }
    }

    /** Dies of a game event; the cause shows up in logs, gossip and grief. */
    public void dieOf(String cause, DamageSource source) {
        pendingCause = cause;
        if (!hurt(source, 1000F) && isAlive()) {
            setHealth(0);
            die(source);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            GameData d = GameData.get(sl.getServer());
            PeopleManager.onDeath(sl, d, this, source, pendingCause);
        }
    }

    /** Remove without a death (emigration, conscription). */
    public void leave() {
        GameData d = Game.data(level());
        Nation n = d == null ? null : d.nation(getNationId());
        if (d != null) {
            PeopleManager.onLeave(d, getUUID());
            if (n != null) n.citizenCount = Math.max(0, n.citizenCount - 1);
            d.markChanged();
        }
        discard();
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide) UnitRegistry.add(this);
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (!level().isClientSide) UnitRegistry.remove(this);
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Nation", getNationId());
        tag.putInt("Profession", entityData.get(DATA_PROFESSION));
        tag.putInt("Skin", getSkin());
        tag.putBoolean("Female", isFemale());
        tag.putInt("Personality", personality);
        tag.putString("CName", citizenName);
        tag.putInt("Session", session);
        if (home != null) tag.putLong("Home", home.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setNationId(tag.getInt("Nation"));
        entityData.set(DATA_PROFESSION, tag.getInt("Profession"));
        entityData.set(DATA_SKIN, tag.getInt("Skin"));
        entityData.set(DATA_FEMALE, tag.getBoolean("Female"));
        personality = tag.getInt("Personality");
        citizenName = tag.getString("CName");
        session = tag.getInt("Session");
        if (tag.contains("Home")) setHome(BlockPos.of(tag.getLong("Home")));
    }

    /** At night (and in grief) citizens stay at home. */
    static class GoHomeGoal extends Goal {
        private final CitizenEntity citizen;
        private int repath;

        GoHomeGoal(CitizenEntity citizen) {
            this.citizen = citizen;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            BlockPos h = citizen.getHome();
            if (h == null || citizen.hasState(TRAPPED)) return false;
            boolean wantHome = citizen.level().isNight() || citizen.hasState(MOURNING);
            return wantHome && citizen.distanceToSqr(Vec3.atCenterOf(h)) > (citizen.hasState(MOURNING) ? 36 : 9);
        }

        @Override
        public void tick() {
            if (--repath <= 0) {
                BlockPos h = citizen.getHome();
                if (h != null) citizen.getNavigation().moveTo(h.getX() + 0.5, h.getY(), h.getZ() + 0.5, citizen.hasState(MOURNING) ? 0.35D : 0.5D);
                repath = 40;
            }
        }
    }
}
