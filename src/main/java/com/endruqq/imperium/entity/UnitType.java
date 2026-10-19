package com.endruqq.imperium.entity;

import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.ResourceType;
import com.endruqq.imperium.game.Resources;
import com.endruqq.imperium.game.building.BuildingType;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.game.tech.Tech;
import com.endruqq.imperium.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum UnitType {
    MILITIA(20, 0.27, 2.0, false, 1.0, Buildings.BARRACKS, null,
            Resources.of(ResourceType.GOLD, 5, ResourceType.FOOD, 10)),
    SWORDSMAN(26, 0.27, 3.0, false, 1.0, Buildings.BARRACKS, null,
            Resources.of(ResourceType.GOLD, 15, ResourceType.FOOD, 10, ResourceType.IRON, 5)),
    ARCHER(20, 0.28, 2.0, true, 1.0, Buildings.ARCHERY_RANGE, Tech.ARCHERY,
            Resources.of(ResourceType.GOLD, 12, ResourceType.FOOD, 10, ResourceType.WOOD, 10)),
    KNIGHT(34, 0.26, 4.0, false, 1.0, Buildings.ARMORY, Tech.STEEL_WEAPONS,
            Resources.of(ResourceType.GOLD, 30, ResourceType.FOOD, 15, ResourceType.STEEL, 6)),
    SAPPER(22, 0.27, 3.0, false, 6.0, Buildings.SIEGE_WORKSHOP, Tech.SIEGE,
            Resources.of(ResourceType.GOLD, 20, ResourceType.FOOD, 10, ResourceType.IRON, 5));

    public final double maxHealth;
    public final double speed;
    public final double attack;
    public final boolean ranged;
    public final double coreDamageMult;
    public final BuildingType building;
    public final Tech tech;
    public final Resources cost;

    UnitType(double maxHealth, double speed, double attack, boolean ranged, double coreDamageMult,
             BuildingType building, Tech tech, Resources cost) {
        this.maxHealth = maxHealth;
        this.speed = speed;
        this.attack = attack;
        this.ranged = ranged;
        this.coreDamageMult = coreDamageMult;
        this.building = building;
        this.tech = tech;
        this.cost = cost;
    }

    public String id() {
        return name().toLowerCase();
    }

    public Component title() {
        return Component.translatable("imperium.unit." + id());
    }

    public static UnitType byOrdinal(int o) {
        UnitType[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, o))];
    }

    /** Cost multiplier from techs. */
    public double costMult(Nation n) {
        return n.has(Tech.MILITARY_SERVICE) ? 0.7 : 1.0;
    }

    public boolean unlocked(Nation n) {
        return tech == null || n.has(tech);
    }

    /** Gives a freshly recruited soldier its starting kit. */
    public void equip(SoldierEntity s, Nation n, GameData data) {
        boolean armory = n != null && data != null && data.hasActive(n.id, Buildings.ARMORY.id);
        switch (this) {
            case MILITIA -> {
                s.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(armory ? Items.IRON_SWORD : Items.STONE_SWORD));
                s.setItemSlot(EquipmentSlot.HEAD, new ItemStack(armory ? Items.IRON_HELMET : Items.LEATHER_HELMET));
            }
            case SWORDSMAN -> {
                s.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                s.setItemSlot(EquipmentSlot.HEAD, new ItemStack(armory ? Items.IRON_HELMET : Items.CHAINMAIL_HELMET));
                s.setItemSlot(EquipmentSlot.CHEST, new ItemStack(armory ? Items.IRON_CHESTPLATE : Items.CHAINMAIL_CHESTPLATE));
                s.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            }
            case ARCHER -> {
                s.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                s.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                s.setItemSlot(EquipmentSlot.CHEST, new ItemStack(armory ? Items.CHAINMAIL_CHESTPLATE : Items.LEATHER_CHESTPLATE));
            }
            case KNIGHT -> {
                s.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.STEEL_SWORD.get()));
                s.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                s.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.STEEL_HELMET.get()));
                s.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.STEEL_CHESTPLATE.get()));
                s.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.STEEL_LEGGINGS.get()));
                s.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.STEEL_BOOTS.get()));
            }
            case SAPPER -> {
                s.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
                s.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                s.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            }
        }
    }
}
