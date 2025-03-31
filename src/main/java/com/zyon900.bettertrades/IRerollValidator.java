package com.zyon900.bettertrades;

import net.minecraft.world.item.trading.MerchantOffer;

public interface IRerollValidator {
    Boolean allowReroll(MerchantOffer offer);
}
