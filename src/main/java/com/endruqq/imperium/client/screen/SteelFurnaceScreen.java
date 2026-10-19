package com.endruqq.imperium.client.screen;

import com.endruqq.imperium.client.UiUtil;
import com.endruqq.imperium.menu.SteelFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class SteelFurnaceScreen extends AbstractContainerScreen<SteelFurnaceMenu> {
    public SteelFurnaceScreen(SteelFurnaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        UiUtil.panelLight(g, x, y, imageWidth, imageHeight);
        for (Slot s : menu.slots) {
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, 0xFF373737);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF8B8B8B);
        }
        // flame
        int burn = menu.burn() * 13 / menu.burnMax();
        g.fill(x + 57, y + 37, x + 71, y + 50, 0xFF3A3A3A);
        if (menu.burn() > 0) g.fill(x + 57, y + 50 - burn, x + 71, y + 50, 0xFFFF8020);
        // arrow
        int prog = menu.progress() * 24 / menu.maxProgress();
        g.fill(x + 79, y + 41, x + 103, y + 47, 0xFF3A3A3A);
        g.fill(x + 79, y + 41, x + 79 + prog, y + 47, 0xFFE0E0E0);
        g.fill(x + 103, y + 38, x + 106, y + 50, 0xFF3A3A3A);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFD700, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFDDDDDD, false);
        Component status = menu.smelteryOk() ? Component.translatable("imperium.ui.furnace_ok") : Component.translatable("imperium.ui.furnace_no_smeltery");
        g.drawString(font, status, 0, -11, menu.smelteryOk() ? 0xFF80FF80 : 0xFFFF7070, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
