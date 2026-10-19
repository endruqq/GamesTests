package com.endruqq.imperium.client;

import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.network.UnitsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/** Client mirror of the game state. */
public final class ClientState {
    public static GameData data = new GameData();
    public static boolean hasData;
    public static List<UnitsPacket.Unit> units = List.of();
    public static int stateVersion;
    public static int territoryVersion;
    public static boolean showBorders = true;
    public static final Deque<Note> NOTES = new ArrayDeque<>();

    public record Note(Component text, long time, boolean important) {
    }

    /** A citizen as listed in the People tab. */
    public record PersonInfo(UUID id, String name, boolean female, int job, String work, String spouse, int children,
                             long mourning, String mourningFor, long trapped) {
    }

    public static List<PersonInfo> people = new ArrayList<>();
    public static int peopleVersion;

    private ClientState() {}

    public static Nation myNation() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        return data.nationOf(mc.player.getUUID());
    }

    public static int myNationId() {
        Nation n = myNation();
        return n == null ? -1 : n.id;
    }

    public static int colorOf(int nationId) {
        Nation n = data.nation(nationId);
        return n == null ? 0xAAAAAA : n.rgb();
    }

    public static float[] rgbOf(int nationId) {
        int c = colorOf(nationId);
        return new float[]{(c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f};
    }

    public static void note(Component c, boolean important) {
        NOTES.addFirst(new Note(c, System.currentTimeMillis(), important));
        while (NOTES.size() > 8) NOTES.removeLast();
    }

    public static boolean isAdmin() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.hasPermissions(2);
    }

    /** Seconds left of the peace period, estimated on the client. */
    public static long graceSeconds() {
        return Mth.floor(data.graceRemainingTicks() / 20.0);
    }

    public static void reset() {
        data = new GameData();
        hasData = false;
        units = List.of();
        people = new ArrayList<>();
        NOTES.clear();
        stateVersion++;
        territoryVersion++;
    }
}
