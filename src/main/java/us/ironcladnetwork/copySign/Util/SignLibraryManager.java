package us.ironcladnetwork.copySign.Util;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import us.ironcladnetwork.copySign.Util.SignItemData;
import us.ironcladnetwork.copySign.Util.SignItemStorage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
     * Per-player active library search query (LIB-03, SC-4, D-07).
     * <p>
     * Keyed by player UUID; populated when {@code /copysign library <query>} runs so the same
     * filter can be re-applied on every page of the library GUI (page 2+ included). The map is
     * cleared on a bare {@code /copysign library} open and on player quit to bound memory, so it
     * holds at most one String per online player. {@link ConcurrentHashMap} mirrors
     * {@code ConfirmationManager} for Folia safety. Purely in-memory: never serialized to disk.
     */
    private final Map<UUID, String> activeSearchFilters = new ConcurrentHashMap<>();

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
    private CompletableFuture<Boolean> saveConfigAsync(Consumer<Boolean> callback) { return saveConfigAsync(null, callback); }
    private CompletableFuture<Boolean> saveConfigAsync(Player owner, Consumer<Boolean> callback) {
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
                // Execute callback on global region scheduler
                if (owner != null) SchedulerUtil.runAtEntity(plugin, owner, () -> callback.accept(result));
                else SchedulerUtil.runGlobal(plugin, () -> callback.accept(result));
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
                PlatformCompat.sendMessage(player, Lang.INVALID_SIGN_NAME_FORMAT.getWithPrefix());
                return;
            }
            
            // Validate that the signItem is not null and is of a sign type.
            if (signItem == null || signItem.getType() == Material.AIR || !signItem.getType().name().endsWith("_SIGN")) {
                PlatformCompat.sendMessage(player, Lang.INVALID_SIGN_ITEM_ERROR.getWithPrefix());
                return;
            }
            
            // Read the copied-sign payload (PDC, with legacy NBT-API fallback).
            java.util.Optional<SignItemData> copiedOpt = SignItemStorage.read(signItem);
            if (copiedOpt.isEmpty()) {
                PlatformCompat.sendMessage(player, Lang.SIGN_NO_REQUIRED_DATA.getWithPrefix());
                return;
            }
            SignItemData copied = copiedOpt.get();
            
                // Check max saved signs limit (permission-aware)
            int configDefault = plugin.getConfigInt("library.max-saved-signs", 50);
            int maxSigns = Permissions.getMaxLibrarySigns(player, configDefault);
            if (maxSigns != -1) { // -1 means unlimited
                Map<String, SavedSignData> existingSigns = getAllSigns(player);
                // If the sign doesn't already exist and we're at the limit
                if (!existingSigns.containsKey(name) && existingSigns.size() >= maxSigns) {
                    PlatformCompat.sendMessage(player, Lang.MAX_SIGNS_REACHED.formatWithPrefix("{max}", String.valueOf(maxSigns)));
                    return;
                }
            }
            
            // Extract sign information from the copied payload.
            String copiedSignFront = copied.getFront();
            String copiedSignBack = copied.getBack();
            
            // Validate NBT data for security
            if (!NBTValidationUtil.validateNBTData(copiedSignFront) || !NBTValidationUtil.validateNBTData(copiedSignBack)) {
                PlatformCompat.sendMessage(player, Lang.PREFIX.get().append(LegacyComponentSerializer.legacySection()
                        .deserialize("§cSign data is too large or invalid.")));
                return;
            }
            
            String copiedFrontColor = copied.hasFrontColor() ? copied.getFrontColor() : "OAK";
            String copiedBackColor = copied.hasBackColor() ? copied.getBackColor() : "OAK";
            
            // Validate color values
            if (!SignValidationUtil.isValidDyeColor(copiedFrontColor) || !SignValidationUtil.isValidDyeColor(copiedBackColor)) {
                PlatformCompat.sendMessage(player, Lang.PREFIX.get().append(LegacyComponentSerializer.legacySection()
                        .deserialize("§cInvalid sign color data.")));
                return;
            }
            
            boolean frontGlowing = copied.isFrontGlowing();
            boolean backGlowing = copied.isBackGlowing();
            String signType = copied.getSignType();
            
            // Validate sign type
            if (!isValidSignType(signType)) {
                PlatformCompat.sendMessage(player, Lang.PREFIX.get().append(LegacyComponentSerializer.legacySection()
                        .deserialize("§cInvalid sign type data.")));
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
                    PlatformCompat.sendMessage(player, Lang.PREFIX.get().append(LegacyComponentSerializer.legacySection()
                            .deserialize("§cSign lore contains invalid data.")));
                    return;
                }
            }

            // Process the front/back text into arrays of lines.
            String[] frontLines = copiedSignFront.split("\n", -1);
            String[] backLines = copiedSignBack.split("\n", -1);

            // Create a SavedSignData instance using the extracted data (per-side glow preserved).
            SavedSignData savedData = new SavedSignData(frontLines, backLines, frontGlowing, backGlowing, copiedFrontColor, copiedBackColor, signType, lore);

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
            saveConfigAsync(player, success -> {
                if (success) {
                    PlatformCompat.sendMessage(player, Lang.SIGN_SAVED_SUCCESSFULLY.getWithPrefix());
                    // Play save sound effect
                    CopySign.getInstance().getSoundManager().playSaveSound(player);
                    // Record metrics
                    CopySign.getInstance().getMetricsManager().recordSaveOperation(player);
                } else {
                    PlatformCompat.sendMessage(player, Lang.PREFIX.get().append(LegacyComponentSerializer.legacySection()
                            .deserialize("§cFailed to save sign. Please try again.")));
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
            saveConfigAsync(player, success -> {
                if (success) {
                    PlatformCompat.sendMessage(player, Lang.SIGN_DELETED.getWithPrefix());
                } else {
                    PlatformCompat.sendMessage(player, Lang.PREFIX.get().append(LegacyComponentSerializer.legacySection()
                            .deserialize("§cFailed to delete sign. Please try again.")));
                }
            });
        } else {
            PlatformCompat.sendMessage(player, Lang.SAVED_SIGN_NOT_FOUND.getWithPrefix());
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
     * Outcome of a personal-library re-key attempt.
     *
     * @see #rekey(ConfigurationSection, String, String)
     */
    public enum RenameStatus {
        /** The sign was re-keyed: data written under the new name, old name freed. */
        OK,
        /** No sign existed under the old name; nothing was written. */
        NOT_FOUND,
        /** A sign already exists under the new name; nothing was written (never overwrite). */
        TARGET_EXISTS,
        /** The new name failed validation; nothing was written. */
        INVALID_NAME
    }

    /**
     * Re-keys a saved sign entry within a single owner's section, preserving the stored
     * {@link SavedSignData} and freeing the old name (LIB-01, D-09).
     * <p>
     * This is a pure, static, side-effect-free helper: no lock, sound, metrics, Lang, or
     * scheduler interaction. It operates only on the supplied {@link ConfigurationSection},
     * which makes it unit-testable against a plain {@code YamlConfiguration} section without
     * constructing the manager. It never reads or writes any version-marker key —
     * Phase 4 versioned only the on-item PDC payload, not the library/template YAML (D-09).
     *
     * @param ownerSection The owner's section (e.g. {@code players.<uuid>}); may be null.
     * @param oldName      The existing sign name to move.
     * @param newName      The target name to move the data to.
     * @return {@link RenameStatus#NOT_FOUND} if the old name is absent (or owner is null);
     *         {@link RenameStatus#INVALID_NAME} if the new name fails validation (D-04);
     *         {@link RenameStatus#TARGET_EXISTS} if the new name is already taken (D-05);
     *         {@link RenameStatus#OK} on a successful re-key.
     */
    static RenameStatus rekey(ConfigurationSection ownerSection, String oldName, String newName) {
        if (ownerSection == null || !ownerSection.contains(oldName)) {
            return RenameStatus.NOT_FOUND;
        }
        // Manager-layer validation of the new name (D-04, LIB-04) — runs before any write.
        if (!ErrorHandler.isValidFileName(newName, 32)) {
            return RenameStatus.INVALID_NAME;
        }
        // Hard reject a collision; never overwrite an existing target (D-05).
        if (ownerSection.contains(newName)) {
            return RenameStatus.TARGET_EXISTS;
        }
        SavedSignData savedData =
                SavedSignData.loadFromConfigurationSection(ownerSection.getConfigurationSection(oldName));
        savedData.saveToConfigurationSection(ownerSection.createSection(newName));
        // Free the old name (D-09).
        ownerSection.set(oldName, null);
        return RenameStatus.OK;
    }

    /**
     * Renames a saved sign for the specified player, preserving the stored data and freeing
     * the old name (LIB-01). Re-keys under {@link #configLock} via the static
     * {@link #rekey(ConfigurationSection, String, String)} helper, then persists
     * asynchronously and reports the outcome to the player.
     *
     * @param player  The player whose library is being modified.
     * @param oldName The existing sign name.
     * @param newName The new sign name.
     */
    public void renameSign(Player player, String oldName, String newName) {
        UUID playerId = player.getUniqueId();

        RenameStatus status;
        configLock.lock();
        try {
            ConfigurationSection playerSection =
                    signLibraryConfig.getConfigurationSection("players." + playerId.toString());
            status = rekey(playerSection, oldName, newName);
        } finally {
            configLock.unlock();
        }

        switch (status) {
            case OK:
                saveConfigAsync(player, success -> {
                    if (success) {
                        PlatformCompat.sendMessage(player, Lang.SIGN_RENAMED.formatWithPrefix("%old%", oldName, "%new%", newName));
                    } else {
                        PlatformCompat.sendMessage(player, Lang.PREFIX.get().append(LegacyComponentSerializer.legacySection()
                                .deserialize("§cFailed to rename sign. Please try again.")));
                    }
                });
                break;
            case NOT_FOUND:
                PlatformCompat.sendMessage(player, Lang.SAVED_SIGN_NOT_FOUND.getWithPrefix());
                break;
            case TARGET_EXISTS:
                PlatformCompat.sendMessage(player, Lang.SIGN_RENAME_TARGET_EXISTS.getWithPrefix());
                break;
            case INVALID_NAME:
                PlatformCompat.sendMessage(player, Lang.INVALID_SIGN_NAME_FORMAT.getWithPrefix());
                break;
        }
    }

    /**
     * Case-insensitive substring filter over a set of saved-sign names (LIB-03, D-07).
     * <p>
     * Pure in-memory helper with no Bukkit dependency, so it is unit-testable without a
     * server. A null or blank query returns all names.
     *
     * @param names The candidate names to filter.
     * @param query The case-insensitive substring to match anywhere in each name.
     * @return The subset of names whose lowercase form contains the lowercase query;
     *         all names if the query is null or blank.
     */
    public static List<String> filterByName(Collection<String> names, String query) {
        List<String> result = new ArrayList<>();
        if (names == null) {
            return result;
        }
        if (query == null || query.trim().isEmpty()) {
            result.addAll(names);
            return result;
        }
        String needle = query.toLowerCase(Locale.ENGLISH);
        for (String name : names) {
            if (name != null && name.toLowerCase(Locale.ENGLISH).contains(needle)) {
                result.add(name);
            }
        }
        return result;
    }

    /**
     * Stores the active library search query for a player (LIB-03, SC-4, D-07).
     * <p>
     * The query is stored verbatim — {@link #filterEntries(Map, String)} (via
     * {@link #filterByName(Collection, String)}) lowercases at match time, so no normalization
     * is applied here. The map is cleared on a bare library open and on player quit to bound
     * memory; a new search simply overwrites the prior entry.
     *
     * @param playerId The player's UUID.
     * @param query    The search query to persist.
     */
    public void setActiveFilter(UUID playerId, String query) {
        activeSearchFilters.put(playerId, query);
    }

    /**
     * Returns the active library search query for a player, or {@code null} if none is set
     * (LIB-03, SC-4). A null result means "no active filter" — {@link #filterEntries(Map, String)}
     * treats it as "return all entries".
     *
     * @param playerId The player's UUID.
     * @return The stored query, or {@code null} if no filter is active.
     */
    public String getActiveFilter(UUID playerId) {
        return activeSearchFilters.get(playerId);
    }

    /**
     * Clears the active library search query for a player (LIB-03, SC-4). Called on a bare
     * {@code /copysign library} open and on player quit to keep the UUID-keyed map bounded.
     *
     * @param playerId The player's UUID.
     */
    public void clearActiveFilter(UUID playerId) {
        activeSearchFilters.remove(playerId);
    }

    /**
     * Pure entry-assembly seam used by BOTH the command's first page and the listener's page 2+
     * navigation, so filtered rendering is identical across pages (LIB-03, SC-4).
     * <p>
     * Returns the subset of {@code entries} whose KEY matches {@code query} via the same
     * case-insensitive substring rule as {@link #filterByName(Collection, String)}. A null or
     * blank query returns ALL entries (matching filterByName's contract); a null {@code entries}
     * returns an empty list. The result is built by iterating {@code entries.entrySet()} in
     * encounter order and keeping matching keys, so the order is preserved — pagination
     * consistency depends on both callers yielding the SAME entry order over the same keys.
     *
     * @param entries The player's saved-sign entries (name → data); may be null.
     * @param query   The case-insensitive substring to match against entry keys.
     * @return The matching entries in encounter order; all entries for a null/blank query;
     *         an empty list for a null {@code entries}.
     */
    public static List<Map.Entry<String, SavedSignData>> filterEntries(
            Map<String, SavedSignData> entries, String query) {
        List<Map.Entry<String, SavedSignData>> result = new ArrayList<>();
        if (entries == null) {
            return result;
        }
        // Reuse filterByName for the matching rule, then build a Set for O(1) membership checks
        // while preserving encounter order via the entrySet iteration below.
        Set<String> matching = new HashSet<>(filterByName(entries.keySet(), query));
        for (Map.Entry<String, SavedSignData> entry : entries.entrySet()) {
            if (matching.contains(entry.getKey())) {
                result.add(entry);
            }
        }
        return result;
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
