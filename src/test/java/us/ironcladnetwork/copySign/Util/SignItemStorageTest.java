package us.ironcladnetwork.copySign.Util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for {@link SignItemStorage} against a real (mock) Bukkit
 * {@link org.bukkit.persistence.PersistentDataContainer}.
 * <p>
 * This is the core of the 2.4.0 NBT-API → PDC migration: it proves that a
 * {@link SignItemData} survives a write→read cycle through an actual item's PDC, that
 * {@code clear} wipes it, and that absent (null) colors round-trip as absent. The legacy
 * NBT-API fallback path is intentionally <em>not</em> exercised here (NBT-API is not on
 * the mock server's classpath); it is covered by in-game upgrade smoke testing.
 */
class SignItemStorageTest {

    private Plugin plugin;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("CopySignTest");
        SignItemStorage.init(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void writeReadRoundTripPreservesAllFields() {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        SignItemData data = new SignItemData(
                "a\nb\nc\nd", "e\nf\ng\nh", "RED", "BLUE", true, false, "hanging");

        SignItemStorage.write(item, data);

        assertTrue(SignItemStorage.has(item));
        Optional<SignItemData> read = SignItemStorage.read(item);
        assertTrue(read.isPresent());

        SignItemData r = read.get();
        assertEquals("a\nb\nc\nd", r.getFront());
        assertEquals("e\nf\ng\nh", r.getBack());
        assertEquals("RED", r.getFrontColor());
        assertEquals("BLUE", r.getBackColor());
        assertTrue(r.isFrontGlowing());
        assertFalse(r.isBackGlowing());
        assertEquals("hanging", r.getSignType());
    }

    @Test
    void clearRemovesAllData() {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        SignItemStorage.write(item,
                new SignItemData("x", "y", "RED", "BLUE", true, true, "regular"));
        assertTrue(SignItemStorage.has(item));

        SignItemStorage.clear(item);

        assertFalse(SignItemStorage.has(item));
        assertTrue(SignItemStorage.read(item).isEmpty());
    }

    @Test
    void nullColorsRoundTripAsAbsent() {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        SignItemStorage.write(item,
                new SignItemData("x", "y", null, null, false, false, "regular"));

        SignItemData r = SignItemStorage.read(item).orElseThrow();
        assertFalse(r.hasFrontColor());
        assertFalse(r.hasBackColor());
        assertNull(r.getFrontColor());
        assertNull(r.getBackColor());
        // Non-color fields still present.
        assertEquals("x", r.getFront());
        assertEquals("regular", r.getSignType());
    }

    @Test
    void freshItemHasNoData() {
        assertFalse(SignItemStorage.has(new ItemStack(Material.OAK_SIGN)));
        assertTrue(SignItemStorage.read(new ItemStack(Material.OAK_SIGN)).isEmpty());
    }

    @Test
    void freshWriteStampsCurrentVersion() {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        SignItemStorage.write(item,
                new SignItemData("f", "b", null, null, false, false, "regular"));

        NamespacedKey key = new NamespacedKey(plugin, "format_version");
        Integer marker = item.getItemMeta().getPersistentDataContainer()
                .get(key, PersistentDataType.INTEGER);
        assertEquals(SignItemStorage.CURRENT_FORMAT_VERSION, marker);
        assertEquals(1, marker);
    }

    @Test
    void missingMarkerTreatedAsV1AndStampedOnWrite() {
        // A fresh item has no pre-seeded marker; a single write should stamp version 1.
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        assertNull(meta.getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "format_version"), PersistentDataType.INTEGER));

        SignItemStorage.write(item,
                new SignItemData("f", "b", null, null, false, false, "regular"));

        NamespacedKey key = new NamespacedKey(plugin, "format_version");
        Integer marker = item.getItemMeta().getPersistentDataContainer()
                .get(key, PersistentDataType.INTEGER);
        assertEquals(1, marker);
    }

    @Test
    void newerVersionItemNotDowngradedAndPreservesUnknownKey() {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        NamespacedKey versionKey = new NamespacedKey(plugin, "format_version");
        NamespacedKey foreignKey = new NamespacedKey(plugin, "future_field");
        // Seed a newer format version and a foreign key not known to SignItemStorage.
        pdc.set(versionKey, PersistentDataType.INTEGER, 2);
        pdc.set(foreignKey, PersistentDataType.STRING, "keep-me");

        SignItemStorage.write(meta,
                new SignItemData("f", "b", null, null, false, false, "regular"));
        item.setItemMeta(meta);

        PersistentDataContainer after = item.getItemMeta().getPersistentDataContainer();
        // Never downgraded: marker stays 2 (D-02).
        assertEquals(2, after.get(versionKey, PersistentDataType.INTEGER));
        // Unknown key preserved: write() never wipes foreign PDC entries (D-01).
        assertEquals("keep-me", after.get(foreignKey, PersistentDataType.STRING));
    }

    @Test
    void writeReadRoundTripWithMarkerPresent() {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        SignItemData data = new SignItemData(
                "a\nb\nc\nd", "e\nf\ng\nh", "RED", "BLUE", true, false, "hanging");

        SignItemStorage.write(item, data);

        // Marker is stamped...
        Integer marker = item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "format_version"), PersistentDataType.INTEGER);
        assertEquals(1, marker);

        // ...and all payload fields still survive the round-trip.
        SignItemData r = SignItemStorage.read(item).orElseThrow();
        assertEquals("a\nb\nc\nd", r.getFront());
        assertEquals("e\nf\ng\nh", r.getBack());
        assertEquals("RED", r.getFrontColor());
        assertEquals("BLUE", r.getBackColor());
        assertTrue(r.isFrontGlowing());
        assertFalse(r.isBackGlowing());
        assertEquals("hanging", r.getSignType());
    }

    @Test
    void writeOverwritesPreviousPayload() {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        SignItemStorage.write(item,
                new SignItemData("old-front", "old-back", "RED", "RED", true, true, "hanging"));
        // Overwrite with a no-color, non-glowing regular sign.
        SignItemStorage.write(item,
                new SignItemData("new-front", "new-back", null, null, false, false, "regular"));

        SignItemData r = SignItemStorage.read(item).orElseThrow();
        assertEquals("new-front", r.getFront());
        assertEquals("new-back", r.getBack());
        assertFalse(r.hasFrontColor()); // stale color must be cleared
        assertFalse(r.isFrontGlowing());
        assertEquals("regular", r.getSignType());
    }
}
