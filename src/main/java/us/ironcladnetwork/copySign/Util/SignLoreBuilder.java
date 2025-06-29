package us.ironcladnetwork.copySign.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building consistent sign lore across the plugin.
 * Centralizes lore formatting to ensure consistency and reduce duplication.
 */
public class SignLoreBuilder {
    
    /**
     * Builds lore for a copied sign item.
     * 
     * @param frontLines The text lines on the front of the sign
     * @param backLines The text lines on the back of the sign (can be null for pre-1.20)
     * @param frontColor The color of the front side
     * @param backColor The color of the back side (can be null)
     * @param isGlowing Whether the sign has glowing text
     * @param signType The type of sign (e.g., "regular", "hanging")
     * @return A formatted lore list
     */
    public static List<String> buildSignLore(String[] frontLines, String[] backLines, 
                                            String frontColor, String backColor, 
                                            boolean isGlowing, String signType) {
        List<String> lore = new ArrayList<>();
        lore.add("§f§l[§b§lCopied Sign§f§l]");
        
        // Add sign type if specified
        if (signType != null && !signType.isEmpty()) {
            lore.add("§7Type: " + signType);
        }
        
        // Handle front side
        addSideToLore(lore, frontLines, frontColor, "Front");
        
        // Handle back side (if applicable)
        if (backLines != null) {
            addSideToLore(lore, backLines, backColor, "Back");
        }
        
        // Always show glowing state
        lore.add("§e§lGlowing: " + (isGlowing ? "§aTrue" : "§cFalse"));
        
        return lore;
    }
    
    /**
     * Builds lore for a saved sign in the library.
     * 
     * @param savedSignData The saved sign data
     * @param name The name of the saved sign
     * @return A formatted lore list
     */
    public static List<String> buildLibrarySignLore(SavedSignData savedSignData, String name) {
        List<String> lore = new ArrayList<>();
        
        // Add name if provided
        if (name != null && !name.isEmpty()) {
            lore.add("§e§lName: §f" + name);
        }
        
        lore.add("§f§l[§b§lSaved Sign§f§l]");
        
        // Add sign type
        String signType = savedSignData.getSignType();
        if (signType != null && !signType.isEmpty()) {
            lore.add("§7Type: " + signType);
        }
        
        // Handle front side
        addSideToLore(lore, savedSignData.getFront(), savedSignData.getFrontColor(), "Front");
        
        // Handle back side (if applicable)
        String[] backLines = savedSignData.getBack();
        if (backLines != null) {
            addSideToLore(lore, backLines, savedSignData.getBackColor(), "Back");
        }
        
        // Show glowing state
        lore.add("§e§lGlowing: " + (savedSignData.isGlowing() ? "§aTrue" : "§cFalse"));
        
        return lore;
    }
    
    /**
     * Adds a side (front or back) to the lore.
     * 
     * @param lore The lore list to add to
     * @param lines The text lines for this side
     * @param color The color of this side
     * @param sideName The name of the side ("Front" or "Back")
     */
    private static void addSideToLore(List<String> lore, String[] lines, String color, String sideName) {
        if (lines == null) {
            return;
        }
        
        boolean hasSideData = false;
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].isEmpty()) {
                if (!hasSideData) {
                    lore.add("§f§l" + sideName + ":");
                    if (color != null && !color.isEmpty()) {
                        lore.add("§f§lColor: " + formatColor(color));
                    }
                    hasSideData = true;
                }
                lore.add("§f§lLine " + (i + 1) + ": §f\"§b" + lines[i] + "§f\"");
            }
        }
        
        // If no data on this side, indicate it's empty
        if (!hasSideData) {
            lore.add("§f§l" + sideName + ": §7(empty)");
        }
    }
    
    /**
     * Formats a color string for display.
     * 
     * @param color The color to format
     * @return A formatted color string
     */
    private static String formatColor(String color) {
        if (color == null || color.isEmpty()) {
            return "§7None";
        }
        
        // Convert to title case
        String formatted = color.toLowerCase().replace('_', ' ');
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1));
        }
        
        return result.toString();
    }
    
    /**
     * Adds a simple preview of sign text to lore.
     * Used for quick previews without full formatting.
     * 
     * @param lore The lore list to add to
     * @param lines The text lines to preview
     * @param maxLines Maximum number of lines to show
     */
    public static void addSimplePreview(List<String> lore, String[] lines, int maxLines) {
        if (lines == null) {
            return;
        }
        
        lore.add("§7Preview:");
        int shown = 0;
        for (int i = 0; i < lines.length && shown < maxLines; i++) {
            if (!lines[i].isEmpty()) {
                lore.add("§7- " + lines[i]);
                shown++;
            }
        }
        
        if (shown == 0) {
            lore.add("§7(empty)");
        }
    }
}