package us.ironcladnetwork.copySign.Util;

import us.ironcladnetwork.copySign.CopySign;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * Simple update checker for the CopySign plugin.
 * Checks for updates from Spigot resource API.
 * 
 * Note: This is a basic implementation that logs to console only.
 * For production use, consider using a more robust update checking solution.
 */
public class UpdateChecker {
    private final CopySign plugin;
    private final int resourceId = 26118; // Replace with actual Spigot resource ID
    
    public UpdateChecker(CopySign plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Checks for updates asynchronously.
     * Results are logged to console only - no player notifications.
     * 
     * @param consumer Callback with the latest version string
     */
    public void checkForUpdate(Consumer<String> consumer) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (InputStream inputStream = new URL(
                    "https://api.spigotmc.org/legacy/update.php?resource=" + resourceId
            ).openStream(); Scanner scanner = new Scanner(inputStream)) {
                
                if (scanner.hasNext()) {
                    String latestVersion = scanner.next();
                    String currentVersion = plugin.getDescription().getVersion();
                    
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        consumer.accept(latestVersion);
                        
                        if (isNewerVersion(currentVersion, latestVersion)) {
                            plugin.getLogger().info("=================================================");
                            plugin.getLogger().info("CopySign Update Available!");
                            plugin.getLogger().info("Current Version: " + currentVersion);
                            plugin.getLogger().info("Latest Version: " + latestVersion);
                            plugin.getLogger().info("Download: https://www.spigotmc.org/resources/copysign." + resourceId);
                            plugin.getLogger().info("=================================================");
                        } else {
                            plugin.getLogger().info("CopySign is up to date (Version: " + currentVersion + ")");
                        }
                    });
                }
            } catch (IOException exception) {
                plugin.getLogger().warning("Cannot check for updates: " + exception.getMessage());
            }
        });
    }
    
    /**
     * Compares two version strings.
     * 
     * @param currentVersion The current plugin version
     * @param latestVersion The latest available version
     * @return true if latest version is newer
     */
    private boolean isNewerVersion(String currentVersion, String latestVersion) {
        try {
            // Remove any non-numeric suffixes (like -SNAPSHOT, -BETA, etc.)
            currentVersion = currentVersion.split("-")[0];
            latestVersion = latestVersion.split("-")[0];
            
            String[] currentParts = currentVersion.split("\\.");
            String[] latestParts = latestVersion.split("\\.");
            
            int length = Math.max(currentParts.length, latestParts.length);
            
            for (int i = 0; i < length; i++) {
                int currentPart = i < currentParts.length ? 
                    Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? 
                    Integer.parseInt(latestParts[i]) : 0;
                
                if (latestPart > currentPart) {
                    return true;
                } else if (currentPart > latestPart) {
                    return false;
                }
            }
            
            return false; // Versions are equal
        } catch (NumberFormatException e) {
            // If version parsing fails, assume no update
            return false;
        }
    }
}