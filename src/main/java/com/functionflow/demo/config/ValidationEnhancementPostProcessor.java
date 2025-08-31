package com.functionflow.demo.config;

import com.functionflow.demo.annotation.Function;
import com.functionflow.demo.core.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证增强后处理器
 * 
 * 功能包括：
 * 1. 自动为函数参数添加验证
 * 2. 缓存验证结果
 * 3. 提供验证统计信息
 */
@Slf4j
@Component
public class ValidationEnhancementPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private ValidationService validationService;
    
    // 验证统计
    private final Map<String, ValidationStats> validationStats = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        
        // 检查是否有需要验证增强的方法
        if (hasValidationAnnotations(beanClass)) {
            log.debug("为组件添加验证增强: {}", beanClass.getSimpleName());
            return createValidationProxy(bean, beanClass);
        }
        
        return bean;
    }

    /**
     * 检查类是否有验证注解
     */
    private boolean hasValidationAnnotations(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Function.class)) {
                for (Parameter parameter : method.getParameters()) {
                    if (hasValidationAnnotations(parameter)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检查参数是否有验证注解
     */
    private boolean hasValidationAnnotations(Parameter parameter) {
        return Arrays.stream(parameter.getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getName().startsWith("jakarta.validation"));
    }

    /**
     * 创建验证代理
     */
    private Object createValidationProxy(Object bean, Class<?> beanClass) {
        try {
            Class<?>[] interfaces = beanClass.getInterfaces();
            
            if (interfaces.length == 0) {
                return bean;
            }
            
            return Proxy.newProxyInstance(
                    beanClass.getClassLoader(),
                    interfaces,
                    new ValidationHandler(bean, beanClass)
            );
        } catch (Exception e) {
            log.warn("创建验证代理失败: {}", beanClass.getSimpleName(), e);
            return bean;
        }
    }

    /**
     * 验证处理器
     */
    private class ValidationHandler implements java.lang.reflect.InvocationHandler {
        private final Object target;
        private final Class<?> targetClass;

        public ValidationHandler(Object target, Class<?> targetClass) {
            this.target = target;
            this.targetClass = targetClass;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 检查是否是需要验证的函数方法
            if (method.isAnnotationPresent(Function.class) && hasValidationParameters(method)) {
                return invokeWithValidation(method, args);
            } else {
                return method.invoke(target, args);
            }
        }

        /**
         * 检查方法是否有需要验证的参数
         */
        private boolean hasValidationParameters(Method method) {
            return Arrays.stream(method.getParameters())
                    .anyMatch(ValidationEnhancementPostProcessor.this::hasValidationAnnotations);
        }

        /**
         * 带验证的方法调用
         */
        private Object invokeWithValidation(Method method, Object[] args) throws Throwable {
            String methodName = targetClass.getSimpleName() + "." + method.getName();
            
            // 延迟获取 ValidationService
            if (validationService == null) {
                try {
                    validationService = applicationContext.getBean(ValidationService.class);
                } catch (Exception e) {
                    log.warn("无法获取 ValidationService，跳过验证: {}", e.getMessage());
                    return method.invoke(target, args);
                }
            }

            try {
                // 执行参数验证
                validateMethodParameters(method, args);
                
                // 记录验证成功
                recordValidationResult(methodName, true, null);
                
                // 执行原方法
                return method.invoke(target, args);
                
            } catch (Exception e) {
                // 记录验证失败
                recordValidationResult(methodName, false, e.getMessage());
                throw e;
            }
        }

        /**
         * 验证方法参数
         */
        private void validateMethodParameters(Method method, Object[] args) {
            Parameter[] parameters = method.getParameters();
            
            for (int i = 0; i < parameters.length && i < args.length; i++) {
                Parameter parameter = parameters[i];
                Object value = args[i];
                
                if (hasValidationAnnotations(parameter) && value != null) {
                    validateParameter(parameter, value);
                }
            }
        }

        /**
         * 验证单个参数
         */
        private void validateParameter(Parameter parameter, Object value) {
            try {
                // 使用 ValidationService 进行验证
                boolean isValid = validationService.isValid(value);
                if (!isValid) {
                    throw new IllegalArgumentException(
                            String.format("参数验证失败 [%s]: 值不符合验证规则", parameter.getName())
                    );
                }
                
                // 如果需要详细错误信息，可以使用 validateAndGetErrors
                Map<String, String> errors = validationService.validateAndGetErrors(value);
                
                if (!errors.isEmpty()) {
                    String errorMessage = String.join("; ", errors.values());
                    throw new IllegalArgumentException(
                            String.format("参数验证失败 [%s]: %s", parameter.getName(), errorMessage)
                    );
                }
            } catch (Exception e) {
                log.debug("参数验证异常: {}", e.getMessage());
                throw new IllegalArgumentException("参数验证失败: " + e.getMessage(), e);
            }
        }

        /**
         * 记录验证结果
         */
        private void recordValidationResult(String methodName, boolean success, String errorMessage) {
            validationStats.compute(methodName, (key, stats) -> {
                if (stats == null) {
                    stats = new ValidationStats();
                }
                stats.totalCount++;
                if (success) {
                    stats.successCount++;
                } else {
                    stats.failureCount++;
                    stats.lastError = errorMessage;
                }
                return stats;
            });
            
            log.debug("验证结果 - 方法: {}, 成功: {}, 错误: {}", methodName, success, errorMessage);
        }
    }

    /**
     * 验证统计信息
     */
    public static class ValidationStats {
        public long totalCount = 0;
        public long successCount = 0;
        public long failureCount = 0;
        public String lastError;

        public double getSuccessRate() {
            return totalCount > 0 ? (double) successCount / totalCount : 0.0;
        }

        @Override
        public String toString() {
            return String.format("ValidationStats{total=%d, success=%d, failure=%d, successRate=%.2f%%, lastError='%s'}", 
                    totalCount, successCount, failureCount, getSuccessRate() * 100, lastError);
        }
    }

    /**
     * 获取验证统计信息
     */
    public Map<String, ValidationStats> getValidationStats() {
        return new ConcurrentHashMap<>(validationStats);
    }

    /**
     * 清除验证统计信息
     */
    public void clearValidationStats() {
        validationStats.clear();
        log.info("验证统计信息已清除");
    }
}
