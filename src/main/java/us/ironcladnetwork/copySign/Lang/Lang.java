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
    SIGN_TYPE_NOT_ALLOWED_LOAD("messages.SIGN_TYPE_NOT_ALLOWED_LOAD"),
    
    // Template messages
    TEMPLATE_CREATED("messages.TEMPLATE_CREATED"),
    TEMPLATE_DELETED("messages.TEMPLATE_DELETED"),
    TEMPLATE_LOADED("messages.TEMPLATE_LOADED"),
    TEMPLATE_NOT_FOUND("messages.TEMPLATE_NOT_FOUND"),
    TEMPLATE_ALREADY_EXISTS("messages.TEMPLATE_ALREADY_EXISTS"),
    INVALID_TEMPLATE_NAME("messages.INVALID_TEMPLATE_NAME"),
    MUST_HOLD_SIGN_WITH_DATA("messages.MUST_HOLD_SIGN_WITH_DATA"),
    TEMPLATE_HELP_HEADER("messages.TEMPLATE_HELP_HEADER"),
    TEMPLATE_HELP_LIST("messages.TEMPLATE_HELP_LIST"),
    TEMPLATE_HELP_CREATE("messages.TEMPLATE_HELP_CREATE"),
    TEMPLATE_HELP_DELETE("messages.TEMPLATE_HELP_DELETE"),
    TEMPLATE_HELP_USE("messages.TEMPLATE_HELP_USE"),
    TEMPLATE_HELP_EXAMPLES("messages.TEMPLATE_HELP_EXAMPLES"),
    TEMPLATE_HELP_EXAMPLE_CREATE("messages.TEMPLATE_HELP_EXAMPLE_CREATE"),
    TEMPLATE_HELP_EXAMPLE_USE("messages.TEMPLATE_HELP_EXAMPLE_USE"),
    TEMPLATE_NO_PERMISSION_VIEW("messages.TEMPLATE_NO_PERMISSION_VIEW"),
    TEMPLATE_NO_PERMISSION_CREATE("messages.TEMPLATE_NO_PERMISSION_CREATE"),
    TEMPLATE_NO_PERMISSION_DELETE("messages.TEMPLATE_NO_PERMISSION_DELETE"),
    TEMPLATE_NO_PERMISSION_USE("messages.TEMPLATE_NO_PERMISSION_USE"),
    TEMPLATE_LIST_EMPTY("messages.TEMPLATE_LIST_EMPTY"),
    TEMPLATE_USAGE_CREATE("messages.TEMPLATE_USAGE_CREATE"),
    TEMPLATE_USAGE_DELETE("messages.TEMPLATE_USAGE_DELETE"),
    TEMPLATE_USAGE_USE("messages.TEMPLATE_USAGE_USE"),
    TEMPLATE_TYPE_MISMATCH("messages.TEMPLATE_TYPE_MISMATCH"),
    TEMPLATE_MUST_HOLD_SIGN("messages.TEMPLATE_MUST_HOLD_SIGN"),
    TEMPLATE_CREATE_FAILED("messages.TEMPLATE_CREATE_FAILED"),
    TEMPLATE_DELETE_FAILED("messages.TEMPLATE_DELETE_FAILED"),
    
    // Command state messages
    COMMAND_FEATURE_DISABLED("messages.COMMAND_FEATURE_DISABLED"),
    
    // Validation messages
    INVALID_SIGN_NAME_FORMAT("messages.INVALID_SIGN_NAME_FORMAT"),
    INVALID_SIGN_ITEM_ERROR("messages.INVALID_SIGN_ITEM_ERROR"),
    SIGN_NO_REQUIRED_DATA("messages.SIGN_NO_REQUIRED_DATA"),
    
    // Permission messages
    NO_PERMISSION_TEMPLATES("messages.NO_PERMISSION_TEMPLATES"),
    
    // Additional template messages
    TEMPLATE_SAVE_SUCCESS("messages.TEMPLATE_SAVE_SUCCESS"),
    TEMPLATE_DELETE_SUCCESS("messages.TEMPLATE_DELETE_SUCCESS"),
    TEMPLATE_NOT_FOUND_ERROR("messages.TEMPLATE_NOT_FOUND_ERROR"),
    TEMPLATE_LOADED_TO_SIGN("messages.TEMPLATE_LOADED_TO_SIGN"),
    TEMPLATE_CREATE_SUCCESS("messages.TEMPLATE_CREATE_SUCCESS"),
    TEMPLATE_CREATE_CANCELLED("messages.TEMPLATE_CREATE_CANCELLED"),
    TEMPLATE_NAME_INVALID("messages.TEMPLATE_NAME_INVALID"),
    TEMPLATE_NAME_EXISTS("messages.TEMPLATE_NAME_EXISTS"),
    TEMPLATE_CREATION_PROMPT("messages.TEMPLATE_CREATION_PROMPT"),
    TEMPLATE_CREATION_CANCEL_HINT("messages.TEMPLATE_CREATION_CANCEL_HINT"),
    TEMPLATE_NO_DATA_ERROR("messages.TEMPLATE_NO_DATA_ERROR"),
    TEMPLATE_MUST_HOLD_SIGN_DATA("messages.TEMPLATE_MUST_HOLD_SIGN_DATA"),
    TEMPLATE_NAME_NOT_IDENTIFIED("messages.TEMPLATE_NAME_NOT_IDENTIFIED"),
    
    // Sign library messages
    SIGN_RENAME_PROMPT("messages.SIGN_RENAME_PROMPT"),
    SIGN_RENAME_CANCEL_HINT("messages.SIGN_RENAME_CANCEL_HINT"),
    SIGN_RENAME_CANCELLED("messages.SIGN_RENAME_CANCELLED"),
    SIGN_RENAME_SUCCESS("messages.SIGN_RENAME_SUCCESS"),
    SIGN_NAME_NOT_IDENTIFIED("messages.SIGN_NAME_NOT_IDENTIFIED"),
    SIGN_NOT_FOUND("messages.SIGN_NOT_FOUND"),
    SIGN_LOADED_TO_HELD("messages.SIGN_LOADED_TO_HELD"),
    
    // Cooldown messages
    COOLDOWN_MESSAGE("messages.COOLDOWN_MESSAGE"),
    COOLDOWN_SIGN_COPY("messages.COOLDOWN_SIGN_COPY"),
    
    // Performance messages
    PERFORMANCE_ERROR_RETRY("messages.PERFORMANCE_ERROR_RETRY"),
    
    // Data validation messages
    SIGN_DATA_SIZE_EXCEEDED("messages.SIGN_DATA_SIZE_EXCEEDED"),
    SIGN_DATA_TEXT_TOO_LARGE("messages.SIGN_DATA_TEXT_TOO_LARGE"),
    
    // Confirmation messages
    NO_PENDING_CONFIRMATIONS("messages.NO_PENDING_CONFIRMATIONS"),
    ACTION_CANCELLED("messages.ACTION_CANCELLED"),
    
    // Template confirmation messages
    TEMPLATE_DELETE_CONFIRMATION("messages.TEMPLATE_DELETE_CONFIRMATION"),
    TEMPLATE_DELETE_CONFIRMATION_COMMAND("messages.TEMPLATE_DELETE_CONFIRMATION_COMMAND"),
    TEMPLATE_DELETE_CONFIRMATION_EXPIRE("messages.TEMPLATE_DELETE_CONFIRMATION_EXPIRE"),
    
    // Permission display messages
    PERMISSIONS_HEADER("messages.PERMISSIONS_HEADER"),
    
    // Sign type permission messages
    NO_PERMISSION_COPY_SIGN_TYPE("messages.NO_PERMISSION_COPY_SIGN_TYPE"),
    NO_PERMISSION_PASTE_SIGN_TYPE("messages.NO_PERMISSION_PASTE_SIGN_TYPE"),
    
    // Main Command Help
    COMMAND_HELP_HEADER("messages.COMMAND_HELP_HEADER"),
    COMMAND_HELP_ON("messages.COMMAND_HELP_ON"),
    COMMAND_HELP_OFF("messages.COMMAND_HELP_OFF"),
    COMMAND_HELP_CLEAR("messages.COMMAND_HELP_CLEAR"),
    COMMAND_HELP_SAVE("messages.COMMAND_HELP_SAVE"),
    COMMAND_HELP_LOAD("messages.COMMAND_HELP_LOAD"),
    COMMAND_HELP_DELETE("messages.COMMAND_HELP_DELETE"),
    COMMAND_HELP_LIBRARY("messages.COMMAND_HELP_LIBRARY"),
    COMMAND_HELP_RELOAD("messages.COMMAND_HELP_RELOAD"),
    COMMAND_HELP_VALIDATE("messages.COMMAND_HELP_VALIDATE"),
    COMMAND_HELP_TEMPLATES("messages.COMMAND_HELP_TEMPLATES"),
    COMMAND_HELP_CONFIRM("messages.COMMAND_HELP_CONFIRM"),
    COMMAND_HELP_CANCEL("messages.COMMAND_HELP_CANCEL"),
    
    // Protection messages
    WORLDGUARD_COPY_DENIED("messages.WORLDGUARD_COPY_DENIED"),
    WORLDGUARD_PASTE_DENIED("messages.WORLDGUARD_PASTE_DENIED");

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
