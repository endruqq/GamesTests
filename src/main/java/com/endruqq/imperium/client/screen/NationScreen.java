package com.endruqq.imperium.client.screen;

import com.endruqq.imperium.client.ClientState;
import com.endruqq.imperium.client.UiUtil;
import com.endruqq.imperium.game.BotAI;
import com.endruqq.imperium.game.DiplomacyManager;
import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.GamePhase;
import com.endruqq.imperium.game.MilitaryManager;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.Proposal;
import com.endruqq.imperium.game.Relation;
import com.endruqq.imperium.game.RelationType;
import com.endruqq.imperium.game.ResourceType;
import com.endruqq.imperium.game.TaxLevel;
import com.endruqq.imperium.game.building.BuildingType;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.network.ActionPacket;
import com.endruqq.imperium.network.ImperiumNet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Nation overview, diplomacy and the lobby to join a nation. */
public class NationScreen extends Screen implements RtsChild {
    private final Screen parent;
    private int tab;
    private int selected = -1;
    private int lastVersion = -1;
    private int left, top, w, h;

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

    @Override
    protected void init() {
        w = Math.min(420, width - 20);
        h = Math.min(250, height - 20);
        left = (width - w) / 2;
        top = (height - h) / 2;
        lastVersion = ClientState.stateVersion;
        Nation me = ClientState.myNation();
        if (me == null && tab != 2) tab = 2;
        String[] tabs = {"imperium.ui.tab.overview", "imperium.ui.tab.diplomacy", "imperium.ui.tab.lobby"};
        for (int i = 0; i < 3; i++) {
            final int k = i;
            Button b = addRenderableWidget(Button.builder(Component.translatable(tabs[i]), btn -> {
                tab = k;
                rebuildWidgets();
            }).bounds(left + i * 96, top - 16, 94, 14).build());
            b.active = tab != i && (i == 2 || me != null);
        }
        switch (tab) {
            case 0 -> initOverview(me);
            case 1 -> initDiplomacy(me);
            default -> initLobby(me);
        }
    }

    private void initOverview(Nation n) {
        if (n == null) return;
        int y = top + 6;
        for (TaxLevel t : TaxLevel.values()) {
            Button b = addRenderableWidget(Button.builder(Component.translatable(t.key()), btn -> {
                CompoundTag tag = new CompoundTag();
                tag.putInt("level", t.ordinal());
                send("tax", tag);
            }).bounds(left + w - 6 - (3 - t.ordinal()) * 54, y, 52, 14).build());
            b.active = n.tax != t;
        }
    }

    private List<Nation> others(Nation me) {
        List<Nation> l = new ArrayList<>();
        for (Nation n : ClientState.data.nations.values()) if (me == null || n.id != me.id) l.add(n);
        return l;
    }

    private void initDiplomacy(Nation me) {
        if (me == null) return;
        GameData d = ClientState.data;
        List<Nation> list = others(me);
        if (selected < 0 && !list.isEmpty()) selected = list.get(0).id;
        int y = top + 6;
        for (Nation o : list) {
            final int id = o.id;
            Button b = addRenderableWidget(Button.builder(Component.literal(UiUtil.trim(font, o.name, 110)), btn -> {
                selected = id;
                rebuildWidgets();
            }).bounds(left + 6, y, 120, 14).build());
            b.active = selected != id;
            y += 15;
            if (y > top + h - 20) break;
        }
        // proposals addressed to us
        int py = top + h - 6 - 15 * Math.min(4, d.proposals.size());
        for (Proposal p : d.proposals) {
            if (p.to != me.id) continue;
            final int pid = p.id;
            addRenderableWidget(Button.builder(Component.translatable("imperium.ui.accept").withStyle(ChatFormatting.GREEN), btn -> respond(pid, true))
                    .bounds(left + w - 112, py, 52, 14).build());
            addRenderableWidget(Button.builder(Component.translatable("imperium.ui.reject").withStyle(ChatFormatting.RED), btn -> respond(pid, false))
                    .bounds(left + w - 58, py, 52, 14).build());
            py += 15;
        }
        Nation other = d.nation(selected);
        if (other == null || other.eliminated || d.phase != GamePhase.RUNNING) return;
        Relation r = d.relation(me.id, other.id);
        RelationType rt = r == null ? RelationType.PEACE : r.type;
        int ax = left + 136, ay = top + 70;
        List<DiplomacyManager.Action> actions = new ArrayList<>();
        if (rt == RelationType.WAR) actions.add(DiplomacyManager.Action.PROPOSE_PEACE);
        else actions.add(DiplomacyManager.Action.DECLARE_WAR);
        if (rt == RelationType.ALLIANCE) actions.add(DiplomacyManager.Action.BREAK_ALLIANCE);
        else if (rt != RelationType.WAR) actions.add(DiplomacyManager.Action.PROPOSE_ALLIANCE);
        if (rt == RelationType.NON_AGGRESSION) actions.add(DiplomacyManager.Action.BREAK_NAP);
        else if (rt == RelationType.PEACE) actions.add(DiplomacyManager.Action.PROPOSE_NAP);
        if (r != null && r.trade) actions.add(DiplomacyManager.Action.CANCEL_TRADE);
        else if (rt != RelationType.WAR) actions.add(DiplomacyManager.Action.PROPOSE_TRADE);
        actions.add(DiplomacyManager.Action.SEND_GIFT);
        if (rt != RelationType.ALLIANCE) actions.add(DiplomacyManager.Action.DEMAND_TRIBUTE);
        actions.add(DiplomacyManager.Action.INSULT);
        int i = 0;
        for (DiplomacyManager.Action a : actions) {
            int bx = ax + (i % 2) * 122, by = ay + (i / 2) * 16;
            Component label = Component.translatable(a.key());
            if (a == DiplomacyManager.Action.DECLARE_WAR) label = label.copy().withStyle(ChatFormatting.RED);
            addRenderableWidget(Button.builder(label, btn -> {
                if (a == DiplomacyManager.Action.DECLARE_WAR && !hasShiftDown()) {
                    if (minecraft.player != null) minecraft.player.displayClientMessage(Component.translatable("imperium.ui.war_confirm"), true);
                    return;
                }
                CompoundTag tag = new CompoundTag();
                tag.putInt("target", selected);
                tag.putInt("action", a.ordinal());
                send("diplomacy", tag);
            }).bounds(bx, by, 120, 14).build());
            i++;
        }
    }

    private void respond(int id, boolean accept) {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putBoolean("accept", accept);
        send("proposal", t);
    }

    private void initLobby(Nation me) {
        int y = top + 22;
        for (Nation n : ClientState.data.nations.values()) {
            final int id = n.id;
            Button b = addRenderableWidget(Button.builder(Component.translatable(me != null && me.id == id ? "imperium.ui.joined" : "imperium.ui.join"), btn -> {
                CompoundTag t = new CompoundTag();
                t.putInt("nation", id);
                send("join", t);
            }).bounds(left + w - 76, y, 70, 14).build());
            b.active = (me == null || me.id != id) && !n.eliminated;
            y += 18;
            if (y > top + h - 20) break;
        }
        if (me != null && (ClientState.data.phase != GamePhase.RUNNING || ClientState.isAdmin())) {
            addRenderableWidget(Button.builder(Component.translatable("imperium.ui.leave"), btn -> send("leave", new CompoundTag()))
                    .bounds(left + 6, top + h - 20, 90, 14).build());
        }
    }

    @Override
    public void tick() {
        if (ClientState.stateVersion != lastVersion && minecraft.level != null && minecraft.level.getGameTime() % 10 == 0) {
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        UiUtil.panelLight(g, left, top, w, h);
        Nation me = ClientState.myNation();
        switch (tab) {
            case 0 -> renderOverview(g, me);
            case 1 -> renderDiplomacy(g, me);
            default -> renderLobby(g, me);
        }
        super.render(g, mx, my, pt);
    }

    private void renderOverview(GuiGraphics g, Nation n) {
        if (n == null) return;
        GameData d = ClientState.data;
        int x = left + 8, y = top + 8;
        g.fill(x, y, x + 10, y + 10, 0xFF000000 | n.rgb());
        g.drawString(font, n.name + (n.bot ? " [BOT]" : ""), x + 14, y + 1, 0xFFFFD700);
        y += 18;
        g.drawString(font, Component.translatable("imperium.ui.tax"), left + w - 170, top + 9, 0xFFCCCCCC);
        int col2 = left + w / 2 + 4;
        line(g, x, y, Component.translatable("imperium.ui.stat.population", n.population, n.housing), n.population > n.housing ? 0xFFFF7070 : 0xFFFFFFFF);
        line(g, col2, y, Component.translatable("imperium.ui.stat.happiness", (int) n.happiness, (int) n.happinessTarget),
                n.happiness < 30 ? 0xFFFF7070 : n.happiness > 70 ? 0xFF90FF90 : 0xFFFFFFFF);
        y += 11;
        line(g, x, y, Component.translatable("imperium.ui.stat.army", n.soldierCount, MilitaryManager.maxArmy(n)), 0xFFFFFFFF);
        line(g, col2, y, Component.translatable("imperium.ui.stat.research", (int) n.research, UiUtil.signed(n.researchDelta)), 0xFF9AD0FF);
        y += 11;
        line(g, x, y, Component.translatable("imperium.ui.stat.provinces", d.provincesOf(n.id).size(), n.techs.size()), 0xFFFFFFFF);
        Component status = n.famine ? Component.translatable("imperium.ui.stat.famine").withStyle(ChatFormatting.RED)
                : n.abundant() ? Component.translatable("imperium.ui.stat.abundance").withStyle(ChatFormatting.GREEN)
                : Component.translatable("imperium.ui.stat.normal");
        line(g, col2, y, status, 0xFFFFFFFF);
        y += 15;
        for (ResourceType t : ResourceType.values()) {
            int cx = x + (t.ordinal() % 3) * ((w - 16) / 3);
            int cy = y + (t.ordinal() / 3) * 18;
            g.renderItem(t.icon(), cx, cy);
            double delta = n.lastDelta.get(t);
            g.drawString(font, n.stock.getInt(t) + " (" + UiUtil.signed(delta) + ")", cx + 18, cy + 4, delta < 0 ? 0xFFFF8080 : 0xFFFFFFFF);
        }
        y += 40;
        g.drawString(font, Component.translatable("imperium.ui.buildings"), x, y, 0xFFFFD700);
        y += 11;
        StringBuilder sb = new StringBuilder();
        for (BuildingType t : Buildings.all()) {
            int a = d.countActive(n.id, t.id), all = d.countAll(n.id, t.id);
            if (all == 0) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(t.name().getString()).append(" ").append(a).append("/").append(all);
        }
        g.drawWordWrap(font, Component.literal(sb.length() == 0 ? "-" : sb.toString()), x, y, w - 16, 0xFFCCCCCC);
        y += 30;
        g.drawString(font, Component.translatable("imperium.ui.log"), x, y, 0xFFFFD700);
        y += 11;
        int lines = Math.max(0, (top + h - 6 - y) / 10);
        for (int i = Math.max(0, n.log.size() - lines); i < n.log.size(); i++) {
            g.drawString(font, UiUtil.trim(font, n.log.get(i).getString(), w - 16), x, y, 0xFFBBBBBB);
            y += 10;
        }
    }

    private void line(GuiGraphics g, int x, int y, Component c, int color) {
        g.drawString(font, c, x, y, color);
    }

    private void renderDiplomacy(GuiGraphics g, Nation me) {
        if (me == null) return;
        GameData d = ClientState.data;
        Nation o = d.nation(selected);
        int x = left + 136, y = top + 8;
        if (o != null) {
            g.fill(x, y, x + 10, y + 10, 0xFF000000 | o.rgb());
            g.drawString(font, o.name + (o.bot ? " [BOT]" : "") + (o.eliminated ? " ✝" : ""), x + 14, y + 1, 0xFFFFD700);
            Relation r = d.relation(me.id, o.id);
            RelationType rt = r == null ? RelationType.PEACE : r.type;
            y += 14;
            g.drawString(font, Component.translatable("imperium.ui.relation", Component.translatable(rt.key())), x, y, 0xFF000000 | rt.color);
            if (r != null && r.trade) g.drawString(font, Component.translatable("imperium.ui.trade_active"), x + 140, y, 0xFF90E0FF);
            y += 11;
            int theirOpinion = r == null ? 0 : r.opinionOf(o.id, me.id);
            g.drawString(font, Component.translatable("imperium.ui.opinion", theirOpinion), x, y, theirOpinion < 0 ? 0xFFFF8080 : 0xFF90FF90);
            g.drawString(font, Component.translatable("imperium.ui.strength", (int) BotAI.strength(d, o), (int) BotAI.strength(d, me)), x + 140, y, 0xFFCCCCCC);
            y += 11;
            String members = o.memberNames.isEmpty() ? "-" : String.join(", ", o.memberNames);
            g.drawString(font, UiUtil.trim(font, Component.translatable("imperium.ui.members", members).getString(), w - 145), x, y, 0xFFAAAAAA);
            y += 11;
            if (r != null && d.gameTick < r.truceUntil) {
                g.drawString(font, Component.translatable("imperium.ui.truce", UiUtil.time((r.truceUntil - d.gameTick) / 20)), x, y, 0xFF9AD0FF);
            }
            if (d.inGracePeriod()) g.drawString(font, Component.translatable("imperium.hud.grace", UiUtil.time(ClientState.graceSeconds())), x + 140, y, 0xFF9AD0FF);
        }
        // relation colours next to the list
        int ly = top + 6;
        for (Nation n : others(me)) {
            RelationType rt = d.relationType(me.id, n.id);
            g.fill(left + 128, ly + 2, left + 132, ly + 12, 0xFF000000 | rt.color);
            ly += 15;
            if (ly > top + h - 20) break;
        }
        int py = top + h - 6 - 15 * Math.min(4, d.proposals.size());
        for (Proposal p : d.proposals) {
            if (p.to != me.id) continue;
            Nation from = d.nation(p.from);
            Component txt = Component.translatable("imperium.ui.proposal", from == null ? "?" : from.name, Component.translatable(p.type.key()));
            g.drawString(font, UiUtil.trim(font, txt.getString(), w - 260), left + 136, py + 3, 0xFFFFE080);
            py += 15;
        }
    }

    private void renderLobby(GuiGraphics g, Nation me) {
        GameData d = ClientState.data;
        g.drawString(font, Component.translatable("imperium.ui.lobby_title"), left + 8, top + 8, 0xFFFFD700);
        int y = top + 22;
        for (Nation n : d.nations.values()) {
            g.fill(left + 8, y + 2, left + 18, y + 12, 0xFF000000 | n.rgb());
            long provinces = d.provincesOf(n.id).size();
            String members = n.memberNames.isEmpty() ? (n.bot ? "BOT" : "-") : String.join(", ", n.memberNames);
            g.drawString(font, UiUtil.trim(font, n.name, 140), left + 22, y + 3, n.eliminated ? 0xFF777777 : 0xFFFFFFFF);
            g.drawString(font, UiUtil.trim(font, provinces + " ⬢  " + members, w - 250), left + 168, y + 3, 0xFFAAAAAA);
            y += 18;
            if (y > top + h - 20) break;
        }
        if (d.nations.isEmpty()) g.drawCenteredString(font, Component.translatable("imperium.ui.lobby_empty"), left + w / 2, top + h / 2, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
