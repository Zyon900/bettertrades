package com.zyon900.bettertrades.mixin;

import com.google.common.collect.Lists;
import com.zyon900.bettertrades.Config;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import static java.lang.Math.min;

@Mixin(Villager.class)
public abstract class VillagerMixin {

    @Shadow @Final private static Logger LOGGER;

    @Shadow protected abstract void updateDemand();

    @Shadow protected abstract void resendOffersToTradingPlayer();

    @Shadow private long lastRestockGameTime;

    @Shadow private int numberOfRestocksToday;

    @Shadow public abstract void setOffers(MerchantOffers offers);

    @Shadow @Final private static int TRADES_PER_LEVEL;

    private Villager bettertrades$self = (Villager)(Object)(this);

    /**
     * Injects into Villager restock method head, to re-roll trades.
     *
     * @param ci Callback to cancel original restock method.
     */
    @Inject(
            method = "restock",
            at = @At("HEAD"),
            cancellable = true
    )
    private void bettertrades$replaceRestock(CallbackInfo ci) {
        boolean enabled = Config.COMMON.enableReRollOnRestock.get();
        Double chance = Config.COMMON.reRollOnRestockChance.get();
        // Return early if no re-roll happens
        if(!enabled || bettertrades$self.getRandom().nextDouble() > chance)
            return;
        // Re-roll trades and update amount of restocks
        try {
            bettertrades$reRollTrades();

            this.lastRestockGameTime = bettertrades$self.level().getGameTime();
            ++this.numberOfRestocksToday;

        } catch (Exception e) {
            LOGGER.error("Error during Villager restock replacement mixin:", e);
        } finally {
            ci.cancel();
        }
    }

    /**
     * Injects into Villager stopSleeping method return, to re-roll trades.
     */
    @Inject(
            method = "stopSleeping",
            at = @At("RETURN")
    )
    private void bettertrades$addToStopSleeping(CallbackInfo ci) {
        boolean enabled = Config.COMMON.enableReRollOnSleep.get();
        Double chance = Config.COMMON.reRollOnSleepChance.get();

        // Return early if no re-roll happens
        if(!enabled || bettertrades$self.getRandom().nextDouble() > chance)
            return;
        // Re-roll trades
        try {
            bettertrades$reRollTrades();
        } catch (Exception e) {
            LOGGER.error("Error during Villager sleep addition mixin:", e);
        }
    }

    /**
     * Re-rolls the villagers trades
     */
    @Unique
    private void bettertrades$reRollTrades() {
        // Update demand
        this.updateDemand();

        // Get villager data
        VillagerData villagerData = bettertrades$self.getVillagerData();

        // Populate proper item listings
        Int2ObjectMap<VillagerTrades.ItemListing[]> int2objectmap = bettertrades$getVillagerItemListings(villagerData);

        // Return if no trades were found
        if(int2objectmap == null || int2objectmap.isEmpty())
            return;

        // Generate trade offers
        MerchantOffers offers = bettertrades$buildMerchantOffers(villagerData, int2objectmap);

        // Finish up
        this.setOffers(offers);
        this.resendOffersToTradingPlayer();
    }

    /**
     * Returns the valid trades of the villager.
     *
     * @param villagerData of this villager.
     * @return Map of valid trades.
     */
    @Unique
    private Int2ObjectMap<VillagerTrades.ItemListing[]> bettertrades$getVillagerItemListings(VillagerData villagerData) {
        Int2ObjectMap<VillagerTrades.ItemListing[]> int2objectmap;
        if (bettertrades$self.level().enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE)) {
            Int2ObjectMap<VillagerTrades.ItemListing[]> int2objectmap1 = VillagerTrades.EXPERIMENTAL_TRADES.get(villagerData.getProfession());
            int2objectmap = int2objectmap1 != null ? int2objectmap1 : VillagerTrades.TRADES.get(villagerData.getProfession());
        } else {
            int2objectmap = VillagerTrades.TRADES.get(villagerData.getProfession());
        }
        return int2objectmap;
    }

    /**
     * Builds merchant offers of villager.
     *
     * @param villagerData VillagerData of this villager.
     * @param int2objectmap Map of valid trades for this villager.
     * @return New merchant offers for villager.
     */
    @Unique
    private MerchantOffers bettertrades$buildMerchantOffers(VillagerData villagerData, Int2ObjectMap<VillagerTrades.ItemListing[]> int2objectmap) {
        // Copy current offers
        MerchantOffers offers = bettertrades$self.getOffers().copy();

        // Generate level trades and replace them in offers
        int currentTradeIndex = 0;

        for (int level = VillagerData.MIN_VILLAGER_LEVEL; level <= villagerData.getLevel(); level++) {
            ArrayList<VillagerTrades.ItemListing> currentLevelTrades = Lists.newArrayList(int2objectmap.get(level));
            int maxTradesPerLevel = min(currentLevelTrades.size(), TRADES_PER_LEVEL);
            for (int i = 1; i <= maxTradesPerLevel ; i++) {
                if(offers.size() - 1 < currentTradeIndex)
                    break;
                if(bettertrades$reRollAllowed(offers.get(currentTradeIndex))) {
                    offers.set(currentTradeIndex, currentLevelTrades.remove(bettertrades$self.getRandom().nextInt(currentLevelTrades.size())).getOffer(bettertrades$self, this.bettertrades$self.getRandom()));
                }
                currentTradeIndex++;
            }
        }
        return offers;
    }

    /**
     * Checks whether an offer is allowed to be re-rolled - currently filled maps are not allowed to be re-rolled.
     * @param offer MerchantOffer to be checked
     * @return True if re-roll is allowed
     */
    @Unique
    public Boolean bettertrades$reRollAllowed(MerchantOffer offer) {
        // Check if the result is a filled map
        ItemStack result = offer.getResult();
        boolean isMapOffer = result.getItem() == Items.FILLED_MAP;

        return !isMapOffer;
    }
}
