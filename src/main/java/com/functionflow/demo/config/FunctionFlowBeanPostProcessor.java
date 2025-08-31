package com.functionflow.demo.config;

import com.functionflow.demo.annotation.Functions;
import com.functionflow.demo.core.FunctionScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * Spring Bean 后处理器，用于增强函数组件
 * 
 * 功能包括：
 * 1. 自动发现和注册带有 @Functions 注解的类
 * 2. 为函数执行添加日志记录
 * 3. 为函数执行添加性能监控
 * 4. 为函数执行添加异常处理增强
 */
@Slf4j
@Component
public class FunctionFlowBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private FunctionScanner functionScanner;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        // 在 Bean 初始化之前的处理
        Class<?> beanClass = bean.getClass();
        
        // 检查是否有 @Functions 注解
        if (beanClass.isAnnotationPresent(Functions.class)) {
            log.info("发现函数组件: {} -> {}", beanName, beanClass.getSimpleName());
            
            // 记录函数组件的详细信息
            Functions functions = beanClass.getAnnotation(Functions.class);
            log.info("函数组件详情: 名称={}, 类别={}, 版本={}, 命名空间={}", 
                    functions.name(), functions.category(), functions.version(), functions.namespace());
        }
        
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        // 在 Bean 初始化之后的处理
        Class<?> beanClass = bean.getClass();
        
        // 如果是函数组件，进行增强处理
        if (beanClass.isAnnotationPresent(Functions.class)) {
            // 延迟获取 FunctionScanner，避免循环依赖
            if (functionScanner == null) {
                try {
                    functionScanner = applicationContext.getBean(FunctionScanner.class);
                } catch (Exception e) {
                    log.warn("无法获取 FunctionScanner: {}", e.getMessage());
                }
            }
            
            // 自动注册函数
            if (functionScanner != null) {
                try {
                    functionScanner.discoverFunctions(beanClass);
                    log.info("自动注册函数组件: {}", beanClass.getSimpleName());
                } catch (Exception e) {
                    log.error("自动注册函数组件失败: {}", beanClass.getSimpleName(), e);
                }
            }
            
            // 创建增强代理
            return createEnhancedProxy(bean, beanClass);
        }
        
        return bean;
    }

    /**
     * 创建增强代理对象
     */
    private Object createEnhancedProxy(Object bean, Class<?> beanClass) {
        try {
            // 获取所有接口
            Class<?>[] interfaces = beanClass.getInterfaces();
            
            // 如果没有接口，直接返回原对象（可以考虑使用 CGLIB 代理）
            if (interfaces.length == 0) {
                log.debug("类 {} 没有实现接口，跳过代理增强", beanClass.getSimpleName());
                return bean;
            }
            
            // 创建 JDK 动态代理
            return Proxy.newProxyInstance(
                    beanClass.getClassLoader(),
                    interfaces,
                    new FunctionExecutionHandler(bean, beanClass)
            );
        } catch (Exception e) {
            log.warn("创建增强代理失败: {}, 返回原对象", beanClass.getSimpleName(), e);
            return bean;
        }
    }

    /**
     * 函数执行处理器
     */
    private static class FunctionExecutionHandler implements java.lang.reflect.InvocationHandler {
        private final Object target;
        private final Class<?> targetClass;

        public FunctionExecutionHandler(Object target, Class<?> targetClass) {
            this.target = target;
            this.targetClass = targetClass;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 检查是否是函数方法
            if (isFunctionMethod(method)) {
                return invokeWithEnhancement(method, args);
            } else {
                // 非函数方法直接调用
                return method.invoke(target, args);
            }
        }

        /**
         * 检查是否是函数方法
         */
        private boolean isFunctionMethod(Method method) {
            // 检查方法是否有 @Function 注解
            return method.isAnnotationPresent(com.functionflow.demo.annotation.Function.class);
        }

        /**
         * 带增强功能的方法调用
         */
        private Object invokeWithEnhancement(Method method, Object[] args) throws Throwable {
            String methodName = targetClass.getSimpleName() + "." + method.getName();
            long startTime = System.currentTimeMillis();
            
            try {
                log.debug("开始执行函数: {} 参数: {}", methodName, Arrays.toString(args));
                
                // 执行原方法
                Object result = method.invoke(target, args);
                
                long executionTime = System.currentTimeMillis() - startTime;
                log.debug("函数执行成功: {} 耗时: {}ms 结果: {}", methodName, executionTime, result);
                
                // 记录性能指标
                recordPerformanceMetrics(methodName, executionTime, true);
                
                return result;
                
            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;
                log.error("函数执行失败: {} 耗时: {}ms 错误: {}", methodName, executionTime, e.getMessage());
                
                // 记录性能指标
                recordPerformanceMetrics(methodName, executionTime, false);
                
                // 增强异常信息
                throw enhanceException(methodName, e, args);
            }
        }

        /**
         * 记录性能指标
         */
        private void recordPerformanceMetrics(String methodName, long executionTime, boolean success) {
            // 这里可以集成监控系统，如 Micrometer、Prometheus 等
            log.info("性能指标 - 方法: {}, 执行时间: {}ms, 成功: {}", methodName, executionTime, success);
            
            // 如果执行时间过长，记录警告
            if (executionTime > 5000) { // 5秒
                log.warn("函数执行时间过长: {} 耗时: {}ms", methodName, executionTime);
            }
        }

        /**
         * 增强异常信息
         */
        private Exception enhanceException(String methodName, Exception originalException, Object[] args) {
            String enhancedMessage = String.format(
                    "函数执行异常 [%s] 参数: %s 原因: %s",
                    methodName,
                    Arrays.toString(args),
                    originalException.getMessage()
            );
            
            // 创建增强的异常
            if (originalException instanceof RuntimeException) {
                return new RuntimeException(enhancedMessage, originalException);
            } else {
                return new Exception(enhancedMessage, originalException);
            }
        }
    }
}
