package us.ironcladnetwork.copySign.Util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
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
