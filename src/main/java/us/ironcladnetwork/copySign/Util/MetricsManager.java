package us.ironcladnetwork.copySign.Util;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.entity.Player;
import us.ironcladnetwork.copySign.CopySign;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages advanced metrics collection for the CopySign plugin.
 * Tracks feature usage and provides insights for plugin development.
 */
public class MetricsManager {
    private final CopySign plugin;
    private final ConfigManager configManager;
    
    // Usage counters (reset on restart)
    private final AtomicInteger copyOperations = new AtomicInteger(0);
    private final AtomicInteger pasteOperations = new AtomicInteger(0);
    private final AtomicInteger saveOperations = new AtomicInteger(0);
    private final AtomicInteger loadOperations = new AtomicInteger(0);
    private final AtomicInteger templateOperations = new AtomicInteger(0);
    
    // Track unique users per session
    private final Map<String, Long> uniqueUsers = new ConcurrentHashMap<>();
    
    public MetricsManager(CopySign plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }
    
    /**
     * Adds advanced metrics charts to the bStats metrics instance.
     * 
     * @param metrics The bStats metrics instance
     */
    public void setupAdvancedMetrics(Metrics metrics) {
        // Track feature usage distribution
        metrics.addCustomChart(new AdvancedPie("feature_usage", () -> {
            Map<String, Integer> values = new HashMap<>();
            values.put("Copy Operations", copyOperations.get());
            values.put("Paste Operations", pasteOperations.get());
            values.put("Save Operations", saveOperations.get());
            values.put("Load Operations", loadOperations.get());
            values.put("Template Operations", templateOperations.get());
            return values;
        }));
        
        // Track sign types distribution
        metrics.addCustomChart(new DrilldownPie("sign_types_config", () -> {
            Map<String, Map<String, Integer>> values = new HashMap<>();
            
            // Get allowed sign types from config
            int regularCount = 0;
            int hangingCount = 0;
            
            for (String signType : configManager.getAllowedSignTypes()) {
                if (signType.contains("HANGING")) {
                    hangingCount++;
                } else {
                    regularCount++;
                }
            }
            
            Map<String, Integer> signTypes = new HashMap<>();
            signTypes.put("Regular Signs", regularCount);
            signTypes.put("Hanging Signs", hangingCount);
            
            values.put("Allowed Sign Types", signTypes);
            return values;
        }));
        
        // Track active users this session
        metrics.addCustomChart(new AdvancedPie("session_activity", () -> {
            Map<String, Integer> values = new HashMap<>();
            
            long now = System.currentTimeMillis();
            int active15min = 0;
            int active1hour = 0;
            int activeSession = uniqueUsers.size();
            
            for (Long lastSeen : uniqueUsers.values()) {
                long timeDiff = now - lastSeen;
                if (timeDiff <= 15 * 60 * 1000) { // 15 minutes
                    active15min++;
                }
                if (timeDiff <= 60 * 60 * 1000) { // 1 hour
                    active1hour++;
                }
            }
            
            values.put("Active (15 min)", active15min);
            values.put("Active (1 hour)", active1hour);
            values.put("Total Session", activeSession);
            
            return values;
        }));
        
        // Track cache sizes for memory monitoring
        metrics.addCustomChart(new AdvancedPie("cache_sizes", () -> {
            Map<String, Integer> values = new HashMap<>();
            
            // Toggle state cache
            values.put("Toggle States", CopySign.getToggleManager().getCacheSize());
            
            // Cooldown cache
            values.put("Cooldowns", CopySign.getCooldownManager().getActiveCooldownCount());
            
            // Sign data cache
            values.put("Sign Data", SignDataCache.getSize());
            
            // Confirmation cache
            values.put("Confirmations", CopySign.getConfirmationManager().getPendingCount());
            
            return values;
        }));
        
        // Track cache effectiveness
        metrics.addCustomChart(new SimplePie("cache_effectiveness", () -> {
            int totalCached = CopySign.getToggleManager().getCacheSize() + 
                              CopySign.getCooldownManager().getActiveCooldownCount() + 
                              SignDataCache.getSize();
            
            if (totalCached == 0) return "No Cache";
            else if (totalCached <= 50) return "Light (1-50 entries)";
            else if (totalCached <= 200) return "Moderate (51-200 entries)";
            else if (totalCached <= 1000) return "Heavy (201-1000 entries)";
            else return "Very Heavy (1000+ entries)";
        }));
        
        plugin.getDebugLogger().debug("Advanced metrics charts configured");
    }
    
    /**
     * Records a copy operation.
     * 
     * @param player The player who performed the operation
     */
    public void recordCopyOperation(Player player) {
        if (!configManager.isMetricsEnabled()) return;
        
        copyOperations.incrementAndGet();
        trackUser(player);
        plugin.getDebugLogger().debugPerformance("Metrics: Copy operation recorded", 0);
    }
    
    /**
     * Records a paste operation.
     * 
     * @param player The player who performed the operation
     */
    public void recordPasteOperation(Player player) {
        if (!configManager.isMetricsEnabled()) return;
        
        pasteOperations.incrementAndGet();
        trackUser(player);
        plugin.getDebugLogger().debugPerformance("Metrics: Paste operation recorded", 0);
    }
    
    /**
     * Records a save operation.
     * 
     * @param player The player who performed the operation
     */
    public void recordSaveOperation(Player player) {
        if (!configManager.isMetricsEnabled()) return;
        
        saveOperations.incrementAndGet();
        trackUser(player);
        plugin.getDebugLogger().debugPerformance("Metrics: Save operation recorded", 0);
    }
    
    /**
     * Records a load operation.
     * 
     * @param player The player who performed the operation
     */
    public void recordLoadOperation(Player player) {
        if (!configManager.isMetricsEnabled()) return;
        
        loadOperations.incrementAndGet();
        trackUser(player);
        plugin.getDebugLogger().debugPerformance("Metrics: Load operation recorded", 0);
    }
    
    /**
     * Records a template operation.
     * 
     * @param player The player who performed the operation (null for system)
     */
    public void recordTemplateOperation(Player player) {
        if (!configManager.isMetricsEnabled()) return;
        
        templateOperations.incrementAndGet();
        if (player != null) {
            trackUser(player);
        }
        plugin.getDebugLogger().debugPerformance("Metrics: Template operation recorded", 0);
    }
    
    /**
     * Tracks a unique user.
     * 
     * @param player The player to track
     */
    private void trackUser(Player player) {
        uniqueUsers.put(player.getUniqueId().toString(), System.currentTimeMillis());
    }
    
    /**
     * Gets current session statistics.
     * 
     * @return A map of statistic names to values
     */
    public Map<String, Integer> getSessionStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("Copy Operations", copyOperations.get());
        stats.put("Paste Operations", pasteOperations.get());
        stats.put("Save Operations", saveOperations.get());
        stats.put("Load Operations", loadOperations.get());
        stats.put("Template Operations", templateOperations.get());
        stats.put("Unique Users", uniqueUsers.size());
        return stats;
    }
}