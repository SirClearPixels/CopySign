package us.ironcladnetwork.copySign.Util;

import org.bukkit.entity.Player;
import us.ironcladnetwork.copySign.CopySign;
import us.ironcladnetwork.copySign.Lang.Lang;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages command cooldowns for players.
 * Tracks when players last used specific commands and enforces configured cooldown periods.
 */
public class CooldownManager {
    
    // Map of player UUID -> command -> last used timestamp
    private final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();
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
        int cooldownSeconds = plugin.getConfig().getInt("commands.cooldowns." + command, 0);
        
        // If cooldown is 0 or negative, no cooldown applies
        if (cooldownSeconds <= 0) {
            return true;
        }
        
        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCommands = playerCooldowns.get(playerId);
        
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
        UUID playerId = player.getUniqueId();
        
        playerCooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
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
        int cooldownSeconds = plugin.getConfig().getInt("commands.cooldowns." + command, 0);
        
        if (cooldownSeconds <= 0) {
            return 0;
        }
        
        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCommands = playerCooldowns.get(playerId);
        
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
        playerCooldowns.remove(player.getUniqueId());
    }
    
    /**
     * Clears expired cooldowns to prevent memory leaks.
     * Should be called periodically.
     */
    public void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();
        
        playerCooldowns.entrySet().removeIf(playerEntry -> {
            Map<String, Long> commands = playerEntry.getValue();
            
            // Remove expired command cooldowns
            commands.entrySet().removeIf(commandEntry -> {
                String command = commandEntry.getKey();
                long lastUsed = commandEntry.getValue();
                int cooldownSeconds = plugin.getConfig().getInt("commands.cooldowns." + command, 0);
                
                if (cooldownSeconds <= 0) {
                    return true; // Remove if no cooldown configured
                }
                
                long cooldownMillis = cooldownSeconds * 1000L;
                return (currentTime - lastUsed) >= cooldownMillis;
            });
            
            // Remove player entry if no commands left
            return commands.isEmpty();
        });
    }
} 