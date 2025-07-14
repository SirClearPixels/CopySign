package us.ironcladnetwork.copySign.GUI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import us.ironcladnetwork.copySign.Util.SavedSignData;
import us.ironcladnetwork.copySign.Util.DesignConstants;
import us.ironcladnetwork.copySign.Util.SignLoreBuilder;

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
        // Create inventory with premium title using design standards
        String title = canEdit ? "§lSign Templates (Admin)" 
                              : "§lSign Templates";
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
            // Add premium "Create New Template" button for admins
            ItemStack createButton = new ItemStack(Material.EMERALD);
            ItemMeta createMeta = createButton.getItemMeta();
            if (createMeta != null) {
                createMeta.setDisplayName(DesignConstants.SUCCESS_ACTIVE + "§lCreate New Template");
                List<String> createLore = new ArrayList<>();
                createLore.add(DesignConstants.SUPPORTING + "Hold a sign with copied data");
                createLore.add(DesignConstants.SUPPORTING + "and click to save as template");
                createMeta.setLore(createLore);
                createButton.setItemMeta(createMeta);
            }
            gui.setItem(size - 5, createButton);
        }
        
        // Add premium close button
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(DesignConstants.WARNING_INACTIVE + "Close Templates");
            List<String> closeLore = new ArrayList<>();
            closeLore.add(DesignConstants.SUPPORTING + "Return to game");
            closeMeta.setLore(closeLore);
            closeButton.setItemMeta(closeMeta);
        }
        gui.setItem(size - 1, closeButton);
        
        // Add premium info item
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(DesignConstants.LABEL_PROPERTY + "Server Templates");
            List<String> infoLore = new ArrayList<>();
            infoLore.add(DesignConstants.SUPPORTING + "These are server-wide templates");
            infoLore.add(DesignConstants.SUPPORTING + "available to all players.");
            infoLore.add("");
            infoLore.add(DesignConstants.INFORMATION + "• Click " + DesignConstants.SUPPORTING + "to load a template");
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
        
        // Let Minecraft show natural item name, only add content identifier to lore
        List<String> lore = SignLoreBuilder.buildPremiumSignLore(
            name, // ONLY content identifier - no physical item name duplication
            data.getFront(),
            data.getBack(), 
            data.getFrontColor(),
            data.getBackColor(),
            data.isFrontGlowing(),
            data.isBackGlowing(),
            data.getSignType(),
            "Template"
        );
        
        // Add template-specific instructions
        lore.add("");
        lore.add(DesignConstants.INFORMATION + "• Click " + DesignConstants.SUPPORTING + "to load template");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
} 