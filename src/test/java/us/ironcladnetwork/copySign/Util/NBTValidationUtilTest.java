package us.ironcladnetwork.copySign.Util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link NBTValidationUtil} — the size/content guards applied to copied sign
 * data before it is written to an item or persisted.
 */
class NBTValidationUtilTest {

    @Test
    void acceptsNormalSignText() {
        assertTrue(NBTValidationUtil.validateNBTData("Hello\nWorld\nLine3\nLine4"));
    }

    @Test
    void acceptsNullAndEmpty() {
        assertTrue(NBTValidationUtil.validateNBTData(null));
        assertTrue(NBTValidationUtil.validateNBTData(""));
    }

    @Test
    void rejectsOversizedPayload() {
        String huge = "x".repeat(33_000); // exceeds the 32KB cap
        assertFalse(NBTValidationUtil.validateNBTData(huge));
    }

    @Test
    void validateSignLinesRejectsTooManyLines() {
        assertTrue(NBTValidationUtil.validateSignLines(new String[]{"a", "b", "c", "d"}));
        assertFalse(NBTValidationUtil.validateSignLines(new String[]{"a", "b", "c", "d", "e"}));
    }

    @Test
    void validateSignLinesRejectsOverlongLine() {
        String longLine = "y".repeat(500); // exceeds the 384 char per-line cap
        assertFalse(NBTValidationUtil.validateSignLines(new String[]{longLine}));
    }

    @Test
    void validateSignDataAcceptsValidFrontAndBack() {
        String[] front = {"one", "two", "three", "four"};
        String[] back = {"five", "six", "seven", "eight"};
        assertTrue(NBTValidationUtil.validateSignData(front, back));
    }

    @Test
    void validateSignDataRejectsOverlongSide() {
        String[] front = {"a", "b", "c", "d", "e"}; // 5 lines > max 4
        String[] back = {"ok"};
        assertFalse(NBTValidationUtil.validateSignData(front, back));
    }
}
