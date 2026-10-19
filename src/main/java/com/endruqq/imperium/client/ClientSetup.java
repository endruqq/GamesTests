package com.endruqq.imperium.client;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.client.render.CitizenRenderer;
import com.endruqq.imperium.client.render.SoldierRenderer;
import com.endruqq.imperium.client.screen.SoldierEquipmentScreen;
import com.endruqq.imperium.client.screen.SteelFurnaceScreen;
import com.endruqq.imperium.registry.ModEntities;
import com.endruqq.imperium.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Imperium.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.SOLDIER_EQUIPMENT.get(), SoldierEquipmentScreen::new);
            MenuScreens.register(ModMenus.STEEL_FURNACE.get(), SteelFurnaceScreen::new);
        });
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CITIZEN.get(), CitizenRenderer::new);
        event.registerEntityRenderer(ModEntities.SOLDIER.get(), SoldierRenderer::new);
    }

    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent event) {
        event.register(Keys.RTS);
        event.register(Keys.TECH);
        event.register(Keys.NATION);
        event.register(Keys.BORDERS);
    }

    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("hud", HudOverlay::render);
    }
}
