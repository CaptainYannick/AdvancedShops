package io.captainyannick.advancedShops.listeners;

import io.captainyannick.advancedShops.AdvancedShops;
import io.captainyannick.advancedShops.core.utils.FormatUtils;
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

public class GUIListener implements Listener {

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
        } else if (title.equals(ChatColor.BLUE + "Manage Shop")) {
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
            case 11: // Buy Item Slot
                ShopManager.buyFromShop(player, shop);
                break;
            case 15: // Sell Item Slot
                ShopManager.sellToShop(player, shop);
                break;
        }
    }

    private void handleManagementGUIClick(InventoryClickEvent event, Player player, Inventory inventory) {
        event.setCancelled(true);

        Shop shop = ShopManager.getActiveShopSession(player);

        if (shop == null || !shop.getOwner().equals(player.getUniqueId()) && !shop.getManagers().contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You do not have permission to manage this shop.");
            return;
        }

        switch (event.getRawSlot()) {
            case 10: // Adjust Buy Price
                adjustBuyPrice(player, shop);
                inventory.close();
                break;
            case 12: // Adjust Sell Price
                adjustSellPrice(player, shop);
                inventory.close();
                break;
            case 14:
                inventory.close();
                toggleShopStatus(player, shop);
                break;
            case 16: // Manage Stock
                ShopManager.addToStockEditGui(player, shop);
                manageStock(player, shop);
                break;
            case 26: // Delete Shop
                if (shop.getOwner().equals(player.getUniqueId())) {
                    confirmOrDeleteShop(player, shop, event.getCurrentItem());
                } else {
                    FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("only_owner_can_delete"), player);
                }
                break;
        }
    }

    private void confirmOrDeleteShop(Player player, Shop shop, ItemStack clickedItem) {
        // Check if the item is already in "Confirm Delete" state
        if (clickedItem != null && clickedItem.getType() == Material.TNT &&
                clickedItem.getItemMeta().getDisplayName().equals(ChatColor.RED + "Confirm Delete")) {
            // Second click: Proceed with deletion
            player.sendMessage(ChatColor.GREEN + "Deleting shop...");
            ShopManager.deleteShop(shop); // Delete the shop and remove associated data
            player.closeInventory();
        } else {
            // First click: Set item to "Confirm Delete" state
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

                // Update the shop's buy or sell price
                if (adjustment.isBuyPrice) {
                    adjustment.shop.setBuyPrice(newPrice);
                    player.sendMessage(ChatColor.GREEN + "Buy price updated to " + newPrice);
                } else {
                    adjustment.shop.setSellPrice(newPrice);
                    player.sendMessage(ChatColor.GREEN + "Sell price updated to " + newPrice);
                }

                // Finish the adjustment and refresh the GUI
                ShopManager.finishPriceAdjustment(player);
                Bukkit.getScheduler().runTask(AdvancedShops.getInstance(), () -> {
                    ShopManager.openManagementGUI(player, adjustment.shop);
                });

            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid price. Please enter a valid number.");
            }
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

        // Check if the player was managing stock
        if (ShopManager.getStockInventory(player) == inventory) {
            shop = ShopManager.getToStockEditGui(player);
            // Count the items in the inventory to determine the new stock
            int newStock = 0;
            for (ItemStack item : inventory.getContents()) {
                if (item != null && item.isSimilar(shop.getItem())) {
                    newStock += item.getAmount();
                }
            }

            // Update the shop's stock
            shop.setStock(newStock);
            player.sendMessage(ChatColor.GREEN + "Stock updated to " + newStock);

            // Remove the stock inventory session
            ShopManager.removeStockInventory(player);
            ShopManager.removeShopSession(player);
            ShopManager.removeStockEditGui(player);
        }
        if (shop != null) {
            ShopManager.updateShop(shop);
            ShopManager.removeShopSession(player);
        }
    }
}