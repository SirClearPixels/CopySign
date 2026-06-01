package us.ironcladnetwork.copySign.Util;

/**
 * Immutable value object describing the copied-sign payload that lives on a sign
 * <em>item</em> (the working copy a player carries between copy and paste).
 * <p>
 * This is deliberately decoupled from how the payload is physically stored on the
 * item. {@link SignItemStorage} is the single place that maps between this object
 * and an item's {@link org.bukkit.persistence.PersistentDataContainer} (with a
 * one-version fallback to legacy NBT-API tags via {@link LegacyNbtBridge}).
 * <p>
 * Text fields are stored as newline-delimited strings exactly as the legacy NBT
 * format used them, so the two storage backends are wire-compatible. Color fields
 * are {@link org.bukkit.DyeColor} names, or {@code null} when no color was copied
 * (e.g. the player lacked {@code copysign.copycolor}).
 *
 * @since 2.4.0
 */
public final class SignItemData {

    private final String front;
    private final String back;
    private final String frontColor; // DyeColor name, or null when absent
    private final String backColor;  // DyeColor name, or null when absent
    private final boolean frontGlowing;
    private final boolean backGlowing;
    private final String signType;   // "regular" or "hanging"

    /**
     * @param front        Newline-delimited front text (must not be null).
     * @param back         Newline-delimited back text (must not be null).
     * @param frontColor   Front {@link org.bukkit.DyeColor} name, or null if no color.
     * @param backColor    Back {@link org.bukkit.DyeColor} name, or null if no color.
     * @param frontGlowing Whether the front side glows.
     * @param backGlowing  Whether the back side glows.
     * @param signType     "regular" or "hanging".
     */
    public SignItemData(String front, String back, String frontColor, String backColor,
                        boolean frontGlowing, boolean backGlowing, String signType) {
        this.front = front == null ? "" : front;
        this.back = back == null ? "" : back;
        this.frontColor = frontColor;
        this.backColor = backColor;
        this.frontGlowing = frontGlowing;
        this.backGlowing = backGlowing;
        this.signType = signType == null ? "regular" : signType;
    }

    public String getFront() {
        return front;
    }

    public String getBack() {
        return back;
    }

    /** @return the front color name, or {@code null} if no color was copied. */
    public String getFrontColor() {
        return frontColor;
    }

    /** @return the back color name, or {@code null} if no color was copied. */
    public String getBackColor() {
        return backColor;
    }

    public boolean hasFrontColor() {
        return frontColor != null;
    }

    public boolean hasBackColor() {
        return backColor != null;
    }

    public boolean isFrontGlowing() {
        return frontGlowing;
    }

    public boolean isBackGlowing() {
        return backGlowing;
    }

    /** @return combined legacy glow flag (true if either side glows). */
    public boolean isGlowing() {
        return frontGlowing || backGlowing;
    }

    public String getSignType() {
        return signType;
    }

    public boolean isHanging() {
        return "hanging".equalsIgnoreCase(signType);
    }
}
