package com.endruqq.imperium.network;

import com.endruqq.imperium.game.ActionHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Generic client -> server request ("place building", "declare war", ...). */
public class ActionPacket {
    public final String action;
    public final CompoundTag data;

    public ActionPacket(String action, CompoundTag data) {
        this.action = action;
        this.data = data == null ? new CompoundTag() : data;
    }

    public ActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readUtf(64);
        CompoundTag t = buf.readNbt();
        this.data = t == null ? new CompoundTag() : t;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(action, 64);
        buf.writeNbt(data);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) ActionHandler.handle(player, action, data);
    }
}
