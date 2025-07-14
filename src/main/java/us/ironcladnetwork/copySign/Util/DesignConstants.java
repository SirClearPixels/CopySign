package us.ironcladnetwork.copySign.Util;

/**
 * Design constants for consistent visual formatting across the CopySign plugin.
 * Centralizes color palette, formatting patterns, and typography standards.
 * 
 * @author IroncladNetwork
 * @since 2.1.1
 */
public final class DesignConstants {
    
    // ===== COLOR PALETTE =====
    
    /**
     * Primary brand color - Gold
     * Used for: Headers, names, emphasis, primary branding
     */
    public static final String PRIMARY_BRAND = "§6";
    
    /**
     * Secondary brand color - Aqua  
     * Used for: Content text, highlights, secondary branding
     */
    public static final String SECONDARY_BRAND = "§b";
    
    /**
     * Success/Active color - Green
     * Used for: Positive states, enabled features, success messages
     */
    public static final String SUCCESS_ACTIVE = "§a";
    
    /**
     * Warning/Inactive color - Red
     * Used for: Negative states, disabled features, error messages
     */
    public static final String WARNING_INACTIVE = "§c";
    
    /**
     * Information color - Yellow
     * Used for: Labels, properties, informational text
     */
    public static final String INFORMATION = "§e";
    
    /**
     * Supporting color - Gray
     * Used for: Secondary text, descriptions, metadata
     */
    public static final String SUPPORTING = "§7";
    
    /**
     * Structure color - Dark Gray
     * Used for: Separators, tree lines, structural elements
     */
    public static final String STRUCTURE = "§8";
    
    /**
     * Clean text color - White
     * Used for: Primary readable content, main text
     */
    public static final String CLEAN_TEXT = "§f";
    
    /**
     * Multi-color indicator - Light Purple
     * Used for: Multi-colored content indicators
     */
    public static final String MULTI_COLOR = "§d";
    
    // ===== TYPOGRAPHY =====
    
    /**
     * Bold formatting for headers and emphasis
     */
    public static final String BOLD = "§l";
    
    /**
     * Combined primary brand with bold for main headers
     */
    public static final String HEADER_PRIMARY = PRIMARY_BRAND + BOLD;
    
    /**
     * Combined clean text with bold for section headers
     */
    public static final String HEADER_SECTION = CLEAN_TEXT + BOLD;
    
    /**
     * Combined information with bold for property labels
     */
    public static final String LABEL_PROPERTY = INFORMATION + BOLD;
    
    // ===== SEPARATORS =====
    
    /**
     * Premium top separator using thick lines
     */
    public static final String SEPARATOR_TOP = HEADER_PRIMARY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    
    /**
     * Premium bottom separator using thick lines
     */
    public static final String SEPARATOR_BOTTOM = HEADER_PRIMARY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    
    /**
     * Standard section separator using thin lines
     */
    public static final String SEPARATOR_SECTION = STRUCTURE + "§m" + "▔".repeat(32);
    
    // ===== TREE STRUCTURE =====
    
    /**
     * Tree branch connector
     */
    public static final String TREE_BRANCH = STRUCTURE + "├─ ";
    
    /**
     * Tree last item connector
     */
    public static final String TREE_LAST = STRUCTURE + "└─ ";
    
    /**
     * Tree vertical divider
     */
    public static final String TREE_DIVIDER = STRUCTURE + "│ ";
    
    // ===== STATE INDICATORS =====
    
    /**
     * Active/enabled state indicator
     */
    public static final String STATE_ACTIVE = SUCCESS_ACTIVE + BOLD + "Active";
    
    /**
     * Empty/inactive state indicator
     */
    public static final String STATE_EMPTY = WARNING_INACTIVE + BOLD + "Empty";
    
    /**
     * Enabled state indicator
     */
    public static final String STATE_ENABLED = SUCCESS_ACTIVE + BOLD + "Enabled";
    
    /**
     * Disabled state indicator
     */
    public static final String STATE_DISABLED = WARNING_INACTIVE + BOLD + "Disabled";
    
    /**
     * On state indicator (for glow states)
     */
    public static final String STATE_ON = SUCCESS_ACTIVE + BOLD + "On";
    
    /**
     * Off state indicator (for glow states)
     */
    public static final String STATE_OFF = WARNING_INACTIVE + BOLD + "Off";
    
    /**
     * Yes confirmation indicator
     */
    public static final String STATE_YES = SUCCESS_ACTIVE + BOLD + "Yes";
    
    /**
     * No confirmation indicator
     */
    public static final String STATE_NO = WARNING_INACTIVE + BOLD + "No";
    
    // ===== CONTENT FORMATTING =====
    
    /**
     * Quote wrapper start for content
     */
    public static final String QUOTE_START = CLEAN_TEXT + "\"" + SECONDARY_BRAND;
    
    /**
     * Quote wrapper end for content
     */
    public static final String QUOTE_END = CLEAN_TEXT + "\"";
    
    /**
     * Multi-colored content indicator
     */
    public static final String MULTI_COLORED_INDICATOR = MULTI_COLOR + BOLD + "Multi-Colored";
    
    /**
     * Default color indicator
     */
    public static final String DEFAULT_COLOR = SUPPORTING + "Default";
    
    /**
     * No color indicator
     */
    public static final String NO_COLOR = SUPPORTING + "None";
    
    // ===== ICONS & SYMBOLS =====
    
    /**
     * Warning symbol for mixed states
     */
    public static final String ICON_WARNING = INFORMATION + "⚠ ";
    
    /**
     * Multi-color palette icon
     */
    public static final String ICON_MULTI_COLOR = MULTI_COLOR + "🎨 ";
    
    /**
     * Glow effect icon
     */
    public static final String ICON_GLOW = SUCCESS_ACTIVE + "✨ ";
    
    /**
     * No glow icon
     */
    public static final String ICON_NO_GLOW = WARNING_INACTIVE + "○ ";
    
    /**
     * Mixed glow state warning message
     */
    public static final String MIXED_GLOW_WARNING = ICON_WARNING + SUPPORTING + "Mixed glow states detected";
    
    /**
     * Multi-color detection message
     */
    public static final String MULTI_COLOR_DETECTED = ICON_MULTI_COLOR + SUPPORTING + "Multiple colors detected";
    
    // ===== COLOR PREVIEW BLOCKS =====
    
    /**
     * Unicode block for color previews - full block
     */
    public static final String COLOR_BLOCK = "█";
    
    /**
     * Unicode block for color previews - medium shade
     */
    public static final String COLOR_BLOCK_MEDIUM = "▓";
    
    /**
     * Unicode block for color previews - light shade
     */
    public static final String COLOR_BLOCK_LIGHT = "▒";
    
    // ===== GLOW STATE VISUAL INDICATORS =====
    
    /**
     * Visual glow indicator for glowing text
     */
    public static final String GLOW_INDICATOR = SUCCESS_ACTIVE + "◆ ";
    
    /**
     * Visual non-glow indicator for non-glowing text
     */
    public static final String NO_GLOW_INDICATOR = STRUCTURE + "◇ ";
    
    /**
     * Mixed glow state visual indicator
     */
    public static final String MIXED_GLOW_INDICATOR = INFORMATION + "◈ ";
    
    // ===== UTILITY METHODS =====
    
    /**
     * Wraps content in consistent quote formatting
     * 
     * @param content The content to wrap
     * @return Formatted quoted content
     */
    public static String wrapInQuotes(String content) {
        return QUOTE_START + content + QUOTE_END;
    }
    
    /**
     * Creates a formatted line entry for tree structure
     * 
     * @param lineNumber The line number (1-indexed)
     * @param content The line content
     * @param isLast Whether this is the last item in the tree
     * @return Formatted tree line
     */
    public static String formatTreeLine(int lineNumber, String content, boolean isLast) {
        String connector = isLast ? TREE_LAST : TREE_BRANCH;
        return connector + SUPPORTING + "Line " + lineNumber + ": " + wrapInQuotes(content);
    }
    
    /**
     * Creates a formatted color entry for tree structure
     * 
     * @param colorStatus The color status string
     * @return Formatted color line
     */
    public static String formatColorLine(String colorStatus) {
        return TREE_LAST + SUPPORTING + "Color: " + CLEAN_TEXT + colorStatus;
    }
    
    /**
     * Creates a formatted side header with status and enhanced glow indicators
     * 
     * @param sideName The side name (Front/Back)
     * @param hasContent Whether the side has content
     * @param isGlowing Whether the side is glowing
     * @return Formatted side header with visual indicators
     */
    public static String formatSideHeader(String sideName, boolean hasContent, boolean isGlowing) {
        String contentStatus = hasContent ? STATE_ACTIVE : STATE_EMPTY;
        String glowStatus = createGlowIndicator(isGlowing);
        return HEADER_SECTION + sideName + " Side " + TREE_DIVIDER + contentStatus + " " + TREE_DIVIDER + SUPPORTING + "Glow: " + glowStatus;
    }
    
    /**
     * Creates a formatted property line
     * 
     * @param propertyName The property name
     * @param propertyValue The property value
     * @param isLast Whether this is the last property
     * @return Formatted property line
     */
    public static String formatProperty(String propertyName, String propertyValue, boolean isLast) {
        String connector = isLast ? TREE_LAST : TREE_BRANCH;
        return connector + SUPPORTING + propertyName + ": " + CLEAN_TEXT + propertyValue;
    }
    
    /**
     * Creates a color preview sample using Unicode blocks
     * 
     * @param colorCode The Minecraft color code (e.g., "§a", "§c")
     * @param colorName The display name of the color
     * @return Formatted color preview with sample
     */
    public static String createColorPreview(String colorCode, String colorName) {
        if (colorCode == null || colorCode.isEmpty()) {
            return DEFAULT_COLOR;
        }
        return colorCode + COLOR_BLOCK + COLOR_BLOCK + COLOR_BLOCK + " " + CLEAN_TEXT + colorName;
    }
    
    /**
     * Creates a multi-color preview indicator
     * 
     * @return Formatted multi-color indicator with icon
     */
    public static String createMultiColorPreview() {
        return ICON_MULTI_COLOR + MULTI_COLORED_INDICATOR;
    }
    
    /**
     * Creates an enhanced glow state indicator with visual symbols
     * 
     * @param isGlowing Whether the element is glowing
     * @return Formatted glow state with visual indicator
     */
    public static String createGlowIndicator(boolean isGlowing) {
        if (isGlowing) {
            return GLOW_INDICATOR + STATE_ON;
        } else {
            return NO_GLOW_INDICATOR + STATE_OFF;
        }
    }
    
    /**
     * Creates a responsive content line based on content length
     * 
     * @param content The content to format
     * @param maxLength The maximum length before truncation
     * @return Formatted content with responsive sizing
     */
    public static String createResponsiveContent(String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return SUPPORTING + "<empty>";
        }
        
        if (content.length() <= maxLength) {
            return wrapInQuotes(content);
        } else {
            return wrapInQuotes(content.substring(0, maxLength - 3) + "...");
        }
    }
    
    /**
     * Creates a mixed glow state warning with enhanced formatting
     * 
     * @return Enhanced mixed glow state warning
     */
    public static String createMixedGlowWarning() {
        return MIXED_GLOW_INDICATOR + MIXED_GLOW_WARNING;
    }
    
    // Private constructor to prevent instantiation
    private DesignConstants() {
        throw new AssertionError("DesignConstants should not be instantiated");
    }
}