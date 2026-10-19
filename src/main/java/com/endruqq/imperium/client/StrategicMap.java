package com.endruqq.imperium.client;

import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.Province;
import com.endruqq.imperium.game.building.Building;
import com.endruqq.imperium.network.UnitsPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Draws the political map of the arena into a rectangle of the screen. */
public class StrategicMap {
    public int x, y, w, h;
    public double centerX, centerZ;
    /** screen pixels per block */
    public double zoom = 0.5;

    public void setRect(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public double toScreenX(double wx) {
        return x + w / 2.0 + (wx - centerX) * zoom;
    }

    public double toScreenY(double wz) {
        return y + h / 2.0 + (wz - centerZ) * zoom;
    }

    public double toWorldX(double sx) {
        return centerX + (sx - x - w / 2.0) / zoom;
    }

    public double toWorldZ(double sy) {
        return centerZ + (sy - y - h / 2.0) / zoom;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    public void fitArena(GameData d) {
        centerX = d.centerX;
        centerZ = d.centerZ;
        zoom = Math.min(w, h) / (double) Math.max(1, d.size) * 0.95;
    }

    public void zoomAt(double mx, double my, double factor) {
        double wx = toWorldX(mx), wz = toWorldZ(my);
        zoom = Math.max(0.02, Math.min(8, zoom * factor));
        centerX = wx - (mx - x - w / 2.0) / zoom;
        centerZ = wz - (my - y - h / 2.0) / zoom;
    }

    public void draw(GuiGraphics g, Font font, Set<Integer> selection, int highlightProvince, boolean detailed) {
        GameData d = ClientState.data;
        g.fill(x, y, x + w, y + h, 0xFF1B1E24);
        if (!d.arenaSet) {
            g.drawCenteredString(font, net.minecraft.network.chat.Component.translatable("imperium.ui.no_arena"), x + w / 2, y + h / 2 - 4, 0xFFAAAAAA);
            return;
        }
        g.enableScissor(x, y, x + w, y + h);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (MapTextures.ready()) {
            double left = toScreenX(d.minX()), top = toScreenY(d.minZ());
            float s = (float) (MapTextures.terrainScale * zoom);
            g.pose().pushPose();
            g.pose().translate(left, top, 0);
            g.pose().scale(s, s, 1);
            g.blit(MapTextures.TERRAIN, 0, 0, 0, 0, MapTextures.terrainRes, MapTextures.terrainRes, MapTextures.terrainRes, MapTextures.terrainRes);
            g.pose().popPose();
            float ps = (float) (d.cellSize * zoom / MapTextures.PX_PER_CELL);
            g.pose().pushPose();
            g.pose().translate(left, top, 0);
            g.pose().scale(ps, ps, 1);
            g.blit(MapTextures.POLITICAL, 0, 0, 0, 0, MapTextures.politicalRes, MapTextures.politicalRes, MapTextures.politicalRes, MapTextures.politicalRes);
            g.pose().popPose();
        }
        // arena frame
        int ax0 = (int) toScreenX(d.minX()), az0 = (int) toScreenY(d.minZ()), ax1 = (int) toScreenX(d.maxX()), az1 = (int) toScreenY(d.maxZ());
        g.renderOutline(ax0, az0, ax1 - ax0, az1 - az0, 0xFF000000);

        if (highlightProvince >= 0) highlight(g, d, highlightProvince);
        long now = System.currentTimeMillis();
        int myNation = ClientState.myNationId();

        // buildings
        if (zoom > 0.25) {
            for (Building b : d.buildings.values()) {
                int bx = (int) toScreenX(b.pos.getX()), bz = (int) toScreenY(b.pos.getZ());
                int r = Math.max(1, (int) (b.radius() * zoom));
                int col = b.isActive() ? 0xFF000000 | ClientState.colorOf(b.nation) : b.state == Building.State.DAMAGED ? 0xFFAA3333 : 0xFF888888;
                g.fill(bx - r, bz - r, bx + r + 1, bz + r + 1, 0xC0000000);
                g.fill(bx - r + 1, bz - r + 1, bx + r, bz + r, col);
                if (detailed && zoom > 1.5 && b.type() != null) {
                    g.pose().pushPose();
                    g.pose().translate(bx, bz + r + 2, 0);
                    g.pose().scale(0.5F, 0.5F, 1);
                    g.drawCenteredString(font, b.type().name(), 0, 0, 0xFFFFFFFF);
                    g.pose().popPose();
                }
            }
        }

        // province cores
        for (Province p : d.provinces.values()) {
            if (p.core == null) continue;
            int px = (int) toScreenX(p.core.getX() + 0.5), pz = (int) toScreenY(p.core.getZ() + 0.5);
            Nation owner = d.nation(p.owner);
            boolean capital = owner != null && owner.capitalProvince == p.id;
            boolean attacked = p.underAttack(d.gameTick) && (now / 300) % 2 == 0;
            int r = capital ? 4 : 3;
            g.fill(px - r - 1, pz - r - 1, px + r + 2, pz + r + 2, attacked ? 0xFFFF2020 : 0xFF000000);
            g.fill(px - r, pz - r, px + r + 1, pz + r + 1, 0xFF000000 | (owner == null ? 0x999999 : owner.rgb()));
            if (capital) g.fill(px - 1, pz - 1, px + 2, pz + 2, 0xFFFFD700);
            if (p.coreMaxHp > 0 && p.coreHp < p.coreMaxHp) {
                int bw = 14;
                g.fill(px - bw / 2, pz + r + 3, px + bw / 2, pz + r + 5, 0xFF400000);
                g.fill(px - bw / 2, pz + r + 3, px - bw / 2 + (int) (bw * Math.max(0, p.coreHp / p.coreMaxHp)), pz + r + 5, 0xFF30E030);
            }
        }

        // province names
        if (detailed) {
            for (Province p : d.provinces.values()) {
                if (p.cellCount == 0) continue;
                double areaPx = p.cellCount * d.cellSize * d.cellSize * zoom * zoom;
                if (areaPx < 900) continue;
                int sx = (int) toScreenX(p.centroidX), sz = (int) toScreenY(p.centroidZ) + 6;
                g.pose().pushPose();
                g.pose().translate(sx, sz, 0);
                g.pose().scale(0.75F, 0.75F, 1);
                g.drawCenteredString(font, p.name, 0, 0, 0xFFEFEFEF);
                g.pose().popPose();
            }
            // nation names
            Map<Integer, double[]> centers = new HashMap<>();
            for (Province p : d.provinces.values()) {
                if (p.owner < 0 || p.cellCount == 0) continue;
                double[] c = centers.computeIfAbsent(p.owner, k -> new double[3]);
                c[0] += p.centroidX * p.cellCount;
                c[1] += p.centroidZ * p.cellCount;
                c[2] += p.cellCount;
            }
            for (Map.Entry<Integer, double[]> e : centers.entrySet()) {
                Nation n = d.nation(e.getKey());
                if (n == null) continue;
                double[] c = e.getValue();
                int sx = (int) toScreenX(c[0] / c[2]), sz = (int) toScreenY(c[1] / c[2]) - 10;
                float s = (float) Math.max(1.0, Math.min(2.5, Math.sqrt(c[2] * d.cellSize * d.cellSize) * zoom / 90));
                g.pose().pushPose();
                g.pose().translate(sx, sz, 0);
                g.pose().scale(s, s, 1);
                String label = n.name + (n.eliminated ? " ✝" : "");
                g.drawCenteredString(font, label, 0, 0, 0xFF000000 | brighten(n.rgb()));
                g.pose().popPose();
            }
        }

        // units
        for (UnitsPacket.Unit u : ClientState.units) {
            int sx = (int) toScreenX(u.x), sz = (int) toScreenY(u.z);
            if (sx < x - 4 || sz < y - 4 || sx > x + w + 4 || sz > y + h + 4) continue;
            int col = 0xFF000000 | ClientState.colorOf(u.nation);
            if (u.type < 0) {
                g.fill(sx - 3, sz - 3, sx + 4, sz + 4, 0xFFFFFFFF);
                g.fill(sx - 2, sz - 2, sx + 3, sz + 3, col);
                if (detailed) {
                    g.pose().pushPose();
                    g.pose().translate(sx, sz - 10, 0);
                    g.pose().scale(0.6F, 0.6F, 1);
                    g.drawCenteredString(font, u.name, 0, 0, 0xFFFFFFFF);
                    g.pose().popPose();
                }
            } else {
                boolean sel = selection.contains(u.entityId);
                int r = zoom > 1 ? 2 : 1;
                g.fill(sx - r - 1, sz - r - 1, sx + r + 2, sz + r + 2, sel ? 0xFFFFFFFF : 0xFF000000);
                g.fill(sx - r, sz - r, sx + r + 1, sz + r + 1, u.nation == myNation && u.hp < 0.35 ? 0xFFFF6060 : col);
            }
        }

        // RTS camera position
        if (RtsCamera.active) {
            int cx = (int) toScreenX(RtsCamera.focusX), cz = (int) toScreenY(RtsCamera.focusZ);
            int r = Math.max(3, (int) (RtsCamera.height * 1.2 * zoom));
            g.renderOutline(cx - r, cz - r, r * 2, r * 2, 0xCCFFFFFF);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            int px = (int) toScreenX(mc.player.getX()), pz = (int) toScreenY(mc.player.getZ());
            g.fill(px - 1, pz - 3, px + 2, pz + 4, 0xFFFFFFFF);
            g.fill(px - 3, pz - 1, px + 4, pz + 2, 0xFFFFFFFF);
        }
        RenderSystem.disableBlend();
        g.disableScissor();
    }

    private void highlight(GuiGraphics g, GameData d, int provinceId) {
        int n = d.gridN;
        double cs = d.cellSize * zoom;
        for (int i = 0; i < d.cells.length; i++) {
            if (d.cells[i] != provinceId) continue;
            int cx = i % n, cz = i / n;
            int sx = (int) toScreenX(d.minX() + cx * d.cellSize), sz = (int) toScreenY(d.minZ() + cz * d.cellSize);
            g.fill(sx, sz, sx + (int) Math.ceil(cs), sz + (int) Math.ceil(cs), 0x40FFFFFF);
        }
    }

    private static int brighten(int rgb) {
        int r = Math.min(255, (rgb >> 16 & 255) + 70), gg = Math.min(255, (rgb >> 8 & 255) + 70), b = Math.min(255, (rgb & 255) + 70);
        return r << 16 | gg << 8 | b;
    }
}
