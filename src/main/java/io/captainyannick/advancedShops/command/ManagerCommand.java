package io.captainyannick.advancedShops.command;

import io.captainyannick.advancedShops.AdvancedShops;
import io.captainyannick.advancedShops.core.command.sCommand;
import io.captainyannick.advancedShops.core.utils.FormatUtils;
import io.captainyannick.advancedShops.shop.Shop;
import io.captainyannick.advancedShops.shop.ShopManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ManagerCommand extends sCommand {
    private AdvancedShops instance;

    public ManagerCommand(AdvancedShops instance) {
        super(false, false, "manager");
        this.instance = instance;
    }

    @Override
    protected void runCommand(CommandSender sender, String ... args) {
        Player player = (Player) sender;
        Player target;
        if (args.length == 3) {
            if (Bukkit.getPlayer(args[2]) != null) {
                target = Bukkit.getPlayer(args[2]);
            } else {
                FormatUtils.sendPrefixedMessage("&7Could not find player", player);
                return;
            }

            if (args[1].equalsIgnoreCase("add")) {
                handleAddManager(player, target);
            } else if (args[1].equalsIgnoreCase("remove")) {
                handleRemoveManager(player, target);
            } else if (args[1].equalsIgnoreCase("addall")) {
                handleAddAllManager(player, target);
            } else if (args[1].equalsIgnoreCase("removeall")) {
                handleRemoveAllManager(player, target);
            } else {
                FormatUtils.sendPrefixedMessage("&7" + getSyntax(), player);
            }
        } else {
            FormatUtils.sendPrefixedMessage("&7" + getSyntax(), player);
        }
    }

    @Override
    public String getPermissionNode() {
        return "advancedshops.managers";
    }

    @Override
    public String getSyntax() {
        return "/shop manager <Add|AddAll|Remove|RemoveAll> <Player>";
    }

    @Override
    public String getDescription() {
        return "Add / Remove Managers from your shops";
    }

    private void handleAddManager(Player player, Player targetPlayer) {
        List<Shop> playerShops = ShopManager.getShopsOwnedBy(player);

        if (playerShops.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not own any shops.");
            return;
        }

        Shop shop = ShopManager.getShopPlayerIsLookingAt(player);
        if (shop != null && shop.getOwner().equals(player.getUniqueId())) {
            shop.addManager(targetPlayer.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Added " + targetPlayer.getName() + " as a manager to this shop.");
        } else {
            player.sendMessage(ChatColor.RED + "You need to look at the shop you wish to add this user to");
        }
    }

    private void handleAddAllManager(Player player, Player targetPlayer) {
        List<Shop> playerShops = ShopManager.getShopsOwnedBy(player);

        if (playerShops.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not own any shops.");
            return;
        }

        for (Shop ownedShop : playerShops) {
            ownedShop.addManager(targetPlayer.getUniqueId());
        }
        player.sendMessage(ChatColor.GREEN + "Added " + targetPlayer.getName() + " as a manager to all your shops.");

    }

    private void handleRemoveManager(Player player, Player targetPlayer) {
        List<Shop> playerShops = ShopManager.getShopsOwnedBy(player);

        if (playerShops.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not own any shops.");
            return;
        }

        Shop shop = ShopManager.getShopPlayerIsLookingAt(player);
        if (shop != null && shop.getOwner().equals(player.getUniqueId())) {
            shop.removeManager(targetPlayer.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Removed " + targetPlayer.getName() + " as a manager from this shop.");
        } else {
            player.sendMessage(ChatColor.RED + "You need to look at the shop you wish to remove this user from");
        }
    }

    private void handleRemoveAllManager(Player player, Player targetPlayer) {
        List<Shop> playerShops = ShopManager.getShopsOwnedBy(player);

        if (playerShops.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not own any shops.");
            return;
        }

        for (Shop ownedShop : playerShops) {
            ownedShop.removeManager(targetPlayer.getUniqueId());
        }
        player.sendMessage(ChatColor.GREEN + "Removed " + targetPlayer.getName() + " as a manager from all your shops.");

    }
}