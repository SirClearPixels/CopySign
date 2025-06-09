package us.ironcladnetwork.copySign.Listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import us.ironcladnetwork.copySign.Util.SignLibraryGUI;
import us.ironcladnetwork.copySign.Util.SignLibraryManager;
import us.ironcladnetwork.copySign.Util.SavedSignData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Event listener for handling player interactions within the Sign Library GUI.
 * <p>
 * This listener manages all {@link InventoryClickEvent}s that occur within the Sign Library
 * interface created by {@link SignLibraryGUI}. It provides the following functionality:
 * <ul>
 *   <li>Prevents item removal by cancelling all click events</li>
 *   <li>Handles navigation between pages (Previous/Next buttons)</li>
 *   <li>Processes sign selection and provides load instructions</li>
 *   <li>Manages the Exit button to close the GUI</li>
 * </ul>
 * <p>
 * The listener identifies Sign Library GUIs by checking if the inventory title starts
 * with "Sign Library". It parses page information from the title format:
 * "Sign Library (Page X/Y)" where X is the current page and Y is the total pages.
 * 
 * @author IroncladNetwork
 * @since 2.0.0
 * @see SignLibraryGUI
 * @see SignLibraryManager
 */
public class SignLibraryGUIListener implements Listener {

    private final SignLibraryManager signLibraryManager;

    /**
     * Constructs the listener with a reference to the SignLibraryManager.
     *
     * @param signLibraryManager The manager that provides access to saved sign data.
     */
    public SignLibraryGUIListener(SignLibraryManager signLibraryManager) {
        this.signLibraryManager = signLibraryManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if the inventory view is the Sign Library GUI by its title.
        if (event.getView().getTitle() == null || !event.getView().getTitle().startsWith("Sign Library"))
            return;

        // Cancel all clicks to prevent the player from taking items.
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta())
            return;

        ItemMeta meta = clickedItem.getItemMeta();
        String displayName = meta.getDisplayName();
        Player player = (Player) event.getWhoClicked();

        // Exit button: close the inventory.
        if (clickedItem.getType() == Material.BARRIER && "Exit".equals(displayName)) {
            player.closeInventory();
            return;
        }

        // Navigation buttons.
        if (clickedItem.getType() == Material.ARROW) {
            // Parse current page from the inventory title.
            int currentPage = parseCurrentPage(event.getView().getTitle());
            if ("Previous Page".equals(displayName)) {
                int newPage = currentPage - 1; // currentPage is 1-indexed
                openPageForPlayer(player, newPage);
            } else if ("Next Page".equals(displayName)) {
                int newPage = currentPage + 1;
                openPageForPlayer(player, newPage);
            }
            return;
        }

        // Handle a click on a sign entry item.
        if (clickedItem.getType() == Material.OAK_SIGN || clickedItem.getType() == Material.OAK_HANGING_SIGN) {
            // Expect the display name format "Sign: <name>".
            if (displayName.startsWith("Sign: ")) {
                String signName = displayName.substring(6); // Remove "Sign: " prefix.
                // Inform the player how to load the sign.
                player.sendMessage("To load sign \"" + signName + "\", use /copysign load " + signName);
            }
        }
    }

    /**
     * Parses the current page number from the inventory title.
     * Expected format: "Sign Library (Page X/Y)" where X is the current page (1-indexed).
     *
     * @param title The inventory title.
     * @return The current page number (1-indexed), or 1 on parsing errors.
     */
    private int parseCurrentPage(String title) {
        try {
            int start = title.indexOf("Page ") + 5;
            int end = title.indexOf("/");
            String pageStr = title.substring(start, end).trim();
            return Integer.parseInt(pageStr);
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * Reopens the Sign Library GUI for the player on the specified page.
     *
     * @param player       The player.
     * @param newPageOneIndexed The new page number (1-indexed).
     */
    private void openPageForPlayer(Player player, int newPageOneIndexed) {
        int newPageIndex = newPageOneIndexed - 1; // Convert to 0-indexed.
        Map<String, SavedSignData> savedSigns = signLibraryManager.getAllSigns(player);
        List<Entry<String, SavedSignData>> entries = new ArrayList<>(savedSigns.entrySet());
        SignLibraryGUI.openPage(player, entries, newPageIndex);
    }
}
