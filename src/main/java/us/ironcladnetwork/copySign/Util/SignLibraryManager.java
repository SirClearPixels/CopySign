package us.ironcladnetwork.copySign.Util;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        try {
            // Create backup before saving
            ErrorHandler.createBackup(signLibraryFile);
            
            // Validate config before saving
            if (signLibraryConfig == null) {
                throw new IllegalStateException("Configuration is null, cannot save");
            }
            
            signLibraryConfig.save(signLibraryFile);
            ErrorHandler.debug("Successfully saved savedSigns.yml");
            
        } catch (IOException e) {
            ErrorHandler.handleFileError("saving savedSigns.yml", signLibraryFile, e, null);
        } catch (Exception e) {
            ErrorHandler.handleGeneralError("saving sign library configuration", e, null);
        }
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
                player.sendMessage(Lang.PREFIX.get() + "&cInvalid sign name. Use only letters, numbers, hyphens, and underscores (max 32 characters).");
                return;
            }
            
            // Validate that the signItem is not null and is of a sign type.
            if (signItem == null || !signItem.getType().name().endsWith("_SIGN")) {
                player.sendMessage(Lang.PREFIX.get() + "&cInvalid sign item.");
                return;
            }
            
            NBTItem nbtItem = new NBTItem(signItem);
            // Ensure required NBT tags are present.
            if (!nbtItem.hasTag("copiedSignFront") || !nbtItem.hasTag("copiedSignBack")) {
                player.sendMessage(Lang.PREFIX.get() + "&cSign does not contain the required data.");
                return;
            }
            
                // Check max saved signs limit
            int maxSigns = plugin.getConfig().getInt("library.max-saved-signs", 50);
            if (maxSigns != -1) { // -1 means unlimited
                Map<String, SavedSignData> existingSigns = getAllSigns(player);
                // If the sign doesn't already exist and we're at the limit
                if (!existingSigns.containsKey(name) && existingSigns.size() >= maxSigns) {
                    player.sendMessage(Lang.MAX_SIGNS_REACHED.getWithPrefix().replace("{max}", String.valueOf(maxSigns)));
                    return;
                }
            }
            
            // Extract sign information from NBT.
            String copiedSignFront = nbtItem.getString("copiedSignFront");
            String copiedSignBack = nbtItem.getString("copiedSignBack");
            String copiedFrontColor = nbtItem.hasTag("copiedSignFrontColor") ? nbtItem.getString("copiedSignFrontColor") : "OAK";
            String copiedBackColor = nbtItem.hasTag("copiedSignBackColor") ? nbtItem.getString("copiedSignBackColor") : "OAK";
            boolean signGlowing = nbtItem.hasTag("signGlowing") && nbtItem.getBoolean("signGlowing");
            String signType = nbtItem.hasTag("signType") ? nbtItem.getString("signType") : "regular";

            // Get lore from the item meta if present.
            java.util.List<String> lore = null;
            if (signItem.hasItemMeta() && signItem.getItemMeta().hasLore()) {
                lore = signItem.getItemMeta().getLore();
            }

            // Process the front/back text into arrays of lines.
            String[] frontLines = copiedSignFront.split("\n", -1);
            String[] backLines = copiedSignBack.split("\n", -1);

            // Create a SavedSignData instance using the extracted data.
            SavedSignData savedData = new SavedSignData(frontLines, backLines, signGlowing, copiedFrontColor, copiedBackColor, signType, lore);

            // Save the data under the player's UUID and the provided sign name.
            UUID playerId = player.getUniqueId();
            ConfigurationSection playersSection = signLibraryConfig.getConfigurationSection("players");
            ConfigurationSection playerSection = playersSection.getConfigurationSection(playerId.toString());
            if (playerSection == null) {
                playerSection = playersSection.createSection(playerId.toString());
            }
            // Create or override a section for this sign.
            ConfigurationSection signSection = playerSection.createSection(name);
            savedData.saveToConfigurationSection(signSection);

            // Persist the updated configuration.
            saveConfig();
            player.sendMessage(Lang.SIGN_SAVED_SUCCESSFULLY.getWithPrefix());
            
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
        ConfigurationSection playerSection = signLibraryConfig.getConfigurationSection("players." + playerId.toString());
        if (playerSection == null)
            return null;
        ConfigurationSection signSection = playerSection.getConfigurationSection(name);
        if (signSection == null)
            return null;
        return SavedSignData.loadFromConfigurationSection(signSection);
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
    }

    /**
     * Deletes a saved sign for the specified player.
     *
     * @param player The player.
     * @param name   The identifier of the sign to delete.
     */
    public void deleteSign(Player player, String name) {
        UUID playerId = player.getUniqueId();
        ConfigurationSection playerSection = signLibraryConfig.getConfigurationSection("players." + playerId.toString());
        if (playerSection == null)
            return;
        if (playerSection.contains(name)) {
            playerSection.set(name, null);
            saveConfig();
            player.sendMessage(Lang.SIGN_DELETED.getWithPrefix());
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
        ConfigurationSection playersSection = signLibraryConfig.getConfigurationSection("players");
        ConfigurationSection playerSection = playersSection.getConfigurationSection(playerId.toString());
        if (playerSection == null) {
            playerSection = playersSection.createSection(playerId.toString());
        }
        ConfigurationSection signSection = playerSection.createSection(name);
        savedData.saveToConfigurationSection(signSection);
        saveConfig();
    }
}
