package com.zyon900.bettertrades;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(BetterTrades.MODID)
public class BetterTrades
{
    public static final String MODID = "bettertrades";

    public BetterTrades(ModContainer container) {
        container.registerConfig(
                ModConfig.Type.COMMON,
                Config.COMMON_SPEC
        );
    }
}
