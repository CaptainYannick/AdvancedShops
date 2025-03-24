package io.captainyannick.advancedShops.command;

import io.captainyannick.advancedShops.AdvancedShops;
import io.captainyannick.advancedShops.core.command.sCommand;
import io.captainyannick.advancedShops.core.utils.FormatUtils;
import io.captainyannick.advancedShops.shop.ShopManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public class AdminRemoveCommand extends sCommand {
    private AdvancedShops instance;

    public AdminRemoveCommand(AdvancedShops instance) {
        super(false, false, "create");
        this.instance = instance;
    }

    @Override
    protected void runCommand(CommandSender sender, String ... args) {
        Player player = (Player) sender;
        if (ShopManager.getShopAtLocation(Objects.requireNonNull(player.getTargetBlockExact(5)).getLocation()) == null) {
            ShopManager.deleteShop(ShopManager.getShopAtLocation(Objects.requireNonNull(player.getTargetBlockExact(5)).getLocation()));
            FormatUtils.sendPrefixedMessage(instance.getMessageConfig().getString("admin_shop_deleted"), player);
        }
    }

    @Override
    public String getPermissionNode() {
        return "advancedshops.admin.remove";
    }

    @Override
    public String getSyntax() {
        return "/shop remove";
    }

    @Override
    public String getDescription() {
        return "Remove the shop you are looking at (admin command)";
    }
}
