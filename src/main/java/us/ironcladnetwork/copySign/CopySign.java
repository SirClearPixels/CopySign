package us.ironcladnetwork.copySign;

import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import us.ironcladnetwork.copySign.Lang.Lang;
import us.ironcladnetwork.copySign.Listeners.SignCopyListener;
import us.ironcladnetwork.copySign.Listeners.SignPlaceListener;
import us.ironcladnetwork.copySign.Util.CopySignToggleManager;
import us.ironcladnetwork.copySign.Util.SignLibraryManager;
import us.ironcladnetwork.copySign.Util.CooldownManager;
import us.ironcladnetwork.copySign.Util.ServerTemplateManager;
import us.ironcladnetwork.copySign.Util.SignDataCache;
import us.ironcladnetwork.copySign.Listeners.SignLibraryGUIListener;
import us.ironcladnetwork.copySign.Util.ConfirmationManager;
import us.ironcladnetwork.copySign.Util.ConfigManager;
import us.ironcladnetwork.copySign.Util.UpdateChecker;
import us.ironcladnetwork.copySign.Util.DebugLogger;
import us.ironcladnetwork.copySign.Integration.WorldGuardIntegration;
import us.ironcladnetwork.copySign.Util.SoundManager;
import us.ironcladnetwork.copySign.Util.MetricsManager;
import us.ironcladnetwork.copySign.Util.ConfigValidator;
import us.ironcladnetwork.copySign.Util.ConfigMigrator;

import org.bukkit.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Main plugin class for the CopySign Minecraft plugin.
 * <p>
 * CopySign allows players to copy and paste sign text, colors, and glow states between signs
 * using NBT data storage. The plugin provides a comprehensive sign management system with
 * features including:
 * <ul>
 *   <li>Sign copying and pasting with shift+left-click interaction</li>
 *   <li>Personal sign libraries for saving and loading sign templates</li>
 *   <li>Server-wide templates managed by administrators</li>
 *   <li>Per-player toggle states for enabling/disabling functionality</li>
 *   <li>Command cooldowns and spam protection</li>
 *   <li>Comprehensive permission system</li>
 *   <li>Support for both regular and hanging signs</li>
 *   <li>Color and glow state preservation</li>
 * </ul>
 * <p>
 * The plugin integrates with bStats for usage metrics and provides extensive configuration
 * options for server administrators to customize behavior.
 * 
 * @author ClearPixels, TheChewyTurtle
 * @version 2.1.0
 * @since 1.0.0
 * @see SignCopyListener
 * @see SignLibraryManager
 * @see CopySignToggleManager
 */
public final class CopySign extends JavaPlugin {
    // Static instance for accessing plugin data elsewhere.
    private static CopySign instance;
    // Field for managing enable/disable state, persisting in players.yml.
    private CopySignToggleManager toggleManager;
    // Field for managing saved signs, persisting in savedSigns.yml.
    private SignLibraryManager signLibraryManager;
    // Field for managing command cooldowns
    private CooldownManager cooldownManager;
    // ReadWriteLock for thread-safe configuration access
    private final ReadWriteLock configLock = new ReentrantReadWriteLock();
    // Field for managing server-wide templates
    private ServerTemplateManager serverTemplateManager;
    // Field for managing confirmation prompts
    private static ConfirmationManager confirmationManager;
    // Field for centralized configuration management
    private ConfigManager configManager;
    // Field for debug logging
    private DebugLogger debugLogger;
    // Field for WorldGuard integration
    private WorldGuardIntegration worldGuardIntegration;
    // Field for sound manager
    private SoundManager soundManager;
    // Field for metrics manager
    private MetricsManager metricsManager;

    /**
     * Initializes the plugin when it is enabled.
     * <p>
     * This method performs the following initialization tasks:
     * <ul>
     *   <li>Sets up the static plugin instance reference</li>
     *   <li>Saves default configuration files (config.yml, messages.yml)</li>
     *   <li>Initializes all manager classes (toggle, library, cooldown, template)</li>
     *   <li>Loads and initializes the language system</li>
     *   <li>Sets up bStats metrics collection (if enabled)</li>
     *   <li>Registers command executors and event listeners</li>
     *   <li>Starts periodic cleanup tasks</li>
     * </ul>
     * <p>
     * The method ensures that all components are properly initialized and connected
     * before the plugin becomes fully operational.
     * 
     * @see #onDisable()
     * @see Lang#init(CopySign)
     */
    @Override
    public void onEnable() {
        instance = this; // Set the static instance
        // Save default configurations
        saveDefaultConfig();
        saveResource("messages.yml", false);
        
        // Initialize toggle manager to load players.yml state (and create it if missing)
        toggleManager = new CopySignToggleManager(getDataFolder(), this);
        // Initialize the sign library manager using savedSigns.yml.
        signLibraryManager = new SignLibraryManager(getDataFolder(), this);
        // Initialize the cooldown manager
        cooldownManager = new CooldownManager(this);
        // Initialize the server template manager
        serverTemplateManager = new ServerTemplateManager(getDataFolder(), this);
        // Initialize the confirmation manager
        confirmationManager = new ConfirmationManager(this);
        
        // Load messages (no lock needed during startup)
        reloadConfig();
        
        // Perform configuration migration if needed
        ConfigMigrator migrator = new ConfigMigrator(this);
        if (migrator.migrate()) {
            // Reload config after migration
            reloadConfig();
        }
        
        Lang.init(this);
        
        // Initialize configuration manager
        configManager = new ConfigManager(this);
        
        // Perform comprehensive configuration validation
        ConfigValidator validator = new ConfigValidator(this);
        if (!validator.validate()) {
            getLogger().severe("Critical configuration errors detected! Please fix them and restart the server.");
            // Still continue loading but with warnings
        }
        
        // Run the basic validation too
        configManager.validateConfiguration();
        
        // Initialize debug logger
        debugLogger = new DebugLogger(this);
        if (configManager.isDebugEnabled()) {
            getLogger().info("Debug mode is enabled. Additional logging will be shown.");
        }
        
        // Initialize WorldGuard integration
        worldGuardIntegration = new WorldGuardIntegration(this);
        
        // Initialize sound manager
        soundManager = new SoundManager(this);
        
        // Initialize metrics manager
        metricsManager = new MetricsManager(this);
        
        // Initialize bStats metrics if enabled in config
        if (configManager.isMetricsEnabled()) {
            int pluginId = 26118;
            Metrics metrics = new Metrics(this, pluginId);
            
            // Add custom charts
            // Track which features are enabled
            metrics.addCustomChart(new SimplePie("sign_library_enabled", () -> 
                getConfig().getBoolean("features.sign-library", true) ? "Enabled" : "Disabled"
            ));
            
            metrics.addCustomChart(new SimplePie("copy_colors_enabled", () -> 
                getConfig().getBoolean("features.copy-colors", true) ? "Enabled" : "Disabled"
            ));
            
            metrics.addCustomChart(new SimplePie("copy_glow_enabled", () -> 
                getConfig().getBoolean("features.copy-glow", true) ? "Enabled" : "Disabled"
            ));
            
            // Track max saved signs setting
            metrics.addCustomChart(new SimplePie("max_saved_signs", () -> {
                int maxSigns = getConfig().getInt("library.max-saved-signs", 50);
                if (maxSigns == -1) return "Unlimited";
                else if (maxSigns <= 10) return "1-10";
                else if (maxSigns <= 25) return "11-25";
                else if (maxSigns <= 50) return "26-50";
                else if (maxSigns <= 100) return "51-100";
                else return "100+";
            }));
            
            // Track server templates feature
            metrics.addCustomChart(new SimplePie("server_templates_enabled", () -> 
                configManager.isServerTemplatesEnabled() ? "Enabled" : "Disabled"
            ));
            
            // Track WorldGuard integration
            metrics.addCustomChart(new SimplePie("worldguard_integration", () -> {
                if (!configManager.respectWorldGuard()) return "Disabled in Config";
                else if (worldGuardIntegration.isEnabled()) return "Active";
                else return "Not Found";
            }));
            
            // Track sound system usage
            metrics.addCustomChart(new SimplePie("sound_effects", () -> 
                soundManager.isEnabled() ? "Enabled" : "Disabled"
            ));
            
            // Track sneak requirements
            metrics.addCustomChart(new SimplePie("sneak_requirements", () -> {
                boolean copy = configManager.requireSneakToCopy();
                boolean paste = configManager.requireSneakToPaste();
                if (copy && paste) return "Both";
                else if (copy) return "Copy Only";
                else if (paste) return "Paste Only";
                else return "None";
            }));
            
            // Track auto-save interval
            metrics.addCustomChart(new SimplePie("auto_save_interval", () -> {
                int interval = configManager.getAutoSaveInterval();
                if (interval <= 0) return "Disabled";
                else if (interval <= 5) return "1-5 minutes";
                else if (interval <= 10) return "6-10 minutes";
                else if (interval <= 30) return "11-30 minutes";
                else return "30+ minutes";
            }));
            
            // Track update checking
            metrics.addCustomChart(new SimplePie("update_checking", () -> 
                configManager.checkForUpdates() ? "Enabled" : "Disabled"
            ));
            
            // Track debug mode usage
            metrics.addCustomChart(new SimplePie("debug_mode", () -> 
                configManager.isDebugEnabled() ? "Enabled" : "Disabled"
            ));
            
            // Add advanced metrics
            metricsManager.setupAdvancedMetrics(metrics);
            
            getLogger().info("bStats metrics enabled. Thank you for helping improve CopySign!");
        }
        
        // Register command executor with both toggle and sign library manager dependencies.
        us.ironcladnetwork.copySign.Commands.CopySignCommand commandExecutor = new us.ironcladnetwork.copySign.Commands.CopySignCommand(toggleManager, signLibraryManager);
        getCommand("copysign").setExecutor(commandExecutor);
        // Tab completion is handled by CopySignCommand itself (implements TabCompleter)
        getCommand("copysign").setTabCompleter(commandExecutor);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new SignCopyListener(), this);
        getServer().getPluginManager().registerEvents(new SignPlaceListener(), this);
        // Register the SignChangeListener to apply cached NBT data on sign change.
        getServer().getPluginManager().registerEvents(new us.ironcladnetwork.copySign.Listeners.SignChangeListener(), this);
        // Register the new SignLibraryGUIListener so that GUI events are processed.
        getServer().getPluginManager().registerEvents(new SignLibraryGUIListener(signLibraryManager), this);
        // Register the PlayerQuitListener for cooldown cleanup
        getServer().getPluginManager().registerEvents(new us.ironcladnetwork.copySign.Listeners.PlayerQuitListener(), this);
        // Register the ServerTemplateGUIListener
        getServer().getPluginManager().registerEvents(new us.ironcladnetwork.copySign.Listeners.ServerTemplateGUIListener(serverTemplateManager), this);
        
        // Start periodic cooldown cleanup task (every 5 minutes)
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            // Snapshot config values on main thread for thread safety
            Map<String, Integer> cooldownConfig = new HashMap<>();
            configLock.readLock().lock();
            try {
                // Read all cooldown configurations while holding read lock
                if (getConfig().contains("commands.cooldowns")) {
                    ConfigurationSection cooldownsSection = getConfig().getConfigurationSection("commands.cooldowns");
                    if (cooldownsSection != null) {
                        for (String command : cooldownsSection.getKeys(false)) {
                            cooldownConfig.put(command, cooldownsSection.getInt(command, 0));
                        }
                    }
                }
            } finally {
                configLock.readLock().unlock();
            }
            
            // Pass the snapshotted config to cleanup method
            cooldownManager.cleanupExpiredCooldowns(cooldownConfig);
        }, 6000L, 6000L); // 6000 ticks = 5 minutes
        
        // Start periodic SignDataCache cleanup task (every 5 minutes)
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            SignDataCache.cleanupExpiredEntries();
        }, 6000L, 6000L); // 6000 ticks = 5 minutes
        
        // Check for updates if enabled
        if (configManager.checkForUpdates()) {
            new UpdateChecker(this).checkForUpdate(latestVersion -> {
                // Callback is handled inside UpdateChecker
            });
        }
        
        // Start auto-save task if enabled
        int autoSaveInterval = configManager.getAutoSaveInterval();
        if (autoSaveInterval > 0) {
            long intervalTicks = autoSaveInterval * 60L * 20L; // Convert minutes to ticks
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                long startTime = System.currentTimeMillis();
                
                // Perform saves asynchronously
                boolean librarySuccess = signLibraryManager.saveConfigSync();
                boolean toggleSuccess = toggleManager.saveConfigSync();
                
                long timeTaken = System.currentTimeMillis() - startTime;
                
                // Log results on main thread
                getServer().getScheduler().runTask(this, () -> {
                    if (librarySuccess && toggleSuccess) {
                        debugLogger.debug("Auto-save completed successfully");
                        debugLogger.debugPerformance("Auto-save", timeTaken);
                    } else {
                        getLogger().warning("Auto-save encountered errors - Library: " + 
                            (librarySuccess ? "OK" : "FAILED") + ", Toggles: " + 
                            (toggleSuccess ? "OK" : "FAILED"));
                    }
                });
            }, intervalTicks, intervalTicks);
            
            getLogger().info("Auto-save enabled - saving every " + autoSaveInterval + " minutes");
        }
    }

    /**
     * Performs cleanup when the plugin is disabled.
     * <p>
     * This method ensures that all data is properly saved before shutdown.
     * Saves are performed synchronously to guarantee data persistence.
     * 
     * @see #onEnable()
     */
    @Override
    public void onDisable() {
        // Clear the sign data cache on shutdown
        SignDataCache.clear();
        
        // Clear any pending confirmations
        if (confirmationManager != null) {
            confirmationManager.clearAll();
        }
        
        // Save all manager data synchronously to prevent data loss
        getLogger().info("Saving plugin data...");
        
        // Save sign library data
        if (signLibraryManager != null) {
            if (signLibraryManager.saveConfigSync()) {
                getLogger().info("Sign library data saved successfully.");
            } else {
                getLogger().warning("Failed to save sign library data!");
            }
        }
        
        // Save player toggle states
        if (toggleManager != null) {
            if (toggleManager.saveConfigSync()) {
                getLogger().info("Player toggle states saved successfully.");
            } else {
                getLogger().warning("Failed to save player toggle states!");
            }
        }
        
        // Server templates are saved immediately when modified, no need for explicit save
        
        // Save main plugin configuration
        saveConfig();
        getLogger().info("Plugin configuration saved successfully.");
    }

    /**
     * Reloads the plugin's configuration and messages.
     * Thread-safe implementation prevents race conditions during concurrent operations.
     */
    public void reloadPlugin() {
        configLock.writeLock().lock();
        try {
            getLogger().info("Reloading plugin configuration...");
            
            // Reload the main configuration
            reloadConfig();
            
            // Validate the reloaded configuration
            ConfigValidator validator = new ConfigValidator(this);
            if (!validator.validate()) {
                getLogger().severe("Configuration errors detected after reload!");
            }
            
            // Reinitialize language messages with new config
            Lang.init(this);
            
            // Reload configuration manager cache
            if (configManager != null) {
                configManager.reloadCache();
                configManager.validateConfiguration();
            }
            
            // Reinitialize debug logger with new settings
            if (debugLogger != null) {
                debugLogger = new DebugLogger(this);
                if (configManager.isDebugEnabled()) {
                    getLogger().info("Debug mode is enabled after reload.");
                }
            }
            
            // Reload server template manager
            if (serverTemplateManager != null) {
                serverTemplateManager.reload();
            }
            
            // Reload WorldGuard integration
            if (worldGuardIntegration != null) {
                worldGuardIntegration.reload();
            }
            
            // Reload sound manager
            if (soundManager != null) {
                soundManager.reload();
            }
            
            // Clear toggle cache if caching is now disabled
            if (toggleManager != null && !configManager.isCacheToggleStates()) {
                toggleManager.clearCache();
                getLogger().info("Toggle state caching disabled, cache cleared.");
            }
            
            getLogger().info("Plugin configuration reloaded successfully.");
            
        } catch (Exception e) {
            getLogger().severe("Failed to reload plugin configuration: " + e.getMessage());
            e.printStackTrace();
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Thread-safe configuration access method.
     * Uses read lock to allow concurrent reads while preventing reads during config reload.
     * 
     * @param configAction A function that takes the config and returns a result
     * @return The result of the config action
     */
    public <T> T withConfigLock(java.util.function.Function<org.bukkit.configuration.file.FileConfiguration, T> configAction) {
        configLock.readLock().lock();
        try {
            return configAction.apply(getConfig());
        } finally {
            configLock.readLock().unlock();
        }
    }
    
    /**
     * Thread-safe configuration access method for operations that don't return values.
     * 
     * @param configAction A consumer that uses the config
     */
    public void withConfigLock(java.util.function.Consumer<org.bukkit.configuration.file.FileConfiguration> configAction) {
        configLock.readLock().lock();
        try {
            configAction.accept(getConfig());
        } finally {
            configLock.readLock().unlock();
        }
    }
    
    /**
     * Thread-safe method to get a boolean value from config.
     * 
     * @param path The configuration path
     * @param defaultValue The default value if not found
     * @return The boolean value from config
     */
    public boolean getConfigBoolean(String path, boolean defaultValue) {
        configLock.readLock().lock();
        try {
            return getConfig().getBoolean(path, defaultValue);
        } finally {
            configLock.readLock().unlock();
        }
    }
    
    /**
     * Thread-safe method to get an int value from config.
     * 
     * @param path The configuration path
     * @param defaultValue The default value if not found
     * @return The int value from config
     */
    public int getConfigInt(String path, int defaultValue) {
        configLock.readLock().lock();
        try {
            return getConfig().getInt(path, defaultValue);
        } finally {
            configLock.readLock().unlock();
        }
    }
    
    /**
     * Thread-safe method to get a string value from config.
     * 
     * @param path The configuration path
     * @param defaultValue The default value if not found
     * @return The string value from config
     */
    public String getConfigString(String path, String defaultValue) {
        configLock.readLock().lock();
        try {
            return getConfig().getString(path, defaultValue);
        } finally {
            configLock.readLock().unlock();
        }
    }
    
    /**
     * Thread-safe method to check if config contains a path.
     * 
     * @param path The configuration path
     * @return True if the path exists
     */
    public boolean configContains(String path) {
        configLock.readLock().lock();
        try {
            return getConfig().contains(path);
        } finally {
            configLock.readLock().unlock();
        }
    }
    
    /**
     * Thread-safe method to get a configuration section.
     * 
     * @param path The configuration path
     * @return The configuration section or null if not found
     */
    public ConfigurationSection getConfigSectionSafe(String path) {
        configLock.readLock().lock();
        try {
            return getConfig().getConfigurationSection(path);
        } finally {
            configLock.readLock().unlock();
        }
    }
    
    /**
     * Static getter to access the toggle manager from other classes.
     *
     * @return the CopySignToggleManager instance.
     */
    public static CopySignToggleManager getToggleManager() {
        return instance.toggleManager;
    }

    /**
     * Returns the instance of the plugin.
     *
     * @return the plugin instance.
     */
    public static CopySign getInstance() {
        return instance;
    }
    
    /**
     * Static getter to access the cooldown manager from other classes.
     *
     * @return the CooldownManager instance.
     */
    public static CooldownManager getCooldownManager() {
        return instance.cooldownManager;
    }
    
    /**
     * Static getter to access the server template manager from other classes.
     *
     * @return the ServerTemplateManager instance.
     */
    public static ServerTemplateManager getServerTemplateManager() {
        return instance.serverTemplateManager;
    }
    
    /**
     * Gets the confirmation manager instance.
     * @return The confirmation manager
     */
    public static ConfirmationManager getConfirmationManager() {
        return confirmationManager;
    }
    
    /**
     * Gets the configuration manager instance.
     * Provides centralized access to configuration values.
     * @return The configuration manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Gets the debug logger instance.
     * @return The debug logger
     */
    public DebugLogger getDebugLogger() {
        return debugLogger;
    }
    
    /**
     * Gets the WorldGuard integration instance.
     * @return The WorldGuard integration
     */
    public WorldGuardIntegration getWorldGuardIntegration() {
        return worldGuardIntegration;
    }
    
    /**
     * Gets the sound manager instance.
     * @return The sound manager
     */
    public SoundManager getSoundManager() {
        return soundManager;
    }
    
    /**
     * Gets the metrics manager instance.
     * @return The metrics manager
     */
    public MetricsManager getMetricsManager() {
        return metricsManager;
    }
}
