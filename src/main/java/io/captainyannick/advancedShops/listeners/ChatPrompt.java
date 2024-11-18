package io.captainyannick.advancedShops.listeners;

import io.captainyannick.advancedShops.shop.Shop;

public class ChatPrompt {
    private final Shop shop;
    private final String action;
    private final int maxAmount;

    public ChatPrompt(Shop shop, String action, int maxAmount) {
        this.shop = shop;
        this.action = action;
        this.maxAmount = maxAmount;
    }

    public Shop getShop() {
        return shop;
    }

    public String getAction() {
        return action;
    }

    public int getMax(){
        return maxAmount;
    }
}
