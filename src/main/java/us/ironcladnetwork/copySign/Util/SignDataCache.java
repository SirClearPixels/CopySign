package us.ironcladnetwork.copySign.Util;

import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple cache for pending sign NBT data.
 */
public class SignDataCache {
    private static final Map<Location, SignData> pendingData = new HashMap<>();

    /**
     * Stores sign data for the given location.
     * 
     * @param loc the location of the sign.
     * @param data the stored sign text data.
     */
    public static void put(Location loc, SignData data) {
        pendingData.put(loc, data);
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
        return pendingData.remove(loc);
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