package com.endruqq.imperium.game;

import net.minecraft.nbt.CompoundTag;

/** Symmetric relation between two nations (a < b) plus directed opinions. */
public class Relation {
    public final int a;
    public final int b;
    public RelationType type = RelationType.PEACE;
    public boolean trade;
    public long truceUntil;
    public long since;
    /** opinion of a about b */
    public int opinionAB;
    /** opinion of b about a */
    public int opinionBA;

    public Relation(int a, int b) {
        this.a = Math.min(a, b);
        this.b = Math.max(a, b);
    }

    public static long key(int x, int y) {
        int lo = Math.min(x, y), hi = Math.max(x, y);
        return ((long) lo << 32) | (hi & 0xFFFFFFFFL);
    }

    public int opinionOf(int who, int about) {
        if (who == a && about == b) return opinionAB;
        if (who == b && about == a) return opinionBA;
        return 0;
    }

    public void addOpinion(int who, int about, int delta) {
        if (who == a && about == b) opinionAB = clamp(opinionAB + delta);
        else if (who == b && about == a) opinionBA = clamp(opinionBA + delta);
    }

    private static int clamp(int v) {
        return Math.max(-100, Math.min(100, v));
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("a", a);
        t.putInt("b", b);
        t.putInt("type", type.ordinal());
        t.putBoolean("trade", trade);
        t.putLong("truce", truceUntil);
        t.putLong("since", since);
        t.putInt("oab", opinionAB);
        t.putInt("oba", opinionBA);
        return t;
    }

    public static Relation load(CompoundTag t) {
        Relation r = new Relation(t.getInt("a"), t.getInt("b"));
        r.type = RelationType.values()[Math.min(RelationType.values().length - 1, t.getInt("type"))];
        r.trade = t.getBoolean("trade");
        r.truceUntil = t.getLong("truce");
        r.since = t.getLong("since");
        r.opinionAB = t.getInt("oab");
        r.opinionBA = t.getInt("oba");
        return r;
    }
}
