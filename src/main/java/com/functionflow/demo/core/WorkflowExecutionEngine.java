package com.functionflow.demo.core;

import com.functionflow.demo.model.Workflow;
import com.functionflow.demo.model.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 工作流执行引擎
 */
@Slf4j
@Service
public class WorkflowExecutionEngine {
    
    @Autowired
    private FunctionExecutionEngine functionEngine;
    
    private final Map<String, Workflow> workflowRegistry = new ConcurrentHashMap<>();
    
    /**
     * 注册工作流
     */
    public void registerWorkflow(Workflow workflow) {
        workflowRegistry.put(workflow.getId(), workflow);
        log.info("注册工作流: {}", workflow.getId());
    }
    
    /**
     * 执行工作流
     */
    public Map<String, Object> executeWorkflow(String workflowId, Map<String, Object> inputs) {
        Workflow workflow = workflowRegistry.get(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException("工作流不存在: " + workflowId);
        }
        
        log.info("开始执行工作流: {}", workflowId);
        
        // 验证输入
        validateWorkflowInputs(workflow, inputs);
        
        // 构建执行上下文
        ExecutionContext context = new ExecutionContext(inputs);
        
        // 拓扑排序获取执行顺序
        List<WorkflowNode> executionOrder = topologicalSort(workflow);
        
        // 按顺序执行节点
        for (WorkflowNode node : executionOrder) {
            executeNode(node, workflow, context);
        }
        
        // 构建输出
        Map<String, Object> outputs = buildWorkflowOutputs(workflow, context);
        
        log.info("工作流执行完成: {}", workflowId);
        return outputs;
    }
    
    /**
     * 异步执行工作流
     */
    public CompletableFuture<Map<String, Object>> executeWorkflowAsync(String workflowId, Map<String, Object> inputs) {
        return CompletableFuture.supplyAsync(() -> executeWorkflow(workflowId, inputs));
    }
    
    /**
     * 验证工作流输入
     */
    private void validateWorkflowInputs(Workflow workflow, Map<String, Object> inputs) {
        if (workflow.getInputs() == null) {
            return;
        }
        
        for (var input : workflow.getInputs()) {
            if (input.isRequired() && !inputs.containsKey(input.getName())) {
                throw new IllegalArgumentException("缺少必需的工作流输入: " + input.getName());
            }
        }
    }
    
    /**
     * 拓扑排序
     */
    private List<WorkflowNode> topologicalSort(Workflow workflow) {
        Map<String, WorkflowNode> nodeMap = workflow.getNodes().stream()
                .collect(Collectors.toMap(WorkflowNode::getId, node -> node));
        
        Map<String, List<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        
        // 初始化
        for (WorkflowNode node : workflow.getNodes()) {
            graph.put(node.getId(), new ArrayList<>());
            inDegree.put(node.getId(), 0);
        }
        
        // 构建图
        for (var connection : workflow.getConnections()) {
            String sourceId = connection.getSourceNodeId();
            String targetId = connection.getTargetNodeId();
            
            graph.get(sourceId).add(targetId);
            inDegree.put(targetId, inDegree.get(targetId) + 1);
        }
        
        // 拓扑排序
        List<WorkflowNode> result = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();
        
        for (String nodeId : inDegree.keySet()) {
            if (inDegree.get(nodeId) == 0) {
                queue.offer(nodeId);
            }
        }
        
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            result.add(nodeMap.get(nodeId));
            
            for (String neighbor : graph.get(nodeId)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        
        if (result.size() != workflow.getNodes().size()) {
            throw new RuntimeException("工作流存在循环依赖");
        }
        
        return result;
    }
    
    /**
     * 执行节点
     */
    private void executeNode(WorkflowNode node, Workflow workflow, ExecutionContext context) {
        log.info("执行节点: {}", node.getId());
        
        // 准备节点输入
        Map<String, Object> nodeInputs = prepareNodeInputs(node, workflow, context);
        
        Object result;
        if ("function".equals(node.getType())) {
            // 执行函数节点
            result = functionEngine.executeFunction(node.getFunctionId(), nodeInputs);
        } else if ("module".equals(node.getType())) {
            // 执行模块节点
            result = executeWorkflow(node.getModuleId(), nodeInputs);
        } else {
            throw new IllegalArgumentException("不支持的节点类型: " + node.getType());
        }
        
        // 存储节点输出
        context.setNodeOutput(node.getId(), result);
        
        log.info("节点执行完成: {} -> {}", node.getId(), result);
    }
    
    /**
     * 准备节点输入
     */
    private Map<String, Object> prepareNodeInputs(WorkflowNode node, Workflow workflow, ExecutionContext context) {
        Map<String, Object> inputs = new HashMap<>();
        
        // 从工作流输入映射
        if (node.getInputMapping() != null) {
            for (Map.Entry<String, String> entry : node.getInputMapping().entrySet()) {
                String nodeInput = entry.getKey();
                String workflowInput = entry.getValue();
                
                if (context.hasWorkflowInput(workflowInput)) {
                    inputs.put(nodeInput, context.getWorkflowInput(workflowInput));
                }
            }
        }
        
        // 从其他节点输出映射
        for (var connection : workflow.getConnections()) {
            if (connection.getTargetNodeId().equals(node.getId())) {
                String sourceNodeId = connection.getSourceNodeId();
                String sourcePort = connection.getSourcePort();
                String targetPort = connection.getTargetPort();
                
                if (context.hasNodeOutput(sourceNodeId)) {
                    Object sourceOutput = context.getNodeOutput(sourceNodeId);
                    inputs.put(targetPort, sourceOutput);
                }
            }
        }
        
        return inputs;
    }
    
    /**
     * 构建工作流输出
     */
    private Map<String, Object> buildWorkflowOutputs(Workflow workflow, ExecutionContext context) {
        Map<String, Object> outputs = new HashMap<>();
        
        if (workflow.getOutputs() == null) {
            return outputs;
        }
        
        for (var output : workflow.getOutputs()) {
            // 这里可以根据输出映射规则来构建输出
            // 简化实现：直接返回最后一个节点的输出
            if (!workflow.getNodes().isEmpty()) {
                String lastNodeId = workflow.getNodes().get(workflow.getNodes().size() - 1).getId();
                if (context.hasNodeOutput(lastNodeId)) {
                    outputs.put(output.getName(), context.getNodeOutput(lastNodeId));
                }
            }
        }
        
        return outputs;
    }
    
    /**
     * 执行上下文
     */
    private static class ExecutionContext {
        private final Map<String, Object> workflowInputs;
        private final Map<String, Object> nodeOutputs = new HashMap<>();
        
        public ExecutionContext(Map<String, Object> workflowInputs) {
            this.workflowInputs = workflowInputs;
        }
        
        public boolean hasWorkflowInput(String name) {
            return workflowInputs.containsKey(name);
        }
        
        public Object getWorkflowInput(String name) {
            return workflowInputs.get(name);
        }
        
        public void setNodeOutput(String nodeId, Object output) {
            nodeOutputs.put(nodeId, output);
        }
        
        public boolean hasNodeOutput(String nodeId) {
            return nodeOutputs.containsKey(nodeId);
        }
        
        public Object getNodeOutput(String nodeId) {
            return nodeOutputs.get(nodeId);
        }
    }
}
