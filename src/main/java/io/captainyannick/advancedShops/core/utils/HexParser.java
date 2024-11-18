package io.captainyannick.advancedShops.core.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class HexParser {
    static List<Integer> findHexIndexes(String text) {
        List<Integer> indexes = new ArrayList();
        int i = 0;

        while (true) {
            int index = text.indexOf("&#", i);
            if (index == -1) {
                return indexes;
            }

            indexes.add(index);
            ++i;
        }
    }

    static HexText parseHexText(String text, List<Integer> indexes) {
        StringBuilder newText = new StringBuilder();
        StringBuilder currentHex = new StringBuilder();
        boolean isInHex = false;

        for (int i = 0; i < text.length(); ++i) {
            if (indexes.contains(i)) {
                isInHex = true;
            } else if (isInHex) {
                currentHex.append(text.charAt(i));
                if (currentHex.length() == 7) {
                    isInHex = false;
                    newText.append(ChatColor.of(String.valueOf(currentHex)).toString());
                    currentHex.setLength(0);
                }
            } else {
                newText.append(text.charAt(i));
            }
        }

        return new HexText(newText.toString());
    }
}
