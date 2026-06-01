package us.ironcladnetwork.copySign.Util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for {@link SignItemData} — the value object that mirrors the
 * copied-sign payload carried on an item. No Bukkit runtime required.
 */
class SignItemDataTest {

    @Test
    void storesAllFields() {
        SignItemData data = new SignItemData("a\nb\nc\nd", "e\nf\ng\nh",
                "RED", "BLUE", true, false, "hanging");

        assertEquals("a\nb\nc\nd", data.getFront());
        assertEquals("e\nf\ng\nh", data.getBack());
        assertEquals("RED", data.getFrontColor());
        assertEquals("BLUE", data.getBackColor());
        assertTrue(data.isFrontGlowing());
        assertFalse(data.isBackGlowing());
        assertEquals("hanging", data.getSignType());
    }

    @Test
    void nullTextDefaultsToEmptyString() {
        SignItemData data = new SignItemData(null, null, null, null, false, false, null);
        assertEquals("", data.getFront());
        assertEquals("", data.getBack());
    }

    @Test
    void nullSignTypeDefaultsToRegular() {
        SignItemData data = new SignItemData("x", "y", null, null, false, false, null);
        assertEquals("regular", data.getSignType());
        assertFalse(data.isHanging());
    }

    @Test
    void colorPresenceReflectsNullability() {
        SignItemData withColors = new SignItemData("x", "y", "GREEN", "BLACK", false, false, "regular");
        assertTrue(withColors.hasFrontColor());
        assertTrue(withColors.hasBackColor());

        SignItemData noColors = new SignItemData("x", "y", null, null, false, false, "regular");
        assertFalse(noColors.hasFrontColor());
        assertFalse(noColors.hasBackColor());
    }

    @Test
    void combinedGlowIsTrueIfEitherSideGlows() {
        assertFalse(new SignItemData("x", "y", null, null, false, false, "regular").isGlowing());
        assertTrue(new SignItemData("x", "y", null, null, true, false, "regular").isGlowing());
        assertTrue(new SignItemData("x", "y", null, null, false, true, "regular").isGlowing());
        assertTrue(new SignItemData("x", "y", null, null, true, true, "regular").isGlowing());
    }

    @Test
    void isHangingIsCaseInsensitive() {
        assertTrue(new SignItemData("x", "y", null, null, false, false, "HANGING").isHanging());
        assertTrue(new SignItemData("x", "y", null, null, false, false, "hanging").isHanging());
        assertFalse(new SignItemData("x", "y", null, null, false, false, "regular").isHanging());
    }
}
