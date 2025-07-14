package us.ironcladnetwork.copySign.Util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import us.ironcladnetwork.copySign.CopySign;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manager for handling individual player CopySign feature toggle states.
 * <p>
 * This class manages whether the CopySign functionality is enabled or disabled for each
 * player individually. It provides persistent storage of player preferences using a
 * YAML configuration file ("players.yml") in the plugin's data folder.
 * <p>
 * Key features:
 * <ul>
 *   <li>Per-player toggle state management</li>
 *   <li>Persistent storage in players.yml</li>
 *   <li>Configurable default state for new players</li>
 *   <li>Automatic file creation and error recovery</li>
 *   <li>Memory caching for performance</li>
 * </ul>
 * <p>
 * The manager loads all player states into memory on initialization and saves
 * changes immediately when toggle states are modified. New players inherit the
 * default state from the plugin configuration.
 * 
 * @author IroncladNetwork
 * @since 2.0.0
 * @see CopySign
 * @see ErrorHandler
 */
public class CopySignToggleManager {
    private final Map<UUID, Boolean> playerStates = new HashMap<>();
    private final File playersFile;
    private YamlConfiguration playersConfig;
    private final CopySign plugin;

    /**
     * Initializes the manager by loading player states from the specified data folder.
     * If the players.yml file does not exist, it will be created with default values.
     *
     * @param dataFolder The plugin's data folder.
     * @param plugin The plugin instance for accessing config.
     */
    public CopySignToggleManager(File dataFolder, CopySign plugin) {
        this.plugin = plugin;
        playersFile = new File(dataFolder, "players.yml");
        
        if (!playersFile.exists()) {
            try {
                // Ensure the parent directories exist.
                if (!playersFile.getParentFile().exists() && !playersFile.getParentFile().mkdirs()) {
                    throw new IOException("Failed to create plugin data directory");
                }
                
                if (!playersFile.createNewFile()) {
                    throw new IOException("Failed to create players.yml file");
                }
                
                playersConfig = new YamlConfiguration();
                playersConfig.createSection("players");
                saveConfig();
                
                ErrorHandler.debug("Created new players.yml file");
            } catch (IOException e) {
                ErrorHandler.handleFileError("creating players.yml", playersFile, e, null);
                // Create a minimal in-memory config as fallback
                playersConfig = new YamlConfiguration();
                playersConfig.createSection("players");
            }
        } else {
            try {
                playersConfig = YamlConfiguration.loadConfiguration(playersFile);
                
                if (playersConfig == null) {
                    throw new IllegalStateException("Failed to load configuration from players.yml");
                }
                
                ErrorHandler.debug("Successfully loaded players.yml");
            } catch (Exception e) {
                ErrorHandler.handleConfigError("players.yml", e);
                // Create a minimal in-memory config as fallback
                playersConfig = new YamlConfiguration();
                playersConfig.createSection("players");
            }
        }
        
        loadStates();
    }

    /**
     * Loads player CopySign states from the players.yml file into memory.
     */
    private void loadStates() {
        try {
            if (playersConfig != null && playersConfig.contains("players")) {
                ConfigurationSection playersSection = playersConfig.getConfigurationSection("players");
                if (playersSection != null) {
                    playersSection.getKeys(false).forEach(key -> {
                        try {
                            UUID uuid = UUID.fromString(key);
                            boolean status = playersConfig.getBoolean("players." + key);
                            playerStates.put(uuid, status);
                        } catch (IllegalArgumentException ex) {
                            ErrorHandler.handlePlayerDataError(key, "toggle state", ex);
                        }
                    });
                }
            }
            
            ErrorHandler.debug("Loaded " + playerStates.size() + " player toggle states");
        } catch (Exception e) {
            ErrorHandler.handleGeneralError("loading player toggle states", e, null);
        }
    }

    /**
     * Saves the current player states into the players.yml file.
     */
    private void saveConfig() {
        try {
            // Create backup before saving
            ErrorHandler.createBackup(playersFile);
            
            // Validate config before saving
            if (playersConfig == null) {
                throw new IllegalStateException("Configuration is null, cannot save");
            }
            
            playerStates.forEach((uuid, state) ->
                    playersConfig.set("players." + uuid.toString(), state)
            );
            
            playersConfig.save(playersFile);
            ErrorHandler.debug("Successfully saved players.yml");
            
        } catch (IOException e) {
            ErrorHandler.handleFileError("saving players.yml", playersFile, e, null);
        } catch (Exception e) {
            ErrorHandler.handleGeneralError("saving player toggle configuration", e, null);
        }
    }

    /**
     * Asynchronously saves the current player toggle configuration to prevent main thread blocking.
     * This method creates a backup and saves the file in a separate thread to avoid server lag.
     * 
     * @param player The player whose toggle state was changed (for error reporting)
     * @param enabled The new toggle state (for debug logging)
     */
    private void saveConfigAsync(Player player, boolean enabled) {
        CompletableFuture.runAsync(() -> {
            try {
                // Create backup before saving (async)
                ErrorHandler.createBackup(playersFile);
                
                // Validate config before saving
                if (playersConfig == null) {
                    throw new IllegalStateException("Configuration is null, cannot save");
                }
                
                // Save the configuration file
                playersConfig.save(playersFile);
                
                ErrorHandler.debug("Asynchronously saved toggle state for " + player.getName() + " to " + enabled);
                
            } catch (IOException e) {
                ErrorHandler.handleFileError("saving player toggle state", playersFile, e, player);
            } catch (Exception e) {
                ErrorHandler.handleGeneralError("saving player toggle configuration async", e, player);
            }
        });
    }

    /**
     * Synchronously saves the configuration.
     * Used during plugin shutdown to ensure data is saved before disable.
     * 
     * @return true if save was successful, false otherwise
     */
    public boolean saveConfigSync() {
        try {
            // Create backup before saving
            ErrorHandler.createBackup(playersFile);
            
            // Save the configuration file
            playersConfig.save(playersFile);
            
            ErrorHandler.debug("Synchronously saved player toggle states");
            return true;
            
        } catch (IOException e) {
            ErrorHandler.handleFileError("saving player toggle states", playersFile, e, null);
            return false;
        } catch (Exception e) {
            ErrorHandler.handleGeneralError("saving player toggle configuration sync", e, null);
            return false;
        }
    }

    /**
     * Checks if CopySign is enabled for the given player.
     * Uses the config default for new players.
     *
     * @param player The player to check.
     * @return true if CopySign is enabled, false otherwise.
     */
    public boolean isEnabled(Player player) {
        boolean defaultEnabled = plugin.getConfigBoolean("general.default-enabled", true);
        
        // If caching is disabled, always load from config
        if (!plugin.getConfigManager().isCacheToggleStates()) {
            if (playersConfig.contains("players." + player.getUniqueId().toString())) {
                return playersConfig.getBoolean("players." + player.getUniqueId().toString(), defaultEnabled);
            }
            return defaultEnabled;
        }
        
        // Use cached value
        return playerStates.getOrDefault(player.getUniqueId(), defaultEnabled);
    }

    /**
     * Sets the CopySign enabled state for the given player.
     *
     * @param player  The player to update.
     * @param enabled true to enable CopySign, false to disable.
     */
    public void setCopySignEnabled(Player player, boolean enabled) {
        try {
            if (player == null) {
                ErrorHandler.handleGeneralError("setting toggle state with null player", new IllegalArgumentException("Player cannot be null"), null);
                return;
            }
            
            // Update in-memory state immediately (synchronous for instant response)
            playerStates.put(player.getUniqueId(), enabled);
            playersConfig.set("players." + player.getUniqueId().toString(), enabled);
            
            // Save to file asynchronously to prevent main thread blocking
            saveConfigAsync(player, enabled);
            
        } catch (Exception e) {
            ErrorHandler.handleGeneralError("setting player toggle state", e, player);
        }
    }

    /**
     * Toggles the CopySign status for the given player.
     *
     * @param player The player to toggle.
     * @return The new status after toggling.
     */
    public boolean toggle(Player player) {
        boolean newState = !isEnabled(player);
        setCopySignEnabled(player, newState);
        return newState;
    }
    
    /**
     * Clears cached toggle state for a player.
     * Called when a player quits to free memory if caching is enabled.
     *
     * @param player The player whose cache to clear
     */
    public void clearPlayerCache(Player player) {
        if (plugin.getConfigManager().isCacheToggleStates()) {
            playerStates.remove(player.getUniqueId());
            ErrorHandler.debug("Cleared toggle cache for player: " + player.getName());
        }
    }
    
    /**
     * Gets the current size of the toggle state cache.
     * Useful for monitoring memory usage.
     *
     * @return Number of players in cache
     */
    public int getCacheSize() {
        return playerStates.size();
    }
    
    /**
     * Clears the entire cache.
     * Used during reload or when caching is disabled.
     */
    public void clearCache() {
        playerStates.clear();
        ErrorHandler.debug("Cleared entire toggle state cache");
    }
}