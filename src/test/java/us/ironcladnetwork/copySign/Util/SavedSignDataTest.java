package us.ironcladnetwork.copySign.Util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip and backward-compatibility tests for {@link SavedSignData} YAML
 * serialization. Uses a standalone {@link YamlConfiguration} (no Bukkit server).
 * <p>
 * This guards the persisted format for sign libraries and server templates — the data
 * that must survive plugin upgrades untouched.
 */
class SavedSignDataTest {

    private ConfigurationSection newSection() {
        return new YamlConfiguration().createSection("sign");
    }

    @Test
    void roundTripsPerSideGlowAndColors() {
        SavedSignData original = new SavedSignData(
                new String[]{"Line1", "Line2", "Line3", "Line4"},
                new String[]{"BackA", "BackB", "BackC", "BackD"},
                true, false, "RED", "BLUE", "hanging",
                List.of("lore1", "lore2"));

        ConfigurationSection section = newSection();
        original.saveToConfigurationSection(section);
        SavedSignData loaded = SavedSignData.loadFromConfigurationSection(section);

        assertArrayEquals(new String[]{"Line1", "Line2", "Line3", "Line4"}, loaded.getFront());
        assertArrayEquals(new String[]{"BackA", "BackB", "BackC", "BackD"}, loaded.getBack());
        assertTrue(loaded.isFrontGlowing());
        assertFalse(loaded.isBackGlowing());
        assertEquals("RED", loaded.getFrontColor());
        assertEquals("BLUE", loaded.getBackColor());
        assertEquals("hanging", loaded.getSignType());
        assertEquals(Arrays.asList("lore1", "lore2"), loaded.getLore());
    }

    @Test
    void preservesInteriorBlankLines() {
        // A blank line BETWEEN content lines must survive the newline round-trip.
        SavedSignData original = new SavedSignData(
                new String[]{"A", "", "C", "D"},
                new String[]{"E", "F", "G", "H"},
                false, false, "BLACK", "BLACK", "regular", null);

        ConfigurationSection section = newSection();
        original.saveToConfigurationSection(section);
        SavedSignData loaded = SavedSignData.loadFromConfigurationSection(section);

        assertArrayEquals(new String[]{"A", "", "C", "D"}, loaded.getFront());
    }

    @Test
    void documentsTrailingBlankLineCollapseOnLoad() {
        // KNOWN BEHAVIOR (pre-2.4): loadFromConfigurationSection() uses split("\n")
        // without a negative limit, so trailing blank lines are dropped on load.
        // Pinned here so any future change to the persisted format is a deliberate,
        // visible decision. Candidate Tier-2 fix: use split("\n", -1) on load.
        SavedSignData original = new SavedSignData(
                new String[]{"BackA", "BackB", "", ""},
                new String[]{"x", "", "", ""},
                false, false, "BLACK", "BLACK", "regular", null);

        ConfigurationSection section = newSection();
        original.saveToConfigurationSection(section);
        SavedSignData loaded = SavedSignData.loadFromConfigurationSection(section);

        // Trailing empties collapsed: {"BackA","BackB","",""} -> {"BackA","BackB"}.
        assertArrayEquals(new String[]{"BackA", "BackB"}, loaded.getFront());
        assertArrayEquals(new String[]{"x"}, loaded.getBack());
    }

    @Test
    void persistsBothLegacyAndPerSideGlowKeys() {
        SavedSignData original = new SavedSignData(
                new String[]{"x"}, new String[]{"y"},
                true, false, "BLACK", "BLACK", "regular", null);

        ConfigurationSection section = newSection();
        original.saveToConfigurationSection(section);

        // Per-side keys present...
        assertTrue(section.getBoolean("frontGlowing"));
        assertFalse(section.getBoolean("backGlowing"));
        // ...and the legacy combined flag is still written for old readers.
        assertTrue(section.getBoolean("glowing"));
    }

    @Test
    void decodesLegacySingleGlowWhenPerSideAbsent() {
        // Simulate a pre-2.1 saved entry: only the combined "glowing" flag exists.
        ConfigurationSection section = newSection();
        section.set("front", "hello");
        section.set("back", "world");
        section.set("glowing", true);
        section.set("frontColor", "GREEN");
        section.set("backColor", "WHITE");
        section.set("signType", "regular");

        SavedSignData loaded = SavedSignData.loadFromConfigurationSection(section);

        // Legacy single glow should apply to both sides.
        assertTrue(loaded.isFrontGlowing());
        assertTrue(loaded.isBackGlowing());
        assertEquals("GREEN", loaded.getFrontColor());
        assertEquals("WHITE", loaded.getBackColor());
    }

    @Test
    void appliesDefaultsWhenFieldsMissing() {
        ConfigurationSection section = newSection();
        section.set("front", "only-front");
        section.set("back", "only-back");

        SavedSignData loaded = SavedSignData.loadFromConfigurationSection(section);

        assertEquals("BLACK", loaded.getFrontColor());
        assertEquals("BLACK", loaded.getBackColor());
        assertEquals("regular", loaded.getSignType());
        assertFalse(loaded.isFrontGlowing());
        assertFalse(loaded.isBackGlowing());
    }

    @Test
    void preservesMultiLineTextAcrossNewlineEncoding() {
        SavedSignData original = new SavedSignData(
                new String[]{"top", "second", "third", "bottom"},
                new String[]{"b1", "b2", "b3", "b4"},
                false, false, "BLACK", "BLACK", "regular", null);

        ConfigurationSection section = newSection();
        original.saveToConfigurationSection(section);
        SavedSignData loaded = SavedSignData.loadFromConfigurationSection(section);

        assertEquals(4, loaded.getFront().length);
        assertEquals("third", loaded.getFront()[2]);
        assertEquals("b4", loaded.getBack()[3]);
    }
}
