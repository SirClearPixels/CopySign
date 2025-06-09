package us.ironcladnetwork.copySign.Listeners;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.Player;
import us.ironcladnetwork.copySign.CopySign;
import us.ironcladnetwork.copySign.Util.Util;
import us.ironcladnetwork.copySign.Util.SignDataCache;
import org.bukkit.DyeColor;
import org.bukkit.block.sign.Side;
import java.util.List;
import us.ironcladnetwork.copySign.Lang.Lang;

/**
 * Event listener for handling sign placement with copied NBT data.
 * <p>
 * This listener intercepts {@link BlockPlaceEvent} when players place signs that contain
 * copied sign data stored in NBT tags. When such a sign is placed, the listener:
 * <ul>
 *   <li>Validates that the sign contains the required NBT data</li>
 *   <li>Checks cooldowns and permissions for paste operations</li>
 *   <li>Applies stored colors directly to the placed sign</li>
 *   <li>Caches text data for the {@link SignChangeListener} to apply</li>
 *   <li>Enforces sign type restrictions from configuration</li>
 * </ul>
 * <p>
 * The actual text content is applied by {@link SignChangeListener} after the sign
 * is fully placed and ready for text modification.
 * 
 * @author IroncladNetwork
 * @since 2.0.0
 * @see SignChangeListener
 * @see SignDataCache
 * @see SignCopyListener
 */
public class SignPlaceListener implements Listener {

    public SignPlaceListener() {
        // No assignment needed
    }

    @EventHandler
    public void onSignPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        // Ensure the placed block is a sign.
        if (!(block.getState() instanceof Sign))
            return;

        ItemStack itemStack = event.getItemInHand();
        if (itemStack == null)
            return;

        NBTItem nbtItem = new NBTItem(itemStack);
        // Check that both front and back text have been stored via NBT.
        if (!nbtItem.hasTag("copiedSignFront") || !nbtItem.hasTag("copiedSignBack"))
            return;
        
        Player player = event.getPlayer();
        
        // Check cooldown for paste operation
        if (!CopySign.getCooldownManager().canUseCommand(player, "paste")) {
            CopySign.getCooldownManager().sendCooldownMessage(player, "paste");
            event.setCancelled(true); // Cancel the sign placement
            return;
        }
        
        // Check if the sign type is allowed for pasting
        if (!isSignTypeAllowed(itemStack.getType().name())) {
            player.sendMessage(Lang.SIGN_TYPE_NOT_ALLOWED_PASTE.getWithPrefix());
            event.setCancelled(true); // Cancel the sign placement
            return;
        }
        
        Sign sign = (Sign) block.getState();
        
        // Apply the dye colors directly to the sign
        if (nbtItem.hasTag("copiedSignFrontColor")) {
            try {
                DyeColor frontDyeColor = DyeColor.valueOf(nbtItem.getString("copiedSignFrontColor"));
                sign.getSide(Side.FRONT).setColor(frontDyeColor);
            } catch (IllegalArgumentException e) {
                // Invalid dye color, skip it
            }
        }
        
        if (nbtItem.hasTag("copiedSignBackColor")) {
            try {
                DyeColor backDyeColor = DyeColor.valueOf(nbtItem.getString("copiedSignBackColor"));
                sign.getSide(Side.BACK).setColor(backDyeColor);
            } catch (IllegalArgumentException e) {
                // Invalid dye color, skip it
            }
        }
        
        // Update the sign state
        sign.update();

        // Cache the text data for the SignChangeEvent
        String copiedSignFront = nbtItem.getString("copiedSignFront");
        String copiedSignBack = nbtItem.getString("copiedSignBack");
        boolean signGlowing = nbtItem.hasTag("signGlowing") ? nbtItem.getBoolean("signGlowing") : false;
        
        String[] frontLines = copiedSignFront.split("\n", -1);
        frontLines = Util.preserveColors(frontLines);
        String[] backLines = copiedSignBack.split("\n", -1);
        backLines = Util.preserveColors(backLines);
        
        // Store only the text data in cache, as we've already applied the dye colors
        SignDataCache.put(block.getLocation(), new SignDataCache.SignData(frontLines, backLines, signGlowing));
        
        // Record command usage
        CopySign.getCooldownManager().recordCommandUse(player, "paste");
    }
    
    /**
     * Checks if a sign type is allowed to be copied/pasted based on the configuration.
     * 
     * @param signType The material name of the sign (e.g., "OAK_SIGN", "BIRCH_HANGING_SIGN")
     * @return true if the sign type is allowed, false otherwise
     */
    private boolean isSignTypeAllowed(String signType) {
        List<String> allowedTypes = CopySign.getInstance().getConfig().getStringList("sign-types.allowed");
        
        // If the list is empty, allow all sign types (default behavior)
        if (allowedTypes.isEmpty()) {
            return true;
        }
        
        // Check if the specific sign type is in the allowed list
        return allowedTypes.contains(signType);
    }
} 