package io.captainyannick.advancedShops.core.utils;

import io.captainyannick.advancedShops.AdvancedShops;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FormatUtils {
    public static void sendPrefixedMessage(String message, Player player) {
        player.sendMessage(TextUtils.formatText(AdvancedShops.getInstance().getMessageConfig().getString("prefix") + "&r " + message));
    }

    public static void sendPrefixedMessage(String message, CommandSender sender) {
        sender.sendMessage(TextUtils.formatText(AdvancedShops.getInstance().getMessageConfig().getString("prefix") + "&r " + message));
    }
}
