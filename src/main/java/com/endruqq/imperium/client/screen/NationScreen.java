package com.endruqq.imperium.client.screen;

import com.endruqq.imperium.client.ClientState;
import com.endruqq.imperium.client.UiUtil;
import com.endruqq.imperium.client.ui.FlatButton;
import com.endruqq.imperium.client.ui.Theme;
import com.endruqq.imperium.entity.Profession;
import com.endruqq.imperium.game.BotAI;
import com.endruqq.imperium.game.CasusBelli;
import com.endruqq.imperium.game.DiplomacyManager;
import com.endruqq.imperium.game.DiplomacyManager.Action;
import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.GamePhase;
import com.endruqq.imperium.game.MilitaryManager;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.PeopleManager;
import com.endruqq.imperium.game.Proposal;
import com.endruqq.imperium.game.Relation;
import com.endruqq.imperium.game.RelationType;
import com.endruqq.imperium.game.ResourceType;
import com.endruqq.imperium.game.TaxLevel;
import com.endruqq.imperium.game.WarReasons;
import com.endruqq.imperium.game.building.Building;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Nation window: overview, people and jobs, diplomacy, and the list of nations to join. */
public class NationScreen extends Screen implements RtsChild {
    public static final int OVERVIEW = 0, DIPLOMACY = 1, LOBBY = 2, PEOPLE = 3;
    private static final int[] TAB_ORDER = {OVERVIEW, PEOPLE, DIPLOMACY, LOBBY};
    private static final String[] TAB_KEYS = {"imperium.ui.tab.overview", "imperium.ui.tab.diplomacy", "imperium.ui.tab.lobby", "imperium.ui.tab.people"};

    private final Screen parent;
    private int tab;
    private int selected = -1;
    private int lastVersion = -1;
    private int lastPeople = -1;
    private int left, top, w, h;
    private int peopleScroll;
    private int nationScroll;
    private int requestTimer;

    public NationScreen(Screen parent, int tab) {
        super(Component.translatable("imperium.ui.nation"));
        this.parent = parent;
        this.tab = tab;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void send(String action, CompoundTag t) {
        ImperiumNet.sendToServer(new ActionPacket(action, t));
    }

    private int cx() {
        return left + 8;
    }

    private int cy() {
        return top + 42;
    }

    private int cw() {
        return w - 16;
    }

    @Override
    protected void init() {
        w = Math.min(480, width - 16);
        h = Math.min(296, height - 16);
        left = (width - w) / 2;
        top = (height - h) / 2;
        lastVersion = ClientState.stateVersion;
        lastPeople = ClientState.peopleVersion;
        Nation me = ClientState.myNation();
        if (me == null && tab != LOBBY) tab = LOBBY;
        int tx = left + 6;
        for (int t : TAB_ORDER) {
            final int k = t;
            boolean enabled = t == LOBBY || me != null;
            int tw = Math.max(70, font.width(Component.translatable(TAB_KEYS[t])) + 16);
            addRenderableWidget(FlatButton.of(tx, top + 23, tw, 15, Component.translatable(TAB_KEYS[t]), FlatButton.Style.TAB, b -> {
                tab = k;
                rebuildWidgets();
            }).selected(tab == t).enabled(enabled));
            tx += tw + 2;
        }
        addRenderableWidget(FlatButton.of(left + w - 18, top + 3, 14, 14, Component.literal("✕"), FlatButton.Style.GHOST, b -> onClose()));
        switch (tab) {
            case OVERVIEW -> initOverview(me);
            case PEOPLE -> initPeople(me);
            case DIPLOMACY -> initDiplomacy(me);
            default -> initLobby(me);
        }
        if (tab == PEOPLE) send("people", new CompoundTag());
    }

    // ------------------------------------------------------------------ overview

    private int overviewSplitY() {
        return cy() + 72;
    }

    private void initOverview(Nation n) {
        if (n == null) return;
        int y = overviewSplitY() + 14;
        int x = cx() + 6;
        for (TaxLevel t : TaxLevel.values()) {
            addRenderableWidget(FlatButton.of(x + 52 + t.ordinal() * 50, y, 48, 14, Component.translatable(t.key()), b -> {
                CompoundTag tag = new CompoundTag();
                tag.putInt("level", t.ordinal());
                send("tax", tag);
            }).selected(n.tax == t).tip(Component.translatable("imperium.ui.tip.tax." + t.name().toLowerCase())));
        }
        Component borderLabel = Component.translatable(n.bordersClosed ? "imperium.ui.borders.closed" : "imperium.ui.borders.open");
        addRenderableWidget(FlatButton.of(x + 52, y + 17, 148, 14, borderLabel, n.bordersClosed ? FlatButton.Style.DANGER : FlatButton.Style.SUCCESS,
                b -> send("borders", new CompoundTag())).tip(Component.translatable("imperium.ui.tip.borders")));
    }

    private void renderOverview(GuiGraphics g, Nation n, int mx, int my) {
        if (n == null) return;
        GameData d = ClientState.data;
        int x = cx(), y = cy(), cw = cw();
        // stat cards
        int cards = 5, gap = 4, cardW = (cw - gap * (cards - 1)) / cards;
        String[] titles = {"imperium.ui.card.population", "imperium.ui.card.happiness", "imperium.ui.card.citizens", "imperium.ui.card.army", "imperium.ui.card.research"};
        for (int i = 0; i < cards; i++) {
            int bx = x + i * (cardW + gap);
            boolean hover = UiUtil.inside(mx, my, bx, y, cardW, 32);
            UiUtil.card(g, bx, y, cardW, 32, hover);
            UiUtil.text(g, font, Component.translatable(titles[i]), bx + 5, y + 4, Theme.TEXT_DIM);
            String value;
            double frac;
            int color;
            switch (i) {
                case 0 -> {
                    value = n.population + " / " + n.housing;
                    frac = n.population / (double) Math.max(1, n.housing);
                    color = n.population > n.housing ? Theme.BAD : Theme.INFO;
                }
                case 1 -> {
                    value = (int) n.happiness + "% → " + (int) n.happinessTarget + "%";
                    frac = n.happiness / 100.0;
                    color = n.happiness < 30 ? Theme.BAD : n.happiness > 70 ? Theme.GOOD : Theme.WARN;
                }
                case 2 -> {
                    value = n.citizenCount + " / " + n.citizenCap;
                    frac = n.employed / (double) Math.max(1, n.citizenCount);
                    color = Theme.PURPLE;
                }
                case 3 -> {
                    value = n.soldierCount + " / " + MilitaryManager.maxArmy(n);
                    frac = n.soldierCount / (double) Math.max(1, MilitaryManager.maxArmy(n));
                    color = Theme.BAD;
                }
                default -> {
                    value = (int) n.research + "  " + UiUtil.signed(n.researchDelta);
                    frac = Math.min(1, n.techs.size() / (double) Tech.values().length);
                    color = Theme.INFO;
                }
            }
            UiUtil.text(g, font, value, bx + 5, y + 14, Theme.TEXT);
            UiUtil.bar(g, bx + 5, y + 25, cardW - 10, 3, frac, color);
        }
        // resources
        int ry = y + 37;
        UiUtil.card(g, x, ry, cw, 24, false);
        ResourceType[] res = ResourceType.values();
        int cell = cw / res.length;
        for (int i = 0; i < res.length; i++) {
            ResourceType t = res[i];
            int rx = x + i * cell + 4;
            g.renderItem(t.icon(), rx, ry + 4);
            double delta = n.lastDelta.get(t);
            UiUtil.text(g, font, UiUtil.compact(n.stock.get(t)), rx + 18, ry + 4, t == ResourceType.FOOD && n.famine ? Theme.BAD : Theme.TEXT);
            UiUtil.text(g, font, UiUtil.signed(delta), rx + 18, ry + 13, delta < -0.5 ? Theme.BAD : delta > 0.5 ? Theme.GOOD : Theme.TEXT_MUTED);
        }
        // state section
        int sy = overviewSplitY();
        int half = cw / 2 - 4;
        UiUtil.section(g, font, x, sy, half, Component.translatable("imperium.ui.section.state"));
        UiUtil.text(g, font, Component.translatable("imperium.ui.tax"), x + 6, sy + 17, Theme.TEXT_DIM);
        UiUtil.text(g, font, Component.translatable("imperium.ui.borders"), x + 6, sy + 34, Theme.TEXT_DIM);
        int bx = x + 6, by = sy + 52;
        if (n.famine) bx += UiUtil.badge(g, font, bx, by, Component.translatable("imperium.ui.stat.famine"), Theme.BAD) + 3;
        else if (n.abundant()) bx += UiUtil.badge(g, font, bx, by, Component.translatable("imperium.ui.stat.abundance"), Theme.GOOD) + 3;
        if (n.prosperity) bx += UiUtil.badge(g, font, bx, by, Component.translatable("imperium.ui.stat.prosperity"), Theme.INFO) + 3;
        if (d.atWarWithAnyone(n.id)) bx += UiUtil.badge(g, font, bx, by, Component.translatable("imperium.ui.stat.war"), Theme.BAD) + 3;
        if (n.lostLands > 0) bx += UiUtil.badge(g, font, bx, by, Component.translatable("imperium.ui.stat.lost_lands", n.lostLands), Theme.WARN) + 3;
        if (n.mourningCount > 0) UiUtil.badge(g, font, bx, by, Component.translatable("imperium.ui.stat.mourning", n.mourningCount), Theme.TEXT_DIM);
        // buildings
        int bx0 = x + half + 8;
        UiUtil.section(g, font, bx0, sy, half, Component.translatable("imperium.ui.buildings"));
        int col = 0, row = 0, perRow = Math.max(1, half / 56);
        for (BuildingType t : Buildings.all()) {
            int a = d.countActive(n.id, t.id), all = d.countAll(n.id, t.id);
            if (all == 0) continue;
            int ix = bx0 + col * 56, iy = sy + 13 + row * 18;
            if (iy > sy + 13 + 18 * 2) break;
            g.renderItem(t.icon(), ix, iy);
            UiUtil.text(g, font, a + "/" + all, ix + 18, iy + 5, a == all ? Theme.TEXT : Theme.WARN);
            if (UiUtil.inside(mx, my, ix, iy, 54, 16)) hoverTip = t.name();
            if (++col >= perRow) {
                col = 0;
                row++;
            }
        }
        if (col == 0 && row == 0) UiUtil.text(g, font, "-", bx0 + 4, sy + 16, Theme.TEXT_MUTED);
        // chronicle
        int ly = sy + 70;
        UiUtil.section(g, font, x, ly, cw, Component.translatable("imperium.ui.log"));
        ly += 12;
        int lines = Math.max(0, (top + h - 6 - ly) / 10);
        for (int i = Math.max(0, n.log.size() - lines); i < n.log.size(); i++) {
            g.fill(x + 2, ly + 3, x + 4, ly + 5, Theme.ACCENT_DARK);
            UiUtil.text(g, font, UiUtil.trim(font, n.log.get(i).getString(), cw - 12), x + 8, ly, Theme.TEXT_DIM);
            ly += 10;
        }
    }

    private Component hoverTip;

    // ------------------------------------------------------------------ people

    private List<BuildingType> workplaceTypes(Nation n) {
        List<BuildingType> l = new ArrayList<>();
        for (BuildingType t : Buildings.all()) {
            if (t.isWorkplace() && ClientState.data.countActive(n.id, t.id) > 0) l.add(t);
        }
        return l;
    }

    private int[] workers(Nation n, BuildingType t) {
        int used = 0, slots = 0;
        for (Building b : ClientState.data.buildings.values()) {
            if (b.nation == n.id && b.isActive() && b.typeId.equals(t.id)) {
                used += b.workers;
                slots += t.workerSlots;
            }
        }
        return new int[]{used, slots};
    }

    private void jobs(String op, String type) {
        CompoundTag t = new CompoundTag();
        t.putString("op", op);
        if (type != null) t.putString("type", type);
        send("jobs", t);
    }

    private void initPeople(Nation n) {
        if (n == null) return;
        int x = cx(), y = cy();
        addRenderableWidget(FlatButton.of(x + cw() - 238, y, 132, 15, Component.translatable("imperium.ui.people.hire_all"), FlatButton.Style.PRIMARY,
                b -> jobs("auto", null)).tip(Component.translatable("imperium.ui.tip.hire_all")));
        addRenderableWidget(FlatButton.of(x + cw() - 102, y, 102, 15,
                Component.translatable(n.autoJobs ? "imperium.ui.people.auto_on" : "imperium.ui.people.auto_off"), b -> jobs("toggle_auto", null))
                .selected(n.autoJobs).tip(Component.translatable("imperium.ui.tip.auto_jobs")));
        int ry = y + 46;
        int half = cw() / 2 - 4;
        for (BuildingType t : workplaceTypes(n)) {
            if (ry > top + h - 22) break;
            int[] wk = workers(n, t);
            addRenderableWidget(FlatButton.of(x + half - 30, ry + 2, 14, 13, Component.literal("−"), b -> jobs("fire", t.id))
                    .enabled(wk[0] > 0).tip(Component.translatable("imperium.ui.tip.fire", t.profession.title())));
            addRenderableWidget(FlatButton.of(x + half - 14, ry + 2, 14, 13, Component.literal("+"), FlatButton.Style.SUCCESS, b -> jobs("hire", t.id))
                    .enabled(wk[0] < wk[1] && n.unemployed > 0).tip(Component.translatable("imperium.ui.tip.hire", t.profession.title(), t.name())));
            ry += 18;
        }
    }

    private void renderPeople(GuiGraphics g, Nation n, int mx, int my) {
        if (n == null) return;
        GameData d = ClientState.data;
        int x = cx(), y = cy();
        // summary chips, wrapped so they never run under the buttons on the right
        Component cap = Component.translatable("imperium.ui.people.citizens", n.citizenCount, n.citizenCap);
        List<Component> chips = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        chips.add(cap);
        colors.add(Theme.PURPLE);
        chips.add(Component.translatable("imperium.ui.people.employed", n.employed));
        colors.add(Theme.GOOD);
        chips.add(Component.translatable("imperium.ui.people.unemployed", n.unemployed));
        colors.add(n.unemployed > 0 ? Theme.WARN : Theme.TEXT_DIM);
        chips.add(Component.translatable("imperium.ui.people.married", n.marriedCount / 2));
        colors.add(0xFFE07AB0);
        if (n.mourningCount > 0) {
            chips.add(Component.translatable("imperium.ui.people.mourning", n.mourningCount));
            colors.add(Theme.TEXT_DIM);
        }
        if (n.trappedCount > 0) {
            chips.add(Component.translatable("imperium.ui.people.trapped", n.trappedCount));
            colors.add(Theme.BAD);
        }
        int bx = x, by = y + 2, limit = x + cw() - 244;
        for (int i = 0; i < chips.size(); i++) {
            int cwid = font.width(chips.get(i)) + 6;
            if (bx > x && bx + cwid > limit) {
                bx = x;
                by += 14;
                limit = x + cw();
            }
            UiUtil.badge(g, font, bx, by, chips.get(i), colors.get(i));
            if (i == 0 && UiUtil.inside(mx, my, bx, by, cwid, 10)) {
                hoverTip = Component.translatable("imperium.ui.tip.citizen_cap", 4, PeopleManager.timeBonus(d), PeopleManager.techBonus(n),
                        PeopleManager.buildingBonus(d, n), n.population);
            }
            bx += cwid + 3;
        }
        int half = cw() / 2 - 4;
        // workplaces
        int sy = y + 32;
        UiUtil.section(g, font, x, sy, half, Component.translatable("imperium.ui.people.workplaces"));
        int ry = sy + 14;
        List<BuildingType> types = workplaceTypes(n);
        if (types.isEmpty()) g.drawWordWrap(font, Component.translatable("imperium.ui.people.no_workplaces"), x + 4, ry + 4, half - 8, Theme.TEXT_MUTED);
        for (BuildingType t : types) {
            if (ry > top + h - 22) break;
            int[] wk = workers(n, t);
            UiUtil.card(g, x, ry, half - 34, 16, UiUtil.inside(mx, my, x, ry, half - 34, 16));
            UiUtil.icon10(g, t.icon(), x + 3, ry + 3);
            UiUtil.text(g, font, UiUtil.trim(font, t.name().getString(), half - 110), x + 16, ry + 4, Theme.TEXT);
            String cnt = wk[0] + "/" + wk[1];
            UiUtil.text(g, font, cnt, x + half - 70 - font.width(cnt), ry + 4, wk[0] == 0 ? Theme.BAD : wk[0] < wk[1] ? Theme.WARN : Theme.GOOD);
            UiUtil.bar(g, x + half - 66, ry + 6, 28, 4, wk[0] / (double) Math.max(1, wk[1]), wk[0] < wk[1] ? Theme.WARN : Theme.GOOD);
            if (UiUtil.inside(mx, my, x, ry, half - 34, 16)) {
                hoverTip = Component.translatable("imperium.ui.tip.workplace", t.profession.title(), (int) Math.round(t.unstaffedOutput * 100));
            }
            ry += 18;
        }
        // citizens
        int px = x + half + 8;
        UiUtil.section(g, font, px, sy, half, Component.translatable("imperium.ui.people.list"));
        List<ClientState.PersonInfo> people = ClientState.people;
        int rows = Math.max(1, (top + h - 8 - (sy + 14)) / 11);
        peopleScroll = Math.max(0, Math.min(peopleScroll, Math.max(0, people.size() - rows)));
        int ly = sy + 14;
        g.enableScissor(px, ly, px + half, top + h - 6);
        for (int i = peopleScroll; i < people.size() && i < peopleScroll + rows; i++) {
            ClientState.PersonInfo p = people.get(i);
            boolean hover = UiUtil.inside(mx, my, px, ly, half, 11);
            if (hover) g.fill(px, ly, px + half, ly + 11, Theme.CARD_HOVER);
            Profession prof = Profession.byOrdinal(p.job());
            UiUtil.icon10(g, prof.employed() ? prof.tool() : new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS), px + 1, ly);
            UiUtil.text(g, font, UiUtil.trim(font, p.name(), 82), px + 13, ly + 1, p.trapped() >= 0 ? Theme.BAD : Theme.TEXT);
            int sx = px + 98;
            UiUtil.text(g, font, UiUtil.trim(font, prof.title().getString(), 60), sx, ly + 1, prof.employed() ? Theme.TEXT_DIM : Theme.TEXT_MUTED);
            sx += 62;
            if (p.trapped() >= 0) {
                UiUtil.text(g, font, "⚠ " + UiUtil.time(p.trapped()), sx, ly + 1, Theme.BAD);
            } else if (p.mourning() > 0) {
                UiUtil.text(g, font, "✝ " + UiUtil.time(p.mourning()), sx, ly + 1, Theme.TEXT_MUTED);
            } else if (!p.spouse().isEmpty()) {
                UiUtil.text(g, font, "♥", sx, ly + 1, 0xFFE07AB0);
            }
            if (hover) hoverTip = personTip(p);
            ly += 11;
        }
        g.disableScissor();
        if (people.isEmpty()) UiUtil.text(g, font, Component.translatable("imperium.ui.people.empty"), px + 4, sy + 18, Theme.TEXT_MUTED);
        if (people.size() > rows) {
            int trackH = top + h - 8 - (sy + 14);
            int thumb = Math.max(8, trackH * rows / people.size());
            int ty = sy + 14 + (trackH - thumb) * peopleScroll / Math.max(1, people.size() - rows);
            g.fill(px + half - 2, ty, px + half, ty + thumb, Theme.BORDER_LIGHT);
        }
    }

    private Component personTip(ClientState.PersonInfo p) {
        MutableComponent c = Component.literal(p.name()).withStyle(ChatFormatting.GOLD);
        Profession prof = Profession.byOrdinal(p.job());
        c.append(Component.literal("\n")).append(prof.title().copy().withStyle(ChatFormatting.GRAY));
        BuildingType wt = Buildings.get(p.work());
        if (wt != null) c.append(Component.literal(" - ").withStyle(ChatFormatting.GRAY)).append(wt.name().copy().withStyle(ChatFormatting.GRAY));
        if (!p.spouse().isEmpty()) c.append(Component.literal("\n")).append(Component.translatable(p.female() ? "imperium.ui.people.wife_of" : "imperium.ui.people.husband_of", p.spouse())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        if (p.children() > 0) c.append(Component.literal("\n")).append(Component.translatable("imperium.ui.people.children", p.children()).withStyle(ChatFormatting.GRAY));
        if (p.mourning() > 0) c.append(Component.literal("\n")).append(Component.translatable("imperium.ui.people.mourns", p.mourningFor(), UiUtil.time(p.mourning()))
                .withStyle(ChatFormatting.DARK_GRAY));
        if (p.trapped() >= 0) c.append(Component.literal("\n")).append(Component.translatable("imperium.ui.people.is_trapped", UiUtil.time(p.trapped()))
                .withStyle(ChatFormatting.RED));
        return c;
    }

    // ------------------------------------------------------------------ diplomacy

    private List<Nation> others(Nation me) {
        List<Nation> l = new ArrayList<>();
        for (Nation n : ClientState.data.nations.values()) if (me == null || n.id != me.id) l.add(n);
        return l;
    }

    private record Act(Action action, int group, Component reason, FlatButton.Style style) {
    }

    /** Diplomatic actions towards {@code o}, each with the reason why it is unavailable (null = available). */
    private List<Act> actions(GameData d, Nation me, Nation o) {
        List<Act> l = new ArrayList<>();
        Relation r = d.relations.get(Relation.key(me.id, o.id));
        RelationType rt = r == null ? RelationType.PEACE : r.type;
        boolean war = rt == RelationType.WAR;
        boolean running = d.phase == GamePhase.RUNNING && !o.eliminated;
        Component notRunning = running ? null : Component.translatable("imperium.err.not_running");
        Component needDiplo = me.has(Tech.DIPLOMACY) ? null : Component.translatable("imperium.err.locked", Tech.DIPLOMACY.title());
        Component needTrade = me.has(Tech.TRADE) ? null : Component.translatable("imperium.err.locked", Tech.TRADE.title());
        Component needSpy = me.has(Tech.ESPIONAGE) ? null : Component.translatable("imperium.err.locked", Tech.ESPIONAGE.title());
        Component atWar = war ? null : Component.translatable("imperium.err.not_at_war");
        Component notWar = war ? Component.translatable("imperium.err.already_war") : null;
        // war & peace
        if (!war) {
            Component why = notRunning;
            if (why == null && d.inGracePeriod()) why = Component.translatable("imperium.err.grace", ClientState.graceSeconds());
            if (why == null && rt == RelationType.ALLIANCE) why = Component.translatable("imperium.err.allied");
            if (why == null && r != null && d.gameTick < r.truceUntil) why = Component.translatable("imperium.err.truce", (r.truceUntil - d.gameTick) / 20);
            l.add(new Act(Action.DECLARE_WAR, 0, why, FlatButton.Style.DANGER));
        } else {
            l.add(new Act(Action.PROPOSE_PEACE, 0, notRunning, FlatButton.Style.SUCCESS));
            l.add(new Act(Action.PEACE_PROVINCE, 0, notRunning, FlatButton.Style.NORMAL));
            l.add(new Act(Action.PEACE_GOLD, 0, me.stock.get(ResourceType.GOLD) < DiplomacyManager.PEACE_GOLD
                    ? Component.translatable("imperium.err.cost", DiplomacyManager.PEACE_GOLD + " ").append(ResourceType.GOLD.label()) : notRunning, FlatButton.Style.NORMAL));
        }
        boolean allied = rt == RelationType.ALLIANCE;
        if (allied) {
            l.add(new Act(Action.CALL_TO_ARMS, 0, d.atWarWithAnyone(me.id) ? notRunning : Component.translatable("imperium.err.no_common_war"), FlatButton.Style.NORMAL));
        }
        // treaties
        if (allied) l.add(new Act(Action.BREAK_ALLIANCE, 1, notRunning, FlatButton.Style.NORMAL));
        else l.add(new Act(Action.PROPOSE_ALLIANCE, 1, first(notRunning, needDiplo, notWar), FlatButton.Style.NORMAL));
        if (rt == RelationType.NON_AGGRESSION) l.add(new Act(Action.BREAK_NAP, 1, notRunning, FlatButton.Style.NORMAL));
        else if (!allied) l.add(new Act(Action.PROPOSE_NAP, 1, first(notRunning, needDiplo, rt == RelationType.PEACE ? null
                : Component.translatable("imperium.err.bad_relation")), FlatButton.Style.NORMAL));
        if (r != null && r.trade) l.add(new Act(Action.CANCEL_TRADE, 1, notRunning, FlatButton.Style.NORMAL));
        else l.add(new Act(Action.PROPOSE_TRADE, 1, first(notRunning, needTrade, notWar), FlatButton.Style.NORMAL));
        if (r != null && r.access) l.add(new Act(Action.REVOKE_ACCESS, 1, notRunning, FlatButton.Style.NORMAL));
        else l.add(new Act(Action.PROPOSE_ACCESS, 1, first(notRunning, notWar), FlatButton.Style.NORMAL));
        if (r != null && r.guarantees(me.id, o.id)) l.add(new Act(Action.REVOKE_GUARANTEE, 1, notRunning, FlatButton.Style.NORMAL));
        else l.add(new Act(Action.GUARANTEE, 1, first(notRunning, needDiplo, notWar), FlatButton.Style.NORMAL));
        // gestures
        l.add(new Act(Action.SEND_GIFT, 2, first(notRunning, me.stock.get(ResourceType.GOLD) < DiplomacyManager.GIFT_GOLD
                ? Component.translatable("imperium.err.not_enough") : null), FlatButton.Style.NORMAL));
        l.add(new Act(Action.SEND_FOOD, 2, first(notRunning, me.stock.get(ResourceType.FOOD) < DiplomacyManager.FOOD_AID
                ? Component.translatable("imperium.err.not_enough") : null), FlatButton.Style.NORMAL));
        l.add(new Act(Action.INSULT, 2, notRunning, FlatButton.Style.NORMAL));
        l.add(new Act(Action.DENOUNCE, 2, notRunning, FlatButton.Style.NORMAL));
        // demands & intelligence
        if (!allied) l.add(new Act(Action.DEMAND_TRIBUTE, 3, notRunning, FlatButton.Style.NORMAL));
        if (!allied && !war) l.add(new Act(Action.DEMAND_PROVINCE, 3, notRunning, FlatButton.Style.NORMAL));
        l.add(new Act(Action.FABRICATE_CLAIM, 3, first(notRunning, needSpy, notWar, WarReasons.claimInProgress(d, me.id, o.id) != null
                ? Component.translatable("imperium.err.claim_in_progress") : null), FlatButton.Style.NORMAL));
        return l;
    }

    private static Component first(Component... reasons) {
        for (Component c : reasons) if (c != null) return c;
        return null;
    }

    private int detailX() {
        return left + 148;
    }

    private int detailW() {
        return w - 156;
    }

    private int incoming(GameData d, Nation me) {
        int c = 0;
        for (Proposal p : d.proposals) if (p.to == me.id) c++;
        return c;
    }

    private void initDiplomacy(Nation me) {
        if (me == null) return;
        GameData d = ClientState.data;
        List<Nation> list = others(me);
        if ((selected < 0 || d.nation(selected) == null) && !list.isEmpty()) selected = list.get(0).id;
        int y = cy();
        // incoming proposals
        int shown = 0;
        for (Proposal p : d.proposals) {
            if (p.to != me.id || shown >= 2) continue;
            final int pid = p.id;
            int py = y + shown * 17;
            addRenderableWidget(FlatButton.of(left + w - 122, py + 1, 56, 14, Component.translatable("imperium.ui.accept"), FlatButton.Style.SUCCESS, b -> respond(pid, true)));
            addRenderableWidget(FlatButton.of(left + w - 64, py + 1, 56, 14, Component.translatable("imperium.ui.reject"), FlatButton.Style.DANGER, b -> respond(pid, false)));
            shown++;
        }
        Nation other = d.nation(selected);
        if (other == null) return;
        int ax = detailX(), aw = detailW();
        int ay = actionsY(d, me);
        int col = 3, bw = (aw - (col - 1) * 3) / col;
        int[] counts = new int[4];
        List<Act> acts = actions(d, me, other);
        int gy = ay;
        for (int grp = 0; grp < 4; grp++) {
            int i = 0;
            boolean any = false;
            for (Act a : acts) {
                if (a.group() != grp) continue;
                any = true;
                int bx = ax + (i % col) * (bw + 3), by = gy + 11 + (i / col) * 16;
                MutableComponent tip = Component.translatable(a.action().key() + ".tip").withStyle(ChatFormatting.GRAY);
                if (a.reason() != null) tip = Component.empty().append(a.reason().copy().withStyle(ChatFormatting.RED)).append("\n").append(tip);
                if (a.action() == Action.DECLARE_WAR) tip.append("\n").append(Component.translatable("imperium.ui.war_confirm").withStyle(ChatFormatting.YELLOW));
                Action act = a.action();
                addRenderableWidget(FlatButton.of(bx, by, bw, 14, Component.translatable(act.key()), a.style(), b -> {
                    if (act == Action.DECLARE_WAR && !hasShiftDown()) {
                        if (minecraft.player != null) minecraft.player.displayClientMessage(Component.translatable("imperium.ui.war_confirm"), true);
                        return;
                    }
                    CompoundTag tag = new CompoundTag();
                    tag.putInt("target", selected);
                    tag.putInt("action", act.ordinal());
                    send("diplomacy", tag);
                }).enabled(a.reason() == null).tip(tip));
                i++;
            }
            counts[grp] = i;
            if (any) gy += 11 + ((i + col - 1) / col) * 16 + 3;
        }
    }

    /** y where the action buttons start (below the incoming proposals and details) */
    private int actionsY(GameData d, Nation me) {
        int y = cy() + Math.min(2, incoming(d, me)) * 17;
        return y + 92;
    }

    private void respond(int id, boolean accept) {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putBoolean("accept", accept);
        send("proposal", t);
    }

    private void renderDiplomacy(GuiGraphics g, Nation me, int mx, int my) {
        if (me == null) return;
        GameData d = ClientState.data;
        int y = cy();
        // incoming proposals banner
        int shown = 0;
        for (Proposal p : d.proposals) {
            if (p.to != me.id || shown >= 2) continue;
            Nation from = d.nation(p.from);
            int py = y + shown * 17;
            g.fill(cx(), py, left + w - 6, py + 16, Theme.alpha(Theme.ACCENT, 0x30));
            g.renderOutline(cx(), py, left + w - 6 - cx(), 16, Theme.ACCENT_DARK);
            Component txt = Component.translatable("imperium.ui.proposal", from == null ? "?" : from.name, DiplomacyManager.describe(d, p));
            UiUtil.text(g, font, UiUtil.trim(font, "✉ " + txt.getString(), w - 150), cx() + 4, py + 4, Theme.ACCENT);
            long left2 = Math.max(0, (p.expires - d.gameTick) / 20);
            UiUtil.text(g, font, UiUtil.time(left2), left + w - 152, py + 4, Theme.TEXT_DIM);
            shown++;
        }
        int listY = y + shown * 17;
        // nation list
        int ly = listY;
        List<Nation> list = others(me);
        int rows = Math.max(1, (top + h - 6 - listY) / 25);
        nationScroll = Math.max(0, Math.min(nationScroll, Math.max(0, list.size() - rows)));
        for (int i = nationScroll; i < list.size() && i < nationScroll + rows; i++) {
            Nation o = list.get(i);
            Relation r = d.relations.get(Relation.key(me.id, o.id));
            RelationType rt = r == null ? RelationType.PEACE : r.type;
            boolean sel = o.id == selected;
            boolean hover = UiUtil.inside(mx, my, cx(), ly, 132, 23);
            g.fill(cx(), ly, cx() + 132, ly + 23, sel ? Theme.CARD_SELECTED : hover ? Theme.CARD_HOVER : Theme.CARD);
            g.renderOutline(cx(), ly, 132, 23, sel ? Theme.ACCENT : Theme.BORDER);
            g.fill(cx() + 1, ly + 1, cx() + 4, ly + 22, 0xFF000000 | o.rgb());
            UiUtil.text(g, font, UiUtil.trim(font, o.name, 120), cx() + 8, ly + 3, o.eliminated ? Theme.TEXT_MUTED : Theme.TEXT);
            UiUtil.text(g, font, Component.translatable(rt.key()), cx() + 8, ly + 13, 0xFF000000 | rt.color);
            int op = r == null ? 0 : r.opinionOf(o.id, me.id);
            int barX = cx() + 82;
            g.fill(barX, ly + 15, barX + 44, ly + 18, 0xFF0C1015);
            int mid = barX + 22;
            int len = (int) Math.round(22 * Math.abs(op) / 100.0);
            if (op >= 0) g.fill(mid, ly + 15, mid + len, ly + 18, Theme.GOOD);
            else g.fill(mid - len, ly + 15, mid, ly + 18, Theme.BAD);
            g.vLine(mid, ly + 14, ly + 18, Theme.TEXT_MUTED);
            ly += 25;
        }
        Nation o = d.nation(selected);
        if (o == null) return;
        Relation r = d.relations.get(Relation.key(me.id, o.id));
        RelationType rt = r == null ? RelationType.PEACE : r.type;
        int x = detailX(), dw = detailW();
        int dy = listY;
        UiUtil.card(g, x, dy, dw, 88, false);
        g.fill(x + 4, dy + 4, x + 14, dy + 14, 0xFF000000 | o.rgb());
        UiUtil.text(g, font, o.name, x + 18, dy + 5, Theme.ACCENT);
        int bx = x + 22 + font.width(o.name);
        if (o.bot) bx += UiUtil.badge(g, font, bx, dy + 4, Component.literal("BOT"), Theme.TEXT_DIM) + 3;
        bx += UiUtil.badge(g, font, bx, dy + 4, Component.translatable(rt.key()), 0xFF000000 | rt.color) + 3;
        if (o.bordersClosed) UiUtil.badge(g, font, bx, dy + 4, Component.translatable("imperium.ui.borders.closed_short"), Theme.WARN);
        int c1 = x + 6, c2 = x + dw / 2 + 2, ry = dy + 19;
        int theirOpinion = r == null ? 0 : r.opinionOf(o.id, me.id);
        UiUtil.text(g, font, Component.translatable("imperium.ui.opinion", theirOpinion), c1, ry, theirOpinion < 0 ? Theme.BAD : Theme.GOOD);
        UiUtil.text(g, font, Component.translatable("imperium.ui.strength", (int) BotAI.strength(d, o), (int) BotAI.strength(d, me)), c2, ry, Theme.TEXT_DIM);
        ry += 10;
        List<String> flags = new ArrayList<>();
        if (r != null && r.trade) flags.add(Component.translatable("imperium.ui.trade_active").getString());
        if (r != null && r.access) flags.add(Component.translatable("imperium.ui.access_active").getString());
        if (r != null && r.guarantees(me.id, o.id)) flags.add(Component.translatable("imperium.ui.we_guarantee").getString());
        if (r != null && r.guarantees(o.id, me.id)) flags.add(Component.translatable("imperium.ui.they_guarantee").getString());
        UiUtil.text(g, font, UiUtil.trim(font, flags.isEmpty() ? "-" : String.join(" · ", flags), dw - 12), c1, ry, Theme.INFO);
        ry += 10;
        if (rt == RelationType.WAR && r != null) {
            UiUtil.text(g, font, Component.translatable("imperium.ui.war_score", r.scoreOf(me.id), r.scoreOf(o.id)), c1, ry, Theme.WARN);
        } else if (r != null && d.gameTick < r.truceUntil) {
            UiUtil.text(g, font, Component.translatable("imperium.ui.truce", UiUtil.time((r.truceUntil - d.gameTick) / 20)), c1, ry, Theme.INFO);
        } else if (d.inGracePeriod()) {
            UiUtil.text(g, font, Component.translatable("imperium.hud.grace", UiUtil.time(ClientState.graceSeconds())), c1, ry, Theme.INFO);
        }
        String members = o.memberNames.isEmpty() ? "-" : String.join(", ", o.memberNames);
        UiUtil.text(g, font, UiUtil.trim(font, Component.translatable("imperium.ui.members", members).getString(), dw / 2 - 8), c2, ry, Theme.TEXT_MUTED);
        ry += 13;
        // reasons for war
        UiUtil.text(g, font, Component.translatable("imperium.ui.cb.ours"), c1, ry, Theme.TEXT_DIM);
        int rx = c1 + font.width(Component.translatable("imperium.ui.cb.ours")) + 4;
        List<CasusBelli.Type> ours = WarReasons.reasons(d, me.id, o.id);
        if (ours.isEmpty()) {
            UiUtil.text(g, font, UiUtil.trim(font, Component.translatable("imperium.ui.cb.none").getString(), x + dw - rx - 4), rx, ry, Theme.TEXT_MUTED);
            if (UiUtil.inside(mx, my, c1, ry - 1, dw - 12, 10)) hoverTip = Component.translatable("imperium.ui.tip.aggression");
        }
        for (CasusBelli.Type t : ours) {
            long s = WarReasons.secondsLeft(d, me.id, o.id, t);
            Component lbl = s > 0 ? Component.translatable("imperium.ui.cb.timed", t.title(), UiUtil.time(s)) : t.title();
            if (rx + font.width(lbl) + 6 > x + dw - 4) break;
            int bw2 = UiUtil.badge(g, font, rx, ry - 1, lbl, Theme.GOOD);
            if (UiUtil.inside(mx, my, rx, ry - 1, bw2, 10)) hoverTip = Component.translatable(t.key() + ".tip");
            rx += bw2 + 3;
        }
        ry += 12;
        UiUtil.text(g, font, Component.translatable("imperium.ui.cb.theirs"), c1, ry, Theme.TEXT_DIM);
        rx = c1 + font.width(Component.translatable("imperium.ui.cb.theirs")) + 4;
        List<CasusBelli.Type> theirs = WarReasons.reasons(d, o.id, me.id);
        if (theirs.isEmpty()) UiUtil.text(g, font, "-", rx, ry, Theme.TEXT_MUTED);
        for (CasusBelli.Type t : theirs) {
            if (rx + font.width(t.title()) + 6 > x + dw - 4) break;
            rx += UiUtil.badge(g, font, rx, ry - 1, t.title(), Theme.BAD) + 3;
        }
        CasusBelli claim = WarReasons.claimInProgress(d, me.id, o.id);
        if (claim != null) {
            ry += 12;
            UiUtil.text(g, font, Component.translatable("imperium.ui.cb.forging", UiUtil.time(Math.max(0, (claim.expires - d.gameTick) / 20))), c1, ry, Theme.PURPLE);
        }
        // action group titles
        int ay = actionsY(d, me);
        List<Act> acts = actions(d, me, o);
        String[] groups = {"imperium.ui.diplo.group.war", "imperium.ui.diplo.group.treaties", "imperium.ui.diplo.group.gestures", "imperium.ui.diplo.group.demands"};
        int gy = ay;
        for (int grp = 0; grp < 4; grp++) {
            int n = 0;
            for (Act a : acts) if (a.group() == grp) n++;
            if (n == 0) continue;
            UiUtil.section(g, font, x, gy, dw, Component.translatable(groups[grp]));
            gy += 11 + ((n + 2) / 3) * 16 + 3;
        }
    }

    // ------------------------------------------------------------------ lobby

    private void initLobby(Nation me) {
        int y = cy() + 14;
        int colW = (cw() - 6) / 2;
        int i = 0;
        for (Nation n : ClientState.data.nations.values()) {
            final int id = n.id;
            int x = cx() + (i % 2) * (colW + 6);
            int yy = y + (i / 2) * 32;
            if (yy > top + h - 40) break;
            boolean mine = me != null && me.id == id;
            addRenderableWidget(FlatButton.of(x + colW - 66, yy + 8, 60, 14, Component.translatable(mine ? "imperium.ui.joined" : "imperium.ui.join"),
                    mine ? FlatButton.Style.NORMAL : FlatButton.Style.PRIMARY, btn -> {
                        CompoundTag t = new CompoundTag();
                        t.putInt("nation", id);
                        send("join", t);
                    }).enabled(!mine && !n.eliminated).selected(mine));
            i++;
        }
        if (me != null && (ClientState.data.phase != GamePhase.RUNNING || ClientState.isAdmin())) {
            addRenderableWidget(FlatButton.of(cx(), top + h - 22, 100, 15, Component.translatable("imperium.ui.leave"), FlatButton.Style.DANGER,
                    btn -> send("leave", new CompoundTag())));
        }
    }

    private void renderLobby(GuiGraphics g, Nation me, int mx, int my) {
        GameData d = ClientState.data;
        UiUtil.section(g, font, cx(), cy(), cw(), Component.translatable("imperium.ui.lobby_title"));
        int y = cy() + 14;
        int colW = (cw() - 6) / 2;
        int i = 0;
        for (Nation n : d.nations.values()) {
            int x = cx() + (i % 2) * (colW + 6);
            int yy = y + (i / 2) * 32;
            if (yy > top + h - 40) break;
            UiUtil.card(g, x, yy, colW, 29, UiUtil.inside(mx, my, x, yy, colW, 29));
            g.fill(x + 1, yy + 1, x + 4, yy + 28, 0xFF000000 | n.rgb());
            UiUtil.text(g, font, UiUtil.trim(font, n.name + (n.eliminated ? " ✝" : ""), colW - 80), x + 8, yy + 4, n.eliminated ? Theme.TEXT_MUTED : Theme.TEXT);
            long provinces = d.provincesOf(n.id).size();
            String members = n.memberNames.isEmpty() ? (n.bot ? "BOT" : "-") : String.join(", ", n.memberNames);
            UiUtil.text(g, font, UiUtil.trim(font, Component.translatable("imperium.ui.lobby_line", provinces, members).getString(), colW - 80), x + 8, yy + 16, Theme.TEXT_DIM);
            i++;
        }
        if (d.nations.isEmpty()) g.drawCenteredString(font, Component.translatable("imperium.ui.lobby_empty"), left + w / 2, top + h / 2, Theme.TEXT_DIM);
    }

    // ------------------------------------------------------------------ frame

    @Override
    public void tick() {
        if (minecraft.level == null) return;
        boolean changed = ClientState.stateVersion != lastVersion || ClientState.peopleVersion != lastPeople;
        if (changed && minecraft.level.getGameTime() % 10 == 0) rebuildWidgets();
        if (tab == PEOPLE && ++requestTimer >= 40) {
            requestTimer = 0;
            send("people", new CompoundTag());
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, Theme.SCRIM);
        Nation me = ClientState.myNation();
        Component title = me == null ? Component.translatable("imperium.ui.nation") : Component.literal(me.name);
        UiUtil.window(g, font, left, top, w, h, title, me == null ? Theme.ACCENT : 0xFF000000 | me.rgb());
        if (me != null) {
            Component sub = Component.translatable("imperium.ui.nation_sub", ClientState.data.provincesOf(me.id).size(), me.population);
            UiUtil.text(g, font, sub, left + w - 24 - font.width(sub), top + 6, Theme.TEXT_DIM);
        }
        g.hLine(left + 1, left + w - 2, top + 38, Theme.BORDER_LIGHT);
        hoverTip = null;
        switch (tab) {
            case OVERVIEW -> renderOverview(g, me, mx, my);
            case PEOPLE -> renderPeople(g, me, mx, my);
            case DIPLOMACY -> renderDiplomacy(g, me, mx, my);
            default -> renderLobby(g, me, mx, my);
        }
        super.render(g, mx, my, pt);
        if (hoverTip != null) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (String part : hoverTip.getString().split("\n")) lines.addAll(font.split(Component.literal(part), 240));
            if (!lines.isEmpty()) g.renderTooltip(font, lines, mx, my);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (tab == DIPLOMACY) {
            Nation me = ClientState.myNation();
            if (me == null) return false;
            GameData d = ClientState.data;
            int ly = cy() + Math.min(2, incoming(d, me)) * 17;
            List<Nation> list = others(me);
            for (int i = nationScroll; i < list.size(); i++) {
                if (UiUtil.inside(mx, my, cx(), ly, 132, 23)) {
                    selected = list.get(i).id;
                    rebuildWidgets();
                    return true;
                }
                ly += 25;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (tab == PEOPLE) {
            peopleScroll = Math.max(0, peopleScroll - (int) Math.signum(delta) * 3);
            return true;
        }
        if (tab == DIPLOMACY && mx < cx() + 136) {
            nationScroll = Math.max(0, nationScroll - (int) Math.signum(delta));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
