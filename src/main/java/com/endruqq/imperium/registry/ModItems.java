package com.endruqq.imperium.registry;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.item.SteelArmorMaterial;
import com.endruqq.imperium.item.SteelTier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Imperium.MODID);

    public static final RegistryObject<Item> STEEL_INGOT = ITEMS.register("steel_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STEEL_NUGGET = ITEMS.register("steel_nugget", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> STEEL_SWORD = ITEMS.register("steel_sword",
            () -> new SwordItem(SteelTier.INSTANCE, 3, -2.4F, new Item.Properties()));
    public static final RegistryObject<Item> STEEL_PICKAXE = ITEMS.register("steel_pickaxe",
            () -> new PickaxeItem(SteelTier.INSTANCE, 1, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> STEEL_AXE = ITEMS.register("steel_axe",
            () -> new AxeItem(SteelTier.INSTANCE, 5.5F, -3.1F, new Item.Properties()));
    public static final RegistryObject<Item> STEEL_SHOVEL = ITEMS.register("steel_shovel",
            () -> new ShovelItem(SteelTier.INSTANCE, 1.5F, -3.0F, new Item.Properties()));

    public static final RegistryObject<Item> STEEL_HELMET = ITEMS.register("steel_helmet",
            () -> new ArmorItem(SteelArmorMaterial.INSTANCE, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> STEEL_CHESTPLATE = ITEMS.register("steel_chestplate",
            () -> new ArmorItem(SteelArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> STEEL_LEGGINGS = ITEMS.register("steel_leggings",
            () -> new ArmorItem(SteelArmorMaterial.INSTANCE, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> STEEL_BOOTS = ITEMS.register("steel_boots",
            () -> new ArmorItem(SteelArmorMaterial.INSTANCE, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> PROVINCE_CORE = ITEMS.register("province_core",
            () -> new BlockItem(ModBlocks.PROVINCE_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> CONSTRUCTION_SITE = ITEMS.register("construction_site",
            () -> new BlockItem(ModBlocks.CONSTRUCTION_SITE.get(), new Item.Properties()));
    public static final RegistryObject<Item> TREASURY = ITEMS.register("treasury",
            () -> new BlockItem(ModBlocks.TREASURY.get(), new Item.Properties()));
    public static final RegistryObject<Item> STEEL_FURNACE = ITEMS.register("steel_furnace",
            () -> new BlockItem(ModBlocks.STEEL_FURNACE.get(), new Item.Properties()));
    public static final RegistryObject<Item> STEEL_BLOCK = ITEMS.register("steel_block",
            () -> new BlockItem(ModBlocks.STEEL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> CITIZEN_SPAWN_EGG = ITEMS.register("citizen_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CITIZEN, 0x8B5A2B, 0xE8C9A0, new Item.Properties()));
    public static final RegistryObject<Item> SOLDIER_SPAWN_EGG = ITEMS.register("soldier_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SOLDIER, 0x505A6B, 0xC0392B, new Item.Properties()));

    private ModItems() {}
}
