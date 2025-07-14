package us.ironcladnetwork.copySign.Listeners;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import us.ironcladnetwork.copySign.Lang.Lang;
import us.ironcladnetwork.copySign.Util.SignLibraryGUI;
import us.ironcladnetwork.copySign.Util.SignLibraryManager;
import us.ironcladnetwork.copySign.Util.SavedSignData;
import us.ironcladnetwork.copySign.Util.DesignConstants;
import us.ironcladnetwork.copySign.Util.SignLoreBuilder;

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
        if (event.getView().getTitle() == null || !event.getView().getTitle().contains("Sign Library"))
            return;

        // Cancel all clicks to prevent the player from taking items.
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR || !clickedItem.hasItemMeta())
            return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return; // Prevent NPE
        String displayName = meta.getDisplayName();
        Player player = (Player) event.getWhoClicked();

        // Exit button: close the inventory.
        if (clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        // Navigation buttons.
        if (clickedItem.getType() == Material.ARROW) {
            // Parse current page from the inventory title.
            int currentPage = parseCurrentPage(event.getView().getTitle());
            if (displayName.contains("Previous Page")) {
                int newPage = currentPage - 1; // currentPage is 1-indexed
                openPageForPlayer(player, newPage);
            } else if (displayName.contains("Next Page")) {
                int newPage = currentPage + 1;
                openPageForPlayer(player, newPage);
            }
            return;
        }

        // Handle a click on a sign entry item.
        if (clickedItem.getType().name().endsWith("_SIGN")) {
            if (event.getClick() == ClickType.LEFT) {
                // Extract sign name from lore (should be at index 1 after the top separator)
                List<String> lore = meta.getLore();
                String signName = null;
                if (lore != null && lore.size() > 1) {
                    // The name is on line index 1, after SEPARATOR_TOP
                    String nameLine = lore.get(1);
                    signName = ChatColor.stripColor(nameLine).trim();
                }
                
                if (signName == null) {
                    player.sendMessage(Lang.SIGN_NAME_NOT_IDENTIFIED.getWithPrefix());
                    return;
                }
                
                // Load sign data
                SavedSignData signData = signLibraryManager.getSign(player, signName);
                if (signData == null) {
                    player.sendMessage(Lang.SIGN_NOT_FOUND.getWithPrefix());
                    return;
                }
                
                ItemStack heldItem = player.getInventory().getItemInMainHand();
                if (heldItem == null || heldItem.getType() == Material.AIR || !heldItem.getType().name().endsWith("_SIGN")) {
                    player.sendMessage(Lang.MUST_HOLD_SIGN.getWithPrefix());
                    return;
                }
                
                // Check sign type compatibility
                boolean heldHanging = heldItem.getType().name().contains("HANGING_SIGN");
                boolean savedHanging = signData.getSignType().equalsIgnoreCase("hanging");
                if (heldHanging != savedHanging) {
                    player.sendMessage(Lang.SIGN_TYPE_MISMATCH.formatWithPrefix(
                        "%held%", heldHanging ? Lang.HANGING_SIGN.get() : Lang.REGULAR_SIGN.get(),
                        "%target%", savedHanging ? Lang.HANGING_SIGN.get() : Lang.REGULAR_SIGN.get()));
                    return;
                }
                
                // Apply sign data to held sign
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
                
                nbtItem.setString("copiedSignFront", String.join("\n", signData.getFront()));
                nbtItem.setString("copiedSignBack", String.join("\n", signData.getBack()));
                nbtItem.setString("copiedSignFrontColor", signData.getFrontColor());
                nbtItem.setString("copiedSignBackColor", signData.getBackColor());
                
                // Store per-side glow states
                nbtItem.setBoolean("frontGlowing", signData.isFrontGlowing());
                nbtItem.setBoolean("backGlowing", signData.isBackGlowing());
                nbtItem.setBoolean("signGlowing", signData.isGlowing()); // Legacy compatibility
                
                nbtItem.setString("signType", signData.getSignType());
                
                // Update item with premium lore
                ItemStack updatedItem = nbtItem.getItem();
                ItemMeta updatedMeta = updatedItem.getItemMeta();
                if (updatedMeta != null) {
                    List<String> newLore = SignLoreBuilder.buildPremiumSignLore(
                        signName,
                        signData.getFront(),
                        signData.getBack(),
                        signData.getFrontColor(),
                        signData.getBackColor(),
                        signData.isFrontGlowing(),
                        signData.isBackGlowing(),
                        signData.getSignType(),
                        "Library"
                    );
                    
                    updatedMeta.setLore(newLore);
                    updatedItem.setItemMeta(updatedMeta);
                }
                
                player.getInventory().setItemInMainHand(updatedItem);
                player.closeInventory();
                player.sendMessage(Lang.SIGN_LOADED_TO_HELD.formatWithPrefix("%name%", signName));
                
                // Send enhanced mixed glow state warning if applicable
                if (signData.hasMixedGlowStates()) {
                    player.sendMessage(DesignConstants.createMixedGlowWarning());
                }
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
