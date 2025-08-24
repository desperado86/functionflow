package com.functionflow.demo.controller;

import com.functionflow.demo.core.WorkflowExecutionEngine;
import com.functionflow.demo.model.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 工作流管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
    
    @Autowired
    private WorkflowExecutionEngine executionEngine;
    
    /**
     * 注册工作流
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> registerWorkflow(@RequestBody Workflow workflow) {
        try {

            executionEngine.registerWorkflow(workflow);
            return ResponseEntity.ok(Map.of(
                "message", "工作流注册成功",
                "workflowId", workflow.getId()
            ));
        } catch (Exception e) {
            log.error("工作流注册失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 执行工作流
     */
    @PostMapping("/{workflowId}/execute")
    public ResponseEntity<Object> executeWorkflow(
            @PathVariable String workflowId,
            @RequestBody Map<String, Object> inputs) {
        try {
            Map<String, Object> result = executionEngine.executeWorkflow(workflowId, inputs);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("工作流执行失败: {}", workflowId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 异步执行工作流
     */
    @PostMapping("/{workflowId}/execute-async")
    public ResponseEntity<Map<String, Object>> executeWorkflowAsync(
            @PathVariable String workflowId,
            @RequestBody Map<String, Object> inputs) {
        try {
            var future = executionEngine.executeWorkflowAsync(workflowId, inputs);
            return ResponseEntity.ok(Map.of(
                "taskId", java.util.UUID.randomUUID().toString(),
                "status", "started",
                "message", "异步工作流任务已启动"
            ));
        } catch (Exception e) {
            log.error("异步工作流执行失败: {}", workflowId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
