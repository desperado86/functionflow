package com.functionflow.demo.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.functionflow.demo.model.FunctionSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 函数执行引擎
 */
@Slf4j
@Service
public class FunctionExecutionEngine {
    
    @Autowired
    private FunctionScanner discoveryService;
    
    @Autowired
    private ValidationService validationService;
    
    @Autowired
    private Executor asyncExecutor;
    
    private final Map<String, Object> instanceCache = new ConcurrentHashMap<>();
    
    /**
     * 同步执行函数
     */
    public Object executeFunction(String functionId, Map<String, Object> parameters) {
        try {
            FunctionSchema schema = discoveryService.getFunction(functionId);
            Method method = discoveryService.getFunctionMethod(functionId);
            
            if (schema == null || method == null) {
                throw new IllegalArgumentException("函数不存在: " + functionId);
            }
            
            // 验证参数
            validateParameters(schema, parameters);
            
            // 准备参数
            Object[] args = prepareArguments(method, schema, parameters);
            
            // 获取实例
            Object instance = getInstance(method.getDeclaringClass());
            
            // 执行函数
            Object result = method.invoke(instance, args);
            
            log.info("函数执行成功: {} -> {}", functionId, result);
            return result;
            
        } catch (Exception e) {
            log.error("函数执行失败: {}", functionId, e);
            throw new RuntimeException("函数执行失败: " + functionId, e);
        }
    }
    
    /**
     * 异步执行函数
     */
    public CompletableFuture<Object> executeFunctionAsync(String functionId, Map<String, Object> parameters) {
        FunctionSchema schema = discoveryService.getFunction(functionId);
        if (schema == null) {
            throw new IllegalArgumentException("函数不存在: " + functionId);
        }
        
        if (!schema.isAsync()) {
            throw new IllegalArgumentException("函数不支持异步执行: " + functionId);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeFunction(functionId, parameters);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, asyncExecutor).orTimeout(schema.getTimeout(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * 验证参数
     */
    private void validateParameters(FunctionSchema schema, Map<String, Object> parameters) {
        for (var input : schema.getInputs()) {
            if (input.isRequired() && !parameters.containsKey(input.getName())) {
                throw new IllegalArgumentException("缺少必需参数: " + input.getName());
            }
            
            if (parameters.containsKey(input.getName())) {
                Object value = parameters.get(input.getName());
                validateParameterValue(input, value);
            }
        }
    }
    
    /**
     * 验证参数值
     */
    private void validateParameterValue(com.functionflow.demo.model.ParameterSchema input, Object value) {
        if (input.getValidation() == null || input.getValidation().isEmpty()) {
            return;
        }
        
        // 使用 Hibernate Validator 进行验证
        try {
            // 创建一个临时的验证对象
            Object validationObject = createValidationObject(input, value);
            
            // 使用验证服务进行验证
            Map<String, String> errors = validationService.validateAndGetErrors(validationObject);
            
            if (!errors.isEmpty()) {
                String errorMessage = String.join("; ", errors.values());
                throw new IllegalArgumentException("参数验证失败 [" + input.getName() + "]: " + errorMessage);
            }
        } catch (Exception e) {
            log.debug("参数验证失败: {}", e.getMessage());
            // 如果验证服务失败，回退到基本验证
            performBasicValidation(input, value);
        }
    }
    
    /**
     * 创建验证对象
     */
    private Object createValidationObject(com.functionflow.demo.model.ParameterSchema input, Object value) {
        // 这里可以创建一个包含验证注解的临时对象
        // 为了简化，我们直接使用基本验证
        return value;
    }
    
    /**
     * 基本验证（回退方案）
     */
    private void performBasicValidation(com.functionflow.demo.model.ParameterSchema input, Object value) {
        Map<String, Object> validation = input.getValidation();
        
        // 检查最小值
        if (validation.containsKey("min") && value instanceof Number) {
            Number min = (Number) validation.get("min");
            Number numValue = (Number) value;
            if (numValue.doubleValue() < min.doubleValue()) {
                throw new IllegalArgumentException("参数值小于最小值: " + input.getName());
            }
        }
        
        // 检查最大值
        if (validation.containsKey("max") && value instanceof Number) {
            Number max = (Number) validation.get("max");
            Number numValue = (Number) value;
            if (numValue.doubleValue() > max.doubleValue()) {
                throw new IllegalArgumentException("参数值大于最大值: " + input.getName());
            }
        }
        
        // 检查字符串长度
        if (validation.containsKey("minSize") && value instanceof String) {
            Integer minSize = (Integer) validation.get("minSize");
            String strValue = (String) value;
            if (strValue.length() < minSize) {
                throw new IllegalArgumentException("字符串长度小于最小值: " + input.getName());
            }
        }
        
        if (validation.containsKey("maxSize") && value instanceof String) {
            Integer maxSize = (Integer) validation.get("maxSize");
            String strValue = (String) value;
            if (strValue.length() > maxSize) {
                throw new IllegalArgumentException("字符串长度大于最大值: " + input.getName());
            }
        }
    }
    
    /**
     * 准备函数参数
     */
    private Object[] prepareArguments(Method method, FunctionSchema schema, Map<String, Object> parameters) {
        Object[] args = new Object[method.getParameterCount()];
        
        for (int i = 0; i < schema.getInputs().size(); i++) {
            var input = schema.getInputs().get(i);
            String paramName = input.getName();
            
            if (parameters.containsKey(paramName)) {
                args[i] = parameters.get(paramName);
            } else if (input.getDefaultValue() != null) {
                args[i] = convertValue(input.getDefaultValue(), method.getParameterTypes()[i]);
            } else {
                args[i] = null;
            }
        }
        
        return args;
    }
    
    /**
     * 类型转换
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        if (targetType == String.class) {
            return value.toString();
        }
        
        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        }
        
        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        }
        
        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        }
        
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            return Boolean.parseBoolean(value.toString());
        }
        
        // 处理复杂对象转换
        if (value instanceof Map && !targetType.isPrimitive() && !targetType.isArray()) {
            return convertMapToObject((Map<String, Object>) value, targetType);
        }
        
        throw new IllegalArgumentException("无法转换类型: " + value.getClass() + " -> " + targetType);
    }
    
    /**
     * 将Map转换为对象
     */
    private Object convertMapToObject(Map<String, Object> map, Class<?> targetType) {
        try {
            // 使用Jackson进行对象转换
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return objectMapper.convertValue(map, targetType);
        } catch (Exception e) {
            log.warn("使用Jackson转换失败，尝试手动转换: {}", e.getMessage());
            return convertMapToObjectManually(map, targetType);
        }
    }
    
    /**
     * 手动将Map转换为对象
     */
    private Object convertMapToObjectManually(Map<String, Object> map, Class<?> targetType) {
        try {
            Object instance = targetType.getDeclaredConstructor().newInstance();
            
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String fieldName = entry.getKey();
                Object fieldValue = entry.getValue();
                
                try {
                    Field field = targetType.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    
                    // 递归处理嵌套对象
                    if (fieldValue instanceof Map && !field.getType().isPrimitive() && !field.getType().isArray()) {
                        fieldValue = convertMapToObjectManually((Map<String, Object>) fieldValue, field.getType());
                    }
                    
                    field.set(instance, fieldValue);
                } catch (NoSuchFieldException e) {
                    log.debug("字段不存在: {}", fieldName);
                } catch (Exception e) {
                    log.warn("设置字段失败: {} = {}", fieldName, fieldValue, e);
                }
            }
            
            return instance;
        } catch (Exception e) {
            throw new IllegalArgumentException("无法创建对象实例: " + targetType.getName(), e);
        }
    }
    
    /**
     * 获取实例
     */
    private Object getInstance(Class<?> clazz) throws Exception {
        String className = clazz.getName();
        
        if (instanceCache.containsKey(className)) {
            return instanceCache.get(className);
        }
        
        Object instance = clazz.getDeclaredConstructor().newInstance();
        instanceCache.put(className, instance);
        
        return instance;
    }
}
