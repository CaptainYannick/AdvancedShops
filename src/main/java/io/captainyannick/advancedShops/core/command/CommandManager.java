package io.captainyannick.advancedShops.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.captainyannick.advancedShops.AdvancedShops;
import io.captainyannick.advancedShops.core.utils.FormatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandManager
        implements CommandExecutor {
    private AdvancedShops plugin;
    private sCommand mainCommand;
    private List<sCommand> commands = new ArrayList<>();

    public CommandManager(AdvancedShops plugin) {
        this.plugin = plugin;
    }

    private void processRequirements(sCommand command, CommandSender sender, String[] strings) {
        if (!(sender instanceof Player) && command.allowConsole()) {
            sender.sendMessage("You must be a player to use this command.");
            return;
        }
        if (command.getPermissionNode() == null || sender.hasPermission(command.getPermissionNode())) {
            command.runCommand(sender, strings);
            return;
        }
        FormatUtils.sendPrefixedMessage(plugin.getMessageConfig().getString("no_permission"), sender);
    }

    public List<sCommand> getCommands() {
        return Collections.unmodifiableList(this.commands);
    }

    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        for (sCommand sCommand2 : this.commands) {
            if (sCommand2.getCommand() != null && sCommand2.getCommand().equalsIgnoreCase(command.getName().toLowerCase())) {
                if (strings.length != 0) continue;
                this.processRequirements(sCommand2, sender, strings);
                return true;
            }
            if (strings.length == 0 || this.getMainCommand() == null || !this.getMainCommand().getCommand().equalsIgnoreCase(command.getName())) continue;
            String cmd = strings[0];
            String cmd2 = strings.length >= 2 ? String.join(" ", strings[0], strings[1]) : null;
            for (String cmds : sCommand2.getSubCommand()) {
                if (!cmd.equalsIgnoreCase(cmds) && (cmd2 == null || !cmd2.equalsIgnoreCase(cmds))) continue;
                this.processRequirements(sCommand2, sender, strings);
                return true;
            }
        }
        FormatUtils.sendPrefixedMessage(plugin.getMessageConfig().getString("incorrect_command"), sender);
        return true;
    }

    public CommandManager setMainCommand(sCommand command) {
        this.plugin.getCommand(command.getCommand()).setExecutor(this);
        this.mainCommand = command;
        this.commands.add(command);
        return this;
    }

    public CommandManager addSubCommand(sCommand command) {
        this.commands.add(command);
        return this;
    }

    public sCommand getMainCommand() {
        return this.mainCommand;
    }
}