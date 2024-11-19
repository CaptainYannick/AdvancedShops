package io.captainyannick.advancedShops.listeners;

import io.captainyannick.advancedShops.AdvancedShops;
import io.captainyannick.advancedShops.core.utils.FormatUtils;
import io.captainyannick.advancedShops.core.utils.TextUtils;
import io.captainyannick.advancedShops.shop.PriceAdjustment;
import io.captainyannick.advancedShops.shop.Shop;
import io.captainyannick.advancedShops.shop.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GUIListener implements Listener {

    private final Map<Player, ChatPrompt> activePrompts = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if the inventory title matches the customer or management GUI
        Inventory inventory = event.getInventory();
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();

        // Check if the player is managing stock
        Inventory stockInventory = ShopManager.getStockInventory(player);
        if (stockInventory != null && inventory.equals(stockInventory)) {
            Shop shop = ShopManager.getToStockEditGui(player);

            if (shop == null) {
                FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("could_not_retrieve_shop"), player);
                return;
            }

            // Check by material type only
            Material shopItemMaterial = shop.getItem().getType();

            ItemStack currentItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();

            boolean isCurrentItemValid = currentItem == null || currentItem.getType() == Material.AIR || currentItem.getType() == shopItemMaterial;
            boolean isCursorItemValid = cursorItem == null || cursorItem.getType() == Material.AIR || cursorItem.getType() == shopItemMaterial;

            if (!isCurrentItemValid || !isCursorItemValid) {
                event.setCancelled(true);
                FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("only_add_shop_item"), player);
            }
        }

        if (title.startsWith(ChatColor.DARK_GRAY + "Shop: ")) {
            handleCustomerGUIClick(event, player, inventory);
        } else if (title.equals(ChatColor.DARK_GRAY + "Manage Shop")) {
            handleManagementGUIClick(event, player, inventory);
        }
    }


    private void handleCustomerGUIClick(InventoryClickEvent event, Player player, Inventory inventory) {
        event.setCancelled(true);

        Shop shop = ShopManager.getActiveShopSession(player);

        if (shop == null) {
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("shop_not_found"), player);
            return;
        }

        switch (event.getRawSlot()) {
            case 11:
                if(shop.getBuyPrice() > 0) {
                    if (AdvancedShops.getEconomy().getBalance(player) > shop.getBuyPrice()) {
                        startChatPrompt(player, shop, "buy");
                        inventory.close();
                    } else {
                        FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("not_enough_money"), player);
                    }
                } else {
                    FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("shop_buy_disabled"), player);
                }
                break;
            case 15:
                if (shop.getSellPrice() > 0) {
                    startChatPrompt(player, shop, "sell");
                    inventory.close();
                } else {
                    FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("shop_sell_disabled"), player);
                }
                break;
        }
    }

    private void startChatPrompt(Player player, Shop shop, String action) {
        if (activePrompts.containsKey(player)) {
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("already_entering_amount"), player);
            return;
        }
        int maxAmount;
        if (action.equalsIgnoreCase("buy")) {
            maxAmount = calculateMaxBuyable(player, shop);
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("buy").replaceAll("%max_amount%", String.valueOf(maxAmount)), player);

        } else {
            maxAmount = calculateMaxSellable(player, shop);
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("sell").replaceAll("%max_amount%", String.valueOf(maxAmount)), player);
        }
        activePrompts.put(player, new ChatPrompt(shop, action, maxAmount));

    }

    private void handleManagementGUIClick(InventoryClickEvent event, Player player, Inventory inventory) {
        event.setCancelled(true);

        Shop shop = ShopManager.getActiveShopSession(player);

        if (shop == null || !shop.getOwner().equals(player.getUniqueId()) && !shop.getManagers().contains(player.getUniqueId())) {
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("no_permission_to_manage"), player);
            return;
        }

        switch (event.getRawSlot()) {
            case 10:
                adjustBuyPrice(player, shop);
                inventory.close();
                break;
            case 12:
                adjustSellPrice(player, shop);
                inventory.close();
                break;
            case 14:
                inventory.close();
                toggleShopStatus(player, shop);
                ShopManager.updateShop(shop);
                break;
            case 16:
                ShopManager.addToStockEditGui(player, shop);
                manageStock(player, shop);
                break;
            case 31:
                if (shop.getOwner().equals(player.getUniqueId())) {
                    confirmOrDeleteShop(player, shop, event.getCurrentItem());
                } else {
                    FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("only_owner_can_delete"), player);
                }
                break;
        }
    }

    private void confirmOrDeleteShop(Player player, Shop shop, ItemStack clickedItem) {
        if (clickedItem != null && clickedItem.getType() == Material.TNT &&
                clickedItem.getItemMeta().getDisplayName().equals(ChatColor.RED + "Confirm Delete")) {
            ShopManager.deleteShop(shop);
            player.closeInventory();
        } else {
            ItemMeta meta = clickedItem.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Confirm Delete");
            meta.setLore(Arrays.asList(
                    ChatColor.RED + "Click again to confirm deletion.",
                    ChatColor.GRAY + "This action cannot be undone!"
            ));
            clickedItem.setItemMeta(meta);
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("click_to_confirm"), player);
        }

    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PriceAdjustment adjustment = ShopManager.getPriceAdjustment(player);

        if (adjustment != null) {
            event.setCancelled(true); // Prevent the message from being shown in chat
            String message = event.getMessage();

            try {
                double newPrice = Double.parseDouble(message);
                if (newPrice < 0) {
                    FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("positive_number"), player);
                    return;
                }

                if (adjustment.isBuyPrice) {
                    adjustment.shop.setBuyPrice(newPrice);
                    FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("buyprice_updated")
                            .replaceAll("%new_price%", String.valueOf(newPrice)), player);
                } else {
                    adjustment.shop.setSellPrice(newPrice);
                    FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("sellprice_updated")
                            .replaceAll("%new_price%", String.valueOf(newPrice)), player);
                }

                ShopManager.finishPriceAdjustment(player);
                Bukkit.getScheduler().runTask(AdvancedShops.getInstance(), () -> {
                    ShopManager.openManagementGUI(player, adjustment.shop);
                });

            } catch (NumberFormatException e) {
                FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("invalid_number"), player);
            }
        }

        if (!activePrompts.containsKey(player)) return;
        event.setCancelled(true);

        ChatPrompt prompt = activePrompts.get(player);
        String message = event.getMessage();

        if (message.equalsIgnoreCase("cancel")) {
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("cancelled"), player);
            activePrompts.remove(player);
            return;
        }

        try {
            int amount = Integer.parseInt(message);

            if (amount <= 0) {
                FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("positive_number"), player);
                return;
            }

            Bukkit.getScheduler().runTask(AdvancedShops.getInstance(), () -> {

                if (prompt.getAction().equals("buy")) {
                    if (amount > prompt.getMax()) {
                        FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("cannot_buy_that_amount")
                                .replaceAll("%max%", String.valueOf(prompt.getMax())), player);
                    } else {
                        performBuy(player, prompt.getShop(), amount);
                    }
                } else if (prompt.getAction().equals("sell")) {
                    if (amount > prompt.getMax()) {
                        FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("cannot_sell_that_amount")
                                .replaceAll("%max%", String.valueOf(prompt.getMax())), player);
                    } else {
                        performSell(player, prompt.getShop(), amount);
                    }
                }
            });

            activePrompts.remove(player);
        } catch (NumberFormatException e) {
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("invalid_number"), player);
        }
    }

    private void adjustBuyPrice(Player player, Shop shop) {
        ShopManager.startPriceAdjustment(player, shop, true);
        FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("prompt_adjust_buy"), player);
    }

    private void adjustSellPrice(Player player, Shop shop) {
        ShopManager.startPriceAdjustment(player, shop, false);
        FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("prompt_adjust_sell"), player);
    }

    private void toggleShopStatus(Player player, Shop shop) {
        shop.setEnabled(!shop.isEnabled());
        FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("toggled")
                .replaceAll("%status%",(shop.isEnabled() ? "Enabled" : "Disabled")), player);
    }

    private void manageStock(Player player, Shop shop) {
        FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("adjust_stock"), player);
        ShopManager.openStockManagementGUI(player, shop);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        Shop shop = ShopManager.getActiveShopSession(player);

        if (ShopManager.getStockInventory(player) == inventory) {
            shop = ShopManager.getToStockEditGui(player);
            int newStock = 0;
            for (ItemStack item : inventory.getContents()) {
                if (item != null && item.isSimilar(shop.getItem())) {
                    newStock += item.getAmount();
                }
            }

            shop.setStock(newStock);
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("stock_update")
                    .replaceAll("%stock%", String.valueOf(newStock)), player);

            ShopManager.removeStockInventory(player);
            ShopManager.removeShopSession(player);
            ShopManager.removeStockEditGui(player);
        }
        if (shop != null) {
            ShopManager.updateShop(shop);
            ShopManager.removeShopSession(player);
        }
    }

    private int calculateMaxBuyable(Player player, Shop shop) {
        int stock = shop.getStock();
        int playerBalance = (int) AdvancedShops.getEconomy().getBalance(player);
        int maxAffordable = (int) (playerBalance / shop.getBuyPrice());

        int maxStackable = calculateInventorySpace(player, shop.getItem());

        return Math.min(stock, Math.min(maxAffordable, maxStackable));
    }

    private int calculateMaxSellable(Player player, Shop shop) {
        int playerItemCount = countItemsInInventory(player, shop.getItem());
        return playerItemCount;
    }

    private void performBuy(Player player, Shop shop, int amount) {
        double totalPrice = amount * shop.getBuyPrice();
        if (AdvancedShops.getEconomy().withdrawPlayer(player, totalPrice).transactionSuccess()) {
            AdvancedShops.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(shop.getOwner()), totalPrice);
            shop.reduceStock(amount);
            addItemToInventory(player, shop.getItem(), amount);
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("player_bought")
                    .replaceAll("%amount%", String.valueOf(amount))
                    .replaceAll("%item%", TextUtils.formatItemName(shop.getItem()))
                    .replaceAll("%price%", String.valueOf(totalPrice)), player);
            ShopManager.updateShop(shop);
            Player owner = (Player) Bukkit.getOfflinePlayer(shop.getOwner());
            if (owner.isOnline()) {
                FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("buy_notification")
                        .replaceAll("%player%", player.getName())
                        .replaceAll("%amount%", String.valueOf(amount))
                        .replaceAll("%item%", TextUtils.formatItemName(shop.getItem()))
                        .replaceAll("%money%", String.valueOf(totalPrice)), owner);
            }
        } else {
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("not_enough_money"), player);
        }
    }

    private void performSell(Player player, Shop shop, int amount) {
        double totalPrice = amount * shop.getSellPrice();
        if (AdvancedShops.getEconomy().getBalance(Bukkit.getOfflinePlayer(shop.getOwner())) > totalPrice) {
            shop.addStock(amount);
            removeItemsFromInventory(player, shop.getItem(), amount);
            AdvancedShops.getEconomy().depositPlayer(player, totalPrice);
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("player_sold")
                    .replaceAll("%amount%", String.valueOf(amount))
                    .replaceAll("%item%", TextUtils.formatItemName(shop.getItem()))
                    .replaceAll("%price%", String.valueOf(totalPrice)), player);
            ShopManager.updateShop(shop);
            Player owner = (Player) Bukkit.getOfflinePlayer(shop.getOwner());
            if (owner.isOnline()) {
                FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("sell_notification")
                        .replaceAll("%player%", player.getName())
                        .replaceAll("%amount%", String.valueOf(amount))
                        .replaceAll("%item%", TextUtils.formatItemName(shop.getItem()))
                        .replaceAll("%money%", String.valueOf(totalPrice)), owner);
            }
        } else {
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("not_enough_money"), player);
        }
    }

    private int calculateInventorySpace(Player player, ItemStack item) {
        int freeSpace = 0;
        for (ItemStack invItem : player.getInventory().getStorageContents()) {
            if (invItem == null || invItem.getType() == Material.AIR) {
                freeSpace += item.getMaxStackSize();
            } else if (invItem.isSimilar(item)) {
                freeSpace += item.getMaxStackSize() - invItem.getAmount();
            }
        }
        return freeSpace;
    }

    private int countItemsInInventory(Player player, ItemStack item) {
        int count = 0;
        for (ItemStack invItem : player.getInventory().getStorageContents()) {
            if (invItem != null && invItem.isSimilar(item)) {
                count += invItem.getAmount();
            }
        }
        return count;
    }

    private void addItemToInventory(Player player, ItemStack item, int amount) {
        ItemStack clone = item.clone();
        while (amount > 0) {
            int stackSize = Math.min(amount, clone.getMaxStackSize());
            clone.setAmount(stackSize);
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(clone);
            if (!leftovers.isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), leftovers.values().iterator().next());
            }
            amount -= stackSize;
        }
    }

    private void removeItemsFromInventory(Player player, ItemStack item, int amount) {
        for (ItemStack invItem : player.getInventory().getStorageContents()) {
            if (invItem != null && invItem.isSimilar(item)) {
                int toRemove = Math.min(amount, invItem.getAmount());
                invItem.setAmount(invItem.getAmount() - toRemove);
                amount -= toRemove;
                if (amount <= 0) break;
            }
        }
        player.updateInventory();
    }
}