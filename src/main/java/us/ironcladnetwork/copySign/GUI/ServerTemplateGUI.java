package us.ironcladnetwork.copySign.GUI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import us.ironcladnetwork.copySign.Util.SavedSignData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI utility class for displaying and managing server-wide sign templates.
 * <p>
 * This class provides an inventory-based interface for players to browse and load
 * server templates, and for administrators to manage them. The GUI displays templates
 * in a grid format with the following features:
 * <ul>
 *   <li>Template preview with front/back text content</li>
 *   <li>Sign type and glow state indicators</li>
 *   <li>Different interaction modes for players vs. administrators</li>
 *   <li>Template creation controls for administrators</li>
 *   <li>Responsive sizing based on template count</li>
 * </ul>
 * <p>
 * The GUI differentiates between regular players and administrators:
 * <ul>
 *   <li><strong>Players:</strong> Can only view and load templates</li>
 *   <li><strong>Administrators:</strong> Can load, delete, and create new templates</li>
 * </ul>
 * <p>
 * Template items display a preview of the sign content, including both front and back
 * text (if present), along with metadata such as glow state and sign type.
 * 
 * @author IroncladNetwork
 * @since 2.0.0
 * @see ServerTemplateGUIListener
 * @see us.ironcladnetwork.copySign.Util.ServerTemplateManager
 */
public class ServerTemplateGUI {

    /**
     * Opens the server template GUI for a player.
     *
     * @param player The player to open the GUI for.
     * @param templates Map of template names to their data.
     * @param canEdit Whether the player can edit templates (has admin permission).
     */
    public static void open(Player player, Map<String, SavedSignData> templates, boolean canEdit) {
        // Create inventory with appropriate title
        String title = canEdit ? "§6§lServer Templates §7(Admin)" : "§b§lServer Templates";
        int size = Math.min(54, ((templates.size() + 8) / 9) * 9); // Round up to nearest multiple of 9
        if (size < 27) size = 27; // Minimum 3 rows
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // Add template items
        int slot = 0;
        for (Map.Entry<String, SavedSignData> entry : templates.entrySet()) {
            if (slot >= size - 9) break; // Leave last row for controls
            
            String templateName = entry.getKey();
            SavedSignData data = entry.getValue();
            
            // Create sign item for the template
            ItemStack signItem = createTemplateItem(templateName, data, canEdit);
            gui.setItem(slot, signItem);
            slot++;
        }
        
        // Add control buttons in the last row
        if (canEdit) {
            // Add "Create New Template" button for admins
            ItemStack createButton = new ItemStack(Material.EMERALD);
            ItemMeta createMeta = createButton.getItemMeta();
            if (createMeta != null) {
                createMeta.setDisplayName("§a§lCreate New Template");
            List<String> createLore = new ArrayList<>();
            createLore.add("§7Hold a sign with copied data");
            createLore.add("§7and click to save as template");
                createMeta.setLore(createLore);
                createButton.setItemMeta(createMeta);
            }
            gui.setItem(size - 5, createButton);
        }
        
        // Add close button
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("§c§lClose");
            closeButton.setItemMeta(closeMeta);
        }
        gui.setItem(size - 1, closeButton);
        
        // Add info item
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§e§lServer Templates");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7These are server-wide templates");
        infoLore.add("§7available to all players.");
        if (canEdit) {
            infoLore.add("");
            infoLore.add("§6Admin Controls:");
            infoLore.add("§e• Left-click§7 to load template");
            infoLore.add("§c• Right-click§7 to delete template");
        } else {
            infoLore.add("");
            infoLore.add("§e• Click§7 to load a template");
        }
            infoMeta.setLore(infoLore);
            infoItem.setItemMeta(infoMeta);
        }
        gui.setItem(size - 9, infoItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Creates an item representing a server template.
     */
    private static ItemStack createTemplateItem(String name, SavedSignData data, boolean canEdit) {
        // Determine sign material based on type
        Material signMaterial;
        if (data.getSignType().equalsIgnoreCase("hanging")) {
            signMaterial = Material.OAK_HANGING_SIGN;
        } else {
            signMaterial = Material.OAK_SIGN;
        }
        
        ItemStack item = new ItemStack(signMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item; // Fallback if meta is null
        
        // Set display name
        meta.setDisplayName("§6§l" + name);
        
        // Build lore
        List<String> lore = new ArrayList<>();
        lore.add("§8§m                    ");
        
        // Add front side preview
        String[] frontLines = data.getFront();
        boolean hasFrontText = false;
        for (String line : frontLines) {
            if (!line.isEmpty()) {
                hasFrontText = true;
                break;
            }
        }
        
        if (hasFrontText) {
            lore.add("§f§lFront Side:");
            for (int i = 0; i < frontLines.length && i < 4; i++) {
                if (!frontLines[i].isEmpty()) {
                    lore.add("§7" + ChatColor.stripColor(frontLines[i]));
                }
            }
        }
        
        // Add back side preview
        String[] backLines = data.getBack();
        boolean hasBackText = false;
        for (String line : backLines) {
            if (!line.isEmpty()) {
                hasBackText = true;
                break;
            }
        }
        
        if (hasBackText) {
            if (hasFrontText) lore.add("");
            lore.add("§f§lBack Side:");
            for (int i = 0; i < backLines.length && i < 4; i++) {
                if (!backLines[i].isEmpty()) {
                    lore.add("§7" + ChatColor.stripColor(backLines[i]));
                }
            }
        }
        
        // Add properties
        lore.add("");
        lore.add("§eGlowing: " + (data.isGlowing() ? "§aYes" : "§cNo"));
        lore.add("§eType: §f" + (data.getSignType().equalsIgnoreCase("hanging") ? "Hanging Sign" : "Regular Sign"));
        
        // Add instructions
        lore.add("§8§m                    ");
        if (canEdit) {
            lore.add("§e• Left-click§7 to load");
            lore.add("§c• Right-click§7 to delete");
        } else {
            lore.add("§e• Click§7 to load template");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
} 