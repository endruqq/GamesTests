package com.endruqq.imperium.entity;

import com.endruqq.imperium.entity.goal.SoldierAttackCoreGoal;
import com.endruqq.imperium.entity.goal.SoldierDefendGoal;
import com.endruqq.imperium.entity.goal.SoldierMeleeGoal;
import com.endruqq.imperium.entity.goal.SoldierOrderGoal;
import com.endruqq.imperium.entity.goal.SoldierRangedGoal;
import com.endruqq.imperium.entity.goal.SoldierRetreatGoal;
import com.endruqq.imperium.entity.goal.SoldierTargetGoal;
import com.endruqq.imperium.game.Game;
import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.NameGen;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.game.DialogManager;
import com.endruqq.imperium.menu.SoldierEquipmentMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

public class SoldierEntity extends PathfinderMob implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> DATA_NATION = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ORDER = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STANCE = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);

    private Vec3 orderPos;
    private UUID orderEntity;
    private int orderProvince = -1;
    private BlockPos home;
    private Vec3 engagePos;
    private long lastCombatTick = -1000;
    private boolean retreating;
    private int regenTimer;
    private String soldierName = "";

    public SoldierEntity(EntityType<? extends SoldierEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanFloat(true);
        }
        setPersistenceRequired();
        this.xpReward = 2;
        for (EquipmentSlot slot : EquipmentSlot.values()) setDropChance(slot, 0.35F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.2);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_NATION, -1);
        entityData.define(DATA_TYPE, 0);
        entityData.define(DATA_ORDER, OrderType.HOLD.ordinal());
        entityData.define(DATA_STANCE, Stance.DEFENSIVE.ordinal());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SoldierRetreatGoal(this));
        goalSelector.addGoal(2, new SoldierRangedGoal(this));
        goalSelector.addGoal(2, new SoldierMeleeGoal(this));
        goalSelector.addGoal(3, new SoldierAttackCoreGoal(this));
        goalSelector.addGoal(4, new SoldierOrderGoal(this));
        goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new SoldierDefendGoal(this));
        targetSelector.addGoal(2, new SoldierTargetGoal(this));
    }

    // ------------------------------------------------------------------ setup

    /** Initialise a newly recruited soldier. */
    public void setup(UnitType type, Nation nation, GameData data, BlockPos home) {
        setNationId(nation == null ? -1 : nation.id);
        setUnitType(type);
        this.home = home;
        this.orderPos = position();
        boolean female = random.nextInt(6) == 0;
        this.soldierName = NameGen.person(random, female);
        setCustomName(Component.translatable("imperium.unit." + type.id() + ".named", soldierName));
        setCustomNameVisible(false);
        type.equip(this, nation, data);
    }

    public void setUnitType(UnitType type) {
        entityData.set(DATA_TYPE, type.ordinal());
        setBase(Attributes.MAX_HEALTH, type.maxHealth);
        setBase(Attributes.MOVEMENT_SPEED, type.speed);
        setBase(Attributes.ATTACK_DAMAGE, type.attack);
        setHealth(getMaxHealth());
    }

    private void setBase(net.minecraft.world.entity.ai.attributes.Attribute attr, double v) {
        AttributeInstance inst = getAttribute(attr);
        if (inst != null) inst.setBaseValue(v);
    }

    public UnitType getUnitType() {
        return UnitType.byOrdinal(entityData.get(DATA_TYPE));
    }

    public int getNationId() {
        return entityData.get(DATA_NATION);
    }

    public void setNationId(int id) {
        entityData.set(DATA_NATION, id);
    }

    public OrderType getOrder() {
        return OrderType.byOrdinal(entityData.get(DATA_ORDER));
    }

    public Stance getStance() {
        return Stance.byOrdinal(entityData.get(DATA_STANCE));
    }

    public void setStance(Stance s) {
        entityData.set(DATA_STANCE, s.ordinal());
    }

    public String getSoldierName() {
        return soldierName;
    }

    @Nullable
    public BlockPos getHome() {
        return home;
    }

    public void setHome(BlockPos home) {
        this.home = home;
    }

    // ------------------------------------------------------------------ orders

    public void order(OrderType type, @Nullable Vec3 pos, @Nullable UUID entity, int province) {
        entityData.set(DATA_ORDER, type.ordinal());
        this.orderPos = pos != null ? pos : position();
        this.orderEntity = entity;
        this.orderProvince = province;
        this.retreating = false;
        if (type == OrderType.MOVE || type == OrderType.RETURN) {
            setTarget(null);
        }
        getNavigation().stop();
    }

    public void hold(Vec3 at) {
        order(OrderType.HOLD, at, null, -1);
    }

    public Vec3 getOrderPos() {
        return orderPos == null ? position() : orderPos;
    }

    @Nullable
    public UUID getOrderEntity() {
        return orderEntity;
    }

    public int getOrderProvince() {
        return orderProvince;
    }

    // ------------------------------------------------------------------ combat logic used by goals

    public boolean isRangedUnitArmed() {
        return getMainHandItem().getItem() instanceof BowItem;
    }

    /** May the soldier start/continue a fight according to its order and stance? */
    public boolean canFight() {
        if (retreating) return false;
        OrderType o = getOrder();
        if (o == OrderType.MOVE || o == OrderType.RETURN) return false;
        if (o == OrderType.ATTACK_TARGET) return true;
        return getStance() != Stance.PASSIVE || (getLastHurtByMob() != null && tickCount - getLastHurtByMobTimestamp() < 100);
    }

    /** May the soldier look for new targets on its own? */
    public boolean canAcquireTargets() {
        if (!canFight()) return false;
        return getStance() != Stance.PASSIVE || getOrder() == OrderType.ATTACK_MOVE || getOrder() == OrderType.ATTACK_CORE;
    }

    public void markEngaged() {
        engagePos = position();
        lastCombatTick = level().getGameTime();
    }

    public boolean withinLeash() {
        if (getOrder() == OrderType.ATTACK_TARGET) return true;
        double leash = getStance().leash;
        if (getOrder() == OrderType.ATTACK_MOVE || getOrder() == OrderType.ATTACK_CORE) leash = Math.max(leash, 20);
        if (getStance() == Stance.PASSIVE) leash = 8;
        Vec3 anchor = engagePos != null ? engagePos : getOrderPos();
        return position().distanceToSqr(anchor) <= leash * leash;
    }

    public boolean inCombat() {
        return level().getGameTime() - lastCombatTick < 100;
    }

    public boolean isRetreating() {
        return retreating;
    }

    public void setRetreating(boolean r) {
        this.retreating = r;
        if (r) setTarget(null);
    }

    public double retreatThreshold() {
        if (getOrder() == OrderType.ATTACK_TARGET) return 0.15;
        GameData d = Game.data(level());
        Nation n = d == null ? null : d.nation(getNationId());
        if (n != null && n.morale() < 30) return 0.4;
        return 0.25;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && target != getTarget()) markEngaged();
        super.setTarget(target);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        lastCombatTick = level().getGameTime();
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean r = super.hurt(source, amount);
        if (r) lastCombatTick = level().getGameTime();
        return r;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        ItemStack arrowStack = new ItemStack(Items.ARROW);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, arrowStack, power);
        if (getMainHandItem().getItem() instanceof BowItem bow) arrow = bow.customArrow(arrow);
        double dx = target.getX() - getX();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + d * 0.2F, dz, 1.6F, 5.0F);
        playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(arrow);
        lastCombatTick = level().getGameTime();
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void aiStep() {
        updateSwingTime();
        super.aiStep();
        if (!level().isClientSide && isAlive()) {
            if (--regenTimer <= 0) {
                regenTimer = 40;
                if (!inCombat() && getHealth() < getMaxHealth()) {
                    float heal = 1.0F;
                    GameData d = Game.data(level());
                    if (d != null && getNationId() >= 0 && d.nationIdAt(blockPosition()) == getNationId()
                            && d.hasActive(getNationId(), Buildings.HOSPITAL.id)) heal = 3.0F;
                    if (retreating) heal *= 1.5F;
                    heal(heal);
                }
            }
        }
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
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) {
            GameData d = Game.data(level());
            if (d != null) {
                Nation n = d.nation(getNationId());
                if (n != null) {
                    n.soldierCount = Math.max(0, n.soldierCount - 1);
                    n.addLog(Component.translatable("imperium.log.soldier_died", getDisplayName(),
                            source.getEntity() != null ? source.getEntity().getDisplayName() : Component.translatable("imperium.unknown")));
                    d.markChanged();
                }
            }
        }
    }

    /** Remove the soldier without a death (dismissal / desertion). */
    public void dismiss() {
        GameData d = Game.data(level());
        if (d != null) {
            Nation n = d.nation(getNationId());
            if (n != null) {
                n.soldierCount = Math.max(0, n.soldierCount - 1);
                d.markChanged();
            }
        }
        discard();
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
    public boolean isAlliedTo(Entity other) {
        if (super.isAlliedTo(other)) return true;
        if (other instanceof SoldierEntity s && s.getNationId() == getNationId() && getNationId() >= 0) return true;
        if (level().isClientSide) return false;
        GameData d = Game.data(level());
        if (d == null) return false;
        int me = getNationId();
        return me >= 0 && d.friendly(me, Allegiance.nationOf(other, d));
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        GameData d = Game.data(level());
        int pn = d == null ? -1 : d.nationIdOf(player.getUUID());
        boolean own = pn >= 0 && pn == getNationId();
        ItemStack held = player.getItemInHand(hand);
        if (own) {
            if (player.isShiftKeyDown()) {
                openEquipment(sp);
                return InteractionResult.CONSUME;
            }
            if (!held.isEmpty() && tryEquip(player, hand, held)) return InteractionResult.CONSUME;
            if (held.isEdible() && getHealth() < getMaxHealth()) {
                var food = held.getFoodProperties(this);
                heal(food == null ? 2 : food.getNutrition());
                if (!player.getAbilities().instabuild) held.shrink(1);
                playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
                return InteractionResult.CONSUME;
            }
        }
        DialogManager.openSoldierDialog(sp, this);
        return InteractionResult.CONSUME;
    }

    private boolean tryEquip(Player player, InteractionHand hand, ItemStack held) {
        EquipmentSlot slot = null;
        if (held.getItem() instanceof ArmorItem armor) slot = armor.getEquipmentSlot();
        else if (held.getItem() instanceof ShieldItem) slot = EquipmentSlot.OFFHAND;
        else if (held.getItem() instanceof TieredItem || held.getItem() instanceof BowItem) slot = EquipmentSlot.MAINHAND;
        if (slot == null) return false;
        ItemStack old = getItemBySlot(slot);
        setItemSlot(slot, held.split(1));
        setGuaranteedDrop(slot);
        if (!old.isEmpty() && !player.getInventory().add(old)) player.drop(old, false);
        playSound(SoundEvents.ARMOR_EQUIP_GENERIC, 1.0F, 1.0F);
        return true;
    }

    public void openEquipment(ServerPlayer player) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inv, p) -> new SoldierEquipmentMenu(id, inv, this),
                getDisplayName()), buf -> buf.writeVarInt(getId()));
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Nation", getNationId());
        tag.putInt("UnitType", entityData.get(DATA_TYPE));
        tag.putInt("Order", entityData.get(DATA_ORDER));
        tag.putInt("Stance", entityData.get(DATA_STANCE));
        Vec3 op = getOrderPos();
        tag.putDouble("OX", op.x);
        tag.putDouble("OY", op.y);
        tag.putDouble("OZ", op.z);
        if (orderEntity != null) tag.putUUID("OEntity", orderEntity);
        tag.putInt("OProvince", orderProvince);
        if (home != null) tag.putLong("Home", home.asLong());
        tag.putString("SName", soldierName);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setNationId(tag.getInt("Nation"));
        entityData.set(DATA_TYPE, tag.getInt("UnitType"));
        entityData.set(DATA_ORDER, tag.getInt("Order"));
        entityData.set(DATA_STANCE, tag.getInt("Stance"));
        orderPos = new Vec3(tag.getDouble("OX"), tag.getDouble("OY"), tag.getDouble("OZ"));
        if (tag.hasUUID("OEntity")) orderEntity = tag.getUUID("OEntity");
        orderProvince = tag.getInt("OProvince");
        if (tag.contains("Home")) home = BlockPos.of(tag.getLong("Home"));
        soldierName = tag.getString("SName");
    }
}
