package us.ironcladnetwork.copySign.Lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.ironcladnetwork.copySign.CopySign;

import java.io.File;
import java.util.Locale;

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
    TEMPLATE_USAGE_RENAME("messages.TEMPLATE_USAGE_RENAME"),
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

    // Rename + search messages (Phase 5: Library & Template Management)
    SIGN_RENAMED("messages.SIGN_RENAMED"),
    SIGN_RENAME_TARGET_EXISTS("messages.SIGN_RENAME_TARGET_EXISTS"),
    LIBRARY_SEARCH_NO_MATCH("messages.LIBRARY_SEARCH_NO_MATCH"),
    TEMPLATE_RENAMED("messages.TEMPLATE_RENAMED"),
    TEMPLATE_RENAME_TARGET_EXISTS("messages.TEMPLATE_RENAME_TARGET_EXISTS"),

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
    COMMAND_HELP_TEMPLATES("messages.COMMAND_HELP_TEMPLATES"),
    COMMAND_HELP_CONFIRM("messages.COMMAND_HELP_CONFIRM"),
    COMMAND_HELP_CANCEL("messages.COMMAND_HELP_CANCEL"),
    
    // Protection messages
    WORLDGUARD_COPY_DENIED("messages.WORLDGUARD_COPY_DENIED"),
    WORLDGUARD_PASTE_DENIED("messages.WORLDGUARD_PASTE_DENIED");

    /** Shared MiniMessage instance — created once and reused (RESEARCH anti-pattern: never per-call). */
    private static final MiniMessage MM = MiniMessage.miniMessage();
    /** Legacy serializer for {@code &}-coded strings (handles hex {@code &#rrggbb} + all format codes). */
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    /** Legacy serializer for strings already containing {@code §} section codes. */
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private final String path;
    /** Raw (unparsed) string value loaded from messages.yml. Parsed to a Component at render time. */
    private String message;
    /** Lazily-cached Component for placeholder-free keys; null until first {@link #get()}, cleared by {@link #reload()}. */
    private Component cached;
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
     * <p>
     * Stores the RAW (unparsed) config string so that keys containing {@code %x%}
     * placeholders can be parsed at {@link #format(Object...)} time with their
     * {@link TagResolver}s applied (Open Question 2). Clears any cached Component.
     */
    public void reload() {
        message = config.getString(path, path);
        cached = null;
    }

    // ---- Rendering core (D-01): per-string legacy-vs-MiniMessage auto-detection. ----

    /**
     * Auto-detect predicate (D-01). A string is treated as legacy when it contains a
     * {@code §} section character OR a {@code &} immediately followed by a color/format/hex
     * code; otherwise it is treated as MiniMessage.
     *
     * @param s the raw string to classify
     * @return {@code true} if the string should render via a legacy serializer
     */
    static boolean isLegacy(String s) {
        if (s == null) return false;
        return s.indexOf('§') >= 0                          // any § (section)
                || s.matches("(?s).*&[0-9A-Fa-fK-Ok-oRrXx#].*");  // & followed by a color/format/hex code
    }

    /**
     * Renders a raw string to a {@link Component} via the auto-detect path (no placeholders).
     * {@code §}-containing strings route to {@link LegacyComponentSerializer#legacySection()},
     * {@code &}-only strings to {@link LegacyComponentSerializer#legacyAmpersand()}, and
     * everything else to {@link MiniMessage#deserialize(String)}.
     *
     * @param raw the raw message string
     * @return the rendered Component
     */
    static Component render(String raw) {
        if (raw == null) return Component.empty();
        if (raw.indexOf('§') >= 0) {
            return LEGACY_SECTION.deserialize(raw);
        }
        if (isLegacy(raw)) {
            return LEGACY_AMPERSAND.deserialize(raw);
        }
        return MM.deserialize(raw);
    }

    /**
     * Formats an arbitrary raw template with {@code %x%} placeholder pairs, applying the
     * injection-safe substitution rules (D-03/D-04). On the MiniMessage path each value is
     * inserted via {@link Placeholder#unparsed(String, String)} so a value containing
     * {@code <...>} renders as literal text and is never parsed as a tag. On the legacy path
     * the value is plain-{@code String.replace}d (already literal) before deserialization.
     *
     * @param raw  the raw template string
     * @param args placeholder/value pairs (e.g. {@code "%query%", userInput})
     * @return the rendered Component with placeholders substituted
     * @throws IllegalArgumentException if {@code args} is not in pairs
     */
    static Component formatRaw(String raw, Object... args) {
        if (args.length % 2 != 0)
            throw new IllegalArgumentException("Args must be in pairs of placeholder and value!");
        if (raw == null) return Component.empty();

        if (isLegacy(raw)) {
            // Legacy path: plain String.replace is already literal — preserves MSG-02 byte-identical output.
            boolean section = raw.indexOf('§') >= 0;
            String formatted = raw;
            for (int i = 0; i < args.length; i += 2) {
                formatted = formatted.replace(args[i].toString(), args[i + 1].toString());
            }
            return section ? LEGACY_SECTION.deserialize(formatted) : LEGACY_AMPERSAND.deserialize(formatted);
        }

        // MiniMessage path: rewrite each literal %x%/{x} token to a <tag> and bind its value as
        // an unparsed (injection-safe) placeholder (D-03/D-04). MiniMessage only substitutes
        // <tag> syntax, so the literal author token must be converted to its tag form for the
        // resolver to apply. The author-facing %x%/{x} convention is preserved; values containing
        // <...> render as literal text and are never parsed as tags. The tag name is derived from
        // the developer-supplied token (never from user input), so sanitising it to a valid
        // MiniMessage tag name (strip %, {, }) is safe.
        String template = raw;
        TagResolver[] resolvers = new TagResolver[args.length / 2];
        for (int i = 0; i < args.length; i += 2) {
            String token = args[i].toString();                        // e.g. "%query%" or "{max}"
            String name = token.replaceAll("[^A-Za-z0-9_-]", "")      // -> "query" / "max"
                               .toLowerCase(Locale.ROOT);
            template = template.replace(token, "<" + name + ">");     // %query% -> <query>
            resolvers[i / 2] = Placeholder.unparsed(name, args[i + 1].toString());
        }
        return MM.deserialize(template, resolvers);
    }

    /**
     * Gets the rendered message Component.
     *
     * @return the message rendered to an Adventure {@link Component}.
     */
    public Component get() {
        if (cached != null) return cached;
        Component rendered = render(message);
        // Cache only placeholder-free keys; %x% keys must parse per-format so resolvers apply.
        if (message == null || message.indexOf('%') < 0) {
            cached = rendered;
        }
        return rendered;
    }

    /**
     * Gets the message Component with placeholders replaced.
     *
     * @param args The placeholder replacements in pairs (placeholder, value).
     * @return The message Component with placeholders substituted (injection-safe on the MM path).
     */
    public Component format(Object... args) {
        return formatRaw(message, args);
    }

    /**
     * Gets the message Component with the prefix prepended.
     *
     * @return the PREFIX Component appended with this message Component (D-02 / Pattern 4).
     */
    public Component getWithPrefix() {
        return PREFIX.get().append(get());
    }

    /**
     * Gets the message Component with the prefix prepended and placeholders replaced.
     *
     * @param args The placeholder replacements in pairs (placeholder, value).
     * @return the PREFIX Component appended with the formatted message Component.
     */
    public Component formatWithPrefix(Object... args) {
        return PREFIX.get().append(format(args));
    }
}
