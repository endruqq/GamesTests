package com.endruqq.imperium.game;

import java.util.HashMap;
import java.util.Map;

/** Number of variants for every dialog line key (generated together with the language files). */
public final class DialogLines {
    private static final Map<String, Integer> COUNTS = new HashMap<>();

    private DialogLines() {}

    public static int count(String key) {
        return COUNTS.getOrDefault(key, 0);
    }
}
