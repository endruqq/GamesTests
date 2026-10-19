package com.endruqq.imperium.client;

import com.endruqq.imperium.game.BuildingManager;
import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.GamePhase;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.Province;
import com.endruqq.imperium.game.RelationType;
import com.endruqq.imperium.game.ResourceType;
import com.endruqq.imperium.game.building.Building;
import com.endruqq.imperium.game.building.BuildingType;
import com.endruqq.imperium.game.building.Requirement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** First-person HUD: nation summary, current province, construction checklist, notifications. */
public final class HudOverlay {
    private static Building scanned;
    private static int[] counts = new int[0];
    private static int scanTimer;

    private HudOverlay() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !ClientState.hasData) {
            scanned = null;
            return;
        }
        Building b = buildingHere(ClientState.data, mc.player.position());
        if (b != scanned) {
            scanned = b;
            scanTimer = 0;
        }
        if (b != null && --scanTimer <= 0) {
            scanTimer = 10;
            counts = BuildingManager.count(mc.level, b);
        }
    }

    public static Building buildingHere(GameData d, Vec3 p) {
        for (Building b : d.buildings.values()) {
            if (b.containsXZ(p.x, p.z, 0) && p.y >= b.minY() - 1 && p.y <= b.maxY() + 2) return b;
        }
        return null;
    }

    public static void render(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || RtsCamera.active || mc.player == null || !ClientState.hasData) return;
        Font font = mc.font;
        GameData d = ClientState.data;
        Nation n = ClientState.myNation();
        int x = 4, y = 4;
        if (d.phase == GamePhase.SETUP) {
            UiUtil.panel(g, x - 2, y - 2, 230, 24);
            g.drawString(font, Component.translatable("imperium.hud.setup").withStyle(ChatFormatting.GOLD), x, y, 0xFFFFFF);
            g.drawString(font, Component.translatable("imperium.hud.setup_hint"), x, y + 10, 0xCCCCCC);
            renderNotes(g, font, h);
            return;
        }
        int panelH = n == null ? 24 : 46;
        String stats = n == null ? "" : "☺ " + (int) n.happiness + "%  ⌂ " + n.population + "/" + n.housing + "  ⚔ " + n.soldierCount;
        int panelW = 210;
        if (n != null) {
            panelW = Math.max(panelW, 19 + font.width(n.name) + font.width(stats));
            int rw = 4;
            for (ResourceType t : ResourceType.values()) rw += 16 + font.width(String.valueOf(n.stock.getInt(t)));
            panelW = Math.max(panelW, rw);
        }
        UiUtil.panel(g, x - 2, y - 2, panelW, panelH);
        if (n == null) {
            g.drawString(font, Component.translatable("imperium.hud.no_nation"), x, y, 0xFFAAAAAA);
        } else {
            g.fill(x, y, x + 8, y + 8, 0xFF000000 | n.rgb());
            g.drawString(font, n.name, x + 11, y, 0xFFFFFF);
            g.drawString(font, stats, x + 11 + font.width(n.name) + 6, y, n.happiness < 30 ? 0xFFFF6060 : 0xFFB0FFB0);
            int ix = x;
            for (ResourceType t : ResourceType.values()) {
                UiUtil.smallItem(g, t.icon(), ix, y + 22);
                String v = String.valueOf(n.stock.getInt(t));
                g.drawString(font, v, ix + 9, y + 23, t == ResourceType.FOOD && n.famine ? 0xFFFF4040 : 0xFFEEEEEE);
                ix += 16 + font.width(v);
            }
            if (n.famine) g.drawString(font, Component.translatable("imperium.hud.famine").withStyle(ChatFormatting.RED), x, y + 33, 0xFFFFFF);
            else if (d.inGracePeriod()) g.drawString(font, Component.translatable("imperium.hud.grace", UiUtil.time(ClientState.graceSeconds())), x, y + 33, 0xFF9AD0FF);
            else if (d.atWarWithAnyone(n.id)) g.drawString(font, Component.translatable("imperium.hud.at_war").withStyle(ChatFormatting.RED), x, y + 33, 0xFFFFFF);
        }
        // current province
        Province p = d.provinceAt(mc.player.getX(), mc.player.getZ());
        Component where;
        int color = 0xFFDDDDDD;
        if (p == null) {
            where = Component.translatable(d.inArena(mc.player.getX(), mc.player.getZ()) ? "imperium.hud.wilderness" : "imperium.hud.outside");
        } else {
            Nation owner = d.nation(p.owner);
            where = Component.translatable("imperium.hud.province", p.name, owner == null ? "-" : owner.name);
            if (n != null && owner != null && owner.id != n.id) {
                RelationType rt = d.relationType(n.id, owner.id);
                color = rt == RelationType.WAR ? 0xFFFF4040 : rt == RelationType.ALLIANCE ? 0xFF60FF60 : 0xFFFFD060;
                where = where.copy().append(" [").append(Component.translatable(rt.key())).append("]");
            } else if (owner != null) {
                color = 0xFF000000 | owner.rgb();
            }
        }
        g.drawString(font, where, x, y + 11, color);

        renderChecklist(g, font, w, d, n);
        renderCore(g, font, w, d, mc);
        renderNotes(g, font, h);
    }

    private static void renderChecklist(GuiGraphics g, Font font, int w, GameData d, Nation n) {
        Building b = scanned;
        if (b == null || b.type() == null) return;
        if (b.isActive() && (n == null || b.nation != n.id)) return;
        BuildingType t = b.type();
        int pw = 170;
        int ph = 26 + t.requirements.size() * 11;
        int x = w - pw - 4, y = 40;
        UiUtil.panel(g, x - 2, y - 2, pw + 4, ph);
        g.drawString(font, t.name(), x + 2, y, 0xFFFFD700);
        g.drawString(font, Component.translatable(b.state.key()), x + 2, y + 10,
                b.state == Building.State.ACTIVE ? 0xFF60FF60 : b.state == Building.State.DAMAGED ? 0xFFFF6060 : 0xFFFFD060);
        int yy = y + 22;
        for (int i = 0; i < t.requirements.size(); i++) {
            Requirement r = t.requirements.get(i);
            int have = i < counts.length ? counts[i] : 0;
            boolean ok = have >= r.count;
            UiUtil.smallItem(g, r.icon(), x + 2, yy);
            String num = Math.min(have, 999) + "/" + r.count;
            g.drawString(font, UiUtil.trim(font, r.name().getString(), pw - 40 - font.width(num)), x + 13, yy + 1, ok ? 0xFF90FF90 : 0xFFFFFFFF);
            g.drawString(font, num, x + pw - font.width(num), yy + 1, ok ? 0xFF60FF60 : 0xFFFF7070);
            yy += 11;
        }
    }

    private static void renderCore(GuiGraphics g, Font font, int w, GameData d, Minecraft mc) {
        Province best = null;
        double bestD = 14 * 14;
        for (Province p : d.provinces.values()) {
            if (p.core == null) continue;
            double dd = mc.player.distanceToSqr(Vec3.atCenterOf(p.core));
            if (dd < bestD) {
                bestD = dd;
                best = p;
            }
        }
        if (best == null || best.coreMaxHp <= 0) return;
        int bw = 160, x = w / 2 - bw / 2, y = 22;
        Nation owner = d.nation(best.owner);
        UiUtil.panel(g, x - 3, y - 12, bw + 6, 22);
        g.drawCenteredString(font, Component.translatable("imperium.hud.core", best.name, owner == null ? "-" : owner.name), w / 2, y - 10, 0xFFFFD700);
        double f = Math.max(0, Math.min(1, best.coreHp / best.coreMaxHp));
        g.fill(x, y + 1, x + bw, y + 7, 0xFF400000);
        g.fill(x, y + 1, x + (int) (bw * f), y + 7, f > 0.5 ? 0xFF30D030 : f > 0.25 ? 0xFFE0C020 : 0xFFE03030);
    }

    public static void renderNotes(GuiGraphics g, Font font, int h) {
        long now = System.currentTimeMillis();
        int y = h / 2 - 20;
        int shown = 0;
        for (ClientState.Note note : ClientState.NOTES) {
            long age = now - note.time();
            if (age > 9000 || shown >= 5) continue;
            int alpha = age > 7000 ? (int) (255 * (9000 - age) / 2000.0) : 255;
            if (alpha < 8) continue;
            int tw = Math.min(260, font.width(note.text()));
            g.fill(2, y - 2, 8 + tw, y + 10, (alpha / 2) << 24);
            g.drawString(font, UiUtil.trim(font, note.text().getString(), 256), 5, y, (alpha << 24) | (note.important() ? 0xFFD700 : 0xFFFFFF));
            y += 13;
            shown++;
        }
    }
}
