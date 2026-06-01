package us.ironcladnetwork.copySign.Util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure tests for the case-insensitive substring search primitive
 * {@link SignLibraryManager#filterByName(java.util.Collection, String)} (LIB-03, D-07).
 * No Bukkit dependency — the helper is plain in-memory logic.
 */
class LibrarySearchFilterTest {

    private final List<String> names =
            Arrays.asList("Welcome", "Shop", "WarpHub", "abandoned", "ABBA");

    @Test
    void matchesCaseInsensitiveSubstringAnywhere() {
        // "ab" appears in "abandoned" and "ABBA" (case-insensitive).
        List<String> result = SignLibraryManager.filterByName(names, "ab");
        assertEquals(2, result.size());
        assertTrue(result.contains("abandoned"));
        assertTrue(result.contains("ABBA"));
    }

    @Test
    void matchIsCaseInsensitiveForQueryAndName() {
        // Query "SHOP" matches name "Shop".
        List<String> result = SignLibraryManager.filterByName(names, "SHOP");
        assertEquals(List.of("Shop"), result);
    }

    @Test
    void matchesInteriorSubstringNotJustPrefix() {
        // "hub" appears in the middle/end of "WarpHub".
        List<String> result = SignLibraryManager.filterByName(names, "hub");
        assertEquals(List.of("WarpHub"), result);
    }

    @Test
    void nullQueryReturnsAllNames() {
        List<String> result = SignLibraryManager.filterByName(names, null);
        assertEquals(names.size(), result.size());
        assertTrue(result.containsAll(names));
    }

    @Test
    void blankQueryReturnsAllNames() {
        List<String> result = SignLibraryManager.filterByName(names, "   ");
        assertEquals(names.size(), result.size());
        assertTrue(result.containsAll(names));
    }

    @Test
    void emptyQueryReturnsAllNames() {
        List<String> result = SignLibraryManager.filterByName(names, "");
        assertEquals(names.size(), result.size());
    }

    @Test
    void noMatchReturnsEmptyList() {
        List<String> result = SignLibraryManager.filterByName(names, "zzz");
        assertTrue(result.isEmpty());
    }

    @Test
    void emptyInputCollectionReturnsEmpty() {
        List<String> result =
                SignLibraryManager.filterByName(Collections.emptyList(), "anything");
        assertTrue(result.isEmpty());
    }
}
