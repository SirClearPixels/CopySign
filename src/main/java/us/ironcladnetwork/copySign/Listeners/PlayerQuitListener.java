package us.ironcladnetwork.copySign.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import us.ironcladnetwork.copySign.CopySign;

/**
 * Listener to clean up player data when they leave the server.
 */
public class PlayerQuitListener implements Listener {
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up cooldowns for the leaving player to prevent memory leaks
        CopySign.getCooldownManager().clearPlayerCooldowns(event.getPlayer());
    }
} 