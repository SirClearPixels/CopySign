package us.ironcladnetwork.copySign.Listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import us.ironcladnetwork.copySign.CopySign;
import us.ironcladnetwork.copySign.Util.SignDataCache;
import us.ironcladnetwork.copySign.Util.SignDataCache.SignData;
import us.ironcladnetwork.copySign.Util.SchedulerUtil;

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
        // Set the front lines using Adventure Component API (non-deprecated)
        for (int i = 0; i < Math.min(frontLines.length, 4); i++) {
            Component component = LegacyComponentSerializer.legacySection()
                .deserialize(frontLines[i] != null ? frontLines[i] : "");
            event.line(i, component);
        }

        // Schedule a task on the next tick to update the back lines
        // since SignChangeEvent does not directly support modifying the back side.
        // Use region scheduler since this is a block operation
        SchedulerUtil.runAtLocationDelayed(CopySign.getInstance(), block.getLocation(), () -> {
            Sign sign = (Sign) block.getState();
            String[] backLines = data.getBack();
            // Update the back side lines if supported.
            for (int i = 0; i < backLines.length; i++) {
                sign.getSide(Side.BACK).setLine(i, backLines[i]);
            }
            // Apply per-side glow state independently.
            sign.getSide(Side.FRONT).setGlowingText(data.isFrontGlowing());
            sign.getSide(Side.BACK).setGlowingText(data.isBackGlowing());
            sign.update();

            // Play paste sound after successful paste
            if (event.getPlayer() != null) {
                CopySign.getInstance().getSoundManager().playPasteSound(event.getPlayer());
                // Record metrics
                CopySign.getInstance().getMetricsManager().recordPasteOperation(event.getPlayer());
            }
        }, 1L); // 1 tick delay

        // Remove the data from the cache since it has now been applied.
        SignDataCache.remove(block.getLocation());
    }
} 