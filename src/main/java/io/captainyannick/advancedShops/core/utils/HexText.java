package io.captainyannick.advancedShops.core.utils;

import net.md_5.bungee.api.ChatColor;

public class HexText {
    private final String text;

    public HexText(String text) {
        this.text = text;
    }

    public String toString() {
        return this.text;
    }

    public HexText translateColorCodes() {
        return new HexText(ChatColor.translateAlternateColorCodes('&', this.text));
    }

    public HexText parseHex() {
        return new HexText(HexParser.parseHexText(this.text, HexParser.findHexIndexes(this.text)).text);
    }

    public HexText append(String text) {
        return new HexText(this.text + text);
    }

    public HexText append(HexText hexText) {
        return this.append(hexText.text);
    }
}
