package io.captainyannick.advancedShops.shop;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ShopHologram {

    private static final Map<Shop, Item> floatingItems = new HashMap<>();

    public static void createFloatingItem(Shop shop) {
        if (shop != null) {
            Location location = shop.getLocation().clone().add(0.5, 1, 0.5);

            ItemStack shopItem = shop.getItem().clone();
            shopItem.setAmount(1);
            Item floatingItem = location.getWorld().dropItem(location, shopItem);

            floatingItem.setGravity(false);
            floatingItem.setCanMobPickup(false);
            floatingItem.setPickupDelay(Integer.MAX_VALUE);
            floatingItem.setUnlimitedLifetime(true);
            floatingItem.setCustomNameVisible(false);
            floatingItem.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            floatingItem.setCanPlayerPickup(false);

            floatingItems.put(shop, floatingItem);
        }
    }

    public static void removeFloatingItem(Shop shop) {
        Location location = shop.getLocation().clone().add(0.5, 1, 0.5);
        ItemStack shopItem = shop.getItem();
        floatingItems.remove(shop);

        location.getWorld().getNearbyEntities(location, 1, 1, 1).stream()
                .filter(entity -> entity instanceof Item)
                .map(entity -> (Item) entity)
                .filter(item -> item.getItemStack().isSimilar(shopItem))
                .forEach(Item::remove);

    }

    public static void updateFloatingItem(Shop shop) {
        removeFloatingItem(shop);
        createFloatingItem(shop);
    }
}
