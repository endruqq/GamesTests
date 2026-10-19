package com.endruqq.imperium.registry;

import com.endruqq.imperium.Imperium;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Imperium.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.imperium"))
            .icon(() -> new ItemStack(ModItems.PROVINCE_CORE.get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.STEEL_INGOT.get());
                out.accept(ModItems.STEEL_NUGGET.get());
                out.accept(ModItems.STEEL_BLOCK.get());
                out.accept(ModItems.STEEL_SWORD.get());
                out.accept(ModItems.STEEL_PICKAXE.get());
                out.accept(ModItems.STEEL_AXE.get());
                out.accept(ModItems.STEEL_SHOVEL.get());
                out.accept(ModItems.STEEL_HELMET.get());
                out.accept(ModItems.STEEL_CHESTPLATE.get());
                out.accept(ModItems.STEEL_LEGGINGS.get());
                out.accept(ModItems.STEEL_BOOTS.get());
                out.accept(ModItems.STEEL_FURNACE.get());
                out.accept(ModItems.TREASURY.get());
                out.accept(ModItems.CITIZEN_SPAWN_EGG.get());
                out.accept(ModItems.SOLDIER_SPAWN_EGG.get());
            })
            .build());

    private ModCreativeTabs() {}
}
