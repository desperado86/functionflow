package com.functionflow.demo.controller;

import com.functionflow.demo.config.CacheEnhancementPostProcessor;
import com.functionflow.demo.config.ValidationEnhancementPostProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 增强功能管理控制器
 * 
 * 提供以下功能：
 * 1. 查看缓存统计信息
 * 2. 查看验证统计信息
 * 3. 管理缓存（清除、刷新等）
 * 4. 系统增强功能状态
 */
@RestController
@RequestMapping("/api/enhancement")
@RequiredArgsConstructor
public class EnhancementController {

    private final CacheEnhancementPostProcessor cacheProcessor;
    private final ValidationEnhancementPostProcessor validationProcessor;

    /**
     * 获取所有增强功能状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getEnhancementStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // 缓存状态
        Map<String, Object> cacheStatus = new HashMap<>();
        cacheStatus.put("enabled", true);
        cacheStatus.put("cacheSize", cacheProcessor.getCacheSize());
        cacheStatus.put("stats", cacheProcessor.getCacheStats());
        status.put("cache", cacheStatus);
        
        // 验证状态
        Map<String, Object> validationStatus = new HashMap<>();
        validationStatus.put("enabled", true);
        validationStatus.put("stats", validationProcessor.getValidationStats());
        status.put("validation", validationStatus);
        
        return ResponseEntity.ok(status);
    }

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, CacheEnhancementPostProcessor.CacheStats>> getCacheStats() {
        return ResponseEntity.ok(cacheProcessor.getCacheStats());
    }

    /**
     * 获取验证统计信息
     */
    @GetMapping("/validation/stats")
    public ResponseEntity<Map<String, ValidationEnhancementPostProcessor.ValidationStats>> getValidationStats() {
        return ResponseEntity.ok(validationProcessor.getValidationStats());
    }

    /**
     * 清除所有缓存
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, Object>> clearCache() {
        int oldSize = cacheProcessor.getCacheSize();
        cacheProcessor.clearCache();
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "缓存已清除");
        result.put("clearedEntries", oldSize);
        result.put("currentSize", cacheProcessor.getCacheSize());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 清除缓存统计信息
     */
    @DeleteMapping("/cache/stats")
    public ResponseEntity<Map<String, String>> clearCacheStats() {
        cacheProcessor.clearCacheStats();
        
        Map<String, String> result = new HashMap<>();
        result.put("message", "缓存统计信息已清除");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 清除验证统计信息
     */
    @DeleteMapping("/validation/stats")
    public ResponseEntity<Map<String, String>> clearValidationStats() {
        validationProcessor.clearValidationStats();
        
        Map<String, String> result = new HashMap<>();
        result.put("message", "验证统计信息已清除");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 清除过期缓存
     */
    @PostMapping("/cache/evict")
    public ResponseEntity<Map<String, Object>> evictExpiredCache() {
        int oldSize = cacheProcessor.getCacheSize();
        cacheProcessor.evictExpiredEntries();
        int newSize = cacheProcessor.getCacheSize();
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "过期缓存已清除");
        result.put("evictedEntries", oldSize - newSize);
        result.put("remainingEntries", newSize);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取系统增强功能概览
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getEnhancementOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        // 缓存概览
        Map<String, CacheEnhancementPostProcessor.CacheStats> cacheStats = cacheProcessor.getCacheStats();
        long totalCacheHits = cacheStats.values().stream().mapToLong(s -> s.hitCount).sum();
        long totalCacheMisses = cacheStats.values().stream().mapToLong(s -> s.missCount).sum();
        double overallCacheHitRate = totalCacheHits + totalCacheMisses > 0 
                ? (double) totalCacheHits / (totalCacheHits + totalCacheMisses) : 0.0;
        
        Map<String, Object> cacheOverview = new HashMap<>();
        cacheOverview.put("totalMethods", cacheStats.size());
        cacheOverview.put("totalHits", totalCacheHits);
        cacheOverview.put("totalMisses", totalCacheMisses);
        cacheOverview.put("hitRate", String.format("%.2f%%", overallCacheHitRate * 100));
        cacheOverview.put("cacheSize", cacheProcessor.getCacheSize());
        
        // 验证概览
        Map<String, ValidationEnhancementPostProcessor.ValidationStats> validationStats = validationProcessor.getValidationStats();
        long totalValidations = validationStats.values().stream().mapToLong(s -> s.totalCount).sum();
        long totalValidationSuccesses = validationStats.values().stream().mapToLong(s -> s.successCount).sum();
        double overallValidationSuccessRate = totalValidations > 0 
                ? (double) totalValidationSuccesses / totalValidations : 0.0;
        
        Map<String, Object> validationOverview = new HashMap<>();
        validationOverview.put("totalMethods", validationStats.size());
        validationOverview.put("totalValidations", totalValidations);
        validationOverview.put("totalSuccesses", totalValidationSuccesses);
        validationOverview.put("successRate", String.format("%.2f%%", overallValidationSuccessRate * 100));
        
        overview.put("cache", cacheOverview);
        overview.put("validation", validationOverview);
        
        return ResponseEntity.ok(overview);
    }
}
