package us.ironcladnetwork.copySign.Util;

import org.bukkit.entity.Player;
import us.ironcladnetwork.copySign.CopySign;
import us.ironcladnetwork.copySign.Lang.Lang;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages command cooldowns for players.
 * Tracks when players last used specific commands and enforces configured cooldown periods.
 * Uses optimized storage with long keys for memory efficiency.
 */
public class CooldownManager {
    
    // Map of player UUID hash -> command -> last used timestamp
    // Using long keys instead of UUID objects for memory efficiency
    private final Map<Long, Map<String, Long>> playerCooldowns = new ConcurrentHashMap<>();
    private final CopySign plugin;
    
    public CooldownManager(CopySign plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Checks if a player can use a command (not on cooldown).
     * 
     * @param player The player to check
     * @param command The command name (e.g., "save", "load", "copy")
     * @return true if the player can use the command, false if on cooldown
     */
    public boolean canUseCommand(Player player, String command) {
        // Check if player has cooldown bypass permission
        if (player.hasPermission("copysign.bypass.cooldowns")) {
            return true;
        }
        
        int cooldownSeconds = plugin.getConfigInt("commands.cooldowns." + command, 0);
        
        // If cooldown is 0 or negative, no cooldown applies
        if (cooldownSeconds <= 0) {
            return true;
        }
        
        long playerKey = getPlayerKey(player.getUniqueId());
        Map<String, Long> playerCommands = playerCooldowns.get(playerKey);
        
        if (playerCommands == null) {
            return true; // First time using any command
        }
        
        Long lastUsed = playerCommands.get(command);
        if (lastUsed == null) {
            return true; // First time using this specific command
        }
        
        long currentTime = System.currentTimeMillis();
        long cooldownMillis = cooldownSeconds * 1000L;
        
        return (currentTime - lastUsed) >= cooldownMillis;
    }
    
    /**
     * Records that a player has used a command.
     * 
     * @param player The player who used the command
     * @param command The command name
     */
    public void recordCommandUse(Player player, String command) {
        long playerKey = getPlayerKey(player.getUniqueId());
        
        playerCooldowns.computeIfAbsent(playerKey, k -> new ConcurrentHashMap<>())
                      .put(command, System.currentTimeMillis());
    }
    
    /**
     * Gets the remaining cooldown time for a command in seconds.
     * 
     * @param player The player to check
     * @param command The command name
     * @return remaining cooldown in seconds, or 0 if no cooldown
     */
    public int getRemainingCooldown(Player player, String command) {
        int cooldownSeconds = plugin.getConfigInt("commands.cooldowns." + command, 0);
        
        if (cooldownSeconds <= 0) {
            return 0;
        }
        
        long playerKey = getPlayerKey(player.getUniqueId());
        Map<String, Long> playerCommands = playerCooldowns.get(playerKey);
        
        if (playerCommands == null) {
            return 0;
        }
        
        Long lastUsed = playerCommands.get(command);
        if (lastUsed == null) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long cooldownMillis = cooldownSeconds * 1000L;
        long elapsed = currentTime - lastUsed;
        
        if (elapsed >= cooldownMillis) {
            return 0;
        }
        
        return (int) ((cooldownMillis - elapsed) / 1000L) + 1; // +1 to round up
    }
    
    /**
     * Sends a cooldown message to the player.
     * 
     * @param player The player to send the message to
     * @param command The command they tried to use
     */
    public void sendCooldownMessage(Player player, String command) {
        int remaining = getRemainingCooldown(player, command);
        String timeUnit = remaining == 1 ? "second" : "seconds";
        player.sendMessage(Lang.PREFIX.get() + "&cYou must wait " + remaining + " " + timeUnit + " before using that command again.");
    }
    
    /**
     * Clears all cooldowns for a player (useful for cleanup when player leaves).
     * 
     * @param player The player whose cooldowns to clear
     */
    public void clearPlayerCooldowns(Player player) {
        playerCooldowns.remove(getPlayerKey(player.getUniqueId()));
    }
    
    /**
     * Clears expired cooldowns to prevent memory leaks.
     * Should be called periodically.
     * Optimized to O(n) complexity using single-pass iteration.
     * Thread-safe implementation using pre-snapshotted config values.
     * 
     * @param cooldownConfig Pre-snapshotted cooldown configuration from main thread
     */
    public void cleanupExpiredCooldowns(Map<String, Integer> cooldownConfig) {
        long currentTime = System.currentTimeMillis();
        
        // Use iterator for efficient single-pass cleanup (O(n) instead of O(n²))
        Iterator<Map.Entry<Long, Map<String, Long>>> playerIterator = playerCooldowns.entrySet().iterator();
        
        while (playerIterator.hasNext()) {
            Map.Entry<Long, Map<String, Long>> playerEntry = playerIterator.next();
            Map<String, Long> commands = playerEntry.getValue();
            
            // Clean up expired commands for this player
            Iterator<Map.Entry<String, Long>> commandIterator = commands.entrySet().iterator();
            while (commandIterator.hasNext()) {
                Map.Entry<String, Long> commandEntry = commandIterator.next();
                String command = commandEntry.getKey();
                long lastUsed = commandEntry.getValue();
                
                // Use pre-snapshotted config for thread safety
                int cooldownSeconds = cooldownConfig.getOrDefault(command, 0);
                
                // Remove if no cooldown configured or cooldown has expired
                if (cooldownSeconds <= 0) {
                    commandIterator.remove();
                } else {
                    long cooldownMillis = cooldownSeconds * 1000L;
                    if ((currentTime - lastUsed) >= cooldownMillis) {
                        commandIterator.remove();
                    }
                }
            }
            
            // Remove player entry if no commands left
            if (commands.isEmpty()) {
                playerIterator.remove();
            }
        }
    }
    
    /**
     * Clears expired cooldowns to prevent memory leaks.
     * This method loads config values synchronously - use only from main thread.
     * For async operation, use {@link #cleanupExpiredCooldowns(Map)} instead.
     * 
     * @deprecated Use cleanupExpiredCooldowns(Map) for thread safety
     */
    @Deprecated
    public void cleanupExpiredCooldowns() {
        // Create a snapshot of config values for thread safety
        Map<String, Integer> cooldownConfig = new HashMap<>();
        if (plugin.configContains("commands.cooldowns")) {
            org.bukkit.configuration.ConfigurationSection cooldownsSection = plugin.getConfigSectionSafe("commands.cooldowns");
            if (cooldownsSection != null) {
                for (String command : cooldownsSection.getKeys(false)) {
                    cooldownConfig.put(command, cooldownsSection.getInt(command, 0));
                }
            }
        }
        cleanupExpiredCooldowns(cooldownConfig);
    }
    
    /**
     * Converts a UUID to a memory-efficient long key.
     * Uses XOR of most and least significant bits to create a unique long.
     * 
     * @param uuid The UUID to convert
     * @return A long key representing the UUID
     */
    private long getPlayerKey(UUID uuid) {
        // XOR the most and least significant bits
        // This provides good distribution while being memory efficient
        return uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
    }
    
    /**
     * Gets the current number of players with active cooldowns.
     * Useful for monitoring memory usage.
     * 
     * @return Number of players with cooldowns
     */
    public int getActiveCooldownCount() {
        return playerCooldowns.size();
    }
    
    /**
     * Gets the total number of command cooldowns being tracked.
     * Useful for monitoring memory usage.
     * 
     * @return Total number of command cooldowns
     */
    public int getTotalCooldownEntries() {
        return playerCooldowns.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
} 