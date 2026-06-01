package us.ironcladnetwork.copySign.Util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * Single source of truth for reading and writing the copied-sign payload on a sign
 * <em>item</em>.
 * <p>
 * As of 2.4.0 this uses the native Bukkit {@link PersistentDataContainer} (PDC), which
 * removes the previous hard dependency on the NBT-API plugin. All seven former
 * {@code new NBTItem(...)} call sites now go through this class.
 * <p>
 * <strong>Backward compatibility:</strong> {@link #read(ItemStack)} and
 * {@link #has(ItemStack)} fall back to {@link LegacyNbtBridge} so items copied with
 * CopySign &le; 2.3.0 still work while the NBT-API {@code softdepend} is present.
 * {@link #migrateIfLegacy(ItemStack)} rewrites such an item to PDC so it survives even
 * after NBT-API is eventually removed.
 *
 * @since 2.4.0
 */
public final class SignItemStorage {

    private static NamespacedKey keyFront;
    private static NamespacedKey keyBack;
    private static NamespacedKey keyFrontColor;
    private static NamespacedKey keyBackColor;
    private static NamespacedKey keyFrontGlow;
    private static NamespacedKey keyBackGlow;
    private static NamespacedKey keyType;
    private static volatile boolean initialized = false;

    private SignItemStorage() {
    }

    /**
     * Initializes the namespaced keys. Must be called once during {@code onEnable}
     * before any read/write occurs.
     *
     * @param plugin the owning plugin used to namespace the PDC keys.
     */
    public static void init(Plugin plugin) {
        keyFront = new NamespacedKey(plugin, "copied_sign_front");
        keyBack = new NamespacedKey(plugin, "copied_sign_back");
        keyFrontColor = new NamespacedKey(plugin, "copied_sign_front_color");
        keyBackColor = new NamespacedKey(plugin, "copied_sign_back_color");
        keyFrontGlow = new NamespacedKey(plugin, "front_glowing");
        keyBackGlow = new NamespacedKey(plugin, "back_glowing");
        keyType = new NamespacedKey(plugin, "sign_type");
        initialized = true;
    }

    private static void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("SignItemStorage.init(plugin) must be called before use.");
        }
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * @return {@code true} if the item carries copied-sign data in either the native
     *         PDC format or (as a fallback) the legacy NBT-API format.
     */
    public static boolean has(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return hasLegacy(item);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && hasPdc(meta.getPersistentDataContainer())) {
            return true;
        }
        return hasLegacy(item);
    }

    /**
     * Reads the copied-sign payload from an item. PDC is authoritative; if absent, a
     * one-time legacy read is attempted.
     *
     * @return the decoded data, or {@link Optional#empty()} if the item carries none.
     */
    public static Optional<SignItemData> read(ItemStack item) {
        ensureInitialized();
        if (item == null) {
            return Optional.empty();
        }
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && hasPdc(meta.getPersistentDataContainer())) {
                return Optional.of(readPdc(meta.getPersistentDataContainer()));
            }
        }
        if (LegacyNbtBridge.isAvailable()) {
            return LegacyNbtBridge.read(item);
        }
        return Optional.empty();
    }

    /**
     * Writes the payload onto the supplied {@link ItemMeta}'s PDC. The caller is
     * responsible for applying the meta back to the item (useful when the caller also
     * mutates lore/display name on the same meta to avoid a double fetch).
     */
    public static void write(ItemMeta meta, SignItemData data) {
        ensureInitialized();
        if (meta == null || data == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyFront, PersistentDataType.STRING, data.getFront());
        pdc.set(keyBack, PersistentDataType.STRING, data.getBack());

        setOrRemoveString(pdc, keyFrontColor, data.getFrontColor());
        setOrRemoveString(pdc, keyBackColor, data.getBackColor());

        pdc.set(keyFrontGlow, PersistentDataType.BYTE, (byte) (data.isFrontGlowing() ? 1 : 0));
        pdc.set(keyBackGlow, PersistentDataType.BYTE, (byte) (data.isBackGlowing() ? 1 : 0));
        pdc.set(keyType, PersistentDataType.STRING, data.getSignType());
    }

    /**
     * Convenience overload that writes the payload and applies the meta back to the
     * item in one step.
     *
     * @return the same {@link ItemStack} instance, with updated meta.
     */
    public static ItemStack write(ItemStack item, SignItemData data) {
        ensureInitialized();
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        write(meta, data);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Removes all copied-sign PDC keys from the supplied meta. Does not touch
     * lore/display name (the caller manages those).
     */
    public static void clear(ItemMeta meta) {
        ensureInitialized();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(keyFront);
        pdc.remove(keyBack);
        pdc.remove(keyFrontColor);
        pdc.remove(keyBackColor);
        pdc.remove(keyFrontGlow);
        pdc.remove(keyBackGlow);
        pdc.remove(keyType);
    }

    /**
     * Convenience overload that clears the keys and applies the meta back to the item.
     *
     * @return the same {@link ItemStack} instance, with updated meta.
     */
    public static ItemStack clear(ItemStack item) {
        ensureInitialized();
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        clear(meta);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Strips any legacy NBT-API copied-sign tags from the item. No-op when NBT-API is
     * unavailable. Used by {@code /copysign clear} so clearing also wipes data from
     * items copied with CopySign &le; 2.3.0.
     *
     * @return the (possibly new) item instance; callers must use the returned reference.
     */
    public static ItemStack stripLegacy(ItemStack item) {
        if (item == null || !LegacyNbtBridge.isAvailable()) {
            return item;
        }
        return LegacyNbtBridge.strip(item);
    }

    /**
     * If the item carries <em>only</em> legacy NBT-API data (no PDC payload yet),
     * rewrites it into the native PDC format. Used by the chest-open and lazy migration
     * paths so old items survive the eventual removal of NBT-API.
     *
     * @return {@code true} if a migration was performed.
     */
    public static boolean migrateIfLegacy(ItemStack item) {
        ensureInitialized();
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || hasPdc(meta.getPersistentDataContainer())) {
            return false; // already in the new format (or nothing to do)
        }
        if (!LegacyNbtBridge.isAvailable()) {
            return false;
        }
        Optional<SignItemData> legacy = LegacyNbtBridge.read(item);
        if (legacy.isEmpty()) {
            return false;
        }
        write(meta, legacy.get());
        item.setItemMeta(meta);
        return true;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static boolean hasLegacy(ItemStack item) {
        return LegacyNbtBridge.isAvailable() && LegacyNbtBridge.hasLegacy(item);
    }

    private static boolean hasPdc(PersistentDataContainer pdc) {
        return pdc.has(keyFront, PersistentDataType.STRING) && pdc.has(keyBack, PersistentDataType.STRING);
    }

    private static SignItemData readPdc(PersistentDataContainer pdc) {
        String front = pdc.getOrDefault(keyFront, PersistentDataType.STRING, "");
        String back = pdc.getOrDefault(keyBack, PersistentDataType.STRING, "");
        String frontColor = pdc.has(keyFrontColor, PersistentDataType.STRING)
                ? pdc.get(keyFrontColor, PersistentDataType.STRING) : null;
        String backColor = pdc.has(keyBackColor, PersistentDataType.STRING)
                ? pdc.get(keyBackColor, PersistentDataType.STRING) : null;
        boolean frontGlow = pdc.getOrDefault(keyFrontGlow, PersistentDataType.BYTE, (byte) 0) != 0;
        boolean backGlow = pdc.getOrDefault(keyBackGlow, PersistentDataType.BYTE, (byte) 0) != 0;
        String type = pdc.getOrDefault(keyType, PersistentDataType.STRING, "regular");
        return new SignItemData(front, back, frontColor, backColor, frontGlow, backGlow, type);
    }

    private static void setOrRemoveString(PersistentDataContainer pdc, NamespacedKey key, String value) {
        if (value == null) {
            pdc.remove(key);
        } else {
            pdc.set(key, PersistentDataType.STRING, value);
        }
    }
}
