package us.ironcladnetwork.copySign.Lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-unit rendering tests for the Adventure {@link Component} migration of {@link Lang}
 * (MSG-01, MSG-02) plus the legacy-vs-MiniMessage auto-detect routing and the D-04
 * placeholder injection-safety contract.
 * <p>
 * These tests exercise the static rendering seam(s) created by plan 06-01 Task 2 —
 * {@code Lang.render(String)}, {@code Lang.isLegacy(String)}, and the
 * {@code Lang.formatRaw(String, Object...)} format primitive. No Bukkit / MockBukkit is
 * required: rendering is pure {@code String -> Component} logic.
 * <p>
 * Mirrors the pure-unit harness style of {@code LibrarySearchFilterTest} (package decl,
 * JUnit 5 imports, static assertions, no MockBukkit).
 */
class LangRenderingTest {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    /**
     * MSG-02: a legacy {@code &}-coded string must render to a Component byte-identical to
     * what the legacy serializer itself produces, pinning it to the old
     * {@code translateAlternateColorCodes} behavior.
     */
    @Test
    void legacyStringRendersUnchanged() {
        String raw = "&aSign copy feature enabled.";
        Component expected = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
        Component actual = Lang.render(raw);
        assertEquals(expected, actual,
                "Legacy & string must render identically to LegacyComponentSerializer.legacyAmpersand()");
    }

    /**
     * MSG-01: a MiniMessage string with a gradient and a click tag must parse to a styled
     * Component carrying a ClickEvent — not a flat literal Component.
     */
    @Test
    void miniMessageParsesTags() {
        String raw = "<gradient:#ff0000:#0000ff>Hello</gradient><click:run_command:/x>go</click>";
        Component parsed = Lang.render(raw);

        // It must NOT be the same as treating the raw string as literal text.
        Component literal = Component.text(raw);
        assertNotEquals(literal, parsed,
                "MiniMessage tags must be parsed, not rendered as literal text");

        // The expectation is exactly MiniMessage's own parse of the same string.
        Component expected = MiniMessage.miniMessage().deserialize(raw);
        assertEquals(expected, parsed, "render() must MiniMessage-parse a tag-only string");

        // A ClickEvent must be present somewhere in the tree (on the <click> child).
        assertTrue(hasClickEvent(parsed), "Parsed MiniMessage must carry a ClickEvent from <click>");
    }

    /**
     * MSG-01/02: the production auto-detect predicate routes {@code &}/{@code §} strings to
     * the legacy path and tag-only strings to MiniMessage.
     */
    @Test
    void autoDetectRouting() {
        assertTrue(Lang.isLegacy("&aFoo"), "& color code must be detected as legacy");
        assertTrue(Lang.isLegacy("§aFoo"), "§ (section) code must be detected as legacy");
        assertFalse(Lang.isLegacy("<red>Foo"), "tag-only string must NOT be detected as legacy");
    }

    /**
     * MSG-01, D-04: a player-supplied value containing MiniMessage tags must be inserted as
     * literal text via Placeholder.unparsed — it appears verbatim in the rendered plain text
     * and contributes NO ClickEvent. Proves injection safety.
     */
    @Test
    void placeholderValueIsLiteral() {
        String template = "<gray>No match: <q>";
        String evil = "<click:run_command:/op me>evil</click>";

        Component rendered = Lang.formatRaw(template, "%q%", evil);

        String plain = PLAIN.serialize(rendered);
        assertTrue(plain.contains("<click:run_command:/op me>"),
                "Player value must appear as literal text, not be parsed as a tag. Got: " + plain);
        assertFalse(hasClickEvent(rendered),
                "Player-supplied value must NOT introduce a ClickEvent (injection blocked)");
    }

    /** Recursively checks whether any node in the Component tree carries a ClickEvent. */
    private static boolean hasClickEvent(Component component) {
        if (component.clickEvent() != null) {
            return true;
        }
        for (Component child : component.children()) {
            if (hasClickEvent(child)) {
                return true;
            }
        }
        return false;
    }
}
