package com.endruqq.imperium.client.screen;

import com.endruqq.imperium.client.ClientState;
import com.endruqq.imperium.client.UiUtil;
import com.endruqq.imperium.entity.UnitType;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.building.BuildingType;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.game.tech.Tech;
import com.endruqq.imperium.network.ActionPacket;
import com.endruqq.imperium.network.ImperiumNet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** The technology ("skill") tree. */
public class TechTreeScreen extends Screen implements RtsChild {
    private static final int NODE_W = 112;
    private static final int NODE_H = 30;
    private final Screen parent;
    private int gridX, gridY, colStep, rowStep;

    public TechTreeScreen(Screen parent) {
        super(Component.translatable("imperium.ui.tech_tree"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        colStep = Math.max(NODE_W + 8, Math.min(NODE_W + 40, (width - 20) / 6));
        rowStep = Math.max(NODE_H + 14, Math.min(NODE_H + 40, (height - 70) / 4));
        int totalW = colStep * 5 + NODE_W;
        gridX = (width - totalW) / 2;
        gridY = 50;
    }

    private int nodeX(Tech t) {
        return gridX + t.column * colStep;
    }

    private int nodeY(Tech t) {
        return gridY + t.row * rowStep;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        Nation n = ClientState.myNation();
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFD700);
        if (n == null) {
            g.drawCenteredString(font, Component.translatable("imperium.hud.no_nation"), width / 2, height / 2, 0xFFAAAAAA);
            return;
        }
        g.drawCenteredString(font, Component.translatable("imperium.ui.research_points", (int) n.research, UiUtil.signed(n.researchDelta)),
                width / 2, 22, 0xFF9AD0FF);
        String[] branches = {"imperium.ui.branch.economy", "imperium.ui.branch.society", "imperium.ui.branch.military"};
        for (int b = 0; b < 3; b++) {
            int x = gridX + b * 2 * colStep + (colStep + NODE_W) / 2;
            g.drawCenteredString(font, Component.translatable(branches[b]), x, 36, 0xFFCCCCCC);
        }
        // connections
        for (Tech t : Tech.values()) {
            for (Tech p : t.prerequisites()) {
                int x0 = nodeX(p) + NODE_W / 2, y0 = nodeY(p) + NODE_H;
                int x1 = nodeX(t) + NODE_W / 2, y1 = nodeY(t);
                int col = n.has(p) ? 0xFF80C080 : 0xFF606060;
                int midY = (y0 + y1) / 2;
                g.vLine(x0, y0, midY, col);
                g.hLine(Math.min(x0, x1), Math.max(x0, x1), midY, col);
                g.vLine(x1, midY, y1, col);
            }
        }
        Tech hovered = null;
        for (Tech t : Tech.values()) {
            int x = nodeX(t), y = nodeY(t);
            boolean has = n.has(t);
            boolean avail = t.available(n);
            boolean afford = avail && n.research >= t.cost;
            int bg = has ? 0xFF1F4A26 : afford ? 0xFF5A4A16 : avail ? 0xFF3A3A26 : 0xFF262626;
            int border = has ? 0xFF60E070 : afford ? 0xFFFFD700 : avail ? 0xFFA0A060 : 0xFF505050;
            boolean hover = UiUtil.inside(mx, my, x, y, NODE_W, NODE_H);
            if (hover) hovered = t;
            g.fill(x, y, x + NODE_W, y + NODE_H, bg);
            g.renderOutline(x, y, NODE_W, NODE_H, hover ? 0xFFFFFFFF : border);
            g.renderItem(t.icon(), x + 4, y + 7);
            g.drawString(font, UiUtil.trim(font, t.title().getString(), NODE_W - 26), x + 23, y + 5, has ? 0xFF90FF90 : 0xFFFFFFFF);
            String cost = has ? "✔" : t.cost + " ✦";
            g.drawString(font, cost, x + 23, y + 17, has ? 0xFF60E070 : afford ? 0xFFFFD700 : 0xFF999999);
        }
        super.render(g, mx, my, pt);
        if (hovered != null) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.addAll(font.split(hovered.title().copy().withStyle(ChatFormatting.GOLD), 220));
            lines.addAll(font.split(hovered.description().copy().withStyle(ChatFormatting.GRAY), 220));
            for (BuildingType b : Buildings.all()) {
                if (b.tech == hovered) lines.addAll(font.split(Component.translatable("imperium.ui.unlocks", b.name()).withStyle(ChatFormatting.AQUA), 220));
            }
            for (UnitType u : UnitType.values()) {
                if (u.tech == hovered) lines.addAll(font.split(Component.translatable("imperium.ui.unlocks", u.title()).withStyle(ChatFormatting.AQUA), 220));
            }
            if (!hovered.prerequisites().isEmpty()) {
                List<String> names = new ArrayList<>();
                for (Tech p : hovered.prerequisites()) names.add(p.title().getString());
                lines.addAll(font.split(Component.translatable("imperium.ui.prereq", String.join(", ", names)).withStyle(ChatFormatting.YELLOW), 220));
            }
            if (!n.has(hovered)) {
                lines.addAll(font.split(Component.translatable("imperium.ui.tech_cost", hovered.cost).withStyle(ChatFormatting.WHITE), 220));
            }
            g.renderTooltip(font, lines, mx, my);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        Nation n = ClientState.myNation();
        if (n != null && button == 0) {
            for (Tech t : Tech.values()) {
                if (UiUtil.inside(mx, my, nodeX(t), nodeY(t), NODE_W, NODE_H) && t.available(n)) {
                    CompoundTag tag = new CompoundTag();
                    tag.putString("tech", t.id());
                    ImperiumNet.sendToServer(new ActionPacket("research", tag));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
