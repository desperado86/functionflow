package com.functionflow.demo.controller;

import com.functionflow.demo.core.FunctionScanner;
import com.functionflow.demo.core.FunctionExecutionEngine;
import com.functionflow.demo.model.FunctionSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 函数管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/functions")
public class FunctionController {
    
    @Autowired
    private FunctionScanner discoveryService;
    
    @Autowired
    private FunctionExecutionEngine executionEngine;
    
    /**
     * 获取所有函数
     */
    @GetMapping
    public ResponseEntity<List<FunctionSchema>> getAllFunctions() {
        List<FunctionSchema> functions = discoveryService.getAllFunctions();
        return ResponseEntity.ok(functions);
    }
    
    /**
     * 根据ID获取函数
     */
    @GetMapping("/{functionId}")
    public ResponseEntity<FunctionSchema> getFunction(@PathVariable String functionId) {
        FunctionSchema function = discoveryService.getFunction(functionId);
        if (function == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(function);
    }
    
    /**
     * 执行函数
     */
    @PostMapping("/{functionId}/execute")
    public ResponseEntity<Object> executeFunction(
            @PathVariable String functionId,
            @RequestBody Map<String, Object> parameters) {
        try {
            Object result = executionEngine.executeFunction(functionId, parameters);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("函数执行失败: {}", functionId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 异步执行函数
     */
    @PostMapping("/{functionId}/execute-async")
    public ResponseEntity<Map<String, Object>> executeFunctionAsync(
            @PathVariable String functionId,
            @RequestBody Map<String, Object> parameters) {
        try {
            var future = executionEngine.executeFunctionAsync(functionId, parameters);
            return ResponseEntity.ok(Map.of(
                "taskId", java.util.UUID.randomUUID().toString(),
                "status", "started",
                "message", "异步任务已启动"
            ));
        } catch (Exception e) {
            log.error("异步函数执行失败: {}", functionId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 获取函数JSON Schema
     */
    @GetMapping("/{functionId}/schema")
    public ResponseEntity<Map<String, Object>> getFunctionSchema(@PathVariable String functionId) {
        FunctionSchema function = discoveryService.getFunction(functionId);
        if (function == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 构建JSON Schema
        Map<String, Object> schema = buildJsonSchema(function);
        return ResponseEntity.ok(schema);
    }
    
    /**
     * 构建JSON Schema
     */
    private Map<String, Object> buildJsonSchema(FunctionSchema function) {
        Map<String, Object> schema = new java.util.HashMap<>();
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put("type", "object");
        schema.put("title", function.getName());
        schema.put("description", function.getDescription());
        
        // 属性定义
        Map<String, Object> properties = new java.util.HashMap<>();
        List<String> required = new java.util.ArrayList<>();
        
        if (function.getInputs() != null) {
            for (var input : function.getInputs()) {
                Map<String, Object> property = new java.util.HashMap<>();
                property.put("type", getJsonSchemaType(input.getType()));
                property.put("description", input.getDescription());
                
                if (input.getDefaultValue() != null) {
                    property.put("default", input.getDefaultValue());
                }
                
                // 添加验证规则
                if (input.getValidation() != null) {
                    for (Map.Entry<String, Object> entry : input.getValidation().entrySet()) {
                        property.put(entry.getKey(), entry.getValue());
                    }
                }
                
                properties.put(input.getName(), property);
                
                if (input.isRequired()) {
                    required.add(input.getName());
                }
            }
        }
        
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        
        return schema;
    }
    
    /**
     * 获取JSON Schema类型
     */
    private String getJsonSchemaType(String javaType) {
        switch (javaType.toLowerCase()) {
            case "string":
                return "string";
            case "integer":
            case "int":
            case "long":
                return "integer";
            case "double":
            case "float":
                return "number";
            case "boolean":
                return "boolean";
            default:
                return "string";
        }
    }
}
