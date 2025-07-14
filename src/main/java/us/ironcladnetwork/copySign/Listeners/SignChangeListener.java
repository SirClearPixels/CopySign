package us.ironcladnetwork.copySign.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import us.ironcladnetwork.copySign.CopySign;
import us.ironcladnetwork.copySign.Util.SignDataCache;
import us.ironcladnetwork.copySign.Util.SignDataCache.SignData;

/**
 * Event listener for applying cached sign text data during sign editing.
 * <p>
 * This listener works in conjunction with {@link SignPlaceListener} to complete the
 * sign pasting process. When a sign is placed with copied NBT data, the placement
 * listener caches the text content, and this listener applies it during the
 * {@link SignChangeEvent}.
 * <p>
 * The listener performs the following operations:
 * <ul>
 *   <li>Retrieves cached sign data from {@link SignDataCache}</li>
 *   <li>Applies front-side text directly through the event</li>
 *   <li>Schedules a task to apply back-side text and glow state</li>
 *   <li>Cleans up the cached data after application</li>
 * </ul>
 * <p>
 * The back-side text and glow state must be applied in a scheduled task because
 * the {@link SignChangeEvent} only provides direct access to front-side text.
 * 
 * @author IroncladNetwork
 * @since 2.0.0
 * @see SignPlaceListener
 * @see SignDataCache
 * @see SignCopyListener
 */
public class SignChangeListener implements Listener {

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Block block = event.getBlock();
        // Check if we have cached sign data for this sign location.
        SignData data = SignDataCache.get(block.getLocation());
        if (data == null) return;

        String[] frontLines = data.getFront();
        // Set the front lines
        for (int i = 0; i < Math.min(frontLines.length, event.getLines().length); i++) {
            event.setLine(i, frontLines[i]);
        }

        // Schedule a task on the next tick to update the back lines 
        // since SignChangeEvent does not directly support modifying the back side.
        Bukkit.getScheduler().runTask(CopySign.getInstance(), () -> {
            Sign sign = (Sign) block.getState();
            String[] backLines = data.getBack();
            // Update the back side lines if supported.
            for (int i = 0; i < backLines.length; i++) {
                sign.getSide(Side.BACK).setLine(i, backLines[i]);
            }
            // Apply the glow state if desired.
            sign.getSide(Side.FRONT).setGlowingText(data.isGlowing());
            sign.getSide(Side.BACK).setGlowingText(data.isGlowing());
            sign.update();
            
            // Play paste sound after successful paste
            if (event.getPlayer() != null) {
                CopySign.getInstance().getSoundManager().playPasteSound(event.getPlayer());
                // Record metrics
                CopySign.getInstance().getMetricsManager().recordPasteOperation(event.getPlayer());
            }
        });

        // Remove the data from the cache since it has now been applied.
        SignDataCache.remove(block.getLocation());
    }
} 