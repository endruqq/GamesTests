package com.endruqq.imperium.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A flat, theme-coloured button (optionally with an item icon) used across the Imperium screens. */
public class FlatButton extends Button {
    public enum Style { NORMAL, PRIMARY, DANGER, SUCCESS, GHOST, TAB }

    private Style style;
    private boolean selected;
    private ItemStack icon = ItemStack.EMPTY;

    public FlatButton(int x, int y, int w, int h, Component message, OnPress onPress, Style style) {
        super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
        this.style = style;
    }

    public static FlatButton of(int x, int y, int w, int h, Component message, OnPress onPress) {
        return new FlatButton(x, y, w, h, message, onPress, Style.NORMAL);
    }

    public static FlatButton of(int x, int y, int w, int h, Component message, Style style, OnPress onPress) {
        return new FlatButton(x, y, w, h, message, onPress, style);
    }

    public FlatButton tip(Component c) {
        if (c != null) setTooltip(Tooltip.create(c));
        return this;
    }

    public FlatButton selected(boolean s) {
        this.selected = s;
        return this;
    }

    public FlatButton icon(ItemStack s) {
        this.icon = s == null ? ItemStack.EMPTY : s;
        return this;
    }

    public FlatButton enabled(boolean on) {
        this.active = on;
        return this;
    }

    public void setStyle(Style s) {
        this.style = s;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        Font font = Minecraft.getInstance().font;
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hover = isHovered() && active;
        int bg, border, text;
        switch (style) {
            case PRIMARY -> {
                bg = hover ? Theme.BTN_PRIMARY_HOVER : Theme.BTN_PRIMARY;
                border = Theme.ACCENT;
                text = 0xFFFFF4D6;
            }
            case DANGER -> {
                bg = hover ? Theme.BTN_DANGER_HOVER : Theme.BTN_DANGER;
                border = 0xFFC24A4A;
                text = 0xFFFFE0E0;
            }
            case SUCCESS -> {
                bg = hover ? Theme.BTN_SUCCESS_HOVER : Theme.BTN_SUCCESS;
                border = 0xFF3FA36B;
                text = 0xFFE2FFEE;
            }
            case GHOST -> {
                bg = hover ? Theme.BTN_HOVER : 0x00000000;
                border = hover ? Theme.BORDER_LIGHT : Theme.BORDER;
                text = Theme.TEXT_DIM;
            }
            case TAB -> {
                bg = selected ? Theme.PANEL_SOLID : hover ? Theme.BTN_HOVER : Theme.HEADER;
                border = selected ? Theme.BORDER_LIGHT : Theme.BORDER;
                text = selected ? Theme.ACCENT : Theme.TEXT_DIM;
            }
            default -> {
                bg = selected ? Theme.CARD_SELECTED : hover ? Theme.BTN_HOVER : Theme.BTN;
                border = selected ? Theme.ACCENT : hover ? Theme.BORDER_LIGHT : Theme.BORDER;
                text = Theme.TEXT;
            }
        }
        if (!active) {
            bg = style == Style.TAB ? Theme.HEADER : Theme.BTN_DISABLED;
            border = Theme.BORDER;
            text = Theme.TEXT_MUTED;
        }
        g.fill(x, y, x + w, y + h, bg);
        g.renderOutline(x, y, w, h, border);
        if (active && style != Style.GHOST && style != Style.TAB) g.hLine(x + 1, x + w - 2, y + 1, Theme.HIGHLIGHT);
        if (style == Style.TAB && selected) {
            g.fill(x + 1, y, x + w - 1, y + 2, Theme.ACCENT);
            g.hLine(x + 1, x + w - 2, y + h - 1, Theme.PANEL_SOLID);
        }
        int tx = x + w / 2;
        int textW = font.width(getMessage());
        if (!icon.isEmpty()) {
            int total = 10 + (textW > 0 ? 3 + textW : 0);
            int ix = x + (w - total) / 2;
            UiUtilIcons.small(g, icon, ix, y + (h - 8) / 2);
            if (textW > 0) g.drawString(font, getMessage(), ix + 13, y + (h - 8) / 2, text, false);
            return;
        }
        String s = getMessage().getString();
        if (textW > w - 6) {
            s = font.plainSubstrByWidth(s, Math.max(0, w - 6 - font.width("…"))) + "…";
            g.drawString(font, s, x + (w - font.width(s)) / 2, y + (h - 8) / 2, text, false);
        } else {
            g.drawString(font, getMessage(), tx - textW / 2, y + (h - 8) / 2, text, false);
        }
    }

    /** tiny local helper so this class does not depend on screen code */
    static final class UiUtilIcons {
        private UiUtilIcons() {}

        static void small(GuiGraphics g, ItemStack stack, int x, int y) {
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(0.625F, 0.625F, 1F);
            g.renderItem(stack, 0, 0);
            g.pose().popPose();
        }
    }
}
