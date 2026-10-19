package com.endruqq.imperium.game.building;

public enum BuildingCategory {
    HOUSING, ECONOMY, SOCIETY, MILITARY;

    public String key() {
        return "imperium.category." + name().toLowerCase();
    }
}
