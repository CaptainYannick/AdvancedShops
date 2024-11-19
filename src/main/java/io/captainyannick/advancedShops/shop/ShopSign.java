package io.captainyannick.advancedShops.shop;

import io.captainyannick.advancedShops.AdvancedShops;
import io.captainyannick.advancedShops.core.utils.FormatUtils;
import io.captainyannick.advancedShops.core.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

public class ShopSign {

    public static boolean createShopSign(Shop shop, Player player) {
        Block chestBlock = shop.getLocation().getBlock();
        BlockFace playerFacing = getPlayerFacingBlock(chestBlock, player);

        if (playerFacing == null) {
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("sign_creation_side"), player);
            Bukkit.getLogger().warning("Player facing is null. Chest location: " + chestBlock.getLocation());
            return false;
        }

        Block signBlock = chestBlock.getRelative(playerFacing);

        if (!signBlock.getType().isAir() && !signBlock.isReplaceable()) {
            FormatUtils.sendPrefixedMessage(AdvancedShops.getInstance().getMessageConfig().getString("sign_creation_obstructed"), player);
            Bukkit.getLogger().warning("Sign block is obstructed. Location: " + signBlock.getLocation());
            return false;
        }

        signBlock.setType(Material.OAK_WALL_SIGN);
        org.bukkit.block.data.type.WallSign wallSignData = (org.bukkit.block.data.type.WallSign) signBlock.getBlockData();
        wallSignData.setFacing(playerFacing);
        signBlock.setBlockData(wallSignData);

        updateShopSign(signBlock, shop);

        shop.setSignLocation(signBlock.getLocation());
        return true;
    }

    public static void updateShopSign(Block signBlock, Shop shop) {
        if (!(signBlock.getState() instanceof Sign)) {
            Bukkit.getLogger().warning("Sign block is not a valid sign at " + signBlock.getLocation());
            return;
        }
        Sign sign = (Sign) signBlock.getState();
        sign.setLine(0, shop.getOwnerName());

        if (shop.getBuyPrice() > 0 && shop.getSellPrice() > 0) {
            sign.setLine(1, "B" + shop.getBuyPrice() + " | S" + shop.getSellPrice());
        } else if (shop.getBuyPrice() > 0 && shop.getSellPrice() == 0) {
            sign.setLine(1, "B" + shop.getBuyPrice());
        } else if (shop.getSellPrice() > 0 && shop.getBuyPrice() == 0) {
            sign.setLine(1, "S" + shop.getSellPrice());
        }
        sign.setLine(2, TextUtils.formatItemName(shop.getItem()));
        if (shop.isEnabled()) {
            String stockLine = "Stock " + shop.getStock();
            if (shop.getStock() > 0) {
                sign.setLine(3, ChatColor.GREEN + stockLine);
            } else {
                sign.setLine(3, ChatColor.RED + stockLine);
            }
        } else {
            sign.setLine(3, ChatColor.RED+ "Shop Disabled");
        }

        sign.update();
    }

    private static BlockFace getPlayerFacingBlock(Block chestBlock, Player player) {
        Location playerLocation = player.getLocation();
        Location chestLocation = chestBlock.getLocation();

        double deltaX = playerLocation.getX() - chestLocation.getX();
        double deltaZ = playerLocation.getZ() - chestLocation.getZ();

        if (Math.abs(deltaX) > Math.abs(deltaZ)) {
            return deltaX > 0 ? BlockFace.EAST : BlockFace.WEST;
        } else {
            return deltaZ > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }
    }

    public static void removeShopSign(Shop shop) {
        Location signLocation = shop.getSignLocation();
        if (signLocation == null) return;

        Block signBlock = signLocation.getBlock();
        if (signBlock.getType().toString().contains("_SIGN")) {
            signBlock.setType(Material.AIR);
            shop.setSignLocation(null);
        }
    }
}
