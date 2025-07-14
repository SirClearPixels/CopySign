package us.ironcladnetwork.copySign.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import us.ironcladnetwork.copySign.CopySign;

/**
 * Listener to clean up player data when they leave the server.
 * Prevents memory leaks by removing cached data for players who have disconnected.
 */
public class PlayerQuitListener implements Listener {
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up cooldowns for the leaving player to prevent memory leaks
        CopySign.getCooldownManager().clearPlayerCooldowns(event.getPlayer());
        
        // Clean up toggle state cache if caching is enabled
        CopySign.getToggleManager().clearPlayerCache(event.getPlayer());
    }
} 