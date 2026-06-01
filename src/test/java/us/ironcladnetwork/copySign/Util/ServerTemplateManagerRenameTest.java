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
 * Pure-YAML tests for the static {@link ServerTemplateManager#rekey} re-key helper that
 * backs the admin template rename (LIB-02, LIB-04, D-05, D-09).
 * <p>
 * Mirrors {@link SignLibraryManagerRenameTest}'s plain {@link YamlConfiguration} style —
 * no MockBukkit, no {@link ServerTemplateManager} instance, no singletons (the manager's
 * constructor needs live singletons). The helper under test must be static and
 * side-effect-free (no lock / sound / metrics / Lang / scheduler calls) so it is
 * unit-testable against a bare configuration section.
 */
class ServerTemplateManagerRenameTest {

    /** A templates section pre-populated with a single template named "Foo". */
    private ConfigurationSection templatesSectionWithFoo() {
        ConfigurationSection templates = new YamlConfiguration().createSection("templates");
        SavedSignData foo = new SavedSignData(
                new String[]{"Line1", "Line2", "Line3", "Line4"},
                new String[]{"BackA", "BackB", "BackC", "BackD"},
                true, false, "RED", "BLUE", "hanging",
                List.of("lore1", "lore2"));
        foo.saveToConfigurationSection(templates.createSection("Foo"));
        return templates;
    }

    @Test
    void rekeyMovesDataToNewNameAndFreesOldName() {
        ConfigurationSection templates = templatesSectionWithFoo();

        ServerTemplateManager.RenameStatus status =
                ServerTemplateManager.rekey(templates, "Foo", "Bar");

        assertEquals(ServerTemplateManager.RenameStatus.OK, status);
        // Old name freed (D-09).
        assertFalse(templates.contains("Foo"));
        assertNull(templates.getConfigurationSection("Foo"));
        // New name now holds the data.
        assertTrue(templates.contains("Bar"));
    }

    @Test
    void rekeyPreservesFullSavedSignDataValue() {
        ConfigurationSection templates = templatesSectionWithFoo();

        ServerTemplateManager.rekey(templates, "Foo", "Bar");

        SavedSignData moved =
                SavedSignData.loadFromConfigurationSection(templates.getConfigurationSection("Bar"));
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
        ConfigurationSection templates = templatesSectionWithFoo();

        ServerTemplateManager.RenameStatus status =
                ServerTemplateManager.rekey(templates, "DoesNotExist", "Bar");

        assertEquals(ServerTemplateManager.RenameStatus.NOT_FOUND, status);
        assertFalse(templates.contains("Bar"));
        assertTrue(templates.contains("Foo")); // untouched
    }

    @Test
    void rekeyReturnsNotFoundForNullTemplatesSection() {
        ServerTemplateManager.RenameStatus status =
                ServerTemplateManager.rekey(null, "Foo", "Bar");
        assertEquals(ServerTemplateManager.RenameStatus.NOT_FOUND, status);
    }

    @Test
    void rekeyHardRejectsWhenTargetExistsAndNeverOverwrites() {
        ConfigurationSection templates = templatesSectionWithFoo();
        // A second, different template already occupies "Bar".
        SavedSignData existingBar = new SavedSignData(
                new String[]{"KEEP"}, new String[]{"KEEP"},
                false, false, "GREEN", "WHITE", "regular", null);
        existingBar.saveToConfigurationSection(templates.createSection("Bar"));

        ServerTemplateManager.RenameStatus status =
                ServerTemplateManager.rekey(templates, "Foo", "Bar");

        assertEquals(ServerTemplateManager.RenameStatus.TARGET_EXISTS, status);
        // Bar must be untouched (D-05 — never overwrite).
        SavedSignData bar =
                SavedSignData.loadFromConfigurationSection(templates.getConfigurationSection("Bar"));
        assertArrayEquals(new String[]{"KEEP"}, bar.getFront());
        assertEquals("GREEN", bar.getFrontColor());
        // Foo still present (no write happened).
        assertTrue(templates.contains("Foo"));
    }

    @Test
    void rekeyRejectsInvalidNewNameAndMakesNoWrites() {
        ConfigurationSection templates = templatesSectionWithFoo();

        // Contains a space — fails ErrorHandler.isValidFileName allow-list.
        ServerTemplateManager.RenameStatus status =
                ServerTemplateManager.rekey(templates, "Foo", "bad name");

        assertEquals(ServerTemplateManager.RenameStatus.INVALID_NAME, status);
        assertFalse(templates.contains("bad name"));
        assertTrue(templates.contains("Foo")); // untouched
    }

    @Test
    void rekeyRejectsReservedWindowsNewName() {
        ConfigurationSection templates = templatesSectionWithFoo();

        ServerTemplateManager.RenameStatus status =
                ServerTemplateManager.rekey(templates, "Foo", "CON");

        assertEquals(ServerTemplateManager.RenameStatus.INVALID_NAME, status);
        assertFalse(templates.contains("CON"));
        assertTrue(templates.contains("Foo"));
    }

    @Test
    void rekeyDoesNotReadOrWriteVersionMarker() {
        ConfigurationSection templates = templatesSectionWithFoo();

        ServerTemplateManager.rekey(templates, "Foo", "Bar");

        // Rename touches only the templates YAML re-key (D-09 negative constraint):
        // no version-marker key is introduced anywhere in the templates section.
        assertFalse(templates.contains("format_version"));
        assertFalse(templates.getConfigurationSection("Bar").contains("format_version"));
    }
}
