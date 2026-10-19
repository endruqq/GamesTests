package com.endruqq.imperium.client;

import com.endruqq.imperium.client.ui.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Drawing helpers shared by all Imperium screens and the HUD. */
public final class UiUtil {
    private UiUtil() {}

    /** Translucent dark panel with a thin border. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 2, y + 2, x + w + 2, y + h + 2, Theme.SHADOW);
        g.fill(x, y, x + w, y + h, Theme.PANEL);
        g.renderOutline(x, y, w, h, Theme.BORDER);
        g.hLine(x + 1, x + w - 2, y + 1, Theme.HIGHLIGHT);
    }

    /** Opaque panel for windows. */
    public static void panelLight(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 3, y + 3, x + w + 3, y + h + 3, Theme.SHADOW);
        g.fill(x, y, x + w, y + h, Theme.PANEL_SOLID);
        g.renderOutline(x, y, w, h, Theme.BORDER_LIGHT);
        g.hLine(x + 1, x + w - 2, y + 1, Theme.HIGHLIGHT);
    }

    /** A window: panel + header bar with a title. @return y of the content area */
    public static int window(GuiGraphics g, Font font, int x, int y, int w, int h, Component title, int accent) {
        panelLight(g, x, y, w, h);
        g.fill(x + 1, y + 1, x + w - 1, y + 19, Theme.HEADER);
        g.hLine(x + 1, x + w - 2, y + 19, Theme.BORDER);
        g.fill(x + 1, y + 1, x + 4, y + 19, accent);
        g.drawString(font, title, x + 9, y + 6, Theme.TEXT, false);
        return y + 24;
    }

    public static void card(GuiGraphics g, int x, int y, int w, int h, boolean hover) {
        g.fill(x, y, x + w, y + h, hover ? Theme.CARD_HOVER : Theme.CARD);
        g.renderOutline(x, y, w, h, hover ? Theme.BORDER_LIGHT : Theme.BORDER);
    }

    /** Small heading with a line, used to split a panel into sections. */
    public static void section(GuiGraphics g, Font font, int x, int y, int w, Component title) {
        g.drawString(font, title, x, y, Theme.ACCENT, false);
        int tw = font.width(title) + 4;
        if (w > tw) g.hLine(x + tw, x + w, y + 4, Theme.BORDER);
    }

    /** Progress bar. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, double frac, int color) {
        frac = Math.max(0, Math.min(1, frac));
        g.fill(x, y, x + w, y + h, 0xFF0C1015);
        g.fill(x, y, x + (int) Math.round(w * frac), y + h, color);
        g.renderOutline(x, y, w, h, Theme.BORDER);
    }

    /** Coloured label chip. @return its width */
    public static int badge(GuiGraphics g, Font font, int x, int y, Component text, int color) {
        int w = font.width(text) + 6;
        g.fill(x, y, x + w, y + 10, Theme.alpha(color, 0x40));
        g.renderOutline(x, y, w, 10, Theme.alpha(color, 0xC0));
        g.drawString(font, text, x + 3, y + 1, color, false);
        return w;
    }

    public static void text(GuiGraphics g, Font font, Component c, int x, int y, int color) {
        g.drawString(font, c, x, y, color, false);
    }

    public static void text(GuiGraphics g, Font font, String s, int x, int y, int color) {
        g.drawString(font, s, x, y, color, false);
    }

    public static void smallItem(GuiGraphics g, ItemStack stack, int x, int y) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.5F, 0.5F, 1F);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    /** Item icon at 10x10. */
    public static void icon10(GuiGraphics g, ItemStack stack, int x, int y) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.625F, 0.625F, 1F);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    public static String trim(Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) return s;
        return font.plainSubstrByWidth(s, Math.max(0, maxWidth - font.width("…"))) + "…";
    }

    public static String time(long seconds) {
        seconds = Math.max(0, seconds);
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    public static String signed(double v) {
        int i = (int) Math.round(v);
        return (i >= 0 ? "+" : "") + i;
    }

    /** 1234 -> "1.2k" */
    public static String compact(double v) {
        if (Math.abs(v) >= 10000) return String.format("%.0fk", v / 1000);
        if (Math.abs(v) >= 1000) return String.format("%.1fk", v / 1000);
        return String.valueOf((int) Math.floor(v));
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
