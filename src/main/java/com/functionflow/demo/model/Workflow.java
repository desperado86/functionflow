package com.functionflow.demo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 工作流定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Workflow {
    
    /**
     * 工作流ID
     */
    private String id;
    
    /**
     * 工作流名称
     */
    private String name;
    
    /**
     * 工作流描述
     */
    private String description;
    
    /**
     * 工作流版本
     */
    private String version;
    
    /**
     * 工作流节点
     */
    private List<WorkflowNode> nodes;
    
    /**
     * 工作流连接
     */
    private List<WorkflowConnection> connections;
    
    /**
     * 输入定义
     */
    private List<ParameterSchema> inputs;
    
    /**
     * 输出定义
     */
    private List<ParameterSchema> outputs;
    
    /**
     * 工作流配置
     */
    private Map<String, Object> config;
    
    /**
     * 工作流连接
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowConnection {
        private String id;
        private String sourceNodeId;
        private String sourcePort;
        private String targetNodeId;
        private String targetPort;
    }
}
