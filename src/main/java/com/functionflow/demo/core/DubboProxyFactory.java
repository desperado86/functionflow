package com.functionflow.demo.core;

import com.functionflow.demo.annotation.DubboService;
import com.functionflow.demo.config.DubboConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

/**
 * Dubbo 动态代理工厂
 * 为标记了 @DubboService 的接口创建动态代理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DubboProxyFactory {

    private final DubboConfiguration dubboConfiguration;
    
    // 缓存已创建的代理对象
    private final Map<Class<?>, Object> proxyCache = new ConcurrentHashMap<>();
    
    // 缓存 Dubbo 服务引用
    private final Map<String, Object> dubboReferenceCache = new ConcurrentHashMap<>();
    
    // 服务调用统计
    private final Map<String, ServiceStats> serviceStats = new ConcurrentHashMap<>();
    
    // Dubbo 应用配置
    private ApplicationConfig applicationConfig;
    
    // Dubbo 注册中心配置
    private RegistryConfig registryConfig;
    
    // Dubbo 引用配置缓存 (使用简单的 Map 代替)
    private final Map<String, ReferenceConfig<?>> referenceConfigCache = new ConcurrentHashMap<>();

    /**
     * 初始化 Dubbo 配置
     */
    @PostConstruct
    public void initDubboConfig() {
        if (!dubboConfiguration.isEnabled()) {
            log.info("Dubbo 未启用，跳过初始化");
            return;
        }
        
        try {
            // 初始化应用配置
            applicationConfig = new ApplicationConfig();
            applicationConfig.setName(dubboConfiguration.getApplicationName());
            log.info("Dubbo 应用配置初始化完成: {}", dubboConfiguration.getApplicationName());
            
            // 初始化注册中心配置
            registryConfig = new RegistryConfig();
            registryConfig.setAddress(dubboConfiguration.getRegistry().getAddress());
            registryConfig.setTimeout(dubboConfiguration.getRegistry().getTimeout());
            registryConfig.setUsername(dubboConfiguration.getRegistry().getUsername());
            registryConfig.setPassword(dubboConfiguration.getRegistry().getPassword());
            log.info("Dubbo 注册中心配置初始化完成: {}", dubboConfiguration.getRegistry().getAddress());
            
            log.info("Dubbo 引用配置缓存初始化完成");
            
        } catch (Exception e) {
            log.error("Dubbo 配置初始化失败，将使用模拟模式", e);
        }
    }

    /**
     * 为接口创建 Dubbo 代理
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> interfaceClass) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("只能为接口创建 Dubbo 代理: " + interfaceClass.getName());
        }
        
        DubboService dubboService = interfaceClass.getAnnotation(DubboService.class);
        if (dubboService == null) {
            throw new IllegalArgumentException("接口必须标记 @DubboService 注解: " + interfaceClass.getName());
        }
        
        // 从缓存中获取代理对象
        return (T) proxyCache.computeIfAbsent(interfaceClass, this::doCreateProxy);
    }

    /**
     * 实际创建代理对象
     */
    private Object doCreateProxy(Class<?> interfaceClass) {
        DubboService dubboService = interfaceClass.getAnnotation(DubboService.class);
        
        log.info("创建 Dubbo 代理: {} -> {}", 
                interfaceClass.getSimpleName(), 
                dubboService.interfaceName().isEmpty() ? interfaceClass.getName() : dubboService.interfaceName());
        
        return Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class[]{interfaceClass},
                new DubboInvocationHandler(interfaceClass, dubboService)
        );
    }

    /**
     * Dubbo 调用处理器
     */
    private class DubboInvocationHandler implements InvocationHandler {
        private final Class<?> interfaceClass;
        private final DubboService dubboService;

        public DubboInvocationHandler(Class<?> interfaceClass, DubboService dubboService) {
            this.interfaceClass = interfaceClass;
            this.dubboService = dubboService;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 处理 Object 类的方法
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            
            String serviceKey = generateServiceKey(interfaceClass, method);
            long startTime = System.currentTimeMillis();
            
            try {
                // 执行真实 Dubbo 远程调用
                Object result = realDubboCall(method, args);
                
                // 记录成功调用
                recordServiceCall(serviceKey, true, System.currentTimeMillis() - startTime);
                
                return result;
                
            } catch (Exception e) {
                // 记录失败调用
                recordServiceCall(serviceKey, false, System.currentTimeMillis() - startTime);
                
                log.error("Dubbo 服务调用失败: {}.{}", interfaceClass.getSimpleName(), method.getName(), e);
                throw new RuntimeException("Dubbo 服务调用失败: " + e.getMessage(), e);
            }
        }

        /**
         * 真正的 Dubbo 远程调用
         */
        private Object realDubboCall(Method method, Object[] args) throws Exception {
            String interfaceName = dubboService.interfaceName().isEmpty() 
                    ? interfaceClass.getName() 
                    : dubboService.interfaceName();
            
            log.debug("执行真实 Dubbo 调用: {} -> {}.{}", 
                    interfaceName, method.getDeclaringClass().getSimpleName(), method.getName());
            
            // 如果 Dubbo 未正确初始化，回退到模拟模式
            if (applicationConfig == null || registryConfig == null) {
                log.warn("Dubbo 未正确初始化，使用模拟模式调用: {}.{}", 
                        interfaceClass.getSimpleName(), method.getName());
                return simulateDubboCall(method, args);
            }
            
            try {
                // 获取 Dubbo 服务引用
                Object dubboReference = getDubboReference(interfaceClass, dubboService);
                
                // 执行远程调用
                return method.invoke(dubboReference, args);
                
            } catch (Exception e) {
                log.error("Dubbo 真实调用失败，回退到模拟模式: {}.{}", 
                        interfaceClass.getSimpleName(), method.getName(), e);
                return simulateDubboCall(method, args);
            }
        }

        /**
         * 模拟 Dubbo 远程调用（备用模式）
         */
        private Object simulateDubboCall(Method method, Object[] args) throws Exception {
            String interfaceName = dubboService.interfaceName().isEmpty() 
                    ? interfaceClass.getName() 
                    : dubboService.interfaceName();
            
            log.debug("模拟 Dubbo 调用: {} -> {}.{}", 
                    interfaceName, method.getDeclaringClass().getSimpleName(), method.getName());
            
            // 模拟网络延迟
            Thread.sleep(10 + (int)(Math.random() * 50));
            
            // 如果是异步调用，返回 CompletableFuture
            if (dubboService.async() && method.getReturnType() == CompletableFuture.class) {
                return CompletableFuture.completedFuture(generateMockResult(method));
            }
            
            return generateMockResult(method);
        }

        /**
         * 生成模拟结果
         */
        private Object generateMockResult(Method method) {
            Class<?> returnType = method.getReturnType();
            
            if (returnType == void.class || returnType == Void.class) {
                return null;
            } else if (returnType == String.class) {
                return "Dubbo Mock Result for " + method.getName();
            } else if (returnType == Integer.class || returnType == int.class) {
                return (int)(Math.random() * 100);
            } else if (returnType == Long.class || returnType == long.class) {
                return (long)(Math.random() * 1000);
            } else if (returnType == Double.class || returnType == double.class) {
                return Math.random() * 100;
            } else if (returnType == Boolean.class || returnType == boolean.class) {
                return Math.random() > 0.5;
            } else {
                // 对于复杂对象，返回 null 或者可以使用反射创建实例
                return null;
            }
        }
    }

    /**
     * 获取 Dubbo 服务引用
     */
    @SuppressWarnings("unchecked")
    private <T> T getDubboReference(Class<T> interfaceClass, DubboService dubboService) {
        String cacheKey = generateReferenceKey(interfaceClass, dubboService);
        
        return (T) dubboReferenceCache.computeIfAbsent(cacheKey, key -> {
            try {
                // 创建引用配置
                ReferenceConfig<T> referenceConfig = new ReferenceConfig<>();
                
                // 设置接口
                referenceConfig.setInterface(interfaceClass);
                
                // 设置应用配置 (使用新的方法)
                if (applicationConfig != null) {
                    referenceConfig.setApplication(applicationConfig);
                }
                
                // 设置注册中心配置
                referenceConfig.setRegistry(registryConfig);
                
                // 设置服务相关配置
                String interfaceName = dubboService.interfaceName().isEmpty() 
                        ? interfaceClass.getName() 
                        : dubboService.interfaceName();
                        
                if (!dubboService.version().isEmpty()) {
                    referenceConfig.setVersion(dubboService.version());
                }
                
                if (!dubboService.group().isEmpty()) {
                    referenceConfig.setGroup(dubboService.group());
                }
                
                referenceConfig.setTimeout(dubboService.timeout());
                referenceConfig.setRetries(dubboService.retries());
                referenceConfig.setLoadbalance(dubboService.loadbalance());
                referenceConfig.setAsync(dubboService.async());
                
                if (!dubboService.protocol().isEmpty()) {
                    referenceConfig.setProtocol(dubboService.protocol());
                }
                
                // 设置检查提供者存在
                referenceConfig.setCheck(false); // 启动时不检查提供者存在
                
                log.info("创建 Dubbo 服务引用: {} (version: {}, group: {})", 
                        interfaceName, dubboService.version(), dubboService.group());
                
                // 获取服务引用
                T reference = referenceConfig.get();
                
                // 缓存 ReferenceConfig
                referenceConfigCache.put(cacheKey, referenceConfig);
                
                return reference;
                
            } catch (Exception e) {
                log.error("创建 Dubbo 服务引用失败: {}", interfaceClass.getName(), e);
                throw new RuntimeException("创建 Dubbo 服务引用失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 生成引用缓存键
     */
    private String generateReferenceKey(Class<?> interfaceClass, DubboService dubboService) {
        return String.format("%s:%s:%s", 
                dubboService.interfaceName().isEmpty() ? interfaceClass.getName() : dubboService.interfaceName(),
                dubboService.version(),
                dubboService.group());
    }

    /**
     * 生成服务调用键
     */
    private String generateServiceKey(Class<?> interfaceClass, Method method) {
        return interfaceClass.getSimpleName() + "." + method.getName();
    }

    /**
     * 记录服务调用统计
     */
    private void recordServiceCall(String serviceKey, boolean success, long executionTime) {
        serviceStats.compute(serviceKey, (key, stats) -> {
            if (stats == null) {
                stats = new ServiceStats();
            }
            stats.totalCalls++;
            if (success) {
                stats.successCalls++;
            } else {
                stats.failureCalls++;
            }
            stats.totalExecutionTime += executionTime;
            stats.avgExecutionTime = (double) stats.totalExecutionTime / stats.totalCalls;
            return stats;
        });
    }

    /**
     * 获取服务调用统计
     */
    public Map<String, ServiceStats> getServiceStats() {
        return new ConcurrentHashMap<>(serviceStats);
    }

    /**
     * 清除服务调用统计
     */
    public void clearServiceStats() {
        serviceStats.clear();
        log.info("Dubbo 服务调用统计已清除");
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCaches() {
        proxyCache.clear();
        dubboReferenceCache.clear();
        serviceStats.clear();
        
        // 销毁所有引用配置
        try {
            for (ReferenceConfig<?> config : referenceConfigCache.values()) {
                try {
                    config.destroy();
                } catch (Exception e) {
                    log.warn("销毁 Dubbo 引用配置时出错", e);
                }
            }
            referenceConfigCache.clear();
            log.info("Dubbo 引用配置缓存已清除");
        } catch (Exception e) {
            log.warn("清除 Dubbo 引用配置缓存时出错", e);
        }
        
        log.info("所有 Dubbo 缓存已清除");
    }

    /**
     * 获取 Dubbo 连接状态
     */
    public Map<String, Object> getDubboStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        
        status.put("enabled", dubboConfiguration.isEnabled());
        status.put("initialized", applicationConfig != null && registryConfig != null);
        status.put("applicationName", dubboConfiguration.getApplicationName());
        status.put("registryAddress", dubboConfiguration.getRegistry().getAddress());
        status.put("cachedReferences", dubboReferenceCache.size());
        status.put("cachedProxies", proxyCache.size());
        
        return status;
    }

    /**
     * 获取已注册的代理服务列表
     */
    public Map<String, Object> getRegisteredServices() {
        Map<String, Object> services = new ConcurrentHashMap<>();
        for (Map.Entry<Class<?>, Object> entry : proxyCache.entrySet()) {
            Class<?> interfaceClass = entry.getKey();
            DubboService dubboService = interfaceClass.getAnnotation(DubboService.class);
            
            Map<String, Object> serviceInfo = new ConcurrentHashMap<>();
            serviceInfo.put("interfaceClass", interfaceClass.getName());
            serviceInfo.put("interfaceName", dubboService.interfaceName().isEmpty() 
                    ? interfaceClass.getName() : dubboService.interfaceName());
            serviceInfo.put("version", dubboService.version());
            serviceInfo.put("group", dubboService.group());
            serviceInfo.put("timeout", dubboService.timeout());
            serviceInfo.put("async", dubboService.async());
            serviceInfo.put("description", dubboService.description());
            
            services.put(interfaceClass.getSimpleName(), serviceInfo);
        }
        return services;
    }

    /**
     * 服务调用统计信息
     */
    public static class ServiceStats {
        public long totalCalls = 0;
        public long successCalls = 0;
        public long failureCalls = 0;
        public long totalExecutionTime = 0;
        public double avgExecutionTime = 0.0;

        public double getSuccessRate() {
            return totalCalls > 0 ? (double) successCalls / totalCalls : 0.0;
        }

        @Override
        public String toString() {
            return String.format("ServiceStats{total=%d, success=%d, failure=%d, successRate=%.2f%%, avgTime=%.2fms}", 
                    totalCalls, successCalls, failureCalls, getSuccessRate() * 100, avgExecutionTime);
        }
    }
}
