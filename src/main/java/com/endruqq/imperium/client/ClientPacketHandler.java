package com.endruqq.imperium.client;

import com.endruqq.imperium.client.screen.DialogScreen;
import com.endruqq.imperium.client.screen.RtsScreen;
import com.endruqq.imperium.client.screen.TreasuryScreen;
import com.endruqq.imperium.network.ClientActionPacket;
import com.endruqq.imperium.network.StatePacket;
import com.endruqq.imperium.network.TerritoryPacket;
import com.endruqq.imperium.network.UnitsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Arrays;

public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void onState(StatePacket p) {
        int[] cells = ClientState.data.cells;
        ClientState.data.load(p.data, false);
        if (cells.length == ClientState.data.gridN * ClientState.data.gridN) {
            ClientState.data.cells = cells;
        } else {
            ClientState.data.cells = new int[ClientState.data.gridN * ClientState.data.gridN];
            Arrays.fill(ClientState.data.cells, -1);
        }
        ClientState.hasData = true;
        ClientState.stateVersion++;
    }

    public static void onTerritory(TerritoryPacket p) {
        ClientState.data.gridN = p.gridN;
        ClientState.data.cells = p.cells;
        ClientState.data.recomputeGeometry();
        ClientState.territoryVersion++;
    }

    public static void onUnits(UnitsPacket p) {
        ClientState.units = p.units;
    }

    public static void onAction(ClientActionPacket p) {
        Minecraft mc = Minecraft.getInstance();
        switch (p.action) {
            case "notify" -> {
                Component c = Component.Serializer.fromJson(p.data.getString("text"));
                if (c != null) ClientState.note(c, p.data.getBoolean("important"));
            }
            case "dialog" -> {
                if (mc.screen instanceof DialogScreen ds) ds.update(p.data);
                else mc.setScreen(new DialogScreen(p.data, mc.screen instanceof RtsScreen ? mc.screen : null));
            }
            case "dialog_close" -> {
                if (mc.screen instanceof DialogScreen ds) ds.onClose();
            }
            case "open_treasury" -> mc.setScreen(new TreasuryScreen(BlockPos.of(p.data.getLong("pos"))));
            case "editor_province" -> RtsScreen.editorProvince = p.data.getInt("province");
            case "people" -> {
                java.util.List<ClientState.PersonInfo> list = new java.util.ArrayList<>();
                net.minecraft.nbt.ListTag l = p.data.getList("people", net.minecraft.nbt.Tag.TAG_COMPOUND);
                for (int i = 0; i < l.size(); i++) {
                    net.minecraft.nbt.CompoundTag e = l.getCompound(i);
                    list.add(new ClientState.PersonInfo(e.getUUID("id"), e.getString("name"), e.getBoolean("female"), e.getInt("job"),
                            e.getString("work"), e.getString("spouse"), e.getInt("children"), e.getLong("mourning"), e.getString("mourningFor"),
                            e.getLong("trapped")));
                }
                ClientState.people = list;
                ClientState.peopleVersion++;
            }
            default -> {
            }
        }
    }
}
