package me.blek.utils;

import me.blek.jetpack.Jetpack;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;


public class Common {

    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

}
