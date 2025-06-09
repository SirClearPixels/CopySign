package us.ironcladnetwork.copySign.Listeners;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import us.ironcladnetwork.copySign.Lang.Lang;
import us.ironcladnetwork.copySign.CopySign;
import us.ironcladnetwork.copySign.Util.ErrorHandler;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.DyeColor;

/**
 * Listener to copy a sign's text (both front and back) when a player shift+punches a sign while holding a sign item.
 */
public class SignCopyListener implements Listener {
    /**
     * Handles player interactions with signs for copying sign data.
     * <p>
     * This method processes {@link PlayerInteractEvent} when players shift+left-click on signs
     * while holding a sign item. The method performs the following operations:
     * <ul>
     *   <li>Validates that the player is sneaking and left-clicking</li>
     *   <li>Checks if the player is holding a sign item</li>
     *   <li>Verifies that CopySign is enabled for the player</li>
     *   <li>Validates permissions and cooldowns</li>
     *   <li>Ensures sign type compatibility between held and clicked signs</li>
     *   <li>Extracts sign data (text, colors, glow state)</li>
     *   <li>Stores the data in NBT tags on the held sign item</li>
     *   <li>Updates the item's lore for visual feedback</li>
     * </ul>
     * <p>
     * The method respects various configuration settings and permissions:
     * <ul>
     *   <li>Feature toggles for sign copying, color copying, and glow copying</li>
     *   <li>Sign type restrictions from configuration</li>
     *   <li>Per-player cooldowns for copy operations</li>
     *   <li>Permission-based access control</li>
     * </ul>
     * 
     * @param event The PlayerInteractEvent triggered by the player's interaction
     * @see SignPlaceListener
     * @see SignChangeListener
     * @see CooldownManager
     */
    @EventHandler
    public void onPlayerPunchSign(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();
        if (!player.isSneaking())
            return;

        // First check if the player is holding a sign - if not, silently return
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || !heldItem.getType().name().endsWith("_SIGN")) {
            // Player is not holding a sign, so they're not trying to copy - just return silently
            return;
        }

        // Check if the copy sign feature is enabled before doing any further validation
        if (!CopySign.getToggleManager().isEnabled(player)) {
            // Only show the disabled message if they're actually clicking on a sign
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && clickedBlock.getState() instanceof Sign) {
                player.sendMessage(Lang.COPYSIGN_DISABLED.getWithPrefix());
            }
            // Otherwise silently return
            return;
        }
        
        // Check if the sign-copy feature is enabled in config
        if (!CopySign.getInstance().getConfig().getBoolean("features.sign-copy", true)) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && clickedBlock.getState() instanceof Sign) {
                player.sendMessage(Lang.PREFIX.get() + "&cSign copying is currently disabled.");
            }
            return;
        }

        // Now check if the clicked block is a sign (only show message when feature is enabled)
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !(clickedBlock.getState() instanceof Sign)) {
            player.sendMessage(Lang.INVALID_SIGN.getWithPrefix());
            return;
        }

        // Check if the player has permission to use the sign copy feature.
        if (!player.hasPermission("copysign.use")) {
            player.sendMessage(Lang.NO_PERMISSION_USE.getWithPrefix());
            return;
        }
        
        // Check cooldown for copy operation
        if (!CopySign.getCooldownManager().canUseCommand(player, "copy")) {
            CopySign.getCooldownManager().sendCooldownMessage(player, "copy");
            return;
        }

        // Check if the clicked sign type is allowed
        if (!isSignTypeAllowed(clickedBlock.getType().name())) {
            player.sendMessage(Lang.SIGN_TYPE_NOT_ALLOWED_COPY.getWithPrefix());
            return;
        }
        
        // Check if the held sign type is allowed
        if (!isSignTypeAllowed(heldItem.getType().name())) {
            player.sendMessage(Lang.SIGN_TYPE_NOT_ALLOWED_COPY.getWithPrefix());
            return;
        }

        // Determine sign type category for held item and clicked block.
        String heldType = heldItem.getType().name();
        String clickedType = clickedBlock.getType().name();
        // A sign is considered hanging if its type name contains "HANGING_SIGN".
        boolean heldHanging = heldType.contains("HANGING_SIGN");
        boolean clickedHanging = clickedType.contains("HANGING_SIGN");
        // If the sign types do not match, send an error message and cancel the copy.
        if (heldHanging != clickedHanging) {
            player.sendMessage(Lang.SIGN_TYPE_MISMATCH.formatWithPrefix("%s", heldHanging ? Lang.HANGING_SIGN.get() : Lang.REGULAR_SIGN.get()));
            return;
        }

        Sign sign = (Sign) clickedBlock.getState();
        // Retrieve side colors using the new API.
        DyeColor frontColor = sign.getSide(org.bukkit.block.sign.Side.FRONT).getColor();
        DyeColor backColor = sign.getSide(org.bukkit.block.sign.Side.BACK).getColor();
        
        // If the player lacks permission to copy color, use our safe conversion fallback instead of directly calling DyeColor.valueOf("OAK")
        if (!player.hasPermission("copysign.copycolor") || !CopySign.getInstance().getConfig().getBoolean("features.copy-colors", true)) {
            frontColor = getValidDyeColor("black");
            backColor = getValidDyeColor("black");
        }

        // Concatenate the sign's four lines for both front and back sides using newline as the separator.
        StringBuilder frontText = new StringBuilder();
        StringBuilder backText = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            frontText.append(sign.getSide(org.bukkit.block.sign.Side.FRONT).getLine(i));
            backText.append(sign.getSide(org.bukkit.block.sign.Side.BACK).getLine(i));
            if (i < 3) {
                frontText.append("\n");
                backText.append("\n");
            }
        }

        // Copy the glow state from the sign
        @SuppressWarnings("deprecation")
        boolean signGlowing = sign.isGlowingText();

        // If the player lacks permission to copy glow, disable it
        if (!player.hasPermission("copysign.copyglow") || !CopySign.getInstance().getConfig().getBoolean("features.copy-glow", true)) {
            signGlowing = false;
        }

        // Use NBT-API to store the copied sign text and side colors onto the held sign item.
        try {
            NBTItem nbtItem = new NBTItem(heldItem);
            nbtItem.setString("copiedSignFront", frontText.toString());
            nbtItem.setString("copiedSignBack", backText.toString());
            nbtItem.setString("copiedSignFrontColor", frontColor.name());
            nbtItem.setString("copiedSignBackColor", backColor.name());
            nbtItem.setBoolean("signGlowing", signGlowing);
            // Store the sign type so that later paste or change events can verify the type if desired.
            nbtItem.setString("signType", clickedHanging ? "hanging" : "regular");

            // Get the updated item from NBTItem.
            ItemStack updatedItem = nbtItem.getItem();

            // Update item meta with lore for visual display.
            ItemMeta meta = updatedItem.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add("§f§l[§b§lCopied Sign§f§l]");
                
                // Handle front side
                String[] frontLines = frontText.toString().split("\n");
                boolean hasFrontData = false;
                for (int i = 0; i < frontLines.length; i++) {
                    if (!frontLines[i].isEmpty()) {
                        if (!hasFrontData) {
                            lore.add("§f§lFront:");
                            lore.add("§f§lColor: " + frontColor.name());
                            hasFrontData = true;
                        }
                        lore.add("§f§lLine " + (i + 1) + ": §f\"§b" + frontLines[i] + "§f\"");
                    }
                }
                
                // Handle back side
                String[] backLines = backText.toString().split("\n");
                boolean hasBackData = false;
                for (int i = 0; i < backLines.length; i++) {
                    if (!backLines[i].isEmpty()) {
                        if (!hasBackData) {
                            lore.add("§f§lBack:");
                            lore.add("§f§lColor: " + backColor.name());
                            hasBackData = true;
                        }
                        lore.add("§f§lLine " + (i + 1) + ": §f\"§b" + backLines[i] + "§f\"");
                    }
                }
                
                // Always show glowing state
                lore.add("§e§lGlowing: " + (signGlowing ? "§aTrue" : "§cFalse"));
                
                meta.setLore(lore);
                updatedItem.setItemMeta(meta);
            }
            
            // Replace the held sign item with the updated item (with lore metadata).
            player.getInventory().setItemInMainHand(updatedItem);

            player.sendMessage(Lang.SIGN_COPIED.getWithPrefix());
            
            // Record command usage
            CopySign.getCooldownManager().recordCommandUse(player, "copy");
            
        } catch (Exception e) {
            ErrorHandler.handleNBTError("copying sign data", player, e);
        }
    }

    /**
     * Safely converts a string to a DyeColor.
     * If the provided string is not a valid DyeColor (e.g. "OAK"), it falls back to a default value.
     *
     * @param colorString the stored string representing the color
     * @return a valid DyeColor (DyeColor.WHITE as default)
     */
    private DyeColor getValidDyeColor(String colorString) {
        try {
            return DyeColor.valueOf(colorString.toUpperCase());
        } catch (IllegalArgumentException ex) {
            // Map known non-dye values to a fallback. In this case, "OAK" is likely coming from an oak sign.
            if (colorString.equalsIgnoreCase("oak")) {
                return DyeColor.WHITE; // default to white or choose your preferred fallback.
            }
            // Log a warning if desired, then return a default value.
            return DyeColor.WHITE;
        }
    }

    /**
     * Checks if a sign type is allowed to be copied based on the configuration.
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