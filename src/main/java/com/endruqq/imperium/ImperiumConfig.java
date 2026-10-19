package com.endruqq.imperium;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ImperiumConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue ECONOMY_CYCLE_SECONDS;
    public static final ForgeConfigSpec.IntValue GRACE_PERIOD_MINUTES;
    public static final ForgeConfigSpec.IntValue TRUCE_MINUTES;
    public static final ForgeConfigSpec.IntValue CORE_BASE_HP;
    public static final ForgeConfigSpec.IntValue MAX_CITIZENS_PER_NATION;
    public static final ForgeConfigSpec.IntValue STARTING_POPULATION;
    public static final ForgeConfigSpec.BooleanValue AUTO_BOTS;
    public static final ForgeConfigSpec.BooleanValue PROTECT_TERRITORY;
    public static final ForgeConfigSpec.BooleanValue SET_WORLD_BORDER;
    public static final ForgeConfigSpec.BooleanValue KEEP_ARMIES_LOADED;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("game");
        ECONOMY_CYCLE_SECONDS = b.comment("Length of one economy cycle in seconds").defineInRange("economyCycleSeconds", 20, 5, 600);
        GRACE_PERIOD_MINUTES = b.comment("Peace period after game start during which wars cannot be declared").defineInRange("gracePeriodMinutes", 5, 0, 240);
        TRUCE_MINUTES = b.comment("Truce length after a peace treaty").defineInRange("truceMinutes", 10, 0, 240);
        CORE_BASE_HP = b.comment("Base hit points of a province core").defineInRange("coreBaseHp", 300, 10, 100000);
        MAX_CITIZENS_PER_NATION = b.comment("Maximum number of visible citizen NPCs per nation").defineInRange("maxCitizensPerNation", 24, 0, 200);
        STARTING_POPULATION = b.comment("Population of every nation at game start").defineInRange("startingPopulation", 12, 1, 1000);
        AUTO_BOTS = b.comment("Nations without players become AI controlled when the game starts").define("autoBots", true);
        PROTECT_TERRITORY = b.comment("Prevent foreign players from breaking/placing blocks in a nation's territory unless at war").define("protectTerritory", true);
        SET_WORLD_BORDER = b.comment("Set the vanilla world border to the arena when the game starts").define("setWorldBorder", true);
        KEEP_ARMIES_LOADED = b.comment("Keep bot capitals and marching armies chunk-loaded so wars also happen far from players").define("keepArmiesLoaded", true);
        b.pop();
        SPEC = b.build();
    }

    private ImperiumConfig() {}
}
