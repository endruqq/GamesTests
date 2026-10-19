package com.endruqq.imperium.game;

import net.minecraft.nbt.CompoundTag;

/** Something people of a nation talk about: a death in the mine, a wedding, a rescue... */
public class Gossip {
    /** death.lava, death.trapped, death.killed, trapped, rescued, marriage, birth, lava, accident, surrender, execution, mercy */
    public String type = "";
    /** main person */
    public String who = "";
    /** second person (spouse, captor nation...) */
    public String other = "";
    /** province or building */
    public String place = "";
    public long tick;

    public static Gossip of(String type, String who, String other, String place, long tick) {
        Gossip g = new Gossip();
        g.type = type;
        g.who = who == null ? "" : who;
        g.other = other == null ? "" : other;
        g.place = place == null ? "" : place;
        g.tick = tick;
        return g;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("type", type);
        t.putString("who", who);
        t.putString("other", other);
        t.putString("place", place);
        t.putLong("tick", tick);
        return t;
    }

    public static Gossip load(CompoundTag t) {
        return of(t.getString("type"), t.getString("who"), t.getString("other"), t.getString("place"), t.getLong("tick"));
    }
}
