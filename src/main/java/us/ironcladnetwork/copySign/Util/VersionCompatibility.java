package us.ironcladnetwork.copySign.Util;

import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;

/**
 * Version compatibility layer for handling differences between Minecraft versions.
 * Provides safe access to potentially deprecated or version-specific API methods.
 */
public class VersionCompatibility {
    
    private static final String BUKKIT_VERSION = Bukkit.getBukkitVersion();
    private static final boolean IS_LEGACY_VERSION = isLegacyVersion();
    
    /**
     * Safely gets the glow state of a sign, handling deprecated API usage.
     * 
     * @param sign The sign to check
     * @return true if the sign is glowing, false otherwise
     */
    public static boolean isSignGlowing(Sign sign) {
        if (sign == null) {
            return false;
        }
        
        try {
            if (IS_LEGACY_VERSION) {
                // For older versions, use the deprecated method with suppression
                @SuppressWarnings("deprecation")
                boolean glowing = sign.isGlowingText();
                return glowing;
            } else {
                // For newer versions, use the side-specific API
                return sign.getSide(Side.FRONT).isGlowingText();
            }
        } catch (NoSuchMethodError | AbstractMethodError e) {
            // Fallback for unexpected API changes
            ErrorHandler.debug("API compatibility issue when checking sign glow state: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Safely sets the glow state of a sign, handling deprecated API usage.
     * 
     * @param sign The sign to modify
     * @param glowing Whether the sign should glow
     */
    public static void setSignGlowing(Sign sign, boolean glowing) {
        if (sign == null) {
            return;
        }
        
        try {
            if (IS_LEGACY_VERSION) {
                // For older versions, use the deprecated method with suppression
                @SuppressWarnings("deprecation")
                boolean ignored = sign.isGlowingText(); // Test if method exists
                // Note: Legacy versions may not support setting glow state
                ErrorHandler.debug("Legacy version detected, glow state may not be settable");
            } else {
                // For newer versions, use the side-specific API
                sign.getSide(Side.FRONT).setGlowingText(glowing);
                sign.getSide(Side.BACK).setGlowingText(glowing);
            }
        } catch (NoSuchMethodError | AbstractMethodError e) {
            // Fallback for unexpected API changes
            ErrorHandler.debug("API compatibility issue when setting sign glow state: " + e.getMessage());
        }
    }
    
    /**
     * Checks if we're running on a legacy version that uses deprecated sign APIs.
     * 
     * @return true if running on a legacy version
     */
    private static boolean isLegacyVersion() {
        try {
            // Try to access the newer side-specific API
            Sign.class.getMethod("getSide", Side.class);
            return false; // New API available
        } catch (NoSuchMethodException e) {
            return true; // Old API only
        }
    }
    
    /**
     * Gets the server version for debugging and logging purposes.
     * 
     * @return The Bukkit version string
     */
    public static String getServerVersion() {
        return BUKKIT_VERSION;
    }
    
    /**
     * Checks if the current version supports side-specific sign operations.
     * 
     * @return true if side-specific operations are supported
     */
    public static boolean supportsSideSpecificSigns() {
        return !IS_LEGACY_VERSION;
    }
    
    /**
     * Safely gets text from a specific side of a sign.
     * 
     * @param sign The sign to read from
     * @param side The side to read (FRONT or BACK)
     * @param lineIndex The line index (0-3)
     * @return The text on the specified line, or empty string if not available
     */
    public static String getSignLine(Sign sign, Side side, int lineIndex) {
        if (sign == null || lineIndex < 0 || lineIndex > 3) {
            return "";
        }
        
        try {
            if (IS_LEGACY_VERSION) {
                // Legacy versions only support front side
                if (side == Side.BACK) {
                    return ""; // Back side not supported
                }
                @SuppressWarnings("deprecation")
                String line = sign.getLine(lineIndex);
                return line != null ? line : "";
            } else {
                // Use newer side-specific API
                String line = sign.getSide(side).getLine(lineIndex);
                return line != null ? line : "";
            }
        } catch (NoSuchMethodError | AbstractMethodError e) {
            ErrorHandler.debug("API compatibility issue when reading sign line: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Safely sets text on a specific side of a sign.
     * 
     * @param sign The sign to modify
     * @param side The side to modify (FRONT or BACK)
     * @param lineIndex The line index (0-3)
     * @param text The text to set
     */
    public static void setSignLine(Sign sign, Side side, int lineIndex, String text) {
        if (sign == null || lineIndex < 0 || lineIndex > 3 || text == null) {
            return;
        }
        
        try {
            if (IS_LEGACY_VERSION) {
                // Legacy versions only support front side
                if (side == Side.BACK) {
                    ErrorHandler.debug("Attempted to set back side text on legacy version");
                    return;
                }
                // Use legacy API for older versions
                @SuppressWarnings("deprecation")
                String ignored = sign.getLine(lineIndex); // Test API access
                @SuppressWarnings("deprecation") 
                boolean setResult = true; // Assume success
                sign.setLine(lineIndex, text);
            } else {
                // Use newer side-specific API
                sign.getSide(side).setLine(lineIndex, text);
            }
        } catch (NoSuchMethodError | AbstractMethodError e) {
            ErrorHandler.debug("API compatibility issue when setting sign line: " + e.getMessage());
        }
    }
}