package com.endruqq.imperium.game.building;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

/** A placed building (construction site or finished). */
public class Building {
    public enum State {
        CONSTRUCTION, ACTIVE, DAMAGED;

        public String key() {
            return "imperium.bstate." + name().toLowerCase();
        }
    }

    public int id;
    public String typeId;
    public int nation;
    public BlockPos pos;
    public State state = State.CONSTRUCTION;
    public int[] counts = new int[0];
    public long createdTick;
    public int housing;

    public BuildingType type() {
        return Buildings.get(typeId);
    }

    public int radius() {
        BuildingType t = type();
        return t == null ? 3 : t.radius;
    }

    public int height() {
        BuildingType t = type();
        return t == null ? 6 : t.height;
    }

    public int minX() { return pos.getX() - radius(); }
    public int maxX() { return pos.getX() + radius(); }
    public int minZ() { return pos.getZ() - radius(); }
    public int maxZ() { return pos.getZ() + radius(); }
    public int minY() { return pos.getY() - 1; }
    public int maxY() { return pos.getY() + height(); }

    public boolean contains(BlockPos p) {
        return p.getX() >= minX() && p.getX() <= maxX() && p.getZ() >= minZ() && p.getZ() <= maxZ()
                && p.getY() >= minY() - 1 && p.getY() <= maxY() + 1;
    }

    public boolean containsXZ(double x, double z, double margin) {
        return x >= minX() - margin && x < maxX() + 1 + margin && z >= minZ() - margin && z < maxZ() + 1 + margin;
    }

    public AABB box() {
        return new AABB(minX(), minY(), minZ(), maxX() + 1, maxY() + 1, maxZ() + 1);
    }

    public boolean isActive() {
        return state == State.ACTIVE;
    }

    /** number of satisfied requirements, total */
    public int satisfied() {
        BuildingType t = type();
        if (t == null) return 0;
        int s = 0;
        for (int i = 0; i < t.requirements.size() && i < counts.length; i++) {
            if (counts[i] >= t.requirements.get(i).count) s++;
        }
        return s;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("type", typeId);
        t.putInt("nation", nation);
        t.putLong("pos", pos.asLong());
        t.putInt("state", state.ordinal());
        t.putIntArray("counts", counts);
        t.putLong("created", createdTick);
        t.putInt("housing", housing);
        return t;
    }

    public static Building load(CompoundTag t) {
        Building b = new Building();
        b.id = t.getInt("id");
        b.typeId = t.getString("type");
        b.nation = t.getInt("nation");
        b.pos = BlockPos.of(t.getLong("pos"));
        b.state = State.values()[Math.min(State.values().length - 1, t.getInt("state"))];
        b.counts = t.getIntArray("counts");
        b.createdTick = t.getLong("created");
        b.housing = t.getInt("housing");
        return b;
    }
}
