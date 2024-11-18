package io.captainyannick.advancedShops.shop;

public class PriceAdjustment {
    public Shop shop;
    public boolean isBuyPrice;

    public PriceAdjustment(Shop shop, boolean isBuyPrice) {
        this.shop = shop;
        this.isBuyPrice = isBuyPrice;
    }
}
