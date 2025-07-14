package us.ironcladnetwork.copySign.Util;

import us.ironcladnetwork.copySign.CopySign;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * Debug logging utility for CopySign plugin.
 * Provides conditional debug output based on configuration setting.
 * All debug messages are prefixed with [DEBUG] for easy identification.
 */
public class DebugLogger {
    private final CopySign plugin;
    private final ConfigManager configManager;
    
    public DebugLogger(CopySign plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }
    
    /**
     * Logs a debug message if debug mode is enabled.
     * 
     * @param message The message to log
     */
    public void debug(String message) {
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
    
    /**
     * Logs a debug message with player context.
     * 
     * @param player The player involved
     * @param message The message to log
     */
    public void debug(Player player, String message) {
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] " + message);
        }
    }
    
    /**
     * Logs a debug message with exception details.
     * 
     * @param message The message to log
     * @param throwable The exception to log
     */
    public void debug(String message, Throwable throwable) {
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().log(Level.INFO, "[DEBUG] " + message, throwable);
        }
    }
    
    /**
     * Logs sign copy operation details.
     * 
     * @param player The player copying the sign
     * @param signType The type of sign being copied
     * @param lines The lines of text on the sign
     */
    public void debugSignCopy(Player player, String signType, String[] lines) {
        if (!configManager.isDebugEnabled()) return;
        
        StringBuilder sb = new StringBuilder("[DEBUG] Sign Copy Operation:\n");
        sb.append("  Player: ").append(player.getName()).append("\n");
        sb.append("  Sign Type: ").append(signType).append("\n");
        sb.append("  Lines:\n");
        for (int i = 0; i < lines.length; i++) {
            sb.append("    [").append(i).append("]: '").append(lines[i]).append("'\n");
        }
        plugin.getLogger().info(sb.toString());
    }
    
    /**
     * Logs sign paste operation details.
     * 
     * @param player The player pasting the sign
     * @param signType The type of sign being pasted to
     * @param success Whether the paste was successful
     */
    public void debugSignPaste(Player player, String signType, boolean success) {
        if (configManager.isDebugEnabled()) {
            debug(player, "Sign paste to " + signType + " - " + (success ? "SUCCESS" : "FAILED"));
        }
    }
    
    /**
     * Logs library operation details.
     * 
     * @param player The player performing the operation
     * @param operation The operation type (save, load, delete)
     * @param signName The name of the sign
     * @param success Whether the operation was successful
     */
    public void debugLibraryOperation(Player player, String operation, String signName, boolean success) {
        if (configManager.isDebugEnabled()) {
            debug(player, "Library " + operation + " '" + signName + "' - " + (success ? "SUCCESS" : "FAILED"));
        }
    }
    
    /**
     * Logs template operation details.
     * 
     * @param player The player performing the operation (null for system operations)
     * @param operation The operation type (create, use, delete)
     * @param templateName The name of the template
     * @param success Whether the operation was successful
     */
    public void debugTemplateOperation(Player player, String operation, String templateName, boolean success) {
        if (!configManager.isDebugEnabled()) return;
        
        String playerInfo = player != null ? "[" + player.getName() + "] " : "[SYSTEM] ";
        debug(playerInfo + "Template " + operation + " '" + templateName + "' - " + (success ? "SUCCESS" : "FAILED"));
    }
    
    /**
     * Logs performance metrics.
     * 
     * @param operation The operation being measured
     * @param timeMs The time taken in milliseconds
     */
    public void debugPerformance(String operation, long timeMs) {
        if (configManager.isDebugEnabled()) {
            debug("Performance: " + operation + " took " + timeMs + "ms");
        }
    }
    
    /**
     * Logs configuration access.
     * 
     * @param path The configuration path accessed
     * @param value The value retrieved
     */
    public void debugConfig(String path, Object value) {
        if (configManager.isDebugEnabled()) {
            debug("Config access: " + path + " = " + value);
        }
    }
    
    /**
     * Logs permission checks.
     * 
     * @param player The player being checked
     * @param permission The permission being checked
     * @param result The result of the permission check
     */
    public void debugPermission(Player player, String permission, boolean result) {
        if (configManager.isDebugEnabled()) {
            debug(player, "Permission check '" + permission + "' = " + result);
        }
    }
}