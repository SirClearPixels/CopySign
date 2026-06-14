package us.ironcladnetwork.copySign.Lang;

import net.kyori.adventure.text.Component;
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

        // render() must route a tag-only string to MiniMessage — assert via the rendered
        // plain text (the gradient expands to per-character children, so the raw "<gradient...>"
        // markup must be GONE while the visible text "Hellogo" survives).
        String plain = PLAIN.serialize(parsed);
        assertFalse(plain.contains("<gradient"),
                "render() must MiniMessage-parse the gradient tag, not keep it literal. Got: " + plain);
        assertTrue(plain.contains("Hello") && plain.contains("go"),
                "Parsed visible text must survive. Got: " + plain);

        // The styling must reflect the tags: at least one child carries a color (gradient color).
        assertTrue(hasColor(parsed), "Parsed MiniMessage gradient must apply color styling");

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
     * <p>
     * Uses the SHIPPED {@code %query%} token form (matching messages.yml), NOT the {@code <q>}
     * tag form, so it exercises the real substitution path (regression guard for CR-01: a prior
     * version registered the resolver against {@code <query>} while the template kept the literal
     * {@code %query%}, so the value was never inserted).
     */
    @Test
    void placeholderValueIsLiteral() {
        String template = "<yellow>No saved signs match \"%query%\".";
        String evil = "<click:run_command:/op me>evil</click>";

        Component rendered = Lang.formatRaw(template, "%query%", evil);

        String plain = PLAIN.serialize(rendered);
        assertTrue(plain.contains("<click:run_command:/op me>"),
                "Player value must appear as literal text, not be parsed as a tag. Got: " + plain);
        assertFalse(plain.contains("%query%"),
                "The literal %query% token must be substituted, not left in the output. Got: " + plain);
        assertFalse(hasClickEvent(rendered),
                "Player-supplied value must NOT introduce a ClickEvent (injection blocked)");
    }

    /**
     * CR-01 regression: a {@code %name%} token in a MiniMessage-path string must have its value
     * substituted in. Pins the shipped-default behavior (e.g. {@code SIGN_LOADED_TO_HELD}).
     */
    @Test
    void percentTokenIsSubstitutedOnMiniMessagePath() {
        String template = "<green>Sign '%name%' loaded to your held sign!";
        Component rendered = Lang.formatRaw(template, "%name%", "myHouse");

        String plain = PLAIN.serialize(rendered);
        assertTrue(plain.contains("myHouse"),
                "Value 'myHouse' must be substituted for %name%. Got: " + plain);
        assertFalse(plain.contains("%name%"),
                "The literal %name% token must not survive substitution. Got: " + plain);
    }

    /**
     * CR-02 regression: a brace-style {@code {max}} token must substitute its value without
     * throwing. A prior version passed the raw {@code {max}} key to Placeholder.unparsed, which
     * threw IllegalArgumentException (tag names cannot contain braces) and crashed the branch
     * (e.g. {@code MAX_SIGNS_REACHED}).
     */
    @Test
    void bracePlaceholderSubstitutesWithoutThrowing() {
        String template = "<red>You have reached the maximum number of saved signs ({max})!";
        Component rendered = Lang.formatRaw(template, "{max}", "42");

        String plain = PLAIN.serialize(rendered);
        assertTrue(plain.contains("42"),
                "Value '42' must be substituted for {max}. Got: " + plain);
        assertFalse(plain.contains("{max}"),
                "The literal {max} token must not survive substitution. Got: " + plain);
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

    /** Recursively checks whether any node in the Component tree carries a text color. */
    private static boolean hasColor(Component component) {
        if (component.color() != null) {
            return true;
        }
        for (Component child : component.children()) {
            if (hasColor(child)) {
                return true;
            }
        }
        return false;
    }
}
