package io.captainyannick.advancedShops.command;

import io.captainyannick.advancedShops.AdvancedShops;
import io.captainyannick.advancedShops.core.command.sCommand;
import io.captainyannick.advancedShops.core.utils.FormatUtils;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends sCommand {
    private AdvancedShops instance;

    public ReloadCommand(AdvancedShops instance) {
        super(false, false, "reload");
        this.instance = instance;
    }

    @Override
    protected void runCommand(CommandSender sender, String ... args) {
        this.instance.reload();
        FormatUtils.sendPrefixedMessage("&7Succesfully reloaded the plugin!", sender);
    }

    @Override
    public String getPermissionNode() {
        return "advancedshops.admin";
    }

    @Override
    public String getSyntax() {
        return "/shop reload";
    }

    @Override
    public String getDescription() {
        return "Reload the plugin";
    }
}
