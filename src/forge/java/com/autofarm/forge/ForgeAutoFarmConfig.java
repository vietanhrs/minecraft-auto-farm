package com.autofarm.forge;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ForgeAutoFarmConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue ATTACK_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.IntValue CRITICAL_FOOD_LEVEL;

    static {
        BUILDER.comment("AutoFarm configuration").push("autofarm");

        ATTACK_INTERVAL_SECONDS = BUILDER
                .comment("Seconds between each automatic sword attack (default: 10)")
                .defineInRange("attack_interval_seconds", 10, 1, 600);

        CRITICAL_FOOD_LEVEL = BUILDER
                .comment("Food level at or below which the player will eat before attacking (default: 3)")
                .defineInRange("critical_food_level", 3, 1, 19);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private ForgeAutoFarmConfig() {
    }
}
