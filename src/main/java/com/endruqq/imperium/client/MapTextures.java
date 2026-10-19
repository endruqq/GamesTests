package com.endruqq.imperium.client;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.Province;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Dynamic textures for the strategic map: explored terrain + political overlay. */
public final class MapTextures {
    public static final ResourceLocation TERRAIN = new ResourceLocation(Imperium.MODID, "dynamic/terrain");
    public static final ResourceLocation POLITICAL = new ResourceLocation(Imperium.MODID, "dynamic/political");
    public static final int PX_PER_CELL = 4;
    private static final int PARCHMENT = 0xFFC9B48A;

    private static DynamicTexture terrain;
    private static DynamicTexture political;
    public static int terrainRes;
    public static int terrainScale = 1;
    public static int politicalRes;
    private static int arenaKey;
    private static int politicalKey = -1;
    private static boolean terrainDirty;
    private static int ticks;
    private static final Map<Long, Long> SCANNED = new HashMap<>();

    private MapTextures() {}

    public static boolean ready() {
        return terrain != null && political != null;
    }

    private static int abgr(int argb) {
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        return a << 24 | b << 16 | g << 8 | r;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        GameData d = ClientState.data;
        if (mc.level == null || !ClientState.hasData || !d.arenaSet) return;
        ticks++;
        ensureTerrain(d);
        int key = Objects.hash(ClientState.stateVersion, ClientState.territoryVersion);
        if (key != politicalKey) {
            politicalKey = key;
            rebuildPolitical(d);
        }
        scanSomeChunks(mc, d);
        if (terrainDirty && ticks % 10 == 0) {
            terrain.upload();
            terrainDirty = false;
        }
    }

    private static void ensureTerrain(GameData d) {
        int key = Objects.hash(d.centerX, d.centerZ, d.size);
        if (terrain != null && key == arenaKey) return;
        arenaKey = key;
        terrainScale = Math.max(1, (int) Math.ceil(d.size / 1024.0));
        terrainRes = (int) Math.ceil(d.size / (double) terrainScale);
        NativeImage img = new NativeImage(terrainRes, terrainRes, true);
        img.fillRect(0, 0, terrainRes, terrainRes, abgr(PARCHMENT));
        if (terrain != null) terrain.close();
        terrain = new DynamicTexture(img);
        Minecraft.getInstance().getTextureManager().register(TERRAIN, terrain);
        SCANNED.clear();
    }

    private static void scanSomeChunks(Minecraft mc, GameData d) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        int pcx = mc.player.chunkPosition().x, pcz = mc.player.chunkPosition().z;
        int radius = Math.min(mc.options.getEffectiveRenderDistance(), 16);
        long now = System.currentTimeMillis();
        int budget = 3;
        for (int r = 0; r <= radius && budget > 0; r++) {
            for (int dx = -r; dx <= r && budget > 0; dx++) {
                for (int dz = -r; dz <= r && budget > 0; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int cx = pcx + dx, cz = pcz + dz;
                    long k = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
                    Long last = SCANNED.get(k);
                    if (last != null && now - last < (r <= 2 ? 5000 : 30000)) continue;
                    if (!level.getChunkSource().hasChunk(cx, cz)) continue;
                    SCANNED.put(k, now);
                    scanChunk(level, d, cx, cz);
                    budget--;
                }
            }
        }
    }

    private static void scanChunk(ClientLevel level, GameData d, int cx, int cz) {
        int minX = d.minX(), minZ = d.minZ();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        NativeImage img = terrain.getPixels();
        if (img == null) return;
        for (int x = cx * 16; x < cx * 16 + 16; x++) {
            int rx = x - minX;
            if (rx < 0 || rx % terrainScale != 0) continue;
            int px = rx / terrainScale;
            if (px >= terrainRes) continue;
            for (int z = cz * 16; z < cz * 16 + 16; z++) {
                int rz = z - minZ;
                if (rz < 0 || rz % terrainScale != 0) continue;
                int pz = rz / terrainScale;
                if (pz >= terrainRes) continue;
                img.setPixelRGBA(px, pz, abgr(colorAt(level, m, x, z)));
                terrainDirty = true;
            }
        }
    }

    private static int colorAt(ClientLevel level, BlockPos.MutableBlockPos m, int x, int z) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        m.set(x, y, z);
        BlockState s = level.getBlockState(m);
        int depth = 0;
        if (!s.getFluidState().isEmpty() && s.getFluidState().is(FluidTags.WATER)) {
            BlockPos.MutableBlockPos w = m.mutable();
            while (depth < 12 && !level.getBlockState(w).getFluidState().isEmpty()) {
                w.move(0, -1, 0);
                depth++;
            }
        }
        MapColor col = s.getMapColor(level, m);
        int rgb = col == MapColor.NONE ? 0x7FB238 : col.col;
        double shade;
        if (depth > 0) {
            shade = 1.05 - Math.min(0.45, depth * 0.05);
        } else {
            int yn = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z - 1) - 1;
            shade = y > yn ? 1.1 : y < yn ? 0.82 : 0.96;
        }
        int r = clamp((int) ((rgb >> 16 & 255) * shade)), g = clamp((int) ((rgb >> 8 & 255) * shade)), b = clamp((int) ((rgb & 255) * shade));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static int ownerAt(GameData d, int cx, int cz) {
        if (cx < 0 || cz < 0 || cx >= d.gridN || cz >= d.gridN) return Integer.MIN_VALUE;
        int pid = d.cells[cz * d.gridN + cx];
        Province p = d.provinces.get(pid);
        return p == null ? -1 : p.owner;
    }

    private static int provAt(GameData d, int cx, int cz) {
        if (cx < 0 || cz < 0 || cx >= d.gridN || cz >= d.gridN) return Integer.MIN_VALUE;
        return d.cells[cz * d.gridN + cx];
    }

    private static void rebuildPolitical(GameData d) {
        int n = d.gridN;
        if (n <= 0 || d.cells.length != n * n) return;
        int res = n * PX_PER_CELL;
        if (political == null || politicalRes != res) {
            if (political != null) political.close();
            political = new DynamicTexture(new NativeImage(res, res, true));
            Minecraft.getInstance().getTextureManager().register(POLITICAL, political);
            politicalRes = res;
        }
        NativeImage img = political.getPixels();
        if (img == null) return;
        int P = PX_PER_CELL;
        for (int cz = 0; cz < n; cz++) {
            for (int cx = 0; cx < n; cx++) {
                int pid = d.cells[cz * n + cx];
                Province p = d.provinces.get(pid);
                int owner = p == null ? -1 : p.owner;
                Nation nat = d.nation(owner);
                int base = p == null ? 0 : nat == null ? 0x40FFFFFF : (0x58 << 24) | nat.rgb();
                for (int py = 0; py < P; py++) for (int px = 0; px < P; px++) img.setPixelRGBA(cx * P + px, cz * P + py, abgr(base));
                if (p == null) continue;
                int dark = nat == null ? 0xFF606060 : 0xFF000000 | darken(nat.rgb(), 0.55);
                int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int[] dd : dirs) {
                    int npid = provAt(d, cx + dd[0], cz + dd[1]);
                    if (npid == pid) continue;
                    int nowner = ownerAt(d, cx + dd[0], cz + dd[1]);
                    int color;
                    int thick;
                    if (nowner != owner) {
                        boolean war = owner >= 0 && nowner >= 0 && d.atWar(owner, nowner);
                        color = war ? 0xFFE01818 : dark;
                        thick = 2;
                    } else {
                        color = 0x99FFFFFF;
                        thick = 1;
                    }
                    for (int t = 0; t < thick; t++) {
                        for (int k = 0; k < P; k++) {
                            int px, py;
                            if (dd[0] == 1) { px = P - 1 - t; py = k; }
                            else if (dd[0] == -1) { px = t; py = k; }
                            else if (dd[1] == 1) { px = k; py = P - 1 - t; }
                            else { px = k; py = t; }
                            img.setPixelRGBA(cx * P + px, cz * P + py, abgr(color));
                        }
                    }
                }
            }
        }
        political.upload();
    }

    public static int darken(int rgb, double f) {
        int r = (int) ((rgb >> 16 & 255) * f), g = (int) ((rgb >> 8 & 255) * f), b = (int) ((rgb & 255) * f);
        return r << 16 | g << 8 | b;
    }

    public static void reset() {
        Minecraft mc = Minecraft.getInstance();
        if (terrain != null) mc.getTextureManager().release(TERRAIN);
        if (political != null) mc.getTextureManager().release(POLITICAL);
        terrain = null;
        political = null;
        arenaKey = 0;
        politicalKey = -1;
        SCANNED.clear();
    }
}
