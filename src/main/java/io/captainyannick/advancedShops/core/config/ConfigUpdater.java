package io.captainyannick.advancedShops.core.config;

import com.google.common.base.Preconditions;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class ConfigUpdater {
    private static final char SEPARATOR = '.';

    public static void update(Plugin plugin, String resourceName, File toUpdate, String ... ignoredSections) throws IOException {
        ConfigUpdater.update(plugin, resourceName, toUpdate, Arrays.asList(ignoredSections));
    }

    public static void update(Plugin plugin, String resourceName, File toUpdate, List<String> ignoredSections) throws IOException {
        Preconditions.checkArgument(toUpdate.exists(), "The toUpdate file doesn't exist!");
        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(plugin.getResource(resourceName), StandardCharsets.UTF_8));
        YamlConfiguration currentConfig = YamlConfiguration.loadConfiguration((File)toUpdate);
        Map<String, String> comments = ConfigUpdater.parseComments(plugin, resourceName, (FileConfiguration)defaultConfig);
        Map<String, String> ignoredSectionsValues = ConfigUpdater.parseIgnoredSections(toUpdate, (FileConfiguration)currentConfig, comments, ignoredSections == null ? Collections.emptyList() : ignoredSections);
        StringWriter writer = new StringWriter();
        ConfigUpdater.write((FileConfiguration)defaultConfig, (FileConfiguration)currentConfig, new BufferedWriter(writer), comments, ignoredSectionsValues);
        String value = writer.toString();
        Path toUpdatePath = toUpdate.toPath();
        if (!value.equals(new String(Files.readAllBytes(toUpdatePath), StandardCharsets.UTF_8))) {
            Files.write(toUpdatePath, value.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
        }
    }

    private static void write(FileConfiguration defaultConfig, FileConfiguration currentConfig, BufferedWriter writer, Map<String, String> comments, Map<String, String> ignoredSectionsValues) throws IOException {
        YamlConfiguration parserConfig = new YamlConfiguration();
        block0: for (String fullKey : defaultConfig.getKeys(true)) {
            String indents = KeyBuilder.getIndents(fullKey, '.');
            if (ignoredSectionsValues.isEmpty()) {
                ConfigUpdater.writeCommentIfExists(comments, writer, fullKey, indents);
            } else {
                for (Map.Entry<String, String> entry : ignoredSectionsValues.entrySet()) {
                    if (entry.getKey().equals(fullKey)) {
                        writer.write(entry.getValue() + "\n");
                        continue block0;
                    }
                    if (KeyBuilder.isSubKeyOf(entry.getKey(), fullKey, '.')) continue block0;
                    ConfigUpdater.writeCommentIfExists(comments, writer, fullKey, indents);
                }
            }
            Object currentValue = currentConfig.get(fullKey);
            if (currentValue == null) {
                currentValue = defaultConfig.get(fullKey);
            }
            String[] splitFullKey = fullKey.split("[.]");
            String trailingKey = splitFullKey[splitFullKey.length - 1];
            if (currentValue instanceof ConfigurationSection) {
                writer.write(indents + trailingKey + ":");
                if (!((ConfigurationSection)currentValue).getKeys(false).isEmpty()) {
                    writer.write("\n");
                    continue;
                }
                writer.write(" {}\n");
                continue;
            }
            parserConfig.set(trailingKey, currentValue);
            String yaml = parserConfig.saveToString();
            yaml = yaml.substring(0, yaml.length() - 1).replace("\n", "\n" + indents);
            String toWrite = indents + yaml + "\n";
            parserConfig.set(trailingKey, null);
            writer.write(toWrite);
        }
        String danglingComments = comments.get(null);
        if (danglingComments != null) {
            writer.write(danglingComments);
        }
        writer.close();
    }

    private static Map<String, String> parseComments(Plugin plugin, String resourceName, FileConfiguration defaultConfig) throws IOException {
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(plugin.getResource(resourceName)));
        LinkedHashMap<String, String> comments = new LinkedHashMap<String, String>();
        StringBuilder commentBuilder = new StringBuilder();
        KeyBuilder keyBuilder = new KeyBuilder(defaultConfig, '.');
        while ((line = reader.readLine()) != null) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("-")) continue;
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                commentBuilder.append(trimmedLine).append("\n");
                continue;
            }
            keyBuilder.parseLine(trimmedLine);
            String key = keyBuilder.toString();
            if (commentBuilder.length() > 0) {
                comments.put(key, commentBuilder.toString());
                commentBuilder.setLength(0);
            }
            if (keyBuilder.isConfigSectionWithKeys()) continue;
            keyBuilder.removeLastKey();
        }
        reader.close();
        if (commentBuilder.length() > 0) {
            comments.put(null, commentBuilder.toString());
        }
        return comments;
    }

    private static Map<String, String> parseIgnoredSections(File toUpdate, FileConfiguration currentConfig, Map<String, String> comments, List<String> ignoredSections) throws IOException {
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(toUpdate));
        LinkedHashMap<String, String> ignoredSectionsValues = new LinkedHashMap<String, String>(ignoredSections.size());
        KeyBuilder keyBuilder = new KeyBuilder(currentConfig, '.');
        StringBuilder valueBuilder = new StringBuilder();
        String currentIgnoredSection = null;
        block0: while ((line = reader.readLine()) != null) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) continue;
            if (trimmedLine.startsWith("-")) {
                for (String ignoredSection : ignoredSections) {
                    boolean isIgnoredParent = ignoredSection.equals(keyBuilder.toString());
                    if (!isIgnoredParent && !keyBuilder.isSubKeyOf(ignoredSection)) continue;
                    valueBuilder.append("\n").append(line);
                    continue block0;
                }
            }
            keyBuilder.parseLine(trimmedLine);
            String fullKey = keyBuilder.toString();
            if (currentIgnoredSection != null && !KeyBuilder.isSubKeyOf(currentIgnoredSection, fullKey, '.')) {
                ignoredSectionsValues.put(currentIgnoredSection, valueBuilder.toString());
                valueBuilder.setLength(0);
                currentIgnoredSection = null;
            }
            for (String ignoredSection : ignoredSections) {
                String comment;
                boolean isIgnoredParent = ignoredSection.equals(fullKey);
                if (!isIgnoredParent && !keyBuilder.isSubKeyOf(ignoredSection)) continue;
                if (valueBuilder.length() > 0) {
                    valueBuilder.append("\n");
                }
                if ((comment = comments.get(fullKey)) != null) {
                    String indents = KeyBuilder.getIndents(fullKey, '.');
                    valueBuilder.append(indents).append(comment.replace("\n", "\n" + indents));
                    valueBuilder.setLength(valueBuilder.length() - indents.length());
                }
                valueBuilder.append(line);
                if (!isIgnoredParent) continue block0;
                currentIgnoredSection = fullKey;
                continue block0;
            }
        }
        reader.close();
        if (valueBuilder.length() > 0) {
            ignoredSectionsValues.put(currentIgnoredSection, valueBuilder.toString());
        }
        return ignoredSectionsValues;
    }

    private static void writeCommentIfExists(Map<String, String> comments, BufferedWriter writer, String fullKey, String indents) throws IOException {
        String comment = comments.get(fullKey);
        if (comment != null) {
            writer.write(indents + comment.substring(0, comment.length() - 1).replace("\n", "\n" + indents) + "\n");
        }
    }

    private static void removeLastKey(StringBuilder keyBuilder) {
        if (keyBuilder.length() == 0) {
            return;
        }
        String keyString = keyBuilder.toString();
        String[] split = keyString.split("[.]");
        int minIndex = Math.max(0, keyBuilder.length() - split[split.length - 1].length() - 1);
        keyBuilder.replace(minIndex, keyBuilder.length(), "");
    }

    private static void appendNewLine(StringBuilder builder) {
        if (builder.length() > 0) {
            builder.append("\n");
        }
    }
}
