package com.endruqq.imperium.game;

import com.endruqq.imperium.entity.CitizenEntity;
import com.endruqq.imperium.entity.OrderType;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.Stance;
import com.endruqq.imperium.game.building.Building;
import com.endruqq.imperium.game.building.BuildingType;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.network.ClientActionPacket;
import com.endruqq.imperium.network.ImperiumNet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Conversations with NPCs. Lines depend on the state of the speaker's nation: famine, abundance, war,
 * overcrowding, happiness... Text lives in the language files, this class picks keys and arguments.
 */
public final class DialogManager {
    private DialogManager() {}

    // ------------------------------------------------------------------ moods

    public static List<String> moods(GameData d, Nation n) {
        List<String> m = new ArrayList<>();
        if (n == null) {
            m.add("normal");
            return m;
        }
        if (n.famine) m.add("famine");
        if (d.atWarWithAnyone(n.id)) m.add("war");
        if (!n.famine && n.lastDelta.get(ResourceType.FOOD) < 0 && n.stock.get(ResourceType.FOOD) < n.population * 4) m.add("hungry");
        if (n.population > n.housing) m.add("homeless");
        if (n.happiness < 30) m.add("unhappy");
        if (n.abundant()) m.add("abundant");
        if (n.happiness > 70) m.add("happy");
        m.add("normal");
        return m;
    }

    private static String pickMood(List<String> moods, RandomSource r, String topic) {
        List<String> usable = new ArrayList<>();
        for (String m : moods) if (DialogLines.count("imperium.dlg." + topic + "." + m) > 0) usable.add(m);
        if (usable.isEmpty()) return "normal";
        if (usable.size() > 1 && r.nextInt(10) < 3) return usable.get(1);
        return usable.get(0);
    }

    private static Component line(RandomSource r, String base, Object... args) {
        int c = DialogLines.count(base);
        if (c <= 0) return Component.literal("...");
        return Component.translatable(base + "." + r.nextInt(c), args);
    }

    // ------------------------------------------------------------------ packets

    private static void send(ServerPlayer p, Entity speaker, String name, Component subtitle, int nation, String mood,
                             Component text, Map<String, Component> options) {
        CompoundTag t = new CompoundTag();
        t.putInt("entity", speaker.getId());
        t.putString("name", name);
        t.putString("subtitle", Component.Serializer.toJson(subtitle));
        t.putInt("nation", nation);
        t.putString("mood", mood);
        t.putString("line", Component.Serializer.toJson(text));
        ListTag opts = new ListTag();
        for (Map.Entry<String, Component> e : options.entrySet()) {
            CompoundTag o = new CompoundTag();
            o.putString("id", e.getKey());
            o.putString("text", Component.Serializer.toJson(e.getValue()));
            opts.add(o);
        }
        t.put("options", opts);
        ImperiumNet.sendTo(p, new ClientActionPacket("dialog", t));
    }

    private static void close(ServerPlayer p) {
        ImperiumNet.sendTo(p, new ClientActionPacket("dialog_close", new CompoundTag()));
    }

    private static Component opt(String id) {
        return Component.translatable("imperium.dlgopt." + id);
    }

    // ------------------------------------------------------------------ citizens

    private record Ctx(GameData d, Nation own, Nation playerNation, Person person, String mood, Object[] args) {
    }

    private static Ctx citizenCtx(ServerPlayer p, CitizenEntity c, RandomSource r, String topic) {
        GameData d = GameData.get(p.getServer());
        Nation own = d.nation(c.getNationId());
        Nation pn = d.nationOf(p.getUUID());
        Person person = PeopleManager.get(d, c.getUUID());
        String mood;
        if (own != null && pn != null && own.id != pn.id) mood = d.atWar(own.id, pn.id) ? "enemy" : "foreign";
        else if (person != null && person.trapped) mood = "trapped";
        else if (person != null && person.mourning(d.gameTick)) mood = "mourning";
        else mood = pickMood(moods(d, own), r, topic);
        String other = "-";
        List<Nation> enemies = own == null ? List.of() : d.enemiesOf(own.id);
        if (!enemies.isEmpty()) other = enemies.get(r.nextInt(enemies.size())).name;
        else {
            List<Nation> all = new ArrayList<>();
            for (Nation n : d.activeNations()) if (own == null || n.id != own.id) all.add(n);
            if (!all.isEmpty()) other = all.get(r.nextInt(all.size())).name;
        }
        if (person != null && (mood.equals("mourning") || mood.equals("trapped"))) other = mood.equals("mourning") ? person.mourningFor : person.spouseName;
        Province prov = d.provinceAt(c.blockPosition());
        Object[] args = {p.getGameProfile().getName(), own == null ? "-" : own.name, other, c.getCitizenName(),
                prov == null ? "-" : prov.name, ""};
        return new Ctx(d, own, pn, person, mood, args);
    }

    private static Map<String, Component> citizenOptions(ServerPlayer p, Ctx ctx) {
        Map<String, Component> o = new LinkedHashMap<>();
        boolean same = ctx.own != null && ctx.playerNation != null && ctx.own.id == ctx.playerNation.id;
        boolean enemy = "enemy".equals(ctx.mood);
        boolean sad = "mourning".equals(ctx.mood) || "trapped".equals(ctx.mood);
        o.put("how", opt("how"));
        o.put("news", opt("news"));
        if (!enemy && !sad) o.put("joke", opt("joke"));
        o.put("work", opt("work"));
        if (!enemy) o.put("family", opt("family"));
        if (same) {
            if (!"trapped".equals(ctx.mood)) o.put("jobs", opt("jobs"));
            o.put("ruler", opt("ruler"));
            if (!sad) o.put("recruit", opt("recruit"));
        }
        if (p.getMainHandItem().isEdible()) o.put("gift", opt("gift"));
        if (enemy) o.put("threaten", opt("threaten"));
        o.put("bye", opt("bye"));
        return o;
    }

    /** The job menu: free workplaces of the nation. */
    private static Map<String, Component> jobOptions(Ctx ctx) {
        Map<String, Component> o = new LinkedHashMap<>();
        if (ctx.own != null) {
            Map<String, int[]> free = new LinkedHashMap<>();
            for (Building b : ctx.d.buildings.values()) {
                BuildingType t = b.type();
                if (b.nation != ctx.own.id || !b.isActive() || t == null || !t.isWorkplace()) continue;
                int f = PeopleManager.freeSlots(ctx.d, b);
                if (ctx.person != null && ctx.person.workplace == b.id) continue;
                free.computeIfAbsent(t.id, k -> new int[1])[0] += f;
            }
            for (Map.Entry<String, int[]> e : free.entrySet()) {
                if (e.getValue()[0] <= 0 || o.size() >= 7) continue;
                BuildingType t = Buildings.get(e.getKey());
                o.put("job:" + e.getKey(), Component.translatable("imperium.dlgopt.job_at", t.profession.title(), t.name(), e.getValue()[0]));
            }
        }
        if (ctx.person != null && ctx.person.workplace >= 0) o.put("job:quit", opt("job_quit"));
        o.put("back", opt("back"));
        return o;
    }

    private static Component subtitle(GameData d, CitizenEntity c) {
        Nation n = d.nation(c.getNationId());
        return Component.translatable("imperium.dlg.subtitle", c.getProfession().title(), n == null ? "-" : n.name);
    }

    public static void openCitizenDialog(ServerPlayer p, CitizenEntity c) {
        RandomSource r = p.getRandom();
        Ctx ctx = citizenCtx(p, c, r, "greet");
        Component text = line(r, "imperium.dlg.greet." + ctx.mood, ctx.args);
        send(p, c, c.getCitizenName(), subtitle(ctx.d, c), c.getNationId(), ctx.mood, text, citizenOptions(p, ctx));
    }

    private static void citizenChoice(ServerPlayer p, CitizenEntity c, String option) {
        RandomSource r = p.getRandom();
        Ctx ctx = citizenCtx(p, c, r, option);
        GameData d = ctx.d;
        Component text;
        Map<String, Component> next = null;
        if (option.startsWith("job:")) {
            text = job(c, ctx, r, option.substring(4));
        } else {
            switch (option) {
                case "how" -> text = how(r, ctx);
                case "joke" -> text = line(r, "imperium.dlg.joke." + pickMood(moods(d, ctx.own), r, "joke"), ctx.args);
                case "work" -> text = line(r, "imperium.dlg.work." + c.getProfession().id(), ctx.args);
                case "news" -> text = news(r, d, ctx);
                case "family" -> text = family(r, d, ctx);
                case "jobs" -> {
                    text = line(r, "imperium.dlg.jobs.ask", ctx.args);
                    next = jobOptions(ctx);
                }
                case "back" -> text = line(r, "imperium.dlg.greet." + ("enemy".equals(ctx.mood) || "foreign".equals(ctx.mood) ? ctx.mood : "normal"), ctx.args);
                case "ruler" -> {
                    double h = ctx.own == null ? 50 : ctx.own.happiness;
                    text = line(r, "imperium.dlg.ruler." + (h > 65 ? "love" : h > 35 ? "ok" : "hate"), ctx.args);
                }
                case "recruit" -> text = recruit(p, c, ctx, r);
                case "gift" -> text = gift(p, c, ctx, r);
                case "threaten" -> text = threaten(p, c, ctx, r);
                default -> {
                    close(p);
                    p.displayClientMessage(Component.literal(c.getCitizenName() + ": ").append(line(r, "imperium.dlg.bye", ctx.args)), true);
                    return;
                }
            }
        }
        if (!c.isAlive() || c.isRemoved()) {
            close(p);
            p.sendSystemMessage(Component.literal("<" + c.getCitizenName() + "> ").append(text));
            return;
        }
        Ctx after = citizenCtx(p, c, r, "greet");
        send(p, c, c.getCitizenName(), subtitle(d, c), c.getNationId(), after.mood, text, next != null ? next : citizenOptions(p, after));
    }

    private static Component how(RandomSource r, Ctx ctx) {
        if ("mourning".equals(ctx.mood) && ctx.person != null) {
            String cause = ctx.person.mourningCause.isEmpty() ? "other" : ctx.person.mourningCause;
            String key = "imperium.dlg.how.mourning." + cause;
            return line(r, DialogLines.count(key) > 0 ? key : "imperium.dlg.how.mourning.other", ctx.args);
        }
        return line(r, "imperium.dlg.how." + ctx.mood, ctx.args);
    }

    private static Component family(RandomSource r, GameData d, Ctx ctx) {
        Person p = ctx.person;
        Object[] args = ctx.args.clone();
        if (p == null) return line(r, "imperium.dlg.family.single", args);
        if ("trapped".equals(ctx.mood)) return line(r, "imperium.dlg.family.trapped", args);
        if (p.mourning(d.gameTick)) {
            args[2] = p.mourningFor;
            return line(r, "imperium.dlg.family.mourning", args);
        }
        if (p.married()) {
            args[2] = p.spouseName;
            Person s = d.people.get(p.spouse);
            if (s != null && s.trapped) return line(r, "imperium.dlg.family.spouse_trapped", args);
            if (p.children > 0 && r.nextInt(3) == 0) {
                args[5] = String.valueOf(p.children);
                return line(r, "imperium.dlg.family.children", args);
            }
            if (s != null && s.job == com.endruqq.imperium.entity.Profession.MINER) {
                return line(r, p.female ? "imperium.dlg.family.miner_wife" : "imperium.dlg.family.miner_husband", args);
            }
            return line(r, p.female ? "imperium.dlg.family.married_f" : "imperium.dlg.family.married_m", args);
        }
        if (!p.spouseName.isEmpty()) {
            args[2] = p.spouseName;
            return line(r, "imperium.dlg.family.widowed", args);
        }
        return line(r, p.female ? "imperium.dlg.family.single_f" : "imperium.dlg.family.single", args);
    }

    private static Component job(CitizenEntity c, Ctx ctx, RandomSource r, String what) {
        Person p = ctx.person;
        GameData d = ctx.d;
        if (p == null || ctx.own == null || ctx.playerNation == null || ctx.own.id != ctx.playerNation.id) {
            return line(r, "imperium.dlg.recruit.foreign", ctx.args);
        }
        if (what.equals("quit")) {
            PeopleManager.unassign(d, p);
            PeopleManager.recountWorkers(d);
            d.markChanged();
            return line(r, "imperium.dlg.jobs.quit", ctx.args);
        }
        if (p.mourning(d.gameTick)) {
            Object[] args = ctx.args.clone();
            args[2] = p.mourningFor;
            return line(r, "imperium.dlg.jobs.refuse_mourning", args);
        }
        BuildingType t = Buildings.get(what);
        if (t == null) return line(r, "imperium.dlg.jobs.none", ctx.args);
        if (c.getPersonality() == 1 && r.nextInt(4) == 0) return line(r, "imperium.dlg.jobs.refuse_grumpy", ctx.args);
        Building b = PeopleManager.findWorkplace(d, p, t);
        if (b == null) return line(r, "imperium.dlg.jobs.none", ctx.args);
        Component err = PeopleManager.assign(d, p, b);
        if (err != null) return err;
        Object[] args = ctx.args.clone();
        args[4] = t.name();
        String key = "imperium.dlg.jobs.accept." + t.profession.id();
        return line(r, DialogLines.count(key) > 0 ? key : "imperium.dlg.jobs.accept.any", args);
    }

    private static Component news(RandomSource r, GameData d, Ctx ctx) {
        Nation own = ctx.own;
        // what everybody talks about: accidents, weddings, rescues
        if (own != null && !own.gossip.isEmpty() && r.nextInt(10) < 6) {
            List<Gossip> fresh = new ArrayList<>();
            String me = ctx.person == null ? "" : ctx.person.name;
            for (Gossip g : own.gossip) {
                if (d.gameTick - g.tick < 20 * 60 * 30 && !g.who.equals(me) && !g.other.equals(me)) fresh.add(g);
            }
            if (!fresh.isEmpty()) {
                Gossip g = fresh.get(Math.max(0, fresh.size() - 1 - r.nextInt(Math.min(3, fresh.size()))));
                String key = "imperium.dlg.news.gossip." + g.type;
                if (DialogLines.count(key) > 0) {
                    Object[] args = {ctx.args[0], ctx.args[1], g.who, ctx.args[3], g.place.isEmpty() ? ctx.args[4] : g.place, g.other};
                    return line(r, key, args);
                }
            }
        }
        List<String> kinds = new ArrayList<>();
        if (own != null) {
            if (d.atWarWithAnyone(own.id)) kinds.add("war");
            if (own.famine) kinds.add("famine");
            if (own.stock.get(ResourceType.GOLD) > 300) kinds.add("rich");
            if (d.gameTick - own.lastProvinceLostTick < 20 * 60 * 10) kinds.add("lost");
            for (Nation n : d.nations.values()) if (d.allied(own.id, n.id)) {
                kinds.add("ally");
                break;
            }
            if (!d.buildingsOf(own.id).isEmpty()) kinds.add("building");
            if (own.bordersClosed) kinds.add("borders");
            if (own.prosperity) kinds.add("prosperity");
        }
        kinds.add("strongest");
        if (kinds.size() == 1 || (own != null && !d.atWarWithAnyone(own.id))) kinds.add("peace");
        String kind = kinds.get(r.nextInt(kinds.size()));
        Object[] args = ctx.args.clone();
        if (kind.equals("strongest")) {
            Nation best = null;
            for (Nation n : d.activeNations()) if (best == null || BotAI.strength(d, n) > BotAI.strength(d, best)) best = n;
            args[2] = best == null ? "-" : best.name;
        } else if (kind.equals("ally") && own != null) {
            for (Nation n : d.nations.values()) if (d.allied(own.id, n.id)) args[2] = n.name;
        } else if (kind.equals("building") && own != null) {
            List<Building> bl = d.buildingsOf(own.id);
            Building b = bl.get(r.nextInt(bl.size()));
            if (b.type() != null) args[4] = b.type().name();
        }
        return line(r, "imperium.dlg.news." + kind, args);
    }

    private static Component recruit(ServerPlayer p, CitizenEntity c, Ctx ctx, RandomSource r) {
        Nation n = ctx.own;
        if (n == null || ctx.playerNation == null || n.id != ctx.playerNation.id) return line(r, "imperium.dlg.recruit.foreign", ctx.args);
        if (!ctx.d.hasActive(n.id, Buildings.BARRACKS.id)) return line(r, "imperium.dlg.recruit.nobarracks", ctx.args);
        if (n.soldierCount >= MilitaryManager.maxArmy(n)) return line(r, "imperium.dlg.recruit.full", ctx.args);
        double chance = 0.45;
        if (n.famine) chance = 0.85;
        else if (ctx.d.atWarWithAnyone(n.id)) chance = 0.7;
        else if (n.happiness > 70) chance = 0.3;
        if (c.getPersonality() == 2) chance += 0.15;
        if (c.getPersonality() == 1) chance -= 0.15;
        // family people would rather stay home
        if (ctx.person != null && ctx.person.married()) chance -= 0.2;
        String moodKey = n.famine ? "famine" : ctx.d.atWarWithAnyone(n.id) ? "war" : n.happiness > 70 ? "happy" : "normal";
        if (ctx.person != null && ctx.person.married() && r.nextDouble() >= chance) {
            Object[] args = ctx.args.clone();
            args[2] = ctx.person.spouseName;
            return line(r, "imperium.dlg.recruit.no.married", args);
        }
        if (r.nextDouble() < chance) {
            Component text = line(r, "imperium.dlg.recruit.yes." + (DialogLines.count("imperium.dlg.recruit.yes." + moodKey) > 0 ? moodKey : "normal"), ctx.args);
            MilitaryManager.conscriptCitizen((ServerLevel) c.level(), ctx.d, n, c);
            return text;
        }
        return line(r, "imperium.dlg.recruit.no." + (DialogLines.count("imperium.dlg.recruit.no." + moodKey) > 0 ? moodKey : "normal"), ctx.args);
    }

    private static Component gift(ServerPlayer p, CitizenEntity c, Ctx ctx, RandomSource r) {
        ItemStack held = p.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.isEdible()) return line(r, "imperium.dlg.how.normal", ctx.args);
        held.shrink(1);
        Nation n = ctx.own;
        String key;
        if (n != null && ctx.playerNation != null && n.id != ctx.playerNation.id) {
            ctx.d.relation(n.id, ctx.playerNation.id).addOpinion(n.id, ctx.playerNation.id, 2);
            key = "foreign";
        } else if ("mourning".equals(ctx.mood)) {
            key = "mourning";
        } else if (n != null && n.famine) {
            n.happiness = Math.min(100, n.happiness + 3);
            key = "famine";
        } else if (n != null && n.abundant()) {
            key = "abundant";
        } else {
            if (n != null) n.happiness = Math.min(100, n.happiness + 1);
            key = "normal";
        }
        c.heal(4);
        ctx.d.markChanged();
        return line(r, "imperium.dlg.gift." + (DialogLines.count("imperium.dlg.gift." + key) > 0 ? key : "normal"), ctx.args);
    }

    private static Component threaten(ServerPlayer p, CitizenEntity c, Ctx ctx, RandomSource r) {
        if (r.nextBoolean()) {
            ItemStack loot = switch (r.nextInt(4)) {
                case 0 -> new ItemStack(Items.BREAD, 1 + r.nextInt(3));
                case 1 -> new ItemStack(Items.GOLD_NUGGET, 2 + r.nextInt(6));
                case 2 -> new ItemStack(Items.EMERALD, 1);
                default -> new ItemStack(Items.POTATO, 2 + r.nextInt(3));
            };
            if (!p.getInventory().add(loot)) p.drop(loot, false);
            return line(r, "imperium.dlg.threaten.give", ctx.args);
        }
        Vec3 away = DefaultRandomPos.getPosAway(c, 16, 7, p.position());
        if (away != null) c.getNavigation().moveTo(away.x, away.y, away.z, 0.8D);
        return line(r, "imperium.dlg.threaten.flee", ctx.args);
    }

    // ------------------------------------------------------------------ soldiers

    private static String soldierMood(GameData d, Nation n) {
        if (n == null) return "normal";
        if (n.famine) return "famine";
        if (n.unpaid) return "unpaid";
        if (d.atWarWithAnyone(n.id)) return "war";
        if (n.happiness > 70) return "happy";
        return "normal";
    }

    private static Map<String, Component> soldierOptions(boolean own, SoldierEntity s) {
        Map<String, Component> o = new LinkedHashMap<>();
        if (own) {
            o.put("follow", opt("follow"));
            o.put("hold", opt("hold"));
            o.put("return", opt("return"));
            o.put("stance", Component.translatable("imperium.dlgopt.stance", Component.translatable(s.getStance().key())));
            o.put("morale", opt("morale"));
            o.put("equipment", opt("equipment"));
            o.put("dismiss", opt("dismiss"));
        } else {
            o.put("how", opt("how"));
        }
        o.put("bye", opt("bye"));
        return o;
    }

    private static Component soldierSubtitle(GameData d, SoldierEntity s) {
        Nation n = d.nation(s.getNationId());
        return Component.translatable("imperium.dlg.subtitle", s.getUnitType().title(), n == null ? "-" : n.name);
    }

    public static void openSoldierDialog(ServerPlayer p, SoldierEntity s) {
        GameData d = GameData.get(p.getServer());
        RandomSource r = p.getRandom();
        Nation n = d.nation(s.getNationId());
        Nation pn = d.nationOf(p.getUUID());
        boolean own = n != null && pn != null && n.id == pn.id;
        Object[] args = {p.getGameProfile().getName(), n == null ? "-" : n.name, pn == null ? "-" : pn.name, s.getSoldierName(), ""};
        String mood;
        Component text;
        if (own) {
            mood = soldierMood(d, n);
            text = line(r, "imperium.dlg.soldier.greet." + mood, args);
        } else {
            mood = n != null && pn != null && d.atWar(n.id, pn.id) ? "enemy" : "foreign";
            text = line(r, "imperium.dlg.soldier." + mood, args);
        }
        send(p, s, s.getSoldierName(), soldierSubtitle(d, s), s.getNationId(), mood, text, soldierOptions(own, s));
    }

    private static void soldierChoice(ServerPlayer p, SoldierEntity s, String option) {
        GameData d = GameData.get(p.getServer());
        RandomSource r = p.getRandom();
        Nation n = d.nation(s.getNationId());
        Nation pn = d.nationOf(p.getUUID());
        boolean own = n != null && pn != null && n.id == pn.id;
        Object[] args = {p.getGameProfile().getName(), n == null ? "-" : n.name, pn == null ? "-" : pn.name, s.getSoldierName(), ""};
        String mood = own ? soldierMood(d, n) : "foreign";
        Component text;
        if (!own) {
            if (option.equals("how")) {
                String m = n != null && pn != null && d.atWar(n.id, pn.id) ? "enemy" : "foreign";
                text = line(r, "imperium.dlg.soldier." + m, args);
                send(p, s, s.getSoldierName(), soldierSubtitle(d, s), s.getNationId(), m, text, soldierOptions(false, s));
            } else {
                close(p);
            }
            return;
        }
        switch (option) {
            case "follow" -> {
                s.order(OrderType.FOLLOW, s.position(), p.getUUID(), -1);
                text = line(r, "imperium.dlg.soldier.follow", args);
            }
            case "hold" -> {
                s.hold(s.position());
                text = line(r, "imperium.dlg.soldier.hold", args);
            }
            case "return" -> {
                s.order(OrderType.RETURN, s.position(), null, -1);
                text = line(r, "imperium.dlg.soldier.return", args);
            }
            case "stance" -> {
                Stance next = Stance.byOrdinal((s.getStance().ordinal() + 1) % Stance.values().length);
                s.setStance(next);
                text = Component.translatable("imperium.dlg.soldier.stance", Component.translatable(next.key()));
            }
            case "morale" -> {
                String m = n.famine ? "famine" : n.unpaid ? "unpaid" : n.morale() > 65 ? "high" : n.morale() > 35 ? "ok" : "low";
                text = line(r, "imperium.dlg.soldier.morale." + m, args);
            }
            case "equipment" -> {
                close(p);
                s.openEquipment(p);
                return;
            }
            case "dismiss" -> {
                p.sendSystemMessage(Component.literal("<" + s.getSoldierName() + "> ").append(line(r, "imperium.dlg.soldier.dismiss", args)));
                close(p);
                n.population++;
                s.dismiss();
                return;
            }
            default -> {
                close(p);
                return;
            }
        }
        d.markChanged();
        send(p, s, s.getSoldierName(), soldierSubtitle(d, s), s.getNationId(), mood, text, soldierOptions(true, s));
    }

    // ------------------------------------------------------------------ prisoners of war

    private static Map<String, Component> prisonerOptions(GameData d, SoldierEntity s, boolean captor) {
        Map<String, Component> o = new LinkedHashMap<>();
        if (captor) {
            o.put("p_army", Component.translatable("imperium.dlgopt.prisoner_army", pct(PrisonerManager.chance(d, s, PrisonerManager.Offer.ARMY))));
            o.put("p_work", Component.translatable("imperium.dlgopt.prisoner_work", pct(PrisonerManager.chance(d, s, PrisonerManager.Offer.WORK))));
            o.put("p_bribe", Component.translatable("imperium.dlgopt.prisoner_bribe", PrisonerManager.BRIBE_GOLD,
                    pct(PrisonerManager.chance(d, s, PrisonerManager.Offer.BRIBE))));
            o.put("p_release", opt("prisoner_release"));
        } else {
            o.put("how", opt("how"));
        }
        o.put("bye", opt("bye"));
        return o;
    }

    private static String pct(double c) {
        return (int) Math.round(c * 100) + "%";
    }

    public static void openPrisonerDialog(ServerPlayer p, SoldierEntity s) {
        GameData d = GameData.get(p.getServer());
        RandomSource r = p.getRandom();
        Nation n = d.nation(s.getNationId());
        Nation pn = d.nationOf(p.getUUID());
        boolean captor = pn != null && pn.id == s.getCaptor();
        Object[] args = {p.getGameProfile().getName(), n == null ? "-" : n.name, pn == null ? "-" : pn.name, s.getSoldierName(), ""};
        Component text = line(r, captor ? "imperium.dlg.prisoner.greet" : "imperium.dlg.prisoner.other", args);
        send(p, s, s.getSoldierName(), Component.translatable("imperium.dlg.subtitle_prisoner", s.getUnitType().title(), n == null ? "-" : n.name),
                s.getNationId(), "prisoner", text, prisonerOptions(d, s, captor));
    }

    private static void prisonerChoice(ServerPlayer p, SoldierEntity s, String option) {
        GameData d = GameData.get(p.getServer());
        RandomSource r = p.getRandom();
        Nation n = d.nation(s.getNationId());
        Nation pn = d.nationOf(p.getUUID());
        boolean captor = pn != null && pn.id == s.getCaptor();
        Object[] args = {p.getGameProfile().getName(), n == null ? "-" : n.name, pn == null ? "-" : pn.name, s.getSoldierName(), ""};
        PrisonerManager.Offer offer = switch (option) {
            case "p_army" -> PrisonerManager.Offer.ARMY;
            case "p_work" -> PrisonerManager.Offer.WORK;
            case "p_bribe" -> PrisonerManager.Offer.BRIBE;
            case "p_release" -> PrisonerManager.Offer.RELEASE;
            default -> null;
        };
        if (offer == null || !captor) {
            if (option.equals("how")) {
                send(p, s, s.getSoldierName(), Component.empty(), s.getNationId(), "prisoner",
                        line(r, "imperium.dlg.prisoner.other", args), prisonerOptions(d, s, captor));
            } else {
                close(p);
            }
            return;
        }
        String result = PrisonerManager.offer(p.serverLevel(), d, s, pn, offer, r);
        String key = "imperium.dlg.prisoner." + (result.equals("yes") ? "yes." + (offer == PrisonerManager.Offer.WORK ? "work" : "army") : result);
        Component text = line(r, key, args);
        if (!s.isAlive() || s.isRemoved() || !s.isSurrendered()) {
            close(p);
            p.sendSystemMessage(Component.literal("<" + s.getSoldierName() + "> ").append(text));
            return;
        }
        send(p, s, s.getSoldierName(), Component.translatable("imperium.dlg.subtitle_prisoner", s.getUnitType().title(), n == null ? "-" : n.name),
                s.getNationId(), "prisoner", text, prisonerOptions(d, s, true));
    }

    /** Handles a dialog option picked by the player. */
    public static void choice(ServerPlayer p, int entityId, String option) {
        Entity e = p.serverLevel().getEntity(entityId);
        if (e == null || !e.isAlive() || e.distanceToSqr(p) > 10 * 10) {
            close(p);
            return;
        }
        if (e instanceof CitizenEntity c) citizenChoice(p, c, option);
        else if (e instanceof SoldierEntity s && s.isSurrendered()) prisonerChoice(p, s, option);
        else if (e instanceof SoldierEntity s) soldierChoice(p, s, option);
        else close(p);
    }
}
