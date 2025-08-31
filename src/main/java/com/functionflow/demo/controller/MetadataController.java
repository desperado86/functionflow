package com.functionflow.demo.controller;

import com.functionflow.demo.core.FunctionMetadataManager;
import com.functionflow.demo.model.FunctionMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 函数元数据 API 控制器
 * 提供函数元数据的查询和管理接口
 */
@RestController
@RequestMapping("/api/metadata")
@RequiredArgsConstructor
public class MetadataController {

    private final FunctionMetadataManager metadataManager;

    /**
     * 获取所有函数元数据
     */
    @GetMapping("/functions")
    public ResponseEntity<Collection<FunctionMetadata>> getAllFunctionMetadata() {
        return ResponseEntity.ok(metadataManager.getAllFunctionMetadata());
    }

    /**
     * 根据函数ID获取元数据
     */
    @GetMapping("/functions/{functionId}")
    public ResponseEntity<FunctionMetadata> getFunctionMetadata(@PathVariable String functionId) {
        FunctionMetadata metadata = metadataManager.getFunctionMetadata(functionId);
        if (metadata != null) {
            return ResponseEntity.ok(metadata);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取所有类元数据
     */
    @GetMapping("/classes")
    public ResponseEntity<Collection<FunctionMetadataManager.ClassMetadata>> getAllClassMetadata() {
        return ResponseEntity.ok(metadataManager.getAllClassMetadata());
    }

    /**
     * 根据类名获取类元数据
     */
    @GetMapping("/classes/{className}")
    public ResponseEntity<FunctionMetadataManager.ClassMetadata> getClassMetadata(@PathVariable String className) {
        FunctionMetadataManager.ClassMetadata metadata = metadataManager.getClassMetadata(className);
        if (metadata != null) {
            return ResponseEntity.ok(metadata);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 根据分类获取函数元数据
     */
    @GetMapping("/functions/category/{category}")
    public ResponseEntity<List<FunctionMetadata>> getFunctionsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(metadataManager.getFunctionsByCategory(category));
    }

    /**
     * 根据命名空间获取函数元数据
     */
    @GetMapping("/functions/namespace/{namespace}")
    public ResponseEntity<List<FunctionMetadata>> getFunctionsByNamespace(@PathVariable String namespace) {
        return ResponseEntity.ok(metadataManager.getFunctionsByNamespace(namespace));
    }

    /**
     * 搜索函数元数据
     */
    @GetMapping("/functions/search")
    public ResponseEntity<List<FunctionMetadata>> searchFunctions(@RequestParam String keyword) {
        return ResponseEntity.ok(metadataManager.searchFunctions(keyword));
    }

    /**
     * 获取元数据统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<FunctionMetadataManager.MetadataStats> getMetadataStats() {
        return ResponseEntity.ok(metadataManager.getMetadataStats());
    }

    /**
     * 获取函数分类统计
     */
    @GetMapping("/stats/categories")
    public ResponseEntity<Map<String, Long>> getCategoryStats() {
        FunctionMetadataManager.MetadataStats stats = metadataManager.getMetadataStats();
        return ResponseEntity.ok(stats.getCategoryStats());
    }

    /**
     * 获取命名空间统计
     */
    @GetMapping("/stats/namespaces")
    public ResponseEntity<Map<String, Long>> getNamespaceStats() {
        FunctionMetadataManager.MetadataStats stats = metadataManager.getMetadataStats();
        return ResponseEntity.ok(stats.getNamespaceStats());
    }

    /**
     * 获取函数性能统计
     */
    @GetMapping("/stats/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceStats() {
        Collection<FunctionMetadata> allMetadata = metadataManager.getAllFunctionMetadata();
        
        Map<String, Object> performanceStats = new HashMap<>();
        
        // 计算总体统计
        long totalInvokes = allMetadata.stream().mapToLong(FunctionMetadata::getInvokeCount).sum();
        long totalSuccesses = allMetadata.stream().mapToLong(FunctionMetadata::getSuccessCount).sum();
        long totalFailures = allMetadata.stream().mapToLong(FunctionMetadata::getFailureCount).sum();
        double avgExecutionTime = allMetadata.stream()
                .filter(m -> m.getInvokeCount() > 0)
                .mapToDouble(FunctionMetadata::getAvgExecutionTime)
                .average()
                .orElse(0.0);
        
        performanceStats.put("totalInvokes", totalInvokes);
        performanceStats.put("totalSuccesses", totalSuccesses);
        performanceStats.put("totalFailures", totalFailures);
        performanceStats.put("overallSuccessRate", totalInvokes > 0 ? (double) totalSuccesses / totalInvokes : 0.0);
        performanceStats.put("avgExecutionTime", avgExecutionTime);
        
        // 最活跃的函数
        List<Map<String, Object>> mostActive = allMetadata.stream()
                .filter(m -> m.getInvokeCount() > 0)
                .sorted((a, b) -> Long.compare(b.getInvokeCount(), a.getInvokeCount()))
                .limit(5)
                .map(m -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("functionId", m.getFunctionId());
                    result.put("name", m.getName());
                    result.put("invokeCount", m.getInvokeCount());
                    result.put("successRate", m.getSuccessRate());
                    result.put("avgExecutionTime", m.getAvgExecutionTime());
                    return result;
                })
                .collect(java.util.stream.Collectors.toList());
        
        performanceStats.put("mostActiveFunctions", mostActive);
        
        // 最慢的函数
        List<Map<String, Object>> slowest = allMetadata.stream()
                .filter(m -> m.getInvokeCount() > 0)
                .sorted((a, b) -> Double.compare(b.getAvgExecutionTime(), a.getAvgExecutionTime()))
                .limit(5)
                .map(m -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("functionId", m.getFunctionId());
                    result.put("name", m.getName());
                    result.put("avgExecutionTime", m.getAvgExecutionTime());
                    result.put("invokeCount", m.getInvokeCount());
                    return result;
                })
                .collect(java.util.stream.Collectors.toList());
        
        performanceStats.put("slowestFunctions", slowest);
        
        return ResponseEntity.ok(performanceStats);
    }

    /**
     * 刷新元数据
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshMetadata() {
        metadataManager.refreshMetadata();
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "元数据刷新成功");
        result.put("stats", metadataManager.getMetadataStats());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 重置函数统计信息
     */
    @PostMapping("/functions/{functionId}/reset-stats")
    public ResponseEntity<Map<String, String>> resetFunctionStats(@PathVariable String functionId) {
        FunctionMetadata metadata = metadataManager.getFunctionMetadata(functionId);
        if (metadata != null) {
            metadata.resetStats();
            
            Map<String, String> result = new HashMap<>();
            result.put("message", "函数统计信息重置成功");
            result.put("functionId", functionId);
            
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 批量重置所有函数统计信息
     */
    @PostMapping("/functions/reset-all-stats")
    public ResponseEntity<Map<String, Object>> resetAllFunctionStats() {
        Collection<FunctionMetadata> allMetadata = metadataManager.getAllFunctionMetadata();
        
        allMetadata.forEach(FunctionMetadata::resetStats);
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "所有函数统计信息重置成功");
        result.put("resetCount", allMetadata.size());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取函数调用历史（简化版）
     */
    @GetMapping("/functions/{functionId}/history")
    public ResponseEntity<Map<String, Object>> getFunctionHistory(@PathVariable String functionId) {
        FunctionMetadata metadata = metadataManager.getFunctionMetadata(functionId);
        if (metadata != null) {
            Map<String, Object> history = new HashMap<>();
            history.put("functionId", functionId);
            history.put("name", metadata.getName());
            history.put("totalInvokes", metadata.getInvokeCount());
            history.put("successCount", metadata.getSuccessCount());
            history.put("failureCount", metadata.getFailureCount());
            history.put("successRate", metadata.getSuccessRate());
            history.put("avgExecutionTime", metadata.getAvgExecutionTime());
            history.put("lastInvokeTime", metadata.getLastInvokeTime());
            history.put("lastExecutionTime", metadata.getLastExecutionTime());
            history.put("lastError", metadata.getLastError());
            
            return ResponseEntity.ok(history);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
