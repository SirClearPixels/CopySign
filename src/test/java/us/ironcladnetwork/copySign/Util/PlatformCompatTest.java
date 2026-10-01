package us.ironcladnetwork.copySign.Util;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/** Runs against the Spigot API, which lacks Paper's Adventure overloads. */
class PlatformCompatTest {
    @Test void messagesUseSpigotStrings() {
        CommandSender sender = mock(CommandSender.class);
        PlatformCompat.sendMessage(sender, Component.text("Poplar sign copied"));
        Object argument = mockingDetails(sender).getInvocations().iterator().next().getArgument(0);
        assertEquals(hasAdventure(CommandSender.class, "sendMessage", Component.class)
                ? Component.text("Poplar sign copied") : "Poplar sign copied", argument);
    }
    @Test void itemNamesAndLoreUseSpigotStrings() {
        ItemMeta meta = mock(ItemMeta.class);
        PlatformCompat.displayName(meta, Component.text("Poplar"));
        PlatformCompat.lore(meta, List.of(Component.text("Front and back")));
        var calls = mockingDetails(meta).getInvocations().iterator();
        assertEquals(hasAdventure(ItemMeta.class, "displayName", Component.class)
                ? Component.text("Poplar") : "Poplar", calls.next().getArgument(0));
        assertEquals(hasAdventure(ItemMeta.class, "lore", List.class)
                ? List.of(Component.text("Front and back")) : List.of("\u00a7rFront and back"), calls.next().getArgument(0));
    }
    @Test void signTextUsesSpigotEventApi() {
        SignChangeEvent event = mock(SignChangeEvent.class);
        PlatformCompat.signLine(event, 2, Component.text("Third line"));
        var call = mockingDetails(event).getInvocations().iterator().next();
        assertEquals(2, (int) call.getArgument(0));
        assertEquals(hasAdventure(SignChangeEvent.class, "line", int.class, Component.class)
                ? Component.text("Third line") : "Third line", call.getArgument(1));
    }
    private boolean hasAdventure(Class<?> api, String name, Class<?>... types) {
        try { api.getMethod(name, types); return true; }
        catch (NoSuchMethodException ex) { return false; }
    }
}
