package com.endruqq.imperium.game;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public enum NationColor {
    RED(0xD03A2F, DyeColor.RED),
    BLUE(0x2F5BD0, DyeColor.BLUE),
    GREEN(0x2E9E3F, DyeColor.GREEN),
    YELLOW(0xE8C51E, DyeColor.YELLOW),
    PURPLE(0x8A3FC4, DyeColor.PURPLE),
    ORANGE(0xE8781E, DyeColor.ORANGE),
    CYAN(0x1FA8A8, DyeColor.CYAN),
    MAGENTA(0xC63FA6, DyeColor.MAGENTA),
    LIME(0x86D12F, DyeColor.LIME),
    LIGHT_BLUE(0x5AA8E8, DyeColor.LIGHT_BLUE),
    PINK(0xF28DB0, DyeColor.PINK),
    BROWN(0x83552E, DyeColor.BROWN),
    WHITE(0xE6E6E6, DyeColor.WHITE),
    BLACK(0x2B2B2B, DyeColor.BLACK),
    GRAY(0x7E7E7E, DyeColor.GRAY);

    public final int rgb;
    public final DyeColor dye;

    NationColor(int rgb, DyeColor dye) {
        this.rgb = rgb;
        this.dye = dye;
    }

    public static NationColor byIndex(int i) {
        NationColor[] v = values();
        return v[Math.floorMod(i, v.length)];
    }

    public Block wool() {
        return switch (dye) {
            case WHITE -> Blocks.WHITE_WOOL;
            case ORANGE -> Blocks.ORANGE_WOOL;
            case MAGENTA -> Blocks.MAGENTA_WOOL;
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_WOOL;
            case YELLOW -> Blocks.YELLOW_WOOL;
            case LIME -> Blocks.LIME_WOOL;
            case PINK -> Blocks.PINK_WOOL;
            case GRAY -> Blocks.GRAY_WOOL;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_WOOL;
            case CYAN -> Blocks.CYAN_WOOL;
            case PURPLE -> Blocks.PURPLE_WOOL;
            case BLUE -> Blocks.BLUE_WOOL;
            case BROWN -> Blocks.BROWN_WOOL;
            case GREEN -> Blocks.GREEN_WOOL;
            case RED -> Blocks.RED_WOOL;
            case BLACK -> Blocks.BLACK_WOOL;
        };
    }
}
