package com.endruqq.imperium.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Loaded soldiers and citizens on the server, for fast lookups. */
public final class UnitRegistry {
    private static final Set<SoldierEntity> SOLDIERS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<CitizenEntity> CITIZENS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private UnitRegistry() {}

    public static void add(SoldierEntity s) {
        SOLDIERS.add(s);
    }

    public static void remove(SoldierEntity s) {
        SOLDIERS.remove(s);
    }

    public static void add(CitizenEntity c) {
        CITIZENS.add(c);
    }

    public static void remove(CitizenEntity c) {
        CITIZENS.remove(c);
    }

    public static List<SoldierEntity> soldiers() {
        List<SoldierEntity> l = new ArrayList<>();
        for (SoldierEntity s : SOLDIERS) if (s.isAlive() && !s.isRemoved()) l.add(s);
        return l;
    }

    public static List<SoldierEntity> soldiersOf(int nation) {
        List<SoldierEntity> l = new ArrayList<>();
        for (SoldierEntity s : SOLDIERS) if (s.isAlive() && !s.isRemoved() && s.getNationId() == nation) l.add(s);
        return l;
    }

    public static List<CitizenEntity> citizens() {
        List<CitizenEntity> l = new ArrayList<>();
        for (CitizenEntity c : CITIZENS) if (c.isAlive() && !c.isRemoved()) l.add(c);
        return l;
    }

    public static List<CitizenEntity> citizensOf(int nation) {
        List<CitizenEntity> l = new ArrayList<>();
        for (CitizenEntity c : CITIZENS) if (c.isAlive() && !c.isRemoved() && c.getNationId() == nation) l.add(c);
        return l;
    }

    public static void clear() {
        SOLDIERS.clear();
        CITIZENS.clear();
    }
}
