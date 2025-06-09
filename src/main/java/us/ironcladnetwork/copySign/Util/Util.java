package us.ironcladnetwork.copySign.Util;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class providing common helper methods for the CopySign plugin.
 * <p>
 * This class contains static utility methods for:
 * <ul>
 *   <li>Color code translation and hex color support</li>
 *   <li>Sign text processing and color preservation</li>
 *   <li>String manipulation for sign content</li>
 * </ul>
 * 
 * @author IroncladNetwork
 * @since 2.0.0
 */
public class Util {
    // Pattern for hex color codes like &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    /**
     * Translates color codes and hex colors in a string
     * @param text The text to colorize
     * @return The colorized text
     */
    public static String colorize(String text) {
        if (text == null) return "";
        
        // Convert hex colors (&#RRGGBB)
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String color = matcher.group(1);
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + color).toString());
        }
        matcher.appendTail(buffer);
        
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
    
    /**
     * Preserves colors between lines when copying sign text
     * @param lines The sign lines to process
     * @return Processed lines with preserved colors
     */
    public static String[] preserveColors(String[] lines) {
        String lastColors = "";
        String[] result = new String[lines.length];
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.isEmpty()) {
                result[i] = lastColors;
                continue;
            }
            
            result[i] = lastColors + line;
            lastColors = ChatColor.getLastColors(line);
        }
        
        return result;
    }
}
