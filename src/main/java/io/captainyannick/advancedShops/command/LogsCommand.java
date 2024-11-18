package io.captainyannick.advancedShops.command;

import io.captainyannick.advancedShops.AdvancedShops;
import io.captainyannick.advancedShops.core.command.sCommand;
import io.captainyannick.advancedShops.shop.ShopManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LogsCommand extends sCommand {
    private AdvancedShops instance;

    public LogsCommand(AdvancedShops instance) {
        super(false, false, "logs");
        this.instance = instance;
    }

    @Override
    protected void runCommand(CommandSender sender, String ... args) {
        Player player = (Player) sender;
        ShopManager.showTransactionLogs(player);
    }

    @Override
    public String getPermissionNode() {
        return "";
    }

    @Override
    public String getSyntax() {
        return "/shop logs";
    }

    @Override
    public String getDescription() {
        return "View your shop logs";
    }
}
