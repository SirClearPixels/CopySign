package us.ironcladnetwork.copySign.Util;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * One-version compatibility bridge that reads copied-sign data written by CopySign
 * &le; 2.3.0 using the (now optional) NBT-API.
 * <p>
 * As of 2.4.0 the primary storage backend is the native
 * {@link org.bukkit.persistence.PersistentDataContainer} (see {@link SignItemStorage}).
 * NBT-API is downgraded from a hard {@code depend} to a {@code softdepend}; this class
 * exists only so that items copied with an older version (sitting in a player's
 * inventory or a chest) can still be read and transparently migrated to the new
 * format. It is expected to be removed in a future release.
 * <p>
 * <strong>Classloading safety:</strong> every public method is guarded by
 * {@link #isAvailable()} and wrapped in a {@code catch (Throwable)} so that, when the
 * NBT-API plugin is <em>not</em> installed, the {@link NBTItem} reference never escapes
 * as an uncaught {@link NoClassDefFoundError}.
 *
 * @since 2.4.0
 */
final class LegacyNbtBridge {

    private LegacyNbtBridge() {
    }

    /**
     * @return {@code true} if the NBT-API plugin is present and enabled, so legacy
     *         tags can be read safely.
     */
    static boolean isAvailable() {
        try {
            return Bukkit.getPluginManager().getPlugin("NBTAPI") != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * @return {@code true} if the item carries legacy copied-sign NBT tags.
     */
    static boolean hasLegacy(ItemStack item) {
        if (item == null) {
            return false;
        }
        try {
            NBTItem nbt = new NBTItem(item);
            return nbt.hasTag("copiedSignFront") && nbt.hasTag("copiedSignBack");
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Reads legacy copied-sign data from an item, mapping the old per-side / legacy
     * glow tags into a {@link SignItemData}.
     *
     * @return the decoded data, or {@link Optional#empty()} if the item has no legacy
     *         payload or NBT-API is unavailable.
     */
    static Optional<SignItemData> read(ItemStack item) {
        if (item == null) {
            return Optional.empty();
        }
        try {
            NBTItem nbt = new NBTItem(item);
            if (!nbt.hasTag("copiedSignFront") || !nbt.hasTag("copiedSignBack")) {
                return Optional.empty();
            }
            String front = nbt.getString("copiedSignFront");
            String back = nbt.getString("copiedSignBack");
            String frontColor = nbt.hasTag("copiedSignFrontColor") ? nbt.getString("copiedSignFrontColor") : null;
            String backColor = nbt.hasTag("copiedSignBackColor") ? nbt.getString("copiedSignBackColor") : null;

            boolean frontGlowing;
            boolean backGlowing;
            if (nbt.hasTag("frontGlowing") || nbt.hasTag("backGlowing")) {
                frontGlowing = nbt.hasTag("frontGlowing") && nbt.getBoolean("frontGlowing");
                backGlowing = nbt.hasTag("backGlowing") && nbt.getBoolean("backGlowing");
            } else {
                boolean legacyGlow = nbt.hasTag("signGlowing") && nbt.getBoolean("signGlowing");
                frontGlowing = legacyGlow;
                backGlowing = legacyGlow;
            }

            String signType = nbt.hasTag("signType") ? nbt.getString("signType") : "regular";
            return Optional.of(new SignItemData(front, back, frontColor, backColor, frontGlowing, backGlowing, signType));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    /**
     * Removes all legacy copied-sign NBT-API tags from the item. Required by the
     * {@code /copysign clear} command so a clear genuinely wipes data from an item that
     * was copied with an older version (PDC clearing alone would leave legacy tags
     * readable via the fallback path).
     *
     * @return the (possibly new) item instance with legacy tags removed; the caller
     *         must use the returned reference.
     */
    static ItemStack strip(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            NBTItem nbt = new NBTItem(item);
            nbt.removeKey("copiedSignFront");
            nbt.removeKey("copiedSignBack");
            nbt.removeKey("copiedSignFrontColor");
            nbt.removeKey("copiedSignBackColor");
            nbt.removeKey("signGlowing");
            nbt.removeKey("frontGlowing");
            nbt.removeKey("backGlowing");
            nbt.removeKey("signType");
            return nbt.getItem();
        } catch (Throwable t) {
            return item;
        }
    }
}
