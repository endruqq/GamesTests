package com.endruqq.imperium.registry;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.menu.SoldierEquipmentMenu;
import com.endruqq.imperium.menu.SteelFurnaceMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Imperium.MODID);

    public static final RegistryObject<MenuType<SoldierEquipmentMenu>> SOLDIER_EQUIPMENT = MENUS.register("soldier_equipment",
            () -> IForgeMenuType.create(SoldierEquipmentMenu::new));

    public static final RegistryObject<MenuType<SteelFurnaceMenu>> STEEL_FURNACE = MENUS.register("steel_furnace",
            () -> IForgeMenuType.create(SteelFurnaceMenu::new));

    private ModMenus() {}
}
