package com.endruqq.imperium.client.screen;

import com.endruqq.imperium.client.UiUtil;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.menu.SoldierEquipmentMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class SoldierEquipmentScreen extends AbstractContainerScreen<SoldierEquipmentMenu> {
    public SoldierEquipmentScreen(SoldierEquipmentMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        UiUtil.panelLight(g, x, y, imageWidth, imageHeight);
        g.fill(x + 7, y + 7, x + 55, y + 78, 0xFF000000);
        for (Slot s : menu.slots) {
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, 0xFF373737);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF8B8B8B);
        }
        SoldierEntity s = menu.getSoldier();
        if (s != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 31, y + 74, 30, (x + 31) - mx, (y + 25) - my, s);
            g.drawString(font, s.getUnitType().title(), x + 118, y + 10, 0xFFFFD700);
            g.drawString(font, "❤ " + (int) s.getHealth() + "/" + (int) s.getMaxHealth(), x + 118, y + 22, 0xFFFF8080);
            g.drawString(font, "⛨ " + s.getArmorValue(), x + 118, y + 34, 0xFFCCCCCC);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFDDDDDD, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
