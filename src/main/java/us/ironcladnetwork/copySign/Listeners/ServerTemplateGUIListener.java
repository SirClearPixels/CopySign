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
import us.ironcladnetwork.copySign.CopySign;
import us.ironcladnetwork.copySign.GUI.ServerTemplateGUI;
import us.ironcladnetwork.copySign.Lang.Lang;
import us.ironcladnetwork.copySign.Util.SavedSignData;
import us.ironcladnetwork.copySign.Util.ServerTemplateManager;

import java.util.ArrayList;
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
        if (!title.contains("Server Templates")) return;
        
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
                player.sendMessage(Lang.PREFIX.get() + "&cYou must be holding a sign with copied data!");
                return;
            }
            
            NBTItem nbtItem = new NBTItem(heldItem);
            if (!nbtItem.hasTag("copiedSignFront") || !nbtItem.hasTag("copiedSignBack")) {
                player.sendMessage(Lang.PREFIX.get() + "&cThe held sign has no copied data!");
                return;
            }
            
            // Ask for template name
            player.closeInventory();
            player.sendMessage(Lang.PREFIX.get() + "&aPlease type the name for the new server template in chat:");
            player.sendMessage(Lang.PREFIX.get() + "&7Type 'cancel' to cancel.");
            pendingTemplateNames.put(player.getUniqueId(), "CREATE");
            return;
        }
        
        // Handle template items
        if (clickedItem.getType().name().endsWith("_SIGN")) {
            String templateName = ChatColor.stripColor(displayName);
            
            if (event.getClick() == ClickType.RIGHT && isAdmin) {
                // Delete template
                if (templateManager.deleteTemplate(player, templateName)) {
                    // Refresh GUI
                    Map<String, SavedSignData> templates = templateManager.getAllTemplates();
                    ServerTemplateGUI.open(player, templates, true);
                }
            } else if (event.getClick() == ClickType.LEFT) {
                // Load template
                SavedSignData templateData = templateManager.getTemplate(templateName);
                if (templateData == null) {
                    player.sendMessage(Lang.PREFIX.get() + "&cTemplate not found!");
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
                
                // Apply template data to held sign
                NBTItem nbtItem = new NBTItem(heldItem);
                nbtItem.setString("copiedSignFront", String.join("\n", templateData.getFront()));
                nbtItem.setString("copiedSignBack", String.join("\n", templateData.getBack()));
                nbtItem.setString("copiedSignFrontColor", templateData.getFrontColor());
                nbtItem.setString("copiedSignBackColor", templateData.getBackColor());
                nbtItem.setBoolean("signGlowing", templateData.isGlowing());
                nbtItem.setString("signType", templateData.getSignType());
                
                // Update item with lore
                ItemStack updatedItem = nbtItem.getItem();
                ItemMeta updatedMeta = updatedItem.getItemMeta();
                if (updatedMeta != null) {
                    List<String> lore = new ArrayList<>();
                    lore.add("§f§l[§6§lServer Template§f§l]");
                    lore.add("§eTemplate: §f" + templateName);
                    
                    // Add preview of content
                    String[] frontLines = templateData.getFront();
                    boolean hasFrontData = false;
                    for (int i = 0; i < frontLines.length; i++) {
                        if (!frontLines[i].isEmpty()) {
                            if (!hasFrontData) {
                                lore.add("§f§lFront:");
                                hasFrontData = true;
                            }
                            lore.add("§f§lLine " + (i + 1) + ": §f\"§b" + frontLines[i] + "§f\"");
                        }
                    }
                    
                    String[] backLines = templateData.getBack();
                    boolean hasBackData = false;
                    for (int i = 0; i < backLines.length; i++) {
                        if (!backLines[i].isEmpty()) {
                            if (!hasBackData) {
                                lore.add("§f§lBack:");
                                hasBackData = true;
                            }
                            lore.add("§f§lLine " + (i + 1) + ": §f\"§b" + backLines[i] + "§f\"");
                        }
                    }
                    
                    lore.add("§e§lGlowing: " + (templateData.isGlowing() ? "§aTrue" : "§cFalse"));
                    
                    updatedMeta.setLore(lore);
                    updatedItem.setItemMeta(updatedMeta);
                }
                
                player.getInventory().setItemInMainHand(updatedItem);
                player.closeInventory();
                player.sendMessage(Lang.PREFIX.get() + "&aServer template '" + templateName + "' loaded to your held sign!");
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
            player.sendMessage(Lang.PREFIX.get() + "&cTemplate creation cancelled.");
            return;
        }
        
        // Validate name
        if (message.contains(" ") || message.length() > 16) {
            player.sendMessage(Lang.PREFIX.get() + "&cTemplate name must be one word and less than 16 characters!");
            return;
        }
        
        // Check if template already exists
        if (templateManager.getTemplate(message) != null) {
            player.sendMessage(Lang.PREFIX.get() + "&cA server template with that name already exists!");
            return;
        }
        
        // Save the template
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (templateManager.saveTemplate(player, message, heldItem)) {
            player.sendMessage(Lang.PREFIX.get() + "&aServer template created successfully!");
        }
    }
} 