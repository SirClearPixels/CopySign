package us.ironcladnetwork.copySign.Util;

import us.ironcladnetwork.copySign.Util.PlatformCompat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.ironcladnetwork.copySign.CopySign;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/** Uses native Adventure on Paper/Folia and Bukkit strings on Spigot. */
public final class PlatformCompat {
    private PlatformCompat() {}
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('\u00a7').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private static boolean invoke(Class<?> api, Object target, String name, Class<?>[] types, Object... args) {
        try {
            Method method = api.getMethod(name, types);
            method.invoke(target, args);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        } catch (ReflectiveOperationException ex) {
            Throwable cause = ex instanceof InvocationTargetException ? ex.getCause() : ex;
            throw new IllegalStateException("Failed platform API call: " + name, cause);
        }
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (SchedulerUtil.isFolia() && sender instanceof Player player) {
            SchedulerUtil.runAtEntity(CopySign.getInstance(), player, () -> sender.sendMessage(message));
        } else sender.sendMessage(message);
    }
    public static void sendMessage(CommandSender sender, Component message) {
        if (SchedulerUtil.isFolia() && sender instanceof Player player) {
            SchedulerUtil.runAtEntity(CopySign.getInstance(), player, () -> sendComponent(sender, message));
        } else sendComponent(sender, message);
    }
    private static void sendComponent(CommandSender sender, Component message) {
        if (!invoke(CommandSender.class, sender, "sendMessage", new Class<?>[]{Component.class}, message)) {
            sender.sendMessage(LEGACY.serialize(message));
        }
    }
    public static void displayName(ItemMeta meta, Component name) {
        if (!invoke(ItemMeta.class, meta, "displayName", new Class<?>[]{Component.class}, name)) {
            meta.setDisplayName(name == null ? null : LEGACY.serialize(name));
        }
    }
    public static void lore(ItemMeta meta, List<Component> lore) {
        if (!invoke(ItemMeta.class, meta, "lore", new Class<?>[]{List.class}, lore)) {
            meta.setLore(lore == null ? null : lore.stream().map(c -> "\u00a7r" + LEGACY.serialize(c)).toList());
        }
    }
    public static Inventory createInventory(int size, Component title) {
        try {
            Method method = Bukkit.class.getMethod("createInventory", InventoryHolder.class, int.class, Component.class);
            return (Inventory) method.invoke(null, null, size, title);
        } catch (NoSuchMethodException ignored) {
            return Bukkit.createInventory(null, size, LEGACY.serialize(title));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to create inventory", ex);
        }
    }
    public static void signLine(SignChangeEvent event, int index, Component text) {
        if (!invoke(SignChangeEvent.class, event, "line", new Class<?>[]{int.class, Component.class}, index, text)) {
            event.setLine(index, LEGACY.serialize(text));
        }
    }
}
