package us.ironcladnetwork.copySign.Util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    // ---- filterEntries seam (LIB-03, SC-4): the order-preserving entry filter reused by both
    // the command's first page and the listener's page 2+ navigation. No Bukkit dependency. ----

    /** A minimal SavedSignData; the seam only filters by KEY, so the value content is irrelevant. */
    private static SavedSignData stub() {
        return new SavedSignData(
                new String[]{"a", "b", "c", "d"},
                new String[]{"e", "f", "g", "h"},
                false, false, "OAK", "OAK", "regular", null);
    }

    /** An insertion-ordered map of the same names used above, each mapped to a stub value. */
    private Map<String, SavedSignData> entriesMap() {
        Map<String, SavedSignData> map = new LinkedHashMap<>();
        for (String name : names) {
            map.put(name, stub());
        }
        return map;
    }

    @Test
    void filterEntriesReturnsOnlyMatchingEntriesInOrder() {
        // "ab" matches "abandoned" and "ABBA"; result preserves insertion order.
        List<Map.Entry<String, SavedSignData>> result =
                SignLibraryManager.filterEntries(entriesMap(), "ab");
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, SavedSignData> e : result) {
            keys.add(e.getKey());
        }
        assertEquals(List.of("abandoned", "ABBA"), keys);
    }

    @Test
    void filterEntriesNullOrBlankQueryReturnsAllEntries() {
        assertEquals(names.size(), SignLibraryManager.filterEntries(entriesMap(), null).size());
        assertEquals(names.size(), SignLibraryManager.filterEntries(entriesMap(), "   ").size());
    }

    @Test
    void filterEntriesNoMatchReturnsEmptyList() {
        List<Map.Entry<String, SavedSignData>> result =
                SignLibraryManager.filterEntries(entriesMap(), "zzz");
        assertTrue(result.isEmpty());
    }

    @Test
    void filterEntriesNullMapReturnsEmptyList() {
        assertTrue(SignLibraryManager.filterEntries(null, "anything").isEmpty());
    }
}
