package us.ironcladnetwork.copySign.Util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.ironcladnetwork.copySign.CopySign;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles configuration migrations between versions.
 * Automatically updates old configurations to new formats.
 */
public class ConfigMigrator {
    private final CopySign plugin;
    private final File configFile;
    private boolean migrationPerformed = false;
    
    // Current configuration version
    private static final int CURRENT_VERSION = 2;
    
    public ConfigMigrator(CopySign plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }
    
    /**
     * Checks and performs necessary migrations.
     * 
     * @return true if migration was performed
     */
    public boolean migrate() {
        FileConfiguration config = plugin.getConfig();
        int version = config.getInt("config-version", 0);
        
        if (version >= CURRENT_VERSION) {
            return false; // No migration needed
        }
        
        plugin.getLogger().info("Configuration migration needed: v" + version + " -> v" + CURRENT_VERSION);
        
        // Create backup before migration
        if (!createBackup()) {
            plugin.getLogger().severe("Failed to create configuration backup. Migration aborted.");
            return false;
        }
        
        // Perform migrations in sequence
        if (version < 1) {
            migrateToV1(config);
        }
        
        // Migrate to version 2 (adds more reserved names and comments)
        if (version < 2) {
            migrateToV2(config);
        }
        
        // Update version
        config.set("config-version", CURRENT_VERSION);
        
        // Save migrated configuration
        try {
            config.save(configFile);
            plugin.getLogger().info("Configuration migration completed successfully!");
            migrationPerformed = true;
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save migrated configuration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Creates a backup of the current configuration.
     * 
     * @return true if backup was successful
     */
    private boolean createBackup() {
        if (!configFile.exists()) {
            return true; // No file to backup
        }
        
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFile = new File(plugin.getDataFolder(), "config_backup_" + timestamp + ".yml");
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.save(backupFile);
            plugin.getLogger().info("Configuration backup created: " + backupFile.getName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create configuration backup: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Migrates configuration to version 1.
     * This is the initial structured configuration.
     */
    private void migrateToV1(FileConfiguration config) {
        plugin.getLogger().info("Migrating configuration to version 1...");
        
        // Map old paths to new paths
        Map<String, String> pathMigrations = new HashMap<>();
        
        // Example migrations (if any old configs exist)
        pathMigrations.put("copysign.enabled", "general.default-enabled");
        pathMigrations.put("copysign.max-signs", "library.max-saved-signs");
        pathMigrations.put("copysign.use-colors", "features.copy-colors");
        pathMigrations.put("copysign.use-glow", "features.copy-glow");
        
        // Perform path migrations
        for (Map.Entry<String, String> entry : pathMigrations.entrySet()) {
            String oldPath = entry.getKey();
            String newPath = entry.getValue();
            
            if (config.contains(oldPath) && !config.contains(newPath)) {
                Object value = config.get(oldPath);
                config.set(newPath, value);
                config.set(oldPath, null); // Remove old path
                plugin.getLogger().info("Migrated: " + oldPath + " -> " + newPath);
            }
        }
        
        // Add new configuration sections if missing
        addMissingDefaults(config);
        
        // Clean up any obsolete entries
        cleanupObsolete(config);
    }
    
    /**
     * Adds missing default values for new configuration options.
     */
    private void addMissingDefaults(FileConfiguration config) {
        // General settings
        setDefault(config, "general.confirmation-timeout", 30);
        
        // Protection settings
        setDefault(config, "protection.max-sign-text-length", 15);
        setDefault(config, "protection.validate-text-content", true);
        
        // Performance settings
        setDefault(config, "performance.async-operations", true);
        setDefault(config, "performance.cache-expiry-seconds", 30);
        
        // Validation settings (new section)
        if (!config.contains("validation")) {
            plugin.getLogger().info("Adding new validation section...");
            config.set("validation.max-sign-name-length", 32);
            config.set("validation.reserved-names", java.util.Arrays.asList("template", "system", "default"));
            config.set("validation.gui-min-rows", 1);
            config.set("validation.gui-max-rows", 6);
        }
        
        // Sound settings
        if (!config.contains("sign-interaction.sounds")) {
            plugin.getLogger().info("Adding sound configuration...");
            config.set("sign-interaction.sounds.enabled", true);
            config.set("sign-interaction.sounds.copy", "BLOCK_NOTE_BLOCK_PLING");
            config.set("sign-interaction.sounds.paste", "BLOCK_NOTE_BLOCK_CHIME");
            config.set("sign-interaction.sounds.save", "ENTITY_EXPERIENCE_ORB_PICKUP");
            config.set("sign-interaction.sounds.error", "BLOCK_NOTE_BLOCK_BASS");
        }
        
        // Ensure all cooldowns exist
        ensureCooldowns(config);
    }
    
    
    /**
     * Ensures all cooldowns are present.
     */
    private void ensureCooldowns(FileConfiguration config) {
        String basePath = "cooldowns";
        Map<String, Integer> defaultCooldowns = new HashMap<>();
        defaultCooldowns.put("copy", 0);
        defaultCooldowns.put("paste", 0);
        defaultCooldowns.put("save", 2);
        defaultCooldowns.put("load", 1);
        defaultCooldowns.put("delete", 3);
        defaultCooldowns.put("clear", 1);
        defaultCooldowns.put("library", 1);
        
        for (Map.Entry<String, Integer> entry : defaultCooldowns.entrySet()) {
            String path = basePath + "." + entry.getKey();
            if (!config.contains(path)) {
                config.set(path, entry.getValue());
                plugin.getLogger().info("Added missing cooldown: " + entry.getKey());
            }
        }
    }
    
    /**
     * Removes obsolete configuration entries.
     */
    private void cleanupObsolete(FileConfiguration config) {
        String[] obsoletePaths = {
            // Add any paths that should be removed
            "copysign", // Old root section
            "old-setting", // Example
        };
        
        for (String path : obsoletePaths) {
            if (config.contains(path)) {
                config.set(path, null);
                plugin.getLogger().info("Removed obsolete configuration: " + path);
            }
        }
    }
    
    /**
     * Sets a default value if the path doesn't exist.
     */
    private void setDefault(FileConfiguration config, String path, Object value) {
        if (!config.contains(path)) {
            config.set(path, value);
            plugin.getLogger().info("Added missing configuration: " + path + " = " + value);
        }
    }
    
    /**
     * Checks if migration was performed.
     * 
     * @return true if migration was performed
     */
    public boolean wasMigrationPerformed() {
        return migrationPerformed;
    }
    
    /**
     * Migrates configuration to version 2.
     * Adds additional reserved names and updates comments.
     */
    private void migrateToV2(FileConfiguration config) {
        plugin.getLogger().info("Migrating configuration to version 2...");
        
        // Add additional reserved names
        if (config.contains("validation.reserved-names")) {
            java.util.List<String> reservedNames = config.getStringList("validation.reserved-names");
            boolean updated = false;
            
            // Add new reserved names if not present
            String[] newReservedNames = {"confirm", "cancel", "list", "create", "delete", "use"};
            for (String name : newReservedNames) {
                if (!reservedNames.contains(name)) {
                    reservedNames.add(name);
                    updated = true;
                }
            }
            
            if (updated) {
                config.set("validation.reserved-names", reservedNames);
                plugin.getLogger().info("Updated reserved names list");
            }
        }
        
        // Migrate cooldowns from commands.cooldowns to cooldowns
        if (config.contains("commands.cooldowns")) {
            ConfigurationSection oldCooldowns = config.getConfigurationSection("commands.cooldowns");
            if (oldCooldowns != null) {
                for (String key : oldCooldowns.getKeys(false)) {
                    config.set("cooldowns." + key, oldCooldowns.get(key));
                }
                config.set("commands.cooldowns", null); // Remove old section
                plugin.getLogger().info("Migrated cooldowns from commands.cooldowns to cooldowns");
            }
        }
        
        // Remove commands.enabled section if it exists
        if (config.contains("commands.enabled")) {
            config.set("commands.enabled", null);
            plugin.getLogger().info("Removed commands.enabled section (no longer needed)");
        }
        
        // Remove entire commands section if it's now empty
        if (config.contains("commands")) {
            ConfigurationSection commands = config.getConfigurationSection("commands");
            if (commands == null || commands.getKeys(false).isEmpty()) {
                config.set("commands", null);
                plugin.getLogger().info("Removed empty commands section");
            }
        }
        
        // Note: Comments are updated when the default config is saved
        // The actual comment updates happen in the config.yml file itself
    }
    
    /**
     * Gets the current configuration version.
     * 
     * @return current version number
     */
    public static int getCurrentVersion() {
        return CURRENT_VERSION;
    }
}