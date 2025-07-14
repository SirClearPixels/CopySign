package us.ironcladnetwork.copySign.Util;

import de.tr7zw.nbtapi.NBTItem;
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
        
        NBTItem nbtItem = new NBTItem(signItem);
        // Ensure required NBT tags are present.
        if (!nbtItem.hasTag("copiedSignFront") || !nbtItem.hasTag("copiedSignBack")) {
            player.sendMessage(Lang.SIGN_NO_REQUIRED_DATA.getWithPrefix());
            return false;
        }
        
        // Extract sign information from NBT.
        String copiedSignFront = nbtItem.getString("copiedSignFront");
        String copiedSignBack = nbtItem.getString("copiedSignBack");
        String copiedFrontColor = nbtItem.hasTag("copiedSignFrontColor") ? nbtItem.getString("copiedSignFrontColor") : "BLACK";
        String copiedBackColor = nbtItem.hasTag("copiedSignBackColor") ? nbtItem.getString("copiedSignBackColor") : "BLACK";
        boolean signGlowing = nbtItem.hasTag("signGlowing") && nbtItem.getBoolean("signGlowing");
        String signType = nbtItem.hasTag("signType") ? nbtItem.getString("signType") : "regular";

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

        // Create a SavedSignData instance using the extracted data.
        SavedSignData savedData = new SavedSignData(frontLines, backLines, signGlowing, copiedFrontColor, copiedBackColor, signType, lore);

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
     * Reloads the templates from file.
     */
    public void reload() {
        templateConfig = YamlConfiguration.loadConfiguration(templateFile);
        if (!templateConfig.contains("templates")) {
            templateConfig.createSection("templates");
        }
    }
} 