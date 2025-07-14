package us.ironcladnetwork.copySign.Util;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import us.ironcladnetwork.copySign.CopySign;

import java.util.*;

/**
 * Comprehensive configuration validator for CopySign.
 * Validates configuration values, ranges, and types.
 * Provides auto-fix capabilities for common issues.
 */
public class ConfigValidator {
    private final CopySign plugin;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private final List<String> fixes = new ArrayList<>();
    
    // Valid configuration paths and their types
    private static final Map<String, Class<?>> CONFIG_PATHS = new HashMap<>();
    
    static {
        // General settings
        CONFIG_PATHS.put("general.default-enabled", Boolean.class);
        CONFIG_PATHS.put("general.check-for-updates", Boolean.class);
        CONFIG_PATHS.put("general.debug", Boolean.class);
        CONFIG_PATHS.put("general.confirmation-timeout", Integer.class);
        
        // Feature toggles
        CONFIG_PATHS.put("features.sign-copy", Boolean.class);
        CONFIG_PATHS.put("features.sign-library", Boolean.class);
        CONFIG_PATHS.put("features.copy-colors", Boolean.class);
        CONFIG_PATHS.put("features.copy-glow", Boolean.class);
        CONFIG_PATHS.put("features.strict-sign-type", Boolean.class);
        CONFIG_PATHS.put("features.clear-command", Boolean.class);
        CONFIG_PATHS.put("features.server-templates", Boolean.class);
        
        // Library settings
        CONFIG_PATHS.put("library.max-saved-signs", Integer.class);
        CONFIG_PATHS.put("library.allow-overwrite", Boolean.class);
        CONFIG_PATHS.put("library.gui.title", String.class);
        CONFIG_PATHS.put("library.gui.rows", Integer.class);
        CONFIG_PATHS.put("library.gui.show-preview", Boolean.class);
        
        // Cooldown settings
        CONFIG_PATHS.put("cooldowns", ConfigurationSection.class);
        
        // Sign interaction
        CONFIG_PATHS.put("sign-interaction.require-sneak-to-copy", Boolean.class);
        CONFIG_PATHS.put("sign-interaction.require-sneak-to-paste", Boolean.class);
        CONFIG_PATHS.put("sign-interaction.sounds", ConfigurationSection.class);
        
        // Storage settings
        CONFIG_PATHS.put("storage.auto-save-interval", Integer.class);
        CONFIG_PATHS.put("storage.backup.enabled", Boolean.class);
        CONFIG_PATHS.put("storage.backup.max-backups", Integer.class);
        CONFIG_PATHS.put("storage.backup.on-startup", Boolean.class);
        
        // Protection settings
        CONFIG_PATHS.put("protection.respect-worldguard", Boolean.class);
        CONFIG_PATHS.put("protection.max-sign-text-length", Integer.class);
        CONFIG_PATHS.put("protection.validate-text-content", Boolean.class);
        
        // Performance settings
        CONFIG_PATHS.put("performance.cache-toggle-states", Boolean.class);
        CONFIG_PATHS.put("performance.async-operations", Boolean.class);
        CONFIG_PATHS.put("performance.cache-expiry-seconds", Integer.class);
        
        // Template settings
        CONFIG_PATHS.put("templates.require-confirmation-on-delete", Boolean.class);
        CONFIG_PATHS.put("templates.max-name-length", Integer.class);
        CONFIG_PATHS.put("templates.allow-special-characters", Boolean.class);
        CONFIG_PATHS.put("templates.hide-system-templates-in-tab", Boolean.class);
        CONFIG_PATHS.put("templates.system-template-prefix", String.class);
        
        // Validation settings
        CONFIG_PATHS.put("validation.max-sign-name-length", Integer.class);
        CONFIG_PATHS.put("validation.reserved-names", List.class);
        CONFIG_PATHS.put("validation.gui-min-rows", Integer.class);
        CONFIG_PATHS.put("validation.gui-max-rows", Integer.class);
        
        // Metrics
        CONFIG_PATHS.put("metrics.enabled", Boolean.class);
        
        // Config version
        CONFIG_PATHS.put("config-version", Integer.class);
    }
    
    public ConfigValidator(CopySign plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Performs comprehensive configuration validation.
     * 
     * @return true if configuration is valid, false if critical errors found
     */
    public boolean validate() {
        warnings.clear();
        errors.clear();
        fixes.clear();
        
        FileConfiguration config = plugin.getConfig();
        
        // Check config version
        validateConfigVersion(config);
        
        // Validate structure and types
        validateStructure(config);
        
        // Validate value ranges
        validateRanges(config);
        
        // Validate sound names
        validateSounds(config);
        
        // Validate sign types
        validateSignTypes(config);
        
        // Check for deprecated options
        checkDeprecated(config);
        
        // Check for missing required values
        checkRequired(config);
        
        // Log results
        logResults();
        
        return errors.isEmpty();
    }
    
    /**
     * Validates the configuration version.
     */
    private void validateConfigVersion(FileConfiguration config) {
        int version = config.getInt("config-version", 0);
        int expectedVersion = 1;
        
        if (version < expectedVersion) {
            warnings.add("Configuration is outdated (version " + version + ", expected " + expectedVersion + "). Consider backing up and regenerating.");
        } else if (version > expectedVersion) {
            warnings.add("Configuration version is newer than expected (version " + version + "). This plugin version may be outdated.");
        }
    }
    
    /**
     * Validates configuration structure and types.
     */
    private void validateStructure(FileConfiguration config) {
        for (Map.Entry<String, Class<?>> entry : CONFIG_PATHS.entrySet()) {
            String path = entry.getKey();
            Class<?> expectedType = entry.getValue();
            
            if (!config.contains(path)) {
                // Skip optional sections
                if (path.contains("cooldowns.")) {
                    continue;
                }
                warnings.add("Missing configuration path: " + path);
                continue;
            }
            
            Object value = config.get(path);
            if (value == null) {
                warnings.add("Null value at path: " + path);
                continue;
            }
            
            // Special handling for configuration sections
            if (expectedType == ConfigurationSection.class) {
                if (!(value instanceof ConfigurationSection)) {
                    errors.add("Invalid type at " + path + ": expected section, got " + value.getClass().getSimpleName());
                }
            } else if (expectedType == List.class) {
                if (!(value instanceof List)) {
                    errors.add("Invalid type at " + path + ": expected list, got " + value.getClass().getSimpleName());
                }
            } else if (!expectedType.isInstance(value)) {
                errors.add("Invalid type at " + path + ": expected " + expectedType.getSimpleName() + 
                          ", got " + value.getClass().getSimpleName());
            }
        }
    }
    
    /**
     * Validates value ranges.
     */
    private void validateRanges(FileConfiguration config) {
        // GUI rows
        int guiRows = config.getInt("library.gui.rows", 6);
        int minRows = config.getInt("validation.gui-min-rows", 1);
        int maxRows = config.getInt("validation.gui-max-rows", 6);
        
        if (guiRows < minRows || guiRows > maxRows) {
            errors.add("library.gui.rows must be between " + minRows + " and " + maxRows + " (current: " + guiRows + ")");
            fixes.add("Set library.gui.rows to 6 (default)");
        }
        
        // Cooldowns
        ConfigurationSection cooldowns = config.getConfigurationSection("cooldowns");
        if (cooldowns != null) {
            for (String key : cooldowns.getKeys(false)) {
                int cooldown = cooldowns.getInt(key, 0);
                if (cooldown < 0) {
                    errors.add("cooldowns." + key + " cannot be negative (current: " + cooldown + ")");
                    fixes.add("Set cooldowns." + key + " to 0");
                }
            }
        }
        
        // Max saved signs
        int maxSigns = config.getInt("library.max-saved-signs", 50);
        if (maxSigns < -1 || maxSigns == 0) {
            errors.add("library.max-saved-signs must be -1 (unlimited) or positive (current: " + maxSigns + ")");
            fixes.add("Set library.max-saved-signs to 50 (default)");
        }
        
        // Confirmation timeout
        int timeout = config.getInt("general.confirmation-timeout", 30);
        if (timeout < 5 || timeout > 300) {
            warnings.add("general.confirmation-timeout should be between 5 and 300 seconds (current: " + timeout + ")");
        }
        
        // Auto-save interval
        int autoSave = config.getInt("storage.auto-save-interval", 5);
        if (autoSave < 0) {
            errors.add("storage.auto-save-interval cannot be negative (current: " + autoSave + ")");
            fixes.add("Set storage.auto-save-interval to 0 (disabled)");
        }
        
        // Max backups
        int maxBackups = config.getInt("storage.backup.max-backups", 5);
        if (maxBackups < 0) {
            errors.add("storage.backup.max-backups cannot be negative (current: " + maxBackups + ")");
            fixes.add("Set storage.backup.max-backups to 5 (default)");
        }
        
        // Cache expiry
        int cacheExpiry = config.getInt("performance.cache-expiry-seconds", 30);
        if (cacheExpiry < 1) {
            errors.add("performance.cache-expiry-seconds must be at least 1 (current: " + cacheExpiry + ")");
            fixes.add("Set performance.cache-expiry-seconds to 30 (default)");
        }
        
        // Template name length
        int templateNameLength = config.getInt("templates.max-name-length", 32);
        if (templateNameLength < 1 || templateNameLength > 64) {
            warnings.add("templates.max-name-length should be between 1 and 64 (current: " + templateNameLength + ")");
        }
        
        // Sign name length
        int signNameLength = config.getInt("validation.max-sign-name-length", 32);
        if (signNameLength < 1 || signNameLength > 64) {
            warnings.add("validation.max-sign-name-length should be between 1 and 64 (current: " + signNameLength + ")");
        }
    }
    
    /**
     * Validates sound configuration.
     */
    private void validateSounds(FileConfiguration config) {
        ConfigurationSection sounds = config.getConfigurationSection("sign-interaction.sounds");
        if (sounds == null) return;
        
        if (!sounds.getBoolean("enabled", true)) {
            return; // Sounds disabled, skip validation
        }
        
        String[] soundKeys = {"copy", "paste", "save", "error"};
        for (String key : soundKeys) {
            String soundName = sounds.getString(key);
            if (soundName == null || soundName.isEmpty()) {
                continue; // Empty means disabled
            }
            
            try {
                Sound.valueOf(soundName);
            } catch (IllegalArgumentException e) {
                warnings.add("Invalid sound name for sign-interaction.sounds." + key + ": " + soundName);
            }
        }
    }
    
    /**
     * Validates sign types configuration.
     */
    private void validateSignTypes(FileConfiguration config) {
        List<String> allowedTypes = config.getStringList("sign-types.allowed");
        
        if (allowedTypes.isEmpty()) {
            errors.add("sign-types.allowed cannot be empty - at least one sign type must be allowed");
            fixes.add("Add default sign types to sign-types.allowed");
        }
        
        for (String type : allowedTypes) {
            if (!type.endsWith("_SIGN")) {
                warnings.add("Suspicious sign type in sign-types.allowed: " + type + " (should end with _SIGN)");
            }
        }
    }
    
    /**
     * Checks for deprecated configuration options.
     */
    private void checkDeprecated(FileConfiguration config) {
        // Currently commented out options
        if (config.contains("performance.max-batch-size")) {
            warnings.add("performance.max-batch-size is defined but not implemented");
        }
        
        if (config.contains("protection.respect-sign-ownership")) {
            warnings.add("protection.respect-sign-ownership is defined but not implemented");
        }
    }
    
    /**
     * Checks for required configuration values.
     */
    private void checkRequired(FileConfiguration config) {
        // List of absolutely required paths
        String[] required = {
            "general.default-enabled",
            "features.sign-copy",
            "features.sign-library",
            "library.max-saved-signs",
            "metrics.enabled"
        };
        
        for (String path : required) {
            if (!config.contains(path)) {
                errors.add("Required configuration missing: " + path);
            }
        }
    }
    
    /**
     * Logs validation results.
     */
    private void logResults() {
        if (!errors.isEmpty()) {
            plugin.getLogger().severe("=== Configuration Errors Found ===");
            for (String error : errors) {
                plugin.getLogger().severe("ERROR: " + error);
            }
            if (!fixes.isEmpty()) {
                plugin.getLogger().severe("=== Suggested Fixes ===");
                for (String fix : fixes) {
                    plugin.getLogger().severe("FIX: " + fix);
                }
            }
        }
        
        if (!warnings.isEmpty()) {
            plugin.getLogger().warning("=== Configuration Warnings ===");
            for (String warning : warnings) {
                plugin.getLogger().warning("WARNING: " + warning);
            }
        }
        
        if (errors.isEmpty() && warnings.isEmpty()) {
            plugin.getLogger().info("Configuration validation passed - no issues found!");
        } else {
            plugin.getLogger().info("Configuration validation complete: " + 
                                   errors.size() + " errors, " + warnings.size() + " warnings");
        }
    }
    
    /**
     * Gets validation errors.
     * @return List of error messages
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    /**
     * Gets validation warnings.
     * @return List of warning messages
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    /**
     * Gets suggested fixes.
     * @return List of fix suggestions
     */
    public List<String> getFixes() {
        return new ArrayList<>(fixes);
    }
}