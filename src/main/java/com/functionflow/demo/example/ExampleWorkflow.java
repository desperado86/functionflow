package com.functionflow.demo.example;

import com.functionflow.demo.model.ParameterSchema;
import com.functionflow.demo.model.Workflow;
import com.functionflow.demo.model.WorkflowNode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 示例工作流
 */
@Component
public class ExampleWorkflow implements CommandLineRunner {
    
    @Override
    public void run(String... args) throws Exception {
        // 这里可以创建示例工作流
        // 由于WorkflowExecutionEngine还没有注入，暂时注释掉
        // createExampleWorkflow();
    }
    
    /**
     * 创建示例工作流：计算圆的面积
     */
    public Workflow createCircleAreaWorkflow() {
        // 输入参数：半径
        ParameterSchema radiusInput = ParameterSchema.builder()
                .name("radius")
                .description("圆的半径")
                .type("Double")
                .required(true)
                .order(0)
                .build();
        
        // 输出参数：面积
        ParameterSchema areaOutput = ParameterSchema.builder()
                .name("area")
                .description("圆的面积")
                .type("Double")
                .required(true)
                .order(0)
                .build();
        
        // 创建节点：计算半径的平方
        WorkflowNode squareNode = WorkflowNode.builder()
                .id("square")
                .type("function")
                .name("计算平方")
                .functionId("MathFunctions.pow")
                .position(WorkflowNode.Position.builder().x(100).y(100).build())
                .inputMapping(Map.of("base", "radius", "exponent", "2"))
                .build();
        
        // 创建节点：计算面积（π * r²）
        WorkflowNode areaNode = WorkflowNode.builder()
                .id("area")
                .type("function")
                .name("计算面积")
                .functionId("MathFunctions.multiply")
                .position(WorkflowNode.Position.builder().x(300).y(100).build())
                .inputMapping(Map.of("a", "3.14159", "b", "square"))
                .build();
        
        // 创建连接
        Workflow.WorkflowConnection connection = Workflow.WorkflowConnection.builder()
                .id("conn1")
                .sourceNodeId("square")
                .sourcePort("power")
                .targetNodeId("area")
                .targetPort("b")
                .build();
        
        return Workflow.builder()
                .id("circle-area")
                .name("计算圆的面积")
                .description("根据半径计算圆的面积")
                .version("1.0.0")
                .nodes(Arrays.asList(squareNode, areaNode))
                .connections(Arrays.asList(connection))
                .inputs(Arrays.asList(radiusInput))
                .outputs(Arrays.asList(areaOutput))
                .build();
    }
    
    /**
     * 创建示例工作流：字符串处理
     */
    public Workflow createStringProcessingWorkflow() {
        // 输入参数：原始字符串
        ParameterSchema textInput = ParameterSchema.builder()
                .name("text")
                .description("原始字符串")
                .type("String")
                .required(true)
                .order(0)
                .build();
        
        // 输出参数：处理后的字符串
        ParameterSchema resultOutput = ParameterSchema.builder()
                .name("result")
                .description("处理后的字符串")
                .type("String")
                .required(true)
                .order(0)
                .build();
        
        // 创建节点：转换为大写
        WorkflowNode upperNode = WorkflowNode.builder()
                .id("upper")
                .type("function")
                .name("转大写")
                .functionId("StringFunctions.toUpperCase")
                .position(WorkflowNode.Position.builder().x(100).y(100).build())
                .inputMapping(Map.of("text", "text"))
                .build();
        
        // 创建节点：添加前缀
        WorkflowNode prefixNode = WorkflowNode.builder()
                .id("prefix")
                .type("function")
                .name("添加前缀")
                .functionId("StringFunctions.concat")
                .position(WorkflowNode.Position.builder().x(300).y(100).build())
                .inputMapping(Map.of("str1", "PREFIX: ", "str2", "upper"))
                .build();
        
        // 创建连接
        Workflow.WorkflowConnection connection = Workflow.WorkflowConnection.builder()
                .id("conn1")
                .sourceNodeId("upper")
                .sourcePort("result")
                .targetNodeId("prefix")
                .targetPort("str2")
                .build();
        
        return Workflow.builder()
                .id("string-processing")
                .name("字符串处理")
                .description("将字符串转换为大写并添加前缀")
                .version("1.0.0")
                .nodes(Arrays.asList(upperNode, prefixNode))
                .connections(Arrays.asList(connection))
                .inputs(Arrays.asList(textInput))
                .outputs(Arrays.asList(resultOutput))
                .build();
    }
}
