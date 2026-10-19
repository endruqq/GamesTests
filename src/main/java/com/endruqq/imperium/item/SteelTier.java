package com.endruqq.imperium.item;

import com.endruqq.imperium.registry.ModItems;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** Steel: between iron and diamond. */
public final class SteelTier implements Tier {
    public static final SteelTier INSTANCE = new SteelTier();

    private SteelTier() {}

    @Override
    public int getUses() {
        return 900;
    }

    @Override
    public float getSpeed() {
        return 7.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 2.5F;
    }

    @Override
    public int getLevel() {
        return 2;
    }

    @Override
    public int getEnchantmentValue() {
        return 12;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.STEEL_INGOT.get());
    }
}
