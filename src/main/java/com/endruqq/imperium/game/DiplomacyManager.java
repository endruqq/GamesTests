package com.endruqq.imperium.game;

import com.endruqq.imperium.ImperiumConfig;
import com.endruqq.imperium.game.tech.Tech;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** Wars, peace treaties, alliances, pacts, trade, access, guarantees, demands, spies, gifts and insults. */
public final class DiplomacyManager {
    public enum Action {
        DECLARE_WAR, PROPOSE_PEACE, PROPOSE_ALLIANCE, BREAK_ALLIANCE, PROPOSE_NAP, BREAK_NAP, PROPOSE_TRADE, CANCEL_TRADE,
        SEND_GIFT, DEMAND_TRIBUTE, INSULT,
        PROPOSE_ACCESS, REVOKE_ACCESS, SEND_FOOD, DENOUNCE, GUARANTEE, REVOKE_GUARANTEE, FABRICATE_CLAIM, DEMAND_PROVINCE,
        CALL_TO_ARMS, PEACE_PROVINCE, PEACE_GOLD;

        public String key() {
            return "imperium.diplo." + name().toLowerCase();
        }

        public static Action byOrdinal(int o) {
            Action[] v = values();
            return v[Math.max(0, Math.min(v.length - 1, o))];
        }
    }

    public static final int GIFT_GOLD = 50;
    public static final int FOOD_AID = 50;
    public static final int TRIBUTE_GOLD = 60;
    public static final int PEACE_GOLD = 100;
    private static final long PROPOSAL_TICKS = 20 * 90;
    private static final long DENOUNCE_COOLDOWN = 20L * 60 * 3;

    private DiplomacyManager() {}

    /** @return null on success, else the reason */
    public static Component perform(MinecraftServer server, GameData d, int fromId, int toId, Action action) {
        Nation from = d.nation(fromId), to = d.nation(toId);
        if (from == null || to == null || from == to) return Component.translatable("imperium.err.no_nation");
        if (from.eliminated || to.eliminated) return Component.translatable("imperium.err.eliminated");
        if (d.phase != GamePhase.RUNNING) return Component.translatable("imperium.err.not_running");
        Relation r = d.relation(fromId, toId);
        switch (action) {
            case DECLARE_WAR -> {
                if (r.type == RelationType.WAR) return Component.translatable("imperium.err.already_war");
                if (d.inGracePeriod()) return Component.translatable("imperium.err.grace", d.graceRemainingTicks() / 20);
                if (r.type == RelationType.ALLIANCE) return Component.translatable("imperium.err.allied");
                if (d.gameTick < r.truceUntil) return Component.translatable("imperium.err.truce", (r.truceUntil - d.gameTick) / 20);
                boolean treachery = r.type == RelationType.NON_AGGRESSION;
                declareWar(server, d, from, to, treachery);
            }
            case PROPOSE_PEACE -> {
                if (r.type != RelationType.WAR) return Component.translatable("imperium.err.not_at_war");
                return propose(server, d, from, to, Proposal.Type.PEACE, 0);
            }
            case PEACE_PROVINCE -> {
                if (r.type != RelationType.WAR) return Component.translatable("imperium.err.not_at_war");
                Province p = provinceToTake(d, from.id, to.id);
                if (p == null) return Component.translatable("imperium.err.no_province");
                return propose(server, d, from, to, Proposal.Type.PEACE_PROVINCE, p.id);
            }
            case PEACE_GOLD -> {
                if (r.type != RelationType.WAR) return Component.translatable("imperium.err.not_at_war");
                if (from.stock.get(ResourceType.GOLD) < PEACE_GOLD) return Component.translatable("imperium.err.cost",
                        Resources.of(ResourceType.GOLD, PEACE_GOLD).describe(1));
                return propose(server, d, from, to, Proposal.Type.PEACE_GOLD, PEACE_GOLD);
            }
            case PROPOSE_ALLIANCE -> {
                if (!from.has(Tech.DIPLOMACY)) return Component.translatable("imperium.err.locked", Tech.DIPLOMACY.title());
                if (r.type == RelationType.WAR || r.type == RelationType.ALLIANCE) return Component.translatable("imperium.err.bad_relation");
                return propose(server, d, from, to, Proposal.Type.ALLIANCE, 0);
            }
            case BREAK_ALLIANCE -> {
                if (r.type != RelationType.ALLIANCE) return Component.translatable("imperium.err.bad_relation");
                setRelation(d, r, RelationType.PEACE);
                r.addOpinion(toId, fromId, -30);
                WarReasons.add(server, d, toId, fromId, CasusBelli.Type.BROKEN_TREATY);
                announce(server, Component.translatable("imperium.msg.alliance_broken", from.name, to.name).withStyle(ChatFormatting.YELLOW));
            }
            case PROPOSE_NAP -> {
                if (!from.has(Tech.DIPLOMACY)) return Component.translatable("imperium.err.locked", Tech.DIPLOMACY.title());
                if (r.type != RelationType.PEACE) return Component.translatable("imperium.err.bad_relation");
                return propose(server, d, from, to, Proposal.Type.NON_AGGRESSION, 0);
            }
            case BREAK_NAP -> {
                if (r.type != RelationType.NON_AGGRESSION) return Component.translatable("imperium.err.bad_relation");
                setRelation(d, r, RelationType.PEACE);
                r.addOpinion(toId, fromId, -15);
                announce(server, Component.translatable("imperium.msg.nap_broken", from.name, to.name).withStyle(ChatFormatting.YELLOW));
            }
            case PROPOSE_TRADE -> {
                if (!from.has(Tech.TRADE)) return Component.translatable("imperium.err.locked", Tech.TRADE.title());
                if (r.type == RelationType.WAR || r.trade) return Component.translatable("imperium.err.bad_relation");
                return propose(server, d, from, to, Proposal.Type.TRADE, 0);
            }
            case CANCEL_TRADE -> {
                if (!r.trade) return Component.translatable("imperium.err.bad_relation");
                r.trade = false;
                r.addOpinion(toId, fromId, -10);
                announce(server, Component.translatable("imperium.msg.trade_cancelled", from.name, to.name).withStyle(ChatFormatting.YELLOW));
            }
            case PROPOSE_ACCESS -> {
                if (r.type == RelationType.WAR || r.access) return Component.translatable("imperium.err.bad_relation");
                return propose(server, d, from, to, Proposal.Type.ACCESS, 0);
            }
            case REVOKE_ACCESS -> {
                if (!r.access) return Component.translatable("imperium.err.bad_relation");
                r.access = false;
                r.addOpinion(toId, fromId, -5);
                announce(server, Component.translatable("imperium.msg.access_revoked", from.name, to.name).withStyle(ChatFormatting.YELLOW));
            }
            case SEND_GIFT -> {
                if (from.stock.get(ResourceType.GOLD) < GIFT_GOLD) return Component.translatable("imperium.err.cost",
                        Resources.of(ResourceType.GOLD, GIFT_GOLD).describe(1));
                from.stock.add(ResourceType.GOLD, -GIFT_GOLD);
                to.stock.add(ResourceType.GOLD, GIFT_GOLD);
                r.addOpinion(toId, fromId, 15);
                Notify.nation(server, d, toId, Component.translatable("imperium.msg.gift_received", from.name, GIFT_GOLD)
                        .withStyle(ChatFormatting.GREEN), false);
                from.addLog(Component.translatable("imperium.log.gift_sent", to.name));
                to.addLog(Component.translatable("imperium.log.gift_received", from.name));
            }
            case SEND_FOOD -> {
                if (from.stock.get(ResourceType.FOOD) < FOOD_AID) return Component.translatable("imperium.err.cost",
                        Resources.of(ResourceType.FOOD, FOOD_AID).describe(1));
                from.stock.add(ResourceType.FOOD, -FOOD_AID);
                to.stock.add(ResourceType.FOOD, FOOD_AID);
                r.addOpinion(toId, fromId, to.famine ? 22 : 10);
                if (to.famine) to.happiness = Math.min(100, to.happiness + 3);
                Notify.nation(server, d, toId, Component.translatable("imperium.msg.food_received", from.name, FOOD_AID)
                        .withStyle(ChatFormatting.GREEN), false);
                from.addLog(Component.translatable("imperium.log.food_sent", to.name));
                to.addLog(Component.translatable("imperium.log.food_received", from.name));
            }
            case DEMAND_TRIBUTE -> {
                if (r.type == RelationType.ALLIANCE) return Component.translatable("imperium.err.allied");
                return propose(server, d, from, to, Proposal.Type.TRIBUTE, TRIBUTE_GOLD);
            }
            case DEMAND_PROVINCE -> {
                if (r.type == RelationType.ALLIANCE) return Component.translatable("imperium.err.allied");
                if (r.type == RelationType.WAR) return Component.translatable("imperium.err.use_peace_terms");
                Province p = provinceToTake(d, from.id, to.id);
                if (p == null) return Component.translatable("imperium.err.no_border");
                return propose(server, d, from, to, Proposal.Type.PROVINCE, p.id);
            }
            case INSULT -> {
                r.addOpinion(toId, fromId, -20);
                WarReasons.add(server, d, toId, fromId, CasusBelli.Type.INSULT);
                int i = server.overworld().random.nextInt(8);
                announce(server, Component.translatable("imperium.insult." + i, from.name, to.name).withStyle(ChatFormatting.LIGHT_PURPLE));
                to.happiness = Math.min(100, to.happiness + 1);
            }
            case DENOUNCE -> {
                if (d.gameTick - r.lastDenounce < DENOUNCE_COOLDOWN) {
                    return Component.translatable("imperium.err.cooldown", (DENOUNCE_COOLDOWN - (d.gameTick - r.lastDenounce)) / 20);
                }
                r.lastDenounce = d.gameTick;
                r.addOpinion(toId, fromId, -25);
                for (Nation o : d.nations.values()) {
                    if (o.id == toId || o.id == fromId || o.eliminated) continue;
                    Relation ro = d.relation(o.id, toId);
                    int weight = d.relation(o.id, fromId).opinionOf(o.id, fromId) >= 0 ? -6 : -2;
                    ro.addOpinion(o.id, toId, weight);
                }
                WarReasons.add(server, d, toId, fromId, CasusBelli.Type.INSULT);
                announce(server, Component.translatable("imperium.msg.denounced", from.name, to.name).withStyle(ChatFormatting.GOLD));
            }
            case GUARANTEE -> {
                if (!from.has(Tech.DIPLOMACY)) return Component.translatable("imperium.err.locked", Tech.DIPLOMACY.title());
                if (r.type == RelationType.WAR || r.guarantees(fromId, toId)) return Component.translatable("imperium.err.bad_relation");
                r.setGuarantee(fromId, toId, true);
                r.addOpinion(toId, fromId, 15);
                announce(server, Component.translatable("imperium.msg.guarantee", from.name, to.name).withStyle(ChatFormatting.AQUA));
            }
            case REVOKE_GUARANTEE -> {
                if (!r.guarantees(fromId, toId)) return Component.translatable("imperium.err.bad_relation");
                r.setGuarantee(fromId, toId, false);
                r.addOpinion(toId, fromId, -15);
                announce(server, Component.translatable("imperium.msg.guarantee_revoked", from.name, to.name).withStyle(ChatFormatting.YELLOW));
            }
            case FABRICATE_CLAIM -> {
                if (!from.has(Tech.ESPIONAGE)) return Component.translatable("imperium.err.locked", Tech.ESPIONAGE.title());
                if (r.type == RelationType.WAR) return Component.translatable("imperium.err.already_war");
                return WarReasons.fabricate(server, d, from, to);
            }
            case CALL_TO_ARMS -> {
                if (r.type != RelationType.ALLIANCE) return Component.translatable("imperium.err.not_allied");
                Nation enemy = null;
                for (Nation e : d.enemiesOf(fromId)) {
                    if (e.id != toId && !d.atWar(toId, e.id) && !d.allied(toId, e.id)) {
                        enemy = e;
                        break;
                    }
                }
                if (enemy == null) return Component.translatable("imperium.err.no_common_war");
                return propose(server, d, from, to, Proposal.Type.CALL_TO_ARMS, enemy.id);
            }
        }
        d.markChanged();
        return null;
    }

    public static void declareWar(MinecraftServer server, GameData d, Nation attacker, Nation defender, boolean treachery) {
        Relation r = d.relation(attacker.id, defender.id);
        List<CasusBelli.Type> reasons = WarReasons.reasons(d, attacker.id, defender.id);
        setRelation(d, r, RelationType.WAR);
        r.trade = false;
        r.access = false;
        r.scoreA = 0;
        r.scoreB = 0;
        r.addOpinion(defender.id, attacker.id, -50);
        d.proposals.removeIf(p -> (p.from == attacker.id && p.to == defender.id) || (p.from == defender.id && p.to == attacker.id));
        announce(server, Component.translatable("imperium.msg.war_declared", attacker.name, defender.name)
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        attacker.addLog(Component.translatable("imperium.log.war_declared_on", defender.name));
        defender.addLog(Component.translatable("imperium.log.war_declared_by", attacker.name));
        if (reasons.isEmpty()) {
            // a war of aggression: nobody likes it, not even our own people
            attacker.happiness = Math.max(0, attacker.happiness - 15);
            for (Nation other : d.nations.values()) {
                if (other.id != attacker.id && !other.eliminated) d.relation(other.id, attacker.id).addOpinion(other.id, attacker.id, -12);
            }
            announce(server, Component.translatable("imperium.msg.aggression", attacker.name, defender.name).withStyle(ChatFormatting.DARK_RED));
        } else {
            attacker.happiness = Math.min(100, attacker.happiness + 5);
            announce(server, Component.translatable("imperium.msg.just_war", attacker.name, reasons.get(0).title()).withStyle(ChatFormatting.GOLD));
        }
        if (treachery) {
            attacker.happiness = Math.max(0, attacker.happiness - 15);
            for (Nation other : d.nations.values()) {
                if (other.id != attacker.id) d.relation(other.id, attacker.id).addOpinion(other.id, attacker.id, -15);
            }
            WarReasons.add(server, d, defender.id, attacker.id, CasusBelli.Type.BROKEN_TREATY);
            announce(server, Component.translatable("imperium.msg.treachery", attacker.name).withStyle(ChatFormatting.DARK_RED));
        }
        // the defender's allies and guarantors join the war
        for (Nation ally : new ArrayList<>(d.nations.values())) {
            if (ally.eliminated || ally.id == attacker.id || ally.id == defender.id) continue;
            Relation gr = d.relations.get(Relation.key(ally.id, defender.id));
            boolean guarantor = gr != null && gr.guarantees(ally.id, defender.id);
            if ((d.allied(ally.id, defender.id) || guarantor) && !d.allied(ally.id, attacker.id) && !d.atWar(ally.id, attacker.id)) {
                Relation ar = d.relation(ally.id, attacker.id);
                setRelation(d, ar, RelationType.WAR);
                ar.trade = false;
                ar.access = false;
                ar.scoreA = 0;
                ar.scoreB = 0;
                announce(server, Component.translatable(guarantor && !d.allied(ally.id, defender.id) ? "imperium.msg.guarantor_joins" : "imperium.msg.ally_joins",
                        ally.name, defender.name, attacker.name).withStyle(ChatFormatting.RED));
            }
        }
        d.markChanged();
    }

    private static void setRelation(GameData d, Relation r, RelationType t) {
        r.type = t;
        r.since = d.gameTick;
    }

    private static Component propose(MinecraftServer server, GameData d, Nation from, Nation to, Proposal.Type type, int amount) {
        for (Proposal p : d.proposals) {
            if (p.from == from.id && p.to == to.id && p.type == type) return Component.translatable("imperium.err.pending");
        }
        Proposal p = new Proposal();
        p.id = d.nextProposalId++;
        p.from = from.id;
        p.to = to.id;
        p.type = type;
        p.amount = amount;
        p.expires = d.gameTick + PROPOSAL_TICKS;
        if (to.bot) {
            boolean accept = BotAI.evaluateProposal(d, to, from, p);
            resolve(server, d, p, accept);
            return null;
        }
        d.proposals.add(p);
        Notify.nation(server, d, to.id, Component.translatable("imperium.msg.proposal_received", from.name,
                describe(d, p)).withStyle(ChatFormatting.AQUA), true);
        Notify.nation(server, d, from.id, Component.translatable("imperium.msg.proposal_sent", to.name,
                describe(d, p)).withStyle(ChatFormatting.GRAY), false);
        d.markChanged();
        return null;
    }

    /** "peace treaty", "the province of X", ... */
    public static Component describe(GameData d, Proposal p) {
        return switch (p.type) {
            case PROVINCE, PEACE_PROVINCE -> {
                Province pr = d.provinces.get(p.amount);
                yield Component.translatable(p.type.key(), pr == null ? "?" : pr.name);
            }
            case CALL_TO_ARMS -> {
                Nation e = d.nation(p.amount);
                yield Component.translatable(p.type.key(), e == null ? "?" : e.name);
            }
            case PEACE_GOLD, TRIBUTE -> Component.translatable(p.type.key(), p.amount);
            default -> Component.translatable(p.type.key());
        };
    }

    /** Bots use this to send proposals to anyone. */
    public static void botPropose(MinecraftServer server, GameData d, Nation from, Nation to, Proposal.Type type) {
        propose(server, d, from, to, type, type == Proposal.Type.TRIBUTE ? TRIBUTE_GOLD : 0);
    }

    public static void respond(ServerPlayer player, GameData d, int proposalId, boolean accept) {
        Proposal p = d.proposal(proposalId);
        Nation n = d.nationOf(player.getUUID());
        if (p == null || n == null || p.to != n.id) return;
        if (n.leader != null && !n.leader.equals(player.getUUID())
                && player.getServer().getPlayerList().getPlayer(n.leader) != null) {
            player.displayClientMessage(Component.translatable("imperium.err.leader_only").withStyle(ChatFormatting.RED), true);
            return;
        }
        d.proposals.remove(p);
        resolve(player.getServer(), d, p, accept);
    }

    public static void resolve(MinecraftServer server, GameData d, Proposal p, boolean accept) {
        Nation from = d.nation(p.from), to = d.nation(p.to);
        if (from == null || to == null) return;
        Relation r = d.relation(p.from, p.to);
        if (!accept) {
            r.addOpinion(p.from, p.to, -5);
            if (p.type == Proposal.Type.TRIBUTE || p.type == Proposal.Type.PROVINCE) {
                r.addOpinion(p.from, p.to, -15);
                WarReasons.add(server, d, p.from, p.to, CasusBelli.Type.REFUSED_DEMAND);
            }
            if (p.type == Proposal.Type.CALL_TO_ARMS) r.addOpinion(p.from, p.to, -10);
            Notify.nation(server, d, from.id, Component.translatable("imperium.msg.proposal_rejected", to.name,
                    describe(d, p)).withStyle(ChatFormatting.RED), true);
            d.markChanged();
            return;
        }
        switch (p.type) {
            case PEACE -> makePeace(server, d, r, from, to);
            case PEACE_PROVINCE -> {
                if (r.type != RelationType.WAR) break;
                makePeace(server, d, r, from, to);
                Province pr = d.provinces.get(p.amount);
                if (pr != null && pr.owner == to.id) WarManager.cede(server.overworld(), d, pr, from.id);
            }
            case PEACE_GOLD -> {
                if (r.type != RelationType.WAR) break;
                double pay = Math.min(p.amount, from.stock.get(ResourceType.GOLD));
                from.stock.add(ResourceType.GOLD, -pay);
                to.stock.add(ResourceType.GOLD, pay);
                makePeace(server, d, r, from, to);
            }
            case ALLIANCE -> {
                if (r.type == RelationType.WAR) break;
                setRelation(d, r, RelationType.ALLIANCE);
                r.addOpinion(p.from, p.to, 20);
                r.addOpinion(p.to, p.from, 20);
                announce(server, Component.translatable("imperium.msg.alliance_signed", from.name, to.name).withStyle(ChatFormatting.GREEN));
            }
            case NON_AGGRESSION -> {
                if (r.type != RelationType.PEACE) break;
                setRelation(d, r, RelationType.NON_AGGRESSION);
                announce(server, Component.translatable("imperium.msg.nap_signed", from.name, to.name).withStyle(ChatFormatting.GREEN));
            }
            case TRADE -> {
                if (r.type == RelationType.WAR) break;
                r.trade = true;
                r.addOpinion(p.from, p.to, 10);
                r.addOpinion(p.to, p.from, 10);
                announce(server, Component.translatable("imperium.msg.trade_signed", from.name, to.name).withStyle(ChatFormatting.GREEN));
            }
            case ACCESS -> {
                if (r.type == RelationType.WAR) break;
                r.access = true;
                r.addOpinion(p.from, p.to, 5);
                announce(server, Component.translatable("imperium.msg.access_signed", from.name, to.name).withStyle(ChatFormatting.GREEN));
            }
            case TRIBUTE -> {
                double pay = Math.min(p.amount, to.stock.get(ResourceType.GOLD));
                to.stock.add(ResourceType.GOLD, -pay);
                from.stock.add(ResourceType.GOLD, pay);
                to.happiness = Math.max(0, to.happiness - 3);
                announce(server, Component.translatable("imperium.msg.tribute_paid", to.name, from.name, (int) pay).withStyle(ChatFormatting.YELLOW));
            }
            case PROVINCE -> {
                Province pr = d.provinces.get(p.amount);
                if (pr == null || pr.owner != to.id || r.type == RelationType.WAR) break;
                to.happiness = Math.max(0, to.happiness - 10);
                WarManager.cede(server.overworld(), d, pr, from.id);
            }
            case CALL_TO_ARMS -> {
                Nation enemy = d.nation(p.amount);
                if (enemy == null || enemy.eliminated || d.atWar(to.id, enemy.id) || d.allied(to.id, enemy.id)) break;
                r.addOpinion(p.from, p.to, 15);
                declareWar(server, d, to, enemy, d.relationType(to.id, enemy.id) == RelationType.NON_AGGRESSION);
            }
        }
        d.markChanged();
    }

    private static void makePeace(MinecraftServer server, GameData d, Relation r, Nation from, Nation to) {
        if (r.type != RelationType.WAR) return;
        setRelation(d, r, RelationType.PEACE);
        r.truceUntil = d.gameTick + ImperiumConfig.TRUCE_MINUTES.get() * 60L * 20L;
        announce(server, Component.translatable("imperium.msg.peace_signed", from.name, to.name).withStyle(ChatFormatting.GREEN));
    }

    /** A province of {@code owner} that borders {@code claimant}: the smallest non-capital first. */
    public static Province provinceToTake(GameData d, int claimant, int owner) {
        List<Province> border = borderProvinces(d, owner, claimant);
        Nation o = d.nation(owner);
        border.sort(Comparator.comparingInt((Province p) -> o != null && o.capitalProvince == p.id ? 1 : 0).thenComparingInt(p -> p.cellCount));
        if (!border.isEmpty()) return border.get(0);
        return null;
    }

    /** Provinces of {@code owner} touching land of {@code neighbour}. */
    public static List<Province> borderProvinces(GameData d, int owner, int neighbour) {
        List<Province> l = new ArrayList<>();
        int n = d.gridN;
        for (int i = 0; i < d.cells.length; i++) {
            Province p = d.provinces.get(d.cells[i]);
            if (p == null || p.owner != owner || l.contains(p)) continue;
            int x = i % n, z = i / n;
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] dd : dirs) {
                int nx = x + dd[0], nz = z + dd[1];
                if (nx < 0 || nz < 0 || nx >= n || nz >= n) continue;
                Province q = d.provinces.get(d.cells[nz * n + nx]);
                if (q != null && q.owner == neighbour) {
                    l.add(p);
                    break;
                }
            }
        }
        return l;
    }

    /** Remove expired proposals. Called every second. */
    public static void tick(MinecraftServer server, GameData d) {
        Iterator<Proposal> it = d.proposals.iterator();
        List<Proposal> expired = new ArrayList<>();
        while (it.hasNext()) {
            Proposal p = it.next();
            if (d.gameTick > p.expires) {
                expired.add(p);
                it.remove();
            }
        }
        for (Proposal p : expired) resolve(server, d, p, false);
        if (!expired.isEmpty()) d.markChanged();
    }

    private static void announce(MinecraftServer server, Component c) {
        Notify.all(server, c, true);
    }
}
