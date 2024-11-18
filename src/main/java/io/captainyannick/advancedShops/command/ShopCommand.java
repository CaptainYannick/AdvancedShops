package io.captainyannick.advancedShops.command;

import io.captainyannick.advancedShops.AdvancedShops;
import io.captainyannick.advancedShops.core.command.sCommand;
import io.captainyannick.advancedShops.core.utils.FormatUtils;
import io.captainyannick.advancedShops.core.utils.TextUtils;
import org.bukkit.command.CommandSender;

public class ShopCommand extends sCommand {

    private final AdvancedShops instance;

    public ShopCommand(AdvancedShops instance) {
        super(true, true, "shop");
        this.instance = instance;
    }

    @Override
    protected void runCommand(CommandSender sender, String ... args) {
        sender.sendMessage("");
        FormatUtils.sendPrefixedMessage("&7Version &e" + this.instance.getDescription().getVersion(), sender);
        for (sCommand command : this.instance.getCommandManager().getCommands()) {
            if (command.getPermissionNode() != null && !sender.hasPermission(command.getPermissionNode())) continue;
            sender.sendMessage(TextUtils.formatText("&8 - &d" + command.getSyntax() + "&8 - &7" + command.getDescription()));
        }
        sender.sendMessage("");
    }

    @Override
    public String getPermissionNode() {
        return null;
    }

    @Override
    public String getSyntax() {
        return "/shop";
    }

    @Override
    public String getDescription() {
        return "Shows all commands";
    }
}
