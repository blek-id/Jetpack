package me.blekdigits.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public class Utils {
    
    public static String colorize(String msg) {
        Matcher braceMatch = Pattern.compile("\\{#([a-fA-F0-9]{6})\\}").matcher(msg);
        StringBuffer buffer = new StringBuffer();
        while (braceMatch.find()) {
            String color = "#" + braceMatch.group(1);
            braceMatch.appendReplacement(buffer, ChatColor.of(color).toString());
        }
        braceMatch.appendTail(buffer);

        msg = buffer.toString();

        Matcher directMatch = Pattern.compile("#[a-fA-F0-9]{6}").matcher(msg);
        while (directMatch.find()) {
            String color = msg.substring(directMatch.start(), directMatch.end());
            msg = msg.replace(color, String.valueOf(ChatColor.of(color)));
            directMatch = Pattern.compile("#[a-fA-F0-9]{6}").matcher(msg);
        }

        return ChatColor.translateAlternateColorCodes('&', msg);
    }
    
    // Add more utility methods here
}