package com.endruqq.imperium.item;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public final class SteelArmorMaterial implements ArmorMaterial {
    public static final SteelArmorMaterial INSTANCE = new SteelArmorMaterial();

    private SteelArmorMaterial() {}

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 11 * 22;
            case CHESTPLATE -> 16 * 22;
            case LEGGINGS -> 15 * 22;
            case BOOTS -> 13 * 22;
        };
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 2;
            case CHESTPLATE -> 7;
            case LEGGINGS -> 6;
            case BOOTS -> 2;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 11;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.STEEL_INGOT.get());
    }

    @Override
    public String getName() {
        return Imperium.MODID + ":steel";
    }

    @Override
    public float getToughness() {
        return 1.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.05F;
    }
}
