package com.zyon900.bettertrades;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {

    public static final ModConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    public static class CommonConfig {
        // Settings for re-rolling trades when villagers sleep
        public final ModConfigSpec.BooleanValue enableReRollOnSleep;
        public final ModConfigSpec.DoubleValue reRollOnSleepChance;
        // Settings for re-rolling trades when villagers restock
        public final ModConfigSpec.EnumValue<RerollType> reRollOnRestockType;
        public final ModConfigSpec.DoubleValue reRollOnRestockChance;

        CommonConfig(ModConfigSpec.Builder builder) {
            builder.comment("Villager Trade Settings").push("trades");
            enableReRollOnSleep = builder.define("enableReRollOnSleep", true);
            reRollOnSleepChance = builder.defineInRange("reRollOnSleepChance", 1.0, 0.0, 1.0);
            builder.comment("Accepted values for reRollOnRestockType: USED_UP_REROLL (Only rerolls trades that have been used up), FULL_REROLL (Rerolls all trades), DISABLED (No rerolls, and normal restocking for trades on restock.");
            reRollOnRestockType = builder.defineEnum("reRollOnRestockType", RerollType.USED_UP_REROLL);
            reRollOnRestockChance = builder.defineInRange("reRollOnRestockChance", 1.0, 0.0, 1.0);
            builder.pop();
        }
    }

    static {
        final Pair<CommonConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(CommonConfig::new);

        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
}
