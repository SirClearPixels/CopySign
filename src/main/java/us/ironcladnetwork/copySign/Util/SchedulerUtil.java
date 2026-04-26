package us.ironcladnetwork.copySign.Util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Compatibility layer for scheduling tasks on both Paper and Folia.
 * <p>
 * Folia uses a regionized threading model where different regions of the world
 * run on different threads. This utility class provides methods that work on both
 * traditional Paper/Spigot servers and Folia servers.
 * <p>
 * The class automatically detects whether the server is running Folia by checking
 * for Folia-specific classes at startup.
 *
 * @author IroncladNetwork
 * @since 2.2.0
 */
public class SchedulerUtil {

    private static final boolean IS_FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        IS_FOLIA = folia;
    }

    /**
     * Checks if the server is running Folia.
     *
     * @return true if running on Folia, false otherwise
     */
    public static boolean isFolia() {
        return IS_FOLIA;
    }

    /**
     * Runs a task asynchronously.
     * Works on both Paper and Folia.
     *
     * @param plugin The plugin instance
     * @param task The task to run
     */
    public static void runAsync(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin, (t) -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    /**
     * Runs a task asynchronously after a delay.
     * Works on both Paper and Folia.
     *
     * @param plugin The plugin instance
     * @param task The task to run
     * @param delayTicks Delay in ticks (Paper) or converted to milliseconds (Folia)
     */
    public static void runAsyncDelayed(Plugin plugin, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            long delayMs = delayTicks * 50; // Convert ticks to milliseconds
            Bukkit.getAsyncScheduler().runDelayed(plugin, (t) -> task.run(), delayMs, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        }
    }

    /**
     * Runs a task asynchronously at a fixed rate.
     * Works on both Paper and Folia.
     *
     * @param plugin The plugin instance
     * @param task The task to run
     * @param delayTicks Initial delay in ticks
     * @param periodTicks Period between runs in ticks
     * @return A task object that can be used to cancel the task
     */
    public static Object runAsyncTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (IS_FOLIA) {
            long delayMs = delayTicks * 50; // Convert ticks to milliseconds
            long periodMs = periodTicks * 50;
            return Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (t) -> task.run(), delayMs, periodMs, TimeUnit.MILLISECONDS);
        } else {
            return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        }
    }

    /**
     * Runs a task on the global region scheduler.
     * On Paper, this runs on the main thread.
     * On Folia, this runs on the global region.
     *
     * @param plugin The plugin instance
     * @param task The task to run
     */
    public static void runGlobal(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().run(plugin, (t) -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Runs a task on the global region scheduler after a delay.
     * On Paper, this runs on the main thread.
     * On Folia, this runs on the global region.
     *
     * @param plugin The plugin instance
     * @param task The task to run
     * @param delayTicks Delay in ticks
     */
    public static void runGlobalDelayed(Plugin plugin, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (t) -> task.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Runs a task on the region that owns the specified location.
     * This is critical for block operations.
     * On Paper, this runs on the main thread.
     * On Folia, this runs on the region thread that owns the location.
     *
     * @param plugin The plugin instance
     * @param location The location that determines which region to run on
     * @param task The task to run
     */
    public static void runAtLocation(Plugin plugin, Location location, Runnable task) {
        if (IS_FOLIA) {
            Bukkit.getRegionScheduler().run(plugin, location, (t) -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Runs a task on the region that owns the specified location after a delay.
     * This is critical for delayed block operations.
     * On Paper, this runs on the main thread.
     * On Folia, this runs on the region thread that owns the location.
     *
     * @param plugin The plugin instance
     * @param location The location that determines which region to run on
     * @param task The task to run
     * @param delayTicks Delay in ticks
     */
    public static void runAtLocationDelayed(Plugin plugin, Location location, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            Bukkit.getRegionScheduler().runDelayed(plugin, location, (t) -> task.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Runs a task on the entity's scheduler.
     * This is critical for entity/player operations.
     * On Paper, this runs on the main thread.
     * On Folia, this runs on the region thread that owns the entity.
     *
     * @param plugin The plugin instance
     * @param entity The entity (e.g., player) that determines which region to run on
     * @param task The task to run
     */
    public static void runAtEntity(Plugin plugin, Entity entity, Runnable task) {
        if (IS_FOLIA) {
            entity.getScheduler().run(plugin, (t) -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Runs a task on the entity's scheduler after a delay.
     * This is critical for delayed entity/player operations.
     * On Paper, this runs on the main thread.
     * On Folia, this runs on the region thread that owns the entity.
     *
     * @param plugin The plugin instance
     * @param entity The entity (e.g., player) that determines which region to run on
     * @param task The task to run
     * @param delayTicks Delay in ticks
     * @return A task object that can be used to cancel the task
     */
    public static Object runAtEntityDelayed(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            return entity.getScheduler().runDelayed(plugin, (t) -> task.run(), null, delayTicks);
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Cancels a scheduled task.
     * Works with tasks returned by the timer methods.
     *
     * @param task The task object returned by a scheduling method
     */
    public static void cancelTask(Object task) {
        if (task == null) return;

        if (IS_FOLIA) {
            // Folia tasks implement io.papermc.paper.threadedregions.scheduler.ScheduledTask
            try {
                ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) task).cancel();
            } catch (Exception e) {
                // Task might already be cancelled or invalid
            }
        } else {
            // Paper/Spigot tasks are BukkitTask
            try {
                ((org.bukkit.scheduler.BukkitTask) task).cancel();
            } catch (Exception e) {
                // Task might already be cancelled or invalid
            }
        }
    }
}
