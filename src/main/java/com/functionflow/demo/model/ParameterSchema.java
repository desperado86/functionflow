package com.functionflow.demo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * 参数模式定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParameterSchema {
    
    /**
     * 参数名称
     */
    private String name;
    
    /**
     * 参数描述
     */
    private String description;
    
    /**
     * 参数类型
     */
    private String type;
    
    /**
     * 是否必需
     */
    private boolean required;
    
    /**
     * 默认值
     */
    private Object defaultValue;
    
    /**
     * 参数顺序
     */
    private int order;
    
    /**
     * 验证规则
     */
    private Map<String, Object> validation;
    
    /**
     * 额外属性
     */
    private Map<String, Object> properties;
    
    /**
     * JSON Schema
     */
    private JsonNode jsonSchema;
}
