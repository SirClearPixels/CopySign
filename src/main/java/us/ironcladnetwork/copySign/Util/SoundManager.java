package us.ironcladnetwork.copySign.Util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import us.ironcladnetwork.copySign.CopySign;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages sound effects for the CopySign plugin.
 * Handles playing sounds for various operations with version compatibility.
 */
public class SoundManager {
    private final CopySign plugin;
    private final Map<String, Sound> soundCache = new HashMap<>();
    private final Map<String, String> soundNameCache = new HashMap<>();
    private Map<String, Sound> soundLookup = new HashMap<>();
    private boolean enabled = false;
    
    public SoundManager(CopySign plugin) {
        this.plugin = plugin;
        initialize();
    }
    
    /**
     * Initializes the sound system and caches valid sounds.
     */
    private void initialize() {
        // Check if sounds are enabled in config
        enabled = plugin.getConfig().getBoolean("sign-interaction.sounds.enabled", true);

        if (!enabled) {
            plugin.getLogger().info("Sound effects are disabled in configuration");
            return;
        }

        // Build the registry-based sound lookup map before caching configured sounds
        buildSoundLookup();

        // Cache configured sounds
        cacheSoundIfValid("copy", plugin.getConfig().getString("sign-interaction.sounds.copy", "BLOCK_NOTE_BLOCK_PLING"));
        cacheSoundIfValid("paste", plugin.getConfig().getString("sign-interaction.sounds.paste", "BLOCK_NOTE_BLOCK_CHIME"));
        cacheSoundIfValid("save", plugin.getConfig().getString("sign-interaction.sounds.save", "ENTITY_EXPERIENCE_ORB_PICKUP"));
        cacheSoundIfValid("error", plugin.getConfig().getString("sign-interaction.sounds.error", "BLOCK_NOTE_BLOCK_BASS"));
        
        plugin.getLogger().info("Sound system initialized with " + soundCache.size() + " valid sounds");
    }
    
    /**
     * Caches a sound if it's valid for the current server version.
     * 
     * @param key The sound key
     * @param soundName The sound name from config
     */
    private void cacheSoundIfValid(String key, String soundName) {
        if (soundName == null || soundName.isEmpty()) {
            plugin.getDebugLogger().debug("Sound '" + key + "' is disabled (empty string)");
            return;
        }

        Sound sound = lookupSound(soundName);
        if (sound != null) {
            soundCache.put(key, sound);
            soundNameCache.put(key, soundName);
            plugin.getDebugLogger().debug("Cached sound '" + key + "': " + soundName);
            return;
        }

        // Try alternative sound names for version compatibility
        String alternativeName = getAlternativeSoundName(soundName);
        if (alternativeName != null) {
            sound = lookupSound(alternativeName);
            if (sound != null) {
                soundCache.put(key, sound);
                soundNameCache.put(key, alternativeName);
                plugin.getDebugLogger().debug("Cached sound '" + key + "' with alternative: " + alternativeName);
                return;
            }
        }

        plugin.getLogger().warning("Invalid sound name for '" + key + "': " + soundName);
    }

    /**
     * Builds a lookup map from enum-style sound names to Sound objects by iterating
     * the entire Registry.SOUNDS at initialization time.
     * <p>
     * This approach is necessary because the registry key format (e.g., "block.note_block.pling")
     * cannot be reliably derived from the enum-style name (e.g., "BLOCK_NOTE_BLOCK_PLING")
     * via simple string manipulation. Iterating the registry gives us the correct mapping.
     * <p>
     * The deprecated {@code sound.getKey()} call is intentional -- it is called once at init
     * and the result is cached, matching the Phase 1 established pattern for soundNameCache.
     */
    @SuppressWarnings({"deprecation", "removal"})
    private void buildSoundLookup() {
        Map<String, Sound> lookup = new HashMap<>();
        int count = 0;
        for (Sound sound : Registry.SOUNDS) {
            try {
                NamespacedKey key = sound.getKey();
                // Convert registry key (e.g., "block.note_block.pling")
                // to enum-style name (e.g., "BLOCK_NOTE_BLOCK_PLING")
                String enumStyleName = key.getKey().toUpperCase(Locale.ROOT).replace('.', '_');
                lookup.put(enumStyleName, sound);
                // Also store the raw registry key for direct lookups
                lookup.put(key.getKey(), sound);
                count++;
            } catch (Exception e) {
                // Skip sounds that can't be processed
            }
        }
        this.soundLookup = lookup;
        plugin.getDebugLogger().debug("Built sound lookup with " + count + " registry entries");
    }

    /**
     * Looks up a Sound from the registry by name.
     * Handles both enum-style names (BLOCK_NOTE_BLOCK_PLING) and
     * namespaced keys (block.note_block.pling) and fully-qualified keys
     * (minecraft:block.note_block.pling).
     *
     * @param soundName The sound name to look up
     * @return The Sound, or null if not found
     */
    private Sound lookupSound(String soundName) {
        if (soundName == null || soundName.isEmpty()) {
            return null;
        }

        // Try exact match first (handles both enum-style and key-style as stored)
        Sound sound = soundLookup.get(soundName);
        if (sound != null) return sound;

        // Try uppercase match (for case-insensitive enum names like "block_note_block_pling")
        sound = soundLookup.get(soundName.toUpperCase(Locale.ROOT));
        if (sound != null) return sound;

        // Try lowercase match (for registry key-style names like "block.note_block.pling")
        sound = soundLookup.get(soundName.toLowerCase(Locale.ROOT));
        if (sound != null) return sound;

        // Final fallback: try as a direct NamespacedKey (for "minecraft:block.note_block.pling" format)
        try {
            NamespacedKey key = NamespacedKey.fromString(soundName.toLowerCase(Locale.ROOT));
            if (key != null) {
                return Registry.SOUNDS.get(key);
            }
        } catch (Exception e) {
            plugin.getDebugLogger().debug("Failed to parse sound as NamespacedKey: " + soundName);
        }

        return null;
    }
    
    /**
     * Gets alternative sound names for version compatibility.
     * 
     * @param soundName The original sound name
     * @return Alternative sound name or null
     */
    private String getAlternativeSoundName(String soundName) {
        // Map of old names to new names (for different MC versions)
        Map<String, String> alternatives = new HashMap<>();
        alternatives.put("BLOCK_NOTE_PLING", "BLOCK_NOTE_BLOCK_PLING");
        alternatives.put("BLOCK_NOTE_CHIME", "BLOCK_NOTE_BLOCK_CHIME");
        alternatives.put("BLOCK_NOTE_BASS", "BLOCK_NOTE_BLOCK_BASS");
        alternatives.put("ENTITY_EXPERIENCE_ORB_PICKUP", "ENTITY_EXPERIENCE_BOTTLE_THROW");
        
        return alternatives.get(soundName.toUpperCase());
    }
    
    /**
     * Plays the copy sound effect.
     * 
     * @param player The player to play the sound for
     */
    public void playCopySound(Player player) {
        playSound(player, "copy");
    }
    
    /**
     * Plays the paste sound effect.
     * 
     * @param player The player to play the sound for
     */
    public void playPasteSound(Player player) {
        playSound(player, "paste");
    }
    
    /**
     * Plays the save sound effect.
     * 
     * @param player The player to play the sound for
     */
    public void playSaveSound(Player player) {
        playSound(player, "save");
    }
    
    /**
     * Plays the error sound effect.
     * 
     * @param player The player to play the sound for
     */
    public void playErrorSound(Player player) {
        playSound(player, "error");
    }
    
    /**
     * Plays a sound effect for the player.
     * 
     * @param player The player to play the sound for
     * @param key The sound key
     */
    private void playSound(Player player, String key) {
        if (!enabled) {
            return;
        }
        
        Sound sound = soundCache.get(key);
        if (sound == null) {
            plugin.getDebugLogger().debug("No cached sound for key: " + key);
            return;
        }
        
        try {
            // Play sound at player location with default volume and pitch
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            plugin.getDebugLogger().debug(player, "Played sound: " + soundNameCache.getOrDefault(key, "unknown"));
        } catch (Exception e) {
            plugin.getDebugLogger().debug("Failed to play sound: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reloads the sound configuration.
     */
    public void reload() {
        soundCache.clear();
        soundNameCache.clear();
        soundLookup.clear();
        initialize();
    }
    
    /**
     * Checks if sounds are enabled.
     * 
     * @return true if sounds are enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}