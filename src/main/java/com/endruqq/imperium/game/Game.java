package com.endruqq.imperium.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Server-side access helpers. */
public final class Game {
    private Game() {}

    /** @return game data for a server level, or null on the client */
    public static GameData data(Level level) {
        if (level instanceof ServerLevel sl) return GameData.get(sl.getServer());
        return null;
    }

    public static GameData data() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : GameData.get(server);
    }

    public static MinecraftServer server() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    public static boolean running(GameData d) {
        return d != null && d.phase == GamePhase.RUNNING;
    }
}
