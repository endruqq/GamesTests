package com.endruqq.imperium.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Generic server -> client instruction (open a screen, show a dialog, notification...). */
public class ClientActionPacket {
    public final String action;
    public final CompoundTag data;

    public ClientActionPacket(String action, CompoundTag data) {
        this.action = action;
        this.data = data == null ? new CompoundTag() : data;
    }

    public ClientActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readUtf(64);
        CompoundTag t = buf.readNbt();
        this.data = t == null ? new CompoundTag() : t;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(action, 64);
        buf.writeNbt(data);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.endruqq.imperium.client.ClientPacketHandler.onAction(this));
    }
}
