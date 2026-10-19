# -*- coding: utf-8 -*-
"""Tiny voxel building kit for the ready-made buildings: a block grid, NBT structure export, an isometric preview
renderer and a requirement checker that mirrors the Java rules."""
import gzip
import io
import struct

# ------------------------------------------------------------------------------------------------ geometry

DIRS = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
OPP = {"north": "south", "south": "north", "west": "east", "east": "west"}
CW = {"north": "east", "east": "south", "south": "west", "west": "north"}
CCW = {v: k for k, v in CW.items()}


class Build:
    """Local coordinates: x 0..w-1 (west->east), z 0..d-1 (north->south, the entrance is on the south side),
    y = 0 is the floor layer (it replaces the ground), negative y is underground."""

    def __init__(self, name, w, d, top, bottom=0):
        self.name = name
        self.w, self.d = w, d
        self.top, self.bottom = top, bottom
        self.blocks = {}
        self.tunnel = None
        self.smoke = []

    # -------------------------------------------------------------- primitives
    def set(self, x, y, z, block, **props):
        if not (0 <= x < self.w and 0 <= z < self.d and self.bottom <= y <= self.top):
            return
        if not block.startswith("minecraft:"):
            block = "minecraft:" + block
        self.blocks[(x, y, z)] = (block, {k: str(v).lower() for k, v in props.items()})

    def get(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b[0][10:] if b else None

    def clear(self, x, y, z):
        self.set(x, y, z, "air")

    def fill(self, x0, y0, z0, x1, y1, z1, block, **props):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, block, **props)

    def walls(self, x0, z0, x1, z1, y0, y1, block, corner=None, **props):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                for z in (z0, z1):
                    self.set(x, y, z, block, **props)
            for z in range(z0, z1 + 1):
                for x in (x0, x1):
                    self.set(x, y, z, block, **props)
            if corner:
                for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
                    self.set(x, y, z, corner)

    def floor(self, x0, z0, x1, z1, y, block, **props):
        self.fill(x0, y, z0, x1, y, z1, block, **props)

    def air(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, "air")

    # -------------------------------------------------------------- furniture & details
    def stairs(self, x, y, z, mat, facing, half="bottom", shape="straight"):
        self.set(x, y, z, mat + "_stairs", facing=facing, half=half, shape=shape)

    def slab(self, x, y, z, mat, kind="bottom"):
        self.set(x, y, z, mat + "_slab", type=kind)

    def door(self, x, y, z, mat, facing, hinge="left"):
        """facing: direction a person walking in looks (the panel sits on the opposite side of the block)"""
        self.set(x, y, z, mat + "_door", facing=facing, half="lower", hinge=hinge, open="false")
        self.set(x, y + 1, z, mat + "_door", facing=facing, half="upper", hinge=hinge, open="false")

    def bed(self, x, y, z, color, facing):
        """foot at (x, z), head one step towards facing"""
        dx, dz = DIRS[facing]
        self.set(x, y, z, color + "_bed", facing=facing, part="foot")
        self.set(x + dx, y, z + dz, color + "_bed", facing=facing, part="head")

    def lantern(self, x, y, z, hanging=False, soul=False):
        self.set(x, y, z, "soul_lantern" if soul else "lantern", hanging=hanging)

    def wall_torch(self, x, y, z, facing):
        self.set(x, y, z, "wall_torch", facing=facing)

    def trapdoor(self, x, y, z, mat, facing, half="bottom", open=False):
        self.set(x, y, z, mat + "_trapdoor", facing=facing, half=half, open=open)

    def pot(self, x, y, z, plant="red_tulip"):
        self.set(x, y, z, "potted_" + plant)

    def chest(self, x, y, z, facing):
        self.set(x, y, z, "chest", facing=facing, type="single")

    def barrel(self, x, y, z, facing="up"):
        self.set(x, y, z, "barrel", facing=facing, open="false")

    def log(self, x, y, z, mat, axis="y"):
        self.set(x, y, z, mat, axis=axis)

    def lamp_post(self, x, z, y=1, h=3, fence="dark_oak_fence"):
        for i in range(h):
            self.set(x, y + i, z, fence)
        self.lantern(x, y + h, z)

    def fence_line(self, x0, z0, x1, z1, y, mat):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                self.set(x, y, z, mat)

    # -------------------------------------------------------------- architecture
    def window(self, x, y, z, face, glass="glass_pane", sill="smooth_stone_slab", lintel=None, h=2, out=None):
        """A window in a wall block column at (x, z); face = outward direction. Adds a sill below (outside)."""
        for i in range(h):
            self.set(x, y + i, z, glass)
        dx, dz = DIRS[face]
        if sill:
            self.set(x + dx, y - 1, z + dz, sill, type="top") if sill.endswith("_slab") else self.set(x + dx, y - 1, z + dz, sill)
        if lintel:
            self.set(x, y + h, z, lintel)

    def cornice(self, x0, z0, x1, z1, y, mat, corners=None):
        """Upside-down stairs running around the outside of a rectangle (one block out)."""
        for x in range(x0, x1 + 1):
            self.stairs(x, y, z0 - 1, mat, "south", "top")
            self.stairs(x, y, z1 + 1, mat, "north", "top")
        for z in range(z0, z1 + 1):
            self.stairs(x0 - 1, y, z, mat, "east", "top")
            self.stairs(x1 + 1, y, z, mat, "west", "top")
        c = corners or (mat + "s" if mat.endswith("brick") else mat)
        for x, z in ((x0 - 1, z0 - 1), (x1 + 1, z0 - 1), (x0 - 1, z1 + 1), (x1 + 1, z1 + 1)):
            self.stairs(x, y, z, mat, "south" if z < z0 else "north", "top", "outer_left")

    def band(self, x0, z0, x1, z1, y, block, **props):
        """A ring of blocks along the walls of a rectangle."""
        self.walls(x0, z0, x1, z1, y, y, block, **props)

    def gable_roof(self, x0, z0, x1, z1, y, stairs, ridge_axis="x", fill=None, overhang=1, gable=None):
        """Pitched roof. ridge_axis 'x': slopes face north and south. Gable ends are filled with `gable`."""
        if ridge_axis == "x":
            a0, a1 = z0 - overhang, z1 + overhang
            lo, hi = x0 - overhang, x1 + overhang
            layer = 0
            while a0 <= a1:
                for x in range(lo, hi + 1):
                    if a0 == a1:
                        self.slab(x, y + layer, a0, stairs, "bottom")
                    else:
                        self.stairs(x, y + layer, a0, stairs, "south")
                        self.stairs(x, y + layer, a1, stairs, "north")
                        if fill and layer > 0:
                            for z in range(a0 + 1, a1):
                                if lo + overhang <= x <= hi - overhang:
                                    pass
                if gable:
                    for z in range(max(a0 + 1, z0), min(a1, z1 + 1)):
                        self.set(x0, y + layer, z, gable)
                        self.set(x1, y + layer, z, gable)
                a0 += 1
                a1 -= 1
                layer += 1
            return layer
        else:
            a0, a1 = x0 - overhang, x1 + overhang
            lo, hi = z0 - overhang, z1 + overhang
            layer = 0
            while a0 <= a1:
                for z in range(lo, hi + 1):
                    if a0 == a1:
                        self.slab(a0, y + layer, z, stairs, "bottom")
                    else:
                        self.stairs(a0, y + layer, z, stairs, "east")
                        self.stairs(a1, y + layer, z, stairs, "west")
                if gable:
                    for x in range(max(a0 + 1, x0), min(a1, x1 + 1)):
                        self.set(x, y + layer, z0, gable)
                        self.set(x, y + layer, z1, gable)
                a0 += 1
                a1 -= 1
                layer += 1
            return layer

    def chimney(self, x, z, y0, y1, block="bricks", cap="stone_brick_slab", smoke=True):
        for y in range(y0, y1 + 1):
            self.set(x, y, z, block)
        self.set(x, y1 + 1, z, cap, type="bottom")
        if smoke:
            self.smoke.append((x, y1 + 1, z))

    def water_tower(self, cx, cz, y):
        """NYC-style wooden water tank on four legs; (cx, cz) is the centre, y the roof level it stands on."""
        for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            for i in range(3):
                self.set(cx + dx, y + i, cz + dz, "dark_oak_fence")
        self.fill(cx - 1, y + 3, cz - 1, cx + 1, y + 3, cz + 1, "dark_oak_planks")
        for i in range(4, 7):
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    self.set(cx + dx, y + i, cz + dz, "stripped_spruce_wood", axis="y")
            for (dx, dz, f) in ((0, -2, "north"), (0, 2, "south"), (-2, 0, "west"), (2, 0, "east")):
                self.set(cx + dx, y + i, cz + dz, "spruce_trapdoor", facing=OPP[f], half="bottom", open="true")
        self.set(cx, y + 5, cz, "water")
        self.stairs(cx, y + 7, cz - 1, "spruce", "south")
        self.stairs(cx, y + 7, cz + 1, "spruce", "north")
        self.stairs(cx - 1, y + 7, cz, "spruce", "east")
        self.stairs(cx + 1, y + 7, cz, "spruce", "west")
        for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            self.slab(cx + dx, y + 7, cz + dz, "spruce")
        self.set(cx, y + 7, cz, "spruce_planks")
        self.slab(cx, y + 8, cz, "spruce")

    def fire_escape(self, x0, x1, z_wall, face, levels, height=1):
        """Black iron balconies on a wall. face: outward direction (north/south) of the wall at z_wall."""
        dz = DIRS[face][1]
        z = z_wall + dz
        for lv in levels:
            for x in range(x0, x1 + 1):
                self.slab(x, lv, z, "polished_blackstone", "top")
                self.set(x, lv + 1, z + dz, "iron_bars")
            self.set(x0 - 1, lv + 1, z, "iron_bars")
            self.set(x1 + 1, lv + 1, z, "iron_bars")
            self.set(x0 - 1, lv + 1, z + dz, "iron_bars")
            self.set(x1 + 1, lv + 1, z + dz, "iron_bars")
            self.slab(x0 - 1, lv, z, "polished_blackstone", "top")
            self.slab(x1 + 1, lv, z, "polished_blackstone", "top")
        # ladder between the platforms
        lx = x1
        for i in range(len(levels) - 1):
            for y in range(levels[i] + 1, levels[i + 1]):
                self.set(lx, y, z, "ladder", facing=face)

    def fire_escape_x(self, z0, z1, x_wall, face, levels):
        """Same as fire_escape for a wall facing east or west."""
        dx = DIRS[face][0]
        x = x_wall + dx
        for lv in levels:
            for z in range(z0, z1 + 1):
                self.slab(x, lv, z, "polished_blackstone", "top")
                self.set(x + dx, lv + 1, z, "iron_bars")
            for zz in (z0 - 1, z1 + 1):
                self.slab(x, lv, zz, "polished_blackstone", "top")
                self.set(x, lv + 1, zz, "iron_bars")
                self.set(x + dx, lv + 1, zz, "iron_bars")
        lz = z1
        for i in range(len(levels) - 1):
            for y in range(levels[i] + 1, levels[i + 1]):
                self.set(x, y, lz, "ladder", facing=face)

    def awning(self, x0, x1, z_wall, y, face, colors=("red_wool", "white_wool"), depth=2):
        """Striped shop awning sloping away from a south/north wall."""
        dz = DIRS[face][1]
        for i in range(depth):
            for x in range(x0, x1 + 1):
                c = colors[(x - x0) % len(colors)]
                self.set(x, y - i, z_wall + dz * (i + 1), c)

    def awning_x(self, z0, z1, x_wall, y, face, colors=("red_wool", "white_wool"), depth=2):
        dx = DIRS[face][0]
        for i in range(depth):
            for z in range(z0, z1 + 1):
                c = colors[(z - z0) % len(colors)]
                self.set(x_wall + dx * (i + 1), y - i, z, c)

    def ac_unit(self, x, y, z, face):
        """A small box under a window."""
        dx, dz = DIRS[face]
        self.set(x + dx, y, z + dz, "iron_trapdoor", facing=face, half="top", open="false")

    # -------------------------------------------------------------- export
    def nbt(self):
        """gzip-compressed structure NBT (DataVersion 3465 = 1.20.1)"""
        ys = range(self.bottom, self.top + 1)
        size = (self.w, self.top - self.bottom + 1, self.d)
        palette = []
        index = {}
        blocks = []
        for (x, y, z), (name, props) in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            key = (name, tuple(sorted(props.items())))
            if key not in index:
                index[key] = len(palette)
                palette.append(key)
            blocks.append(((x, y - self.bottom, z), index[key]))
        out = io.BytesIO()
        w = NbtWriter(out)
        w.begin_compound("")
        w.int_("DataVersion", 3465)
        w.list_int("size", list(size))
        w.begin_list("palette", 10, len(palette))
        for name, props in palette:
            w.string("Name", name)
            if props:
                w.begin_compound("Properties")
                for k, v in props:
                    w.string(k, v)
                w.end()
            w.end()
        w.begin_list("blocks", 10, len(blocks))
        for (x, y, z), st in blocks:
            w.list_int("pos", [x, y, z])
            w.int_("state", st)
            w.end()
        w.begin_list("entities", 10, 0)
        w.end()
        raw = out.getvalue()
        buf = io.BytesIO()
        with gzip.GzipFile(fileobj=buf, mode="wb", mtime=0) as g:
            g.write(raw)
        return buf.getvalue()


class NbtWriter:
    def __init__(self, out):
        self.o = out

    def _name(self, tag, name):
        self.o.write(struct.pack(">b", tag))
        b = name.encode("utf-8")
        self.o.write(struct.pack(">H", len(b)))
        self.o.write(b)

    def begin_compound(self, name):
        self._name(10, name)

    def end(self):
        self.o.write(b"\x00")

    def int_(self, name, v):
        self._name(3, name)
        self.o.write(struct.pack(">i", v))

    def string(self, name, v):
        self._name(8, name)
        b = v.encode("utf-8")
        self.o.write(struct.pack(">H", len(b)))
        self.o.write(b)

    def list_int(self, name, vals):
        self._name(9, name)
        self.o.write(struct.pack(">bi", 3, len(vals)))
        for v in vals:
            self.o.write(struct.pack(">i", v))

    def begin_list(self, name, elem_tag, n):
        self._name(9, name)
        self.o.write(struct.pack(">bi", elem_tag if n else 0, n))


# ------------------------------------------------------------------------------------------------ requirement check

PLANKS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"}
STONE_BRICKS = {"stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks"}
MASONRY = STONE_BRICKS | {"bricks", "cobblestone", "mossy_cobblestone", "smooth_stone", "polished_andesite", "polished_diorite",
                          "polished_granite", "deepslate_bricks", "cracked_deepslate_bricks", "deepslate_tiles", "cracked_deepslate_tiles",
                          "polished_deepslate", "cobbled_deepslate", "chiseled_deepslate", "cut_sandstone", "smooth_sandstone",
                          "chiseled_sandstone", "cut_red_sandstone", "smooth_red_sandstone", "mud_bricks", "nether_bricks", "red_nether_bricks",
                          "polished_blackstone_bricks", "cracked_polished_blackstone_bricks", "polished_blackstone", "quartz_block",
                          "quartz_bricks", "end_stone_bricks", "prismarine_bricks", "packed_mud"}
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
          "brown", "green", "red", "black"]


def is_log(b):
    return any(b == f"{w}_log" or b == f"stripped_{w}_log" or b == f"{w}_wood" or b == f"stripped_{w}_wood" for w in PLANKS) \
        or b in ("crimson_stem", "warped_stem", "stripped_crimson_stem", "stripped_warped_stem")


def is_planks(b):
    return b.endswith("_planks")


def is_terracotta(b):
    return b == "terracotta" or (b.endswith("_terracotta") and "glazed" not in b)


def building_material(b):
    return is_planks(b) or is_log(b) or b in MASONRY or is_terracotta(b) or b in (
        "white_concrete", "light_gray_concrete", "gray_concrete", "bamboo_block", "bamboo_mosaic")


def stone_block(b):
    return b in MASONRY or b in ("stone", "deepslate", "andesite", "diorite", "granite", "tuff", "calcite")


def glass_pane(b):
    return b == "glass_pane" or b.endswith("_stained_glass_pane")


TESTS = {
    "building_materials": building_material,
    "masonry": lambda b: b in MASONRY,
    "stone_blocks": stone_block,
    "planks": is_planks,
    "logs": is_log,
    "beds": lambda b: b.endswith("_bed"),
    "doors": lambda b: b.endswith("_door") and "trapdoor" not in b,
    "glass_panes": glass_pane,
    "lanterns": lambda b: b in ("lantern", "soul_lantern"),
    "chests": lambda b: b in ("chest", "trapped_chest"),
    "torches": lambda b: b in ("torch", "wall_torch"),
    "flower_pots": lambda b: b == "flower_pot" or b.startswith("potted_"),
    "rails": lambda b: b in ("rail", "powered_rail", "detector_rail", "activator_rail"),
    "anvils": lambda b: b in ("anvil", "chipped_anvil", "damaged_anvil"),
    "cauldrons": lambda b: b.endswith("cauldron"),
    "wool": lambda b: b.endswith("_wool"),
    "candles": lambda b: b == "candle" or (b.endswith("_candle") and "cake" not in b),
    "campfires": lambda b: b in ("campfire", "soul_campfire"),
    "walls": lambda b: b.endswith("_wall") and "sign" not in b and "banner" not in b and "torch" not in b and "fan" not in b and "head" not in b and "skull" not in b,
    "water": lambda b: b == "water",
}


def count(build, reqs, height):
    """reqs: list of (id, n); id is a TESTS key or an exact block name. Counts like the Java check."""
    res = {}
    for rid, n in reqs:
        test = TESTS.get(rid, None)
        c = 0
        for (x, y, z), (name, props) in build.blocks.items():
            if y < 0 or y > height + 1:
                continue
            b = name[10:]
            if props.get("half") == "upper" and b.endswith("_door"):
                continue
            if props.get("part") == "foot":
                continue
            if test(b) if test else b == rid:
                c += 1
        res[rid] = (c, n)
    return res


# ------------------------------------------------------------------------------------------------ preview

COL = {}


def _c(names, rgb):
    for n in names.split():
        COL[n] = rgb


_c("bricks brick", (150, 74, 58))
_c("granite polished_granite", (153, 104, 84))
_c("terracotta", (152, 94, 67))
_c("red_terracotta", (143, 61, 46))
_c("orange_terracotta", (162, 84, 38))
_c("white_terracotta", (209, 178, 161))
_c("light_gray_terracotta", (135, 107, 98))
_c("brown_terracotta", (77, 51, 36))
_c("mud_bricks packed_mud", (137, 104, 79))
_c("stone_bricks stone_brick chiseled_stone_bricks cracked_stone_bricks", (122, 121, 122))
_c("smooth_stone smooth_stone_slab stone", (158, 158, 158))
_c("andesite polished_andesite", (132, 135, 134))
_c("cobblestone cobblestone_wall mossy_cobblestone", (110, 110, 110))
_c("deepslate_tiles deepslate_tile deepslate_bricks deepslate_brick polished_deepslate cobbled_deepslate", (54, 54, 56))
_c("polished_blackstone polished_blackstone_bricks blackstone", (52, 46, 54))
_c("quartz_block quartz_bricks quartz_pillar smooth_quartz quartz chiseled_quartz_block", (234, 228, 220))
_c("calcite white_concrete", (222, 224, 220))
_c("light_gray_concrete", (125, 125, 115))
_c("gray_concrete", (55, 58, 62))
_c("smooth_sandstone cut_sandstone sandstone", (220, 206, 160))
_c("oak_planks oak", (162, 131, 79))
_c("spruce_planks spruce stripped_spruce_wood stripped_spruce_log", (114, 84, 48))
_c("spruce_log", (58, 37, 16))
_c("dark_oak_planks dark_oak dark_oak_fence", (66, 43, 20))
_c("dark_oak_log stripped_dark_oak_log", (60, 46, 26))
_c("oak_log", (109, 85, 50))
_c("birch_planks birch", (192, 175, 121))
_c("glass_pane glass", (170, 210, 230))
_c("light_blue_stained_glass_pane", (120, 170, 220))
_c("gray_stained_glass_pane", (70, 80, 90))
_c("light_gray_stained_glass_pane", (150, 160, 170))
_c("green_stained_glass_pane", (90, 130, 60))
_c("white_stained_glass_pane", (225, 230, 235))
_c("red_wool", (160, 39, 34))
_c("white_wool", (233, 236, 236))
_c("blue_wool", (53, 57, 157))
_c("green_wool", (84, 109, 27))
_c("yellow_wool", (248, 197, 39))
_c("iron_bars iron_block iron_trapdoor iron_door anvil", (160, 160, 165))
_c("chain", (60, 65, 75))
_c("lantern torch wall_torch soul_lantern", (250, 200, 90))
_c("grass_block", (95, 159, 53))
_c("dirt dirt_path farmland coarse_dirt", (121, 85, 58))
_c("water", (63, 118, 228))
_c("hay_block", (186, 153, 35))
_c("barrel composter", (125, 92, 54))
_c("chest crafting_table", (160, 115, 50))
_c("bookshelf", (117, 94, 58))
_c("gold_block", (246, 208, 61))
_c("tnt", (200, 60, 50))
_c("furnace blast_furnace smoker stonecutter", (110, 110, 110))
_c("piston", (150, 130, 100))
_c("red_bed", (180, 40, 40))
_c("white_bed", (230, 230, 230))
_c("blue_bed", (60, 70, 170))
_c("green_bed", (90, 120, 40))
_c("bell", (230, 190, 60))
_c("green_concrete", (73, 91, 36))
_c("lime_terracotta", (103, 117, 53))
_c("cyan_terracotta", (86, 91, 91))
_c("rail", (130, 110, 80))
_c("ladder", (130, 100, 60))
_c("oak_leaves spruce_leaves azalea_leaves", (60, 120, 40))
_c("red_concrete", (142, 33, 33))
_c("black_concrete", (8, 10, 15))
_c("target", (230, 200, 180))
_c("lectern cartography_table fletching_table smithing_table grindstone loom", (150, 110, 70))
_c("brewing_stand cauldron", (80, 80, 80))
_c("campfire", (240, 140, 40))
_c("birch_log", (216, 215, 210))
_c("wheat carrots potatoes beetroots", (110, 150, 40))
_c("pumpkin carved_pumpkin", (198, 118, 24))
_c("melon", (111, 145, 30))
_c("coal_block", (16, 16, 16))
_c("dark_prismarine", (51, 91, 75))
_c("polished_diorite diorite", (192, 193, 194))
_c("gravel", (131, 127, 126))
_c("flower_pot", (124, 68, 53))
_c("lava_cauldron", (210, 90, 20))
_c("water_cauldron", (70, 90, 160))
_c("rose_bush peony", (180, 60, 90))
_c("red_stained_glass_pane", (160, 50, 50))
_c("yellow_stained_glass_pane", (220, 200, 60))


def color_of(name):
    n = name[10:] if name.startswith("minecraft:") else name
    if n in COL:
        return COL[n]
    for suffix in ("_stairs", "_slab", "_wall", "_fence", "_fence_gate", "_trapdoor", "_door", "_pressure_plate", "_button"):
        if n.endswith(suffix):
            base = n[: -len(suffix)]
            for cand in (base, base + "s", base + "_planks", base.replace("_brick", "_bricks"), base.replace("_tile", "_tiles")):
                if cand in COL:
                    return COL[cand]
    if n.startswith("potted_"):
        return (200, 80, 80)
    if n.endswith("_bed"):
        return (180, 40, 40)
    if n.endswith("_carpet"):
        return COL.get(n.replace("_carpet", "_wool"), (200, 200, 200))
    if "candle" in n:
        return (230, 220, 190)
    return (255, 0, 255)


def shape_of(name, props):
    n = name[10:]
    if n == "air":
        return None
    if n.endswith("_slab"):
        t = props.get("type", "bottom")
        return "full" if t == "double" else ("top" if t == "top" else "bottom")
    if n.endswith("_stairs"):
        return "stairs_top" if props.get("half") == "top" else "stairs"
    if n.endswith("_fence") or n.endswith("_wall") or n in ("iron_bars", "chain") or n.endswith("glass_pane"):
        return "post"
    if n.endswith("_trapdoor"):
        return "thin_v" if props.get("open") == "true" else ("top_thin" if props.get("half") == "top" else "thin")
    if n.endswith("_carpet") or n in ("rail", "ladder"):
        return "thin"
    if n in ("lantern", "soul_lantern", "torch", "wall_torch", "bell", "lectern", "brewing_stand") or "candle" in n or n.startswith("potted_"):
        return "small"
    if n.endswith("_door"):
        return "post"
    return "full"


def render(build, path, scale=10):
    from PIL import Image, ImageDraw
    w, d = build.w, build.d
    ys = [p[1] for p in build.blocks]
    if not ys:
        return
    y0, y1 = min(ys), max(ys)
    s = scale
    # isometric: screen x = (x - z) * s, screen y = (x + z) * s/2 - y * s
    W = (w + d) * s + 40
    H = (w + d) * s // 2 + (y1 - y0 + 2) * s + 40
    img = Image.new("RGB", (W, H), (178, 202, 228))
    dr = ImageDraw.Draw(img)
    ox, oy = d * s + 20, (y1 + 1) * s + 20

    def P(x, y, z):
        return (ox + (x - z) * s, oy + (x + z) * s / 2 - y * s)

    def shade(c, f):
        return tuple(max(0, min(255, int(v * f))) for v in c)

    items = []
    for (x, y, z), (name, props) in build.blocks.items():
        sh = shape_of(name, props)
        if sh is None:
            continue
        items.append((x + z, y, x, z, name, props, sh))
    items.sort(key=lambda t: (t[0], t[1]))
    for _, y, x, z, name, props, sh in items:
        c = color_of(name)
        if sh == "full" or sh.startswith("stairs"):
            yb, yt = y, y + 1
        elif sh == "bottom":
            yb, yt = y, y + 0.5
        elif sh == "top":
            yb, yt = y + 0.5, y + 1
        elif sh == "thin":
            yb, yt = y, y + 0.12
        elif sh == "top_thin":
            yb, yt = y + 0.85, y + 1
        else:
            yb, yt = y, y + 1
        inset = 0.0
        if sh in ("post",):
            inset = 0.36
        elif sh == "small":
            inset = 0.3
            yt = y + 0.6
        elif sh == "thin_v":
            inset = 0.0
        x0, x1, z0, z1 = x + inset, x + 1 - inset, z + inset, z + 1 - inset
        top = [P(x0, yt, z0), P(x1, yt, z0), P(x1, yt, z1), P(x0, yt, z1)]
        left = [P(x0, yt, z1), P(x1, yt, z1), P(x1, yb, z1), P(x0, yb, z1)]
        right = [P(x1, yt, z0), P(x1, yt, z1), P(x1, yb, z1), P(x1, yb, z0)]
        dr.polygon(right, fill=shade(c, 0.72), outline=shade(c, 0.55))
        dr.polygon(left, fill=shade(c, 0.86), outline=shade(c, 0.62))
        dr.polygon(top, fill=shade(c, 1.08), outline=shade(c, 0.8))
    img.save(path)
