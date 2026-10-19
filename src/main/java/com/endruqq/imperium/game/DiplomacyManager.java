package com.endruqq.imperium.game;

import com.endruqq.imperium.ImperiumConfig;
import com.endruqq.imperium.game.tech.Tech;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Wars, peace treaties, alliances, pacts, trade, gifts and insults. */
public final class DiplomacyManager {
    public enum Action {
        DECLARE_WAR, PROPOSE_PEACE, PROPOSE_ALLIANCE, BREAK_ALLIANCE, PROPOSE_NAP, BREAK_NAP, PROPOSE_TRADE, CANCEL_TRADE,
        SEND_GIFT, DEMAND_TRIBUTE, INSULT;

        public String key() {
            return "imperium.diplo." + name().toLowerCase();
        }

        public static Action byOrdinal(int o) {
            Action[] v = values();
            return v[Math.max(0, Math.min(v.length - 1, o))];
        }
    }

    public static final int GIFT_GOLD = 50;
    public static final int TRIBUTE_GOLD = 60;
    private static final long PROPOSAL_TICKS = 20 * 90;

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
            case PROPOSE_ALLIANCE -> {
                if (!from.has(Tech.DIPLOMACY)) return Component.translatable("imperium.err.locked", Tech.DIPLOMACY.title());
                if (r.type == RelationType.WAR || r.type == RelationType.ALLIANCE) return Component.translatable("imperium.err.bad_relation");
                return propose(server, d, from, to, Proposal.Type.ALLIANCE, 0);
            }
            case BREAK_ALLIANCE -> {
                if (r.type != RelationType.ALLIANCE) return Component.translatable("imperium.err.bad_relation");
                setRelation(d, r, RelationType.PEACE);
                r.addOpinion(toId, fromId, -30);
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
            case DEMAND_TRIBUTE -> {
                if (r.type == RelationType.ALLIANCE) return Component.translatable("imperium.err.allied");
                return propose(server, d, from, to, Proposal.Type.TRIBUTE, TRIBUTE_GOLD);
            }
            case INSULT -> {
                r.addOpinion(toId, fromId, -20);
                int i = server.overworld().random.nextInt(8);
                announce(server, Component.translatable("imperium.insult." + i, from.name, to.name).withStyle(ChatFormatting.LIGHT_PURPLE));
                to.happiness = Math.min(100, to.happiness + 1);
            }
        }
        d.markChanged();
        return null;
    }

    public static void declareWar(MinecraftServer server, GameData d, Nation attacker, Nation defender, boolean treachery) {
        Relation r = d.relation(attacker.id, defender.id);
        setRelation(d, r, RelationType.WAR);
        r.trade = false;
        r.addOpinion(defender.id, attacker.id, -50);
        d.proposals.removeIf(p -> (p.from == attacker.id && p.to == defender.id) || (p.from == defender.id && p.to == attacker.id));
        announce(server, Component.translatable("imperium.msg.war_declared", attacker.name, defender.name)
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        attacker.addLog(Component.translatable("imperium.log.war_declared_on", defender.name));
        defender.addLog(Component.translatable("imperium.log.war_declared_by", attacker.name));
        if (treachery) {
            attacker.happiness = Math.max(0, attacker.happiness - 15);
            for (Nation other : d.nations.values()) {
                if (other.id != attacker.id) d.relation(other.id, attacker.id).addOpinion(other.id, attacker.id, -15);
            }
            announce(server, Component.translatable("imperium.msg.treachery", attacker.name).withStyle(ChatFormatting.DARK_RED));
        }
        // the defender's allies join the war
        for (Nation ally : new ArrayList<>(d.nations.values())) {
            if (ally.eliminated || ally.id == attacker.id || ally.id == defender.id) continue;
            if (d.allied(ally.id, defender.id) && !d.allied(ally.id, attacker.id) && !d.atWar(ally.id, attacker.id)) {
                Relation ar = d.relation(ally.id, attacker.id);
                setRelation(d, ar, RelationType.WAR);
                ar.trade = false;
                announce(server, Component.translatable("imperium.msg.ally_joins", ally.name, defender.name, attacker.name)
                        .withStyle(ChatFormatting.RED));
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
                Component.translatable(type.key())).withStyle(ChatFormatting.AQUA), true);
        Notify.nation(server, d, from.id, Component.translatable("imperium.msg.proposal_sent", to.name,
                Component.translatable(type.key())).withStyle(ChatFormatting.GRAY), false);
        d.markChanged();
        return null;
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
            if (p.type == Proposal.Type.TRIBUTE) r.addOpinion(p.from, p.to, -15);
            Notify.nation(server, d, from.id, Component.translatable("imperium.msg.proposal_rejected", to.name,
                    Component.translatable(p.type.key())).withStyle(ChatFormatting.RED), true);
            d.markChanged();
            return;
        }
        switch (p.type) {
            case PEACE -> {
                if (r.type != RelationType.WAR) break;
                setRelation(d, r, RelationType.PEACE);
                r.truceUntil = d.gameTick + ImperiumConfig.TRUCE_MINUTES.get() * 60L * 20L;
                announce(server, Component.translatable("imperium.msg.peace_signed", from.name, to.name).withStyle(ChatFormatting.GREEN));
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
            case TRIBUTE -> {
                double pay = Math.min(p.amount, to.stock.get(ResourceType.GOLD));
                to.stock.add(ResourceType.GOLD, -pay);
                from.stock.add(ResourceType.GOLD, pay);
                to.happiness = Math.max(0, to.happiness - 3);
                announce(server, Component.translatable("imperium.msg.tribute_paid", to.name, from.name, (int) pay).withStyle(ChatFormatting.YELLOW));
            }
        }
        d.markChanged();
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
