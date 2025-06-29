package us.ironcladnetwork.copySign.Util;

/**
 * Utility class for validating NBT data to prevent injection attacks.
 * Implements size limits and content validation for sign text data.
 */
public class NBTValidationUtil {
    
    private static final int MAX_NBT_SIZE = 32768; // 32KB limit
    private static final int MAX_LINE_LENGTH = 384; // Per Minecraft limits
    private static final int MAX_LINES = 4; // Maximum lines per sign side
    private static final int MAX_TOTAL_LINES = 8; // Maximum total lines (front + back)
    
    /**
     * Validates NBT data string for size and content constraints.
     * Uses enhanced validation including Unicode normalization and content checks.
     * 
     * @param data The NBT data string to validate
     * @return true if the data is valid, false otherwise
     */
    public static boolean validateNBTData(String data) {
        if (data == null) {
            return true; // null is acceptable
        }
        
        // Check overall size limit
        if (data.length() > MAX_NBT_SIZE) {
            return false;
        }
        
        // Use enhanced sign content validation
        if (!ErrorHandler.isValidSignContent(data, MAX_NBT_SIZE)) {
            return false;
        }
        
        // Check individual line lengths
        String[] lines = data.split("\n", -1);
        if (lines.length > MAX_LINES) {
            return false;
        }
        
        for (String line : lines) {
            if (line.length() > MAX_LINE_LENGTH) {
                return false;
            }
            // Additional content validation for each line
            if (!ErrorHandler.isValidSignContent(line, MAX_LINE_LENGTH)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validates an array of sign lines.
     * 
     * @param lines The array of sign lines to validate
     * @return true if all lines are valid, false otherwise
     */
    public static boolean validateSignLines(String[] lines) {
        if (lines == null) {
            return true;
        }
        
        if (lines.length > MAX_LINES) {
            return false;
        }
        
        for (String line : lines) {
            if (line != null && line.length() > MAX_LINE_LENGTH) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validates both front and back sign data together.
     * 
     * @param frontLines Front side lines
     * @param backLines Back side lines
     * @return true if both sides are valid, false otherwise
     */
    public static boolean validateSignData(String[] frontLines, String[] backLines) {
        if (!validateSignLines(frontLines) || !validateSignLines(backLines)) {
            return false;
        }
        
        // Check total line count
        int totalLines = 0;
        if (frontLines != null) {
            totalLines += frontLines.length;
        }
        if (backLines != null) {
            totalLines += backLines.length;
        }
        
        return totalLines <= MAX_TOTAL_LINES;
    }
    
    /**
     * Sanitizes a string by truncating if it exceeds limits.
     * 
     * @param data The string to sanitize
     * @return The sanitized string
     */
    public static String sanitizeNBTData(String data) {
        if (data == null) {
            return null;
        }
        
        if (data.length() > MAX_NBT_SIZE) {
            data = data.substring(0, MAX_NBT_SIZE);
        }
        
        String[] lines = data.split("\n", -1);
        StringBuilder sanitized = new StringBuilder();
        
        for (int i = 0; i < Math.min(lines.length, MAX_LINES); i++) {
            String line = lines[i];
            if (line.length() > MAX_LINE_LENGTH) {
                line = line.substring(0, MAX_LINE_LENGTH);
            }
            sanitized.append(line);
            if (i < Math.min(lines.length, MAX_LINES) - 1) {
                sanitized.append("\n");
            }
        }
        
        return sanitized.toString();
    }
}