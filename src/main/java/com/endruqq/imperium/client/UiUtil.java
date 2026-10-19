package com.endruqq.imperium.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class UiUtil {
    private UiUtil() {}

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xC0101418);
        g.renderOutline(x, y, w, h, 0xFF4A5260);
    }

    public static void panelLight(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xE0202630);
        g.renderOutline(x, y, w, h, 0xFF6A7488);
    }

    public static void smallItem(GuiGraphics g, ItemStack stack, int x, int y) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.5F, 0.5F, 1F);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    public static String trim(Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) return s;
        return font.plainSubstrByWidth(s, Math.max(0, maxWidth - font.width("…"))) + "…";
    }

    public static String time(long seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    public static String signed(double v) {
        int i = (int) Math.round(v);
        return (i >= 0 ? "+" : "") + i;
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
