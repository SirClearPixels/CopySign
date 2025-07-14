package us.ironcladnetwork.copySign.Util;

import org.bukkit.configuration.file.FileConfiguration;
import us.ironcladnetwork.copySign.CopySign;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized configuration manager for thread-safe access to plugin configuration.
 * Provides type-safe getters with proper defaults and caching for frequently accessed values.
 */
public class ConfigManager {
    private final CopySign plugin;
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    
    // Cache keys for frequently accessed values
    private static final String CACHE_CONFIRMATION_TIMEOUT = "general.confirmation-timeout";
    private static final String CACHE_MAX_SIGN_NAME_LENGTH = "validation.max-sign-name-length";
    private static final String CACHE_CACHE_EXPIRY = "performance.cache-expiry-seconds";
    private static final String CACHE_RESERVED_NAMES = "validation.reserved-names";
    
    public ConfigManager(CopySign plugin) {
        this.plugin = plugin;
        reloadCache();
    }
    
    /**
     * Reloads the configuration cache with frequently accessed values.
     */
    public void reloadCache() {
        cache.clear();
        FileConfiguration config = plugin.getConfig();
        
        // Cache frequently accessed values
        cache.put(CACHE_CONFIRMATION_TIMEOUT, config.getInt("general.confirmation-timeout", 30));
        cache.put(CACHE_MAX_SIGN_NAME_LENGTH, config.getInt("validation.max-sign-name-length", 32));
        cache.put(CACHE_CACHE_EXPIRY, config.getInt("performance.cache-expiry-seconds", 30));
        cache.put(CACHE_RESERVED_NAMES, config.getStringList("validation.reserved-names"));
    }
    
    // General settings
    public boolean isDefaultEnabled() {
        return plugin.getConfigBoolean("general.default-enabled", true);
    }
    
    public boolean checkForUpdates() {
        return plugin.getConfigBoolean("general.check-for-updates", true);
    }
    
    public boolean isDebugEnabled() {
        return plugin.getConfigBoolean("general.debug", false);
    }
    
    public int getConfirmationTimeout() {
        return (int) cache.getOrDefault(CACHE_CONFIRMATION_TIMEOUT, 30);
    }
    
    // Feature toggles
    public boolean isSignCopyEnabled() {
        return plugin.getConfigBoolean("features.sign-copy", true);
    }
    
    public boolean isSignLibraryEnabled() {
        return plugin.getConfigBoolean("features.sign-library", true);
    }
    
    public boolean isCopyColorsEnabled() {
        return plugin.getConfigBoolean("features.copy-colors", true);
    }
    
    public boolean isCopyGlowEnabled() {
        return plugin.getConfigBoolean("features.copy-glow", true);
    }
    
    public boolean isStrictSignTypeEnabled() {
        return plugin.getConfigBoolean("features.strict-sign-type", true);
    }
    
    public boolean isClearCommandEnabled() {
        return plugin.getConfigBoolean("features.clear-command", true);
    }
    
    public boolean isServerTemplatesEnabled() {
        return plugin.getConfigBoolean("features.server-templates", true);
    }
    
    // Library settings
    public int getMaxSavedSigns() {
        return plugin.getConfigInt("library.max-saved-signs", 50);
    }
    
    public boolean allowOverwrite() {
        return plugin.getConfigBoolean("library.allow-overwrite", false);
    }
    
    public String getLibraryGuiTitle() {
        return plugin.getConfigString("library.gui.title", "&b&lSign Library");
    }
    
    public int getLibraryGuiRows() {
        int rows = plugin.getConfigInt("library.gui.rows", 6);
        // Validate rows between min and max
        int minRows = plugin.getConfigInt("validation.gui-min-rows", 1);
        int maxRows = plugin.getConfigInt("validation.gui-max-rows", 6);
        return Math.max(minRows, Math.min(maxRows, rows));
    }
    
    public boolean showPreview() {
        return plugin.getConfigBoolean("library.gui.show-preview", true);
    }
    
    // Cooldown settings
    public int getCooldown(String action) {
        return plugin.getConfigInt("cooldowns." + action, 0);
    }
    
    // Sign interaction settings
    public boolean requireSneakToCopy() {
        return plugin.getConfigBoolean("sign-interaction.require-sneak-to-copy", true);
    }
    
    public boolean requireSneakToPaste() {
        return plugin.getConfigBoolean("sign-interaction.require-sneak-to-paste", false);
    }
    
    // Sign type restrictions
    public List<String> getAllowedSignTypes() {
        return plugin.withConfigLock((org.bukkit.configuration.file.FileConfiguration config) -> config.getStringList("sign-types.allowed"));
    }
    
    // Storage settings
    public int getAutoSaveInterval() {
        return plugin.getConfigInt("storage.auto-save-interval", 5);
    }
    
    public boolean isBackupEnabled() {
        return plugin.getConfigBoolean("storage.backup.enabled", true);
    }
    
    public int getMaxBackups() {
        return plugin.getConfigInt("storage.backup.max-backups", 5);
    }
    
    public boolean backupOnStartup() {
        return plugin.getConfigBoolean("storage.backup.on-startup", true);
    }
    
    // Protection settings
    public boolean respectWorldGuard() {
        return plugin.getConfigBoolean("protection.respect-worldguard", true);
    }
    
    public int getMaxSignTextLength() {
        return plugin.getConfigInt("protection.max-sign-text-length", 15);
    }
    
    public boolean validateTextContent() {
        return plugin.getConfigBoolean("protection.validate-text-content", true);
    }
    
    // Performance settings
    public boolean isCacheToggleStates() {
        return plugin.getConfigBoolean("performance.cache-toggle-states", true);
    }
    
    public boolean useAsyncOperations() {
        return plugin.getConfigBoolean("performance.async-operations", true);
    }
    
    public int getCacheExpirySeconds() {
        return (int) cache.getOrDefault(CACHE_CACHE_EXPIRY, 30);
    }
    
    // Template settings
    public boolean requireConfirmationOnDelete() {
        return plugin.getConfigBoolean("templates.require-confirmation-on-delete", true);
    }
    
    public int getMaxTemplateNameLength() {
        return plugin.getConfigInt("templates.max-name-length", 32);
    }
    
    public boolean allowSpecialCharacters() {
        return plugin.getConfigBoolean("templates.allow-special-characters", false);
    }
    
    public boolean hideSystemTemplatesInTab() {
        return plugin.getConfigBoolean("templates.hide-system-templates-in-tab", false);
    }
    
    public String getSystemTemplatePrefix() {
        return plugin.getConfigString("templates.system-template-prefix", "system_");
    }
    
    // Validation settings
    public int getMaxSignNameLength() {
        return (int) cache.getOrDefault(CACHE_MAX_SIGN_NAME_LENGTH, 32);
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getReservedNames() {
        return (List<String>) cache.getOrDefault(CACHE_RESERVED_NAMES, List.of("template", "system", "default"));
    }
    
    public int getGuiMinRows() {
        return plugin.getConfigInt("validation.gui-min-rows", 1);
    }
    
    public int getGuiMaxRows() {
        return plugin.getConfigInt("validation.gui-max-rows", 6);
    }
    
    // Metrics settings
    public boolean isMetricsEnabled() {
        return plugin.getConfigBoolean("metrics.enabled", true);
    }
    
    /**
     * Validates the configuration and logs warnings for any issues.
     */
    public void validateConfiguration() {
        FileConfiguration config = plugin.getConfig();
        
        // Validate GUI rows
        int rows = config.getInt("library.gui.rows", 6);
        if (rows < getGuiMinRows() || rows > getGuiMaxRows()) {
            plugin.getLogger().warning("library.gui.rows value " + rows + " is out of bounds. Using default: 6");
        }
        
        // Validate cooldowns
        org.bukkit.configuration.ConfigurationSection cooldownSection = config.getConfigurationSection("cooldowns");
        if (cooldownSection != null) {
            for (String key : cooldownSection.getKeys(false)) {
                int cooldown = cooldownSection.getInt(key);
                if (cooldown < 0) {
                    plugin.getLogger().warning("cooldowns." + key + " cannot be negative. Using 0");
                }
            }
        }
        
        // Validate max saved signs
        int maxSigns = getMaxSavedSigns();
        if (maxSigns < -1 || maxSigns == 0) {
            plugin.getLogger().warning("library.max-saved-signs must be -1 (unlimited) or positive. Using default: 50");
        }
        
        // Check for deprecated or unused options
        if (config.contains("performance.max-batch-size")) {
            plugin.getLogger().info("Note: performance.max-batch-size is not yet implemented");
        }
        
        if (config.contains("protection.respect-sign-ownership")) {
            plugin.getLogger().info("Note: protection.respect-sign-ownership is not yet implemented");
        }
    }
}