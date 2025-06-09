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
import us.ironcladnetwork.copySign.Listeners.SignLibraryGUIListener;

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
    // Field for managing server-wide templates
    private ServerTemplateManager serverTemplateManager;

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
        
        // Load messages
        reloadConfig();
        Lang.init(this);
        
        // Initialize bStats metrics if enabled in config
        if (getConfig().getBoolean("metrics.enabled", true)) {
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
            
            getLogger().info("bStats metrics enabled. Thank you for helping improve CopySign!");
        }
        
        // Register command executor with both toggle and sign library manager dependencies.
        getCommand("copysign").setExecutor(new us.ironcladnetwork.copySign.Commands.CopySignCommand(toggleManager, signLibraryManager));
        // Tab completion is handled by CopySignCommand itself (implements TabCompleter)
        // getCommand("copysign").setTabCompleter(new us.ironcladnetwork.copySign.Commands.CopySignTabCompleter(signLibraryManager));
        
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
            cooldownManager.cleanupExpiredCooldowns();
        }, 6000L, 6000L); // 6000 ticks = 5 minutes
    }

    /**
     * Performs cleanup when the plugin is disabled.
     * <p>
     * This method ensures that the plugin's configuration is saved before shutdown.
     * Additional cleanup tasks may be added in the future as needed.
     * 
     * @see #onEnable()
     */
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        saveConfig();
    }

    /**
     * Reloads the plugin's configuration and messages.
     */
    public void reloadPlugin() {
        reloadConfig();
        Lang.init(this);
        serverTemplateManager.reload();
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
}
