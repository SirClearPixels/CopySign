package us.ironcladnetwork.copySign.Util;

import org.bukkit.entity.Player;
import us.ironcladnetwork.copySign.CopySign;
import us.ironcladnetwork.copySign.Lang.Lang;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized error handling and logging utility for the CopySign plugin.
 * Provides consistent error reporting, user notifications, and recovery mechanisms.
 */
public class ErrorHandler {
    
    private static final Logger logger = CopySign.getInstance().getLogger();
    private static final String BACKUP_SUFFIX = ".backup";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    /**
     * Handles file I/O errors with automatic backup and recovery.
     * 
     * @param operation The operation being performed (e.g., "saving player data")
     * @param file The file that caused the error
     * @param e The IOException that occurred
     * @param player Optional player to notify (can be null)
     * @return true if recovery was successful, false otherwise
     */
    public static boolean handleFileError(String operation, File file, IOException e, Player player) {
        String errorMsg = String.format("Failed %s for file %s: %s", operation, file.getName(), e.getMessage());
        logger.log(Level.SEVERE, errorMsg, e);
        
        // Notify player if provided
        if (player != null) {
            player.sendMessage(Lang.PREFIX.get() + "&cAn error occurred while " + operation + ". Please try again or contact an administrator.");
        }
        
        // Attempt recovery if it's a save operation and backup exists
        if (operation.contains("saving") && file.exists()) {
            return attemptFileRecovery(file);
        }
        
        return false;
    }
    
    /**
     * Handles configuration loading errors with fallback mechanisms.
     * 
     * @param configName The name of the configuration file
     * @param e The exception that occurred
     * @return true if fallback was successful, false otherwise
     */
    public static boolean handleConfigError(String configName, Exception e) {
        String errorMsg = String.format("Failed to load configuration %s: %s", configName, e.getMessage());
        logger.log(Level.SEVERE, errorMsg, e);
        
        // Log recovery attempt
        logger.warning("Attempting to recover " + configName + " from backup or defaults...");
        
        return true; // Bukkit will use defaults if config fails to load
    }
    
    /**
     * Handles NBT data corruption or invalid data.
     * 
     * @param operation The operation being performed
     * @param player The player affected
     * @param e The exception that occurred
     */
    public static void handleNBTError(String operation, Player player, Exception e) {
        String errorMsg = String.format("NBT error during %s for player %s: %s", operation, player.getName(), e.getMessage());
        logger.log(Level.WARNING, errorMsg, e);
        
        player.sendMessage(Lang.PREFIX.get() + "&cThe sign data appears to be corrupted. Please try copying the sign again.");
    }
    
    /**
     * Handles player data corruption with recovery attempts.
     * 
     * @param playerName The name of the player
     * @param dataType The type of data (e.g., "toggle state", "saved signs")
     * @param e The exception that occurred
     * @return true if recovery was successful, false otherwise
     */
    public static boolean handlePlayerDataError(String playerName, String dataType, Exception e) {
        String errorMsg = String.format("Player data corruption for %s (%s): %s", playerName, dataType, e.getMessage());
        logger.log(Level.WARNING, errorMsg, e);
        
        // Log that we're using defaults
        logger.info("Using default values for " + playerName + "'s " + dataType);
        
        return true; // We can always fall back to defaults
    }
    
    /**
     * Handles memory or performance related errors.
     * 
     * @param operation The operation being performed
     * @param e The exception that occurred
     * @param player Optional player to notify
     */
    public static void handlePerformanceError(String operation, Exception e, Player player) {
        String errorMsg = String.format("Performance error during %s: %s", operation, e.getMessage());
        logger.log(Level.WARNING, errorMsg, e);
        
        if (player != null) {
            player.sendMessage(Lang.PREFIX.get() + "&cOperation failed due to server load. Please try again in a moment.");
        }
    }
    
    /**
     * Handles general plugin errors with appropriate logging and user notification.
     * 
     * @param operation The operation being performed
     * @param e The exception that occurred
     * @param player Optional player to notify
     */
    public static void handleGeneralError(String operation, Exception e, Player player) {
        String errorMsg = String.format("Error during %s: %s", operation, e.getMessage());
        logger.log(Level.WARNING, errorMsg, e);
        
        if (player != null) {
            player.sendMessage(Lang.PREFIX.get() + "&cAn unexpected error occurred. Please try again or contact an administrator.");
        }
    }
    
    /**
     * Creates a backup of a file before performing operations.
     * 
     * @param file The file to backup
     * @return true if backup was successful, false otherwise
     */
    public static boolean createBackup(File file) {
        if (!file.exists()) {
            return true; // No file to backup
        }
        
        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            File backupFile = new File(file.getParent(), file.getName() + BACKUP_SUFFIX + "_" + timestamp);
            Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // Keep only the 3 most recent backups
            cleanupOldBackups(file);
            
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create backup for " + file.getName(), e);
            return false;
        }
    }
    
    /**
     * Attempts to recover a file from its backup.
     * 
     * @param file The file to recover
     * @return true if recovery was successful, false otherwise
     */
    private static boolean attemptFileRecovery(File file) {
        File backupFile = new File(file.getParent(), file.getName() + BACKUP_SUFFIX);
        
        if (!backupFile.exists()) {
            logger.warning("No backup found for " + file.getName() + ", cannot recover");
            return false;
        }
        
        try {
            Files.copy(backupFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Successfully recovered " + file.getName() + " from backup");
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to recover " + file.getName() + " from backup", e);
            return false;
        }
    }
    
    /**
     * Cleans up old backup files, keeping only the 3 most recent.
     * 
     * @param originalFile The original file whose backups to clean
     */
    private static void cleanupOldBackups(File originalFile) {
        File parentDir = originalFile.getParentFile();
        if (parentDir == null) return;
        
        String backupPrefix = originalFile.getName() + BACKUP_SUFFIX;
        File[] backupFiles = parentDir.listFiles((dir, name) -> name.startsWith(backupPrefix));
        
        if (backupFiles != null && backupFiles.length > 3) {
            // Sort by last modified time (oldest first)
            java.util.Arrays.sort(backupFiles, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            
            // Delete oldest backups, keeping only the 3 most recent
            for (int i = 0; i < backupFiles.length - 3; i++) {
                if (backupFiles[i].delete()) {
                    logger.fine("Deleted old backup: " + backupFiles[i].getName());
                }
            }
        }
    }
    
    /**
     * Validates that a string is safe for use in file operations.
     * 
     * @param input The string to validate
     * @param maxLength Maximum allowed length
     * @return true if the string is safe, false otherwise
     */
    public static boolean isValidFileName(String input, int maxLength) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        if (input.length() > maxLength) {
            return false;
        }
        
        // Check for invalid characters
        return input.matches("^[a-zA-Z0-9_-]+$");
    }
    
    /**
     * Logs debug information if debug mode is enabled.
     * 
     * @param message The debug message
     */
    public static void debug(String message) {
        if (CopySign.getInstance().getConfig().getBoolean("general.debug", false)) {
            logger.info("[DEBUG] " + message);
        }
    }
} 