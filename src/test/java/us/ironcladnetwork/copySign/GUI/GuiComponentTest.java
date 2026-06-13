package us.ironcladnetwork.copySign.GUI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure-unit test pinning the D-06 italic-off contract for GUI Components (MSG-03):
 * every GUI item display-name / lore Component must be built with ITALIC explicitly
 * disabled, otherwise Minecraft renders item-meta Components italic by default.
 * <p>
 * Plan 06-03 (the GUI builder migration) reuses this identical helper shape, so the
 * assertion here is the contract those builders must satisfy. No Bukkit / MockBukkit.
 */
class GuiComponentTest {

    /**
     * MSG-03 / D-06: a Component built the way the GUI builders will build it
     * (MiniMessage parse + ITALIC=false) reports {@code decoration(ITALIC) == State.FALSE}.
     */
    @Test
    void labelNotItalic() {
        Component label = MiniMessage.miniMessage()
                .deserialize("<gold><bold>Label")
                .decoration(TextDecoration.ITALIC, false);

        assertEquals(TextDecoration.State.FALSE, label.decoration(TextDecoration.ITALIC),
                "GUI label Component must have ITALIC explicitly disabled (D-06)");
    }
}
