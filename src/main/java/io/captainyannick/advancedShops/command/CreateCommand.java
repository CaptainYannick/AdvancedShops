package io.captainyannick.advancedShops.command;

import io.captainyannick.advancedShops.AdvancedShops;
import io.captainyannick.advancedShops.core.command.sCommand;
import io.captainyannick.advancedShops.core.utils.FormatUtils;
import io.captainyannick.advancedShops.shop.ShopManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateCommand extends sCommand {
    private AdvancedShops instance;

    public CreateCommand(AdvancedShops instance) {
        super(false, false, "create");
        this.instance = instance;
    }

    @Override
    protected void runCommand(CommandSender sender, String ... args) {
        Player player = (Player) sender;
        if (args.length == 3) {
            ShopManager.createShop(player, args);
        } else {
            FormatUtils.sendPrefixedMessage("&7" + getSyntax(), player);
        }
    }

    @Override
    public String getPermissionNode() {
        return "advancedshops.create";
    }

    @Override
    public String getSyntax() {
        return "/shop create <Buy Price> <Sell Price>";
    }

    @Override
    public String getDescription() {
        return "Create a shop. To disable buying/selling set the value to 0";
    }
}
