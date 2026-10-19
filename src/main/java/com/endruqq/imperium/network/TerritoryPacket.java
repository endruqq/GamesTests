package com.endruqq.imperium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The province-id grid of the arena. */
public class TerritoryPacket {
    public final int gridN;
    public final int[] cells;

    public TerritoryPacket(int gridN, int[] cells) {
        this.gridN = gridN;
        this.cells = cells;
    }

    public TerritoryPacket(FriendlyByteBuf buf) {
        this.gridN = buf.readVarInt();
        int len = buf.readVarInt();
        this.cells = new int[Math.min(len, 1 << 20)];
        int i = 0;
        while (i < cells.length) {
            int value = buf.readVarInt() - 1;
            int run = buf.readVarInt();
            for (int k = 0; k < run && i < cells.length; k++) cells[i++] = value;
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(gridN);
        buf.writeVarInt(cells.length);
        int i = 0;
        while (i < cells.length) {
            int v = cells[i];
            int run = 1;
            while (i + run < cells.length && cells[i + run] == v) run++;
            buf.writeVarInt(v + 1);
            buf.writeVarInt(run);
            i += run;
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.endruqq.imperium.client.ClientPacketHandler.onTerritory(this));
    }
}
