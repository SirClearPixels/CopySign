package us.ironcladnetwork.copySign.Util;

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

import us.ironcladnetwork.copySign.CopySign;
import us.ironcladnetwork.copySign.Lang.Lang;

/**
 * Manager for handling server-wide sign templates.
 * 
 * This class handles:
 * • Loading and saving the YAML configuration from serverTemplates.yml.
 * • Saving, retrieving, listing, and deleting server-wide sign templates.
 * • Only admins with proper permissions can modify templates.
 */
public class ServerTemplateManager {

    private final File templateFile;
    private YamlConfiguration templateConfig;
    private final CopySign plugin;

    /**
     * Initializes the manager by loading the serverTemplates.yml file.
     * If the file doesn't exist, it will be created with a default "templates" section.
     *
     * @param dataFolder The plugin's data folder.
     * @param plugin The plugin instance for accessing config.
     */
    public ServerTemplateManager(File dataFolder, CopySign plugin) {
        this.plugin = plugin;
        templateFile = new File(dataFolder, "serverTemplates.yml");
        if (!templateFile.exists()) {
            try {
                // Ensure the parent directories exist.
                templateFile.getParentFile().mkdirs();
                templateFile.createNewFile();
                templateConfig = new YamlConfiguration();
                templateConfig.createSection("templates");
                // Add some example templates
                createDefaultTemplates();
                saveConfig();
                ErrorHandler.debug("Created new serverTemplates.yml file with default templates");
            } catch (IOException e) {
                ErrorHandler.handleFileError("creating serverTemplates.yml", templateFile, e, null);
                // Create a minimal in-memory config as fallback
                templateConfig = new YamlConfiguration();
                templateConfig.createSection("templates");
            }
        } else {
            templateConfig = YamlConfiguration.loadConfiguration(templateFile);
            // Ensure the top-level "templates" section exists.
            if (!templateConfig.contains("templates"))
                templateConfig.createSection("templates");
        }
    }

    /**
     * Creates some default example templates for admins.
     */
    private void createDefaultTemplates() {
        // Example: Rules template
        SavedSignData rulesTemplate = new SavedSignData(
            new String[]{"§c§lSERVER RULES", "§71. Be respectful", "§72. No griefing", "§73. Have fun!"},
            new String[]{"§bVisit our website:", "§eexample.com", "§afor more info", ""},
            false, "RED", "BLUE", "regular", null
        );
        ConfigurationSection rulesSection = templateConfig.createSection("templates.rules");
        rulesTemplate.saveToConfigurationSection(rulesSection);
        
        // Example: Welcome template
        SavedSignData welcomeTemplate = new SavedSignData(
            new String[]{"§a§lWELCOME", "§eto our server!", "", "§6Enjoy your stay!"},
            new String[]{"§dNeed help?", "§bType /help", "§bor ask staff", ""},
            true, "GREEN", "LIGHT_BLUE", "regular", null
        );
        ConfigurationSection welcomeSection = templateConfig.createSection("templates.welcome");
        welcomeTemplate.saveToConfigurationSection(welcomeSection);
    }

    /**
     * Persists changes to the serverTemplates.yml file.
     */
    private void saveConfig() {
        try {
            // Create backup before saving
            ErrorHandler.createBackup(templateFile);
            
            // Validate config before saving
            if (templateConfig == null) {
                throw new IllegalStateException("Configuration is null, cannot save");
            }
            
            templateConfig.save(templateFile);
            ErrorHandler.debug("Successfully saved serverTemplates.yml");
            
        } catch (IOException e) {
            ErrorHandler.handleFileError("saving serverTemplates.yml", templateFile, e, null);
        } catch (Exception e) {
            ErrorHandler.handleGeneralError("saving server template configuration", e, null);
        }
    }

    /**
     * Saves a server template.
     * <p>
     * Validates that the provided sign item contains the required NBT tags.
     * Only players with copysign.admin permission can save server templates.
     * 
     * @param player   The player saving the template.
     * @param name     The identifier name for the template.
     * @param signItem The sign item holding the stored NBT data.
     * @return true if saved successfully, false otherwise
     */
    public boolean saveTemplate(Player player, String name, ItemStack signItem) {
        // Check admin permission
        if (!player.hasPermission("copysign.admin")) {
            player.sendMessage(Lang.NO_PERMISSION_TEMPLATES.getWithPrefix());
            return false;
        }
        
        // Validate that the signItem is not null and is of a sign type.
        if (signItem == null || signItem.getType() == Material.AIR || !signItem.getType().name().endsWith("_SIGN")) {
            player.sendMessage(Lang.INVALID_SIGN_ITEM_ERROR.getWithPrefix());
            return false;
        }
        
        // Read the copied-sign payload (PDC, with legacy NBT-API fallback).
        java.util.Optional<SignItemData> copiedOpt = SignItemStorage.read(signItem);
        if (copiedOpt.isEmpty()) {
            player.sendMessage(Lang.SIGN_NO_REQUIRED_DATA.getWithPrefix());
            return false;
        }
        SignItemData copied = copiedOpt.get();

        // Extract sign information from the copied payload.
        String copiedSignFront = copied.getFront();
        String copiedSignBack = copied.getBack();
        String copiedFrontColor = copied.hasFrontColor() ? copied.getFrontColor() : "BLACK";
        String copiedBackColor = copied.hasBackColor() ? copied.getBackColor() : "BLACK";
        boolean frontGlowing = copied.isFrontGlowing();
        boolean backGlowing = copied.isBackGlowing();
        String signType = copied.getSignType();

        // Get lore from the item meta if present.
        java.util.List<String> lore = null;
        if (signItem.hasItemMeta()) {
            ItemMeta itemMeta = signItem.getItemMeta();
            if (itemMeta != null && itemMeta.hasLore()) {
                lore = itemMeta.getLore();
            }
        }

        // Process the front/back text into arrays of lines.
        String[] frontLines = copiedSignFront.split("\n", -1);
        String[] backLines = copiedSignBack.split("\n", -1);

        // Create a SavedSignData instance using the extracted data (per-side glow preserved).
        SavedSignData savedData = new SavedSignData(frontLines, backLines, frontGlowing, backGlowing, copiedFrontColor, copiedBackColor, signType, lore);

        // Save the data under the template name.
        ConfigurationSection templatesSection = templateConfig.getConfigurationSection("templates");
        ConfigurationSection templateSection = templatesSection.createSection(name);
        savedData.saveToConfigurationSection(templateSection);

        // Persist the updated configuration.
        saveConfig();
        player.sendMessage(Lang.TEMPLATE_SAVE_SUCCESS.formatWithPrefix("%name%", name));
        return true;
    }

    /**
     * Retrieves a server template by name.
     *
     * @param name The template's identifier.
     * @return The SavedSignData object if found, otherwise null.
     */
    public SavedSignData getTemplate(String name) {
        ConfigurationSection templateSection = templateConfig.getConfigurationSection("templates." + name);
        if (templateSection == null)
            return null;
        return SavedSignData.loadFromConfigurationSection(templateSection);
    }

    /**
     * Retrieves all server templates.
     *
     * @return A map of template names to their corresponding SavedSignData objects.
     */
    public Map<String, SavedSignData> getAllTemplates() {
        Map<String, SavedSignData> templates = new HashMap<>();
        ConfigurationSection templatesSection = templateConfig.getConfigurationSection("templates");
        if (templatesSection == null)
            return templates;
        for (String key : templatesSection.getKeys(false)) {
            ConfigurationSection templateSection = templatesSection.getConfigurationSection(key);
            if (templateSection != null) {
                SavedSignData data = SavedSignData.loadFromConfigurationSection(templateSection);
                templates.put(key, data);
            }
        }
        return templates;
    }

    /**
     * Deletes a server template.
     * Only players with copysign.admin permission can delete server templates.
     *
     * @param player The player attempting to delete.
     * @param name   The identifier of the template to delete.
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteTemplate(Player player, String name) {
        // Check admin permission
        if (!player.hasPermission("copysign.admin")) {
            player.sendMessage(Lang.NO_PERMISSION_TEMPLATES.getWithPrefix());
            return false;
        }
        
        ConfigurationSection templatesSection = templateConfig.getConfigurationSection("templates");
        if (templatesSection == null || !templatesSection.contains(name)) {
            player.sendMessage(Lang.TEMPLATE_NOT_FOUND_ERROR.getWithPrefix());
            return false;
        }
        
        templatesSection.set(name, null);
        saveConfig();
        player.sendMessage(Lang.TEMPLATE_DELETE_SUCCESS.formatWithPrefix("%name%", name));
        return true;
    }

    /**
     * Outcome of a template re-key (rename) attempt.
     */
    public enum RenameStatus {
        /** The template was re-keyed: data written under the new name, old name freed. */
        OK,
        /** No template existed under the old name; nothing was written. */
        NOT_FOUND,
        /** A template already exists under the new name; nothing was written (never overwrite). */
        TARGET_EXISTS,
        /** The new name failed validation; nothing was written. */
        INVALID_NAME
    }

    /**
     * Re-keys a template entry within the templates section, preserving the stored
     * {@link SavedSignData} and freeing the old name (LIB-02, D-09).
     * <p>
     * This is a pure, static, side-effect-free helper: no lock, sound, metrics, Lang, or
     * scheduler interaction. It operates only on the supplied {@link ConfigurationSection},
     * which makes it unit-testable against a plain {@code YamlConfiguration} section without
     * constructing the manager (whose constructor needs live singletons). It never reads or
     * writes any version-marker key — Phase 4 versioned only the on-item PDC payload, not the
     * library/template YAML (D-09).
     *
     * @param templatesSection The {@code templates} section; may be null.
     * @param oldName          The existing template name to move.
     * @param newName          The target name to move the data to.
     * @return {@link RenameStatus#NOT_FOUND} if the old name is absent (or section is null);
     *         {@link RenameStatus#INVALID_NAME} if the new name fails validation (D-04);
     *         {@link RenameStatus#TARGET_EXISTS} if the new name is already taken (D-05);
     *         {@link RenameStatus#OK} on a successful re-key.
     */
    static RenameStatus rekey(ConfigurationSection templatesSection, String oldName, String newName) {
        if (templatesSection == null || !templatesSection.contains(oldName)) {
            return RenameStatus.NOT_FOUND;
        }
        // Manager-layer validation of the new name (D-04, LIB-04) — runs before any write.
        if (!ErrorHandler.isValidFileName(newName, 32)) {
            return RenameStatus.INVALID_NAME;
        }
        // Hard reject a collision; never overwrite an existing target (D-05).
        if (templatesSection.contains(newName)) {
            return RenameStatus.TARGET_EXISTS;
        }
        SavedSignData savedData =
                SavedSignData.loadFromConfigurationSection(templatesSection.getConfigurationSection(oldName));
        savedData.saveToConfigurationSection(templatesSection.createSection(newName));
        // Free the old name (D-09).
        templatesSection.set(oldName, null);
        return RenameStatus.OK;
    }

    /**
     * Renames a server template in place, preserving the stored data and freeing the old
     * name (LIB-02). Admin-gated. Mirrors this manager's own lockless, synchronous
     * persistence style (no re-entrant lock; synchronous {@link #saveConfig()}) — it
     * deliberately does NOT adopt {@code SignLibraryManager}'s locking/async pattern (D-10
     * caveat). Rename is instant with no confirmation prompt (D-02).
     *
     * @param player  The player attempting the rename (must hold {@code copysign.admin}).
     * @param oldName The existing template name.
     * @param newName The new template name.
     * @return true if the template was renamed, false on any rejection.
     */
    public boolean renameTemplate(Player player, String oldName, String newName) {
        // Admin gate first (LIB-02 — templates stay admin-only).
        if (!player.hasPermission(Permissions.ADMIN)) {
            player.sendMessage(Lang.NO_PERMISSION_TEMPLATES.getWithPrefix());
            return false;
        }

        ConfigurationSection templatesSection = templateConfig.getConfigurationSection("templates");
        RenameStatus status = rekey(templatesSection, oldName, newName);
        switch (status) {
            case OK:
                saveConfig();
                player.sendMessage(Lang.TEMPLATE_RENAMED.formatWithPrefix("%old%", oldName, "%new%", newName));
                return true;
            case NOT_FOUND:
                player.sendMessage(Lang.TEMPLATE_NOT_FOUND.formatWithPrefix("%name%", oldName));
                return false;
            case TARGET_EXISTS:
                player.sendMessage(Lang.TEMPLATE_RENAME_TARGET_EXISTS.getWithPrefix());
                return false;
            case INVALID_NAME:
            default:
                player.sendMessage(Lang.INVALID_SIGN_NAME_FORMAT.getWithPrefix());
                return false;
        }
    }

    /**
     * Reloads the templates from file.
     */
    public void reload() {
        templateConfig = YamlConfiguration.loadConfiguration(templateFile);
        if (!templateConfig.contains("templates")) {
            templateConfig.createSection("templates");
        }
    }
} 