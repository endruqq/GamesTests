package com.endruqq.imperium.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** A bag of nation resources (wood, stone, ...). */
public class Resources {
    private final double[] values = new double[ResourceType.values().length];

    public Resources() {}

    public static Resources of(Object... pairs) {
        Resources r = new Resources();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            r.set((ResourceType) pairs[i], ((Number) pairs[i + 1]).doubleValue());
        }
        return r;
    }

    public double get(ResourceType t) {
        return values[t.ordinal()];
    }

    public int getInt(ResourceType t) {
        return (int) Math.floor(values[t.ordinal()]);
    }

    public void set(ResourceType t, double v) {
        values[t.ordinal()] = v;
    }

    public void add(ResourceType t, double v) {
        values[t.ordinal()] += v;
    }

    public void add(Resources other, double mult) {
        for (ResourceType t : ResourceType.values()) values[t.ordinal()] += other.get(t) * mult;
    }

    public boolean has(Resources cost) {
        return has(cost, 1.0);
    }

    public boolean has(Resources cost, double mult) {
        for (ResourceType t : ResourceType.values()) {
            if (values[t.ordinal()] + 1e-6 < cost.get(t) * mult) return false;
        }
        return true;
    }

    public void subtract(Resources cost) {
        subtract(cost, 1.0);
    }

    public void subtract(Resources cost, double mult) {
        for (ResourceType t : ResourceType.values()) values[t.ordinal()] -= cost.get(t) * mult;
    }

    public boolean isEmpty() {
        for (double v : values) if (Math.abs(v) > 1e-6) return false;
        return true;
    }

    public Resources copy() {
        Resources r = new Resources();
        System.arraycopy(values, 0, r.values, 0, values.length);
        return r;
    }

    public void clampMin(double min) {
        for (int i = 0; i < values.length; i++) values[i] = Math.max(min, values[i]);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (ResourceType t : ResourceType.values()) tag.putDouble(t.id(), values[t.ordinal()]);
        return tag;
    }

    public static Resources load(CompoundTag tag) {
        Resources r = new Resources();
        for (ResourceType t : ResourceType.values()) r.set(t, tag.getDouble(t.id()));
        return r;
    }

    /** e.g. "40 Wood, 20 Stone" */
    public MutableComponent describe(double mult) {
        MutableComponent c = Component.empty();
        boolean first = true;
        for (ResourceType t : ResourceType.values()) {
            double v = get(t) * mult;
            if (v <= 0) continue;
            if (!first) c.append(", ");
            c.append(Component.literal(String.valueOf((int) Math.ceil(v)) + " ")).append(t.label());
            first = false;
        }
        if (first) c.append(Component.translatable("imperium.free"));
        return c;
    }
}
