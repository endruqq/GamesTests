package com.endruqq.imperium.game;

import com.endruqq.imperium.network.ClientActionPacket;
import com.endruqq.imperium.network.ImperiumNet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Chat + on-screen notifications. */
public final class Notify {
    private Notify() {}

    public static void player(ServerPlayer p, Component msg, boolean important) {
        p.sendSystemMessage(msg);
        CompoundTag t = new CompoundTag();
        t.putString("text", Component.Serializer.toJson(msg));
        t.putBoolean("important", important);
        ImperiumNet.sendTo(p, new ClientActionPacket("notify", t));
        if (important) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
        }
    }

    public static void nation(MinecraftServer server, GameData d, int nationId, Component msg, boolean important) {
        Nation n = d.nation(nationId);
        if (n == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (n.isMember(p.getUUID())) player(p, msg, important);
        }
    }

    public static void all(MinecraftServer server, Component msg, boolean important) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) player(p, msg, important);
    }
}
