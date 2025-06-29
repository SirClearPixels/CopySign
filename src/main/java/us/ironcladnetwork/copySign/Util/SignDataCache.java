package us.ironcladnetwork.copySign.Util;

import org.bukkit.Location;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * A thread-safe cache for pending sign NBT data.
 * Uses ConcurrentHashMap for thread safety without synchronization overhead.
 * Memory usage is managed through periodic cleanup by the plugin.
 */
public class SignDataCache {
    private static final Map<Location, SignData> pendingData = new ConcurrentHashMap<>();
    private static final Map<Location, Long> cacheTimestamps = new ConcurrentHashMap<>();
    
    // Cache TTL: 5 minutes (300,000 milliseconds)
    private static final long CACHE_TTL = 300000;

    /**
     * Stores sign data for the given location.
     * 
     * @param loc the location of the sign.
     * @param data the stored sign text data.
     */
    public static void put(Location loc, SignData data) {
        if (loc != null && data != null) {
            pendingData.put(loc, data);
            cacheTimestamps.put(loc, System.currentTimeMillis());
        }
    }

    /**
     * Retrieves the stored sign data for the given location without removing it.
     * 
     * @param loc the location of the sign.
     * @return the sign data, or null if none was stored.
     */
    public static SignData get(Location loc) {
        return pendingData.get(loc);
    }

    /**
     * Removes and returns the stored sign data for the given location.
     *
     * @param loc the location of the sign.
     * @return the sign data, or null if none was stored.
     */
    public static SignData remove(Location loc) {
        if (loc != null) {
            cacheTimestamps.remove(loc);
            return pendingData.remove(loc);
        }
        return null;
    }
    
    /**
     * Returns the current size of the cache.
     * 
     * @return number of entries in the cache
     */
    public static int size() {
        return pendingData.size();
    }
    
    /**
     * Clears all entries from the cache.
     */
    public static void clear() {
        pendingData.clear();
        cacheTimestamps.clear();
    }
    
    /**
     * Checks if the cache contains data for the given location.
     * 
     * @param loc the location to check
     * @return true if data exists for this location
     */
    public static boolean contains(Location loc) {
        return loc != null && pendingData.containsKey(loc);
    }
    
    /**
     * Atomically puts data only if no data exists for the location.
     * 
     * @param loc the location of the sign
     * @param data the sign data to store
     * @return the previous data if it existed, null otherwise
     */
    public static SignData putIfAbsent(Location loc, SignData data) {
        if (loc == null || data == null) return null;
        SignData result = pendingData.putIfAbsent(loc, data);
        if (result == null) {
            // Only set timestamp if we actually stored the data
            cacheTimestamps.put(loc, System.currentTimeMillis());
        }
        return result;
    }
    
    /**
     * Removes expired entries from the cache based on TTL.
     * This method is called periodically by the plugin's cleanup task.
     * Thread-safe implementation using ConcurrentHashMap.
     */
    public static void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        
        // Use removeIf for atomic removal during iteration
        cacheTimestamps.entrySet().removeIf(entry -> {
            Location loc = entry.getKey();
            long timestamp = entry.getValue();
            
            if ((now - timestamp) > CACHE_TTL) {
                // Remove the corresponding data entry as well
                pendingData.remove(loc);
                return true; // Remove this timestamp entry
            }
            return false;
        });
    }
    
    /**
     * Gets the timestamp when data was cached for the given location.
     * Used for debugging and monitoring purposes.
     * 
     * @param loc the location to check
     * @return timestamp in milliseconds, or null if not found
     */
    public static Long getCacheTimestamp(Location loc) {
        return cacheTimestamps.get(loc);
    }
    
    /**
     * Returns the number of expired entries without removing them.
     * Useful for monitoring cache health.
     * 
     * @return count of expired entries
     */
    public static int getExpiredEntriesCount() {
        long now = System.currentTimeMillis();
        return (int) cacheTimestamps.values().stream()
            .mapToLong(timestamp -> now - timestamp)
            .filter(age -> age > CACHE_TTL)
            .count();
    }

    /**
     * Container for front and back sign texts.
     */
    public static class SignData {
        private final String[] front;
        private final String[] back;
        private final boolean glowing;

        public SignData(String[] front, String[] back, boolean glowing) {
            this.front = front;
            this.back = back;
            this.glowing = glowing;
        }

        public String[] getFront() {
            return front;
        }

        public String[] getBack() {
            return back;
        }

        /** Returns whether the sign should be glowing */
        public boolean isGlowing() {
            return glowing;
        }
    }
} 