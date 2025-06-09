package us.ironcladnetwork.copySign.Lang;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.ironcladnetwork.copySign.CopySign;

import java.io.File;

/**
 * Enum containing all plugin messages with methods to load and format them.
 */
public enum Lang {
    PREFIX("messages.PREFIX"),
    SIGN_COPIED("messages.SIGN_COPIED"),
    NO_PERMISSION("messages.NO_PERMISSION"),
    INVALID_SIGN("messages.INVALID_SIGN"),
    MUST_HOLD_SIGN("messages.MUST_HOLD_SIGN"),
    NO_PERMISSION_USE("messages.NO_PERMISSION_USE"),
    NO_PERMISSION_LIBRARY("messages.NO_PERMISSION_LIBRARY"),
    NO_PERMISSION_RELOAD("messages.NO_PERMISSION_RELOAD"),
    COMMAND_PLAYER_ONLY("messages.COMMAND_PLAYER_ONLY"),
    COPYSIGN_USAGE("messages.COPYSIGN_USAGE"),
    COPYSIGN_ENABLED("messages.COPYSIGN_ENABLED"),
    COPYSIGN_DISABLED("messages.COPYSIGN_DISABLED"),
    PLUGIN_RELOADED("messages.PLUGIN_RELOADED"),
    CLEAR_NO_ITEM("messages.CLEAR_NO_ITEM"),
    CLEAR_SUCCESS("messages.CLEAR_SUCCESS"),
    SIGN_SAVED_SUCCESSFULLY("messages.SIGN_SAVED_SUCCESSFULLY"),
    SIGN_DELETED("messages.SIGN_DELETED"),
    SIGN_LOADED("messages.SIGN_LOADED"),
    SAVED_SIGN_NOT_FOUND("messages.SAVED_SIGN_NOT_FOUND"),
    SIGN_TYPE_MISMATCH("messages.SIGN_TYPE_MISMATCH"),
    SIGN_ALREADY_EXISTS("messages.SIGN_ALREADY_EXISTS"),
    SIGN_NO_DATA("messages.SIGN_NO_DATA"),
    SIGN_LIBRARY_EMPTY("messages.SIGN_LIBRARY_EMPTY"),
    MAX_SIGNS_REACHED("messages.MAX_SIGNS_REACHED"),
    HANGING_SIGN("messages.HANGING_SIGN"),
    REGULAR_SIGN("messages.REGULAR_SIGN"),
    SIGN_TYPE_NOT_ALLOWED_COPY("messages.SIGN_TYPE_NOT_ALLOWED_COPY"),
    SIGN_TYPE_NOT_ALLOWED_PASTE("messages.SIGN_TYPE_NOT_ALLOWED_PASTE"),
    SIGN_TYPE_NOT_ALLOWED_SAVE("messages.SIGN_TYPE_NOT_ALLOWED_SAVE"),
    SIGN_TYPE_NOT_ALLOWED_LOAD("messages.SIGN_TYPE_NOT_ALLOWED_LOAD");

    private final String path;
    private String message;
    private static FileConfiguration config;

    Lang(String path) {
        this.path = path;
    }

    /**
     * Initializes the language system with the messages file.
     *
     * @param plugin The CopySign plugin instance.
     */
    public static void init(CopySign plugin) {
        // Load messages.yml from the plugin folder.
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            // Save resource if it doesn't exist.
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Reload all messages from file.
        for (Lang value : values())
            value.reload();
    }

    /**
     * Reloads the message from the configuration.
     */
    public void reload() {
        message = ChatColor.translateAlternateColorCodes('&', config.getString(path, path));
    }

    /**
     * Gets the formatted message.
     *
     * @return The formatted message with colors.
     */
    public String get() {
        return message;
    }

    /**
     * Gets the formatted message with placeholders replaced.
     *
     * @param args The placeholder replacements in pairs (placeholder, value).
     * @return The formatted message with placeholders replaced.
     */
    public String format(Object... args) {
        if (args.length % 2 != 0)
            throw new IllegalArgumentException("Args must be in pairs of placeholder and value!");

        String formatted = message;
        // Iterate through each placeholder pair
        for (int i = 0; i < args.length; i += 2) {
            formatted = formatted.replace(args[i].toString(), args[i + 1].toString());
        }
        return formatted;
    }

    /**
     * Gets the formatted message with the prefix.
     *
     * @return The prefixed and formatted message.
     */
    public String getWithPrefix() {
        return PREFIX.get() + message;
    }

    /**
     * Gets the formatted message with the prefix and placeholders replaced.
     *
     * @param args The placeholder replacements in pairs (placeholder, value).
     * @return The prefixed and formatted message with placeholders replaced.
     */
    public String formatWithPrefix(Object... args) {
        return PREFIX.get() + format(args);
    }
}
