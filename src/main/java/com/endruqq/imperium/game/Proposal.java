package com.endruqq.imperium.game;

import net.minecraft.nbt.CompoundTag;

public class Proposal {
    public enum Type {
        PEACE, ALLIANCE, NON_AGGRESSION, TRADE, TRIBUTE;

        public String key() {
            return "imperium.proposal." + name().toLowerCase();
        }
    }

    public int id;
    public int from;
    public int to;
    public Type type;
    public int amount;
    public long expires;

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putInt("from", from);
        t.putInt("to", to);
        t.putInt("type", type.ordinal());
        t.putInt("amount", amount);
        t.putLong("expires", expires);
        return t;
    }

    public static Proposal load(CompoundTag t) {
        Proposal p = new Proposal();
        p.id = t.getInt("id");
        p.from = t.getInt("from");
        p.to = t.getInt("to");
        p.type = Type.values()[Math.min(Type.values().length - 1, t.getInt("type"))];
        p.amount = t.getInt("amount");
        p.expires = t.getLong("expires");
        return p;
    }
}
