package com.endruqq.imperium.client.screen;

import com.endruqq.imperium.client.ClientState;
import com.endruqq.imperium.client.UiUtil;
import com.endruqq.imperium.client.ui.Theme;
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
    private int NODE_W = 112;
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
        NODE_W = Math.max(64, Math.min(112, (width - 16) / 6 - 6));
        colStep = Math.max(NODE_W + 6, Math.min(NODE_W + 40, (width - 16 - NODE_W) / 5));
        rowStep = Math.max(NODE_H + 12, Math.min(NODE_H + 36, (height - 84) / 4));
        int totalW = colStep * 5 + NODE_W;
        gridX = (width - totalW) / 2;
        gridY = 56;
    }

    private int nodeX(Tech t) {
        return gridX + t.column * colStep;
    }

    private int nodeY(Tech t) {
        return gridY + t.row * rowStep;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xE80C1015);
        Nation n = ClientState.myNation();
        g.fill(0, 0, width, 30, Theme.HEADER);
        g.hLine(0, width, 30, Theme.BORDER_LIGHT);
        g.drawCenteredString(font, title, width / 2, 5, Theme.ACCENT);
        if (n == null) {
            g.drawCenteredString(font, Component.translatable("imperium.hud.no_nation"), width / 2, height / 2, Theme.TEXT_DIM);
            return;
        }
        Component rp = Component.translatable("imperium.ui.research_points", (int) n.research, UiUtil.signed(n.researchDelta));
        g.drawCenteredString(font, rp, width / 2, 17, Theme.INFO);
        String[] branches = {"imperium.ui.branch.economy", "imperium.ui.branch.society", "imperium.ui.branch.military"};
        int[] branchColors = {0xFF6FBF5A, 0xFF5AA9E6, 0xFFE5534B};
        for (int b = 0; b < 3; b++) {
            int x0 = gridX + b * 2 * colStep - 4;
            int x1 = gridX + (b * 2 + 1) * colStep + NODE_W + 4;
            g.fill(x0, 36, x1, 48, Theme.alpha(branchColors[b], 0x30));
            g.fill(x0, 47, x1, 48, branchColors[b]);
            g.drawCenteredString(font, Component.translatable(branches[b]), (x0 + x1) / 2, 38, Theme.TEXT);
        }
        // connections
        for (Tech t : Tech.values()) {
            for (Tech p : t.prerequisites()) {
                int x0 = nodeX(p) + NODE_W / 2, y0 = nodeY(p) + NODE_H;
                int x1 = nodeX(t) + NODE_W / 2, y1 = nodeY(t);
                int col = n.has(p) ? (n.has(t) ? Theme.GOOD : Theme.ACCENT_DARK) : 0xFF3A4350;
                int midY = (y0 + y1) / 2;
                g.fill(x0, y0, x0 + 2, midY + 1, col);
                g.fill(Math.min(x0, x1), midY, Math.max(x0, x1) + 2, midY + 2, col);
                g.fill(x1, midY, x1 + 2, y1, col);
            }
        }
        Tech hovered = null;
        for (Tech t : Tech.values()) {
            int x = nodeX(t), y = nodeY(t);
            boolean has = n.has(t);
            boolean avail = t.available(n);
            boolean afford = avail && n.research >= t.cost;
            boolean hover = UiUtil.inside(mx, my, x, y, NODE_W, NODE_H);
            if (hover) hovered = t;
            int bg = has ? 0xFF17311F : afford ? 0xFF3A3115 : avail ? 0xFF242A33 : 0xFF181C22;
            int border = has ? Theme.GOOD : afford ? Theme.ACCENT : avail ? Theme.BORDER_LIGHT : Theme.BORDER;
            if (afford && (System.currentTimeMillis() / 600) % 2 == 0) g.fill(x - 1, y - 1, x + NODE_W + 1, y + NODE_H + 1, Theme.alpha(Theme.ACCENT, 0x40));
            g.fill(x, y, x + NODE_W, y + NODE_H, hover ? Theme.mix(bg, 0xFFFFFFFF, 0.08) : bg);
            g.renderOutline(x, y, NODE_W, NODE_H, hover ? 0xFFFFFFFF : border);
            g.fill(x + 1, y + 1, x + 3, y + NODE_H - 1, border);
            g.renderItem(t.icon(), x + 5, y + 7);
            g.drawString(font, UiUtil.trim(font, t.title().getString(), NODE_W - 28), x + 24, y + 5, has ? 0xFFB6F2C8 : avail ? Theme.TEXT : Theme.TEXT_MUTED, false);
            String cost = has ? "✔" : t.cost + " ✦";
            g.drawString(font, cost, x + 24, y + 17, has ? Theme.GOOD : afford ? Theme.ACCENT : Theme.TEXT_MUTED, false);
            if (!avail && !has) g.drawString(font, "⊘", x + NODE_W - 10, y + 3, Theme.TEXT_MUTED, false);
        }
        // legend
        int ly = height - 14;
        int lx = width / 2 - 150;
        lx = legend(g, lx, ly, Theme.GOOD, Component.translatable("imperium.ui.tech.legend.done"));
        lx = legend(g, lx, ly, Theme.ACCENT, Component.translatable("imperium.ui.tech.legend.ready"));
        lx = legend(g, lx, ly, Theme.BORDER_LIGHT, Component.translatable("imperium.ui.tech.legend.available"));
        legend(g, lx, ly, Theme.BORDER, Component.translatable("imperium.ui.tech.legend.locked"));
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

    private int legend(GuiGraphics g, int x, int y, int color, Component label) {
        g.fill(x, y + 1, x + 7, y + 8, color);
        g.drawString(font, label, x + 10, y, Theme.TEXT_DIM, false);
        return x + 18 + font.width(label);
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
