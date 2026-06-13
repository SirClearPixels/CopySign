package us.ironcladnetwork.copySign.GUI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
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

    /** Shared MiniMessage instance for authored GUI-chrome labels (D-06). */
    private static final MiniMessage MM = MiniMessage.miniMessage();
    /** Legacy serializer for §-coded lines emitted by SignLoreBuilder / DesignConstants. */
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    /**
     * Converts a §-coded legacy line into a non-italic Component (D-06).
     * <p>
     * Lines from {@link SignLoreBuilder} and {@link DesignConstants} are §-coded strings —
     * including player/admin-supplied template names — so they are legacy-serialized (never
     * MiniMessage parsed), which prevents any MM-tag injection through a template name (T-06-05).
     *
     * @param legacyLine a §-coded string
     * @return the rendered Component with ITALIC explicitly disabled
     */
    private static Component legacyLine(String legacyLine) {
        return LEGACY_SECTION.deserialize(legacyLine).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Builds an authored chrome label from a MiniMessage string with ITALIC disabled (D-06).
     *
     * @param miniMessage a developer-authored MiniMessage string
     * @return the rendered Component with ITALIC explicitly disabled
     */
    private static Component label(String miniMessage) {
        return MM.deserialize(miniMessage).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Opens the server template GUI for a player.
     *
     * @param player The player to open the GUI for.
     * @param templates Map of template names to their data.
     * @param canEdit Whether the player can edit templates (has admin permission).
     */
    public static void open(Player player, Map<String, SavedSignData> templates, boolean canEdit) {
        // Create inventory with premium title using design standards (Component overload, D-06)
        Component title = label(canEdit ? "<bold>Sign Templates (Admin)"
                                        : "<bold>Sign Templates");
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
                createMeta.displayName(label("<green><bold>Create New Template"));
                createMeta.lore(List.of(
                    label("<gray>Hold a sign with copied data"),
                    label("<gray>and click to save as template")));
                createButton.setItemMeta(createMeta);
            }
            gui.setItem(size - 5, createButton);
        }
        
        // Add premium close button
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(label("<red>Close Templates"));
            closeMeta.lore(List.of(label("<gray>Return to game")));
            closeButton.setItemMeta(closeMeta);
        }
        gui.setItem(size - 1, closeButton);
        
        // Add premium info item
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(label("<yellow><bold>Server Templates"));
            infoMeta.lore(List.of(
                legacyLine(DesignConstants.SUPPORTING + "These are server-wide templates"),
                legacyLine(DesignConstants.SUPPORTING + "available to all players."),
                legacyLine(""),
                legacyLine(DesignConstants.INFORMATION + "• Click " + DesignConstants.SUPPORTING + "to load a template")));
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

        // Convert the §-coded List<String> to non-italic Components at the edge (D-06).
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(legacyLine(line));
        }
        meta.lore(loreComponents);
        item.setItemMeta(meta);
        
        return item;
    }
    
} 