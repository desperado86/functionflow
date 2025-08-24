package com.functionflow.demo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * 工作流节点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowNode {
    
    /**
     * 节点ID
     */
    private String id;
    
    /**
     * 节点类型
     */
    private String type;
    
    /**
     * 节点名称
     */
    private String name;
    
    /**
     * 函数ID（如果是函数节点）
     */
    private String functionId;
    
    /**
     * 模块ID（如果是模块节点）
     */
    private String moduleId;
    
    /**
     * 节点位置
     */
    private Position position;
    
    /**
     * 节点配置
     */
    private Map<String, Object> config;
    
    /**
     * 输入映射
     */
    private Map<String, String> inputMapping;
    
    /**
     * 输出映射
     */
    private Map<String, String> outputMapping;
    
    /**
     * 节点位置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private int x;
        private int y;
    }
}
