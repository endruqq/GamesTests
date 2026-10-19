package com.endruqq.imperium.client;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.client.screen.NationScreen;
import com.endruqq.imperium.client.screen.RtsChild;
import com.endruqq.imperium.client.screen.RtsScreen;
import com.endruqq.imperium.client.screen.TechTreeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Imperium.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        while (Keys.RTS.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new RtsScreen());
        }
        while (Keys.TECH.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new TechTreeScreen(null));
        }
        while (Keys.NATION.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new NationScreen(null, 0));
        }
        while (Keys.BORDERS.consumeClick()) {
            ClientState.showBorders = !ClientState.showBorders;
            mc.player.displayClientMessage(Component.translatable(ClientState.showBorders ? "imperium.msg.borders_on" : "imperium.msg.borders_off"), true);
        }
        if (RtsCamera.active && !(mc.screen instanceof RtsScreen) && !(mc.screen instanceof RtsChild)) {
            RtsCamera.exit();
        }
        MapTextures.tick();
        HudOverlay.tick();
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START && RtsCamera.active) RtsCamera.update(false);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            if (RtsCamera.active) {
                RtsCamera.captureMatrices(event.getProjectionMatrix(), event.getPoseStack().last().pose(), event.getCamera().getPosition());
            }
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            WorldOverlayRenderer.render(event);
        }
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
        if (RtsCamera.active) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent event) {
        if (RtsCamera.active) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (RtsCamera.active) event.setFOV(70.0);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        RtsCamera.active = false;
        ClientState.reset();
        MapTextures.reset();
        WorldOverlayRenderer.invalidate();
        RtsScreen.SELECTION.clear();
    }
}
