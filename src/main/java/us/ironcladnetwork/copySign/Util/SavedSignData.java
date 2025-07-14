package us.ironcladnetwork.copySign.Util;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * POJO representing the saved sign data.
 * <p>
 * Encapsulates the sign's front/back text (as arrays of lines), glow state,
 * side colors, sign type and optional lore.
 */
public class SavedSignData {

    private String[] front;
    private String[] back;
    private boolean glowing; // Legacy field - kept for backwards compatibility
    private boolean frontGlowing;
    private boolean backGlowing;
    private String frontColor;
    private String backColor;
    private String signType;
    private List<String> lore;

    /**
     * Constructs a SavedSignData instance with per-side glow states.
     *
     * @param front        Array of strings for the sign's front text.
     * @param back         Array of strings for the sign's back text.
     * @param frontGlowing Whether the front side should be glowing.
     * @param backGlowing  Whether the back side should be glowing.
     * @param frontColor   The color name for the front side.
     * @param backColor    The color name for the back side.
     * @param signType     The type of sign ("regular" or "hanging").
     * @param lore         Optional lore lines.
     */
    public SavedSignData(String[] front, String[] back, boolean frontGlowing, boolean backGlowing, String frontColor, String backColor, String signType, List<String> lore) {
        this.front = front;
        this.back = back;
        this.frontGlowing = frontGlowing;
        this.backGlowing = backGlowing;
        this.glowing = frontGlowing || backGlowing; // Legacy compatibility
        this.frontColor = frontColor;
        this.backColor = backColor;
        this.signType = signType;
        this.lore = lore;
    }

    /**
     * Constructs a SavedSignData instance with legacy single glow state.
     * @deprecated Use the constructor with per-side glow states instead.
     *
     * @param front      Array of strings for the sign's front text.
     * @param back       Array of strings for the sign's back text.
     * @param glowing    Whether the sign text should be glowing.
     * @param frontColor The color name for the front side.
     * @param backColor  The color name for the back side.
     * @param signType   The type of sign ("regular" or "hanging").
     * @param lore       Optional lore lines.
     */
    @Deprecated
    public SavedSignData(String[] front, String[] back, boolean glowing, String frontColor, String backColor, String signType, List<String> lore) {
        this(front, back, glowing, glowing, frontColor, backColor, signType, lore);
    }

    public String[] getFront() {
        return front;
    }

    public void setFront(String[] front) {
        this.front = front;
    }

    public String[] getBack() {
        return back;
    }

    public void setBack(String[] back) {
        this.back = back;
    }

    /**
     * @deprecated Use isFrontGlowing() and isBackGlowing() instead.
     */
    @Deprecated
    public boolean isGlowing() {
        return glowing;
    }

    /**
     * @deprecated Use setFrontGlowing() and setBackGlowing() instead.
     */
    @Deprecated
    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
        this.frontGlowing = glowing;
        this.backGlowing = glowing;
    }

    /**
     * Gets the glow state of the front side.
     * 
     * @return true if the front side is glowing
     */
    public boolean isFrontGlowing() {
        return frontGlowing;
    }

    /**
     * Sets the glow state of the front side.
     * 
     * @param frontGlowing true to make the front side glow
     */
    public void setFrontGlowing(boolean frontGlowing) {
        this.frontGlowing = frontGlowing;
        this.glowing = this.frontGlowing || this.backGlowing; // Update legacy field
    }

    /**
     * Gets the glow state of the back side.
     * 
     * @return true if the back side is glowing
     */
    public boolean isBackGlowing() {
        return backGlowing;
    }

    /**
     * Sets the glow state of the back side.
     * 
     * @param backGlowing true to make the back side glow
     */
    public void setBackGlowing(boolean backGlowing) {
        this.backGlowing = backGlowing;
        this.glowing = this.frontGlowing || this.backGlowing; // Update legacy field
    }

    /**
     * Checks if the sign has mixed glow states (one side glowing, the other not).
     * 
     * @return true if glow states differ between sides
     */
    public boolean hasMixedGlowStates() {
        return frontGlowing != backGlowing;
    }

    public String getFrontColor() {
        return frontColor;
    }

    public void setFrontColor(String frontColor) {
        this.frontColor = frontColor;
    }

    public String getBackColor() {
        return backColor;
    }

    public void setBackColor(String backColor) {
        this.backColor = backColor;
    }

    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    /**
     * Loads and constructs a SavedSignData from the provided configuration section.
     *
     * @param section The configuration section containing the saved sign data.
     * @return A new SavedSignData instance.
     */
    public static SavedSignData loadFromConfigurationSection(ConfigurationSection section) {
        String frontText = section.getString("front", "");
        String backText = section.getString("back", "");
        String[] front = frontText.split("\n");
        String[] back = backText.split("\n");
        
        // Load per-side glow states, falling back to legacy single glow state
        boolean frontGlowing, backGlowing;
        if (section.contains("frontGlowing") || section.contains("backGlowing")) {
            // New format with per-side glow states
            frontGlowing = section.getBoolean("frontGlowing", false);
            backGlowing = section.getBoolean("backGlowing", false);
        } else {
            // Legacy format with single glow state
            boolean legacyGlowing = section.getBoolean("glowing", false);
            frontGlowing = legacyGlowing;
            backGlowing = legacyGlowing;
        }
        
        String frontColor = section.getString("frontColor", "BLACK");
        String backColor = section.getString("backColor", "BLACK");
        String signType = section.getString("signType", "regular");
        List<String> lore = section.getStringList("lore");
        return new SavedSignData(front, back, frontGlowing, backGlowing, frontColor, backColor, signType, lore);
    }

    /**
     * Saves this SavedSignData into the provided configuration section.
     *
     * @param section The configuration section to write data into.
     */
    public void saveToConfigurationSection(ConfigurationSection section) {
        // Combine front and back arrays into newline-delimited strings with optimized StringBuilder capacity.
        
        // Calculate estimated capacity for front text
        int frontCapacity = calculateStringCapacity(front);
        StringBuilder frontBuilder = new StringBuilder(frontCapacity);
        if (front != null) {
            for (int i = 0; i < front.length; i++) {
                frontBuilder.append(front[i]);
                if (i < front.length - 1)
                    frontBuilder.append("\n");
            }
        }
        
        // Calculate estimated capacity for back text
        int backCapacity = calculateStringCapacity(back);
        StringBuilder backBuilder = new StringBuilder(backCapacity);
        if (back != null) {
            for (int i = 0; i < back.length; i++) {
                backBuilder.append(back[i]);
                if (i < back.length - 1)
                    backBuilder.append("\n");
            }
        }
        section.set("front", frontBuilder.toString());
        section.set("back", backBuilder.toString());
        
        // Save both legacy and new glow state formats for compatibility
        section.set("glowing", glowing); // Legacy field
        section.set("frontGlowing", frontGlowing);
        section.set("backGlowing", backGlowing);
        
        section.set("frontColor", frontColor);
        section.set("backColor", backColor);
        section.set("signType", signType);
        section.set("lore", lore);
    }
    
    /**
     * Calculates the estimated capacity needed for a StringBuilder when combining sign lines.
     * This prevents unnecessary array reallocations during string building.
     * 
     * @param lines The array of lines to estimate capacity for
     * @return The estimated capacity needed
     */
    private int calculateStringCapacity(String[] lines) {
        if (lines == null || lines.length == 0) {
            return 16; // Default small capacity
        }
        
        int totalLength = 0;
        for (String line : lines) {
            if (line != null) {
                totalLength += line.length();
            }
        }
        
        // Add space for newline characters (lines.length - 1) plus some buffer (20%)
        int newlineCount = Math.max(0, lines.length - 1);
        int estimatedCapacity = totalLength + newlineCount;
        
        // Add 20% buffer to avoid frequent reallocations
        return (int) (estimatedCapacity * 1.2);
    }
} 