package us.ironcladnetwork.copySign.Util;

import org.bukkit.entity.Player;
import us.ironcladnetwork.copySign.CopySign;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages confirmation prompts for sensitive operations.
 * Thread-safe implementation using ConcurrentHashMap for Folia compatibility.
 */
public class ConfirmationManager {

    private final Map<UUID, PendingConfirmation> pendingConfirmations = new ConcurrentHashMap<>();
    private final Map<UUID, Object> timeoutTasks = new ConcurrentHashMap<>();
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

        // Schedule timeout using entity scheduler (player-specific operation)
        Object timeoutTask = SchedulerUtil.runAtEntityDelayed(plugin, player, () -> {
            if (pendingConfirmations.remove(playerId) != null) {
                // Use entity scheduler to send message to player
                SchedulerUtil.runAtEntity(plugin, player, () -> {
                    player.sendMessage("§cConfirmation timed out.");
                });
                timeoutTasks.remove(playerId);
            }
        }, timeoutSeconds * 20L);

        // Store the task so we can cancel it if needed
        timeoutTasks.put(playerId, timeoutTask);
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
        // Cancel the timeout task if it exists
        Object timeoutTask = timeoutTasks.remove(playerId);
        if (timeoutTask != null) {
            SchedulerUtil.cancelTask(timeoutTask);
        }
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
        // Cancel all timeout tasks
        timeoutTasks.values().forEach(SchedulerUtil::cancelTask);
        timeoutTasks.clear();
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