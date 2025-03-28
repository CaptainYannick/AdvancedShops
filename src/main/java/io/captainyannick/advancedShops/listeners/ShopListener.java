package io.captainyannick.advancedShops.listeners;

import io.captainyannick.advancedShops.AdvancedShops;
import io.captainyannick.advancedShops.core.utils.FormatUtils;
import io.captainyannick.advancedShops.shop.Shop;
import io.captainyannick.advancedShops.shop.ShopManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ShopListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        Material type = block.getType();
        Player player = event.getPlayer();
        if (type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL) {
            Shop shop = ShopManager.getShopAtLocation(block.getLocation());

            if (shop != null) {
                event.setCancelled(true);

                if (shop.getOwner().equals(player.getUniqueId()) || shop.getManagers().contains(player.getUniqueId())) {
                    ShopManager.openManagementGUI(player, shop);
                } else {
                    if (shop.isEnabled()){
                        ShopManager.openCustomerGUI(player, shop);
                    } else {
                        FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("shop_disabled"), player);
                    }
                }
            }
        }

        if (block.getState() instanceof Sign) {
            Location signLocation = block.getLocation();

            for (Shop shop : ShopManager.getAllShops()) {
                if (shop.getSignLocation() != null && shop.getSignLocation().equals(signLocation)) {
                    event.setCancelled(true);
                    if (shop.getOwner().equals(player.getUniqueId()) || shop.getManagers().contains(player.getUniqueId())) {
                        ShopManager.openManagementGUI(player, shop);
                    } else {
                        if (shop.isEnabled()){
                            ShopManager.openCustomerGUI(player, shop);
                        } else {
                            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("shop_disabled"), player);
                        }
                    }
                }
            }
        }

    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getState() instanceof Sign) {
            Location eventLocation = block.getLocation();

            for (Shop shop : ShopManager.getAllShops()) {
                if (shop.getSignLocation() != null && shop.getSignLocation().equals(eventLocation)) {
                    event.setCancelled(true);
                    return;
                } else if (shop.getLocation().equals(eventLocation) && block.getType() == Material.TRAPPED_CHEST || block.getType() == Material.BARREL || block.getType() == Material.CHEST) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
