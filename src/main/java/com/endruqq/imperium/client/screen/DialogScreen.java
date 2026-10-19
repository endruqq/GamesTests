package com.endruqq.imperium.client.screen;

import com.endruqq.imperium.client.ClientState;
import com.endruqq.imperium.client.UiUtil;
import com.endruqq.imperium.network.ActionPacket;
import com.endruqq.imperium.network.ImperiumNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/** Conversation with a citizen or soldier. */
public class DialogScreen extends Screen implements RtsChild {
    private final Screen parent;
    private int entityId;
    private String speaker = "";
    private Component subtitle = Component.empty();
    private Component line = Component.empty();
    private String mood = "normal";
    private int nation = -1;
    private final List<String> optionIds = new ArrayList<>();
    private final List<Component> optionTexts = new ArrayList<>();
    private long lineStart;
    private int px, py, pw, ph;

    public DialogScreen(CompoundTag data, Screen parent) {
        super(Component.translatable("imperium.ui.dialog"));
        this.parent = parent;
        read(data);
    }

    private static Component json(String s) {
        try {
            Component c = Component.Serializer.fromJson(s);
            return c == null ? Component.empty() : c;
        } catch (Exception e) {
            return Component.literal(s);
        }
    }

    private void read(CompoundTag t) {
        entityId = t.getInt("entity");
        speaker = t.getString("name");
        subtitle = json(t.getString("subtitle"));
        line = json(t.getString("line"));
        mood = t.getString("mood");
        nation = t.getInt("nation");
        optionIds.clear();
        optionTexts.clear();
        ListTag opts = t.getList("options", Tag.TAG_COMPOUND);
        for (int i = 0; i < opts.size(); i++) {
            optionIds.add(opts.getCompound(i).getString("id"));
            optionTexts.add(json(opts.getCompound(i).getString("text")));
        }
        lineStart = System.currentTimeMillis();
    }

    public void update(CompoundTag t) {
        read(t);
        rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        pw = Math.min(380, width - 20);
        int rows = (optionIds.size() + 1) / 2;
        ph = 78 + rows * 16;
        px = (width - pw) / 2;
        py = height - ph - 12;
        for (int i = 0; i < optionIds.size(); i++) {
            final String id = optionIds.get(i);
            int bx = px + 70 + (i % 2) * ((pw - 76) / 2);
            int by = py + 72 + (i / 2) * 16;
            addRenderableWidget(Button.builder(optionTexts.get(i), b -> choose(id)).bounds(bx, by, (pw - 80) / 2, 14).build());
        }
    }

    private void choose(String id) {
        CompoundTag t = new CompoundTag();
        t.putInt("entity", entityId);
        t.putString("option", id);
        ImperiumNet.sendToServer(new ActionPacket("dialog", t));
        if (id.equals("bye")) onClose();
    }

    private int moodColor() {
        return switch (mood) {
            case "famine", "enemy", "unpaid" -> 0xFFFF6060;
            case "war", "unhappy", "hungry", "homeless" -> 0xFFFFB060;
            case "abundant", "happy" -> 0xFF80FF80;
            default -> 0xFFDDDDDD;
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        UiUtil.panelLight(g, px, py, pw, ph);
        g.fill(px, py, px + 4, py + ph, 0xFF000000 | ClientState.colorOf(nation));
        Entity e = minecraft.level == null ? null : minecraft.level.getEntity(entityId);
        if (e instanceof LivingEntity le) {
            g.enableScissor(px + 5, py + 2, px + 66, py + ph - 2);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, px + 36, py + 66, 30, (px + 36) - mx, (py + 20) - my, le);
            g.disableScissor();
        }
        g.drawString(font, speaker, px + 70, py + 6, 0xFFFFD700);
        g.drawString(font, subtitle, px + 70 + font.width(speaker) + 6, py + 6, 0xFFAAAAAA);
        String full = line.getString();
        int chars = (int) Math.min(full.length(), (System.currentTimeMillis() - lineStart) / 18);
        String shown = full.substring(0, chars);
        int yy = py + 20;
        for (FormattedCharSequence s : font.split(Component.literal(shown).withStyle(line.getStyle()), pw - 80)) {
            g.drawString(font, s, px + 70, yy, moodColor());
            yy += 10;
            if (yy > py + 64) break;
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        String full = line.getString();
        if ((System.currentTimeMillis() - lineStart) / 18 < full.length() && UiUtil.inside(mx, my, px, py, pw, 70)) {
            lineStart = 0;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
