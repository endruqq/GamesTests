package com.endruqq.imperium.client.screen;

import com.endruqq.imperium.client.ClientState;
import com.endruqq.imperium.client.Keys;
import com.endruqq.imperium.client.RtsCamera;
import com.endruqq.imperium.client.StrategicMap;
import com.endruqq.imperium.client.UiUtil;
import com.endruqq.imperium.entity.CitizenEntity;
import com.endruqq.imperium.entity.OrderType;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.Stance;
import com.endruqq.imperium.entity.UnitType;
import com.endruqq.imperium.game.BuildingManager;
import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.GameManager;
import com.endruqq.imperium.game.GamePhase;
import com.endruqq.imperium.game.MilitaryManager;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.Province;
import com.endruqq.imperium.game.RelationType;
import com.endruqq.imperium.game.ResourceType;
import com.endruqq.imperium.game.WarManager;
import com.endruqq.imperium.game.building.Building;
import com.endruqq.imperium.game.building.BuildingType;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.game.building.Requirement;
import com.endruqq.imperium.network.ActionPacket;
import com.endruqq.imperium.network.ImperiumNet;
import com.endruqq.imperium.network.UnitsPacket;
import com.endruqq.imperium.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The strategy ("RTS") view: top-down camera, political map, building, army control, editor. */
public class RtsScreen extends Screen {
    public static final Set<Integer> SELECTION = new LinkedHashSet<>();
    private static final Map<Integer, Set<Integer>> GROUPS = new HashMap<>();
    public static Placement placement;
    public static int editorProvince = -1;

    private enum Tab { BUILD, ARMY, EDITOR }

    private enum Tool { NONE, PAINT, ERASE, CORE }

    public static class Placement {
        public BuildingType type;
        public boolean prefab;
        public BlockPos pos;
        public boolean valid;
        public Direction facing = Direction.SOUTH;
        public Component error;
    }

    private static boolean mapMode;
    private static Tab tab = Tab.BUILD;
    private static boolean prefabMode = true;
    private static int editorNation = -1;
    private static int nationsCount = 4;
    private static int provincesCount = 4;
    private static Tool tool = Tool.NONE;
    private static double savedMapX = Double.NaN, savedMapZ, savedMapZoom;

    private static final int PANEL_W = 268;
    private static final int PANEL_H = 128;
    private static final int MINI = 116;

    private final StrategicMap bigMap = new StrategicMap();
    private final StrategicMap miniMap = new StrategicMap();
    private int panelX, panelY;

    // mouse state
    private boolean leftDown;
    private boolean boxSelecting;
    private double downX, downY;
    private boolean middleDown;
    private double lastMX, lastMY;
    private boolean painting;
    private final List<Integer> paintBuffer = new ArrayList<>();
    private long lastClickTime;
    private int lastClickEntity = -1;
    private Vec3 orderMarker;
    private long orderMarkerTime;
    private Entity hoverEntity;
    private int infoProvince = -1;
    private EditBox nameBox;
    private Button stanceButton;

    public RtsScreen() {
        super(Component.translatable("imperium.ui.rts"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        if (!RtsCamera.active) RtsCamera.enter();
        panelX = 4;
        panelY = height - PANEL_H - 4;
        bigMap.setRect(0, 18, width, height - 18);
        if (Double.isNaN(savedMapX)) {
            bigMap.fitArena(ClientState.data);
            if (!ClientState.data.arenaSet && minecraft.player != null) {
                bigMap.centerX = minecraft.player.getX();
                bigMap.centerZ = minecraft.player.getZ();
            }
        } else {
            bigMap.centerX = savedMapX;
            bigMap.centerZ = savedMapZ;
            bigMap.zoom = savedMapZoom;
        }
        miniMap.setRect(width - MINI - 4, height - MINI - 4, MINI, MINI);
        if (editorNation < 0 || ClientState.data.nation(editorNation) == null) {
            editorNation = ClientState.data.nations.isEmpty() ? -1 : ClientState.data.nations.keySet().iterator().next();
        }

        // side buttons
        int bx = width - 74, by = 20;
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.btn.map"), b -> toggleMap()).bounds(bx, by, 70, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.btn.tech"), b -> minecraft.setScreen(new TechTreeScreen(this))).bounds(bx, by + 16, 70, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.btn.nation"), b -> minecraft.setScreen(new NationScreen(this, 0))).bounds(bx, by + 32, 70, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.btn.diplomacy"), b -> minecraft.setScreen(new NationScreen(this, 1))).bounds(bx, by + 48, 70, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.btn.exit"), b -> onClose()).bounds(bx, by + 64, 70, 14).build());

        // tabs
        int tx = panelX;
        addRenderableWidget(tabButton(Tab.BUILD, "imperium.ui.tab.build", tx));
        addRenderableWidget(tabButton(Tab.ARMY, "imperium.ui.tab.army", tx + 64));
        if (ClientState.isAdmin()) addRenderableWidget(tabButton(Tab.EDITOR, "imperium.ui.tab.editor", tx + 128));
        if (tab == Tab.EDITOR && !ClientState.isAdmin()) tab = Tab.BUILD;

        int px = panelX + 4, py = panelY + 4;
        switch (tab) {
            case BUILD -> {
                addRenderableWidget(Button.builder(Component.translatable("imperium.ui.prefab"), b -> {
                    prefabMode = true;
                    rebuildWidgets();
                }).bounds(px, py, 90, 14).build()).active = !prefabMode;
                addRenderableWidget(Button.builder(Component.translatable("imperium.ui.manual"), b -> {
                    prefabMode = false;
                    rebuildWidgets();
                }).bounds(px + 92, py, 90, 14).build()).active = prefabMode;
            }
            case ARMY -> {
                int oy = py + 44;
                addRenderableWidget(Button.builder(Component.translatable("imperium.ui.order.hold"), b -> order(OrderType.HOLD))
                        .bounds(px, oy, 62, 14).build());
                addRenderableWidget(Button.builder(Component.translatable("imperium.ui.order.follow"), b -> order(OrderType.FOLLOW))
                        .bounds(px + 64, oy, 62, 14).build());
                addRenderableWidget(Button.builder(Component.translatable("imperium.ui.order.return"), b -> order(OrderType.RETURN))
                        .bounds(px + 128, oy, 62, 14).build());
                addRenderableWidget(Button.builder(Component.translatable("imperium.ui.order.dismiss"), b -> dismiss())
                        .bounds(px + 192, oy, 64, 14).build());
                stanceButton = addRenderableWidget(Button.builder(stanceLabel(), b -> cycleStance()).bounds(px, oy + 16, 126, 14).build());
                addRenderableWidget(Button.builder(Component.translatable("imperium.ui.select_all"), b -> selectAll())
                        .bounds(px + 128, oy + 16, 128, 14).build());
            }
            case EDITOR -> initEditor(px, py);
        }
    }

    private Button tabButton(Tab t, String key, int x) {
        Button b = Button.builder(Component.translatable(key), btn -> {
            tab = t;
            placement = null;
            tool = Tool.NONE;
            rebuildWidgets();
        }).bounds(x, panelY - 15, 62, 14).build();
        b.active = tab != t;
        return b;
    }

    private void initEditor(int px, int py) {
        int x = px;
        int[] sizes = GameManager.ARENA_SIZES;
        for (int i = 0; i < sizes.length; i++) {
            final int size = sizes[i];
            String label = size >= 1000 ? (size % 1000 == 0 ? size / 1000 + "k" : (size / 100) / 10.0 + "k") : String.valueOf(size);
            Button b = addRenderableWidget(Button.builder(Component.literal(label), btn -> editor("arena", t -> {
                t.putInt("size", size);
                t.putBoolean("here", hasShiftDown());
            })).bounds(x, py, 30, 14).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("imperium.ui.editor.arena_tip", size))).build());
            b.active = !ClientState.data.arenaSet || ClientState.data.size != size;
            x += 31;
        }
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.here"), btn -> editor("arena", t -> {
            t.putInt("size", ClientState.data.arenaSet ? ClientState.data.size : 500);
            t.putBoolean("here", true);
        })).bounds(x, py, 40, 14).build());

        int y2 = py + 16;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> { nationsCount = Math.max(2, nationsCount - 1); rebuildWidgets(); }).bounds(px, y2, 12, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.nations", nationsCount), b -> { }).bounds(px + 13, y2, 62, 14).build()).active = false;
        addRenderableWidget(Button.builder(Component.literal("+"), b -> { nationsCount = Math.min(15, nationsCount + 1); rebuildWidgets(); }).bounds(px + 76, y2, 12, 14).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> { provincesCount = Math.max(1, provincesCount - 1); rebuildWidgets(); }).bounds(px + 92, y2, 12, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.provinces", provincesCount), b -> { }).bounds(px + 105, y2, 62, 14).build()).active = false;
        addRenderableWidget(Button.builder(Component.literal("+"), b -> { provincesCount = Math.min(12, provincesCount + 1); rebuildWidgets(); }).bounds(px + 168, y2, 12, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.random"), b -> editor("autodivide", t -> {
            t.putInt("nations", nationsCount);
            t.putInt("provinces", provincesCount);
        })).bounds(px + 184, y2, 76, 14).build());

        int y3 = py + 32;
        boolean running = ClientState.data.phase == GamePhase.RUNNING;
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.start").withStyle(ChatFormatting.GREEN), b -> editor("start", t -> { }))
                .bounds(px, y3, 64, 14).build()).active = !running;
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.stop"), b -> editor("stop", t -> { }))
                .bounds(px + 66, y3, 64, 14).build()).active = running;
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.reset"), b -> editor("reset", t -> { }))
                .bounds(px + 132, y3, 64, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.wipe").withStyle(ChatFormatting.RED), b -> {
            if (hasShiftDown()) editor("wipe", t -> { });
        }).bounds(px + 198, y3, 62, 14).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("imperium.ui.editor.wipe_tip"))).build());

        int y4 = py + 50;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleNation(-1)).bounds(px, y4, 12, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleNation(1)).bounds(px + 100, y4, 12, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.new"), b -> editor("create_nation", t -> t.putString("name", "")))
                .bounds(px + 114, y4, 36, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.delete"), b -> editor("delete_nation", t -> t.putInt("nation", editorNation)))
                .bounds(px + 151, y4, 36, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.color"), b -> editor("color", t -> t.putInt("nation", editorNation)))
                .bounds(px + 188, y4, 36, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.bot"), b -> editor("bot", t -> t.putInt("nation", editorNation)))
                .bounds(px + 225, y4, 35, 14).build());

        int y5 = py + 66;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleProvince(-1)).bounds(px, y5, 12, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleProvince(1)).bounds(px + 100, y5, 12, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.new"), b -> editor("create_province", t -> t.putInt("nation", editorNation)))
                .bounds(px + 114, y5, 36, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.delete"), b -> editor("delete_province", t -> t.putInt("province", editorProvince)))
                .bounds(px + 151, y5, 36, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.owner"), b -> editor("province_owner", t -> {
            t.putInt("province", editorProvince);
            t.putInt("nation", editorNation);
        })).bounds(px + 188, y5, 36, 14).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("imperium.ui.editor.owner_tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.capital"), b -> editor("capital", t -> t.putInt("province", editorProvince)))
                .bounds(px + 225, y5, 35, 14).build());

        int y6 = py + 82;
        addRenderableWidget(toolButton(Tool.PAINT, "imperium.ui.editor.paint", px, y6));
        addRenderableWidget(toolButton(Tool.ERASE, "imperium.ui.editor.erase", px + 66, y6));
        addRenderableWidget(toolButton(Tool.CORE, "imperium.ui.editor.core", px + 132, y6));
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.none"), b -> { tool = Tool.NONE; rebuildWidgets(); })
                .bounds(px + 198, y6, 62, 14).build()).active = tool != Tool.NONE;

        int y7 = py + 100;
        nameBox = new EditBox(font, px, y7, 150, 14, Component.translatable("imperium.ui.editor.name"));
        nameBox.setMaxLength(40);
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.rename_nation"), b -> {
            String v = nameBox.getValue();
            editor("rename_nation", t -> { t.putInt("nation", editorNation); t.putString("name", v); });
        }).bounds(px + 152, y7, 54, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("imperium.ui.editor.rename_province"), b -> {
            String v = nameBox.getValue();
            editor("rename_province", t -> { t.putInt("province", editorProvince); t.putString("name", v); });
        }).bounds(px + 207, y7, 54, 14).build());
    }

    private Button toolButton(Tool t, String key, int x, int y) {
        Button b = Button.builder(Component.translatable(key), btn -> {
            tool = t;
            if (!mapMode) toggleMap();
            rebuildWidgets();
        }).bounds(x, y, 64, 14).build();
        b.active = tool != t;
        return b;
    }

    private void cycleNation(int dir) {
        List<Integer> ids = new ArrayList<>(ClientState.data.nations.keySet());
        if (ids.isEmpty()) return;
        int i = ids.indexOf(editorNation);
        editorNation = ids.get(Math.floorMod(i + dir, ids.size()));
    }

    private void cycleProvince(int dir) {
        List<Integer> ids = new ArrayList<>();
        for (Province p : ClientState.data.provinces.values()) if (p.owner == editorNation || editorNation < 0) ids.add(p.id);
        if (ids.isEmpty()) {
            for (Province p : ClientState.data.provinces.values()) ids.add(p.id);
        }
        if (ids.isEmpty()) return;
        int i = ids.indexOf(editorProvince);
        editorProvince = ids.get(Math.floorMod(i + dir, ids.size()));
        Province p = ClientState.data.provinces.get(editorProvince);
        if (p != null && p.cellCount > 0 && mapMode) {
            bigMap.centerX = p.centroidX;
            bigMap.centerZ = p.centroidZ;
        }
    }

    private interface TagFiller {
        void fill(CompoundTag t);
    }

    private void editor(String op, TagFiller f) {
        CompoundTag t = new CompoundTag();
        t.putString("op", op);
        f.fill(t);
        ImperiumNet.sendToServer(new ActionPacket("editor", t));
    }

    private static void send(String action, CompoundTag t) {
        ImperiumNet.sendToServer(new ActionPacket(action, t));
    }

    // ------------------------------------------------------------------ unit commands

    private int[] selectionIds() {
        return SELECTION.stream().mapToInt(Integer::intValue).toArray();
    }

    private void order(OrderType o) {
        if (SELECTION.isEmpty()) return;
        CompoundTag t = new CompoundTag();
        t.putIntArray("ids", selectionIds());
        t.putInt("order", o.ordinal());
        send("command", t);
    }

    private void orderAt(OrderType o, Vec3 pos, int entity, int province) {
        if (SELECTION.isEmpty()) return;
        CompoundTag t = new CompoundTag();
        t.putIntArray("ids", selectionIds());
        t.putInt("order", o.ordinal());
        t.putDouble("x", pos.x);
        t.putDouble("y", pos.y);
        t.putDouble("z", pos.z);
        t.putInt("entity", entity);
        t.putInt("province", province);
        send("command", t);
        orderMarker = pos;
        orderMarkerTime = System.currentTimeMillis();
    }

    private Stance currentStance() {
        if (minecraft.level != null) {
            for (int id : SELECTION) {
                if (minecraft.level.getEntity(id) instanceof SoldierEntity s) return s.getStance();
            }
        }
        return Stance.DEFENSIVE;
    }

    private Component stanceLabel() {
        return Component.translatable("imperium.ui.stance", Component.translatable(currentStance().key()));
    }

    private void cycleStance() {
        if (SELECTION.isEmpty()) return;
        Stance next = Stance.byOrdinal((currentStance().ordinal() + 1) % Stance.values().length);
        CompoundTag t = new CompoundTag();
        t.putIntArray("ids", selectionIds());
        t.putInt("stance", next.ordinal());
        send("stance", t);
    }

    private void dismiss() {
        if (SELECTION.isEmpty() || !hasShiftDown()) {
            if (minecraft.player != null) minecraft.player.displayClientMessage(Component.translatable("imperium.ui.dismiss_hint"), true);
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putIntArray("ids", selectionIds());
        send("dismiss", t);
        SELECTION.clear();
    }

    private void selectAll() {
        SELECTION.clear();
        int me = ClientState.myNationId();
        for (UnitsPacket.Unit u : ClientState.units) if (u.type >= 0 && u.nation == me) SELECTION.add(u.entityId);
        if (minecraft.level != null) {
            for (Entity e : minecraft.level.entitiesForRendering()) {
                if (e instanceof SoldierEntity s && s.getNationId() == me && me >= 0) SELECTION.add(s.getId());
            }
        }
    }

    private void recruit(UnitType type, int count) {
        CompoundTag t = new CompoundTag();
        t.putInt("unit", type.ordinal());
        t.putInt("count", count);
        send("recruit", t);
    }

    // ------------------------------------------------------------------ map / camera

    private void toggleMap() {
        mapMode = !mapMode;
        RtsCamera.keyboardPan = !mapMode;
        if (mapMode && Double.isNaN(savedMapX)) bigMap.fitArena(ClientState.data);
    }

    @Override
    public void tick() {
        RtsCamera.keyboardPan = !mapMode;
        if (mapMode && !(getFocused() instanceof EditBox)) {
            long win = minecraft.getWindow().getWindow();
            double speed = 6 / bigMap.zoom * (hasShiftDown() ? 2.5 : 1);
            if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(win, GLFW.GLFW_KEY_W)) bigMap.centerZ -= speed;
            if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(win, GLFW.GLFW_KEY_S)) bigMap.centerZ += speed;
            if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(win, GLFW.GLFW_KEY_A)) bigMap.centerX -= speed;
            if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(win, GLFW.GLFW_KEY_D)) bigMap.centerX += speed;
        }
        savedMapX = bigMap.centerX;
        savedMapZ = bigMap.centerZ;
        savedMapZoom = bigMap.zoom;
        if (stanceButton != null) stanceButton.setMessage(stanceLabel());
        if (!paintBuffer.isEmpty() && minecraft.level != null && minecraft.level.getGameTime() % 4 == 0) flushPaint();
        // prune selection
        if (minecraft.level != null && minecraft.level.getGameTime() % 20 == 0) {
            Set<Integer> known = new java.util.HashSet<>();
            for (UnitsPacket.Unit u : ClientState.units) known.add(u.entityId);
            SELECTION.removeIf(id -> {
                Entity e = minecraft.level.getEntity(id);
                return (e == null || !e.isAlive()) && !known.contains(id);
            });
        }
    }

    // ------------------------------------------------------------------ input

    private boolean overUi(double mx, double my) {
        if (my < 18) return true;
        if (UiUtil.inside(mx, my, panelX, panelY - 16, PANEL_W, PANEL_H + 16)) return true;
        if (UiUtil.inside(mx, my, width - 76, 18, 76, 82)) return true;
        return !mapMode && miniMap.contains(mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        setFocused(null);
        if (handlePanelClick(mx, my, button)) return true;
        if (!mapMode && miniMap.contains(mx, my)) {
            double wx = miniMap.toWorldX(mx), wz = miniMap.toWorldZ(my);
            if (button == 0) RtsCamera.lookAt(wx, wz);
            else if (button == 1) mapOrder(miniMap, mx, my, wx, wz);
            return true;
        }
        if (overUi(mx, my)) return true;
        lastMX = mx;
        lastMY = my;
        if (button == 2) {
            middleDown = true;
            return true;
        }
        if (mapMode) return mapClick(mx, my, button);
        return worldClick(mx, my, button);
    }

    private boolean worldClick(double mx, double my, int button) {
        if (placement != null) {
            if (button == 0 && placement.pos != null) {
                if (placement.valid) {
                    CompoundTag t = new CompoundTag();
                    t.putString("type", placement.type.id);
                    t.putLong("pos", placement.pos.asLong());
                    t.putBoolean("prefab", placement.prefab);
                    t.putInt("facing", placement.facing.get2DDataValue());
                    send("place_building", t);
                    if (!hasShiftDown()) placement = null;
                } else if (minecraft.player != null && placement.error != null) {
                    minecraft.player.displayClientMessage(placement.error.copy().withStyle(ChatFormatting.RED), true);
                }
            } else if (button == 1) {
                placement = null;
            }
            return true;
        }
        if (button == 0) {
            leftDown = true;
            boxSelecting = false;
            downX = mx;
            downY = my;
            return true;
        }
        if (button == 1) {
            worldRightClick(mx, my);
            return true;
        }
        return false;
    }

    private boolean isEnemyEntity(Entity e) {
        if (!(e instanceof LivingEntity) || !e.isAlive()) return false;
        if (e instanceof Enemy && !(e instanceof SoldierEntity)) return true;
        int me = ClientState.myNationId();
        int other = -1;
        if (e instanceof SoldierEntity s) other = s.getNationId();
        else if (e instanceof CitizenEntity c) other = c.getNationId();
        else if (e instanceof Player p) other = ClientState.data.nationIdOf(p.getUUID());
        return other >= 0 && ClientState.data.atWar(me, other) && !(e instanceof CitizenEntity);
    }

    private void worldRightClick(double mx, double my) {
        if (SELECTION.isEmpty()) return;
        Entity e = RtsCamera.pickEntity(mx, my);
        if (e != null && isEnemyEntity(e)) {
            orderAt(OrderType.ATTACK_TARGET, e.position(), e.getId(), -1);
            return;
        }
        BlockHitResult hit = RtsCamera.pickBlock(mx, my);
        if (hit == null || minecraft.level == null) return;
        BlockPos bp = hit.getBlockPos();
        if (minecraft.level.getBlockState(bp).is(ModBlocks.PROVINCE_CORE.get())) {
            Province p = WarManager.provinceWithCore(ClientState.data, bp);
            if (p != null) {
                orderAt(OrderType.ATTACK_CORE, Vec3.atCenterOf(bp), -1, p.id);
                return;
            }
        }
        Vec3 loc = hit.getLocation();
        Vec3 target = new Vec3(loc.x, bp.getY() + (hit.getDirection() == Direction.UP ? 1 : 0), loc.z);
        orderAt(hasAltDown() ? OrderType.MOVE : OrderType.ATTACK_MOVE, target, -1, -1);
    }

    private Province coreNear(StrategicMap map, double mx, double my) {
        for (Province p : ClientState.data.provinces.values()) {
            if (p.core == null) continue;
            double sx = map.toScreenX(p.core.getX() + 0.5), sy = map.toScreenY(p.core.getZ() + 0.5);
            if (Math.abs(sx - mx) <= 6 && Math.abs(sy - my) <= 6) return p;
        }
        return null;
    }

    private void mapOrder(StrategicMap map, double mx, double my, double wx, double wz) {
        if (SELECTION.isEmpty()) return;
        Province core = coreNear(map, mx, my);
        int me = ClientState.myNationId();
        if (core != null && core.owner != me && (core.owner < 0 || ClientState.data.atWar(me, core.owner))) {
            orderAt(OrderType.ATTACK_CORE, Vec3.atCenterOf(core.core), -1, core.id);
            return;
        }
        double y = -1000;
        if (minecraft.level != null && minecraft.level.hasChunkAt(BlockPos.containing(wx, 64, wz))) {
            y = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(wx), Mth.floor(wz));
        }
        orderAt(hasAltDown() ? OrderType.MOVE : OrderType.ATTACK_MOVE, new Vec3(wx, y, wz), -1, -1);
    }

    private boolean mapClick(double mx, double my, int button) {
        double wx = bigMap.toWorldX(mx), wz = bigMap.toWorldZ(my);
        if (tool != Tool.NONE && ClientState.isAdmin()) {
            if (tool == Tool.CORE && button == 0) {
                editor("core", t -> {
                    t.putInt("province", editorProvince);
                    t.putInt("x", Mth.floor(wx));
                    t.putInt("z", Mth.floor(wz));
                });
            } else if (button == 0 || button == 1) {
                painting = true;
                paintAt(mx, my, button == 1 || tool == Tool.ERASE);
            }
            return true;
        }
        if (button == 1) {
            mapOrder(bigMap, mx, my, wx, wz);
            return true;
        }
        if (button == 0) {
            leftDown = true;
            boxSelecting = false;
            downX = mx;
            downY = my;
        }
        return true;
    }

    private void paintAt(double mx, double my, boolean erase) {
        GameData d = ClientState.data;
        if (!d.arenaSet || (!erase && editorProvince < 0)) return;
        double wx = bigMap.toWorldX(mx), wz = bigMap.toWorldZ(my);
        int brush = hasShiftDown() ? 2 : hasControlDown() ? 4 : 0;
        int ccx = d.cellX(wx), ccz = d.cellZ(wz);
        int value = erase ? -1 : editorProvince;
        boolean changed = false;
        for (int dx = -brush; dx <= brush; dx++) {
            for (int dz = -brush; dz <= brush; dz++) {
                int cx = ccx + dx, cz = ccz + dz;
                if (cx < 0 || cz < 0 || cx >= d.gridN || cz >= d.gridN) continue;
                int idx = cz * d.gridN + cx;
                if (d.cells[idx] != value) {
                    d.cells[idx] = value;
                    paintBuffer.add(idx);
                    changed = true;
                }
            }
        }
        if (changed) ClientState.territoryVersion++;
    }

    private void flushPaint() {
        if (paintBuffer.isEmpty()) return;
        int value = tool == Tool.ERASE ? -1 : editorProvince;
        int[] cells = paintBuffer.stream().mapToInt(Integer::intValue).toArray();
        paintBuffer.clear();
        editor("paint", t -> {
            t.putInt("province", value);
            t.putIntArray("cells", cells);
        });
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (middleDown) {
            if (mapMode) {
                bigMap.centerX -= (mx - lastMX) / bigMap.zoom;
                bigMap.centerZ -= (my - lastMY) / bigMap.zoom;
            } else {
                RtsCamera.rotate(mx - lastMX, my - lastMY);
            }
            lastMX = mx;
            lastMY = my;
            return true;
        }
        if (painting) {
            paintAt(mx, my, button == 1 || tool == Tool.ERASE);
            return true;
        }
        if (leftDown) {
            if (Math.abs(mx - downX) > 4 || Math.abs(my - downY) > 4) boxSelecting = true;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 2) middleDown = false;
        if (painting && (button == 0 || button == 1)) {
            painting = false;
            flushPaint();
            return true;
        }
        if (button == 0 && leftDown) {
            leftDown = false;
            if (boxSelecting) {
                boxSelect(Math.min(downX, mx), Math.min(downY, my), Math.max(downX, mx), Math.max(downY, my));
            } else {
                clickSelect(mx, my);
            }
            boxSelecting = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    private void clickSelect(double mx, double my) {
        int me = ClientState.myNationId();
        if (!hasShiftDown()) SELECTION.clear();
        if (mapMode) {
            UnitsPacket.Unit best = null;
            double bd = 36;
            for (UnitsPacket.Unit u : ClientState.units) {
                if (u.type < 0 || u.nation != me) continue;
                double sx = bigMap.toScreenX(u.x) - mx, sy = bigMap.toScreenY(u.z) - my;
                double dd = sx * sx + sy * sy;
                if (dd < bd) {
                    bd = dd;
                    best = u;
                }
            }
            if (best != null) {
                toggle(best.entityId);
                infoProvince = -1;
            } else {
                infoProvince = ClientState.data.provinceIdAt(bigMap.toWorldX(mx), bigMap.toWorldZ(my));
            }
            return;
        }
        Entity e = RtsCamera.pickEntity(mx, my);
        if (e instanceof SoldierEntity s && s.getNationId() == me && me >= 0) {
            long now = System.currentTimeMillis();
            if (lastClickEntity == s.getId() && now - lastClickTime < 350) {
                // double click: all visible units of this type
                for (Entity o : minecraft.level.entitiesForRendering()) {
                    if (o instanceof SoldierEntity os && os.getNationId() == me && os.getUnitType() == s.getUnitType()
                            && RtsCamera.project(os.getX(), os.getY() + 1, os.getZ()) != null && os.distanceToSqr(s) < 48 * 48) {
                        SELECTION.add(os.getId());
                    }
                }
            } else {
                toggle(s.getId());
            }
            lastClickTime = now;
            lastClickEntity = s.getId();
        }
    }

    private void toggle(int id) {
        if (hasShiftDown() && SELECTION.contains(id)) SELECTION.remove(id);
        else SELECTION.add(id);
    }

    private void boxSelect(double x0, double y0, double x1, double y1) {
        int me = ClientState.myNationId();
        if (me < 0) return;
        if (mapMode) {
            for (UnitsPacket.Unit u : ClientState.units) {
                if (u.type < 0 || u.nation != me) continue;
                double sx = bigMap.toScreenX(u.x), sy = bigMap.toScreenY(u.z);
                if (sx >= x0 && sx <= x1 && sy >= y0 && sy <= y1) SELECTION.add(u.entityId);
            }
            return;
        }
        for (Entity e : minecraft.level.entitiesForRendering()) {
            if (!(e instanceof SoldierEntity s) || s.getNationId() != me) continue;
            double[] p = RtsCamera.project(s.getX(), s.getY() + 1, s.getZ());
            if (p != null && p[0] >= x0 && p[0] <= x1 && p[1] >= y0 && p[1] <= y1) SELECTION.add(s.getId());
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mapMode && !overUi(mx, my)) {
            bigMap.zoomAt(mx, my, delta > 0 ? 1.2 : 1 / 1.2);
            return true;
        }
        if (!mapMode && !overUi(mx, my)) {
            RtsCamera.zoom(delta);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (getFocused() instanceof EditBox eb && eb.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                setFocused(null);
                return true;
            }
            return super.keyPressed(key, scan, mods);
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (placement != null) {
                placement = null;
                return true;
            }
            if (tool != Tool.NONE) {
                tool = Tool.NONE;
                rebuildWidgets();
                return true;
            }
            onClose();
            return true;
        }
        if (Keys.RTS.matches(key, scan)) {
            onClose();
            return true;
        }
        if (Keys.TECH.matches(key, scan)) {
            minecraft.setScreen(new TechTreeScreen(this));
            return true;
        }
        if (Keys.NATION.matches(key, scan)) {
            minecraft.setScreen(new NationScreen(this, 0));
            return true;
        }
        boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            int g = key - GLFW.GLFW_KEY_0;
            if (ctrl) GROUPS.put(g, new LinkedHashSet<>(SELECTION));
            else {
                Set<Integer> grp = GROUPS.get(g);
                if (grp != null) {
                    SELECTION.clear();
                    SELECTION.addAll(grp);
                }
            }
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_M -> toggleMap();
            case GLFW.GLFW_KEY_SPACE -> {
                if (minecraft.player != null) {
                    RtsCamera.lookAt(minecraft.player.getX(), minecraft.player.getZ());
                    bigMap.centerX = minecraft.player.getX();
                    bigMap.centerZ = minecraft.player.getZ();
                }
            }
            case GLFW.GLFW_KEY_F -> focusSelection();
            case GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_X -> order(OrderType.HOLD);
            case GLFW.GLFW_KEY_V -> order(OrderType.FOLLOW);
            case GLFW.GLFW_KEY_B -> order(OrderType.RETURN);
            case GLFW.GLFW_KEY_T -> cycleStance();
            case GLFW.GLFW_KEY_R -> {
                if (placement != null) placement.facing = placement.facing.getClockWise();
            }
            case GLFW.GLFW_KEY_A -> {
                if (ctrl) selectAll();
            }
            default -> {
                return super.keyPressed(key, scan, mods);
            }
        }
        return true;
    }

    private void focusSelection() {
        double sx = 0, sz = 0;
        int n = 0;
        for (UnitsPacket.Unit u : ClientState.units) {
            if (SELECTION.contains(u.entityId)) {
                sx += u.x;
                sz += u.z;
                n++;
            }
        }
        if (n == 0) return;
        RtsCamera.lookAt(sx / n, sz / n);
        bigMap.centerX = sx / n;
        bigMap.centerZ = sz / n;
    }

    @Override
    public void onClose() {
        flushPaint();
        placement = null;
        RtsCamera.exit();
        super.onClose();
    }

    // ------------------------------------------------------------------ panel (build / army) clicks

    private List<BuildingType> buildList() {
        return new ArrayList<>(Buildings.all());
    }

    private int[] gridCell(int index) {
        int cols = (PANEL_W - 8) / 22;
        int gx = panelX + 4 + (index % cols) * 22;
        int gy = panelY + 22 + (index / cols) * 22;
        return new int[]{gx, gy};
    }

    private boolean handlePanelClick(double mx, double my, int button) {
        if (button != 0) return false;
        Nation n = ClientState.myNation();
        if (tab == Tab.BUILD) {
            List<BuildingType> list = buildList();
            for (int i = 0; i < list.size(); i++) {
                int[] c = gridCell(i);
                if (UiUtil.inside(mx, my, c[0], c[1], 20, 20)) {
                    BuildingType t = list.get(i);
                    if (n == null) return true;
                    if (prefabMode && t.prefabCost == null) return true;
                    Placement p = new Placement();
                    p.type = t;
                    p.prefab = prefabMode;
                    if (placement != null) p.facing = placement.facing;
                    placement = p;
                    if (mapMode) toggleMap();
                    return true;
                }
            }
        } else if (tab == Tab.ARMY) {
            UnitType[] types = UnitType.values();
            for (int i = 0; i < types.length; i++) {
                int x = panelX + 4 + i * 52, y = panelY + 6;
                if (UiUtil.inside(mx, my, x, y, 50, 34)) {
                    recruit(types[i], hasShiftDown() ? 5 : 1);
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        GameData d = ClientState.data;
        Nation n = ClientState.myNation();
        if (mapMode) {
            bigMap.draw(g, font, SELECTION, tool != Tool.NONE ? editorProvince : infoProvince, true);
            hoverEntity = null;
        } else {
            updatePlacement(mx, my, n);
            hoverEntity = overUi(mx, my) ? null : RtsCamera.pickEntity(mx, my);
            renderWorldLabels(g, d);
        }
        if (boxSelecting) {
            int x0 = (int) Math.min(downX, mx), y0 = (int) Math.min(downY, my);
            int x1 = (int) Math.max(downX, mx), y1 = (int) Math.max(downY, my);
            g.fill(x0, y0, x1, y1, 0x3044FF44);
            g.renderOutline(x0, y0, x1 - x0, y1 - y0, 0xFF66FF66);
        }
        renderOrderMarker(g);
        renderTopBar(g, d, n);
        renderPanel(g, d, n, mx, my);
        if (!mapMode) {
            if (d.arenaSet) {
                miniMap.fitArena(d);
            } else if (minecraft.player != null) {
                miniMap.centerX = minecraft.player.getX();
                miniMap.centerZ = minecraft.player.getZ();
                miniMap.zoom = 0.5;
            }
            UiUtil.panel(g, miniMap.x - 2, miniMap.y - 2, miniMap.w + 4, miniMap.h + 4);
            miniMap.draw(g, font, SELECTION, -1, false);
        }
        renderSelectionInfo(g, d);
        renderNotes(g);
        super.render(g, mx, my, pt);
        renderHints(g, n);
        renderTooltips(g, d, n, mx, my);
    }

    private void updatePlacement(int mx, int my, Nation n) {
        Placement p = placement;
        if (p == null) return;
        p.pos = null;
        if (overUi(mx, my) || minecraft.level == null) return;
        BlockHitResult hit = RtsCamera.pickBlock(mx, my);
        if (hit == null) return;
        BlockPos hp = hit.getBlockPos();
        int y = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hp.getX(), hp.getZ());
        p.pos = new BlockPos(hp.getX(), y, hp.getZ());
        if (n == null) {
            p.valid = false;
            p.error = Component.translatable("imperium.err.no_nation");
            return;
        }
        Component err = BuildingManager.validate(ClientState.data, n.id, p.type, p.pos);
        if (err == null && p.prefab && !n.stock.has(p.type.prefabCost)) {
            err = Component.translatable("imperium.err.cost", p.type.prefabCost.describe(1));
        }
        p.valid = err == null;
        p.error = err;
    }

    private void renderWorldLabels(GuiGraphics g, GameData d) {
        if (minecraft.level == null) return;
        int me = ClientState.myNationId();
        for (Entity e : minecraft.level.entitiesForRendering()) {
            if (!(e instanceof SoldierEntity s)) continue;
            if (s.distanceToSqr(RtsCamera.focusX, s.getY(), RtsCamera.focusZ) > 110 * 110) continue;
            double[] p = RtsCamera.project(s.getX(), s.getY() + s.getBbHeight() + 0.45, s.getZ());
            if (p == null) continue;
            int x = (int) p[0], y = (int) p[1];
            boolean sel = SELECTION.contains(s.getId());
            int col = 0xFF000000 | ClientState.colorOf(s.getNationId());
            float hp = s.getHealth() / Math.max(1, s.getMaxHealth());
            int bw = 16;
            g.fill(x - bw / 2 - 1, y - 1, x + bw / 2 + 1, y + 3, 0xFF000000);
            g.fill(x - bw / 2, y, x - bw / 2 + (int) (bw * hp), y + 2, hp > 0.5 ? 0xFF30E030 : hp > 0.25 ? 0xFFE0C020 : 0xFFE03030);
            g.fill(x - bw / 2 - 1, y - 3, x + bw / 2 + 1, y - 1, col);
            if (sel) {
                g.renderOutline(x - bw / 2 - 3, y - 5, bw + 6, 10, 0xFFFFFFFF);
            }
            if (s.getNationId() != me && me >= 0 && d.atWar(me, s.getNationId())) {
                g.drawCenteredString(font, "⚔", x, y - 13, 0xFFFF4040);
            }
        }
        for (Province p : d.provinces.values()) {
            if (p.core == null) continue;
            double dx = p.core.getX() - RtsCamera.focusX, dz = p.core.getZ() - RtsCamera.focusZ;
            if (dx * dx + dz * dz > 140 * 140) continue;
            double[] s = RtsCamera.project(p.core.getX() + 0.5, p.core.getY() + 3, p.core.getZ() + 0.5);
            if (s == null) continue;
            Nation owner = d.nation(p.owner);
            String label = p.name + (owner == null ? "" : " (" + owner.name + ")");
            int x = (int) s[0], y = (int) s[1];
            int w = font.width(label) + 6;
            g.fill(x - w / 2, y - 12, x + w / 2, y + 4, 0xA0000000);
            g.drawCenteredString(font, label, x, y - 10, 0xFF000000 | (owner == null ? 0xDDDDDD : owner.rgb()));
            double f = p.coreMaxHp <= 0 ? 1 : Math.max(0, p.coreHp / p.coreMaxHp);
            g.fill(x - w / 2 + 2, y, x - w / 2 + 2 + (int) ((w - 4) * f), y + 2, p.underAttack(d.gameTick) ? 0xFFFF3030 : 0xFF30D030);
        }
        if (RtsCamera.height < 60) {
            for (Building b : d.buildings.values()) {
                double dx = b.pos.getX() - RtsCamera.focusX, dz = b.pos.getZ() - RtsCamera.focusZ;
                if (dx * dx + dz * dz > 90 * 90 || b.type() == null) continue;
                double[] s = RtsCamera.project(b.pos.getX() + 0.5, b.maxY() + 1, b.pos.getZ() + 0.5);
                if (s == null) continue;
                Component label = b.type().name();
                int col = b.isActive() ? 0xFF90FF90 : b.state == Building.State.DAMAGED ? 0xFFFF7070 : 0xFFFFD060;
                g.drawCenteredString(font, label, (int) s[0], (int) s[1], col);
                if (!b.isActive() && b.type() != null) {
                    g.drawCenteredString(font, b.satisfied() + "/" + b.type().requirements.size(), (int) s[0], (int) s[1] + 9, 0xFFCCCCCC);
                }
            }
        }
    }

    private void renderOrderMarker(GuiGraphics g) {
        if (orderMarker == null) return;
        long age = System.currentTimeMillis() - orderMarkerTime;
        if (age > 900) {
            orderMarker = null;
            return;
        }
        double[] p;
        if (mapMode) p = new double[]{bigMap.toScreenX(orderMarker.x), bigMap.toScreenY(orderMarker.z)};
        else p = RtsCamera.project(orderMarker.x, orderMarker.y + 0.2, orderMarker.z);
        if (p == null) return;
        int r = (int) (4 + age / 100);
        int a = (int) (255 * (1 - age / 900.0));
        g.renderOutline((int) p[0] - r, (int) p[1] - r, r * 2, r * 2, a << 24 | 0x40FF40);
    }

    private void renderTopBar(GuiGraphics g, GameData d, Nation n) {
        g.fill(0, 0, width, 18, 0xD0101418);
        g.hLine(0, width, 18, 0xFF4A5260);
        int x = 4;
        if (n == null) {
            Component c = d.phase == GamePhase.SETUP ? Component.translatable("imperium.ui.setup_bar") : Component.translatable("imperium.hud.no_nation");
            g.drawString(font, c, x, 5, 0xFFFFD060);
            x += font.width(c) + 10;
        } else {
            g.fill(x, 4, x + 10, 14, 0xFF000000 | n.rgb());
            g.drawString(font, n.name, x + 13, 5, 0xFFFFFFFF);
            x += 16 + font.width(n.name) + 8;
            for (ResourceType t : ResourceType.values()) {
                g.renderItem(t.icon(), x, 1);
                String v = String.valueOf(n.stock.getInt(t));
                g.drawString(font, v, x + 17, 5, t == ResourceType.FOOD && n.famine ? 0xFFFF5050 : 0xFFFFFFFF);
                x += 17 + font.width(v) + 6;
            }
            String extra = "☺" + (int) n.happiness + "%  ⌂" + n.population + "/" + n.housing + "  ⚔" + n.soldierCount + "/" + MilitaryManager.maxArmy(n)
                    + "  ✦" + (int) n.research;
            g.drawString(font, extra, x, 5, 0xFFB8E0FF);
            x += font.width(extra) + 10;
        }
        Component phase;
        if (d.phase == GamePhase.RUNNING && d.inGracePeriod()) phase = Component.translatable("imperium.hud.grace", UiUtil.time(ClientState.graceSeconds()));
        else if (d.phase == GamePhase.ENDED && !d.winner.isEmpty()) phase = Component.translatable("imperium.ui.winner", d.winner);
        else phase = Component.translatable(d.phase.key());
        int pw = font.width(phase);
        if (x + pw < width - 4) g.drawString(font, phase, width - pw - 4, 5, 0xFFFFD700);
    }

    private void renderPanel(GuiGraphics g, GameData d, Nation n, int mx, int my) {
        UiUtil.panel(g, panelX, panelY, PANEL_W, PANEL_H);
        if (tab == Tab.BUILD) {
            List<BuildingType> list = buildList();
            for (int i = 0; i < list.size(); i++) {
                BuildingType t = list.get(i);
                int[] c = gridCell(i);
                boolean unlocked = n != null && t.unlocked(n);
                boolean noPrefab = prefabMode && t.prefabCost == null;
                boolean affordable = n != null && (!prefabMode || (t.prefabCost != null && n.stock.has(t.prefabCost)));
                boolean selected = placement != null && placement.type == t;
                int bg = selected ? 0xFF3A6A3A : !unlocked || noPrefab ? 0xFF2A2A2A : affordable ? 0xFF34404E : 0xFF4E3434;
                g.fill(c[0], c[1], c[0] + 20, c[1] + 20, bg);
                g.renderOutline(c[0], c[1], 20, 20, UiUtil.inside(mx, my, c[0], c[1], 20, 20) ? 0xFFFFFFFF : 0xFF5A6270);
                g.renderItem(t.icon(), c[0] + 2, c[1] + 2);
                if (!unlocked || noPrefab) g.fill(c[0] + 1, c[1] + 1, c[0] + 19, c[1] + 19, 0xA0000000);
                if (n != null) {
                    int count = d.countActive(n.id, t.id);
                    if (count > 0) g.drawString(font, String.valueOf(count), c[0] + 20 - font.width(String.valueOf(count)), c[1] + 12, 0xFFFFFFFF);
                }
            }
            Component hint = placement != null ? Component.translatable("imperium.ui.placing", placement.type.name())
                    : Component.translatable(prefabMode ? "imperium.ui.prefab_hint" : "imperium.ui.manual_hint");
            g.drawWordWrap(font, hint, panelX + 4, panelY + 70, PANEL_W - 8, 0xFFBBBBBB);
        } else if (tab == Tab.ARMY) {
            UnitType[] types = UnitType.values();
            for (int i = 0; i < types.length; i++) {
                UnitType t = types[i];
                int x = panelX + 4 + i * 52, y = panelY + 6;
                Component err = n == null ? Component.empty() : MilitaryManager.canRecruit(d, n, t);
                g.fill(x, y, x + 50, y + 34, err == null ? 0xFF34404E : 0xFF2A2A2A);
                g.renderOutline(x, y, 50, 34, UiUtil.inside(mx, my, x, y, 50, 34) ? 0xFFFFFFFF : 0xFF5A6270);
                g.renderItem(unitIcon(t), x + 17, y + 2);
                g.drawCenteredString(font, UiUtil.trim(font, t.title().getString(), 48), x + 25, y + 22, err == null ? 0xFFFFFFFF : 0xFF888888);
            }
            String army = n == null ? "-" : n.soldierCount + "/" + MilitaryManager.maxArmy(n);
            g.drawString(font, Component.translatable("imperium.ui.army", army, SELECTION.size()), panelX + 4, panelY + 94, 0xFFDDDDDD);
            g.drawString(font, Component.translatable("imperium.ui.army_hint"), panelX + 4, panelY + 106, 0xFF999999);
            g.drawString(font, Component.translatable("imperium.ui.army_hint2"), panelX + 4, panelY + 116, 0xFF999999);
        } else if (tab == Tab.EDITOR) {
            Nation en = d.nation(editorNation);
            int px = panelX + 4, py = panelY + 4;
            String nl = en == null ? "-" : en.name + (en.bot ? " [BOT]" : "");
            g.fill(px + 13, py + 50, px + 99, py + 64, 0xFF000000 | (en == null ? 0x333333 : MapTexturesColor.dim(en.rgb())));
            g.drawCenteredString(font, UiUtil.trim(font, nl, 84), px + 56, py + 53, 0xFFFFFFFF);
            Province ep = d.provinces.get(editorProvince);
            g.fill(px + 13, py + 66, px + 99, py + 80, 0xFF333333);
            g.drawCenteredString(font, UiUtil.trim(font, ep == null ? "-" : ep.name, 84), px + 56, py + 69, 0xFFFFFFFF);
            String arena = d.arenaSet ? d.size + "x" + d.size + " @ " + d.centerX + "," + d.centerZ : "-";
            g.drawString(font, Component.translatable("imperium.ui.editor.status", arena, Component.translatable(d.phase.key())), px, py + 118, 0xFF999999);
        }
    }

    private static ItemStack unitIcon(UnitType t) {
        return switch (t) {
            case MILITIA -> new ItemStack(Items.STONE_SWORD);
            case SWORDSMAN -> new ItemStack(Items.IRON_SWORD);
            case ARCHER -> new ItemStack(Items.BOW);
            case KNIGHT -> new ItemStack(com.endruqq.imperium.registry.ModItems.STEEL_SWORD.get());
            case SAPPER -> new ItemStack(Items.TNT);
        };
    }

    private void renderSelectionInfo(GuiGraphics g, GameData d) {
        if (SELECTION.isEmpty()) return;
        Map<UnitType, Integer> counts = new EnumMap<>(UnitType.class);
        double hp = 0;
        int n = 0;
        for (UnitsPacket.Unit u : ClientState.units) {
            if (!SELECTION.contains(u.entityId) || u.type < 0) continue;
            counts.merge(UnitType.byOrdinal(u.type), 1, Integer::sum);
            hp += u.hp;
            n++;
        }
        int x = width - MINI - 8 - 120, y = height - 4 - 14 - counts.size() * 10;
        if (mapMode) x = width - 124;
        UiUtil.panel(g, x, y - 4, 120, 18 + counts.size() * 10);
        g.drawString(font, Component.translatable("imperium.ui.selected", SELECTION.size()), x + 4, y, 0xFFFFFFFF);
        int yy = y + 10;
        for (Map.Entry<UnitType, Integer> e : counts.entrySet()) {
            g.drawString(font, e.getValue() + "× " + e.getKey().title().getString(), x + 4, yy, 0xFFCCCCCC);
            yy += 10;
        }
        if (n > 0) {
            double f = hp / n;
            g.fill(x + 4, yy, x + 116, yy + 2, 0xFF400000);
            g.fill(x + 4, yy, x + 4 + (int) (112 * f), yy + 2, 0xFF30D030);
        }
    }

    private void renderNotes(GuiGraphics g) {
        long now = System.currentTimeMillis();
        int y = 22;
        int shown = 0;
        for (ClientState.Note note : ClientState.NOTES) {
            long age = now - note.time();
            if (age > 12000 || shown >= 4) continue;
            int alpha = age > 10000 ? (int) (255 * (12000 - age) / 2000.0) : 255;
            if (alpha < 8) continue;
            String s = UiUtil.trim(font, note.text().getString(), width - 100);
            g.fill(2, y - 2, 8 + font.width(s), y + 10, (alpha / 2) << 24);
            g.drawString(font, s, 5, y, (alpha << 24) | (note.important() ? 0xFFD700 : 0xFFFFFF));
            y += 13;
            shown++;
        }
    }

    private void renderHints(GuiGraphics g, Nation n) {
        Component hint;
        if (tool != Tool.NONE) hint = Component.translatable("imperium.ui.hint.editor");
        else if (placement != null) hint = Component.translatable("imperium.ui.hint.place");
        else if (mapMode) hint = Component.translatable("imperium.ui.hint.map");
        else hint = Component.translatable("imperium.ui.hint.world");
        int w = font.width(hint);
        int x = Math.max(PANEL_W + 10, width / 2 - w / 2);
        if (x + w > width - MINI - 10) return;
        g.drawString(font, hint, x, height - 12, 0xFFCCCCCC);
    }

    private void renderTooltips(GuiGraphics g, GameData d, Nation n, int mx, int my) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        if (tab == Tab.BUILD) {
            List<BuildingType> list = buildList();
            for (int i = 0; i < list.size(); i++) {
                int[] c = gridCell(i);
                if (UiUtil.inside(mx, my, c[0], c[1], 20, 20)) buildingTooltip(lines, list.get(i), n);
            }
        } else if (tab == Tab.ARMY) {
            UnitType[] types = UnitType.values();
            for (int i = 0; i < types.length; i++) {
                int x = panelX + 4 + i * 52, y = panelY + 6;
                if (UiUtil.inside(mx, my, x, y, 50, 34)) unitTooltip(lines, types[i], d, n);
            }
        }
        if (lines.isEmpty() && placement != null && placement.error != null && !overUi(mx, my)) {
            lines.add(placement.error.copy().withStyle(ChatFormatting.RED).getVisualOrderText());
        }
        if (lines.isEmpty() && hoverEntity != null) {
            add(lines, hoverEntity.getDisplayName().copy().withStyle(ChatFormatting.WHITE));
            int nat = -1;
            if (hoverEntity instanceof SoldierEntity s) {
                nat = s.getNationId();
                add(lines, s.getUnitType().title().copy().withStyle(ChatFormatting.GRAY));
                add(lines, Component.translatable(s.getOrder().key()).withStyle(ChatFormatting.DARK_AQUA));
            } else if (hoverEntity instanceof CitizenEntity c) {
                nat = c.getNationId();
                add(lines, c.getProfession().title().copy().withStyle(ChatFormatting.GRAY));
            } else if (hoverEntity instanceof Player p) {
                nat = d.nationIdOf(p.getUUID());
            }
            Nation hn = d.nation(nat);
            if (hn != null) add(lines, Component.literal(hn.name).withStyle(s -> s.withColor(hn.rgb())));
            if (hoverEntity instanceof LivingEntity le) {
                add(lines, Component.literal("❤ " + (int) le.getHealth() + "/" + (int) le.getMaxHealth()).withStyle(ChatFormatting.RED));
            }
        }
        if (lines.isEmpty() && mapMode && !overUi(mx, my)) {
            Province p = d.provinceAt(bigMap.toWorldX(mx), bigMap.toWorldZ(my));
            if (p != null) {
                Nation owner = d.nation(p.owner);
                add(lines, Component.literal(p.name).withStyle(ChatFormatting.GOLD));
                add(lines, Component.literal(owner == null ? "-" : owner.name).withStyle(s -> s.withColor(owner == null ? 0xAAAAAA : owner.rgb())));
                if (n != null && owner != null && owner.id != n.id) {
                    RelationType rt = d.relationType(n.id, owner.id);
                    add(lines, Component.translatable(rt.key()).withStyle(s -> s.withColor(rt.color)));
                }
                if (p.core != null) add(lines, Component.translatable("imperium.ui.core_hp", (int) p.coreHp, (int) p.coreMaxHp).withStyle(ChatFormatting.GRAY));
                if (!SELECTION.isEmpty() && owner != null && n != null && d.atWar(n.id, owner.id)) {
                    add(lines, Component.translatable("imperium.ui.attack_core_hint").withStyle(ChatFormatting.RED));
                }
            }
        }
        if (!lines.isEmpty()) g.renderTooltip(font, lines, mx, my);
    }

    private void add(List<FormattedCharSequence> lines, Component c) {
        lines.addAll(font.split(c, 240));
    }

    private void buildingTooltip(List<FormattedCharSequence> lines, BuildingType t, Nation n) {
        add(lines, t.name().copy().withStyle(ChatFormatting.GOLD));
        add(lines, t.description().copy().withStyle(ChatFormatting.GRAY));
        if (t.tech != null) {
            boolean has = n != null && n.has(t.tech);
            add(lines, Component.translatable("imperium.ui.requires_tech", t.tech.title()).withStyle(has ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        add(lines, Component.translatable("imperium.ui.requirements").withStyle(ChatFormatting.YELLOW));
        for (Requirement r : t.requirements) add(lines, Component.literal(" " + r.count + "× ").append(r.name()).withStyle(ChatFormatting.WHITE));
        add(lines, Component.translatable("imperium.ui.plot", t.size(), t.size()).withStyle(ChatFormatting.DARK_GRAY));
        if (t.prefabCost != null) {
            boolean ok = n != null && n.stock.has(t.prefabCost);
            add(lines, Component.translatable("imperium.ui.prefab_cost", t.prefabCost.describe(1)).withStyle(ok ? ChatFormatting.AQUA : ChatFormatting.RED));
        } else {
            add(lines, Component.translatable("imperium.ui.no_prefab").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (t.maxPerNation > 0) add(lines, Component.translatable("imperium.ui.limit", t.maxPerNation).withStyle(ChatFormatting.DARK_GRAY));
    }

    private void unitTooltip(List<FormattedCharSequence> lines, UnitType t, GameData d, Nation n) {
        add(lines, t.title().copy().withStyle(ChatFormatting.GOLD));
        add(lines, Component.translatable("imperium.unit." + t.id() + ".desc").withStyle(ChatFormatting.GRAY));
        add(lines, Component.literal("❤ " + (int) t.maxHealth + "   ⚔ " + (int) t.attack).withStyle(ChatFormatting.WHITE));
        double mult = n == null ? 1 : t.costMult(n);
        add(lines, Component.translatable("imperium.ui.cost", t.cost.describe(mult)).withStyle(ChatFormatting.AQUA));
        add(lines, Component.translatable("imperium.ui.needs_building", t.building.name()).withStyle(ChatFormatting.YELLOW));
        if (n != null) {
            Component err = MilitaryManager.canRecruit(d, n, t);
            if (err != null) add(lines, err.copy().withStyle(ChatFormatting.RED));
        }
        add(lines, Component.translatable("imperium.ui.recruit_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** small colour helper kept here to avoid another class */
    private static final class MapTexturesColor {
        static int dim(int rgb) {
            return com.endruqq.imperium.client.MapTextures.darken(rgb, 0.6);
        }
    }
}
