package io.captainyannick.advancedShops.core.config;

import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class Config {
    private final File file;
    private final YamlConfiguration config;

    public Config(Plugin plugin, String configName) {
        this.file = new File(plugin.getDataFolder(), configName);
        if (!this.file.exists()) {
            this.file.getParentFile().mkdir();
            plugin.saveResource(configName, true);
        }
        this.config = new YamlConfiguration();
        try {
            this.config.load(this.file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            this.config.save(this.file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File getFile() {
        return this.file;
    }

    public YamlConfiguration getConfig() {
        return this.config;
    }
}
