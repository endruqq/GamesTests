package com.endruqq.imperium.client.screen;

import com.endruqq.imperium.client.ClientState;
import com.endruqq.imperium.client.Keys;
import com.endruqq.imperium.client.RtsCamera;
import com.endruqq.imperium.client.StrategicMap;
import com.endruqq.imperium.client.UiUtil;
import com.endruqq.imperium.client.ui.FlatButton;
import com.endruqq.imperium.client.ui.Theme;
import com.endruqq.imperium.game.building.BuildingCategory;
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

    private enum Tab { BUILD, ARMY, PEOPLE, EDITOR }

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
    private static BuildingCategory category = null;
    private static boolean patrolMode;
    private static final List<Vec3> patrolPoints = new ArrayList<>();
    private static int editorNation = -1;
    private static int nationsCount = 4;
    private static int provincesCount = 4;
    private static Tool tool = Tool.NONE;
    private static double savedMapX = Double.NaN, savedMapZ, savedMapZoom;

    private static final int PANEL_W = 292;
    private static final int PANEL_H = 136;
    private static final int MINI = 116;
    private static final int TOP = 20;
    private static final int TOOL_W = 86;
    private static final int CELL = 24;

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
    private Component topTip;

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
        bigMap.setRect(0, TOP, width, height - TOP);
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

        // side toolbar
        int bx = width - TOOL_W - 4, by = TOP + 4;
        Nation me = ClientState.myNation();
        addRenderableWidget(FlatButton.of(bx, by, TOOL_W, 15, Component.translatable("imperium.ui.btn.map"), b -> toggleMap()).selected(mapMode));
        addRenderableWidget(FlatButton.of(bx, by + 17, TOOL_W, 15, Component.translatable("imperium.ui.btn.tech"), b -> minecraft.setScreen(new TechTreeScreen(this))));
        addRenderableWidget(FlatButton.of(bx, by + 34, TOOL_W, 15, Component.translatable("imperium.ui.btn.nation"), b -> minecraft.setScreen(new NationScreen(this, NationScreen.OVERVIEW))));
        addRenderableWidget(FlatButton.of(bx, by + 51, TOOL_W, 15, Component.translatable("imperium.ui.btn.people"), b -> minecraft.setScreen(new NationScreen(this, NationScreen.PEOPLE))).enabled(me != null));
        addRenderableWidget(FlatButton.of(bx, by + 68, TOOL_W, 15, Component.translatable("imperium.ui.btn.diplomacy"), b -> minecraft.setScreen(new NationScreen(this, NationScreen.DIPLOMACY))).enabled(me != null));
        if (me != null) {
            addRenderableWidget(FlatButton.of(bx, by + 85, TOOL_W, 15, Component.translatable(me.bordersClosed ? "imperium.ui.borders.closed_short" : "imperium.ui.borders.open_short"),
                    me.bordersClosed ? FlatButton.Style.DANGER : FlatButton.Style.SUCCESS, b -> send("borders", new CompoundTag()))
                    .tip(Component.translatable("imperium.ui.tip.borders")));
        }
        addRenderableWidget(FlatButton.of(bx, by + 102, TOOL_W, 15, Component.translatable("imperium.ui.btn.exit"), FlatButton.Style.GHOST, b -> onClose()));

        // tabs
        int tx = panelX;
        addRenderableWidget(tabButton(Tab.BUILD, "imperium.ui.tab.build", tx));
        addRenderableWidget(tabButton(Tab.ARMY, "imperium.ui.tab.army", tx + 66));
        addRenderableWidget(tabButton(Tab.PEOPLE, "imperium.ui.tab.people", tx + 132));
        if (ClientState.isAdmin()) addRenderableWidget(tabButton(Tab.EDITOR, "imperium.ui.tab.editor", tx + 198));
        if (tab == Tab.EDITOR && !ClientState.isAdmin()) tab = Tab.BUILD;

        int px = panelX + 4, py = panelY + 4;
        switch (tab) {
            case BUILD -> {
                addRenderableWidget(FlatButton.of(px, py, 70, 14, Component.translatable("imperium.ui.prefab"), b -> {
                    prefabMode = true;
                    rebuildWidgets();
                }).selected(prefabMode).tip(Component.translatable("imperium.ui.prefab_hint")));
                addRenderableWidget(FlatButton.of(px + 72, py, 70, 14, Component.translatable("imperium.ui.manual"), b -> {
                    prefabMode = false;
                    rebuildWidgets();
                }).selected(!prefabMode).tip(Component.translatable("imperium.ui.manual_hint")));
                int cxp = px + 148;
                BuildingCategory[] cats = {null, BuildingCategory.HOUSING, BuildingCategory.ECONOMY, BuildingCategory.SOCIETY, BuildingCategory.MILITARY};
                for (BuildingCategory c : cats) {
                    ItemStack icon = c == null ? new ItemStack(Items.COMPASS) : switch (c) {
                        case HOUSING -> new ItemStack(Items.RED_BED);
                        case ECONOMY -> new ItemStack(Items.WHEAT);
                        case SOCIETY -> new ItemStack(Items.BELL);
                        default -> new ItemStack(Items.IRON_SWORD);
                    };
                    Component tip = c == null ? Component.translatable("imperium.ui.category.all") : Component.translatable("imperium.category." + c.name().toLowerCase());
                    addRenderableWidget(FlatButton.of(cxp, py, 26, 14, Component.empty(), b -> {
                        category = c;
                        rebuildWidgets();
                    }).icon(icon).selected(category == c).tip(tip));
                    cxp += 28;
                }
            }
            case ARMY -> {
                int oy = py + 44;
                int ow = (PANEL_W - 8 - 4 * 3) / 5;
                addRenderableWidget(FlatButton.of(px, oy, ow, 14, Component.translatable("imperium.ui.order.hold"), b -> order(OrderType.HOLD)));
                addRenderableWidget(FlatButton.of(px + (ow + 3), oy, ow, 14, Component.translatable("imperium.ui.order.follow"), b -> order(OrderType.FOLLOW)));
                addRenderableWidget(FlatButton.of(px + (ow + 3) * 2, oy, ow, 14, Component.translatable("imperium.ui.order.return"), b -> order(OrderType.RETURN)));
                addRenderableWidget(FlatButton.of(px + (ow + 3) * 3, oy, ow, 14, Component.translatable("imperium.ui.order.patrol"), b -> togglePatrol())
                        .selected(patrolMode).tip(Component.translatable("imperium.ui.tip.patrol")));
                addRenderableWidget(FlatButton.of(px + (ow + 3) * 4, oy, ow, 14, Component.translatable("imperium.ui.order.dismiss"), FlatButton.Style.DANGER, b -> dismiss())
                        .tip(Component.translatable("imperium.ui.dismiss_hint")));
                stanceButton = addRenderableWidget(FlatButton.of(px, oy + 17, 140, 14, stanceLabel(), b -> cycleStance()));
                addRenderableWidget(FlatButton.of(px + 143, oy + 17, PANEL_W - 8 - 143, 14, Component.translatable("imperium.ui.select_all"), b -> selectAll()));
            }
            case PEOPLE -> {
                if (me != null) {
                    addRenderableWidget(FlatButton.of(px, py + 92, 140, 15, Component.translatable("imperium.ui.people.hire_all"), FlatButton.Style.PRIMARY, b -> {
                        CompoundTag t = new CompoundTag();
                        t.putString("op", "auto");
                        send("jobs", t);
                    }).tip(Component.translatable("imperium.ui.tip.hire_all")));
                    addRenderableWidget(FlatButton.of(px + 143, py + 92, PANEL_W - 8 - 143, 15, Component.translatable("imperium.ui.people.open_list"),
                            b -> minecraft.setScreen(new NationScreen(this, NationScreen.PEOPLE))));
                }
            }
            case EDITOR -> initEditor(px, py);
        }
    }

    private Button tabButton(Tab t, String key, int x) {
        return FlatButton.of(x, panelY - 15, 64, 15, Component.translatable(key), FlatButton.Style.TAB, btn -> {
            tab = t;
            placement = null;
            tool = Tool.NONE;
            patrolMode = false;
            rebuildWidgets();
        }).selected(tab == t);
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
        if (my < TOP) return true;
        if (UiUtil.inside(mx, my, panelX, panelY - 16, PANEL_W, PANEL_H + 16)) return true;
        if (UiUtil.inside(mx, my, width - TOOL_W - 6, TOP, TOOL_W + 6, 126)) return true;
        return !mapMode && miniMap.contains(mx, my);
    }

    // ------------------------------------------------------------------ patrol

    private void togglePatrol() {
        patrolMode = !patrolMode && !SELECTION.isEmpty();
        patrolPoints.clear();
        if (patrolMode && minecraft.player != null) minecraft.player.displayClientMessage(Component.translatable("imperium.ui.patrol_hint"), true);
        rebuildWidgets();
    }

    /** Adds a waypoint and (re)sends the patrol order. Shift keeps adding waypoints. */
    private void patrolAt(Vec3 pos) {
        if (SELECTION.isEmpty()) {
            patrolMode = false;
            return;
        }
        patrolPoints.add(pos);
        CompoundTag t = new CompoundTag();
        t.putIntArray("ids", selectionIds());
        net.minecraft.nbt.ListTag l = new net.minecraft.nbt.ListTag();
        for (Vec3 v : patrolPoints) {
            CompoundTag p = new CompoundTag();
            p.putDouble("x", v.x);
            p.putDouble("y", v.y);
            p.putDouble("z", v.z);
            l.add(p);
        }
        t.put("points", l);
        send("patrol", t);
        orderMarker = pos;
        orderMarkerTime = System.currentTimeMillis();
        if (!hasShiftDown()) {
            patrolMode = false;
            patrolPoints.clear();
            rebuildWidgets();
        }
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
        if (patrolMode) {
            BlockHitResult hit = RtsCamera.pickBlock(mx, my);
            if (hit != null) patrolAt(new Vec3(hit.getLocation().x, hit.getBlockPos().getY() + 1, hit.getLocation().z));
            return;
        }
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
        if (patrolMode) {
            double py = -1000;
            if (minecraft.level != null && minecraft.level.hasChunkAt(BlockPos.containing(wx, 64, wz))) {
                py = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(wx), Mth.floor(wz));
            }
            patrolAt(new Vec3(wx, py, wz));
            return;
        }
        Province core = coreNear(map, mx, my);
        int me = ClientState.myNationId();
        if (core != null && core.owner != me && (core.owner < 0 || ClientState.data.atWar(me, core.owner))) {
            orderAt(OrderType.ATTACK_CORE, Vec3.atCenterOf(core.core), -1, core.id);
            return;
        }
        double y = -1000;
        if (minecraft.level != null && minecraft.level.hasChunkAt(BlockPos.containing(wx, 64, wz))) {
            y = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(wx), Mth.floor(wz));
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
            if (patrolMode) {
                patrolMode = false;
                patrolPoints.clear();
                rebuildWidgets();
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
            case GLFW.GLFW_KEY_P -> togglePatrol();
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
        List<BuildingType> l = new ArrayList<>();
        for (BuildingType t : Buildings.all()) if (category == null || t.category == category) l.add(t);
        return l;
    }

    private int[] gridCell(int index) {
        int cols = (PANEL_W - 8) / (CELL + 2);
        int gx = panelX + 4 + (index % cols) * (CELL + 2);
        int gy = panelY + 22 + (index / cols) * (CELL + 2);
        return new int[]{gx, gy};
    }

    private int[] unitCell(int i) {
        int w = (PANEL_W - 8 - 4 * 3) / 5;
        return new int[]{panelX + 4 + i * (w + 3), panelY + 4, w, 37};
    }

    private boolean handlePanelClick(double mx, double my, int button) {
        if (button != 0) return false;
        Nation n = ClientState.myNation();
        if (tab == Tab.BUILD) {
            List<BuildingType> list = buildList();
            for (int i = 0; i < list.size(); i++) {
                int[] c = gridCell(i);
                if (UiUtil.inside(mx, my, c[0], c[1], CELL, CELL)) {
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
                int[] c = unitCell(i);
                if (UiUtil.inside(mx, my, c[0], c[1], c[2], c[3])) {
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
        renderPatrols(g);
        renderTopBar(g, d, n, mx, my);
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
        int y = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING, hp.getX(), hp.getZ());
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
            if (s.isSurrendered()) {
                g.drawCenteredString(font, "⚑", x, y - 13, 0xFFFFFFFF);
            } else if (s.getNationId() != me && me >= 0 && d.atWar(me, s.getNationId())) {
                g.drawCenteredString(font, "⚔", x, y - 13, 0xFFFF4040);
            }
        }
        for (Entity e : minecraft.level.entitiesForRendering()) {
            if (!(e instanceof CitizenEntity c) || c.getNationId() != me || me < 0) continue;
            if (!c.hasState(CitizenEntity.TRAPPED) && !c.hasState(CitizenEntity.MOURNING)) continue;
            if (c.distanceToSqr(RtsCamera.focusX, c.getY(), RtsCamera.focusZ) > 90 * 90) continue;
            double[] p = RtsCamera.project(c.getX(), c.getY() + c.getBbHeight() + 0.4, c.getZ());
            if (p == null) continue;
            boolean trapped = c.hasState(CitizenEntity.TRAPPED);
            g.drawCenteredString(font, trapped ? "⚠" : "✝", (int) p[0], (int) p[1] - 8, trapped ? 0xFFFF5050 : 0xFFAAAAAA);
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

    private void renderTopBar(GuiGraphics g, GameData d, Nation n, int mx, int my) {
        g.fill(0, 0, width, TOP, 0xF0121720);
        g.hLine(0, width, TOP, Theme.BORDER_LIGHT);
        topTip = null;
        int x = 4;
        if (n == null) {
            Component c = d.phase == GamePhase.SETUP ? Component.translatable("imperium.ui.setup_bar") : Component.translatable("imperium.hud.no_nation");
            UiUtil.text(g, font, c, x, 6, Theme.ACCENT);
            x += font.width(c) + 10;
        } else {
            g.fill(x, 5, x + 10, 15, 0xFF000000 | n.rgb());
            g.renderOutline(x, 5, 10, 10, 0xFF000000);
            UiUtil.text(g, font, n.name, x + 14, 6, Theme.TEXT);
            x += 14 + font.width(n.name) + 8;
            for (ResourceType t : ResourceType.values()) {
                String v = UiUtil.compact(n.stock.get(t));
                int cw = 17 + font.width(v) + 4;
                boolean hover = UiUtil.inside(mx, my, x - 1, 1, cw, TOP - 2);
                if (hover) {
                    g.fill(x - 1, 1, x - 1 + cw, TOP - 1, Theme.CARD_HOVER);
                    topTip = Component.translatable("imperium.ui.tip.resource", t.label(), n.stock.getInt(t), UiUtil.signed(n.lastDelta.get(t)));
                }
                g.renderItem(t.icon(), x, 2);
                double delta = n.lastDelta.get(t);
                int col = t == ResourceType.FOOD && n.famine ? Theme.BAD : delta < -0.5 ? 0xFFFFB0A0 : Theme.TEXT;
                UiUtil.text(g, font, v, x + 17, 6, col);
                x += cw + 2;
            }
            x += 4;
            g.vLine(x - 4, 3, TOP - 3, Theme.BORDER);
            x = chip(g, x, "☺ " + (int) n.happiness + "%", n.happiness < 30 ? Theme.BAD : n.happiness > 70 ? Theme.GOOD : Theme.WARN, mx, my,
                    Component.translatable("imperium.ui.tip.happiness", (int) n.happiness, (int) n.happinessTarget));
            x = chip(g, x, "⌂ " + n.population + "/" + n.housing, n.population > n.housing ? Theme.BAD : Theme.TEXT, mx, my,
                    Component.translatable("imperium.ui.tip.population", n.population, n.housing));
            x = chip(g, x, "☻ " + n.citizenCount + "/" + n.citizenCap, Theme.PURPLE, mx, my,
                    Component.translatable("imperium.ui.tip.citizens", n.citizenCount, n.citizenCap, n.employed, n.unemployed));
            x = chip(g, x, "⚔ " + n.soldierCount + "/" + MilitaryManager.maxArmy(n), Theme.TEXT, mx, my,
                    Component.translatable("imperium.ui.tip.army", n.soldierCount, MilitaryManager.maxArmy(n)));
            x = chip(g, x, "✦ " + (int) n.research, Theme.INFO, mx, my,
                    Component.translatable("imperium.ui.tip.research", (int) n.research, UiUtil.signed(n.researchDelta)));
            if (n.trappedCount > 0 && (System.currentTimeMillis() / 500) % 2 == 0) {
                x = chip(g, x, "⚠ " + n.trappedCount, Theme.BAD, mx, my, Component.translatable("imperium.ui.people.trapped", n.trappedCount));
            }
        }
        Component phase;
        int pcol = Theme.ACCENT;
        if (d.phase == GamePhase.RUNNING && d.inGracePeriod()) phase = Component.translatable("imperium.hud.grace", UiUtil.time(ClientState.graceSeconds()));
        else if (d.phase == GamePhase.ENDED && !d.winner.isEmpty()) phase = Component.translatable("imperium.ui.winner", d.winner);
        else if (n != null && d.atWarWithAnyone(n.id)) {
            phase = Component.translatable("imperium.hud.at_war");
            pcol = Theme.BAD;
        } else phase = Component.translatable(d.phase.key());
        int pw = font.width(phase);
        if (x + pw < width - 8) UiUtil.text(g, font, phase, width - pw - 6, 6, pcol);
    }

    private int chip(GuiGraphics g, int x, String text, int color, int mx, int my, Component tip) {
        int w = font.width(text) + 6;
        if (UiUtil.inside(mx, my, x - 2, 1, w, TOP - 2)) {
            g.fill(x - 2, 1, x - 2 + w, TOP - 1, Theme.CARD_HOVER);
            topTip = tip;
        }
        UiUtil.text(g, font, text, x + 1, 6, color);
        return x + w + 2;
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
                boolean hover = UiUtil.inside(mx, my, c[0], c[1], CELL, CELL);
                int bg = selected ? Theme.CARD_SELECTED : hover ? Theme.CARD_HOVER : Theme.CARD;
                g.fill(c[0], c[1], c[0] + CELL, c[1] + CELL, bg);
                int border = selected ? Theme.ACCENT : !unlocked || noPrefab ? Theme.BORDER : affordable ? Theme.BORDER_LIGHT : 0xFF7A3A3A;
                g.renderOutline(c[0], c[1], CELL, CELL, hover ? 0xFFFFFFFF : border);
                g.renderItem(t.icon(), c[0] + 4, c[1] + 4);
                if (!unlocked || noPrefab) {
                    g.fill(c[0] + 1, c[1] + 1, c[0] + CELL - 1, c[1] + CELL - 1, 0xA0101418);
                    if (!unlocked) UiUtil.text(g, font, "⊘", c[0] + CELL - 9, c[1] + 1, Theme.TEXT_MUTED);
                } else if (!affordable) {
                    g.fill(c[0] + 1, c[1] + CELL - 3, c[0] + CELL - 1, c[1] + CELL - 1, Theme.BAD);
                }
                if (n != null) {
                    int count = d.countActive(n.id, t.id);
                    if (count > 0) {
                        String cs = String.valueOf(count);
                        g.pose().pushPose();
                        g.pose().translate(0, 0, 200);
                        g.fill(c[0] + CELL - 2 - font.width(cs) - 2, c[1] + CELL - 11, c[0] + CELL - 1, c[1] + CELL - 2, 0xC0000000);
                        UiUtil.text(g, font, cs, c[0] + CELL - 2 - font.width(cs), c[1] + CELL - 10, Theme.ACCENT);
                        g.pose().popPose();
                    }
                }
            }
            Component hint = placement != null ? Component.translatable("imperium.ui.placing", placement.type.name())
                    : Component.translatable(prefabMode ? "imperium.ui.prefab_hint" : "imperium.ui.manual_hint");
            int rows = (list.size() + (PANEL_W - 8) / (CELL + 2) - 1) / ((PANEL_W - 8) / (CELL + 2));
            int hy = panelY + 24 + rows * (CELL + 2);
            g.hLine(panelX + 4, panelX + PANEL_W - 4, hy - 1, Theme.BORDER);
            g.drawWordWrap(font, hint, panelX + 4, hy + 2, PANEL_W - 8, Theme.TEXT_DIM);
        } else if (tab == Tab.ARMY) {
            UnitType[] types = UnitType.values();
            for (int i = 0; i < types.length; i++) {
                UnitType t = types[i];
                int[] c = unitCell(i);
                Component err = n == null ? Component.empty() : MilitaryManager.canRecruit(d, n, t);
                boolean hover = UiUtil.inside(mx, my, c[0], c[1], c[2], c[3]);
                UiUtil.card(g, c[0], c[1], c[2], c[3], hover && err == null);
                if (err == null) g.fill(c[0] + 1, c[1] + 1, c[0] + c[2] - 1, c[1] + 3, Theme.GOOD);
                g.renderItem(unitIcon(t), c[0] + c[2] / 2 - 8, c[1] + 5);
                g.drawCenteredString(font, UiUtil.trim(font, t.title().getString(), c[2] - 4), c[0] + c[2] / 2, c[1] + 25, err == null ? Theme.TEXT : Theme.TEXT_MUTED);
            }
            int army = n == null ? 0 : n.soldierCount, max = n == null ? 1 : MilitaryManager.maxArmy(n);
            int iy = panelY + 82;
            UiUtil.text(g, font, Component.translatable("imperium.ui.army", n == null ? "-" : army + "/" + max, SELECTION.size()), panelX + 4, iy, Theme.TEXT);
            UiUtil.bar(g, panelX + 4, iy + 10, PANEL_W - 8, 3, army / (double) Math.max(1, max), Theme.BAD);
            UiUtil.text(g, font, Component.translatable(patrolMode ? "imperium.ui.patrol_hint" : "imperium.ui.army_hint"), panelX + 4, iy + 17,
                    patrolMode ? Theme.ACCENT : Theme.TEXT_DIM);
            UiUtil.text(g, font, Component.translatable("imperium.ui.army_hint2"), panelX + 4, iy + 27, Theme.TEXT_MUTED);
            int prisoners = 0;
            for (Entity e : minecraft.level.entitiesForRendering()) {
                if (e instanceof SoldierEntity s && s.isSurrendered() && n != null && s.getNationId() != n.id) prisoners++;
            }
            if (prisoners > 0) UiUtil.text(g, font, Component.translatable("imperium.ui.prisoners_near", prisoners), panelX + 4, iy + 37, Theme.WARN);
        } else if (tab == Tab.PEOPLE) {
            int px = panelX + 6, py = panelY + 6;
            if (n == null) {
                UiUtil.text(g, font, Component.translatable("imperium.hud.no_nation"), px, py, Theme.TEXT_DIM);
                return;
            }
            UiUtil.section(g, font, px, py, PANEL_W - 12, Component.translatable("imperium.ui.tab.people"));
            int y = py + 14;
            UiUtil.text(g, font, Component.translatable("imperium.ui.people.citizens", n.citizenCount, n.citizenCap), px, y, Theme.PURPLE);
            UiUtil.bar(g, px + 150, y + 2, PANEL_W - 162, 4, n.citizenCount / (double) Math.max(1, n.citizenCap), Theme.PURPLE);
            y += 13;
            UiUtil.text(g, font, Component.translatable("imperium.ui.people.employed", n.employed), px, y, Theme.GOOD);
            UiUtil.text(g, font, Component.translatable("imperium.ui.people.unemployed", n.unemployed), px + 140, y, n.unemployed > 0 ? Theme.WARN : Theme.TEXT_DIM);
            y += 11;
            UiUtil.text(g, font, Component.translatable("imperium.ui.people.married", n.marriedCount / 2), px, y, 0xFFE07AB0);
            UiUtil.text(g, font, Component.translatable("imperium.ui.people.mourning", n.mourningCount), px + 140, y, Theme.TEXT_DIM);
            y += 11;
            UiUtil.text(g, font, Component.translatable("imperium.ui.people.trapped", n.trappedCount), px, y, n.trappedCount > 0 ? Theme.BAD : Theme.TEXT_DIM);
            y += 13;
            g.drawWordWrap(font, Component.translatable("imperium.ui.people.hint"), px, y, PANEL_W - 12, Theme.TEXT_MUTED);
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
            g.drawString(font, Component.translatable("imperium.ui.editor.status", arena, Component.translatable(d.phase.key())), px, py + 120, Theme.TEXT_DIM);
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
        int w = 126;
        int x = width - MINI - 10 - w, y = height - 4 - 20 - counts.size() * 10;
        if (mapMode) x = width - w - 4;
        UiUtil.panel(g, x, y - 4, w, 24 + counts.size() * 10);
        UiUtil.text(g, font, Component.translatable("imperium.ui.selected", SELECTION.size()), x + 5, y, Theme.ACCENT);
        int yy = y + 11;
        for (Map.Entry<UnitType, Integer> e : counts.entrySet()) {
            UiUtil.icon10(g, unitIcon(e.getKey()), x + 5, yy - 1);
            UiUtil.text(g, font, e.getValue() + "× " + e.getKey().title().getString(), x + 18, yy, Theme.TEXT_DIM);
            yy += 10;
        }
        if (n > 0) UiUtil.bar(g, x + 5, yy + 2, w - 10, 3, hp / n, hp / n > 0.5 ? Theme.GOOD : hp / n > 0.25 ? Theme.WARN : Theme.BAD);
    }

    private void renderNotes(GuiGraphics g) {
        long now = System.currentTimeMillis();
        int y = TOP + 6;
        int shown = 0;
        for (ClientState.Note note : ClientState.NOTES) {
            long age = now - note.time();
            if (age > 12000 || shown >= 4) continue;
            int alpha = age > 10000 ? (int) (255 * (12000 - age) / 2000.0) : 255;
            if (alpha < 8) continue;
            String s = UiUtil.trim(font, note.text().getString(), Math.min(360, width - TOOL_W - 40));
            int w = font.width(s) + 12;
            int slide = age < 200 ? (int) ((200 - age) / 10) : 0;
            int x = 4 - slide;
            g.fill(x, y - 3, x + w, y + 10, Theme.alpha(Theme.PANEL_SOLID, alpha * 220 / 255));
            g.fill(x, y - 3, x + 2, y + 10, Theme.alpha(note.important() ? Theme.ACCENT : Theme.INFO, alpha));
            g.drawString(font, s, x + 7, y, (Math.max(8, alpha) << 24) | (note.important() ? 0xFFE7B0 : 0xE8ECF1), false);
            y += 15;
            shown++;
        }
    }

    private void renderHints(GuiGraphics g, Nation n) {
        Component hint;
        if (tool != Tool.NONE) hint = Component.translatable("imperium.ui.hint.editor");
        else if (patrolMode) hint = Component.translatable("imperium.ui.hint.patrol");
        else if (placement != null) hint = Component.translatable("imperium.ui.hint.place");
        else if (mapMode) hint = Component.translatable("imperium.ui.hint.map");
        else hint = Component.translatable("imperium.ui.hint.world");
        int w = font.width(hint);
        int x = Math.max(PANEL_W + 10, width / 2 - w / 2);
        if (x + w > width - MINI - 10) return;
        g.fill(x - 4, height - 15, x + w + 4, height - 3, Theme.alpha(Theme.PANEL_SOLID, 0xB0));
        UiUtil.text(g, font, hint, x, height - 13, patrolMode ? Theme.ACCENT : Theme.TEXT_DIM);
    }

    /** Patrol routes of the selected soldiers, as dotted lines. */
    private void renderPatrols(GuiGraphics g) {
        if (minecraft.level == null || SELECTION.isEmpty()) return;
        int done = 0;
        for (int id : SELECTION) {
            if (done > 12) break;
            if (!(minecraft.level.getEntity(id) instanceof SoldierEntity s)) continue;
            List<Vec3> route = s.clientPatrol();
            if (route.size() < 2) continue;
            done++;
            for (int i = 0; i + 1 < route.size(); i++) {
                Vec3 a = route.get(i), b = route.get(i + 1);
                double len = a.distanceTo(b);
                int steps = Math.max(2, (int) (len / (mapMode ? 4 : 1.5)));
                for (int k = 0; k <= steps; k++) {
                    Vec3 p = a.lerp(b, k / (double) steps);
                    double[] sp = mapMode ? new double[]{bigMap.toScreenX(p.x), bigMap.toScreenY(p.z)} : RtsCamera.project(p.x, p.y + 0.15, p.z);
                    if (sp == null) continue;
                    g.fill((int) sp[0] - 1, (int) sp[1] - 1, (int) sp[0] + 1, (int) sp[1] + 1, 0xC0E3B341);
                }
            }
            for (Vec3 p : route) {
                double[] sp = mapMode ? new double[]{bigMap.toScreenX(p.x), bigMap.toScreenY(p.z)} : RtsCamera.project(p.x, p.y + 0.2, p.z);
                if (sp != null) g.renderOutline((int) sp[0] - 3, (int) sp[1] - 3, 6, 6, 0xFFE3B341);
            }
        }
    }

    private void renderTooltips(GuiGraphics g, GameData d, Nation n, int mx, int my) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        if (tab == Tab.BUILD) {
            List<BuildingType> list = buildList();
            for (int i = 0; i < list.size(); i++) {
                int[] c = gridCell(i);
                if (UiUtil.inside(mx, my, c[0], c[1], CELL, CELL)) buildingTooltip(lines, list.get(i), n);
            }
        } else if (tab == Tab.ARMY) {
            UnitType[] types = UnitType.values();
            for (int i = 0; i < types.length; i++) {
                int[] c = unitCell(i);
                if (UiUtil.inside(mx, my, c[0], c[1], c[2], c[3])) unitTooltip(lines, types[i], d, n);
            }
        }
        if (lines.isEmpty() && topTip != null) {
            for (String part : topTip.getString().split("\n")) add(lines, Component.literal(part));
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
                if (s.isSurrendered()) add(lines, Component.translatable("imperium.ui.prisoner").withStyle(ChatFormatting.YELLOW));
                else add(lines, Component.translatable(s.getOrder().key()).withStyle(ChatFormatting.DARK_AQUA));
            } else if (hoverEntity instanceof CitizenEntity c) {
                nat = c.getNationId();
                add(lines, c.getProfession().title().copy().withStyle(ChatFormatting.GRAY));
                if (c.hasState(CitizenEntity.TRAPPED)) add(lines, Component.translatable("imperium.ui.state.trapped").withStyle(ChatFormatting.RED));
                if (c.hasState(CitizenEntity.MOURNING)) add(lines, Component.translatable("imperium.ui.state.mourning").withStyle(ChatFormatting.DARK_GRAY));
                if (c.hasState(CitizenEntity.MARRIED)) add(lines, Component.translatable("imperium.ui.state.married").withStyle(ChatFormatting.LIGHT_PURPLE));
                if (c.hasState(CitizenEntity.WORKING)) add(lines, Component.translatable("imperium.ui.state.working").withStyle(ChatFormatting.GREEN));
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
