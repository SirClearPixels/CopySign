package us.ironcladnetwork.copySign.Util;

import org.bukkit.entity.Player;
import us.ironcladnetwork.copySign.CopySign;
import us.ironcladnetwork.copySign.Lang.Lang;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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
        // Log detailed error with full path for admins/console
        String detailedErrorMsg = String.format("Failed %s for file %s: %s", operation, file.getAbsolutePath(), e.getMessage());
        logger.log(Level.SEVERE, detailedErrorMsg, e);
        
        // Notify player with sanitized message (no file paths exposed)
        if (player != null) {
            player.sendMessage(Lang.PREFIX.get() + "§cAn error occurred while " + operation + ". Please try again or contact an administrator.");
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
        // Log detailed error for admins/console
        String detailedErrorMsg = String.format("NBT error during %s for player %s: %s", operation, player.getName(), e.getMessage());
        logger.log(Level.WARNING, detailedErrorMsg, e);
        
        // Send sanitized message to player
        player.sendMessage(Lang.PREFIX.get() + "§cThe sign data appears to be corrupted. Please try copying the sign again.");
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
        // Log detailed error for admins/console
        String detailedErrorMsg = String.format("Error during %s: %s", operation, e.getMessage());
        logger.log(Level.WARNING, detailedErrorMsg, e);
        
        // Send sanitized message to player
        if (player != null) {
            player.sendMessage(Lang.PREFIX.get() + "§cAn unexpected error occurred. Please try again or contact an administrator.");
        }
    }
    
    /**
     * Creates a backup of a file before performing operations.
     * 
     * @param file The file to backup
     * @return true if backup was successful, false otherwise
     */
    public static boolean createBackup(File file) {
        return createBackupAsync(file, null).join();
    }
    
    /**
     * Asynchronously creates a backup of a file before performing operations.
     * 
     * @param file The file to backup
     * @param callback Optional callback to execute after backup completion
     * @return CompletableFuture that completes with backup success status
     */
    public static CompletableFuture<Boolean> createBackupAsync(File file, Consumer<Boolean> callback) {
        return CompletableFuture.supplyAsync(() -> {
            if (!file.exists()) {
                return true; // No file to backup
            }
            
            try {
                // Validate file path to prevent directory traversal
                Path filePath = file.toPath().normalize();
                if (!isValidPath(filePath)) {
                    logger.warning("Invalid file path detected, backup rejected: " + file.getName());
                    return false;
                }
                
                String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
                String backupFileName = file.getName() + BACKUP_SUFFIX + "_" + timestamp;
                
                // Use secure path resolution
                Path parentPath = filePath.getParent();
                if (parentPath == null) {
                    logger.warning("Cannot determine parent directory for backup: " + file.getName());
                    return false;
                }
                
                Path backupPath = parentPath.resolve(backupFileName).normalize();
                
                // Ensure backup stays within parent directory
                if (!backupPath.startsWith(parentPath)) {
                    logger.warning("Backup path escapes parent directory, rejected: " + backupFileName);
                    return false;
                }
                
                // Calculate original file checksum before backup
                String originalChecksum = calculateFileChecksum(filePath);
                if (originalChecksum == null) {
                    logger.warning("Unable to calculate checksum for original file, backup may be unreliable: " + file.getName());
                }
                
                // Perform the backup
                Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Verify backup integrity by comparing checksums
                boolean integrityVerified = verifyBackupIntegrity(filePath, backupPath);
                if (!integrityVerified) {
                    // If integrity check fails, delete the corrupted backup
                    try {
                        Files.deleteIfExists(backupPath);
                        logger.severe("Deleted corrupted backup file: " + backupPath.getFileName());
                    } catch (IOException deleteException) {
                        logger.log(Level.SEVERE, "Failed to delete corrupted backup: " + backupPath.getFileName(), deleteException);
                    }
                    return false;
                }
                
                // Keep only the 3 most recent backups
                cleanupOldBackupsAsync(file);
                
                debug("Backup created and verified successfully for " + file.getName());
                return true;
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to create backup for " + file.getName(), e);
                return false;
            }
        }).thenApply(result -> {
            if (callback != null) {
                // Execute callback on main thread if running in a Bukkit environment
                if (CopySign.getInstance() != null) {
                    org.bukkit.Bukkit.getScheduler().runTask(CopySign.getInstance(), () -> callback.accept(result));
                } else {
                    callback.accept(result);
                }
            }
            return result;
        });
    }
    
    /**
     * Attempts to recover a file from its backup.
     * 
     * @param file The file to recover
     * @return true if recovery was successful, false otherwise
     */
    private static boolean attemptFileRecovery(File file) {
        return attemptFileRecoveryAsync(file, null).join();
    }
    
    /**
     * Asynchronously attempts to recover a file from its backup.
     * 
     * @param file The file to recover
     * @param callback Optional callback to execute after recovery completion
     * @return CompletableFuture that completes with recovery success status
     */
    public static CompletableFuture<Boolean> attemptFileRecoveryAsync(File file, Consumer<Boolean> callback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate file path to prevent directory traversal
                Path filePath = file.toPath().normalize();
                if (!isValidPath(filePath)) {
                    logger.warning("Invalid file path detected, recovery rejected: " + file.getName());
                    return false;
                }
                
                Path parentPath = filePath.getParent();
                if (parentPath == null) {
                    logger.warning("Cannot determine parent directory for recovery: " + file.getName());
                    return false;
                }
                
                // Use secure path resolution for backup file
                String backupFileName = file.getName() + BACKUP_SUFFIX;
                Path backupPath = parentPath.resolve(backupFileName).normalize();
                
                // Ensure backup path stays within parent directory
                if (!backupPath.startsWith(parentPath)) {
                    logger.warning("Backup path escapes parent directory, recovery rejected: " + backupFileName);
                    return false;
                }
                
                if (!Files.exists(backupPath)) {
                    logger.warning("No backup found for " + file.getName() + ", cannot recover");
                    return false;
                }
                
                Files.copy(backupPath, filePath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Successfully recovered " + file.getName() + " from backup");
                return true;
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to recover " + file.getName() + " from backup", e);
                return false;
            }
        }).thenApply(result -> {
            if (callback != null) {
                // Execute callback on main thread if running in a Bukkit environment
                if (CopySign.getInstance() != null) {
                    org.bukkit.Bukkit.getScheduler().runTask(CopySign.getInstance(), () -> callback.accept(result));
                } else {
                    callback.accept(result);
                }
            }
            return result;
        });
    }
    
    /**
     * Cleans up old backup files, keeping only the 3 most recent.
     * 
     * @param originalFile The original file whose backups to clean
     */
    private static void cleanupOldBackups(File originalFile) {
        cleanupOldBackupsAsync(originalFile);
    }
    
    /**
     * Asynchronously cleans up old backup files, keeping only the 3 most recent.
     * 
     * @param originalFile The original file whose backups to clean
     */
    private static void cleanupOldBackupsAsync(File originalFile) {
        CompletableFuture.runAsync(() -> {
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
        });
    }
    
    /**
     * Validates that a string is safe for use in file operations.
     * Uses Unicode normalization and character type validation to prevent bypass attacks.
     * 
     * @param input The string to validate
     * @param maxLength Maximum allowed length
     * @return true if the string is safe, false otherwise
     */
    public static boolean isValidFileName(String input, int maxLength) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        // Normalize Unicode to prevent normalization attacks
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFKC);
        
        if (normalized.length() > maxLength) {
            return false;
        }
        
        // Check for reserved Windows names (case-insensitive)
        String upperNormalized = normalized.toUpperCase();
        String[] reservedNames = {
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        };
        
        for (String reserved : reservedNames) {
            if (upperNormalized.equals(reserved) || upperNormalized.startsWith(reserved + ".")) {
                return false;
            }
        }
        
        // Use Character.getType() for Unicode-aware validation
        return normalized.codePoints().allMatch(codePoint -> {
            int type = Character.getType(codePoint);
            
            // Allow letters, digits, underscore, and hyphen
            return type == Character.UPPERCASE_LETTER ||
                   type == Character.LOWERCASE_LETTER ||
                   type == Character.TITLECASE_LETTER ||
                   type == Character.DECIMAL_DIGIT_NUMBER ||
                   codePoint == '_' ||
                   codePoint == '-';
        });
    }
    
    /**
     * Validates that a path is safe and doesn't contain directory traversal attempts.
     * 
     * @param path The path to validate
     * @return true if the path is safe, false otherwise
     */
    private static boolean isValidPath(Path path) {
        if (path == null) return false;
        
        String pathStr = path.toString();
        
        // Check for directory traversal patterns
        if (pathStr.contains("..") || pathStr.contains("./") || pathStr.contains("~/")) {
            return false;
        }
        
        // Check for absolute paths that might escape plugin directory
        if (path.isAbsolute()) {
            // Allow only if within the plugin's data folder
            Path pluginDataPath = CopySign.getInstance().getDataFolder().toPath().normalize();
            try {
                return path.startsWith(pluginDataPath);
            } catch (Exception e) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validates that a string is safe for use as sign content.
     * Checks for potential exploits in sign text data.
     * 
     * @param input The string to validate
     * @param maxLength Maximum allowed length
     * @return true if the string is safe, false otherwise
     */
    public static boolean isValidSignContent(String input, int maxLength) {
        if (input == null) {
            return true; // Null content is allowed (empty sign)
        }
        
        // Normalize Unicode to prevent normalization attacks
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC);
        
        if (normalized.length() > maxLength) {
            return false;
        }
        
        // Check for suspicious patterns
        return normalized.codePoints().allMatch(codePoint -> {
            int type = Character.getType(codePoint);
            
            // Block most control characters except common formatting
            if (type == Character.CONTROL) {
                // Allow newline, tab, and carriage return
                return codePoint == '\n' || codePoint == '\t' || codePoint == '\r';
            }
            
            // Block private use characters that could be exploited
            if (type == Character.PRIVATE_USE) {
                return false;
            }
            
            // Block unassigned characters
            if (type == Character.UNASSIGNED) {
                return false;
            }
            
            return true;
        });
    }
    
    /**
     * Sanitizes error messages to remove sensitive information before showing to players.
     * 
     * @param message The original error message
     * @return Sanitized message safe for player display
     */
    private static String sanitizePlayerMessage(String message) {
        if (message == null) return "An unknown error occurred";
        
        // Remove file paths, stack traces, and other sensitive info
        return message.replaceAll("(/[^\\s]*)", "[file path hidden]")
                     .replaceAll("(at [a-zA-Z0-9.]+\\([^)]*\\))", "[stack trace hidden]")
                     .replaceAll("(java\\.[^\\s]*)", "[java error details hidden]")
                     .replaceAll("(Exception|Error): ", "");
    }
    
    /**
     * Calculates SHA-256 checksum of a file for integrity verification.
     * 
     * @param filePath The path to the file to checksum
     * @return The SHA-256 checksum as a hex string, or null if calculation fails
     */
    private static String calculateFileChecksum(Path filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.log(Level.WARNING, "Failed to calculate checksum for " + filePath.getFileName(), e);
            return null;
        }
    }
    
    /**
     * Verifies file integrity by comparing checksums before and after backup operation.
     * 
     * @param originalPath Path to the original file
     * @param backupPath Path to the backup file
     * @return true if checksums match (backup is valid), false otherwise
     */
    private static boolean verifyBackupIntegrity(Path originalPath, Path backupPath) {
        try {
            String originalChecksum = calculateFileChecksum(originalPath);
            String backupChecksum = calculateFileChecksum(backupPath);
            
            if (originalChecksum == null || backupChecksum == null) {
                logger.warning("Unable to verify backup integrity - checksum calculation failed");
                return false;
            }
            
            boolean isValid = originalChecksum.equals(backupChecksum);
            if (isValid) {
                debug("Backup integrity verified: checksums match for " + originalPath.getFileName());
            } else {
                logger.warning("Backup integrity verification failed: checksums do not match for " + originalPath.getFileName());
                logger.warning("Original: " + originalChecksum + ", Backup: " + backupChecksum);
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during backup integrity verification", e);
            return false;
        }
    }
    
    /**
     * Logs debug information if debug mode is enabled.
     * 
     * @param message The debug message
     */
    public static void debug(String message) {
        if (CopySign.getInstance().getConfigBoolean("general.debug", false)) {
            logger.info("[DEBUG] " + message);
        }
    }
} 