package com.endruqq.imperium.client.screen;

import com.endruqq.imperium.client.ClientState;
import com.endruqq.imperium.client.UiUtil;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.ResourceType;
import com.endruqq.imperium.network.ActionPacket;
import com.endruqq.imperium.network.ImperiumNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** National treasury: see the stockpile and withdraw materials. */
public class TreasuryScreen extends Screen {
    private final BlockPos pos;
    private int left, top;

    public TreasuryScreen(BlockPos pos) {
        super(Component.translatable("block.imperium.treasury"));
        this.pos = pos;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        left = width / 2 - 130;
        top = height / 2 - 90;
        int y = top + 28;
        for (ResourceType t : ResourceType.values()) {
            int x = left + 120;
            for (int amount : new int[]{1, 16, 64}) {
                addRenderableWidget(Button.builder(Component.literal("-" + amount), b -> withdraw(t, amount)).bounds(x, y, 40, 14).build());
                x += 42;
            }
            y += 20;
        }
    }

    private void withdraw(ResourceType t, int amount) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("pos", pos.asLong());
        tag.putString("resource", t.id());
        tag.putInt("amount", amount);
        ImperiumNet.sendToServer(new ActionPacket("withdraw", tag));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        UiUtil.panelLight(g, left, top, 260, 180);
        Nation n = ClientState.myNation();
        g.drawCenteredString(font, title, left + 130, top + 8, 0xFFFFD700);
        if (n == null) {
            g.drawCenteredString(font, Component.translatable("imperium.hud.no_nation"), left + 130, top + 80, 0xFFAAAAAA);
            super.render(g, mx, my, pt);
            return;
        }
        int y = top + 28;
        for (ResourceType t : ResourceType.values()) {
            g.renderItem(t.icon(), left + 8, y - 2);
            g.drawString(font, t.label(), left + 28, y + 3, 0xFFFFFFFF);
            g.drawString(font, String.valueOf(n.stock.getInt(t)), left + 84, y + 3, 0xFFFFE080);
            y += 20;
        }
        g.drawString(font, Component.translatable("imperium.ui.research_points", (int) n.research, UiUtil.signed(n.researchDelta)), left + 8, y + 2, 0xFF9AD0FF);
        g.drawWordWrap(font, Component.translatable("imperium.ui.treasury_hint"), left + 8, y + 14, 244, 0xFFAAAAAA);
        super.render(g, mx, my, pt);
    }
}
