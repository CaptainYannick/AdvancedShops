package io.captainyannick.advancedShops.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.command.CommandSender;

public abstract class sCommand {
    private String command;
    private boolean allowConsole;
    private List<String> subCommand = new ArrayList<String>();

    protected sCommand(boolean mainCommand, boolean allowConsole, String ... command) {
        this.allowConsole = allowConsole;
        if (!mainCommand) {
            this.subCommand = Arrays.asList(command);
        } else {
            this.command = Arrays.asList(command).get(0);
        }
    }

    public String getCommand() {
        return this.command;
    }

    public List<String> getSubCommand() {
        return this.subCommand;
    }

    public void addSubCommand(String command) {
        this.subCommand.add(command);
    }

    public boolean allowConsole() {
        return this.allowConsole;
    }

    protected abstract void runCommand(CommandSender var1, String ... var2);

    public abstract String getPermissionNode();

    public abstract String getSyntax();

    public abstract String getDescription();
}
