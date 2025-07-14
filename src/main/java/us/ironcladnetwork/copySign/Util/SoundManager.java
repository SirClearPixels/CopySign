package us.ironcladnetwork.copySign.Util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import us.ironcladnetwork.copySign.CopySign;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages sound effects for the CopySign plugin.
 * Handles playing sounds for various operations with version compatibility.
 */
public class SoundManager {
    private final CopySign plugin;
    private final Map<String, Sound> soundCache = new HashMap<>();
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
        
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            soundCache.put(key, sound);
            plugin.getDebugLogger().debug("Cached sound '" + key + "': " + soundName);
        } catch (IllegalArgumentException e) {
            // Try alternative sound names for version compatibility
            String alternativeName = getAlternativeSoundName(soundName);
            if (alternativeName != null) {
                try {
                    Sound sound = Sound.valueOf(alternativeName);
                    soundCache.put(key, sound);
                    plugin.getDebugLogger().debug("Cached sound '" + key + "' with alternative: " + alternativeName);
                } catch (IllegalArgumentException e2) {
                    plugin.getLogger().warning("Invalid sound name for '" + key + "': " + soundName);
                }
            } else {
                plugin.getLogger().warning("Invalid sound name for '" + key + "': " + soundName);
            }
        }
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
            plugin.getDebugLogger().debug(player, "Played sound: " + sound.name());
        } catch (Exception e) {
            plugin.getDebugLogger().debug("Failed to play sound: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reloads the sound configuration.
     */
    public void reload() {
        soundCache.clear();
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