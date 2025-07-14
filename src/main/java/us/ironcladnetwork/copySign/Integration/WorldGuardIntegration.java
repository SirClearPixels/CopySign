package us.ironcladnetwork.copySign.Integration;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import us.ironcladnetwork.copySign.CopySign;

import java.lang.reflect.Method;

/**
 * Handles integration with WorldGuard for region protection checks.
 * This is a soft dependency - the plugin will work without WorldGuard.
 * Uses reflection to avoid compile-time dependencies.
 */
public class WorldGuardIntegration {
    private final CopySign plugin;
    private Plugin worldGuard;
    private Method canBuildMethod;
    private boolean enabled = false;
    
    public WorldGuardIntegration(CopySign plugin) {
        this.plugin = plugin;
        initialize();
    }
    
    /**
     * Initializes WorldGuard integration if the plugin is present.
     */
    private void initialize() {
        Plugin wgPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        
        if (wgPlugin == null || !wgPlugin.isEnabled()) {
            plugin.getLogger().info("WorldGuard not found - region protection disabled");
            return;
        }
        
        try {
            worldGuard = wgPlugin;
            
            // Try to find the canBuild method - this works for multiple WG versions
            Class<?> wgClass = worldGuard.getClass();
            
            // First try WorldGuard 7+ method signature
            try {
                canBuildMethod = wgClass.getMethod("canBuild", Player.class, Location.class);
                enabled = true;
                plugin.getLogger().info("WorldGuard 7+ integration enabled - respecting region protection");
            } catch (NoSuchMethodException e) {
                // Try WorldGuard 6 method signature
                try {
                    canBuildMethod = wgClass.getMethod("canBuild", Player.class, org.bukkit.block.Block.class);
                    enabled = true;
                    plugin.getLogger().info("WorldGuard 6 integration enabled - respecting region protection");
                } catch (NoSuchMethodException e2) {
                    plugin.getLogger().warning("WorldGuard found but no compatible canBuild method detected");
                    enabled = false;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize WorldGuard integration: " + e.getMessage());
            enabled = false;
        }
    }
    
    /**
     * Checks if WorldGuard integration is enabled and functional.
     * 
     * @return true if WorldGuard is present and integration is working
     */
    public boolean isEnabled() {
        return enabled && plugin.getConfigManager().respectWorldGuard();
    }
    
    /**
     * Checks if a player can copy a sign at the given location.
     * 
     * @param player The player attempting to copy
     * @param location The location of the sign
     * @return true if the player can copy, false otherwise
     */
    public boolean canCopySign(Player player, Location location) {
        if (!isEnabled()) {
            return true; // If disabled, allow by default
        }
        
        // Check bypass permission
        if (player.hasPermission("copysign.bypass.worldguard")) {
            plugin.getDebugLogger().debugPermission(player, "copysign.bypass.worldguard", true);
            return true;
        }
        
        return checkAccess(player, location, "copy");
    }
    
    /**
     * Checks if a player can paste to a sign at the given location.
     * 
     * @param player The player attempting to paste
     * @param location The location of the sign
     * @return true if the player can paste, false otherwise
     */
    public boolean canPasteSign(Player player, Location location) {
        if (!isEnabled()) {
            return true; // If disabled, allow by default
        }
        
        // Check bypass permission
        if (player.hasPermission("copysign.bypass.worldguard")) {
            plugin.getDebugLogger().debugPermission(player, "copysign.bypass.worldguard", true);
            return true;
        }
        
        return checkAccess(player, location, "paste");
    }
    
    /**
     * Performs the actual WorldGuard region check using reflection.
     * 
     * @param player The player to check
     * @param location The location to check
     * @param action The action being performed (for debug logging)
     * @return true if access is allowed, false otherwise
     */
    private boolean checkAccess(Player player, Location location, String action) {
        try {
            boolean canBuild;
            
            // Check which method signature we have
            if (canBuildMethod.getParameterTypes()[1] == Location.class) {
                // WorldGuard 7+ signature: canBuild(Player, Location)
                canBuild = (boolean) canBuildMethod.invoke(worldGuard, player, location);
            } else {
                // WorldGuard 6 signature: canBuild(Player, Block)
                canBuild = (boolean) canBuildMethod.invoke(worldGuard, player, location.getBlock());
            }
            
            plugin.getDebugLogger().debug(player, 
                "WorldGuard check for " + action + " at " + 
                location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + 
                " - " + (canBuild ? "ALLOWED" : "DENIED"));
            
            return canBuild;
        } catch (Exception e) {
            // If anything goes wrong, log it and allow the action
            plugin.getLogger().warning("WorldGuard check failed: " + e.getMessage());
            plugin.getDebugLogger().debug("WorldGuard check error", e);
            return true;
        }
    }
    
    /**
     * Reloads the WorldGuard integration (called when plugin is reloaded).
     */
    public void reload() {
        enabled = false;
        canBuildMethod = null;
        worldGuard = null;
        initialize();
    }
}