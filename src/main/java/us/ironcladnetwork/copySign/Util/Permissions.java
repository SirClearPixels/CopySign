package us.ironcladnetwork.copySign.Util;

import org.bukkit.entity.Player;

/**
 * Centralized permission constants and validation utilities for CopySign.
 * Provides comprehensive permission checking with granular controls.
 */
public final class Permissions {
    
    // Base permissions
    public static final String USE = "copysign.use";
    public static final String ADMIN = "copysign.admin";
    public static final String RELOAD = "copysign.reload";
    
    // Feature-specific permissions
    public static final String COPY_COLOR = "copysign.copycolor";
    public static final String COPY_GLOW = "copysign.copyglow";
    public static final String COPY_REGULAR_SIGNS = "copysign.copy.regular";
    public static final String COPY_HANGING_SIGNS = "copysign.copy.hanging";
    public static final String PASTE_REGULAR_SIGNS = "copysign.paste.regular";
    public static final String PASTE_HANGING_SIGNS = "copysign.paste.hanging";
    
    // Library permissions
    public static final String LIBRARY = "copysign.library";
    public static final String LIBRARY_SAVE = "copysign.library.save";
    public static final String LIBRARY_LOAD = "copysign.library.load";
    public static final String LIBRARY_DELETE = "copysign.library.delete";
    public static final String LIBRARY_VIEW = "copysign.library.view";
    public static final String LIBRARY_UNLIMITED = "copysign.library.unlimited";
    
    // Template permissions
    public static final String TEMPLATES = "copysign.templates";
    public static final String TEMPLATES_VIEW = "copysign.templates.view";
    public static final String TEMPLATES_USE = "copysign.templates.use";
    public static final String TEMPLATES_CREATE = "copysign.templates.create";
    public static final String TEMPLATES_EDIT = "copysign.templates.edit";
    public static final String TEMPLATES_DELETE = "copysign.templates.delete";
    
    // Bypass permissions
    public static final String BYPASS_COOLDOWNS = "copysign.bypass.cooldowns";
    public static final String BYPASS_LIMITS = "copysign.bypass.limits";
    public static final String BYPASS_DISABLED_WORLDS = "copysign.bypass.worlds";
    
    // World-specific permissions (dynamic)
    public static final String WORLD_PREFIX = "copysign.world.";
    
    // Private constructor to prevent instantiation
    private Permissions() {}
    
    /**
     * Checks if a player has basic usage permission.
     * 
     * @param player The player to check
     * @return true if player can use CopySign
     */
    public static boolean canUse(Player player) {
        return player.hasPermission(USE);
    }
    
    /**
     * Checks if a player has admin permissions.
     * 
     * @param player The player to check
     * @return true if player has admin access
     */
    public static boolean isAdmin(Player player) {
        return player.hasPermission(ADMIN);
    }
    
    /**
     * Checks if a player can copy sign colors.
     * 
     * @param player The player to check
     * @return true if player can copy colors
     */
    public static boolean canCopyColor(Player player) {
        return player.hasPermission(COPY_COLOR);
    }
    
    /**
     * Checks if a player can copy sign glow state.
     * 
     * @param player The player to check
     * @return true if player can copy glow
     */
    public static boolean canCopyGlow(Player player) {
        return player.hasPermission(COPY_GLOW);
    }
    
    /**
     * Checks if a player can copy a specific sign type.
     * 
     * @param player The player to check
     * @param isHanging Whether the sign is a hanging sign
     * @return true if player can copy this sign type
     */
    public static boolean canCopySignType(Player player, boolean isHanging) {
        if (isHanging) {
            return player.hasPermission(COPY_HANGING_SIGNS);
        } else {
            return player.hasPermission(COPY_REGULAR_SIGNS);
        }
    }
    
    /**
     * Checks if a player can paste to a specific sign type.
     * 
     * @param player The player to check
     * @param isHanging Whether the target sign is a hanging sign
     * @return true if player can paste to this sign type
     */
    public static boolean canPasteSignType(Player player, boolean isHanging) {
        if (isHanging) {
            return player.hasPermission(PASTE_HANGING_SIGNS);
        } else {
            return player.hasPermission(PASTE_REGULAR_SIGNS);
        }
    }
    
    /**
     * Checks if a player can access the sign library.
     * 
     * @param player The player to check
     * @return true if player can use library features
     */
    public static boolean canUseLibrary(Player player) {
        return player.hasPermission(LIBRARY);
    }
    
    /**
     * Checks if a player can save signs to library.
     * 
     * @param player The player to check
     * @return true if player can save signs
     */
    public static boolean canSaveToLibrary(Player player) {
        return player.hasPermission(LIBRARY_SAVE) || player.hasPermission(LIBRARY);
    }
    
    /**
     * Checks if a player can load signs from library.
     * 
     * @param player The player to check
     * @return true if player can load signs
     */
    public static boolean canLoadFromLibrary(Player player) {
        return player.hasPermission(LIBRARY_LOAD) || player.hasPermission(LIBRARY);
    }
    
    /**
     * Checks if a player can delete signs from library.
     * 
     * @param player The player to check
     * @return true if player can delete signs
     */
    public static boolean canDeleteFromLibrary(Player player) {
        return player.hasPermission(LIBRARY_DELETE) || player.hasPermission(LIBRARY);
    }
    
    /**
     * Checks if a player can view their library.
     * 
     * @param player The player to check
     * @return true if player can view library
     */
    public static boolean canViewLibrary(Player player) {
        return player.hasPermission(LIBRARY_VIEW) || player.hasPermission(LIBRARY);
    }
    
    /**
     * Checks if a player has unlimited library storage.
     * 
     * @param player The player to check
     * @return true if player bypasses library limits
     */
    public static boolean hasUnlimitedLibrary(Player player) {
        return player.hasPermission(LIBRARY_UNLIMITED) || isAdmin(player);
    }
    
    /**
     * Checks if a player can view server templates.
     * 
     * @param player The player to check
     * @return true if player can view templates
     */
    public static boolean canViewTemplates(Player player) {
        return player.hasPermission(TEMPLATES_VIEW) || player.hasPermission(TEMPLATES);
    }
    
    /**
     * Checks if a player can use server templates.
     * 
     * @param player The player to check
     * @return true if player can use templates
     */
    public static boolean canUseTemplates(Player player) {
        return player.hasPermission(TEMPLATES_USE) || player.hasPermission(TEMPLATES);
    }
    
    /**
     * Checks if a player can create server templates.
     * 
     * @param player The player to check
     * @return true if player can create templates
     */
    public static boolean canCreateTemplates(Player player) {
        return player.hasPermission(TEMPLATES_CREATE) || isAdmin(player);
    }
    
    /**
     * Checks if a player can edit server templates.
     * 
     * @param player The player to check
     * @return true if player can edit templates
     */
    public static boolean canEditTemplates(Player player) {
        return player.hasPermission(TEMPLATES_EDIT) || isAdmin(player);
    }
    
    /**
     * Checks if a player can delete server templates.
     * 
     * @param player The player to check
     * @return true if player can delete templates
     */
    public static boolean canDeleteTemplates(Player player) {
        return player.hasPermission(TEMPLATES_DELETE) || isAdmin(player);
    }
    
    /**
     * Checks if a player can reload the plugin.
     * 
     * @param player The player to check
     * @return true if player can reload
     */
    public static boolean canReload(Player player) {
        return player.hasPermission(RELOAD);
    }
    
    /**
     * Checks if a player can bypass cooldowns.
     * 
     * @param player The player to check
     * @return true if player bypasses cooldowns
     */
    public static boolean bypassesCooldowns(Player player) {
        return player.hasPermission(BYPASS_COOLDOWNS) || isAdmin(player);
    }
    
    /**
     * Checks if a player can bypass various limits.
     * 
     * @param player The player to check
     * @return true if player bypasses limits
     */
    public static boolean bypassesLimits(Player player) {
        return player.hasPermission(BYPASS_LIMITS) || isAdmin(player);
    }
    
    /**
     * Checks if a player can use CopySign in disabled worlds.
     * 
     * @param player The player to check
     * @return true if player bypasses world restrictions
     */
    public static boolean bypassesWorldRestrictions(Player player) {
        return player.hasPermission(BYPASS_DISABLED_WORLDS) || isAdmin(player);
    }
    
    /**
     * Checks if a player can use CopySign in a specific world.
     * 
     * @param player The player to check
     * @param worldName The world name
     * @return true if player can use CopySign in this world
     */
    public static boolean canUseInWorld(Player player, String worldName) {
        return player.hasPermission(WORLD_PREFIX + worldName.toLowerCase()) || 
               bypassesWorldRestrictions(player);
    }
    
    /**
     * Gets the maximum number of signs a player can save in their library.
     * Returns -1 for unlimited.
     * 
     * @param player The player to check
     * @param configDefault The default limit from config
     * @return Maximum signs allowed, or -1 for unlimited
     */
    public static int getMaxLibrarySigns(Player player, int configDefault) {
        if (hasUnlimitedLibrary(player)) {
            return -1;
        }
        
        // Check for specific limit permissions (copysign.library.limit.X)
        for (int limit : new int[]{100, 75, 50, 25, 10, 5}) {
            if (player.hasPermission("copysign.library.limit." + limit)) {
                return limit;
            }
        }
        
        return configDefault;
    }
}