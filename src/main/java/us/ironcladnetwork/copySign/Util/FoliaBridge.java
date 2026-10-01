package us.ironcladnetwork.copySign.Util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Keeps Folia types out of Spigot's class linkage. */
final class FoliaBridge {
    private final Class<?> api;
    private final Object scheduler;
    private FoliaBridge(String apiName, Object scheduler) {
        try { this.api = Class.forName("io.papermc.paper.threadedregions.scheduler." + apiName); }
        catch (ClassNotFoundException ex) { throw new IllegalStateException("Missing Folia scheduler", ex); }
        this.scheduler = scheduler;
    }
    private static Object call(Class<?> api, Object target, String name, Class<?>[] types, Object... args) {
        try { return api.getMethod(name, types).invoke(target, args); }
        catch (ReflectiveOperationException ex) {
            Throwable cause = ex instanceof InvocationTargetException ? ex.getCause() : ex;
            throw new IllegalStateException("Folia scheduler call failed: " + name, cause);
        }
    }
    static FoliaBridge async() { return global("getAsyncScheduler", "AsyncScheduler"); }
    static FoliaBridge global() { return global("getGlobalRegionScheduler", "GlobalRegionScheduler"); }
    static FoliaBridge region() { return global("getRegionScheduler", "RegionScheduler"); }
    private static FoliaBridge global(String getter, String api) {
        return new FoliaBridge(api, call(Bukkit.class, null, getter, new Class<?>[0]));
    }
    static FoliaBridge entity(Entity entity) {
        return new FoliaBridge("EntityScheduler", call(Entity.class, entity, "getScheduler", new Class<?>[0]));
    }
    Object runNow(Plugin p, Consumer<Object> c) { return call(api, scheduler, "runNow", new Class<?>[]{Plugin.class, Consumer.class}, p, c); }
    Object run(Plugin p, Consumer<Object> c) { return call(api, scheduler, "run", new Class<?>[]{Plugin.class, Consumer.class}, p, c); }
    Object run(Plugin p, Location l, Consumer<Object> c) { return call(api, scheduler, "run", new Class<?>[]{Plugin.class, Location.class, Consumer.class}, p, l, c); }
    Object run(Plugin p, Consumer<Object> c, Runnable retired) { return call(api, scheduler, "run", new Class<?>[]{Plugin.class, Consumer.class, Runnable.class}, p, c, retired); }
    Object runDelayed(Plugin p, Consumer<Object> c, long t) { return call(api, scheduler, "runDelayed", new Class<?>[]{Plugin.class, Consumer.class, long.class}, p, c, Math.max(1L, t)); }
    Object runDelayed(Plugin p, Consumer<Object> c, long t, TimeUnit unit) { return call(api, scheduler, "runDelayed", new Class<?>[]{Plugin.class, Consumer.class, long.class, TimeUnit.class}, p, c, Math.max(0L, t), unit); }
    Object runDelayed(Plugin p, Location l, Consumer<Object> c, long t) { return call(api, scheduler, "runDelayed", new Class<?>[]{Plugin.class, Location.class, Consumer.class, long.class}, p, l, c, Math.max(1L, t)); }
    Object runDelayed(Plugin p, Consumer<Object> c, Runnable retired, long t) { return call(api, scheduler, "runDelayed", new Class<?>[]{Plugin.class, Consumer.class, Runnable.class, long.class}, p, c, retired, Math.max(1L, t)); }
    Object runAtFixedRate(Plugin p, Consumer<Object> c, long delay, long period, TimeUnit unit) { return call(api, scheduler, "runAtFixedRate", new Class<?>[]{Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class}, p, c, Math.max(1L, delay), Math.max(1L, period), unit); }
    static void cancel(Object task) { new FoliaBridge("ScheduledTask", task).cancel(); }
    private void cancel() { call(api, scheduler, "cancel", new Class<?>[0]); }
}
