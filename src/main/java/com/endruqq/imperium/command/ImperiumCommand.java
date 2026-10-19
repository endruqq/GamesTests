package com.endruqq.imperium.command;

import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.GameManager;
import com.endruqq.imperium.game.GamePhase;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.ResourceType;
import com.endruqq.imperium.game.TerritoryGen;
import com.endruqq.imperium.game.tech.Tech;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

public final class ImperiumCommand {
    private ImperiumCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("imperium")
                .then(Commands.literal("help").executes(ImperiumCommand::help))
                .then(Commands.literal("info").executes(ImperiumCommand::info))
                .then(Commands.literal("join")
                        .then(Commands.argument("nation", StringArgumentType.greedyString())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        GameData.get(c.getSource().getServer()).nations.values().stream().map(n -> n.name), b))
                                .executes(ImperiumCommand::join)))
                .then(Commands.literal("leave").executes(c -> {
                    GameData d = GameData.get(c.getSource().getServer());
                    if (d.phase == GamePhase.RUNNING && !c.getSource().hasPermission(2)) {
                        c.getSource().sendFailure(Component.translatable("imperium.err.cannot_switch"));
                        return 0;
                    }
                    GameManager.leave(d, c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("arena").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("size", IntegerArgumentType.integer(100, 3000))
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(GameManager.ARENA_SIZES).mapToObj(String::valueOf), b))
                                .executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    int size = IntegerArgumentType.getInteger(c, "size");
                                    GameData d = GameData.get(c.getSource().getServer());
                                    GameManager.setArena(d, p.getBlockX(), p.getBlockZ(), size);
                                    c.getSource().sendSuccess(() -> Component.translatable("imperium.msg.arena_set", size, p.getBlockX(), p.getBlockZ()), true);
                                    return 1;
                                })))
                .then(Commands.literal("autodivide").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("nations", IntegerArgumentType.integer(2, 15))
                                .then(Commands.argument("provinces", IntegerArgumentType.integer(1, 12))
                                        .executes(c -> {
                                            GameData d = GameData.get(c.getSource().getServer());
                                            if (d.phase == GamePhase.RUNNING) {
                                                c.getSource().sendFailure(Component.translatable("imperium.err.already_running"));
                                                return 0;
                                            }
                                            if (!d.arenaSet) {
                                                ServerPlayer p = c.getSource().getPlayerOrException();
                                                GameManager.setArena(d, p.getBlockX(), p.getBlockZ(), 500);
                                            }
                                            int nations = IntegerArgumentType.getInteger(c, "nations");
                                            int provinces = IntegerArgumentType.getInteger(c, "provinces");
                                            TerritoryGen.autoDivide(d, nations, provinces, System.nanoTime());
                                            c.getSource().sendSuccess(() -> Component.translatable("imperium.msg.autodivided", nations, provinces), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("start").requires(s -> s.hasPermission(2)).executes(c -> {
                    Component err = GameManager.start(c.getSource().getServer(), GameData.get(c.getSource().getServer()));
                    if (err != null) {
                        c.getSource().sendFailure(err);
                        return 0;
                    }
                    return 1;
                }))
                .then(Commands.literal("stop").requires(s -> s.hasPermission(2)).executes(c -> {
                    GameManager.stop(c.getSource().getServer(), GameData.get(c.getSource().getServer()));
                    return 1;
                }))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2)).executes(c -> {
                    GameManager.reset(c.getSource().getServer(), GameData.get(c.getSource().getServer()));
                    return 1;
                }))
                .then(Commands.literal("wipe").requires(s -> s.hasPermission(2)).executes(c -> {
                    GameManager.wipe(c.getSource().getServer(), GameData.get(c.getSource().getServer()));
                    return 1;
                }))
                .then(Commands.literal("nation").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> {
                            GameData d = GameData.get(c.getSource().getServer());
                            Nation n = GameManager.createNation(d, StringArgumentType.getString(c, "name"), -1);
                            c.getSource().sendSuccess(() -> Component.translatable("imperium.msg.nation_created", n.name), true);
                            return 1;
                        }))))
                .then(Commands.literal("give").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("resource", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(ResourceType.values()).map(ResourceType::id), b))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100000)).executes(c -> {
                                    GameData d = GameData.get(c.getSource().getServer());
                                    Nation n = d.nationOf(c.getSource().getPlayerOrException().getUUID());
                                    ResourceType t = ResourceType.byId(StringArgumentType.getString(c, "resource"));
                                    if (n == null || t == null) return 0;
                                    n.stock.add(t, IntegerArgumentType.getInteger(c, "amount"));
                                    d.markChanged();
                                    return 1;
                                }))))
                .then(Commands.literal("grace").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 86400)).executes(c -> {
                            GameData d = GameData.get(c.getSource().getServer());
                            int sec = IntegerArgumentType.getInteger(c, "seconds");
                            d.graceTicks = Math.max(0, d.gameTick - d.startTick) + sec * 20L;
                            d.markChanged();
                            c.getSource().sendSuccess(() -> Component.translatable("imperium.msg.grace_set", sec), true);
                            return 1;
                        })))
                .then(Commands.literal("showcase").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    int n = com.endruqq.imperium.game.Showcase.build(p.serverLevel(), p.blockPosition()).size();
                    c.getSource().sendSuccess(() -> Component.translatable("imperium.msg.showcase", n), true);
                    return n;
                }))
                .then(Commands.literal("research").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("all").executes(c -> {
                            GameData d = GameData.get(c.getSource().getServer());
                            Nation n = d.nationOf(c.getSource().getPlayerOrException().getUUID());
                            if (n == null) return 0;
                            n.techs.addAll(Arrays.asList(Tech.values()));
                            d.markChanged();
                            return 1;
                        }))));
    }

    private static int help(CommandContext<CommandSourceStack> c) {
        for (int i = 0; i < 9; i++) {
            final int k = i;
            c.getSource().sendSuccess(() -> Component.translatable("imperium.help." + k).withStyle(k == 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        GameData d = GameData.get(c.getSource().getServer());
        c.getSource().sendSuccess(() -> Component.translatable("imperium.info.phase", Component.translatable(d.phase.key()),
                d.arenaSet ? d.size : 0, d.nations.size(), d.provinces.size(), d.buildings.size()).withStyle(ChatFormatting.GOLD), false);
        for (Nation n : d.nations.values()) {
            long provinces = d.provinces.values().stream().filter(p -> p.owner == n.id).count();
            c.getSource().sendSuccess(() -> Component.literal(" - " + n.name + (n.bot ? " [BOT]" : "") + (n.eliminated ? " [X]" : "")
                    + "  " + provinces + "  " + String.join(", ", n.memberNames)).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int join(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        GameData d = GameData.get(c.getSource().getServer());
        String name = StringArgumentType.getString(c, "nation");
        for (Nation n : d.nations.values()) {
            if (n.name.equalsIgnoreCase(name)) {
                GameManager.join(c.getSource().getServer(), d, p, n.id);
                return 1;
            }
        }
        c.getSource().sendFailure(Component.translatable("imperium.err.no_such_nation", name));
        return 0;
    }
}
