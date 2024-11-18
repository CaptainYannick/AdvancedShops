package io.captainyannick.advancedShops.command;

import io.captainyannick.advancedShops.AdvancedShops;
import io.captainyannick.advancedShops.core.utils.FormatUtils;
import io.captainyannick.advancedShops.shop.ShopManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommandExecutor implements CommandExecutor {

    private final AdvancedShops plugin;

    public ShopCommandExecutor(AdvancedShops plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /shop <create|delete|buy|sell|info>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                ShopManager.createShop(player, args);
                break;
            case "logs":
                ShopManager.showTransactionLogs(player);
                break;
            default:
                FormatUtils.sendPrefixedMessage("&7Command Overview \n &d/shop &8- &7Shows this screen", player);
                break;
        }
        return true;
    }
}
