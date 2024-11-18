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
                player.sendMessage(ChatColor.RED + "Error: Could not retrieve shop data.");
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
                player.sendMessage(ChatColor.RED + "You can only add/remove the shop item type.");
            }
        }

        // Identify the shop based on title
        if (title.startsWith(ChatColor.GREEN + "Shop: ")) {
            handleCustomerGUIClick(event, player, inventory);
        } else if (title.equals(ChatColor.DARK_GRAY + "Manage Shop")) {
            handleManagementGUIClick(event, player, inventory);
        }
    }


    private void handleCustomerGUIClick(InventoryClickEvent event, Player player, Inventory inventory) {
        event.setCancelled(true);

        Shop shop = ShopManager.getActiveShopSession(player);

        if (shop == null) {
            player.sendMessage(ChatColor.RED + "Shop not found.");
            return;
        }

        switch (event.getRawSlot()) {
            case 11:
                startChatPrompt(player, shop, "buy");
                inventory.close();
                break;
            case 15:
                startChatPrompt(player, shop, "sell");
                inventory.close();
                break;
        }
    }

    private void startChatPrompt(Player player, Shop shop, String action) {
        if (activePrompts.containsKey(player)) {
            player.sendMessage(ChatColor.RED + "You are already entering an amount.");
            return;
        }
        int maxAmount;
        if (action.equalsIgnoreCase("buy")) {
            maxAmount = calculateMaxBuyable(player, shop);
            player.sendMessage(ChatColor.YELLOW + "You can buy up to " + maxAmount + " items. Enter the amount in the chat, or type 'cancel' to abort.");

        } else {
            maxAmount = calculateMaxSellable(player, shop);
            player.sendMessage(ChatColor.YELLOW + "You can sell up to " + maxAmount + " items. Enter the amount in the chat, or type 'cancel' to abort.");
        }
        activePrompts.put(player, new ChatPrompt(shop, action, maxAmount));

    }

    private void handleManagementGUIClick(InventoryClickEvent event, Player player, Inventory inventory) {
        event.setCancelled(true);

        Shop shop = ShopManager.getActiveShopSession(player);

        if (shop == null || !shop.getOwner().equals(player.getUniqueId()) && !shop.getManagers().contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You do not have permission to manage this shop.");
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
            case 26:
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
            player.sendMessage(ChatColor.GREEN + "Deleting shop...");
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

            player.sendMessage(ChatColor.YELLOW + "Click the delete button again to confirm.");
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
                    player.sendMessage(ChatColor.RED + "Price must be a positive number.");
                    return;
                }

                if (adjustment.isBuyPrice) {
                    adjustment.shop.setBuyPrice(newPrice);
                    player.sendMessage(ChatColor.GREEN + "Buy price updated to " + newPrice);
                } else {
                    adjustment.shop.setSellPrice(newPrice);
                    player.sendMessage(ChatColor.GREEN + "Sell price updated to " + newPrice);
                }

                ShopManager.finishPriceAdjustment(player);
                Bukkit.getScheduler().runTask(AdvancedShops.getInstance(), () -> {
                    ShopManager.openManagementGUI(player, adjustment.shop);
                });

            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid price. Please enter a valid number.");
            }
        }

        if (!activePrompts.containsKey(player)) return;
        event.setCancelled(true);

        ChatPrompt prompt = activePrompts.get(player);
        String message = event.getMessage();

        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.RED + "Action cancelled.");
            activePrompts.remove(player);
            return;
        }

        try {
            int amount = Integer.parseInt(message);

            if (amount <= 0) {
                player.sendMessage(ChatColor.RED + "Please enter a positive number.");
                return;
            }

            Bukkit.getScheduler().runTask(AdvancedShops.getInstance(), () -> {

                if (prompt.getAction().equals("buy")) {
                    if (amount > prompt.getMax()) {
                        player.sendMessage(ChatColor.RED + "You can't buy that many items. Maximum: " + prompt.getMax());
                    } else {
                        performBuy(player, prompt.getShop(), amount);
                    }
                } else if (prompt.getAction().equals("sell")) {
                    if (amount > prompt.getMax()) {
                        player.sendMessage(ChatColor.RED + "You can't sell that many items. Maximum: " + prompt.getMax());
                    } else {
                        performSell(player, prompt.getShop(), amount);
                    }
                }
            });

            activePrompts.remove(player);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Please enter a valid number.");
        }
    }

    private void adjustBuyPrice(Player player, Shop shop) {
        ShopManager.startPriceAdjustment(player, shop, true);
        player.sendMessage(ChatColor.GREEN + "Enter the new buy price in chat.");
    }

    private void adjustSellPrice(Player player, Shop shop) {
        ShopManager.startPriceAdjustment(player, shop, false);
        player.sendMessage(ChatColor.GREEN + "Enter the new sell price in chat.");
    }

    private void toggleShopStatus(Player player, Shop shop) {
        shop.setEnabled(!shop.isEnabled());
        player.sendMessage(ChatColor.GREEN + "Shop is now " + (shop.isEnabled() ? "Enabled" : "Disabled"));
    }

    private void manageStock(Player player, Shop shop) {
        player.sendMessage(ChatColor.GREEN + "To adjust stock, add or remove items to/from the shop’s inventory.");
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
            player.sendMessage(ChatColor.GREEN + "Stock updated to " + newStock);

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
            shop.reduceStock(amount);
            addItemToInventory(player, shop.getItem(), amount);
            player.sendMessage(ChatColor.GREEN + "You bought " + amount + " " + TextUtils.formatItemName(shop.getItem()) + " for " + totalPrice + "!");
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
            player.sendMessage(ChatColor.RED + "Transaction failed. Not enough money.");
        }
    }

    private void performSell(Player player, Shop shop, int amount) {
        double totalPrice = amount * shop.getSellPrice();
        shop.addStock(amount);
        removeItemsFromInventory(player, shop.getItem(), amount);
        AdvancedShops.getEconomy().depositPlayer(player, totalPrice);
        player.sendMessage(ChatColor.GREEN + "You sold " + amount + " " + TextUtils.formatItemName(shop.getItem()) + " for " + totalPrice + "!");
        ShopManager.updateShop(shop);
        Player owner = (Player) Bukkit.getOfflinePlayer(shop.getOwner());
        if (owner.isOnline()) {
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("sell_notification")
                    .replaceAll("%player%", player.getName())
                    .replaceAll("%amount%", String.valueOf(amount))
                    .replaceAll("%item%", TextUtils.formatItemName(shop.getItem()))
                    .replaceAll("%money%", String.valueOf(totalPrice)), owner);
        }
    }

    private int calculateInventorySpace(Player player, ItemStack item) {
        int freeSpace = 0;
        for (ItemStack invItem : player.getInventory().getStorageContents()) {
            if (invItem == null || invItem.getType() == Material.AIR) {
                freeSpace += item.getMaxStackSize(); // Lege sloten
            } else if (invItem.isSimilar(item)) {
                freeSpace += item.getMaxStackSize() - invItem.getAmount(); // Vulbare sloten
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