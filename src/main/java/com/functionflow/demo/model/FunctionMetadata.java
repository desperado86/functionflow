package com.functionflow.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 函数元数据模型
 * 包含函数的所有元信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionMetadata {
    
    /**
     * 函数唯一标识符
     */
    private String functionId;
    
    /**
     * 方法名
     */
    private String methodName;
    
    /**
     * 所属类名
     */
    private String className;
    
    /**
     * Spring Bean 名称
     */
    private String beanName;
    
    /**
     * 函数显示名称
     */
    private String name;
    
    /**
     * 函数描述
     */
    private String description;
    
    /**
     * 函数分类
     */
    private String category;
    
    /**
     * 函数版本
     */
    private String version;
    
    /**
     * 命名空间
     */
    private String namespace;
    
    /**
     * 是否异步执行
     */
    private boolean async;
    
    /**
     * 是否可缓存
     */
    private boolean cacheable;
    
    /**
     * 缓存时间（秒）
     */
    private int cacheTime;
    
    /**
     * 超时时间（毫秒）
     */
    private long timeout;
    
    /**
     * 参数数量
     */
    private int parameterCount;
    
    /**
     * 返回值类型
     */
    private String returnType;
    
    /**
     * 参数类型列表
     */
    private List<String> parameterTypes;
    
    /**
     * 函数 Schema（详细的输入输出定义）
     */
    private FunctionSchema functionSchema;
    
    /**
     * 扫描时间
     */
    private LocalDateTime scanTime;
    
    /**
     * 最后调用时间
     */
    private LocalDateTime lastInvokeTime;
    
    /**
     * 调用次数
     */
    private long invokeCount;
    
    /**
     * 平均执行时间（毫秒）
     */
    private double avgExecutionTime;
    
    /**
     * 最后执行时间（毫秒）
     */
    private long lastExecutionTime;
    
    /**
     * 成功调用次数
     */
    private long successCount;
    
    /**
     * 失败调用次数
     */
    private long failureCount;
    
    /**
     * 最后错误信息
     */
    private String lastError;
    
    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = true;
    
    /**
     * 标签
     */
    private List<String> tags;
    
    /**
     * 自定义属性
     */
    private java.util.Map<String, Object> properties;
    
    // ===== 计算属性 =====
    
    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (invokeCount == 0) {
            return 0.0;
        }
        return (double) successCount / invokeCount;
    }
    
    /**
     * 获取失败率
     */
    public double getFailureRate() {
        if (invokeCount == 0) {
            return 0.0;
        }
        return (double) failureCount / invokeCount;
    }
    
    /**
     * 是否有错误
     */
    public boolean hasErrors() {
        return failureCount > 0;
    }
    
    /**
     * 获取简单类名
     */
    public String getSimpleClassName() {
        if (className == null) {
            return null;
        }
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }
    
    /**
     * 获取包名
     */
    public String getPackageName() {
        if (className == null) {
            return null;
        }
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(0, lastDot) : "";
    }
    
    /**
     * 更新调用统计
     */
    public void updateInvokeStats(boolean success, long executionTime) {
        this.invokeCount++;
        this.lastInvokeTime = LocalDateTime.now();
        this.lastExecutionTime = executionTime;
        
        if (success) {
            this.successCount++;
        } else {
            this.failureCount++;
        }
        
        // 更新平均执行时间
        if (this.avgExecutionTime == 0) {
            this.avgExecutionTime = executionTime;
        } else {
            this.avgExecutionTime = (this.avgExecutionTime * (invokeCount - 1) + executionTime) / invokeCount;
        }
    }
    
    /**
     * 设置错误信息
     */
    public void setLastError(String error) {
        this.lastError = error;
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        this.invokeCount = 0;
        this.successCount = 0;
        this.failureCount = 0;
        this.avgExecutionTime = 0;
        this.lastExecutionTime = 0;
        this.lastInvokeTime = null;
        this.lastError = null;
    }
}
