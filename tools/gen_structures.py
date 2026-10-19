#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Designs of the ready-made buildings (early 20th century town style: brick tenements, a factory, a pit head...).
Writes structure templates to src/main/resources/data/imperium/structures/prefab/, PrefabTemplates.java with their
metadata, and isometric previews (tools/previews/, not committed). Run from the repository root:
python3 tools/gen_structures.py [--preview]"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from structlib import Build, count, render, DIRS, OPP  # noqa: E402

OUT = os.path.join("src", "main", "resources", "data", "imperium", "structures", "prefab")
JAVA = os.path.join("src", "main", "java", "com", "endruqq", "imperium", "game", "PrefabTemplates.java")
PREVIEW = os.path.join("tools", "previews")

# radius, height and requirements, as in Buildings.java
SPEC = {}
DESIGNS = {}


def design(name, r, h, reqs):
    def deco(fn):
        SPEC[name] = (r, h, reqs)
        DESIGNS[name] = fn
        return fn
    return deco


def new(name, r, top, bottom=0):
    return Build(name, 2 * r + 1, 2 * r + 1, top, bottom)


def ground(b, block="grass_block"):
    """Plot floor (y = 0) everywhere, so leftovers of the terrain disappear."""
    b.fill(0, 0, 0, b.w - 1, 0, b.d - 1, block)


def sidewalk(b, x0, z0, x1, z1, block="smooth_stone"):
    b.fill(x0, 0, z0, x1, 0, z1, block)


def hedge(b, x, z, y=1):
    b.set(x, y, z, "oak_leaves", persistent=True, distance=1)


def win_row(b, xs, y, z, face, glass="glass_pane", h=2, sill="smooth_stone_slab", lintel=None):
    for x in xs:
        b.window(x, y, z, face, glass, sill, lintel, h)


def win_col(b, zs, y, x, face, glass="glass_pane", h=2, sill="smooth_stone_slab", lintel=None):
    for z in zs:
        b.window(x, y, z, face, glass, sill, lintel, h)


def table(b, x, y, z, top="spruce_pressure_plate", leg="spruce_fence"):
    b.set(x, y, z, leg)
    b.set(x, y + 1, z, top)


def chair(b, x, y, z, facing, mat="spruce"):
    b.stairs(x, y, z, mat, facing)


def ladder_shaft(b, x, z, y0, y1, facing, holes=()):
    for y in range(y0, y1 + 1):
        b.set(x, y, z, "ladder", facing=facing)
    for y in holes:
        b.set(x, y, z, "ladder", facing=facing)


# ================================================================================================ housing

@design("hut", 4, 10, [("planks", 40), ("logs", 8), ("beds", 2), ("doors", 1), ("torches", 2)])
def hut(b):
    ground(b)
    # foundation and floor
    b.fill(1, 0, 1, 7, 0, 6, "cobblestone")
    b.fill(2, 0, 2, 6, 0, 5, "spruce_planks")
    # timber frame and clapboard walls
    b.walls(1, 1, 7, 6, 1, 4, "spruce_planks")
    for x, z in ((1, 1), (7, 1), (1, 6), (7, 6)):
        b.fill(x, 1, z, x, 4, z, "stripped_dark_oak_log", axis="y")
    for x in range(1, 8):
        b.log(x, 4, 1, "stripped_dark_oak_log", "x")
        b.log(x, 4, 6, "stripped_dark_oak_log", "x")
    for z in range(2, 6):
        b.log(1, 4, z, "stripped_dark_oak_log", "z")
        b.log(7, 4, z, "stripped_dark_oak_log", "z")
    # windows with shutters
    for x in (2, 6):
        b.window(x, 2, 6, "south", "glass_pane", "spruce_slab", None, 1)
    b.window(2, 2, 1, "north", "glass_pane", "spruce_slab", None, 1)
    b.window(6, 2, 1, "north", "glass_pane", "spruce_slab", None, 1)
    b.window(1, 2, 3, "west", "glass_pane", "spruce_slab", None, 2)
    b.window(7, 2, 4, "east", "glass_pane", "spruce_slab", None, 2)
    # door, porch
    b.door(4, 1, 6, "spruce", "north")
    b.fill(2, 0, 7, 6, 0, 7, "spruce_planks")
    for x in (2, 6):
        b.fill(x, 1, 7, x, 3, 7, "spruce_fence")
    b.fill(2, 4, 7, 6, 4, 7, "spruce_slab", type="top")
    b.wall_torch(3, 2, 7, "south")
    b.wall_torch(5, 2, 7, "south")
    b.set(4, 0, 8, "dirt_path")
    b.set(4, 0, 7, "spruce_planks")
    # roof
    b.gable_roof(1, 1, 7, 6, 5, "dark_oak", "x", overhang=1, gable="spruce_planks")
    b.window(1, 6, 3, "west", "glass_pane", None, None, 1)
    b.window(7, 6, 4, "east", "glass_pane", None, None, 1)
    b.chimney(6, 2, 1, 8, "bricks", "stone_brick_slab")
    b.set(6, 0, 2, "bricks")
    # interior
    b.bed(3, 1, 2, "red", "west")
    b.bed(3, 1, 4, "blue", "west")
    b.set(5, 1, 2, "furnace", facing="south")
    b.barrel(5, 1, 5, "up")
    b.set(6, 1, 5, "crafting_table")
    table(b, 6, 1, 4)
    chair(b, 6, 1, 3, "south")
    b.lantern(4, 4, 3, hanging=True)
    b.set(2, 1, 5, "potted_azure_bluet")
    b.fill(3, 1, 5, 3, 1, 5, "red_carpet")
    # garden
    for x in (1, 2, 6, 7):
        b.set(x, 0, 8, "grass_block")
    hedge(b, 0, 8)
    hedge(b, 8, 8)
    b.set(1, 1, 8, "rose_bush", half="lower")
    b.set(1, 2, 8, "rose_bush", half="upper")
    b.set(7, 1, 8, "peony", half="lower")
    b.set(7, 2, 8, "peony", half="upper")


@design("stone_house", 4, 13, [("masonry", 50), ("planks", 20), ("beds", 3), ("doors", 1), ("glass_panes", 4), ("lanterns", 2)])
def stone_house(b):
    ground(b, "smooth_stone")
    b.fill(1, 0, 1, 7, 0, 7, "spruce_planks")
    # stone base, brick walls, stone quoins
    b.walls(1, 1, 7, 7, 1, 1, "polished_andesite")
    b.walls(1, 1, 7, 7, 2, 9, "bricks")
    for x, z in ((1, 1), (7, 1), (1, 7), (7, 7)):
        for y in range(2, 10):
            b.set(x, y, z, "stone_bricks" if y % 2 == 0 else "polished_andesite")
    b.band(1, 1, 7, 7, 5, "polished_andesite")
    # entrance with stoop and transom
    b.door(4, 1, 7, "dark_oak", "north")
    b.set(4, 3, 7, "glass_pane")
    b.stairs(4, 3, 8, "stone_brick", "north", "top")
    b.stairs(3, 3, 8, "stone_brick", "east", "top")
    b.stairs(5, 3, 8, "stone_brick", "west", "top")
    b.lantern(3, 2, 8)
    b.lantern(5, 2, 8)
    b.set(3, 1, 8, "stone_brick_wall")
    b.set(5, 1, 8, "stone_brick_wall")
    # windows: ground floor and first floor
    win_row(b, (2, 6), 2, 7, "south", lintel="polished_andesite")
    win_row(b, (2, 4, 6), 6, 7, "south", lintel="polished_andesite")
    win_row(b, (3, 5), 2, 1, "north", lintel="polished_andesite")
    win_row(b, (2, 4, 6), 6, 1, "north", lintel="polished_andesite")
    win_col(b, (3, 5), 2, 1, "west", lintel="polished_andesite")
    win_col(b, (3, 5), 6, 1, "west", lintel="polished_andesite")
    win_col(b, (3, 5), 2, 7, "east", lintel="polished_andesite")
    win_col(b, (3, 5), 6, 7, "east", lintel="polished_andesite")
    # cornice and flat roof
    b.cornice(1, 1, 7, 7, 9, "stone_brick")
    b.fill(2, 9, 2, 6, 9, 6, "spruce_planks")
    b.fill(1, 10, 1, 7, 10, 7, "smooth_stone")
    b.walls(1, 1, 7, 7, 11, 11, "bricks")
    for x in range(1, 8):
        b.slab(x, 12, 1, "stone_brick")
        b.slab(x, 12, 7, "stone_brick")
    for z in range(2, 7):
        b.slab(1, 12, z, "stone_brick")
        b.slab(7, 12, z, "stone_brick")
    b.chimney(6, 2, 11, 12, "bricks", "stone_brick_slab")
    # floors and stairs between them
    b.fill(2, 5, 2, 6, 5, 6, "spruce_planks")
    ladder_shaft(b, 2, 2, 1, 5, "east")
    ladder_shaft(b, 2, 2, 6, 9, "east")
    # ground floor: living room and kitchen
    b.set(6, 1, 2, "smoker", facing="west")
    b.barrel(6, 1, 3, "up")
    b.set(6, 1, 6, "crafting_table")
    table(b, 3, 1, 5)
    chair(b, 2, 1, 5, "east")
    b.set(5, 1, 6, "bookshelf")
    b.lantern(4, 4, 4, hanging=True)
    b.fill(3, 1, 3, 3, 1, 3, "red_carpet")
    # first floor: bedrooms
    b.bed(4, 6, 3, "white", "north")
    b.bed(6, 6, 3, "red", "north")
    b.bed(5, 6, 6, "blue", "east")
    b.lantern(4, 8, 5, hanging=True)
    b.set(3, 6, 6, "chest", facing="north", type="single")
    b.set(2, 6, 6, "potted_poppy")


@design("manor", 6, 20, [("bricks", 60), ("planks", 40), ("beds", 5), ("doors", 1), ("glass_panes", 6), ("lanterns", 3),
                         ("flower_pots", 2)])
def manor(b):
    """Corner tenement: shop with an awning, three floors of flats, fire escape, cornice and a water tower."""
    ground(b, "smooth_stone")
    sidewalk(b, 0, 11, 12, 12, "smooth_stone")
    sidewalk(b, 11, 0, 12, 12, "smooth_stone")
    for x in range(0, 13, 3):
        b.set(x, 0, 12, "stone_bricks")
    x0, z0, x1, z1 = 1, 1, 10, 10
    b.fill(x0, 0, z0, x1, 0, z1, "dark_oak_planks")
    # ground floor: white stone shop front
    b.walls(x0, z0, x1, z1, 1, 4, "polished_diorite")
    # shop windows and doors on the south side
    for x in (2, 3, 4, 5, 8, 9):
        b.fill(x, 1, z1, x, 3, z1, "glass_pane")
    for x in (2, 3, 4, 5, 8, 9):
        b.set(x, 1, z1, "dark_oak_planks")
    b.door(7, 1, z1, "dark_oak", "north", "right")
    b.set(7, 3, z1, "glass_pane")
    b.door(6, 1, z1, "dark_oak", "north", "left")
    b.set(6, 3, z1, "glass_pane")
    # side shop window
    for z in (3, 4, 5, 6, 7):
        b.fill(x1, 1, z, x1, 3, z, "glass_pane")
        b.set(x1, 1, z, "dark_oak_planks")
    b.door(x1, 1, 8, "dark_oak", "west")
    # awnings
    b.awning(2, 9, z1, 4, "south", ("red_wool", "white_wool"), 2)
    b.awning_x(2, 9, x1, 4, "east", ("red_wool", "white_wool"), 1)
    # brick upper floors with stone bands
    b.walls(x0, z0, x1, z1, 5, 16, "bricks")
    b.band(x0, z0, x1, z1, 5, "polished_diorite")
    for y0 in (6, 10, 14):
        win_row(b, (2, 4, 7, 9), y0, z1, "south", lintel="polished_diorite")
        win_row(b, (2, 4, 7, 9), y0, z0, "north", lintel="polished_diorite")
        win_col(b, (3, 5, 8), y0, x0, "west", lintel="polished_diorite")
        win_col(b, (3, 5, 8), y0, x1, "east", lintel="polished_diorite")
    # air conditioners under a few windows
    b.ac_unit(4, 9, z1, "south")
    b.ac_unit(9, 13, z1, "south")
    # fire escape on the east side
    for lv in (9, 13):
        for z in range(3, 8):
            b.slab(11, lv, z, "polished_blackstone", "top")
            b.set(12, lv + 1, z, "iron_bars")
        b.set(11, lv + 1, 2, "iron_bars")
        b.set(11, lv + 1, 8, "iron_bars")
        b.set(12, lv + 1, 2, "iron_bars")
        b.set(12, lv + 1, 8, "iron_bars")
        b.slab(11, lv, 2, "polished_blackstone", "top")
        b.slab(11, lv, 8, "polished_blackstone", "top")
    for y in range(10, 13):
        b.set(11, y, 7, "ladder", facing="east")
    b.door(x1, 10, 4, "dark_oak", "west")
    b.door(x1, 14, 4, "dark_oak", "west")
    # cornice
    b.band(x0, z0, x1, z1, 16, "dark_prismarine")
    b.cornice(x0, z0, x1, z1, 17, "dark_prismarine")
    b.fill(x0, 17, z0, x1, 17, z1, "smooth_stone")
    b.walls(x0, z0, x1, z1, 18, 18, "bricks")
    for x in range(x0, x1 + 1):
        b.slab(x, 19, z0, "dark_prismarine")
        b.slab(x, 19, z1, "dark_prismarine")
    for z in range(z0 + 1, z1):
        b.slab(x0, 19, z, "dark_prismarine")
        b.slab(x1, 19, z, "dark_prismarine")
    # roof: water tower, chimneys, roof hatch
    b.water_tower(4, 4, 18)
    b.chimney(8, 2, 18, 20, "bricks", "stone_brick_slab")
    b.chimney(2, 8, 18, 19, "bricks", "stone_brick_slab")
    b.trapdoor(8, 17, 8, "spruce", "south", "top")
    # floors
    for y in (5, 9, 13):
        b.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, "spruce_planks")
    for y in range(1, 17):
        b.set(8, y, 8, "ladder", facing="north")
    # shop interior
    b.fill(2, 1, 3, 4, 1, 3, "spruce_slab", type="top")
    b.barrel(2, 1, 2)
    b.barrel(3, 1, 2)
    b.chest(4, 1, 2, "south")
    b.lantern(5, 4, 5, hanging=True)
    b.lantern(8, 4, 5, hanging=True)
    b.set(9, 1, 9, "potted_fern")
    b.set(2, 1, 9, "potted_bamboo")
    b.set(9, 1, 3, "melon")
    b.set(9, 1, 4, "pumpkin")
    # flats: two beds per floor
    for y in (6, 10, 14):
        b.bed(3, y, 3, "red" if y == 6 else "blue" if y == 10 else "green", "north")
        b.bed(6, y, 3, "white", "north")
        b.set(2, y, 2, "chest", facing="south", type="single")
        table(b, 4, y, 7)
        chair(b, 3, y, 7, "east")
        b.set(2, y, 9, "potted_red_tulip")
        b.lantern(5, y + 3, 5, hanging=True)
        b.set(9, y, 2, "barrel", facing="up", open="false")
        b.set(9, y, 9, "bookshelf")


# ================================================================================================ economy

@design("farm", 6, 10, [("farmland", 24), ("water", 2), ("composter", 1)])
def farm(b):
    ground(b)
    rnd = random.Random(7)
    # field with an irrigation ditch
    for x in range(1, 12):
        for z in range(7, 12):
            if x == 6:
                b.set(x, 0, z, "water")
            else:
                b.set(x, 0, z, "farmland", moisture=7)
                crop = ("wheat", "carrots", "potatoes", "beetroots")[(x - 1) // 3 % 4 if x < 6 else (x - 7) // 2 % 4]
                b.set(x, 1, z, crop, age=rnd.randint(1, 3) if crop == "beetroots" else rnd.randint(2, 7))
    for x in range(0, 13):
        b.set(x, 1, 12, "spruce_fence")
        b.set(x, 1, 6, "spruce_fence")
    for z in range(6, 13):
        b.set(0, 1, z, "spruce_fence")
        b.set(12, 1, z, "spruce_fence")
    b.set(6, 1, 12, "spruce_fence_gate", facing="south")
    b.set(6, 0, 12, "dirt_path")
    b.set(4, 1, 6, "spruce_fence_gate", facing="south")
    b.set(4, 0, 6, "dirt_path")
    b.set(6, 0, 6, "dirt_path")
    # scarecrow
    b.set(10, 1, 9, "spruce_fence")
    b.set(10, 2, 9, "hay_block")
    b.set(10, 3, 9, "carved_pumpkin", facing="south")
    # red barn with a gambrel roof
    x0, z0, x1, z1 = 1, 1, 7, 5
    b.fill(x0, 0, z0, x1, 0, z1, "spruce_planks")
    b.walls(x0, z0, x1, z1, 1, 4, "red_terracotta")
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        b.fill(x, 1, z, x, 4, z, "white_terracotta")
    b.band(x0, z0, x1, z1, 4, "white_terracotta")
    b.door(3, 1, z1, "spruce", "north", "left")
    b.door(4, 1, z1, "spruce", "north", "right")
    b.fill(3, 3, z1, 4, 3, z1, "white_terracotta")
    b.window(6, 2, z1, "south", "glass_pane", None, None, 1)
    b.window(2, 2, z1, "south", "glass_pane", None, None, 1)
    # gambrel: steep lower part, shallow upper part
    for x in range(x0 - 1, x1 + 2):
        b.stairs(x, 5, z0 - 1, "dark_oak", "south")
        b.stairs(x, 5, z1 + 1, "dark_oak", "north")
        b.stairs(x, 6, z0, "dark_oak", "south")
        b.stairs(x, 6, z1, "dark_oak", "north")
        b.slab(x, 7, z0 + 1, "dark_oak", "top")
        b.slab(x, 7, z1 - 1, "dark_oak", "top")
        b.slab(x, 8, z0 + 2, "dark_oak")
    for z in range(z0, z1 + 1):
        b.set(x0, 5, z, "red_terracotta")
        b.set(x1, 5, z, "red_terracotta")
    for z in range(z0 + 1, z1):
        b.set(x0, 6, z, "red_terracotta")
        b.set(x1, 6, z, "red_terracotta")
    b.set(x0, 7, z0 + 2, "white_terracotta")
    b.set(x1, 7, z0 + 2, "white_terracotta")
    # barn interior
    b.set(2, 1, 2, "hay_block", axis="y")
    b.set(2, 2, 2, "hay_block", axis="x")
    b.set(3, 1, 2, "hay_block", axis="z")
    b.set(6, 1, 2, "hay_block", axis="y")
    b.barrel(6, 1, 3)
    b.set(2, 1, 4, "composter", level=3)
    b.lantern(4, 4, 3, hanging=True)
    # silo
    for y in range(1, 9):
        for x, z in ((9, 1), (10, 1), (11, 2), (11, 3), (10, 4), (9, 4), (8, 3), (8, 2)):
            b.set(x, y, z, "white_terracotta" if y % 3 else "light_gray_terracotta")
    b.fill(9, 1, 2, 10, 8, 3, "hay_block", axis="y")
    for x, z, f in ((9, 1, "south"), (10, 1, "south"), (11, 2, "west"), (11, 3, "west"), (10, 4, "north"), (9, 4, "north"),
                    (8, 3, "east"), (8, 2, "east")):
        b.stairs(x, 9, z, "spruce", f)
    b.fill(9, 9, 2, 10, 9, 3, "spruce_planks")
    b.slab(9, 10, 2, "spruce")
    b.lamp_post(7, 6, 1, 2, "spruce_fence")


@design("lumber_mill", 5, 10, [("logs", 24), ("planks", 16), ("doors", 1), ("crafting_table", 1), ("stonecutter", 1), ("chests", 1)])
def lumber_mill(b):
    ground(b, "coarse_dirt")
    b.fill(1, 0, 2, 9, 0, 7, "spruce_planks")
    # open timber shed
    for x in (1, 5, 9):
        for z in (2, 7):
            b.fill(x, 1, z, x, 4, z, "stripped_spruce_log", axis="y")
    for x in range(1, 10):
        b.log(x, 5, 2, "stripped_spruce_log", "x")
        b.log(x, 5, 7, "stripped_spruce_log", "x")
    for z in range(3, 7):
        b.log(1, 5, z, "stripped_spruce_log", "z")
        b.log(9, 5, z, "stripped_spruce_log", "z")
    b.fill(2, 1, 2, 4, 4, 2, "spruce_planks")
    b.fill(1, 1, 3, 1, 4, 6, "spruce_planks")
    b.fill(1, 2, 4, 1, 3, 5, "glass_pane")
    # office in the corner
    b.fill(6, 1, 2, 8, 4, 2, "spruce_planks")
    b.fill(9, 1, 3, 9, 4, 4, "spruce_planks")
    b.fill(6, 1, 4, 8, 4, 4, "spruce_planks")
    b.door(7, 1, 4, "spruce", "north")
    b.window(9, 2, 3, "east", "glass_pane", None, None, 2)
    b.chest(8, 1, 3, "west")
    b.set(6, 1, 3, "crafting_table")
    b.lantern(7, 4, 3, hanging=True)
    # saw and work area
    b.set(3, 1, 5, "stonecutter", facing="south")
    b.set(4, 1, 5, "spruce_slab", type="top")
    b.set(2, 1, 5, "spruce_slab", type="top")
    b.set(5, 1, 3, "grindstone", face="floor", facing="south")
    b.barrel(2, 1, 3)
    b.lantern(4, 4, 5, hanging=True)
    # roof
    b.gable_roof(1, 2, 9, 7, 6, "spruce", "x", overhang=1, gable="spruce_planks")
    # log piles
    for z0 in (8, 9):
        for x in range(1, 5):
            b.log(x, 1, z0, "spruce_log", "x")
        for x in range(1, 4):
            b.log(x, 2, z0, "oak_log", "x")
    for x in range(6, 10):
        b.log(x, 1, 9, "birch_log", "z")
        b.log(x, 1, 10, "birch_log", "z")
    for x in range(6, 9):
        b.log(x, 2, 9, "birch_log", "z")
        b.log(x, 2, 10, "birch_log", "z")
    # crane
    b.fill(10, 1, 1, 10, 7, 1, "stripped_dark_oak_log", axis="y")
    for z in range(1, 5):
        b.log(10, 8, z, "stripped_dark_oak_log", "z")
    for y in range(5, 8):
        b.set(10, y, 4, "chain")
    b.set(10, 4, 4, "spruce_log", axis="x")


@design("mine", 5, 15, [("stone_blocks", 40), ("doors", 1), ("rails", 6), ("chests", 1), ("torches", 4)], )
def mine(b):
    """Pit head: brick engine house and a steel headframe over an inclined shaft going north under the tower."""
    ground(b, "gravel")
    # inclined adit: entrance at z = 9, five steps down towards the north
    b.fill(6, 0, 3, 10, 0, 10, "stone_bricks")
    steps = []
    for k in range(0, 6):
        z = 9 - k
        y = 1 - k
        steps.append((8, y, z))
        b.air(8, y, z, 8, y + 2, z)
        b.set(8, y - 1, z, "stone_bricks")
        b.fill(7, y - 1, z, 7, y + 2, z, "stone_bricks")
        b.fill(9, y - 1, z, 9, y + 2, z, "stone_bricks")
        b.set(8, y + 3, z, "stone_bricks")
        b.set(8, y, z, "rail")
    b.set(7, -2, 5, "stone_bricks")
    # torches on the adit walls (attached to the side walls)
    b.set(8, -1, 6, "air")
    b.set(8, -3, 4, "air")
    b.blocks.pop((8, 2, 9), None)
    b.air(8, 1, 9, 8, 3, 9)
    b.air(8, 1, 10, 8, 3, 10)
    b.set(8, 0, 10, "stone_bricks")
    # portal around the adit entrance
    b.fill(7, 1, 9, 7, 4, 9, "stone_bricks")
    b.fill(9, 1, 9, 9, 4, 9, "stone_bricks")
    b.fill(7, 4, 9, 9, 4, 9, "stone_bricks")
    b.stairs(7, 4, 10, "stone_brick", "north", "top")
    b.stairs(8, 4, 10, "stone_brick", "north", "top")
    b.stairs(9, 4, 10, "stone_brick", "north", "top")
    b.wall_torch(7, 3, 10, "south")
    b.wall_torch(9, 3, 10, "south")
    # headframe tower over the shaft
    for x, z in ((6, 3), (10, 3), (6, 8), (10, 8)):
        b.fill(x, 1, z, x, 11, z, "deepslate_brick_wall")
    for y in (4, 8, 11):
        for x in range(6, 11):
            b.set(x, y, 3, "polished_blackstone_slab", type="top") if y != 11 else b.set(x, y, 3, "polished_blackstone")
            b.set(x, y, 8, "polished_blackstone_slab", type="top") if y != 11 else b.set(x, y, 8, "polished_blackstone")
        for z in range(4, 8):
            b.set(6, y, z, "polished_blackstone_slab", type="top") if y != 11 else b.set(6, y, z, "polished_blackstone")
            b.set(10, y, z, "polished_blackstone_slab", type="top") if y != 11 else b.set(10, y, z, "polished_blackstone")
    for x in range(7, 10):
        for z in range(4, 8):
            b.set(x, 11, z, "polished_blackstone")
    # winding wheel
    for (y, z) in ((12, 4), (13, 4), (14, 5), (14, 6), (13, 7), (12, 7), (12, 5), (12, 6)):
        b.set(8, y, z, "iron_bars")
    b.set(8, 13, 5, "iron_block")
    b.set(8, 13, 6, "iron_block")
    b.set(8, 12, 5, "iron_bars")
    for y in range(1, 11):
        b.set(8, y, 6, "chain") if y >= 5 else None
    b.set(8, 4, 6, "iron_trapdoor", facing="north", half="top", open="false")
    # brick engine house
    x0, z0, x1, z1 = 0, 1, 4, 9
    b.fill(x0, 0, z0, x1, 0, z1, "cobblestone")
    b.walls(x0, z0, x1, z1, 1, 5, "bricks")
    b.band(x0, z0, x1, z1, 1, "stone_bricks")
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        b.fill(x, 1, z, x, 5, z, "stone_bricks")
    b.door(2, 1, z1, "spruce", "north")
    b.set(2, 3, z1, "glass_pane")
    win_col(b, (3, 6), 2, x0, "west", lintel="stone_bricks")
    win_col(b, (3, 7), 2, x1, "east", lintel="stone_bricks")
    b.window(3, 2, z1, "south", "glass_pane", "stone_brick_slab", "stone_bricks", 2)
    b.gable_roof(x0, z0, x1, z1, 6, "deepslate_tile", "z", overhang=0, gable="bricks")
    b.chimney(1, 2, 6, 10, "bricks", "stone_brick_slab")
    # engine and stores inside
    b.set(1, 1, 2, "blast_furnace", facing="east")
    b.set(1, 1, 3, "furnace", facing="east")
    b.chest(3, 1, 2, "south")
    b.barrel(3, 1, 3)
    b.barrel(3, 2, 3)
    b.set(1, 1, 7, "anvil", facing="east")
    b.wall_torch(2, 3, 2, "south")
    b.wall_torch(3, 3, 8, "north")
    b.lantern(3, 5, 5, hanging=True)
    # tracks to the shaft
    for x in range(3, 8):
        b.set(x, 1, 10, "rail")
    b.set(2, 1, 10, "rail")
    b.set(8, 1, 10, "rail")
    b.fill(2, 0, 10, 8, 0, 10, "gravel")
    b.tunnel = (8, -4, 4, 2)  # feet of the last step, direction north


@design("smeltery", 5, 16, [("masonry", 40), ("cobblestone", 20), ("doors", 1), ("furnace", 3), ("blast_furnace", 1), ("anvils", 1),
                            ("cauldrons", 1)])
def smeltery(b):
    ground(b, "gravel")
    x0, z0, x1, z1 = 1, 1, 9, 8
    b.fill(x0, 0, z0, x1, 0, z1, "cobblestone")
    b.walls(x0, z0, x1, z1, 1, 7, "bricks")
    for x in (x0, 5, x1):
        for z in (z0, z1):
            b.fill(x, 1, z, x, 7, z, "stone_bricks")
    for z in (z0, 4, z1):
        b.fill(x0, 1, z, x0, 7, z, "stone_bricks")
        b.fill(x1, 1, z, x1, 7, z, "stone_bricks")
    # tall arched windows
    for x in (3, 7):
        b.fill(x, 2, z1, x, 5, z1, "glass_pane")
        b.fill(x, 2, z0, x, 5, z0, "glass_pane")
    for z in (2, 3, 6, 7):
        b.fill(x0, 2, z, x0, 5, z, "glass_pane")
        b.fill(x1, 2, z, x1, 5, z, "glass_pane")
    # large doorway
    b.door(4, 1, z1, "dark_oak", "north", "left")
    b.door(6, 1, z1, "dark_oak", "north", "right")
    b.air(5, 1, z1, 5, 3, z1)
    b.set(5, 1, z1, "air")
    b.fill(4, 3, z1, 6, 3, z1, "stone_bricks")
    b.set(5, 3, z1, "chiseled_stone_bricks")
    # roof with a skylight ridge
    b.gable_roof(x0, z0, x1, z1, 8, "deepslate_tile", "x", overhang=1, gable="bricks")
    for x in range(2, 9):
        b.set(x, 11, 4, "glass")
        b.set(x, 11, 5, "glass")
    # chimney stack
    for y in range(0, 16):
        b.fill(9, y, 9, 10, y, 10, "bricks")
    for x, z in ((9, 9), (10, 9), (9, 10), (10, 10)):
        b.set(x, 15, z, "stone_bricks")
    b.smoke.append((9, 16, 9))
    b.smoke.append((10, 16, 10))
    b.set(9, 16, 9, "air")
    # furnaces and the casting floor
    for z in (2, 3, 6):
        b.set(2, 1, z, "furnace", facing="east")
    b.set(2, 1, 7, "furnace", facing="east")
    b.set(8, 1, 3, "blast_furnace", facing="west")
    b.set(8, 1, 4, "blast_furnace", facing="west")
    b.set(8, 1, 6, "anvil", facing="west")
    b.set(8, 1, 7, "lava_cauldron")
    b.set(4, 1, 2, "smithing_table")
    b.barrel(6, 1, 2)
    b.set(7, 1, 2, "chest", facing="south", type="single")
    for x in (3, 7):
        b.lantern(x, 7, 5, hanging=True)
    # coal heap outside
    b.set(1, 1, 9, "coal_block")
    b.set(2, 1, 9, "coal_block")
    b.set(1, 2, 9, "coal_block")


@design("market", 6, 10, [("building_materials", 30), ("doors", 1), ("barrel", 4), ("chests", 2), ("wool", 6), ("lanterns", 2)])
def market(b):
    """Covered market hall on brick pillars with striped stalls."""
    ground(b, "smooth_stone")
    for x in range(0, 13):
        for z in range(0, 13):
            if (x + z) % 2 == 0:
                b.set(x, 0, z, "polished_andesite")
    x0, z0, x1, z1 = 1, 1, 11, 11
    cols = [(x, z) for x in (1, 6, 11) for z in (1, 6, 11)]
    for x, z in cols:
        if (x, z) == (6, 6):
            continue
        b.fill(x, 1, z, x, 5, z, "bricks")
        b.set(x, 1, z, "stone_bricks")
    # beams and roof
    for x in range(x0, x1 + 1):
        b.set(x, 6, z0, "stripped_dark_oak_log", axis="x")
        b.set(x, 6, z1, "stripped_dark_oak_log", axis="x")
        b.set(x, 6, 6, "stripped_dark_oak_log", axis="x")
    for z in range(z0, z1 + 1):
        b.set(x0, 6, z, "stripped_dark_oak_log", axis="z")
        b.set(x1, 6, z, "stripped_dark_oak_log", axis="z")
    b.gable_roof(x0, z0, x1, z1, 7, "deepslate_tile", "x", overhang=1, gable="bricks")
    # lantern clock in the gable
    # four stalls with striped awnings
    stalls = [((2, 2), ("red_wool", "white_wool")), ((7, 2), ("green_wool", "white_wool")),
              ((2, 8), ("blue_wool", "white_wool")), ((7, 8), ("yellow_wool", "white_wool"))]
    for (sx, sz), colors in stalls:
        for x in range(sx, sx + 4):
            b.set(x, 1, sz + 1, "spruce_slab", type="top")
            b.set(x, 4, sz + 1, colors[(x - sx) % 2])
        b.barrel(sx, 1, sz)
        b.barrel(sx + 3, 1, sz)
        b.set(sx + 1, 1, sz, "melon" if sz == 2 else "pumpkin")
        b.set(sx + 2, 1, sz, "hay_block", axis="y")
        b.set(sx + 1, 2, sz + 1, "potted_cactus") if sx == 2 else b.set(sx + 1, 2, sz + 1, "flower_pot")
        b.fill(sx, 2, sz, sx, 3, sz, "spruce_fence")
        b.fill(sx + 3, 2, sz, sx + 3, 3, sz, "spruce_fence")
    # office kiosk with the door
    b.fill(9, 1, 5, 10, 3, 7, "spruce_planks")
    b.door(10, 1, 7, "spruce", "north")
    b.set(9, 1, 6, "air")
    b.set(10, 1, 6, "air")
    b.set(10, 2, 6, "air")
    b.set(9, 2, 6, "air")
    b.chest(9, 1, 6, "east")
    b.chest(2, 1, 6, "east")
    b.set(9, 2, 7, "glass_pane")
    for x, z in ((3, 6), (9, 3), (9, 10)):
        b.lantern(x, 5, z, hanging=True)
    b.lantern(4, 5, 4, hanging=True)
    b.lamp_post(0, 12)
    b.lamp_post(12, 12)


@design("granary", 5, 13, [("building_materials", 30), ("doors", 1), ("hay_block", 12), ("barrel", 4)])
def granary(b):
    ground(b, "gravel")
    x0, z0, x1, z1 = 1, 1, 7, 9
    b.fill(x0, 0, z0, x1, 0, z1, "spruce_planks")
    b.walls(x0, z0, x1, z1, 1, 8, "bricks")
    b.band(x0, z0, x1, z1, 1, "stone_bricks")
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        b.fill(x, 1, z, x, 8, z, "stone_bricks")
    # loading doors on two levels and a hoist beam
    b.door(4, 1, z1, "spruce", "north")
    b.fill(3, 5, z1, 5, 6, z1, "spruce_planks")
    b.set(4, 9, z1 + 1, "stripped_dark_oak_log", axis="z")
    for y in range(6, 9):
        b.set(4, y, z1 + 1, "chain")
    b.set(4, 5, z1 + 1, "hay_block", axis="y")
    win_col(b, (3, 7), 3, x0, "west", "glass_pane", 2, "stone_brick_slab", "stone_bricks")
    win_col(b, (3, 7), 3, x1, "east", "glass_pane", 2, "stone_brick_slab", "stone_bricks")
    win_row(b, (2, 6), 6, z1, "south", "glass_pane", 2, None, "stone_bricks")
    b.gable_roof(x0, z0, x1, z1, 9, "dark_oak", "z", overhang=1, gable="bricks")
    b.fill(x0 + 1, 5, z0 + 1, x1 - 1, 5, z1 - 1, "spruce_planks")
    for y in range(1, 6):
        b.set(6, y, 8, "ladder", facing="north")
    # sacks and bales
    for x in range(2, 6):
        for z in range(2, 5):
            b.set(x, 1, z, "hay_block", axis="y")
    for x in range(2, 5):
        b.set(x, 2, 2, "hay_block", axis="x")
    for z in (6, 7):
        b.barrel(2, 1, z)
        b.barrel(2, 2, z)
    b.lantern(4, 4, 6, hanging=True)
    for x in range(2, 6):
        b.set(x, 6, 3, "hay_block", axis="z")
    # silo
    ring = [(8, 1), (9, 1), (10, 2), (10, 3), (9, 4), (8, 4)]
    for y in range(1, 11):
        for x, z in ring:
            b.set(x, y, z, "smooth_stone" if y % 4 else "stone_bricks")
    b.fill(8, 1, 2, 9, 10, 3, "hay_block", axis="y")
    for x, z, f in ((8, 1, "south"), (9, 1, "south"), (10, 2, "west"), (10, 3, "west"), (9, 4, "north"), (8, 4, "north")):
        b.stairs(x, 11, z, "stone_brick", f)
    b.fill(8, 11, 2, 9, 11, 3, "stone_bricks")
    b.slab(8, 12, 2, "stone_brick")


@design("bank", 5, 12, [("masonry", 40), ("doors", 1), ("gold_block", 1), ("iron_bars", 6), ("chests", 4)])
def bank(b):
    """Neoclassical bank: quartz portico, pediment, barred windows, a vault."""
    ground(b, "smooth_stone")
    x0, z0, x1, z1 = 1, 1, 9, 7
    b.fill(x0, 0, z0, x1, 0, z1, "polished_diorite")
    b.walls(x0, z0, x1, z1, 1, 7, "quartz_bricks")
    b.band(x0, z0, x1, z1, 1, "smooth_stone")
    for z in (3, 5):
        b.fill(x0, 2, z, x0, 4, z, "iron_bars")
        b.fill(x1, 2, z, x1, 4, z, "iron_bars")
    for x in (3, 7):
        b.fill(x, 2, z0, x, 4, z0, "iron_bars")
        b.fill(x, 2, z1, x, 4, z1, "iron_bars")
    b.door(5, 1, z1, "dark_oak", "north", "left")
    b.set(5, 3, z1, "glass_pane")
    # portico with columns and steps
    b.fill(1, 0, 8, 9, 0, 9, "smooth_stone")
    for x in (2, 4, 6, 8):
        b.fill(x, 1, 9, x, 6, 9, "quartz_pillar", axis="y")
    b.fill(1, 7, 8, 9, 7, 9, "quartz_block")
    b.fill(x0, 8, z0, x1, 8, z1 + 2, "smooth_stone")
    for x in range(x0, x1 + 1):
        b.stairs(x, 7, 10, "quartz", "north", "top")
    # pediment
    for i, (xa, xb) in enumerate(((1, 9), (2, 8), (3, 7), (4, 6), (5, 5))):
        y = 9 + i
        if xa == xb:
            b.slab(xa, y, 9, "quartz")
            continue
        b.stairs(xa, y, 9, "quartz", "east")
        b.stairs(xb, y, 9, "quartz", "west")
        for x in range(xa + 1, xb):
            b.set(x, y, 9, "quartz_block")
    # roof parapet
    for x in range(x0, x1 + 1):
        b.slab(x, 9, z0, "smooth_stone")
    for z in range(z0, z1 + 1):
        b.slab(x0, 9, z, "smooth_stone")
        b.slab(x1, 9, z, "smooth_stone")
    # hall and vault
    for x in range(2, 9):
        b.set(x, 1, 3, "polished_andesite_slab", type="top") if x not in (5,) else None
    b.set(5, 1, 3, "air")
    b.fill(2, 1, 2, 8, 1, 2, "air")
    for x in (2, 3, 7, 8):
        b.chest(x, 1, 2, "south")
    b.set(6, 1, 2, "gold_block")
    b.set(4, 1, 2, "gold_block")
    for x in (3, 7):
        b.lantern(x, 6, 5, hanging=True)
    b.set(2, 1, 6, "potted_fern")
    b.set(8, 1, 6, "potted_fern")


@design("factory", 7, 19, [("masonry", 80), ("doors", 1), ("furnace", 2), ("crafting_table", 2), ("piston", 2), ("chests", 2),
                           ("glass_panes", 8)])
def factory(b):
    """Red-brick factory hall with stone pilasters, big windows, a sawtooth of chimneys and a loading yard."""
    ground(b, "gravel")
    sidewalk(b, 0, 13, 14, 14, "smooth_stone")
    x0, z0, x1, z1 = 1, 2, 13, 12
    b.fill(x0, 0, z0, x1, 0, z1, "polished_andesite")
    b.walls(x0, z0, x1, z1, 1, 9, "bricks")
    b.band(x0, z0, x1, z1, 1, "stone_bricks")
    b.band(x0, z0, x1, z1, 9, "stone_bricks")
    # pilasters
    for x in (x0, 5, 9, x1):
        for z in (z0, z1):
            b.fill(x, 1, z, x, 9, z, "stone_bricks")
    for z in (z0, 7, z1):
        b.fill(x0, 1, z, x0, 9, z, "stone_bricks")
        b.fill(x1, 1, z, x1, 9, z, "stone_bricks")
    # big multi-pane windows with green frames
    for bay in ((2, 4), (6, 8), (10, 12)):
        for x in range(bay[0], bay[1] + 1):
            b.fill(x, 5, z1, x, 7, z1, "green_stained_glass_pane" if x == bay[0] + 1 else "glass_pane")
            b.fill(x, 5, z0, x, 7, z0, "glass_pane")
        b.fill(bay[0], 4, z1 + 1, bay[1], 4, z1 + 1, "stone_brick_slab", type="top")
    for bay in ((3, 6), (8, 11)):
        for z in range(bay[0], bay[1] + 1):
            b.fill(x0, 4, z, x0, 7, z, "glass_pane")
            b.fill(x1, 4, z, x1, 7, z, "glass_pane")
    # loading bay with a door and a big gate
    b.door(3, 1, z1, "dark_oak", "north")
    b.set(3, 3, z1, "glass_pane")
    b.air(10, 1, z1, 12, 3, z1)
    b.fill(10, 0, z1, 12, 0, z1, "polished_andesite")
    b.fill(9, 4, z1, 13, 4, z1, "stone_bricks")
    # office floor (mezzanine) at the west end
    b.fill(x0 + 1, 5, z0 + 1, 4, 5, z1 - 1, "spruce_planks")
    for z in range(z0 + 1, z1):
        b.set(4, 6, z, "iron_bars") if z % 2 else None
    for y in range(1, 6):
        b.set(2, y, z1 - 1, "ladder", facing="north")
    b.set(2, 6, 4, "crafting_table")
    b.chest(3, 6, 3, "south")
    b.set(2, 6, 6, "lectern", facing="east")
    b.lantern(3, 8, 6, hanging=True)
    # roof: gable along x with a skylight
    b.gable_roof(x0, z0, x1, z1, 10, "deepslate_tile", "x", overhang=1, gable="bricks")
    for x in range(2, 13):
        b.set(x, 13, 6, "glass") if x % 3 else None
        b.set(x, 13, 8, "glass") if x % 3 else None
    # chimneys
    for (cx, cz) in ((11, 3), (3, 3)):
        for y in range(1, 19):
            b.fill(cx, y, cz, cx + 1, y, cz + 1, "bricks")
        for x, z in ((cx, cz), (cx + 1, cz), (cx, cz + 1), (cx + 1, cz + 1)):
            b.set(x, 17, z, "stone_bricks")
        b.smoke.append((cx, 19, cz))
        b.smoke.append((cx + 1, 19, cz + 1))
    b.fill(11, 19, 3, 12, 19, 4, "air")
    b.fill(3, 19, 3, 4, 19, 4, "air")
    # machines
    for x in (6, 8, 10):
        b.set(x, 1, 5, "piston", facing="up", extended="false")
        b.set(x, 1, 6, "furnace", facing="south") if x != 8 else b.set(x, 1, 6, "blast_furnace", facing="south")
    b.set(7, 1, 5, "crafting_table")
    b.set(11, 1, 5, "crafting_table")
    for x in range(6, 12):
        b.set(x, 1, 8, "rail")
    b.set(12, 1, 9, "chest", facing="west", type="single")
    b.set(12, 1, 10, "chest", facing="west", type="single")
    b.barrel(6, 1, 10)
    b.barrel(7, 1, 10)
    b.barrel(6, 2, 10)
    b.set(5, 1, 11, "smithing_table")
    for x in (7, 11):
        b.lantern(x, 9, 7, hanging=True)
    # yard: pallets and crates
    for x in (1, 2):
        b.slab(x, 1, 13, "spruce")
    b.barrel(1, 2, 13)
    b.set(13, 1, 13, "spruce_slab", type="bottom")
    b.set(14, 1, 13, "barrel", facing="up", open="false")
    b.lamp_post(8, 14, 1, 3)


# ================================================================================================ society

@design("tavern", 5, 12, [("building_materials", 30), ("doors", 1), ("barrel", 4), ("campfires", 1), ("lanterns", 2)])
def tavern(b):
    """Corner pub: dark wood front with an awning, brick upper floor."""
    ground(b, "smooth_stone")
    x0, z0, x1, z1 = 1, 1, 9, 8
    b.fill(x0, 0, z0, x1, 0, z1, "dark_oak_planks")
    b.walls(x0, z0, x1, z1, 1, 4, "dark_oak_planks")
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        b.fill(x, 1, z, x, 4, z, "stripped_dark_oak_log", axis="y")
    for x in (2, 3, 4, 6, 7, 8):
        b.fill(x, 2, z1, x, 3, z1, "glass_pane")
    for z in (3, 4, 5, 6):
        b.fill(x1, 2, z, x1, 3, z, "glass_pane")
    b.door(5, 1, z1, "spruce", "north")
    b.set(5, 3, z1, "glass_pane")
    b.awning(1, 9, z1, 4, "south", ("green_wool", "white_wool"), 2)
    b.walls(x0, z0, x1, z1, 5, 8, "bricks")
    b.band(x0, z0, x1, z1, 5, "stone_bricks")
    win_row(b, (2, 4, 6, 8), 6, z1, "south", "glass_pane", 2, "stone_brick_slab", "stone_bricks")
    win_col(b, (3, 6), 6, x1, "east", "glass_pane", 2, "stone_brick_slab", "stone_bricks")
    win_col(b, (3, 6), 6, x0, "west", "glass_pane", 2, "stone_brick_slab", "stone_bricks")
    b.cornice(x0, z0, x1, z1, 9, "dark_oak")
    b.fill(x0, 9, z0, x1, 9, z1, "smooth_stone")
    b.walls(x0, z0, x1, z1, 10, 10, "bricks")
    b.chimney(2, 2, 10, 11, "bricks", "stone_brick_slab")
    # hanging sign
    b.set(x1 + 1, 4, 7, "dark_oak_fence")
    b.set(x1 + 1, 3, 7, "lantern", hanging="true")
    # bar
    for x in range(2, 7):
        b.set(x, 1, 3, "spruce_slab", type="top") if x != 6 else b.set(x, 1, 3, "barrel", facing="up", open="false")
    b.barrel(2, 1, 2, "east")
    b.barrel(3, 1, 2, "south")
    b.barrel(4, 1, 2, "south")
    b.barrel(2, 2, 2, "east")
    # fireplace
    b.set(8, 1, 2, "campfire", lit="true", facing="south")
    b.fill(7, 1, 2, 7, 3, 2, "bricks")
    b.fill(8, 2, 2, 8, 3, 2, "bricks")
    b.set(8, 2, 2, "air")
    b.set(8, 3, 2, "bricks")
    # tables
    for (tx, tz) in ((3, 6), (7, 5)):
        table(b, tx, 1, tz, "dark_oak_pressure_plate", "dark_oak_fence")
        chair(b, tx - 1, 1, tz, "east", "dark_oak")
        chair(b, tx + 1, 1, tz, "west", "dark_oak")
    b.lantern(4, 4, 5, hanging=True)
    b.lantern(7, 4, 4, hanging=True)
    # upstairs room
    b.fill(x0 + 1, 5, z0 + 1, x1 - 1, 5, z1 - 1, "spruce_planks")
    for y in range(1, 6):
        b.set(2, y, 7, "ladder", facing="north")
    b.bed(7, 6, 3, "green", "north")
    b.lantern(5, 8, 4, hanging=True)
    # beer garden
    b.set(2, 1, 10, "spruce_fence")
    b.set(2, 2, 10, "spruce_pressure_plate")


@design("academy", 6, 18, [("building_materials", 30), ("doors", 1), ("bookshelf", 10), ("lectern", 2), ("cartography_table", 1)])
def academy(b):
    """Brick university with a clock tower over the entrance."""
    ground(b, "smooth_stone")
    x0, z0, x1, z1 = 1, 1, 11, 8
    b.fill(x0, 0, z0, x1, 0, z1, "spruce_planks")
    b.walls(x0, z0, x1, z1, 1, 9, "bricks")
    b.band(x0, z0, x1, z1, 1, "stone_bricks")
    b.band(x0, z0, x1, z1, 5, "stone_bricks")
    for y0 in (2, 6):
        win_row(b, (2, 3, 9, 10), y0, z1, "south", "glass_pane", 2, "stone_brick_slab", "stone_bricks")
        win_row(b, (2, 4, 6, 8, 10), y0, z0, "north", "glass_pane", 2, "stone_brick_slab", "stone_bricks")
        win_col(b, (3, 6), y0, x0, "west", "glass_pane", 2, "stone_brick_slab", "stone_bricks")
        win_col(b, (3, 6), y0, x1, "east", "glass_pane", 2, "stone_brick_slab", "stone_bricks")
    b.gable_roof(x0, z0, x1, z1, 10, "deepslate_tile", "x", overhang=1, gable="bricks")
    # clock tower
    tx0, tz0, tx1, tz1 = 4, 7, 8, 10
    b.fill(tx0, 0, tz0, tx1, 0, tz1, "polished_andesite")
    b.walls(tx0, tz0, tx1, tz1, 1, 13, "bricks")
    for x, z in ((tx0, tz0), (tx1, tz0), (tx0, tz1), (tx1, tz1)):
        b.fill(x, 1, z, x, 13, z, "stone_bricks")
    b.air(5, 1, 7, 7, 3, 9)
    b.door(6, 1, tz1, "dark_oak", "north")
    b.fill(5, 3, tz1, 7, 3, tz1, "stone_bricks")
    b.set(6, 3, tz1, "chiseled_stone_bricks")
    b.window(6, 5, tz1, "south", "glass_pane", "stone_brick_slab", None, 3)
    # clock face
    for (x, y) in ((5, 10), (6, 10), (7, 10), (5, 11), (7, 11), (5, 12), (6, 12), (7, 12)):
        b.set(x, y, tz1, "white_concrete")
    b.set(6, 11, tz1, "black_concrete")
    b.cornice(tx0, tz0, tx1, tz1, 14, "stone_brick")
    b.fill(tx0, 14, tz0, tx1, 14, tz1, "stone_bricks")
    # spire
    for i, (a0, a1) in enumerate(((4, 8), (5, 7), (6, 6))):
        y = 15 + i
        for x in range(a0, a1 + 1):
            for z in range(tz0 + (a0 - 4), tz1 - (a0 - 4) + 1):
                if x in (a0, a1) or z in (tz0 + (a0 - 4), tz1 - (a0 - 4)):
                    b.set(x, y, z, "deepslate_tiles")
    # library and lecture hall
    for x in range(2, 11):
        if x in (5, 6, 7):
            continue
        b.set(x, 1, 2, "bookshelf")
        b.set(x, 2, 2, "bookshelf")
    b.set(3, 1, 5, "lectern", facing="south")
    b.set(9, 1, 5, "lectern", facing="south")
    b.set(10, 1, 7, "cartography_table")
    for x in (3, 9):
        table(b, x, 1, 4)
    b.fill(x0 + 1, 5, z0 + 1, x1 - 1, 5, z1 - 1, "spruce_planks")
    b.fill(5, 5, 7, 7, 5, 8, "spruce_planks")
    for y in range(1, 6):
        b.set(10, y, 3, "ladder", facing="west")
    for x in range(2, 11, 2):
        b.set(x, 6, 2, "bookshelf")
        b.set(x, 7, 2, "bookshelf")
    b.lantern(4, 4, 4, hanging=True)
    b.lantern(8, 4, 4, hanging=True)
    b.lantern(6, 9, 4, hanging=True)
    b.lamp_post(2, 11)
    b.lamp_post(10, 11)


@design("temple", 6, 24, [("masonry", 60), ("doors", 1), ("candles", 4), ("bell", 1), ("glass_panes", 8)])
def temple(b):
    """Brick church: nave with tall coloured windows and a front tower with a belfry and a spire."""
    ground(b, "smooth_stone")
    x0, z0, x1, z1 = 3, 1, 9, 9
    b.fill(x0, 0, z0, x1, 0, z1, "polished_andesite")
    b.walls(x0, z0, x1, z1, 1, 7, "bricks")
    b.band(x0, z0, x1, z1, 1, "stone_bricks")
    for z in (3, 5, 7):
        for y in range(2, 7):
            b.set(x0, y, z, "light_blue_stained_glass_pane" if y % 2 else "yellow_stained_glass_pane")
            b.set(x1, y, z, "light_blue_stained_glass_pane" if y % 2 else "yellow_stained_glass_pane")
    # buttresses
    for z in (2, 4, 6, 8):
        b.fill(x0 - 1, 1, z, x0 - 1, 4, z, "stone_bricks")
        b.fill(x1 + 1, 1, z, x1 + 1, 4, z, "stone_bricks")
        b.stairs(x0 - 1, 5, z, "stone_brick", "east")
        b.stairs(x1 + 1, 5, z, "stone_brick", "west")
    b.gable_roof(x0, z0, x1, z1, 8, "deepslate_tile", "z", overhang=1, gable="bricks")
    # rose window at the back
    b.fill(5, 8, z0, 7, 10, z0, "white_stained_glass_pane")
    b.set(6, 9, z0, "red_stained_glass_pane")
    # tower
    tx0, tz0, tx1, tz1 = 4, 9, 8, 12
    b.fill(tx0, 0, tz0, tx1, 0, tz1, "polished_andesite")
    b.walls(tx0, tz0, tx1, tz1, 1, 16, "bricks")
    for x, z in ((tx0, tz0), (tx1, tz0), (tx0, tz1), (tx1, tz1)):
        b.fill(x, 1, z, x, 16, z, "stone_bricks")
    b.air(5, 1, 9, 7, 4, 11)
    b.door(6, 1, tz1, "dark_oak", "north")
    b.set(6, 3, tz1, "glass_pane")
    b.fill(5, 4, tz1, 7, 4, tz1, "stone_bricks")
    b.stairs(5, 3, tz1, "stone_brick", "east", "top")
    b.stairs(7, 3, tz1, "stone_brick", "west", "top")
    b.window(6, 6, tz1, "south", "white_stained_glass_pane", "stone_brick_slab", None, 3)
    # belfry with the bell
    b.fill(5, 12, 10, 7, 12, 11, "spruce_planks")
    for x in (5, 6, 7):
        b.fill(x, 13, tz0, x, 15, tz0, "air") if x == 6 else None
        b.fill(x, 13, tz1, x, 15, tz1, "air") if x == 6 else None
    b.fill(tx0, 13, 10, tx0, 15, 11, "air")
    b.fill(tx1, 13, 10, tx1, 15, 11, "air")
    b.set(6, 15, 10, "bell", attachment="ceiling", facing="south")
    b.set(6, 16, 10, "stone_bricks")
    for y in range(1, 12):
        b.set(5, y, 10, "ladder", facing="east") if y > 4 else None
    b.cornice(tx0, tz0, tx1, tz1, 17, "stone_brick")
    b.fill(tx0, 17, tz0, tx1, 17, tz1, "stone_bricks")
    # spire
    for i in range(0, 3):
        a0, a1 = tx0 + i, tx1 - i
        c0, c1 = tz0 + i, tz1 - i
        if c0 > c1:
            break
        for x in range(a0, a1 + 1):
            for z in range(c0, c1 + 1):
                if x in (a0, a1) or z in (c0, c1):
                    b.set(x, 18 + i * 2, z, "deepslate_tiles")
                    b.set(x, 19 + i * 2, z, "deepslate_tiles")
    b.set(6, 24, 10, "deepslate_tile_wall")
    # nave: altar, candles, pews
    b.set(5, 1, 2, "smooth_quartz")
    b.set(6, 1, 2, "smooth_quartz")
    b.set(7, 1, 2, "smooth_quartz")
    b.set(5, 2, 2, "candle", candles=3, lit="true")
    b.set(7, 2, 2, "candle", candles=3, lit="true")
    b.set(6, 2, 2, "white_candle", candles=1, lit="true")
    b.set(4, 1, 2, "candle", candles=4, lit="true")
    b.set(8, 1, 2, "candle", candles=4, lit="true")
    for z in (4, 5, 7, 8):
        for x in (4, 5, 7, 8):
            b.stairs(x, 1, z, "spruce", "south")
    b.lantern(6, 7, 5, hanging=True)


@design("hospital", 6, 13, [("building_materials", 30), ("doors", 1), ("beds", 3), ("brewing_stand", 1), ("cauldrons", 1), ("flower_pots", 2)])
def hospital(b):
    """White hospital with a red cross and an entrance canopy."""
    ground(b, "smooth_stone")
    x0, z0, x1, z1 = 1, 1, 11, 9
    b.fill(x0, 0, z0, x1, 0, z1, "smooth_quartz")
    b.walls(x0, z0, x1, z1, 1, 9, "white_concrete")
    b.band(x0, z0, x1, z1, 1, "light_gray_concrete")
    b.band(x0, z0, x1, z1, 5, "light_gray_concrete")
    for y0 in (2, 6):
        win_row(b, (2, 4, 8, 10), y0, z1, "south", "light_gray_stained_glass_pane", 2, "smooth_quartz_slab")
        win_row(b, (2, 4, 6, 8, 10), y0, z0, "north", "light_gray_stained_glass_pane", 2, "smooth_quartz_slab")
        win_col(b, (3, 5, 7), y0, x0, "west", "light_gray_stained_glass_pane", 2, "smooth_quartz_slab")
        win_col(b, (3, 5, 7), y0, x1, "east", "light_gray_stained_glass_pane", 2, "smooth_quartz_slab")
    # red cross on the facade
    for (x, y) in ((6, 6), (6, 7), (6, 8), (5, 7), (7, 7)):
        b.set(x, y, z1, "red_concrete")
    # entrance and canopy
    b.door(6, 1, z1, "birch", "north")
    b.set(6, 3, z1, "glass_pane")
    b.fill(4, 4, z1 + 1, 8, 4, z1 + 2, "smooth_quartz_slab", type="top")
    for x in (4, 8):
        b.fill(x, 1, z1 + 2, x, 3, z1 + 2, "quartz_pillar", axis="y")
    b.fill(4, 0, z1 + 1, 8, 0, z1 + 3, "smooth_quartz")
    # roof
    b.cornice(x0, z0, x1, z1, 10, "quartz")
    b.fill(x0, 10, z0, x1, 10, z1, "smooth_stone")
    b.walls(x0, z0, x1, z1, 11, 11, "white_concrete")
    # wards
    b.bed(2, 1, 3, "white", "north")
    b.bed(4, 1, 3, "white", "north")
    b.bed(8, 1, 3, "white", "north")
    b.bed(10, 1, 3, "white", "north")
    b.set(6, 1, 2, "brewing_stand")
    b.set(7, 1, 2, "water_cauldron", level=3)
    b.set(5, 1, 2, "chest", facing="south", type="single")
    b.set(2, 1, 8, "potted_dandelion")
    b.set(10, 1, 8, "potted_blue_orchid")
    b.set(9, 1, 8, "potted_lily_of_the_valley")
    b.fill(x0 + 1, 5, z0 + 1, x1 - 1, 5, z1 - 1, "birch_planks")
    for y in range(1, 6):
        b.set(10, y, 6, "ladder", facing="west")
    for x in (2, 4, 6, 8):
        b.bed(x, 6, 3, "white", "north")
    b.lantern(4, 4, 6, hanging=True)
    b.lantern(8, 4, 6, hanging=True)
    b.lantern(6, 9, 6, hanging=True)


# ================================================================================================ military

@design("barracks", 6, 13, [("building_materials", 30), ("doors", 1), ("beds", 3), ("chests", 1), ("crafting_table", 1), ("hay_block", 2)])
def barracks(b):
    ground(b, "gravel")
    x0, z0, x1, z1 = 1, 1, 11, 7
    b.fill(x0, 0, z0, x1, 0, z1, "spruce_planks")
    b.walls(x0, z0, x1, z1, 1, 8, "bricks")
    b.band(x0, z0, x1, z1, 1, "stone_bricks")
    for x in (x0, 6, x1):
        for z in (z0, z1):
            b.fill(x, 1, z, x, 8, z, "stone_bricks")
    for y0 in (2, 6):
        win_row(b, (2, 4, 8, 10), y0, z1, "south", "glass_pane", 2, "stone_brick_slab", "stone_bricks")
        win_row(b, (3, 5, 7, 9), y0, z0, "north", "glass_pane", 2, "stone_brick_slab", "stone_bricks")
        win_col(b, (3, 5), y0, x0, "west", "glass_pane", 2, "stone_brick_slab", "stone_bricks")
        win_col(b, (3, 5), y0, x1, "east", "glass_pane", 2, "stone_brick_slab", "stone_bricks")
    b.air(6, 1, z1, 6, 3, z1)
    b.door(6, 1, z1, "dark_oak", "north")
    b.set(6, 3, z1, "stone_bricks")
    b.set(6, 4, z1, "chiseled_stone_bricks")
    b.gable_roof(x0, z0, x1, z1, 9, "deepslate_tile", "x", overhang=1, gable="bricks")
    b.fill(x0 + 1, 5, z0 + 1, x1 - 1, 5, z1 - 1, "spruce_planks")
    for y in range(1, 6):
        b.set(2, y, 6, "ladder", facing="north")
    # dormitory with bunks
    for x in (3, 5, 8, 10):
        b.bed(x, 6, 3, "green", "north") if x != 5 else b.bed(x, 6, 3, "green", "north")
    b.bed(3, 1, 3, "green", "north")
    b.bed(10, 1, 3, "green", "north")
    b.chest(4, 1, 2, "south")
    b.set(8, 1, 2, "crafting_table")
    b.set(7, 1, 2, "barrel", facing="up", open="false")
    b.lantern(4, 4, 4, hanging=True)
    b.lantern(8, 4, 4, hanging=True)
    # drill yard: flag pole, training dummies, fence
    b.fill(0, 0, 8, 12, 0, 12, "gravel")
    for x in range(0, 13):
        b.set(x, 1, 12, "stone_brick_wall") if x not in (5, 6, 7) else None
    for z in range(9, 12):
        b.set(0, 1, z, "stone_brick_wall")
        b.set(12, 1, z, "stone_brick_wall")
    for y in range(1, 9):
        b.set(2, y, 10, "dark_oak_fence")
    b.fill(3, 7, 10, 5, 8, 10, "red_wool")
    for x in (8, 10):
        b.set(x, 1, 10, "hay_block", axis="y")
        b.set(x, 2, 10, "carved_pumpkin", facing="south")


@design("archery_range", 5, 9, [("building_materials", 24), ("doors", 1), ("target", 2), ("fletching_table", 1), ("hay_block", 4)])
def archery_range(b):
    ground(b)
    # shooting lanes
    for z in range(1, 8):
        for x in (2, 5, 8):
            b.set(x, 0, z, "dirt_path")
    # targets backed by hay
    for x in (2, 5, 8):
        b.set(x, 1, 1, "target")
        b.set(x, 2, 1, "target") if x == 5 else None
        b.set(x, 1, 0, "hay_block", axis="y")
        b.set(x, 2, 0, "hay_block", axis="y")
    b.set(5, 3, 0, "hay_block", axis="x")
    for x in range(0, 11):
        b.set(x, 1, 0, "spruce_fence") if x not in (2, 5, 8) else None
    for z in range(0, 8):
        b.set(0, 1, z, "spruce_fence")
        b.set(10, 1, z, "spruce_fence")
    # shooting shed
    x0, z0, x1, z1 = 1, 7, 9, 10
    b.fill(x0, 0, z0, x1, 0, z1, "spruce_planks")
    b.walls(x0, z0, x1, z1, 1, 3, "bricks")
    b.air(2, 1, z0, 8, 3, z0)
    for x in (x0, 5, x1):
        b.fill(x, 1, z0, x, 3, z0, "stripped_spruce_log", axis="y")
    b.fill(x0, 4, z0, x1, 4, z1, "spruce_planks")
    b.gable_roof(x0, z0, x1, z1, 5, "spruce", "x", overhang=1, gable="bricks")
    b.door(5, 1, z1, "spruce", "north")
    b.window(3, 2, z1, "south", "glass_pane", None, None, 1)
    b.window(7, 2, z1, "south", "glass_pane", None, None, 1)
    b.set(2, 1, 9, "fletching_table")
    b.barrel(8, 1, 9)
    b.set(3, 1, 9, "hay_block", axis="y")
    b.set(4, 1, 8, "spruce_slab", type="top")
    b.lantern(5, 3, 8, hanging=True)


@design("armory", 5, 12, [("masonry", 40), ("doors", 1), ("smithing_table", 1), ("anvils", 1), ("grindstone", 1), ("iron_block", 1)])
def armory(b):
    """Stone arsenal with small barred windows and corner turrets."""
    ground(b, "gravel")
    x0, z0, x1, z1 = 1, 1, 9, 8
    b.fill(x0, 0, z0, x1, 0, z1, "cobblestone")
    b.walls(x0, z0, x1, z1, 1, 7, "stone_bricks")
    b.band(x0, z0, x1, z1, 1, "polished_andesite")
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        b.fill(x, 1, z, x, 9, z, "polished_deepslate")
        b.set(x, 10, z, "deepslate_brick_wall")
    for x in (3, 7):
        b.set(x, 3, z1, "iron_bars")
        b.set(x, 5, z1, "iron_bars")
        b.set(x, 3, z0, "iron_bars")
    for z in (3, 6):
        b.set(x0, 3, z, "iron_bars")
        b.set(x1, 3, z, "iron_bars")
    b.door(5, 1, z1, "spruce", "north")
    b.fill(4, 3, z1, 6, 3, z1, "polished_deepslate")
    b.fill(x0, 8, z0, x1, 8, z1, "stone_bricks")
    for x in range(x0, x1 + 1):
        if x % 2:
            b.set(x, 9, z0, "stone_brick_wall")
            b.set(x, 9, z1, "stone_brick_wall")
    for z in range(z0, z1 + 1):
        if z % 2:
            b.set(x0, 9, z, "stone_brick_wall")
            b.set(x1, 9, z, "stone_brick_wall")
    # forge
    b.set(3, 1, 2, "smithing_table")
    b.set(4, 1, 2, "anvil", facing="east")
    b.set(5, 1, 2, "grindstone", face="floor", facing="south")
    b.set(7, 1, 2, "iron_block")
    b.set(8, 1, 2, "blast_furnace", facing="south")
    b.barrel(2, 1, 6)
    b.barrel(2, 1, 7)
    b.chest(8, 1, 7, "west")
    b.lantern(5, 7, 5, hanging=True)


@design("siege_workshop", 5, 11, [("logs", 24), ("doors", 1), ("crafting_table", 1), ("anvils", 1), ("tnt", 2)])
def siege_workshop(b):
    ground(b, "coarse_dirt")
    x0, z0, x1, z1 = 1, 1, 9, 8
    b.fill(x0, 0, z0, x1, 0, z1, "spruce_planks")
    b.walls(x0, z0, x1, z1, 1, 5, "bricks")
    for x in (x0, 4, 6, x1):
        for z in (z0, z1):
            b.fill(x, 1, z, x, 5, z, "stripped_dark_oak_log", axis="y")
    for z in (z0, 4, z1):
        b.fill(x0, 1, z, x0, 5, z, "stripped_dark_oak_log", axis="y")
        b.fill(x1, 1, z, x1, 5, z, "stripped_dark_oak_log", axis="y")
    for x in range(x0, x1 + 1):
        b.log(x, 5, z0, "stripped_dark_oak_log", "x")
        b.log(x, 5, z1, "stripped_dark_oak_log", "x")
    # garage gate and a small door
    b.air(5, 1, z1, 5, 3, z1)
    b.fill(4, 1, z1, 6, 3, z1, "air")
    b.fill(4, 4, z1, 6, 4, z1, "stripped_dark_oak_log", axis="x")
    b.door(8, 1, z1, "dark_oak", "north")
    b.window(2, 2, z1, "south", "glass_pane", None, None, 2)
    win_col(b, (2, 6), 2, x0, "west", "glass_pane", 2, None)
    b.gable_roof(x0, z0, x1, z1, 6, "dark_oak", "x", overhang=1, gable="bricks")
    # workshop
    b.set(2, 1, 2, "crafting_table")
    b.set(3, 1, 2, "anvil", facing="east")
    b.set(8, 1, 2, "tnt")
    b.set(8, 1, 3, "tnt")
    b.set(8, 2, 2, "tnt")
    b.barrel(7, 1, 2)
    # a battering ram under construction
    for x in range(3, 8):
        b.log(x, 1, 6, "dark_oak_log", "x")
    b.set(2, 1, 6, "iron_block")
    b.lantern(5, 5, 4, hanging=True)
    # timber pile
    for x in range(1, 5):
        b.log(x, 1, 10, "spruce_log", "x")


@design("watchtower", 3, 20, [("building_materials", 40), ("doors", 1), ("ladder", 6), ("lanterns", 1)])
def watchtower(b):
    ground(b, "gravel")
    x0, z0, x1, z1 = 1, 1, 5, 5
    b.fill(x0, 0, z0, x1, 0, z1, "stone_bricks")
    b.walls(x0, z0, x1, z1, 1, 15, "bricks")
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        b.fill(x, 1, z, x, 15, z, "stone_bricks")
    b.door(3, 1, z1, "spruce", "north")
    for y in (5, 9, 13):
        b.window(3, y, z1, "south", "glass_pane", None, None, 2)
        b.window(3, y, z0, "north", "glass_pane", None, None, 2)
        b.window(x0, y, 3, "west", "glass_pane", None, None, 2)
        b.window(x1, y, 3, "east", "glass_pane", None, None, 2)
    for y in range(1, 17):
        b.set(3, y, 2, "ladder", facing="south")
    # lookout
    b.fill(0, 16, 0, 6, 16, 6, "spruce_planks")
    b.set(3, 16, 2, "ladder", facing="south")
    for x in range(0, 7):
        b.set(x, 17, 0, "spruce_fence")
        b.set(x, 17, 6, "spruce_fence")
    for z in range(0, 7):
        b.set(0, 17, z, "spruce_fence")
        b.set(6, 17, z, "spruce_fence")
    for x, z in ((0, 0), (6, 0), (0, 6), (6, 6)):
        b.fill(x, 17, z, x, 18, z, "stripped_spruce_log", axis="y")
    b.gable_roof(0, 0, 6, 6, 19, "deepslate_tile", "x", overhang=0)
    b.lantern(3, 18, 3, hanging=True)


@design("fortress", 6, 13, [("masonry", 80), ("doors", 1), ("iron_bars", 8), ("walls", 10)])
def fortress(b):
    ground(b, "gravel")
    # curtain walls
    b.walls(0, 0, 12, 12, 1, 5, "stone_bricks")
    b.band(0, 0, 12, 12, 1, "polished_andesite")
    for x in range(0, 13):
        if x % 2 == 0:
            b.set(x, 6, 0, "stone_brick_wall")
            b.set(x, 6, 12, "stone_brick_wall")
    for z in range(0, 13):
        if z % 2 == 0:
            b.set(0, 6, z, "stone_brick_wall")
            b.set(12, 6, z, "stone_brick_wall")
    # corner towers
    for cx, cz in ((0, 0), (10, 0), (0, 10), (10, 10)):
        b.fill(cx, 1, cz, cx + 2, 8, cz + 2, "stone_bricks")
        for x in range(cx, cx + 3):
            for z in range(cz, cz + 3):
                if x in (cx, cx + 2) or z in (cz, cz + 2):
                    b.set(x, 9, z, "stone_brick_wall") if (x + z) % 2 == 0 else None
        b.set(cx + 1, 5, cz, "iron_bars") if cz == 0 else b.set(cx + 1, 5, cz + 2, "iron_bars")
        b.set(cx, 5, cz + 1, "iron_bars") if cx == 0 else b.set(cx + 2, 5, cz + 1, "iron_bars")
    # gatehouse
    b.air(5, 1, 12, 7, 4, 12)
    b.fill(5, 4, 12, 7, 4, 12, "stone_bricks")
    for x in (5, 7):
        b.set(x, 1, 12, "stone_bricks")
        b.set(x, 2, 12, "stone_bricks")
        b.set(x, 3, 12, "stone_bricks")
    b.set(6, 3, 12, "iron_bars")
    b.door(6, 1, 12, "dark_oak", "north")
    b.fill(4, 5, 12, 8, 7, 12, "stone_bricks")
    b.set(6, 6, 12, "iron_bars")
    b.set(4, 8, 12, "stone_brick_wall")
    b.set(6, 8, 12, "stone_brick_wall")
    b.set(8, 8, 12, "stone_brick_wall")
    # keep
    k0, k1 = 4, 8
    b.fill(1, 0, 1, 11, 0, 11, "cobblestone")
    b.walls(k0, 3, k1, 8, 1, 9, "stone_bricks")
    b.door(6, 1, 8, "spruce", "north")
    for y in (4, 7):
        b.set(6, y, 8, "iron_bars")
        b.set(k0, y, 5, "iron_bars")
        b.set(k1, y, 5, "iron_bars")
    b.fill(k0, 10, 3, k1, 10, 8, "stone_bricks")
    for x in range(k0, k1 + 1):
        if x % 2 == 0:
            b.set(x, 11, 3, "stone_brick_wall")
            b.set(x, 11, 8, "stone_brick_wall")
    b.set(6, 12, 5, "dark_oak_fence")
    b.lantern(6, 9, 5, hanging=True)
    b.lantern(3, 1, 10)
    b.lantern(9, 1, 10)
    b.set(5, 1, 4, "barrel", facing="up", open="false")


# ================================================================================================ main

def main():
    preview = "--preview" in sys.argv
    os.makedirs(OUT, exist_ok=True)
    if preview:
        os.makedirs(PREVIEW, exist_ok=True)
    metas = []
    ok = True
    for name, fn in DESIGNS.items():
        r, h, reqs = SPEC[name]
        bottom = -8 if name == "mine" else 0
        top = h + 6
        b = new(name, r, top, bottom)
        fn(b)
        # the construction site marker goes to the centre of the ground floor
        c = r
        if b.get(c, 1, c) not in (None, "air"):
            print("WARNING %s: centre is occupied by %s" % (name, b.get(c, 1, c)))
        b.blocks.pop((c, 1, c), None)
        res = count(b, reqs, h)
        bad = {k: v for k, v in res.items() if v[0] < v[1]}
        status = "OK" if not bad else "MISSING " + ", ".join("%s %d/%d" % (k, v[0], v[1]) for k, v in bad.items())
        if bad:
            ok = False
        print("%-15s %2dx%-2d %4d blocks  %s" % (name, b.w, b.d, len(b.blocks), status))
        with open(os.path.join(OUT, name + ".nbt"), "wb") as f:
            f.write(b.nbt())
        floor = -bottom
        tunnel = None
        if b.tunnel:
            tx, ty, tz, td = b.tunnel
            tunnel = (tx, ty + floor, tz, td)
        smoke = [(x, y + floor, z) for (x, y, z) in b.smoke]
        metas.append((name, floor, tunnel, smoke))
        if preview:
            render(b, os.path.join(PREVIEW, name + ".png"))
    write_java(metas)
    if not ok:
        print("Some buildings miss required blocks!")
        sys.exit(1)


def write_java(metas):
    lines = []
    for name, floor, tunnel, smoke in metas:
        t = "null" if tunnel is None else "new int[]{%d, %d, %d, %d}" % tunnel
        s = "new int[][]{%s}" % ", ".join("{%d, %d, %d}" % p for p in smoke)
        lines.append('        META.put("%s", new Meta(%d, %s, %s));' % (name, floor, t, s))
    java = """package com.endruqq.imperium.game;

import java.util.HashMap;
import java.util.Map;

/** Metadata of the ready-made building templates. GENERATED by tools/gen_structures.py - do not edit by hand. */
public final class PrefabTemplates {
    /**
     * @param floor  template layer that lies at the floor level of the plot
     * @param tunnel mine entrance {x, y, z, direction} in template space, or null
     * @param smoke  chimney tops {x, y, z} in template space
     */
    public record Meta(int floor, int[] tunnel, int[][] smoke) {
    }

    private static final Map<String, Meta> META = new HashMap<>();

    static {
%s
    }

    private PrefabTemplates() {}

    public static Meta get(String id) {
        return META.get(id);
    }
}
""" % "\n".join(lines)
    with open(JAVA, "w", encoding="utf-8") as f:
        f.write(java)


if __name__ == "__main__":
    main()
