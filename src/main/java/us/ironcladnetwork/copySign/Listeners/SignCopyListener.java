package us.ironcladnetwork.copySign.Listeners;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Material;
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
import us.ironcladnetwork.copySign.Util.NBTValidationUtil;
import us.ironcladnetwork.copySign.Util.Permissions;
import us.ironcladnetwork.copySign.Util.VersionCompatibility;
import java.util.List;
import us.ironcladnetwork.copySign.Util.SignValidationUtil;
import us.ironcladnetwork.copySign.Util.SignLoreBuilder;
import us.ironcladnetwork.copySign.Util.DesignConstants;
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
        
        // Check if sneaking is required for copying (from config)
        if (CopySign.getInstance().getConfigManager().requireSneakToCopy() && !player.isSneaking())
            return;

        // First check if the player is holding a sign - if not, silently return
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR || !heldItem.getType().name().endsWith("_SIGN")) {
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
        if (!CopySign.getInstance().getConfigManager().isSignCopyEnabled()) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && clickedBlock.getState() instanceof Sign) {
                player.sendMessage(Lang.COMMAND_FEATURE_DISABLED.formatWithPrefix("%feature%", "Sign copying"));
            }
            return;
        }

        // Now check if the clicked block is a sign (return silently if not)
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !(clickedBlock.getState() instanceof Sign)) {
            // Return silently for non-sign blocks to avoid spam
            return;
        }

        // Check if the player has permission to use the sign copy feature.
        if (!Permissions.canUse(player)) {
            player.sendMessage(Lang.NO_PERMISSION_USE.getWithPrefix());
            return;
        }
        
        // Check cooldown for copy operation
        if (!CopySign.getCooldownManager().canUseCommand(player, "copy")) {
            CopySign.getCooldownManager().sendCooldownMessage(player, "copy");
            return;
        }

        // Check if the clicked sign type is allowed
        if (!SignValidationUtil.isSignTypeAllowed(clickedBlock.getType().name())) {
            player.sendMessage(Lang.SIGN_TYPE_NOT_ALLOWED_COPY.getWithPrefix());
            return;
        }
        
        // Check if the held sign type is allowed
        if (!SignValidationUtil.isSignTypeAllowed(heldItem.getType().name())) {
            player.sendMessage(Lang.SIGN_TYPE_NOT_ALLOWED_COPY.getWithPrefix());
            return;
        }

        // Determine sign type category for held item and clicked block.
        String heldType = heldItem.getType().name();
        String clickedType = clickedBlock.getType().name();
        // A sign is considered hanging if its type name contains "HANGING_SIGN".
        boolean heldHanging = heldType.contains("HANGING_SIGN");
        boolean clickedHanging = clickedType.contains("HANGING_SIGN");
        
        // Check if player has permission to copy from this sign type
        if (!Permissions.canCopySignType(player, clickedHanging)) {
            player.sendMessage(Lang.NO_PERMISSION_COPY_SIGN_TYPE.formatWithPrefix("%type%", clickedHanging ? "hanging" : "regular"));
            return;
        }
        
        // Check if player has permission to paste to this sign type
        if (!Permissions.canPasteSignType(player, heldHanging)) {
            player.sendMessage(Lang.NO_PERMISSION_PASTE_SIGN_TYPE.formatWithPrefix("%type%", heldHanging ? "hanging" : "regular"));
            return;
        }
        
        // If the sign types do not match, send an error message and cancel the copy.
        if (heldHanging != clickedHanging) {
            player.sendMessage(Lang.SIGN_TYPE_MISMATCH.formatWithPrefix(
                "%held%", heldHanging ? Lang.HANGING_SIGN.get() : Lang.REGULAR_SIGN.get(),
                "%target%", clickedHanging ? Lang.HANGING_SIGN.get() : Lang.REGULAR_SIGN.get()));
            return;
        }
        
        // Check WorldGuard protection if enabled
        if (!CopySign.getInstance().getWorldGuardIntegration().canCopySign(player, clickedBlock.getLocation())) {
            player.sendMessage(Lang.WORLDGUARD_COPY_DENIED.getWithPrefix());
            return;
        }

        Sign sign = (Sign) clickedBlock.getState();
        // Retrieve side colors using the new API.
        DyeColor frontColor = sign.getSide(org.bukkit.block.sign.Side.FRONT).getColor();
        DyeColor backColor = sign.getSide(org.bukkit.block.sign.Side.BACK).getColor();
        
        // If the player lacks permission to copy color, don't store color information
        boolean copyColors = Permissions.canCopyColor(player) && CopySign.getInstance().getConfigManager().isCopyColorsEnabled();
        if (!copyColors) {
            frontColor = null;
            backColor = null;
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

        // Copy the per-side glow states from the sign using enhanced version-compatible methods
        boolean frontGlowing = VersionCompatibility.isSignGlowingFront(sign);
        boolean backGlowing = VersionCompatibility.isSignGlowingBack(sign);
        boolean legacyGlowing = frontGlowing || backGlowing; // Legacy compatibility

        // If the player lacks permission to copy glow, disable it
        if (!Permissions.canCopyGlow(player) || !CopySign.getInstance().getConfigManager().isCopyGlowEnabled()) {
            frontGlowing = false;
            backGlowing = false;
            legacyGlowing = false;
        }

        // Use NBT-API to store the copied sign text and side colors onto the held sign item.
        try {
            // Validate sign text before storing in NBT
            String frontTextStr = frontText.toString();
            String backTextStr = backText.toString();
            if (!NBTValidationUtil.validateNBTData(frontTextStr) || !NBTValidationUtil.validateNBTData(backTextStr)) {
                player.sendMessage(Lang.PREFIX.get() + "§cSign text too large to copy");
                return;
            }
            
            NBTItem nbtItem = new NBTItem(heldItem);
            nbtItem.setString("copiedSignFront", frontTextStr);
            nbtItem.setString("copiedSignBack", backTextStr);
            // Only store color information if colors were copied
            if (frontColor != null) {
                nbtItem.setString("copiedSignFrontColor", frontColor.name());
            }
            if (backColor != null) {
                nbtItem.setString("copiedSignBackColor", backColor.name());
            }
            // Store per-side glow states with legacy compatibility
            nbtItem.setBoolean("frontGlowing", frontGlowing);
            nbtItem.setBoolean("backGlowing", backGlowing);
            nbtItem.setBoolean("signGlowing", legacyGlowing); // Legacy compatibility
            // Store the sign type so that later paste or change events can verify the type if desired.
            nbtItem.setString("signType", clickedHanging ? "hanging" : "regular");

            // Get the updated item from NBTItem.
            ItemStack updatedItem = nbtItem.getItem();

            // Update item meta with premium lore showing ONLY content identifier (no physical item duplication)
            ItemMeta meta = updatedItem.getItemMeta();
            if (meta != null) {
                String[] frontLines = frontText.toString().split("\n");
                String[] backLines = backText.toString().split("\n");
                
                // Use simple SignLoreBuilder with ONLY content identifier - Minecraft handles physical item name
                List<String> lore = SignLoreBuilder.buildPremiumSignLore(
                    "Copied Sign",
                    frontLines, 
                    backLines, 
                    frontColor != null ? frontColor.name() : null, 
                    backColor != null ? backColor.name() : null, 
                    frontGlowing,
                    backGlowing,
                    clickedHanging ? "hanging" : "regular",
                    "Copied"
                );
                
                meta.setLore(lore);
                updatedItem.setItemMeta(meta);
            }
            
            // Replace the held sign item with the updated item (with lore metadata).
            player.getInventory().setItemInMainHand(updatedItem);

            player.sendMessage(Lang.SIGN_COPIED.getWithPrefix());
            
            // Play copy sound
            CopySign.getInstance().getSoundManager().playCopySound(player);
            
            // Record metrics
            CopySign.getInstance().getMetricsManager().recordCopyOperation(player);
            
            // Send enhanced mixed glow state warning if applicable
            if (frontGlowing != backGlowing && (Permissions.canCopyGlow(player) && CopySign.getInstance().getConfigManager().isCopyGlowEnabled())) {
                player.sendMessage(DesignConstants.createMixedGlowWarning());
            }
            
            // Record command usage
            CopySign.getCooldownManager().recordCommandUse(player, "copy");
            
        } catch (Exception e) {
            ErrorHandler.handleNBTError("copying sign data", player, e);
        }
    }
    

} 