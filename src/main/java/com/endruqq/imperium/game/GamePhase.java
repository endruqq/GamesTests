package com.endruqq.imperium.game;

public enum GamePhase {
    SETUP, RUNNING, ENDED;

    public String key() {
        return "imperium.phase." + name().toLowerCase();
    }
}
