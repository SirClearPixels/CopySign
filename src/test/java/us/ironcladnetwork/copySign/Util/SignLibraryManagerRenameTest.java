package us.ironcladnetwork.copySign.Util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-YAML tests for the static {@link SignLibraryManager#rekey} re-key helper that
 * backs the personal-library rename (LIB-01, LIB-04, D-05, D-09).
 * <p>
 * Mirrors {@link SavedSignDataTest}'s plain {@link YamlConfiguration} style — no
 * MockBukkit, no {@link SignLibraryManager} instance, no singletons. The helper under
 * test must be static and side-effect-free (no lock / sound / metrics / Lang / scheduler
 * calls) so it is unit-testable against a bare configuration section.
 */
class SignLibraryManagerRenameTest {

    /** An owner section pre-populated with a single saved sign named "Foo". */
    private ConfigurationSection ownerSectionWithFoo() {
        ConfigurationSection owner = new YamlConfiguration().createSection("owner");
        SavedSignData foo = new SavedSignData(
                new String[]{"Line1", "Line2", "Line3", "Line4"},
                new String[]{"BackA", "BackB", "BackC", "BackD"},
                true, false, "RED", "BLUE", "hanging",
                List.of("lore1", "lore2"));
        foo.saveToConfigurationSection(owner.createSection("Foo"));
        return owner;
    }

    @Test
    void rekeyMovesDataToNewNameAndFreesOldName() {
        ConfigurationSection owner = ownerSectionWithFoo();

        SignLibraryManager.RenameStatus status =
                SignLibraryManager.rekey(owner, "Foo", "Bar");

        assertEquals(SignLibraryManager.RenameStatus.OK, status);
        // Old name freed (D-09).
        assertFalse(owner.contains("Foo"));
        assertNull(owner.getConfigurationSection("Foo"));
        // New name now holds the data.
        assertTrue(owner.contains("Bar"));
    }

    @Test
    void rekeyPreservesFullSavedSignDataValue() {
        ConfigurationSection owner = ownerSectionWithFoo();

        SignLibraryManager.rekey(owner, "Foo", "Bar");

        SavedSignData moved =
                SavedSignData.loadFromConfigurationSection(owner.getConfigurationSection("Bar"));
        assertArrayEquals(new String[]{"Line1", "Line2", "Line3", "Line4"}, moved.getFront());
        assertArrayEquals(new String[]{"BackA", "BackB", "BackC", "BackD"}, moved.getBack());
        assertTrue(moved.isFrontGlowing());
        assertFalse(moved.isBackGlowing());
        assertEquals("RED", moved.getFrontColor());
        assertEquals("BLUE", moved.getBackColor());
        assertEquals("hanging", moved.getSignType());
        assertEquals(List.of("lore1", "lore2"), moved.getLore());
    }

    @Test
    void rekeyReturnsNotFoundForMissingOldNameAndMakesNoWrites() {
        ConfigurationSection owner = ownerSectionWithFoo();

        SignLibraryManager.RenameStatus status =
                SignLibraryManager.rekey(owner, "DoesNotExist", "Bar");

        assertEquals(SignLibraryManager.RenameStatus.NOT_FOUND, status);
        assertFalse(owner.contains("Bar"));
        assertTrue(owner.contains("Foo")); // untouched
    }

    @Test
    void rekeyReturnsNotFoundForNullOwnerSection() {
        SignLibraryManager.RenameStatus status =
                SignLibraryManager.rekey(null, "Foo", "Bar");
        assertEquals(SignLibraryManager.RenameStatus.NOT_FOUND, status);
    }

    @Test
    void rekeyHardRejectsWhenTargetExistsAndNeverOverwrites() {
        ConfigurationSection owner = ownerSectionWithFoo();
        // A second, different sign already occupies "Bar".
        SavedSignData existingBar = new SavedSignData(
                new String[]{"KEEP"}, new String[]{"KEEP"},
                false, false, "GREEN", "WHITE", "regular", null);
        existingBar.saveToConfigurationSection(owner.createSection("Bar"));

        SignLibraryManager.RenameStatus status =
                SignLibraryManager.rekey(owner, "Foo", "Bar");

        assertEquals(SignLibraryManager.RenameStatus.TARGET_EXISTS, status);
        // Bar must be untouched (D-05 — never overwrite).
        SavedSignData bar =
                SavedSignData.loadFromConfigurationSection(owner.getConfigurationSection("Bar"));
        assertArrayEquals(new String[]{"KEEP"}, bar.getFront());
        assertEquals("GREEN", bar.getFrontColor());
        // Foo still present (no write happened).
        assertTrue(owner.contains("Foo"));
    }

    @Test
    void rekeyRejectsInvalidNewNameAndMakesNoWrites() {
        ConfigurationSection owner = ownerSectionWithFoo();

        // Contains a space — fails ErrorHandler.isValidFileName allow-list.
        SignLibraryManager.RenameStatus status =
                SignLibraryManager.rekey(owner, "Foo", "bad name");

        assertEquals(SignLibraryManager.RenameStatus.INVALID_NAME, status);
        assertFalse(owner.contains("bad name"));
        assertTrue(owner.contains("Foo")); // untouched
    }

    @Test
    void rekeyRejectsReservedWindowsNewName() {
        ConfigurationSection owner = ownerSectionWithFoo();

        SignLibraryManager.RenameStatus status =
                SignLibraryManager.rekey(owner, "Foo", "CON");

        assertEquals(SignLibraryManager.RenameStatus.INVALID_NAME, status);
        assertFalse(owner.contains("CON"));
        assertTrue(owner.contains("Foo"));
    }

    @Test
    void rekeyDoesNotReadOrWriteFormatVersion() {
        ConfigurationSection owner = ownerSectionWithFoo();

        SignLibraryManager.rekey(owner, "Foo", "Bar");

        // Rename touches only the library YAML re-key (D-09 negative constraint):
        // no format_version key is introduced anywhere in the owner section.
        assertFalse(owner.contains("format_version"));
        assertFalse(owner.getConfigurationSection("Bar").contains("format_version"));
    }
}
