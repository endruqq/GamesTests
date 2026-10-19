package com.endruqq.imperium.game;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Admin map editor operations. */
public final class Editor {
    private Editor() {}

    public static void handle(ServerPlayer player, GameData d, CompoundTag t) {
        MinecraftServer server = player.getServer();
        String op = t.getString("op");
        Component err = null;
        switch (op) {
            case "arena" -> {
                int size = t.getInt("size");
                boolean here = t.getBoolean("here") || !d.arenaSet;
                int x = here ? player.getBlockX() : d.centerX;
                int z = here ? player.getBlockZ() : d.centerZ;
                boolean keep = d.arenaSet && d.size == size && !here;
                if (!keep) {
                    GameManager.setArena(d, x, z, size);
                    msg(player, Component.translatable("imperium.msg.arena_set", size, x, z));
                }
            }
            case "autodivide" -> {
                if (!d.arenaSet) GameManager.setArena(d, player.getBlockX(), player.getBlockZ(), 500);
                int nations = Math.max(2, Math.min(15, t.getInt("nations")));
                int provinces = Math.max(1, Math.min(12, t.getInt("provinces")));
                if (d.phase == GamePhase.RUNNING) {
                    err = Component.translatable("imperium.err.already_running");
                    break;
                }
                TerritoryGen.autoDivide(d, nations, provinces, t.contains("seed") ? t.getLong("seed") : System.nanoTime());
                msg(player, Component.translatable("imperium.msg.autodivided", nations, provinces));
            }
            case "create_nation" -> {
                Nation n = GameManager.createNation(d, t.getString("name"), -1);
                msg(player, Component.translatable("imperium.msg.nation_created", n.name));
            }
            case "delete_nation" -> GameManager.deleteNation(d, t.getInt("nation"));
            case "rename_nation" -> {
                Nation n = d.nation(t.getInt("nation"));
                String name = t.getString("name").trim();
                if (n != null && !name.isEmpty()) n.name = name.length() > 40 ? name.substring(0, 40) : name;
            }
            case "color" -> {
                Nation n = d.nation(t.getInt("nation"));
                if (n != null) n.colorIndex = (n.colorIndex + 1) % NationColor.values().length;
            }
            case "bot" -> {
                Nation n = d.nation(t.getInt("nation"));
                if (n != null) n.bot = !n.bot;
            }
            case "create_province" -> {
                Province p = GameManager.createProvince(d, t.getInt("nation"));
                CompoundTag reply = new CompoundTag();
                reply.putInt("province", p.id);
                com.endruqq.imperium.network.ImperiumNet.sendTo(player,
                        new com.endruqq.imperium.network.ClientActionPacket("editor_province", reply));
            }
            case "delete_province" -> GameManager.deleteProvince(d, t.getInt("province"));
            case "rename_province" -> {
                Province p = d.provinces.get(t.getInt("province"));
                String name = t.getString("name").trim();
                if (p != null && !name.isEmpty()) p.name = name.length() > 40 ? name.substring(0, 40) : name;
            }
            case "province_owner" -> {
                Province p = d.provinces.get(t.getInt("province"));
                if (p != null) {
                    int owner = t.getInt("nation");
                    if (d.phase == GamePhase.RUNNING && p.core != null) {
                        WarManager.capture(server.overworld(), d, p, owner);
                    } else {
                        p.owner = owner;
                        p.originalOwner = owner;
                    }
                    d.markTerritoryChanged();
                }
            }
            case "capital" -> {
                Province p = d.provinces.get(t.getInt("province"));
                if (p != null && d.nation(p.owner) != null) d.nation(p.owner).capitalProvince = p.id;
            }
            case "paint" -> TerritoryGen.paint(d, t.getInt("province"), t.getIntArray("cells"));
            case "core" -> {
                Province p = d.provinces.get(t.getInt("province"));
                if (p != null && d.phase != GamePhase.RUNNING) {
                    p.core = new BlockPos(t.getInt("x"), 64, t.getInt("z"));
                    msg(player, Component.translatable("imperium.msg.core_set", p.name));
                }
            }
            case "start" -> {
                err = GameManager.start(server, d);
            }
            case "stop" -> GameManager.stop(server, d);
            case "reset" -> GameManager.reset(server, d);
            case "wipe" -> GameManager.wipe(server, d);
            case "assign" -> {
                ServerPlayer target = server.getPlayerList().getPlayerByName(t.getString("player"));
                if (target != null) GameManager.join(server, d, target, t.getInt("nation"));
            }
            case "give" -> {
                Nation n = d.nation(t.getInt("nation"));
                ResourceType rt = ResourceType.byId(t.getString("resource"));
                if (n != null && rt != null) n.stock.add(rt, t.getInt("amount"));
            }
            default -> {
            }
        }
        if (err != null) player.displayClientMessage(err.copy().withStyle(ChatFormatting.RED), false);
        d.markChanged();
    }

    private static void msg(ServerPlayer p, Component c) {
        p.displayClientMessage(c.copy().withStyle(ChatFormatting.GREEN), false);
    }
}
