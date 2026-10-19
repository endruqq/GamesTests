package com.endruqq.imperium.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Full game state (without the territory grid). */
public class StatePacket {
    public final CompoundTag data;

    public StatePacket(CompoundTag data) {
        this.data = data;
    }

    public StatePacket(FriendlyByteBuf buf) {
        CompoundTag t = buf.readAnySizeNbt();
        this.data = t == null ? new CompoundTag() : t;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.endruqq.imperium.client.ClientPacketHandler.onState(this));
    }
}
