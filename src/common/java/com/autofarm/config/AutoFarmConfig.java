package com.autofarm.config;

import java.util.function.IntSupplier;

public class AutoFarmConfig {

    private static IntSupplier attackIntervalSeconds = () -> 10;
    private static IntSupplier criticalFoodLevel = () -> 3;

    private AutoFarmConfig() {
    }

    public static void configure(IntSupplier attackIntervalSeconds, IntSupplier criticalFoodLevel) {
        AutoFarmConfig.attackIntervalSeconds = attackIntervalSeconds;
        AutoFarmConfig.criticalFoodLevel = criticalFoodLevel;
    }

    public static int attackIntervalSeconds() {
        return attackIntervalSeconds.getAsInt();
    }

    public static int criticalFoodLevel() {
        return criticalFoodLevel.getAsInt();
    }
}
