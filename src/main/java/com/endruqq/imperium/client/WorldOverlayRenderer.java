package com.endruqq.imperium.client;

import com.endruqq.imperium.client.screen.RtsScreen;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.Province;
import com.endruqq.imperium.game.building.Building;
import com.endruqq.imperium.game.building.BuildingType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** In-world overlays: border curtains, building plots, placement ghost, selection rings, core beacons. */
public final class WorldOverlayRenderer {
    private record Segment(float x0, float z0, float x1, float z1, float y, int color) {
    }

    private static final List<Segment> SEGMENTS = new ArrayList<>();
    private static int cacheKey;
    private static double cacheX = Double.NaN, cacheZ = Double.NaN;
    private static long cacheTime;

    private WorldOverlayRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !ClientState.hasData) return;
        GameData d = ClientState.data;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        boolean rts = RtsCamera.active;

        if (d.arenaSet && (rts || ClientState.showBorders)) {
            double cx = rts ? RtsCamera.focusX : mc.player.getX();
            double cz = rts ? RtsCamera.focusZ : mc.player.getZ();
            rebuildBorders(mc.level, d, cx, cz, rts ? 150 : 56);
            drawBorders(pose, cam, rts ? 1.0F : 0.55F);
        }

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        // building plots
        int myNation = ClientState.myNationId();
        for (Building b : d.buildings.values()) {
            double dist = Math.max(Math.abs(b.pos.getX() - cam.x), Math.abs(b.pos.getZ() - cam.z));
            boolean show = rts ? dist < 160 : (dist < 40 && b.nation == myNation && (!b.isActive() || isInside(mc.player.position(), b)));
            if (!show) continue;
            float r = 0.2F, g = 0.9F, bl = 0.3F;
            if (b.state == Building.State.CONSTRUCTION) { r = 1.0F; g = 0.85F; bl = 0.2F; }
            else if (b.state == Building.State.DAMAGED) { r = 1.0F; g = 0.25F; bl = 0.2F; }
            LevelRenderer.renderLineBox(pose, lines, b.box(), r, g, bl, 0.9F);
        }

        // placement ghost
        RtsScreen.Placement pl = RtsScreen.placement;
        if (rts && pl != null && pl.pos != null) {
            BuildingType t = pl.type;
            AABB box = new AABB(pl.pos.getX() - t.radius, pl.pos.getY() - 1, pl.pos.getZ() - t.radius,
                    pl.pos.getX() + t.radius + 1, pl.pos.getY() + t.height, pl.pos.getZ() + t.radius + 1);
            float[] c = pl.valid ? new float[]{0.3F, 1F, 0.3F} : new float[]{1F, 0.2F, 0.2F};
            LevelRenderer.renderLineBox(pose, lines, box, c[0], c[1], c[2], 1F);
            // door marker
            BlockPos door = pl.pos.relative(pl.facing, t.radius);
            LevelRenderer.renderLineBox(pose, lines, new AABB(door).inflate(0.1), 1F, 1F, 1F, 1F);
        }

        // selection rings & core beacons
        if (rts) {
            for (int id : RtsScreen.SELECTION) {
                Entity e = mc.level.getEntity(id);
                if (!(e instanceof SoldierEntity s)) continue;
                double ex = Mth.lerp(event.getPartialTick(), s.xOld, s.getX());
                double ey = Mth.lerp(event.getPartialTick(), s.yOld, s.getY());
                double ez = Mth.lerp(event.getPartialTick(), s.zOld, s.getZ());
                ring(pose, lines, ex, ey + 0.05, ez, 0.65, 0.2F, 1F, 0.3F);
            }
            for (Province p : d.provinces.values()) {
                if (p.core == null) continue;
                double dx = p.core.getX() - cam.x, dz = p.core.getZ() - cam.z;
                if (dx * dx + dz * dz > 220 * 220) continue;
                float[] c = ClientState.rgbOf(p.owner);
                for (int k = 0; k < 3; k++) {
                    double off = 0.4 + k * 0.1;
                    LevelRenderer.renderLineBox(pose, lines, new AABB(p.core.getX() + 0.5 - off, p.core.getY() + 1, p.core.getZ() + 0.5 - off,
                            p.core.getX() + 0.5 + off, p.core.getY() + 40, p.core.getZ() + 0.5 + off), c[0], c[1], c[2], 0.8F);
                }
            }
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());

        if (rts) renderPlayer(event, mc);
    }

    private static boolean isInside(Vec3 p, Building b) {
        return b.containsXZ(p.x, p.z, 0) && p.y >= b.minY() - 1 && p.y <= b.maxY() + 2;
    }

    private static void ring(PoseStack pose, VertexConsumer lines, double x, double y, double z, double r, float cr, float cg, float cb) {
        Matrix4f m = pose.last().pose();
        org.joml.Matrix3f n = pose.last().normal();
        int seg = 20;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            float x0 = (float) (x + Math.cos(a0) * r), z0 = (float) (z + Math.sin(a0) * r);
            float x1 = (float) (x + Math.cos(a1) * r), z1 = (float) (z + Math.sin(a1) * r);
            float nx = x1 - x0, nz = z1 - z0;
            float len = Mth.sqrt(nx * nx + nz * nz);
            lines.vertex(m, x0, (float) y, z0).color(cr, cg, cb, 1F).normal(n, nx / len, 0, nz / len).endVertex();
            lines.vertex(m, x1, (float) y, z1).color(cr, cg, cb, 1F).normal(n, nx / len, 0, nz / len).endVertex();
        }
    }

    /** The commander's own body is not drawn when the camera is detached, so draw it ourselves. */
    private static void renderPlayer(RenderLevelStageEvent event, Minecraft mc) {
        if (mc.player == null) return;
        var erd = mc.getEntityRenderDispatcher();
        float pt = event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        double x = Mth.lerp(pt, mc.player.xOld, mc.player.getX()) - cam.x;
        double y = Mth.lerp(pt, mc.player.yOld, mc.player.getY()) - cam.y;
        double z = Mth.lerp(pt, mc.player.zOld, mc.player.getZ()) - cam.z;
        float yaw = Mth.lerp(pt, mc.player.yRotO, mc.player.getYRot());
        MultiBufferSource.BufferSource bs = mc.renderBuffers().bufferSource();
        erd.render(mc.player, x, y, z, yaw, pt, event.getPoseStack(), bs, erd.getPackedLightCoords(mc.player, pt));
        bs.endBatch();
    }

    // ------------------------------------------------------------------ border curtains

    private static int heightAt(ClientLevel level, double x, double z) {
        int bx = Mth.floor(x), bz = Mth.floor(z);
        if (!level.hasChunkAt(new BlockPos(bx, 64, bz))) return Integer.MIN_VALUE;
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
    }

    private static int owner(GameData d, int pid) {
        Province p = d.provinces.get(pid);
        return p == null ? -1 : p.owner;
    }

    private static void rebuildBorders(ClientLevel level, GameData d, double cx, double cz, double range) {
        int key = ClientState.territoryVersion * 31 + ClientState.stateVersion;
        long now = System.currentTimeMillis();
        boolean moved = Double.isNaN(cacheX) || Math.abs(cx - cacheX) > 12 || Math.abs(cz - cacheZ) > 12;
        if (key == cacheKey && !moved && now - cacheTime < 3000) return;
        cacheKey = key;
        cacheX = cx;
        cacheZ = cz;
        cacheTime = now;
        SEGMENTS.clear();
        int n = d.gridN;
        if (n <= 0 || d.cells.length != n * n) return;
        int c0x = Math.max(0, d.cellX(cx - range)), c1x = Math.min(n - 1, d.cellX(cx + range));
        int c0z = Math.max(0, d.cellZ(cz - range)), c1z = Math.min(n - 1, d.cellZ(cz + range));
        int cs = d.cellSize;
        int step = Math.max(2, cs / 4);
        for (int z = c0z; z <= c1z; z++) {
            for (int x = c0x; x <= c1x; x++) {
                int a = d.cells[z * n + x];
                // east edge
                int b = x + 1 < n ? d.cells[z * n + x + 1] : Integer.MIN_VALUE;
                if (b != a && (a >= 0 || b >= 0)) {
                    double ex = d.minX() + (x + 1) * cs;
                    addEdge(level, d, a, b, ex, d.minZ() + z * cs, ex, d.minZ() + (z + 1) * cs, step);
                }
                int c = z + 1 < n ? d.cells[(z + 1) * n + x] : Integer.MIN_VALUE;
                if (c != a && (a >= 0 || c >= 0)) {
                    double ez = d.minZ() + (z + 1) * cs;
                    addEdge(level, d, a, c, d.minX() + x * cs, ez, d.minX() + (x + 1) * cs, ez, step);
                }
                if (x == 0 && a >= 0) addEdge(level, d, a, Integer.MIN_VALUE, d.minX(), d.minZ() + z * cs, d.minX(), d.minZ() + (z + 1) * cs, step);
                if (z == 0 && a >= 0) addEdge(level, d, a, Integer.MIN_VALUE, d.minX() + x * cs, d.minZ(), d.minX() + (x + 1) * cs, d.minZ(), step);
            }
        }
    }

    private static void addEdge(ClientLevel level, GameData d, int pa, int pb, double x0, double z0, double x1, double z1, int step) {
        int oa = pa == Integer.MIN_VALUE ? Integer.MIN_VALUE : owner(d, pa);
        int ob = pb == Integer.MIN_VALUE ? Integer.MIN_VALUE : owner(d, pb);
        int color;
        if (oa == ob) {
            color = 0x60FFFFFF;
        } else if (oa >= 0 && ob >= 0 && d.atWar(oa, ob)) {
            color = 0xC0FF2020;
        } else {
            int main = oa >= 0 ? oa : ob;
            color = 0xA0000000 | ClientState.colorOf(main);
        }
        double len = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        int parts = Math.max(1, (int) Math.round(len / step));
        for (int i = 0; i < parts; i++) {
            double t0 = i / (double) parts, t1 = (i + 1) / (double) parts;
            double sx0 = x0 + (x1 - x0) * t0, sz0 = z0 + (z1 - z0) * t0;
            double sx1 = x0 + (x1 - x0) * t1, sz1 = z0 + (z1 - z0) * t1;
            int y = heightAt(level, (sx0 + sx1) / 2, (sz0 + sz1) / 2);
            if (y == Integer.MIN_VALUE) continue;
            SEGMENTS.add(new Segment((float) sx0, (float) sz0, (float) sx1, (float) sz1, y, color));
        }
    }

    private static void drawBorders(PoseStack pose, Vec3 cam, float alphaMul) {
        if (SEGMENTS.isEmpty()) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = pose.last().pose();
        long t = System.currentTimeMillis();
        float pulse = 0.75F + 0.25F * Mth.sin(t / 250F);
        for (Segment s : SEGMENTS) {
            int c = s.color;
            float a = (c >>> 24) / 255F * alphaMul;
            if ((c & 0xFFFFFF) == 0xFF2020) a *= pulse;
            float r = (c >> 16 & 255) / 255F, g = (c >> 8 & 255) / 255F, b = (c & 255) / 255F;
            float y0 = (float) (s.y - 0.5 - cam.y), y1 = (float) (s.y + 2.5 - cam.y);
            float ax = (float) (s.x0 - cam.x), az = (float) (s.z0 - cam.z), bx = (float) (s.x1 - cam.x), bz = (float) (s.z1 - cam.z);
            bb.vertex(m, ax, y0, az).color(r, g, b, a).endVertex();
            bb.vertex(m, bx, y0, bz).color(r, g, b, a).endVertex();
            bb.vertex(m, bx, y1, bz).color(r, g, b, 0F).endVertex();
            bb.vertex(m, ax, y1, az).color(r, g, b, 0F).endVertex();
        }
        Tesselator.getInstance().end();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void invalidate() {
        cacheKey = -1;
    }
}
