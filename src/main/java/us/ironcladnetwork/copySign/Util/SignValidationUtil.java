package us.ironcladnetwork.copySign.Util;

import org.bukkit.DyeColor;
import us.ironcladnetwork.copySign.CopySign;

import java.util.List;

/**
 * Utility class for sign-related validation operations.
 * Centralizes common validation logic to avoid code duplication.
 */
public class SignValidationUtil {
    
    /**
     * Checks if a sign type is allowed based on configuration.
     * 
     * @param signType The sign type to check (e.g., "OAK_SIGN", "BIRCH_WALL_SIGN")
     * @return true if the sign type is allowed, false otherwise
     */
    public static boolean isSignTypeAllowed(String signType) {
        // Use ConfigManager for thread-safe access
        List<String> allowedTypes = CopySign.getInstance().getConfigManager().getAllowedSignTypes();
        
        // If the list is empty, allow all sign types (default behavior)
        if (allowedTypes.isEmpty()) {
            return true;
        }
        
        // Check if the specific sign type is in the allowed list
        return allowedTypes.contains(signType);
    }
    
    /**
     * Validates a sign name for saving/loading operations.
     * 
     * @param name The name to validate
     * @return true if the name is valid, false otherwise
     */
    public static boolean isValidSignName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // Get max length from config
        int maxLength = CopySign.getInstance().getConfigManager().getMaxSignNameLength();
        
        // Length validation
        if (name.length() > maxLength) {
            return false;
        }
        
        // Character validation - allow only alphanumeric, hyphens, and underscores
        if (!name.matches("^[a-zA-Z0-9_-]+$")) {
            return false;
        }
        
        // Check against reserved names from config
        List<String> reservedNames = CopySign.getInstance().getConfigManager().getReservedNames();
        String lowerName = name.toLowerCase();
        
        // Check if name is in reserved list
        if (reservedNames.contains(lowerName)) {
            return false;
        }
        
        // Also prevent Windows reserved names
        if (lowerName.equals("con") || lowerName.equals("prn") || lowerName.equals("aux") || 
            lowerName.equals("nul") || lowerName.startsWith("com") || lowerName.startsWith("lpt")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates and returns a DyeColor from a string.
     * Properly handles all valid Minecraft dye colors.
     * 
     * @param colorString The color string to validate
     * @return A valid DyeColor, or null if the string doesn't represent a valid color
     */
    public static DyeColor getValidDyeColor(String colorString) {
        if (colorString == null || colorString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // This will properly handle all valid dye colors including BLACK
            return DyeColor.valueOf(colorString.toUpperCase());
        } catch (IllegalArgumentException ex) {
            // Return null for invalid colors
            return null;
        }
    }
    
    /**
     * Checks if a string represents a valid DyeColor.
     * 
     * @param color The color string to check
     * @return true if it's a valid DyeColor or known fallback, false otherwise
     */
    public static boolean isValidDyeColor(String color) {
        if (color == null || color.trim().isEmpty()) {
            return false;
        }
        
        // Try to parse as DyeColor
        try {
            DyeColor.valueOf(color.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            // Check for known fallbacks
            return color.equalsIgnoreCase("oak");
        }
    }
    
    /**
     * Gets the display name for a dye color.
     * Formats the color name for display in messages and GUIs.
     * 
     * @param color The DyeColor to format
     * @return A formatted display name
     */
    public static String getColorDisplayName(DyeColor color) {
        if (color == null) {
            return "None";
        }
        
        // Convert enum name to title case
        String name = color.name().toLowerCase().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1));
        }
        
        return result.toString();
    }
}