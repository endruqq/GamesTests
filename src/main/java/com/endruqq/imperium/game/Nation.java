package com.endruqq.imperium.game;

import com.endruqq.imperium.game.tech.Tech;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Nation {
    public int id;
    public String name = "Nation";
    public int colorIndex;
    public boolean bot;
    public UUID leader;
    public final Set<UUID> members = new LinkedHashSet<>();
    /** last known names of members, for UI */
    public final List<String> memberNames = new ArrayList<>();

    public Resources stock = new Resources();
    public int population = 12;
    public double happiness = 60;
    public double research = 10;
    public final Set<Tech> techs = EnumSet.noneOf(Tech.class);
    public TaxLevel tax = TaxLevel.NORMAL;
    public int capitalProvince = -1;
    public boolean eliminated;
    public int warCycles;
    public int soldierCount;
    public int citizenCount;
    public int hardshipCycles;
    public long lastProvinceLostTick = -100000;

    // ---- values computed every economy cycle (synced for UI) ----
    public Resources lastDelta = new Resources();
    public double researchDelta;
    public int housing;
    public boolean famine;
    public boolean unpaid;
    public double happinessTarget = 60;

    /** recent events, newest last */
    public final List<Component> log = new ArrayList<>();

    public NationColor color() {
        return NationColor.byIndex(colorIndex);
    }

    public int rgb() {
        return color().rgb;
    }

    public boolean has(Tech t) {
        return techs.contains(t);
    }

    public boolean isMember(UUID id) {
        return members.contains(id);
    }

    public boolean abundant() {
        return !famine && stock.get(ResourceType.FOOD) > population * 8 && happiness > 55;
    }

    /** 0..100 */
    public double morale() {
        double m = happiness;
        if (famine) m -= 25;
        if (unpaid) m -= 15;
        return Math.max(0, Math.min(100, m));
    }

    public void addLog(Component c) {
        log.add(c);
        while (log.size() > 30) log.remove(0);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("name", name);
        t.putInt("color", colorIndex);
        t.putBoolean("bot", bot);
        if (leader != null) t.putUUID("leader", leader);
        ListTag mem = new ListTag();
        for (UUID u : members) {
            CompoundTag m = new CompoundTag();
            m.putUUID("id", u);
            mem.add(m);
        }
        t.put("members", mem);
        ListTag names = new ListTag();
        for (String s : memberNames) names.add(StringTag.valueOf(s));
        t.put("memberNames", names);
        t.put("stock", stock.save());
        t.putInt("pop", population);
        t.putDouble("happy", happiness);
        t.putDouble("research", research);
        ListTag tl = new ListTag();
        for (Tech te : techs) tl.add(StringTag.valueOf(te.name()));
        t.put("techs", tl);
        t.putInt("tax", tax.ordinal());
        t.putInt("capital", capitalProvince);
        t.putBoolean("elim", eliminated);
        t.putInt("warCycles", warCycles);
        t.putInt("soldiers", soldierCount);
        t.putInt("citizens", citizenCount);
        t.putInt("hardship", hardshipCycles);
        t.putLong("lastLost", lastProvinceLostTick);
        t.put("delta", lastDelta.save());
        t.putDouble("rdelta", researchDelta);
        t.putInt("housing", housing);
        t.putBoolean("famine", famine);
        t.putBoolean("unpaid", unpaid);
        t.putDouble("htarget", happinessTarget);
        ListTag lg = new ListTag();
        for (Component c : log) lg.add(StringTag.valueOf(Component.Serializer.toJson(c)));
        t.put("log", lg);
        return t;
    }

    public static Nation load(CompoundTag t) {
        Nation n = new Nation();
        n.id = t.getInt("id");
        n.name = t.getString("name");
        n.colorIndex = t.getInt("color");
        n.bot = t.getBoolean("bot");
        if (t.hasUUID("leader")) n.leader = t.getUUID("leader");
        ListTag mem = t.getList("members", Tag.TAG_COMPOUND);
        for (int i = 0; i < mem.size(); i++) n.members.add(mem.getCompound(i).getUUID("id"));
        ListTag names = t.getList("memberNames", Tag.TAG_STRING);
        for (int i = 0; i < names.size(); i++) n.memberNames.add(names.getString(i));
        n.stock = Resources.load(t.getCompound("stock"));
        n.population = t.getInt("pop");
        n.happiness = t.getDouble("happy");
        n.research = t.getDouble("research");
        ListTag tl = t.getList("techs", Tag.TAG_STRING);
        for (int i = 0; i < tl.size(); i++) {
            try {
                n.techs.add(Tech.valueOf(tl.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        n.tax = TaxLevel.byOrdinal(t.getInt("tax"));
        n.capitalProvince = t.getInt("capital");
        n.eliminated = t.getBoolean("elim");
        n.warCycles = t.getInt("warCycles");
        n.soldierCount = t.getInt("soldiers");
        n.citizenCount = t.getInt("citizens");
        n.hardshipCycles = t.getInt("hardship");
        n.lastProvinceLostTick = t.getLong("lastLost");
        n.lastDelta = Resources.load(t.getCompound("delta"));
        n.researchDelta = t.getDouble("rdelta");
        n.housing = t.getInt("housing");
        n.famine = t.getBoolean("famine");
        n.unpaid = t.getBoolean("unpaid");
        n.happinessTarget = t.getDouble("htarget");
        ListTag lg = t.getList("log", Tag.TAG_STRING);
        for (int i = 0; i < lg.size(); i++) {
            try {
                Component c = Component.Serializer.fromJson(lg.getString(i));
                if (c != null) n.log.add(c);
            } catch (Exception ignored) {
            }
        }
        return n;
    }
}
