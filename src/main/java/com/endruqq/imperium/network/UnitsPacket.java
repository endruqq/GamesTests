package com.endruqq.imperium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Positions of units and players for the strategic map. */
public class UnitsPacket {
    public static class Unit {
        public int entityId;
        public int nation;
        /** UnitType ordinal, or -1 for a player */
        public int type;
        public float x;
        public float y;
        public float z;
        public float hp;
        public int order;
        public String name = "";
    }

    public final List<Unit> units;

    public UnitsPacket(List<Unit> units) {
        this.units = units;
    }

    public UnitsPacket(FriendlyByteBuf buf) {
        int n = Math.min(buf.readVarInt(), 20000);
        units = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Unit u = new Unit();
            u.entityId = buf.readVarInt();
            u.nation = buf.readVarInt() - 1;
            u.type = buf.readByte();
            u.x = buf.readFloat();
            u.y = buf.readFloat();
            u.z = buf.readFloat();
            u.hp = buf.readFloat();
            u.order = buf.readByte();
            if (u.type < 0) u.name = buf.readUtf(64);
            units.add(u);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(units.size());
        for (Unit u : units) {
            buf.writeVarInt(u.entityId);
            buf.writeVarInt(u.nation + 1);
            buf.writeByte(u.type);
            buf.writeFloat(u.x);
            buf.writeFloat(u.y);
            buf.writeFloat(u.z);
            buf.writeFloat(u.hp);
            buf.writeByte(u.order);
            if (u.type < 0) buf.writeUtf(u.name, 64);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.endruqq.imperium.client.ClientPacketHandler.onUnits(this));
    }
}
