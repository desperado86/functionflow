package com.functionflow.demo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 函数模式定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionSchema {
    
    /**
     * 函数ID
     */
    private String id;
    
    /**
     * 函数名称
     */
    private String name;
    
    /**
     * 函数命名空间
     */
    private String namespace;
    
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
     * 是否异步执行
     */
    private boolean async;
    
    /**
     * 超时时间（毫秒）
     */
    private long timeout;
    
    /**
     * 是否启用缓存
     */
    private boolean cacheable;
    
    /**
     * 缓存时间（秒）
     */
    private long cacheTime;
    
    /**
     * 输入参数定义
     */
    private List<ParameterSchema> inputs;
    
    /**
     * 输出定义
     */
    private List<ParameterSchema> outputs;
    
    /**
     * 验证规则
     */
    private Map<String, Object> validation;
    
    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;
}
