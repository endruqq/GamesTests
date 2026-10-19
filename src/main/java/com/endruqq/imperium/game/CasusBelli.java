package com.endruqq.imperium.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** A reason to go to war ("casus belli") that one nation holds against another. */
public class CasusBelli {
    public enum Type {
        /** their soldiers or people crossed our closed border */
        BORDER_VIOLATION(15),
        /** they insulted or denounced us */
        INSULT(10),
        /** they broke a pact, alliance or guarantee with us */
        BROKEN_TREATY(30),
        /** they killed prisoners of war */
        ATROCITY(40),
        /** a claim fabricated by our spies */
        CLAIM(30),
        /** they refused our just demand */
        REFUSED_DEMAND(20),
        /** we caught their spies */
        SPY_CAUGHT(20),
        /** they hold a province that used to be ours (computed, never expires) */
        RECONQUEST(0),
        /** they are at war with our ally (computed) */
        DEFEND_ALLY(0),
        /** they attacked a nation we guarantee (computed) */
        GUARANTEE(0);

        public final int minutes;

        Type(int minutes) {
            this.minutes = minutes;
        }

        public String key() {
            return "imperium.cb." + name().toLowerCase();
        }

        public Component title() {
            return Component.translatable(key());
        }

        public boolean computed() {
            return minutes == 0;
        }

        public static Type byOrdinal(int o) {
            Type[] v = values();
            return v[Math.max(0, Math.min(v.length - 1, o))];
        }
    }

    public int holder;
    public int target;
    public Type type;
    /** game tick when it expires (for claims in progress: when the claim is ready) */
    public long expires;

    public static CasusBelli of(int holder, int target, Type type, long expires) {
        CasusBelli c = new CasusBelli();
        c.holder = holder;
        c.target = target;
        c.type = type;
        c.expires = expires;
        return c;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("h", holder);
        t.putInt("t", target);
        t.putInt("type", type.ordinal());
        t.putLong("exp", expires);
        return t;
    }

    public static CasusBelli load(CompoundTag t) {
        return of(t.getInt("h"), t.getInt("t"), Type.byOrdinal(t.getInt("type")), t.getLong("exp"));
    }
}
