package com.endruqq.imperium.client.screen;

import com.endruqq.imperium.client.ClientState;
import com.endruqq.imperium.client.UiUtil;
import com.endruqq.imperium.client.ui.FlatButton;
import com.endruqq.imperium.client.ui.Theme;
import com.endruqq.imperium.network.ActionPacket;
import com.endruqq.imperium.network.ImperiumNet;
import net.minecraft.ChatFormatting;
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
        pw = Math.min(420, width - 20);
        int rows = (optionIds.size() + 1) / 2;
        ph = 84 + rows * 17;
        px = (width - pw) / 2;
        py = height - ph - 10;
        int colW = (pw - 84) / 2;
        for (int i = 0; i < optionIds.size(); i++) {
            final String id = optionIds.get(i);
            int bx = px + 76 + (i % 2) * (colW + 4);
            int by = py + 78 + (i / 2) * 17;
            Component label = Component.empty().append(Component.literal((i + 1) + ". ").withStyle(ChatFormatting.DARK_GRAY)).append(optionTexts.get(i));
            FlatButton.Style style = id.equals("bye") ? FlatButton.Style.GHOST : id.startsWith("p_") || id.startsWith("job:") ? FlatButton.Style.PRIMARY
                    : FlatButton.Style.NORMAL;
            addRenderableWidget(FlatButton.of(bx, by, colW, 15, label, style, b -> choose(id)));
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        int idx = key - org.lwjgl.glfw.GLFW.GLFW_KEY_1;
        if (idx >= 0 && idx < 9 && idx < optionIds.size()) {
            choose(optionIds.get(idx));
            return true;
        }
        return super.keyPressed(key, scan, mods);
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
            case "famine", "enemy", "unpaid", "trapped" -> 0xFFFF8A80;
            case "war", "unhappy", "hungry", "homeless", "prisoner" -> 0xFFFFC27A;
            case "mourning" -> 0xFFB8BEC8;
            case "abundant", "happy" -> 0xFFA6F0B8;
            default -> Theme.TEXT;
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, height / 2, width, height, 0x00000000, 0x90000000);
        UiUtil.panelLight(g, px, py, pw, ph);
        int accent = 0xFF000000 | ClientState.colorOf(nation);
        g.fill(px + 1, py + 1, px + pw - 1, py + 3, accent);
        // portrait
        g.fill(px + 6, py + 8, px + 68, py + ph - 8, 0xFF0E1218);
        g.renderOutline(px + 6, py + 8, 62, ph - 16, Theme.BORDER_LIGHT);
        Entity e = minecraft.level == null ? null : minecraft.level.getEntity(entityId);
        if (e instanceof LivingEntity le) {
            g.enableScissor(px + 7, py + 9, px + 67, py + ph - 9);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, px + 37, py + 70, 30, (px + 37) - mx, (py + 30) - my, le);
            g.disableScissor();
        }
        g.drawString(font, speaker, px + 76, py + 9, Theme.ACCENT, false);
        g.drawString(font, subtitle, px + 76 + font.width(speaker) + 6, py + 9, Theme.TEXT_DIM, false);
        UiUtil.badge(g, font, px + pw - 8 - font.width(Component.translatable("imperium.mood." + mood)) - 6, py + 8,
                Component.translatable("imperium.mood." + mood), moodColor());
        g.hLine(px + 76, px + pw - 8, py + 21, Theme.BORDER);
        String full = line.getString();
        int chars = (int) Math.min(full.length(), (System.currentTimeMillis() - lineStart) / 18);
        String shown = full.substring(0, chars);
        int yy = py + 27;
        for (FormattedCharSequence s : font.split(Component.literal(shown).withStyle(line.getStyle()), pw - 88)) {
            g.drawString(font, s, px + 76, yy, moodColor(), false);
            yy += 10;
            if (yy > py + 70) break;
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        String full = line.getString();
        if ((System.currentTimeMillis() - lineStart) / 18 < full.length() && UiUtil.inside(mx, my, px, py, pw, 76)) {
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
