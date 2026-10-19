package com.endruqq.imperium;

import com.endruqq.imperium.entity.CitizenEntity;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.network.ImperiumNet;
import com.endruqq.imperium.registry.ModBlockEntities;
import com.endruqq.imperium.registry.ModBlocks;
import com.endruqq.imperium.registry.ModCreativeTabs;
import com.endruqq.imperium.registry.ModEntities;
import com.endruqq.imperium.registry.ModItems;
import com.endruqq.imperium.registry.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Imperium.MODID)
public class Imperium {
    public static final String MODID = "imperium";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public Imperium() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerAttributes);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ImperiumConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ImperiumNet::register);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CITIZEN.get(), CitizenEntity.createAttributes().build());
        event.put(ModEntities.SOLDIER.get(), SoldierEntity.createAttributes().build());
    }
}
