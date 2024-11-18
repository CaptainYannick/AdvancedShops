package io.captainyannick.advancedShops.core.config;

import org.bukkit.configuration.file.FileConfiguration;

public class KeyBuilder
        implements Cloneable {
    private final FileConfiguration config;
    private final char separator;
    private final StringBuilder builder;

    public KeyBuilder(FileConfiguration config, char separator) {
        this.config = config;
        this.separator = separator;
        this.builder = new StringBuilder();
    }

    private KeyBuilder(KeyBuilder keyBuilder) {
        this.config = keyBuilder.config;
        this.separator = keyBuilder.separator;
        this.builder = new StringBuilder(keyBuilder.toString());
    }

    public void parseLine(String line) {
        line = line.trim();
        String[] currentSplitLine = line.split(":");
        while (this.builder.length() > 0 && !this.config.contains(this.builder.toString() + this.separator + currentSplitLine[0])) {
            this.removeLastKey();
        }
        if (this.builder.length() > 0) {
            this.builder.append(this.separator);
        }
        this.builder.append(currentSplitLine[0]);
    }

    public String getLastKey() {
        if (this.builder.length() == 0) {
            return "";
        }
        return this.builder.toString().split("[" + this.separator + "]")[0];
    }

    public boolean isEmpty() {
        return this.builder.length() == 0;
    }

    public boolean isSubKeyOf(String parentKey) {
        return KeyBuilder.isSubKeyOf(parentKey, this.builder.toString(), this.separator);
    }

    public boolean isSubKey(String subKey) {
        return KeyBuilder.isSubKeyOf(this.builder.toString(), subKey, this.separator);
    }

    public static boolean isSubKeyOf(String parentKey, String subKey, char separator) {
        if (parentKey.isEmpty()) {
            return false;
        }
        return subKey.startsWith(parentKey) && subKey.substring(parentKey.length()).startsWith(String.valueOf(separator));
    }

    public static String getIndents(String key, char separator) {
        String[] splitKey = key.split("[" + separator + "]");
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < splitKey.length; ++i) {
            builder.append("  ");
        }
        return builder.toString();
    }

    public boolean isConfigSection() {
        String key = this.builder.toString();
        return this.config.isConfigurationSection(key);
    }

    public boolean isConfigSectionWithKeys() {
        String key = this.builder.toString();
        return this.config.isConfigurationSection(key) && !this.config.getConfigurationSection(key).getKeys(false).isEmpty();
    }

    public void removeLastKey() {
        if (this.builder.length() == 0) {
            return;
        }
        String keyString = this.builder.toString();
        String[] split = keyString.split("[" + this.separator + "]");
        int minIndex = Math.max(0, this.builder.length() - split[split.length - 1].length() - 1);
        this.builder.replace(minIndex, this.builder.length(), "");
    }

    public String toString() {
        return this.builder.toString();
    }

    protected KeyBuilder clone() {
        return new KeyBuilder(this);
    }
}

