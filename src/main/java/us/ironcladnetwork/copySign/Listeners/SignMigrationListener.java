package us.ironcladnetwork.copySign.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.ironcladnetwork.copySign.CopySign;
import us.ironcladnetwork.copySign.Util.SignItemStorage;

/**
 * Transparently migrates copied-sign items from the legacy NBT-API format to the
 * native {@link org.bukkit.persistence.PersistentDataContainer} format.
 * <p>
 * CopySign 2.4.0 switched item storage from NBT-API to PDC. Items copied with an
 * older version still carry NBT-API tags. {@link SignItemStorage#read} reads those tags
 * as a fallback so old items keep working, but that fallback does <em>not</em> persist
 * the upgrade — so this listener rewrites legacy items to PDC at two natural moments:
 * <ul>
 *   <li><b>Container open</b> ({@link InventoryOpenEvent}) — chests, barrels, shulkers,
 *       etc. Matches the player expectation that opening a chest "fixes" the signs
 *       inside.</li>
 *   <li><b>Player join</b> ({@link PlayerJoinEvent}) — scans the player's own inventory,
 *       covering hotbar/backpack items that never fire an inventory-open event.</li>
 * </ul>
 * Together these ensure stored items are migrated before the NBT-API softdepend is
 * eventually removed. Each scan is a no-op unless NBT-API is installed and an item
 * actually carries legacy data, so the cost is negligible.
 *
 * @since 2.4.0
 */
public class SignMigrationListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        migrateInventory(event.getInventory(), "inventory open");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        migrateInventory(event.getPlayer().getInventory(), "player join");
    }

    private void migrateInventory(Inventory inventory, String trigger) {
        if (inventory == null) {
            return;
        }

        int migrated = 0;
        int size = inventory.getSize();
        for (int slot = 0; slot < size; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null) {
                continue;
            }
            try {
                if (SignItemStorage.migrateIfLegacy(item)) {
                    // Write the migrated item back explicitly rather than relying on
                    // the slot holding a live reference.
                    inventory.setItem(slot, item);
                    migrated++;
                }
            } catch (Throwable t) {
                // Never let a migration hiccup interfere with the triggering action.
                CopySign.getInstance().getDebugLogger().debug(
                        "Failed to migrate a legacy sign item on " + trigger + ": " + t.getMessage());
            }
        }

        if (migrated > 0) {
            CopySign.getInstance().getDebugLogger().debug(
                    "Migrated " + migrated + " legacy copied-sign item(s) to PDC on " + trigger + ".");
        }
    }
}
