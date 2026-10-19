package com.endruqq.imperium.game;

import com.endruqq.imperium.entity.OrderType;
import com.endruqq.imperium.entity.Stance;
import com.endruqq.imperium.entity.UnitType;
import com.endruqq.imperium.game.building.BuildingType;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.game.tech.Tech;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Dispatches requests sent by clients (RTS interface, dialogs, editor). */
public final class ActionHandler {
    private ActionHandler() {}

    public static void handle(ServerPlayer player, String action, CompoundTag t) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        GameData d = GameData.get(server);
        ServerLevel level = server.overworld();
        Nation n = d.nationOf(player.getUUID());
        try {
            switch (action) {
                case "rts" -> Sync.setRtsMode(player, t.getBoolean("active"));
                case "place_building" -> {
                    BuildingType type = Buildings.get(t.getString("type"));
                    if (type == null || player.level() != level) return;
                    BlockPos pos = BlockPos.of(t.getLong("pos"));
                    if (pos.distSqr(player.blockPosition()) > 400 * 400) return;
                    pos = new BlockPos(pos.getX(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            pos.getX(), pos.getZ()), pos.getZ());
                    BuildingManager.place(level, player, type, pos, t.getBoolean("prefab"), Direction.from2DDataValue(t.getInt("facing")));
                }
                case "demolish" -> BuildingManager.demolish(level, player, t.getInt("id"));
                case "research" -> {
                    Tech tech = Tech.byId(t.getString("tech"));
                    if (n == null || tech == null) return;
                    if (!tech.available(n)) return;
                    if (n.research < tech.cost) {
                        player.displayClientMessage(Component.translatable("imperium.err.research_points", tech.cost).withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    n.research -= tech.cost;
                    n.techs.add(tech);
                    n.addLog(Component.translatable("imperium.log.researched", tech.title()));
                    Notify.nation(server, d, n.id, Component.translatable("imperium.msg.researched", tech.title()).withStyle(ChatFormatting.AQUA), true);
                    d.markChanged();
                }
                case "recruit" -> {
                    if (n == null) return;
                    UnitType type = UnitType.byOrdinal(t.getInt("unit"));
                    MilitaryManager.recruit(level, d, n, type, Math.max(1, Math.min(10, t.getInt("count"))), player);
                }
                case "command" -> {
                    if (n == null) return;
                    OrderType order = OrderType.byOrdinal(t.getInt("order"));
                    Vec3 target = new Vec3(t.getDouble("x"), t.getDouble("y"), t.getDouble("z"));
                    MilitaryManager.command(level, d, n, t.getIntArray("ids"), order, target, t.getInt("entity"), t.getInt("province"), player);
                }
                case "patrol" -> {
                    if (n == null) return;
                    java.util.List<Vec3> pts = new java.util.ArrayList<>();
                    net.minecraft.nbt.ListTag l = t.getList("points", net.minecraft.nbt.Tag.TAG_COMPOUND);
                    for (int i = 0; i < l.size() && i < 8; i++) {
                        CompoundTag pt = l.getCompound(i);
                        pts.add(new Vec3(pt.getDouble("x"), pt.getDouble("y"), pt.getDouble("z")));
                    }
                    MilitaryManager.patrol(level, d, n, t.getIntArray("ids"), pts);
                }
                case "people" -> {
                    if (n == null) return;
                    com.endruqq.imperium.network.ImperiumNet.sendTo(player,
                            new com.endruqq.imperium.network.ClientActionPacket("people", PeopleManager.listFor(d, n)));
                }
                case "jobs" -> {
                    if (n == null) return;
                    String op = t.getString("op");
                    BuildingType type = Buildings.get(t.getString("type"));
                    Component err = null;
                    switch (op) {
                        case "hire" -> err = type == null ? null : PeopleManager.hire(d, n, type);
                        case "fire" -> {
                            if (type != null) PeopleManager.fire(d, n, type);
                        }
                        case "auto" -> {
                            int hired = PeopleManager.autoAssign(d, n);
                            player.displayClientMessage(Component.translatable("imperium.msg.auto_hired", hired).withStyle(ChatFormatting.GREEN), true);
                        }
                        case "toggle_auto" -> n.autoJobs = !n.autoJobs;
                        case "fire_person" -> {
                            Person p = d.people.get(t.getUUID("id"));
                            if (p != null && p.nation == n.id) PeopleManager.unassign(d, p);
                            PeopleManager.recountWorkers(d);
                        }
                        default -> {
                        }
                    }
                    if (err != null) player.displayClientMessage(err.copy().withStyle(ChatFormatting.RED), true);
                    PeopleManager.stats(d, n);
                    d.markChanged();
                    com.endruqq.imperium.network.ImperiumNet.sendTo(player,
                            new com.endruqq.imperium.network.ClientActionPacket("people", PeopleManager.listFor(d, n)));
                }
                case "borders" -> {
                    if (n == null) return;
                    if (n.leader != null && !n.leader.equals(player.getUUID()) && server.getPlayerList().getPlayer(n.leader) != null) {
                        player.displayClientMessage(Component.translatable("imperium.err.leader_only").withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    n.bordersClosed = !n.bordersClosed;
                    n.addLog(Component.translatable(n.bordersClosed ? "imperium.log.borders_closed" : "imperium.log.borders_opened"));
                    Notify.all(server, Component.translatable(n.bordersClosed ? "imperium.msg.borders_closed" : "imperium.msg.borders_opened", n.name)
                            .withStyle(n.bordersClosed ? ChatFormatting.RED : ChatFormatting.GREEN), false);
                    d.markChanged();
                }
                case "stance" -> {
                    if (n == null) return;
                    MilitaryManager.setStance(level, n, t.getIntArray("ids"), Stance.byOrdinal(t.getInt("stance")));
                }
                case "dismiss" -> {
                    if (n == null) return;
                    MilitaryManager.dismiss(level, d, n, t.getIntArray("ids"));
                }
                case "diplomacy" -> {
                    if (n == null) return;
                    if (n.leader != null && !n.leader.equals(player.getUUID()) && server.getPlayerList().getPlayer(n.leader) != null) {
                        player.displayClientMessage(Component.translatable("imperium.err.leader_only").withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    Component err = DiplomacyManager.perform(server, d, n.id, t.getInt("target"),
                            DiplomacyManager.Action.byOrdinal(t.getInt("action")));
                    if (err != null) player.displayClientMessage(err.copy().withStyle(ChatFormatting.RED), true);
                }
                case "proposal" -> DiplomacyManager.respond(player, d, t.getInt("id"), t.getBoolean("accept"));
                case "tax" -> {
                    if (n == null) return;
                    n.tax = TaxLevel.byOrdinal(t.getInt("level"));
                    d.markChanged();
                }
                case "join" -> GameManager.join(server, d, player, t.getInt("nation"));
                case "leave" -> {
                    if (d.phase != GamePhase.RUNNING || player.hasPermissions(2)) GameManager.leave(d, player);
                }
                case "dialog" -> DialogManager.choice(player, t.getInt("entity"), t.getString("option"));
                case "withdraw" -> {
                    ResourceType rt = ResourceType.byId(t.getString("resource"));
                    if (rt != null) Treasury.withdraw(player, BlockPos.of(t.getLong("pos")), rt, t.getInt("amount"));
                }
                case "editor" -> {
                    if (!player.hasPermissions(2)) {
                        player.displayClientMessage(Component.translatable("imperium.err.no_permission").withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    Editor.handle(player, d, t);
                }
                default -> {
                }
            }
        } catch (Exception e) {
            com.endruqq.imperium.Imperium.LOGGER.error("Failed to handle action {}", action, e);
        }
    }
}
