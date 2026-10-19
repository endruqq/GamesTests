package com.endruqq.imperium.game;

import com.endruqq.imperium.entity.Profession;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The life of one citizen: job, family, grief, accidents. Kept in the game data so that it survives while the citizen's
 * chunk is unloaded; the citizen entity mirrors it.
 */
public class Person {
    public UUID id;
    public String name = "";
    public boolean female;
    public int nation = -1;
    public int personality;
    public Profession job = Profession.PEASANT;
    /** building id of the workplace, -1 if unemployed */
    public int workplace = -1;
    public BlockPos home;
    public long bornTick;

    // ---- family ----
    public UUID spouse;
    public String spouseName = "";
    public long marriedSince;
    public int children;

    // ---- grief ----
    public long mourningUntil;
    public String mourningFor = "";
    /** cause of death of the mourned person (lava, trapped, killed, fall, starvation, accident, drowned, other) */
    public String mourningCause = "";

    // ---- accidents ----
    public boolean trapped;
    /** 0 = behind rubble (dig the rubble away), 1 = lost in a cave (somebody has to find them) */
    public int trapKind;
    public long trappedSince;
    public final List<BlockPos> rubble = new ArrayList<>();
    public long lastAccidentTick = -100000;

    public boolean married() {
        return spouse != null;
    }

    public boolean mourning(long now) {
        return now < mourningUntil;
    }

    /** Can this person go to work today? */
    public boolean ableToWork(long now) {
        return !trapped && !mourning(now);
    }

    public String firstName() {
        int i = name.indexOf(' ');
        return i < 0 ? name : name.substring(0, i);
    }

    public String surname() {
        int i = name.indexOf(' ');
        return i < 0 ? "" : name.substring(i + 1);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putString("name", name);
        t.putBoolean("female", female);
        t.putInt("nation", nation);
        t.putInt("pers", personality);
        t.putInt("job", job.ordinal());
        t.putInt("work", workplace);
        if (home != null) t.putLong("home", home.asLong());
        t.putLong("born", bornTick);
        if (spouse != null) t.putUUID("spouse", spouse);
        t.putString("spouseName", spouseName);
        t.putLong("married", marriedSince);
        t.putInt("children", children);
        t.putLong("mourning", mourningUntil);
        t.putString("mFor", mourningFor);
        t.putString("mCause", mourningCause);
        t.putBoolean("trapped", trapped);
        t.putInt("trapKind", trapKind);
        t.putLong("trappedSince", trappedSince);
        ListTag r = new ListTag();
        for (BlockPos p : rubble) r.add(LongTag.valueOf(p.asLong()));
        t.put("rubble", r);
        t.putLong("accident", lastAccidentTick);
        return t;
    }

    public static Person load(CompoundTag t) {
        Person p = new Person();
        p.id = t.getUUID("id");
        p.name = t.getString("name");
        p.female = t.getBoolean("female");
        p.nation = t.getInt("nation");
        p.personality = t.getInt("pers");
        p.job = Profession.byOrdinal(t.getInt("job"));
        p.workplace = t.contains("work") ? t.getInt("work") : -1;
        if (t.contains("home")) p.home = BlockPos.of(t.getLong("home"));
        p.bornTick = t.getLong("born");
        if (t.hasUUID("spouse")) p.spouse = t.getUUID("spouse");
        p.spouseName = t.getString("spouseName");
        p.marriedSince = t.getLong("married");
        p.children = t.getInt("children");
        p.mourningUntil = t.getLong("mourning");
        p.mourningFor = t.getString("mFor");
        p.mourningCause = t.getString("mCause");
        p.trapped = t.getBoolean("trapped");
        p.trapKind = t.getInt("trapKind");
        p.trappedSince = t.getLong("trappedSince");
        ListTag r = t.getList("rubble", Tag.TAG_LONG);
        for (int i = 0; i < r.size(); i++) p.rubble.add(BlockPos.of(((LongTag) r.get(i)).getAsLong()));
        p.lastAccidentTick = t.getLong("accident");
        return p;
    }
}
