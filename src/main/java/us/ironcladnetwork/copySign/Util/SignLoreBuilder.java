package us.ironcladnetwork.copySign.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced utility class for building premium, consistent sign lore across the plugin.
 * Centralizes lore formatting with modern design standards and per-side glow state support.
 * 
 * @author IroncladNetwork
 * @since 2.1.1 (Enhanced)
 */
public class SignLoreBuilder {
    
    /**
     * Builds premium lore for a copied sign item with enhanced formatting.
     * @deprecated Use buildPremiumSignLore() for the new enhanced format.
     */
    @Deprecated
    public static List<String> buildSignLore(String[] frontLines, String[] backLines, 
                                            String frontColor, String backColor, 
                                            boolean isGlowing, String signType) {
        return buildPremiumSignLore("Copied Sign", frontLines, backLines, frontColor, backColor, 
                                  isGlowing, isGlowing, signType, "Copied");
    }
    
    /**
     * Builds premium lore for a sign item with per-side glow states and advanced formatting.
     * 
     * @param itemName The display name for the item
     * @param frontLines The text lines on the front of the sign
     * @param backLines The text lines on the back of the sign (can be null)
     * @param frontColor The color of the front side
     * @param backColor The color of the back side (can be null)
     * @param frontGlowing Whether the front side is glowing
     * @param backGlowing Whether the back side is glowing
     * @param signType The type of sign (e.g., "regular", "hanging")
     * @param sourceType The source type ("Copied", "Template", "Library")
     * @return A formatted premium lore list
     */
    public static List<String> buildPremiumSignLore(String itemName, String[] frontLines, String[] backLines, 
                                                   String frontColor, String backColor, 
                                                   boolean frontGlowing, boolean backGlowing, 
                                                   String signType, String sourceType) {
        List<String> lore = new ArrayList<>();
        
        // Premium header with separators - ONLY content name, no physical item duplication
        lore.add(DesignConstants.SEPARATOR_TOP);
        if (itemName != null && !itemName.isEmpty()) {
            lore.add(DesignConstants.HEADER_PRIMARY + itemName);
        }
        lore.add(DesignConstants.SEPARATOR_SECTION);
        
        // Front side with enhanced formatting
        boolean hasFrontContent = hasContent(frontLines);
        lore.add(DesignConstants.formatSideHeader("Front", hasFrontContent, frontGlowing));
        addPremiumSideToLore(lore, frontLines, frontColor, hasFrontContent);
        
        // Back side with enhanced formatting
        if (backLines != null) {
            lore.add(""); // Spacing
            boolean hasBackContent = hasContent(backLines);
            lore.add(DesignConstants.formatSideHeader("Back", hasBackContent, backGlowing));
            addPremiumSideToLore(lore, backLines, backColor, hasBackContent);
        }
        
        // Properties section
        lore.add(""); // Spacing
        lore.add(DesignConstants.SEPARATOR_SECTION);
        lore.add(DesignConstants.LABEL_PROPERTY + "Properties");
        
        if (signType != null && !signType.isEmpty()) {
            String formattedType = formatSignType(signType);
            lore.add(DesignConstants.formatProperty("Type", formattedType, !hasSourceType(sourceType)));
        }
        
        if (hasSourceType(sourceType)) {
            lore.add(DesignConstants.formatProperty("Source", sourceType, true));
        }
        
        // Mixed glow state warning with enhanced visual indicator
        if (frontGlowing != backGlowing && backLines != null) {
            lore.add("");
            lore.add(DesignConstants.createMixedGlowWarning());
        }
        
        lore.add(DesignConstants.SEPARATOR_BOTTOM);
        
        return lore;
    }
    
    
    /**
     * Builds lore for a saved sign in the library.
     * @deprecated Use buildPremiumLibrarySignLore() for the new enhanced format.
     */
    @Deprecated
    public static List<String> buildLibrarySignLore(SavedSignData savedSignData, String name) {
        return buildPremiumLibrarySignLore(savedSignData, name);
    }
    
    /**
     * Builds premium lore for a saved sign in the library with enhanced formatting.
     * 
     * @param savedSignData The saved sign data
     * @param name The name of the saved sign
     * @return A formatted premium lore list
     */
    public static List<String> buildPremiumLibrarySignLore(SavedSignData savedSignData, String name) {
        return buildPremiumSignLore(
            name != null ? name : "Library Sign",
            savedSignData.getFront(),
            savedSignData.getBack(),
            savedSignData.getFrontColor(),
            savedSignData.getBackColor(),
            savedSignData.isFrontGlowing(),
            savedSignData.isBackGlowing(),
            savedSignData.getSignType(),
            "Library"
        );
    }
    
    /**
     * Builds premium lore for GUI items without header duplication.
     * Used when the item already has a display name set.
     * 
     * @param frontLines The text lines on the front of the sign
     * @param backLines The text lines on the back of the sign (can be null)
     * @param frontColor The color of the front side
     * @param backColor The color of the back side (can be null)
     * @param frontGlowing Whether the front side is glowing
     * @param backGlowing Whether the back side is glowing
     * @param signType The type of sign (e.g., "regular", "hanging")
     * @param sourceType The source type ("Copied", "Template", "Library")
     * @return A formatted premium lore list without header
     */
    public static List<String> buildPremiumSignLoreForGUI(String[] frontLines, String[] backLines, 
                                                          String frontColor, String backColor, 
                                                          boolean frontGlowing, boolean backGlowing, 
                                                          String signType, String sourceType) {
        List<String> lore = new ArrayList<>();
        
        // Start with content separator (no header duplication)
        lore.add(DesignConstants.SEPARATOR_SECTION);
        
        // Front side with enhanced formatting
        boolean hasFrontContent = hasContent(frontLines);
        lore.add(DesignConstants.formatSideHeader("Front", hasFrontContent, frontGlowing));
        addPremiumSideToLore(lore, frontLines, frontColor, hasFrontContent);
        
        // Back side with enhanced formatting
        if (backLines != null) {
            lore.add(""); // Spacing
            boolean hasBackContent = hasContent(backLines);
            lore.add(DesignConstants.formatSideHeader("Back", hasBackContent, backGlowing));
            addPremiumSideToLore(lore, backLines, backColor, hasBackContent);
        }
        
        // Properties section
        lore.add(""); // Spacing
        lore.add(DesignConstants.SEPARATOR_SECTION);
        lore.add(DesignConstants.LABEL_PROPERTY + "Properties");
        
        
        if (signType != null && !signType.isEmpty()) {
            String formattedType = formatSignType(signType);
            lore.add(DesignConstants.formatProperty("Type", formattedType, !hasSourceType(sourceType)));
        }
        
        if (hasSourceType(sourceType)) {
            lore.add(DesignConstants.formatProperty("Source", sourceType, true));
        }
        
        // Mixed glow state warning with enhanced visual indicator
        if (frontGlowing != backGlowing && backLines != null) {
            lore.add("");
            lore.add(DesignConstants.createMixedGlowWarning());
        }
        
        lore.add(DesignConstants.SEPARATOR_BOTTOM);
        
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
     * @deprecated Use ColorAnalyzer.analyzeColor() for enhanced color detection.
     * 
     * @param color The color to format
     * @return A formatted color string
     */
    @Deprecated
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
    
    // ===== PREMIUM HELPER METHODS =====
    
    /**
     * Adds a side (front or back) to the lore with premium formatting and visual enhancements.
     * 
     * @param lore The lore list to add to
     * @param lines The text lines for this side
     * @param color The color of this side
     * @param hasContent Whether this side has content
     */
    private static void addPremiumSideToLore(List<String> lore, String[] lines, String color, boolean hasContent) {
        if (!hasContent) {
            lore.add(DesignConstants.TREE_LAST + DesignConstants.SUPPORTING + "No content available");
            return;
        }
        
        // Add content lines with responsive sizing
        List<String> contentLines = new ArrayList<>();
        int maxContentLength = 25; // Responsive content length
        
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] != null && !lines[i].isEmpty()) {
                String responsiveContent = DesignConstants.createResponsiveContent(lines[i], maxContentLength);
                String connector = (i == lines.length - 1 || isLastNonEmptyLine(lines, i)) ? 
                                  DesignConstants.TREE_LAST : DesignConstants.TREE_BRANCH;
                contentLines.add(connector + DesignConstants.SUPPORTING + "Line " + (i + 1) + ": " + responsiveContent);
            }
        }
        
        // Add enhanced color information with visual preview
        String colorStatus = ColorAnalyzer.createEnhancedColorAnalysis(lines);
        String colorLine = DesignConstants.TREE_LAST + DesignConstants.SUPPORTING + "Color: " + colorStatus;
        
        // Add all content lines
        for (String line : contentLines) {
            lore.add(line);
        }
        
        // Add color as the last item
        lore.add(colorLine);
    }
    
    /**
     * Checks if the given lines contain any non-empty content.
     * 
     * @param lines The lines to check
     * @return true if any line has content
     */
    private static boolean hasContent(String[] lines) {
        if (lines == null || lines.length == 0) {
            return false;
        }
        
        for (String line : lines) {
            if (line != null && !line.trim().isEmpty()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Formats a sign type for display.
     * 
     * @param signType The raw sign type
     * @return Formatted sign type
     */
    private static String formatSignType(String signType) {
        if (signType == null || signType.isEmpty()) {
            return "Unknown";
        }
        
        switch (signType.toLowerCase()) {
            case "regular":
            case "normal":
                return "Regular Sign";
            case "hanging":
                return "Hanging Sign";
            default:
                // Capitalize first letter
                return signType.substring(0, 1).toUpperCase() + signType.substring(1).toLowerCase() + " Sign";
        }
    }
    
    /**
     * Checks if the source type should be displayed.
     * 
     * @param sourceType The source type
     * @return true if the source type is not null or empty
     */
    private static boolean hasSourceType(String sourceType) {
        return sourceType != null && !sourceType.trim().isEmpty();
    }
    
    /**
     * Checks if the current line index is the last non-empty line in the array.
     * 
     * @param lines The array of lines
     * @param currentIndex The current line index
     * @return true if this is the last non-empty line
     */
    private static boolean isLastNonEmptyLine(String[] lines, int currentIndex) {
        for (int i = currentIndex + 1; i < lines.length; i++) {
            if (lines[i] != null && !lines[i].isEmpty()) {
                return false;
            }
        }
        return true;
    }
}