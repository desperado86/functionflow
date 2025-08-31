package com.functionflow.demo;

import com.functionflow.demo.core.FunctionScanner;
import com.functionflow.demo.core.FunctionExecutionEngine;
import com.functionflow.demo.functions.MathFunctions;
import com.functionflow.demo.functions.StringFunctions;
import com.functionflow.demo.model.FunctionSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 函数流工作引擎测试
 */
@SpringBootTest
public class FunctionFlowTest {
    
    @Autowired
    private FunctionScanner discoveryService;
    
    @Autowired
    private FunctionExecutionEngine executionEngine;
    
    @Test
    public void testFunctionDiscovery() {
        // 发现函数
        discoveryService.discoverFunctions(MathFunctions.class);
        discoveryService.discoverFunctions(StringFunctions.class);
        
        // 验证函数是否被发现
        List<FunctionSchema> functions = discoveryService.getAllFunctions();
        assertFalse(functions.isEmpty(), "应该发现一些函数");
        
        // 验证特定函数
        FunctionSchema addFunction = discoveryService.getFunction("com.functionflow.demo.functions.MathFunctions.add");
        assertNotNull(addFunction, "应该找到加法函数");
        assertEquals("加法", addFunction.getName());
        assertEquals("数学运算", addFunction.getCategory());
        assertEquals(2, addFunction.getInputs().size(), "加法函数应该有2个输入参数");
    }
    
    @Test
    public void testFunctionExecution() {
        // 发现函数
        discoveryService.discoverFunctions(MathFunctions.class);
        
        // 执行加法函数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("a", 5.0);
        parameters.put("b", 3.0);
        
        Object result = executionEngine.executeFunction("com.functionflow.demo.functions.MathFunctions.add", parameters);
        assertEquals(8.0, result, "5 + 3 应该等于 8");
    }
    
    @Test
    public void testStringFunctionExecution() {
        // 发现函数
        discoveryService.discoverFunctions(StringFunctions.class);
        
        // 执行字符串转大写函数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("text", "hello world");
        
        Object result = executionEngine.executeFunction("com.functionflow.demo.functions.StringFunctions.toUpperCase", parameters);
        assertEquals("HELLO WORLD", result, "字符串应该转换为大写");
    }
    
    @Test
    public void testFunctionValidation() {
        // 发现函数
        discoveryService.discoverFunctions(MathFunctions.class);
        
        // 测试缺少必需参数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("a", 5.0);
        // 缺少参数 b
        
        assertThrows(RuntimeException.class, () -> {
            executionEngine.executeFunction("com.functionflow.demo.functions.MathFunctions.add", parameters);
        }, "缺少必需参数应该抛出异常");
    }
    
    @Test
    public void testFunctionWithValidation() {
        // 发现函数
        discoveryService.discoverFunctions(StringFunctions.class);
        
        // 测试字符串长度验证
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("str1", ""); // 空字符串，应该违反 @Size(min = 1) 约束
        parameters.put("str2", "test");
        
        assertThrows(RuntimeException.class, () -> {
            executionEngine.executeFunction("com.functionflow.demo.functions.StringFunctions.concat", parameters);
        }, "违反验证约束应该抛出异常");
    }
    
    @Test
    public void testAsyncFunctionExecution() {
        // 发现函数
        discoveryService.discoverFunctions(MathFunctions.class);
        
        // 执行异步函数（虽然当前函数不是异步的，但可以测试异步执行接口）
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("a", 10.0);
        parameters.put("b", 5.0);
        
        assertThrows(RuntimeException.class, () -> {
            executionEngine.executeFunctionAsync("com.functionflow.demo.functions.MathFunctions.add", parameters);
        }, "非异步函数不应该支持异步执行");
    }
}
