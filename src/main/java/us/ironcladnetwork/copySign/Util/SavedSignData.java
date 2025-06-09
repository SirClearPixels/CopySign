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
    private boolean glowing;
    private String frontColor;
    private String backColor;
    private String signType;
    private List<String> lore;

    /**
     * Constructs a SavedSignData instance.
     *
     * @param front      Array of strings for the sign's front text.
     * @param back       Array of strings for the sign's back text.
     * @param glowing    Whether the sign text should be glowing.
     * @param frontColor The color name for the front side.
     * @param backColor  The color name for the back side.
     * @param signType   The type of sign ("regular" or "hanging").
     * @param lore       Optional lore lines.
     */
    public SavedSignData(String[] front, String[] back, boolean glowing, String frontColor, String backColor, String signType, List<String> lore) {
        this.front = front;
        this.back = back;
        this.glowing = glowing;
        this.frontColor = frontColor;
        this.backColor = backColor;
        this.signType = signType;
        this.lore = lore;
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

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
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
        boolean glowing = section.getBoolean("glowing", false);
        String frontColor = section.getString("frontColor", "BLACK");
        String backColor = section.getString("backColor", "BLACK");
        String signType = section.getString("signType", "regular");
        List<String> lore = section.getStringList("lore");
        return new SavedSignData(front, back, glowing, frontColor, backColor, signType, lore);
    }

    /**
     * Saves this SavedSignData into the provided configuration section.
     *
     * @param section The configuration section to write data into.
     */
    public void saveToConfigurationSection(ConfigurationSection section) {
        // Combine front and back arrays into newline-delimited strings.
        StringBuilder frontBuilder = new StringBuilder();
        if (front != null) {
            for (int i = 0; i < front.length; i++) {
                frontBuilder.append(front[i]);
                if (i < front.length - 1)
                    frontBuilder.append("\n");
            }
        }
        StringBuilder backBuilder = new StringBuilder();
        if (back != null) {
            for (int i = 0; i < back.length; i++) {
                backBuilder.append(back[i]);
                if (i < back.length - 1)
                    backBuilder.append("\n");
            }
        }
        section.set("front", frontBuilder.toString());
        section.set("back", backBuilder.toString());
        section.set("glowing", glowing);
        section.set("frontColor", frontColor);
        section.set("backColor", backColor);
        section.set("signType", signType);
        section.set("lore", lore);
    }
} 