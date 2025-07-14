package us.ironcladnetwork.copySign.Util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced color detection and analysis utility for sign text.
 * Handles both single-color and multi-color sign detection with accurate formatting.
 * 
 * @author IroncladNetwork
 * @since 2.1.1
 */
public final class ColorAnalyzer {
    
    /**
     * Pattern to match Minecraft color codes (§0-9, §a-f, §k-o, §r)
     */
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("§([0-9a-fk-or])");
    
    /**
     * Pattern to match only color codes (§0-9, §a-f), excluding formatting codes
     */
    private static final Pattern PURE_COLOR_PATTERN = Pattern.compile("§([0-9a-f])");
    
    /**
     * Analyzes the color content of sign lines and returns appropriate display string.
     * 
     * @param lines Array of sign lines to analyze
     * @return Formatted color status string for display
     */
    public static String analyzeColor(String[] lines) {
        if (lines == null || lines.length == 0) {
            return DesignConstants.NO_COLOR;
        }
        
        Set<String> detectedColors = new HashSet<>();
        boolean hasColorCodes = false;
        boolean hasNonColorFormatting = false;
        
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            
            // Check for any color codes
            Matcher colorMatcher = COLOR_CODE_PATTERN.matcher(line);
            while (colorMatcher.find()) {
                hasColorCodes = true;
                String code = colorMatcher.group(1).toLowerCase();
                
                // Separate pure colors from formatting codes
                if (code.matches("[0-9a-f]")) {
                    detectedColors.add(code);
                } else {
                    hasNonColorFormatting = true;
                }
            }
        }
        
        // No color codes found
        if (!hasColorCodes) {
            return DesignConstants.DEFAULT_COLOR;
        }
        
        // No pure color codes, only formatting
        if (detectedColors.isEmpty()) {
            return DesignConstants.SUPPORTING + "Formatted";
        }
        
        // Single color detected
        if (detectedColors.size() == 1) {
            String singleColor = detectedColors.iterator().next();
            String colorName = formatSingleColor(singleColor);
            
            // Add formatting indicator if present
            if (hasNonColorFormatting) {
                return DesignConstants.CLEAN_TEXT + colorName + DesignConstants.SUPPORTING + " (Formatted)";
            }
            return DesignConstants.CLEAN_TEXT + colorName;
        }
        
        // Multiple colors detected
        return DesignConstants.createMultiColorPreview();
    }
    
    /**
     * Analyzes a single line for color complexity.
     * 
     * @param line The line to analyze
     * @return Color complexity result
     */
    public static ColorComplexity analyzeLineComplexity(String line) {
        if (line == null || line.isEmpty()) {
            return ColorComplexity.NONE;
        }
        
        Set<String> colors = new HashSet<>();
        Matcher matcher = PURE_COLOR_PATTERN.matcher(line);
        
        while (matcher.find()) {
            colors.add(matcher.group(1).toLowerCase());
        }
        
        if (colors.isEmpty()) return ColorComplexity.NONE;
        if (colors.size() == 1) return ColorComplexity.SINGLE;
        return ColorComplexity.MULTI;
    }
    
    /**
     * Formats a single color code into a readable color name.
     * 
     * @param colorCode The color code character (0-9, a-f)
     * @return Formatted color name
     */
    public static String formatSingleColor(String colorCode) {
        switch (colorCode.toLowerCase()) {
            case "0": return "Black";
            case "1": return "Dark Blue";
            case "2": return "Dark Green";
            case "3": return "Dark Aqua";
            case "4": return "Dark Red";
            case "5": return "Dark Purple";
            case "6": return "Gold";
            case "7": return "Gray";
            case "8": return "Dark Gray";
            case "9": return "Blue";
            case "a": return "Green";
            case "b": return "Aqua";
            case "c": return "Red";
            case "d": return "Light Purple";
            case "e": return "Yellow";
            case "f": return "White";
            default: return "Unknown";
        }
    }
    
    /**
     * Gets the Minecraft color code for a color name.
     * 
     * @param colorName The color name
     * @return The color code or empty string if not found
     */
    public static String getColorCode(String colorName) {
        switch (colorName.toLowerCase()) {
            case "black": return "§0";
            case "dark_blue": return "§1";
            case "dark_green": return "§2";
            case "dark_aqua": return "§3";
            case "dark_red": return "§4";
            case "dark_purple": return "§5";
            case "gold": return "§6";
            case "gray": return "§7";
            case "dark_gray": return "§8";
            case "blue": return "§9";
            case "green": return "§a";
            case "aqua": return "§b";
            case "red": return "§c";
            case "light_purple": return "§d";
            case "yellow": return "§e";
            case "white": return "§f";
            default: return "";
        }
    }
    
    /**
     * Checks if the given lines contain multiple colors per line.
     * 
     * @param lines Array of lines to check
     * @return true if any line contains multiple colors
     */
    public static boolean hasMultiColorLines(String[] lines) {
        if (lines == null) return false;
        
        for (String line : lines) {
            if (analyzeLineComplexity(line) == ColorComplexity.MULTI) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets all unique colors used across all lines.
     * 
     * @param lines Array of lines to analyze
     * @return Set of color codes found
     */
    public static Set<String> getAllColors(String[] lines) {
        Set<String> allColors = new HashSet<>();
        
        if (lines == null) return allColors;
        
        for (String line : lines) {
            if (line == null) continue;
            
            Matcher matcher = PURE_COLOR_PATTERN.matcher(line);
            while (matcher.find()) {
                allColors.add(matcher.group(1).toLowerCase());
            }
        }
        
        return allColors;
    }
    
    /**
     * Creates a color preview string with enhanced visual indicators.
     * 
     * @param lines Array of lines to create preview for
     * @return Formatted color preview string with visual samples
     */
    public static String createColorPreview(String[] lines) {
        Set<String> colors = getAllColors(lines);
        
        if (colors.isEmpty()) {
            return DesignConstants.NO_COLOR;
        }
        
        if (colors.size() == 1) {
            String colorCode = colors.iterator().next();
            String colorName = formatSingleColor(colorCode);
            return DesignConstants.createColorPreview("§" + colorCode, colorName);
        }
        
        StringBuilder preview = new StringBuilder();
        int count = 0;
        for (String colorCode : colors) {
            if (count > 0) preview.append(" ");
            preview.append("§").append(colorCode).append(DesignConstants.COLOR_BLOCK);
            count++;
            if (count >= 5) break; // Limit to 5 color samples
        }
        preview.append(" ").append(DesignConstants.createMultiColorPreview());
        
        return preview.toString();
    }
    
    /**
     * Creates an enhanced color analysis with visual indicators.
     * 
     * @param lines Array of lines to analyze
     * @return Enhanced color analysis with icons and previews
     */
    public static String createEnhancedColorAnalysis(String[] lines) {
        if (lines == null || lines.length == 0) {
            return DesignConstants.NO_COLOR;
        }
        
        Set<String> colors = getAllColors(lines);
        boolean hasMultiColorLines = hasMultiColorLines(lines);
        
        if (colors.isEmpty()) {
            return DesignConstants.DEFAULT_COLOR;
        }
        
        if (colors.size() == 1) {
            String colorCode = colors.iterator().next();
            String colorName = formatSingleColor(colorCode);
            return DesignConstants.createColorPreview("§" + colorCode, colorName);
        }
        
        // Multiple colors detected - create enhanced preview
        StringBuilder result = new StringBuilder();
        result.append(DesignConstants.createMultiColorPreview());
        
        if (hasMultiColorLines) {
            result.append("\n").append(DesignConstants.TREE_LAST)
                  .append(DesignConstants.MULTI_COLOR_DETECTED);
        }
        
        return result.toString();
    }
    
    /**
     * Enumeration for color complexity levels.
     */
    public enum ColorComplexity {
        NONE,   // No colors
        SINGLE, // Single color
        MULTI   // Multiple colors
    }
    
    // Private constructor to prevent instantiation
    private ColorAnalyzer() {
        throw new AssertionError("ColorAnalyzer should not be instantiated");
    }
}