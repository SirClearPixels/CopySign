package us.ironcladnetwork.copySign.Util;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import us.ironcladnetwork.copySign.CopySign;
import us.ironcladnetwork.copySign.Lang.Lang;

/**
 * Manager for handling players' saved signs.
 * 
 * This class handles:
 * • Loading and saving the YAML configuration from savedSigns.yml.
 * • Saving, retrieving, listing, and deleting sign entries.
 */
public class SignLibraryManager {

    private final File signLibraryFile;
    private YamlConfiguration signLibraryConfig;
    private final CopySign plugin;
    
    // Lock for thread-safe access to signLibraryConfig
    private final ReentrantLock configLock = new ReentrantLock();

    /**
     * Initializes the manager by loading the savedSigns.yml file.
     * If the file doesn't exist, it will be created with a default "players" section.
     *
     * @param dataFolder The plugin's data folder.
     * @param plugin The plugin instance for accessing config.
     */
    public SignLibraryManager(File dataFolder, CopySign plugin) {
        this.plugin = plugin;
        signLibraryFile = new File(dataFolder, "savedSigns.yml");
        
        if (!signLibraryFile.exists()) {
            try {
                // Ensure the parent directories exist.
                if (!signLibraryFile.getParentFile().exists() && !signLibraryFile.getParentFile().mkdirs()) {
                    throw new IOException("Failed to create plugin data directory");
                }
                
                if (!signLibraryFile.createNewFile()) {
                    throw new IOException("Failed to create savedSigns.yml file");
                }
                
                signLibraryConfig = new YamlConfiguration();
                signLibraryConfig.createSection("players");
                saveConfig();
                
                ErrorHandler.debug("Created new savedSigns.yml file");
            } catch (IOException e) {
                ErrorHandler.handleFileError("creating savedSigns.yml", signLibraryFile, e, null);
                // Create a minimal in-memory config as fallback
                signLibraryConfig = new YamlConfiguration();
                signLibraryConfig.createSection("players");
            }
        } else {
            try {
                signLibraryConfig = YamlConfiguration.loadConfiguration(signLibraryFile);
                
                // Validate the loaded configuration
                if (signLibraryConfig == null) {
                    throw new IllegalStateException("Failed to load configuration from savedSigns.yml");
                }
                
                // Ensure the top-level "players" section exists.
                if (!signLibraryConfig.contains("players")) {
                    signLibraryConfig.createSection("players");
                    saveConfig(); // Save the corrected structure
                }
                
                ErrorHandler.debug("Successfully loaded savedSigns.yml with " + 
                    signLibraryConfig.getConfigurationSection("players").getKeys(false).size() + " players");
                    
            } catch (Exception e) {
                ErrorHandler.handleConfigError("savedSigns.yml", e);
                // Create a minimal in-memory config as fallback
                signLibraryConfig = new YamlConfiguration();
                signLibraryConfig.createSection("players");
            }
        }
    }

    /**
     * Persists changes to the savedSigns.yml file.
     * Creates a backup before saving and handles errors gracefully.
     */
    private void saveConfig() {
        saveConfigAsync(null);
    }
    
    /**
     * Synchronously saves the configuration.
     * Used during plugin shutdown to ensure data is saved before disable.
     * Thread-safe implementation using ReentrantLock.
     * 
     * @return true if save was successful, false otherwise
     */
    public boolean saveConfigSync() {
        // Acquire lock for thread-safe access to signLibraryConfig
        configLock.lock();
        try {
            // Create backup before saving
            ErrorHandler.createBackup(signLibraryFile);
            
            // Validate config before saving
            if (signLibraryConfig == null) {
                throw new IllegalStateException("Configuration is null, cannot save");
            }
            
            // Save the configuration while holding the lock
            signLibraryConfig.save(signLibraryFile);
            ErrorHandler.debug("Successfully saved savedSigns.yml synchronously");
            return true;
            
        } catch (IOException e) {
            ErrorHandler.handleFileError("saving savedSigns.yml", signLibraryFile, e, null);
            return false;
        } catch (Exception e) {
            ErrorHandler.handleGeneralError("saving sign library configuration", e, null);
            return false;
        } finally {
            // Always release the lock
            configLock.unlock();
        }
    }
    
    /**
     * Asynchronously persists changes to the savedSigns.yml file.
     * Creates a backup before saving and handles errors gracefully.
     * Thread-safe implementation using ReentrantLock.
     * 
     * @param callback Optional callback to execute after save completion
     * @return CompletableFuture that completes when save is done
     */
    private CompletableFuture<Boolean> saveConfigAsync(Consumer<Boolean> callback) {
        return CompletableFuture.supplyAsync(() -> {
            // Acquire lock for thread-safe access to signLibraryConfig
            configLock.lock();
            try {
                // Create backup before saving
                ErrorHandler.createBackup(signLibraryFile);
                
                // Validate config before saving
                if (signLibraryConfig == null) {
                    throw new IllegalStateException("Configuration is null, cannot save");
                }
                
                // Save the configuration while holding the lock
                signLibraryConfig.save(signLibraryFile);
                ErrorHandler.debug("Successfully saved savedSigns.yml");
                return true;
                
            } catch (IOException e) {
                ErrorHandler.handleFileError("saving savedSigns.yml", signLibraryFile, e, null);
                return false;
            } catch (Exception e) {
                ErrorHandler.handleGeneralError("saving sign library configuration", e, null);
                return false;
            } finally {
                // Always release the lock
                configLock.unlock();
            }
        }).thenApply(result -> {
            if (callback != null) {
                // Execute callback on main thread
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
            }
            return result;
        });
    }

    /**
     * Saves a sign for the specified player under the given name.
     * <p>
     * Validates that the provided sign item contains the required NBT tags.
     * Extracts the front/back text, side colors, glow state, sign type, and lore
     * and saves the data under the player's UUID.
     * 
     * @param player   The player saving the sign.
     * @param name     The identifier name for the sign.
     * @param signItem The sign item holding the stored NBT data.
     */
    public void saveSign(Player player, String name, ItemStack signItem) {
        try {
            // Validate input parameters
            if (player == null) {
                ErrorHandler.handleGeneralError("saving sign with null player", new IllegalArgumentException("Player cannot be null"), null);
                return;
            }
            
            if (!ErrorHandler.isValidFileName(name, 32)) {
                player.sendMessage(Lang.INVALID_SIGN_NAME_FORMAT.getWithPrefix());
                return;
            }
            
            // Validate that the signItem is not null and is of a sign type.
            if (signItem == null || signItem.getType() == Material.AIR || !signItem.getType().name().endsWith("_SIGN")) {
                player.sendMessage(Lang.INVALID_SIGN_ITEM_ERROR.getWithPrefix());
                return;
            }
            
            NBTItem nbtItem = new NBTItem(signItem);
            // Ensure required NBT tags are present.
            if (!nbtItem.hasTag("copiedSignFront") || !nbtItem.hasTag("copiedSignBack")) {
                player.sendMessage(Lang.SIGN_NO_REQUIRED_DATA.getWithPrefix());
                return;
            }
            
                // Check max saved signs limit (permission-aware)
            int configDefault = plugin.getConfigInt("library.max-saved-signs", 50);
            int maxSigns = Permissions.getMaxLibrarySigns(player, configDefault);
            if (maxSigns != -1) { // -1 means unlimited
                Map<String, SavedSignData> existingSigns = getAllSigns(player);
                // If the sign doesn't already exist and we're at the limit
                if (!existingSigns.containsKey(name) && existingSigns.size() >= maxSigns) {
                    player.sendMessage(Lang.MAX_SIGNS_REACHED.formatWithPrefix("{max}", String.valueOf(maxSigns)));
                    return;
                }
            }
            
            // Extract sign information from NBT.
            String copiedSignFront = nbtItem.getString("copiedSignFront");
            String copiedSignBack = nbtItem.getString("copiedSignBack");
            
            // Validate NBT data for security
            if (!NBTValidationUtil.validateNBTData(copiedSignFront) || !NBTValidationUtil.validateNBTData(copiedSignBack)) {
                player.sendMessage(Lang.PREFIX.get() + "§cSign data is too large or invalid.");
                return;
            }
            
            String copiedFrontColor = nbtItem.hasTag("copiedSignFrontColor") ? nbtItem.getString("copiedSignFrontColor") : "OAK";
            String copiedBackColor = nbtItem.hasTag("copiedSignBackColor") ? nbtItem.getString("copiedSignBackColor") : "OAK";
            
            // Validate color values
            if (!SignValidationUtil.isValidDyeColor(copiedFrontColor) || !SignValidationUtil.isValidDyeColor(copiedBackColor)) {
                player.sendMessage(Lang.PREFIX.get() + "§cInvalid sign color data.");
                return;
            }
            
            boolean signGlowing = nbtItem.hasTag("signGlowing") && nbtItem.getBoolean("signGlowing");
            String signType = nbtItem.hasTag("signType") ? nbtItem.getString("signType") : "regular";
            
            // Validate sign type
            if (!isValidSignType(signType)) {
                player.sendMessage(Lang.PREFIX.get() + "§cInvalid sign type data.");
                return;
            }

            // Get lore from the item meta if present.
            java.util.List<String> lore = null;
            if (signItem.hasItemMeta()) {
                ItemMeta itemMeta = signItem.getItemMeta();
                if (itemMeta != null && itemMeta.hasLore()) {
                    lore = itemMeta.getLore();
                }
            }
            
            if (lore != null) {
                // Validate lore content
                if (!isValidLore(lore)) {
                    player.sendMessage(Lang.PREFIX.get() + "§cSign lore contains invalid data.");
                    return;
                }
            }

            // Process the front/back text into arrays of lines.
            String[] frontLines = copiedSignFront.split("\n", -1);
            String[] backLines = copiedSignBack.split("\n", -1);

            // Create a SavedSignData instance using the extracted data.
            SavedSignData savedData = new SavedSignData(frontLines, backLines, signGlowing, copiedFrontColor, copiedBackColor, signType, lore);

            // Save the data under the player's UUID and the provided sign name.
            // Use lock for thread-safe access to signLibraryConfig
            UUID playerId = player.getUniqueId();
            configLock.lock();
            try {
                ConfigurationSection playersSection = signLibraryConfig.getConfigurationSection("players");
                ConfigurationSection playerSection = playersSection.getConfigurationSection(playerId.toString());
                if (playerSection == null) {
                    playerSection = playersSection.createSection(playerId.toString());
                }
                // Create or override a section for this sign.
                ConfigurationSection signSection = playerSection.createSection(name);
                savedData.saveToConfigurationSection(signSection);
            } finally {
                configLock.unlock();
            }

            // Persist the updated configuration asynchronously
            saveConfigAsync(success -> {
                if (success) {
                    player.sendMessage(Lang.SIGN_SAVED_SUCCESSFULLY.getWithPrefix());
                    // Play save sound effect
                    CopySign.getInstance().getSoundManager().playSaveSound(player);
                    // Record metrics
                    CopySign.getInstance().getMetricsManager().recordSaveOperation(player);
                } else {
                    player.sendMessage(Lang.PREFIX.get() + "§cFailed to save sign. Please try again.");
                    // Play error sound effect
                    CopySign.getInstance().getSoundManager().playErrorSound(player);
                }
            });
            
        } catch (Exception e) {
            ErrorHandler.handleGeneralError("saving sign to library", e, player);
        }
    }

    /**
     * Retrieves the saved sign data for a given player and sign name.
     *
     * @param player The player.
     * @param name   The sign's identifier.
     * @return The SavedSignData object if found, otherwise null.
     */
    public SavedSignData getSign(Player player, String name) {
        UUID playerId = player.getUniqueId();
        
        // Use lock for thread-safe access to signLibraryConfig
        configLock.lock();
        try {
            ConfigurationSection playerSection = signLibraryConfig.getConfigurationSection("players." + playerId.toString());
            if (playerSection == null)
                return null;
            ConfigurationSection signSection = playerSection.getConfigurationSection(name);
            if (signSection == null)
                return null;
            return SavedSignData.loadFromConfigurationSection(signSection);
        } finally {
            configLock.unlock();
        }
    }

    /**
     * Retrieves all saved signs for the specified player.
     *
     * @param player The player.
     * @return A map of sign names to their corresponding SavedSignData objects.
     */
    public Map<String, SavedSignData> getAllSigns(Player player) {
        Map<String, SavedSignData> signs = new HashMap<>();
        UUID playerId = player.getUniqueId();
        
        // Use lock for thread-safe access to signLibraryConfig
        configLock.lock();
        try {
            ConfigurationSection playerSection = signLibraryConfig.getConfigurationSection("players." + playerId.toString());
            if (playerSection == null)
                return signs;
            for (String key : playerSection.getKeys(false)) {
                ConfigurationSection signSection = playerSection.getConfigurationSection(key);
                if (signSection != null) {
                    SavedSignData data = SavedSignData.loadFromConfigurationSection(signSection);
                    signs.put(key, data);
                }
            }
            return signs;
        } finally {
            configLock.unlock();
        }
    }

    /**
     * Deletes a saved sign for the specified player.
     *
     * @param player The player.
     * @param name   The identifier of the sign to delete.
     */
    public void deleteSign(Player player, String name) {
        UUID playerId = player.getUniqueId();
        
        // Use lock for thread-safe access to signLibraryConfig
        configLock.lock();
        boolean signExists = false;
        try {
            ConfigurationSection playerSection = signLibraryConfig.getConfigurationSection("players." + playerId.toString());
            if (playerSection == null)
                return;
            if (playerSection.contains(name)) {
                playerSection.set(name, null);
                signExists = true;
            }
        } finally {
            configLock.unlock();
        }
        
        if (signExists) {
            saveConfigAsync(success -> {
                if (success) {
                    player.sendMessage(Lang.SIGN_DELETED.getWithPrefix());
                } else {
                    player.sendMessage(Lang.PREFIX.get() + "§cFailed to delete sign. Please try again.");
                }
            });
        } else {
            player.sendMessage(Lang.SAVED_SIGN_NOT_FOUND.getWithPrefix());
        }
    }

    /**
     * Saves sign data directly to the player's library without requiring an ItemStack.
     * <p>
     * This method provides a direct way to save {@link SavedSignData} objects to a player's
     * sign library, bypassing the need for NBT extraction from ItemStacks. This is useful
     * for programmatic sign creation or when copying signs between players.
     * <p>
     * Unlike the ItemStack-based saveSign method, this method does not perform validation
     * checks such as sign limits or file name validation, as it's intended for internal use.
     * 
     * @param player The player whose library will store the sign
     * @param name The identifier name for the saved sign
     * @param savedData The complete sign data to save
     * @since 2.0.0
     * @see #saveSign(Player, String, ItemStack)
     * @see SavedSignData
     */
    public void saveSign(Player player, String name, SavedSignData savedData) {
        UUID playerId = player.getUniqueId();
        
        // Use lock for thread-safe access to signLibraryConfig
        configLock.lock();
        try {
            ConfigurationSection playersSection = signLibraryConfig.getConfigurationSection("players");
            ConfigurationSection playerSection = playersSection.getConfigurationSection(playerId.toString());
            if (playerSection == null) {
                playerSection = playersSection.createSection(playerId.toString());
            }
            ConfigurationSection signSection = playerSection.createSection(name);
            savedData.saveToConfigurationSection(signSection);
        } finally {
            configLock.unlock();
        }
        saveConfigAsync(null); // No callback needed for internal API
    }
    

    
    /**
     * Validates that a sign type string is valid.
     * 
     * @param signType The sign type to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidSignType(String signType) {
        if (signType == null || signType.trim().isEmpty()) {
            return false;
        }
        
        return signType.equalsIgnoreCase("regular") || signType.equalsIgnoreCase("hanging");
    }
    
    /**
     * Validates lore content for security.
     * 
     * @param lore The lore list to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidLore(java.util.List<String> lore) {
        if (lore == null) {
            return true;
        }
        
        // Limit lore size
        if (lore.size() > 20) {  // Maximum 20 lore lines
            return false;
        }
        
        for (String line : lore) {
            if (line != null) {
                // Check line length
                if (line.length() > 256) {  // Maximum 256 characters per line
                    return false;
                }
                
                // Check for suspicious content
                if (line.contains("§k") && line.length() > 100) {  // Obfuscated text abuse
                    return false;
                }
            }
        }
        
        return true;
    }
}
