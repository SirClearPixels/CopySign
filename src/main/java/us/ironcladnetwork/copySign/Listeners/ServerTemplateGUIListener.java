package us.ironcladnetwork.copySign.Listeners;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import us.ironcladnetwork.copySign.GUI.ServerTemplateGUI;
import us.ironcladnetwork.copySign.Lang.Lang;
import us.ironcladnetwork.copySign.Util.SavedSignData;
import us.ironcladnetwork.copySign.Util.ServerTemplateManager;
import us.ironcladnetwork.copySign.Util.DesignConstants;
import us.ironcladnetwork.copySign.Util.SignLoreBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles GUI interactions for the server template system.
 */
public class ServerTemplateGUIListener implements Listener {
    
    private final ServerTemplateManager templateManager;
    private final Map<UUID, String> pendingTemplateNames = new HashMap<>();
    
    public ServerTemplateGUIListener(ServerTemplateManager templateManager) {
        this.templateManager = templateManager;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Check if it's a server template GUI
        if (!title.contains("Sign Templates")) return;
        
        event.setCancelled(true);
        
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        
        ItemStack clickedItem = event.getCurrentItem();
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;
        
        String displayName = meta.getDisplayName();
        boolean isAdmin = player.hasPermission("copysign.admin");
        
        // Handle close button
        if (clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        
        // Handle create new template button (admin only)
        if (clickedItem.getType() == Material.EMERALD && isAdmin) {
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (heldItem == null || heldItem.getType() == Material.AIR || !heldItem.getType().name().endsWith("_SIGN")) {
                player.sendMessage(Lang.TEMPLATE_MUST_HOLD_SIGN_DATA.getWithPrefix());
                return;
            }
            
            NBTItem nbtItem = new NBTItem(heldItem);
            if (!nbtItem.hasTag("copiedSignFront") || !nbtItem.hasTag("copiedSignBack")) {
                player.sendMessage(Lang.TEMPLATE_NO_DATA_ERROR.getWithPrefix());
                return;
            }
            
            // Ask for template name
            player.closeInventory();
            player.sendMessage(Lang.TEMPLATE_CREATION_PROMPT.getWithPrefix());
            player.sendMessage(Lang.TEMPLATE_CREATION_CANCEL_HINT.getWithPrefix());
            pendingTemplateNames.put(player.getUniqueId(), "CREATE");
            return;
        }
        
        // Handle template items
        if (clickedItem.getType().name().endsWith("_SIGN")) {
            // Extract template name from lore (should be at index 1 after the top separator)
            List<String> lore = meta.getLore();
            String templateName = null;
            if (lore != null && lore.size() > 1) {
                // The name is on line index 1, after SEPARATOR_TOP
                String nameLine = lore.get(1);
                templateName = ChatColor.stripColor(nameLine).trim();
            }
            
            if (event.getClick() == ClickType.LEFT) {
                if (templateName == null) {
                    player.sendMessage(Lang.TEMPLATE_NAME_NOT_IDENTIFIED.getWithPrefix());
                    return;
                }
                
                // Load template
                SavedSignData templateData = templateManager.getTemplate(templateName);
                if (templateData == null) {
                    player.sendMessage(Lang.TEMPLATE_NOT_FOUND.getWithPrefix());
                    return;
                }
                
                ItemStack heldItem = player.getInventory().getItemInMainHand();
                if (heldItem == null || heldItem.getType() == Material.AIR || !heldItem.getType().name().endsWith("_SIGN")) {
                    player.sendMessage(Lang.MUST_HOLD_SIGN.getWithPrefix());
                    return;
                }
                
                // Check sign type compatibility
                boolean heldHanging = heldItem.getType().name().contains("HANGING_SIGN");
                boolean templateHanging = templateData.getSignType().equalsIgnoreCase("hanging");
                if (heldHanging != templateHanging) {
                    player.sendMessage(Lang.SIGN_TYPE_MISMATCH.formatWithPrefix(
                        "%held%", heldHanging ? Lang.HANGING_SIGN.get() : Lang.REGULAR_SIGN.get(),
                        "%target%", templateHanging ? Lang.HANGING_SIGN.get() : Lang.REGULAR_SIGN.get()));
                    return;
                }
                
                // Apply template data to held sign with enhanced per-side glow support
                NBTItem nbtItem = new NBTItem(heldItem);
                
                // Clear any existing NBT data first
                nbtItem.removeKey("copiedSignFront");
                nbtItem.removeKey("copiedSignBack");
                nbtItem.removeKey("copiedSignFrontColor");
                nbtItem.removeKey("copiedSignBackColor");
                nbtItem.removeKey("signGlowing");
                nbtItem.removeKey("frontGlowing");
                nbtItem.removeKey("backGlowing");
                nbtItem.removeKey("signType");
                
                nbtItem.setString("copiedSignFront", String.join("\n", templateData.getFront()));
                nbtItem.setString("copiedSignBack", String.join("\n", templateData.getBack()));
                nbtItem.setString("copiedSignFrontColor", templateData.getFrontColor());
                nbtItem.setString("copiedSignBackColor", templateData.getBackColor());
                
                // Store per-side glow states
                nbtItem.setBoolean("frontGlowing", templateData.isFrontGlowing());
                nbtItem.setBoolean("backGlowing", templateData.isBackGlowing());
                nbtItem.setBoolean("signGlowing", templateData.isGlowing()); // Legacy compatibility
                
                nbtItem.setString("signType", templateData.getSignType());
                
                // Update item with premium lore showing ONLY template name (no physical item duplication)
                ItemStack updatedItem = nbtItem.getItem();
                ItemMeta updatedMeta = updatedItem.getItemMeta();
                if (updatedMeta != null) {
                    // Use simple SignLoreBuilder with ONLY template name - Minecraft handles physical item name
                    List<String> updatedLore = SignLoreBuilder.buildPremiumSignLore(
                        templateName,
                        templateData.getFront(),
                        templateData.getBack(),
                        templateData.getFrontColor(),
                        templateData.getBackColor(),
                        templateData.isFrontGlowing(),
                        templateData.isBackGlowing(),
                        templateData.getSignType(),
                        "Server Template"
                    );
                    
                    updatedMeta.setLore(updatedLore);
                    updatedItem.setItemMeta(updatedMeta);
                }
                
                player.getInventory().setItemInMainHand(updatedItem);
                player.closeInventory();
                player.sendMessage(Lang.TEMPLATE_LOADED_TO_SIGN.formatWithPrefix("%name%", templateName));
                
                // Send enhanced mixed glow state warning if applicable
                if (templateData.hasMixedGlowStates()) {
                    player.sendMessage(DesignConstants.createMixedGlowWarning());
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (!pendingTemplateNames.containsKey(playerId)) return;
        
        event.setCancelled(true);
        String message = event.getMessage();
        
        // Remove from pending
        pendingTemplateNames.remove(playerId);
        
        // Handle cancel
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(Lang.TEMPLATE_CREATE_CANCELLED.getWithPrefix());
            return;
        }
        
        // Validate name
        if (message.contains(" ") || message.length() > 16) {
            player.sendMessage(Lang.TEMPLATE_NAME_INVALID.getWithPrefix());
            return;
        }
        
        // Check if template already exists
        if (templateManager.getTemplate(message) != null) {
            player.sendMessage(Lang.TEMPLATE_NAME_EXISTS.getWithPrefix());
            return;
        }
        
        // Save the template
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (templateManager.saveTemplate(player, message, heldItem)) {
            player.sendMessage(Lang.TEMPLATE_CREATE_SUCCESS.getWithPrefix());
        }
    }
    
} 