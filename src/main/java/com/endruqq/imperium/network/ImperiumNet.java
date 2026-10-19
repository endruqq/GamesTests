package com.endruqq.imperium.network;

import com.endruqq.imperium.Imperium;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ImperiumNet {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Imperium.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ImperiumNet() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(ActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActionPacket::encode).decoder(ActionPacket::new).consumerMainThread(ActionPacket::handle).add();
        CHANNEL.messageBuilder(StatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StatePacket::encode).decoder(StatePacket::new).consumerMainThread(StatePacket::handle).add();
        CHANNEL.messageBuilder(TerritoryPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TerritoryPacket::encode).decoder(TerritoryPacket::new).consumerMainThread(TerritoryPacket::handle).add();
        CHANNEL.messageBuilder(UnitsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UnitsPacket::encode).decoder(UnitsPacket::new).consumerMainThread(UnitsPacket::handle).add();
        CHANNEL.messageBuilder(ClientActionPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientActionPacket::encode).decoder(ClientActionPacket::new).consumerMainThread(ClientActionPacket::handle).add();
    }

    public static void sendTo(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToAll(Object msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }
}
