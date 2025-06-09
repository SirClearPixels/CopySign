package us.ironcladnetwork.copySign.Util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * GUI utility class for displaying and managing the Sign Library interface.
 * <p>
 * This class provides a paginated inventory GUI that allows players to view, load, and delete
 * their saved signs. The GUI displays signs in a double chest format (54 slots) with:
 * <ul>
 *   <li>Top 45 slots: Display saved sign entries with detailed lore information</li>
 *   <li>Bottom row: Navigation controls (Previous/Next page buttons and Exit button)</li>
 * </ul>
 * <p>
 * Each sign entry shows:
 * <ul>
 *   <li>Sign name as the display name</li>
 *   <li>Front and back text content with line numbers</li>
 *   <li>Text colors for both sides</li>
 *   <li>Glowing state indicator</li>
 * </ul>
 * 
 * @author IroncladNetwork
 * @since 2.0.0
 * @see SignLibraryGUIListener
 * @see SignLibraryManager
 */
public class SignLibraryGUI {
    public static final int INVENTORY_SIZE = 54;
    public static final int ENTRIES_PER_PAGE = 45;

    /**
     * Opens the Sign Library GUI for the provided player.
     *
     * @param player     The player viewing their saved signs.
     * @param savedSigns A map of saved sign names to their corresponding data.
     */
    public static void open(Player player, Map<String, SavedSignData> savedSigns) {
        // Convert the map entries into a list for easy paging.
        List<Entry<String, SavedSignData>> entries = new ArrayList<>(savedSigns.entrySet());
        openPage(player, entries, 0);
    }

    /**
     * Opens a specific page within the Sign Library GUI.
     *
     * @param player  The player.
     * @param entries List of saved sign entries.
     * @param page    The page index (0-indexed).
     */
    public static void openPage(Player player, List<Entry<String, SavedSignData>> entries, int page) {
        int totalPages = (int) Math.ceil(entries.size() / (double) ENTRIES_PER_PAGE);
        if (totalPages < 1)
            totalPages = 1;
        // Title includes the current page (1-indexed) and total pages.
        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE, "Sign Library (Page " + (page + 1) + "/" + totalPages + ")");

        // Populate the top 45 slots with saved sign items.
        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, entries.size());
        for (int i = start; i < end; i++) {
            Entry<String, SavedSignData> entry = entries.get(i);
            String signName = entry.getKey();
            SavedSignData data = entry.getValue();
            // Choose material based on sign type.
            Material material = data.getSignType().equalsIgnoreCase("hanging")
                    ? Material.OAK_HANGING_SIGN : Material.OAK_SIGN;
            ItemStack signItem = new ItemStack(material);
            ItemMeta meta = signItem.getItemMeta();
            // Set the display name to include the saved sign's key.
            meta.setDisplayName("Sign: " + signName);
            List<String> lore = new ArrayList<>();
            lore.add("§f§l[§b§lCopied Sign§f§l]");

            // Handle front side
            String[] frontLines = data.getFront();
            boolean hasFrontData = false;
            for (int j = 0; j < frontLines.length; j++) {
                if (!frontLines[j].isEmpty()) {
                    if (!hasFrontData) {
                        lore.add("§f§lFront:");
                        lore.add("§f§lColor: " + data.getFrontColor());
                        hasFrontData = true;
                    }
                    lore.add("§f§lLine " + (j + 1) + ": §f\"§b" + frontLines[j] + "§f\"");
                }
            }

            // Handle back side
            String[] backLines = data.getBack();
            boolean hasBackData = false;
            for (int j = 0; j < backLines.length; j++) {
                if (!backLines[j].isEmpty()) {
                    if (!hasBackData) {
                        lore.add("§f§lBack:");
                        lore.add("§f§lColor: " + data.getBackColor());
                        hasBackData = true;
                    }
                    lore.add("§f§lLine " + (j + 1) + ": §f\"§b" + backLines[j] + "§f\"");
                }
            }

            // Always show glowing state
            lore.add("§e§lGlowing: " + (data.isGlowing() ? "§aTrue" : "§cFalse"));

            meta.setLore(lore);
            signItem.setItemMeta(meta);
            // Place item in the slot relative to the current page.
            inv.setItem(i - start, signItem);
        }

        // Set up navigation in the bottom row (slots 45 to 53).
        // Previous page button (if not on the first page) at slot 45.
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName("Previous Page");
            prev.setItemMeta(prevMeta);
            inv.setItem(45, prev);
        }
        // Next page button (if more entries exist) at slot 49.
        if ((page + 1) * ENTRIES_PER_PAGE < entries.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName("Next Page");
            next.setItemMeta(nextMeta);
            inv.setItem(49, next);
        }
        // Exit button always at slot 53.
        ItemStack exit = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exit.getItemMeta();
        exitMeta.setDisplayName("Exit");
        exit.setItemMeta(exitMeta);
        inv.setItem(53, exit);

        player.openInventory(inv);
    }
}
