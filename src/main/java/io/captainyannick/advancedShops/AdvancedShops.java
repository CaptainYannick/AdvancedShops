package io.captainyannick.advancedShops;

import io.captainyannick.advancedShops.command.*;
import io.captainyannick.advancedShops.core.Metrics;
import io.captainyannick.advancedShops.core.command.CommandManager;
import io.captainyannick.advancedShops.core.config.Config;
import io.captainyannick.advancedShops.core.config.ConfigUpdater;
import io.captainyannick.advancedShops.listeners.GUIListener;
import io.captainyannick.advancedShops.listeners.ShopListener;
import io.captainyannick.advancedShops.shop.ShopManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;

import java.io.IOException;

public class AdvancedShops extends JavaPlugin {

    private static Economy economy = null;
    private static AdvancedShops instance;
    private Config dataFile;
    private Config mainConfig;
    private Config messageConfig;
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;
        this.mainConfig = new Config(this, "config.yml");
        this.dataFile = new Config(this, "data.yml");
        this.messageConfig = new Config(this, "messages.yml");
        setupEconomy();


        this.commandManager = new CommandManager(this);
        this.commandManager.setMainCommand(new ShopCommand(this))
                .addSubCommand(new CreateCommand(this))
                .addSubCommand(new ReloadCommand(this))
                .addSubCommand(new ManagerCommand(this))
                .addSubCommand(new AdminRemoveCommand(this));

        this.updateConfigs();
        ShopManager.initialize(this);

        new Metrics(this, 23856);

        getServer().getPluginManager().registerEvents(new ShopListener(), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        updateConfigs();

        getLogger().info("AdvancedShops has been enabled!");
    }

    @Override
    public void onDisable() {
        ShopManager.saveShops();
        getLogger().info("AdvancedShops has been disabled and shop data saved.");
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault is required but not found!");
            return;
        }
        economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
    }

    public void updateConfigs() {
        try {
            ConfigUpdater.update(this, "config.yml", this.mainConfig.getFile(), new String[0]);
            this.mainConfig = new Config(this, "config.yml");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            ConfigUpdater.update(this, "messages.yml", this.messageConfig.getFile(), new String[0]);
            this.messageConfig = new Config(this, "messages.yml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        this.mainConfig = new Config(this, "config.yml");
        this.messageConfig = new Config(this, "messages.yml");
    }

    public static Economy getEconomy() {
        return economy;
    }

    public YamlConfiguration getMainConfig() {
        return this.mainConfig.getConfig();
    }

    public YamlConfiguration getMessageConfig() {
        return this.messageConfig.getConfig();
    }

    public Config getDataFileRaw() {
        return this.dataFile;
    }

    public YamlConfiguration getDataFile() {
        return this.dataFile.getConfig();
    }

    public static AdvancedShops getInstance() {
        return instance;
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }

}