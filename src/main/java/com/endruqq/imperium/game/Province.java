package com.endruqq.imperium.game;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class Province {
    public int id;
    public String name = "Province";
    public int owner = -1;
    public int originalOwner = -1;
    public BlockPos core;
    public boolean coreBuilt;
    public double coreHp;
    public double coreMaxHp;
    public long lastAttackTick = -100000;
    public int lastAttacker = -1;

    // ---- derived geometry (recomputed, also synced) ----
    public int cellCount;
    public double centroidX;
    public double centroidZ;

    public boolean underAttack(long now) {
        return now - lastAttackTick < 200;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("name", name);
        t.putInt("owner", owner);
        t.putInt("orig", originalOwner);
        if (core != null) t.putLong("core", core.asLong());
        t.putBoolean("built", coreBuilt);
        t.putDouble("hp", coreHp);
        t.putDouble("maxHp", coreMaxHp);
        t.putLong("lastAtk", lastAttackTick);
        t.putInt("lastAtkBy", lastAttacker);
        t.putInt("cells", cellCount);
        t.putDouble("cx", centroidX);
        t.putDouble("cz", centroidZ);
        return t;
    }

    public static Province load(CompoundTag t) {
        Province p = new Province();
        p.id = t.getInt("id");
        p.name = t.getString("name");
        p.owner = t.getInt("owner");
        p.originalOwner = t.getInt("orig");
        if (t.contains("core")) p.core = BlockPos.of(t.getLong("core"));
        p.coreBuilt = t.getBoolean("built");
        p.coreHp = t.getDouble("hp");
        p.coreMaxHp = t.getDouble("maxHp");
        p.lastAttackTick = t.getLong("lastAtk");
        p.lastAttacker = t.getInt("lastAtkBy");
        p.cellCount = t.getInt("cells");
        p.centroidX = t.getDouble("cx");
        p.centroidZ = t.getDouble("cz");
        return p;
    }
}
