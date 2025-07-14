package us.ironcladnetwork.copySign.Util;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import us.ironcladnetwork.copySign.CopySign;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages confirmation prompts for sensitive operations
 */
public class ConfirmationManager {
    
    private final Map<UUID, PendingConfirmation> pendingConfirmations = new HashMap<>();
    private final CopySign plugin;
    
    public ConfirmationManager(CopySign plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Request confirmation from a player
     * @param player The player to request confirmation from
     * @param action The action to confirm
     * @param callback The callback to execute if confirmed
     * @param timeoutSeconds How long to wait for confirmation
     */
    public void requestConfirmation(Player player, String action, Runnable callback, int timeoutSeconds) {
        UUID playerId = player.getUniqueId();
        
        // Cancel any existing confirmation
        cancelConfirmation(playerId);
        
        // Create new confirmation
        PendingConfirmation confirmation = new PendingConfirmation(action, callback);
        pendingConfirmations.put(playerId, confirmation);
        
        // Schedule timeout
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingConfirmations.remove(playerId) != null) {
                    player.sendMessage("§cConfirmation timed out.");
                }
            }
        }.runTaskLater(plugin, timeoutSeconds * 20L);
    }
    
    /**
     * Check if a player has a pending confirmation
     */
    public boolean hasPendingConfirmation(UUID playerId) {
        return pendingConfirmations.containsKey(playerId);
    }
    
    /**
     * Confirm a pending action
     */
    public boolean confirm(UUID playerId) {
        PendingConfirmation confirmation = pendingConfirmations.remove(playerId);
        if (confirmation != null) {
            confirmation.callback.run();
            return true;
        }
        return false;
    }
    
    /**
     * Cancel a pending confirmation
     */
    public boolean cancelConfirmation(UUID playerId) {
        return pendingConfirmations.remove(playerId) != null;
    }
    
    /**
     * Get the pending action description
     */
    public String getPendingAction(UUID playerId) {
        PendingConfirmation confirmation = pendingConfirmations.get(playerId);
        return confirmation != null ? confirmation.action : null;
    }
    
    /**
     * Clear all pending confirmations (for plugin disable)
     */
    public void clearAll() {
        pendingConfirmations.clear();
    }
    
    /**
     * Get the number of pending confirmations.
     * Useful for monitoring memory usage.
     * 
     * @return Number of pending confirmations
     */
    public int getPendingCount() {
        return pendingConfirmations.size();
    }
    
    private static class PendingConfirmation {
        final String action;
        final Runnable callback;
        
        PendingConfirmation(String action, Runnable callback) {
            this.action = action;
            this.callback = callback;
        }
    }
}