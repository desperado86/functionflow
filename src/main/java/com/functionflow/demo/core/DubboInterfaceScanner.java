package com.functionflow.demo.core;

import com.functionflow.demo.annotation.DubboService;
import com.functionflow.demo.annotation.Function;
import com.functionflow.demo.annotation.Functions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dubbo 接口扫描器
 * 扫描带有 @DubboService 注解的接口并创建代理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DubboInterfaceScanner {

    private final ApplicationContext applicationContext;
    private final DubboProxyFactory dubboProxyFactory;
    private final FunctionScanner functionScanner;
    
    // 存储扫描到的 Dubbo 接口
    private final Map<String, Class<?>> dubboInterfaces = new ConcurrentHashMap<>();
    
    // 存储创建的代理对象
    private final Map<String, Object> dubboProxies = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        log.info("开始扫描 Dubbo 接口...");
        scanDubboInterfaces();
        log.info("Dubbo 接口扫描完成，共发现 {} 个接口", dubboInterfaces.size());
    }

    /**
     * 扫描 Dubbo 接口
     */
    private void scanDubboInterfaces() {
        ClassPathScanningCandidateComponentProvider scanner = 
                new ClassPathScanningCandidateComponentProvider(false);
        
        // 添加 @DubboService 注解过滤器
        scanner.addIncludeFilter(new AnnotationTypeFilter(DubboService.class));
        
        // 扫描包路径
        String[] basePackages = {
            "com.functionflow.demo.functions",
            "com.functionflow.demo.service",
            "com.functionflow.demo.api"
        };
        
        for (String basePackage : basePackages) {
            try {
                Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
                
                for (BeanDefinition candidate : candidates) {
                    try {
                        Class<?> clazz = Class.forName(candidate.getBeanClassName());
                        
                        // 确保是接口且有 @DubboService 注解
                        if (clazz.isInterface() && clazz.isAnnotationPresent(DubboService.class)) {
                            processDubboInterface(clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.warn("无法加载类: {}", candidate.getBeanClassName());
                    }
                }
            } catch (Exception e) {
                log.warn("扫描包 {} 时出错: {}", basePackage, e.getMessage());
            }
        }
    }

    /**
     * 处理 Dubbo 接口
     */
    private void processDubboInterface(Class<?> interfaceClass) {
        DubboService dubboService = interfaceClass.getAnnotation(DubboService.class);
        
        if (!dubboService.enabled()) {
            log.debug("跳过已禁用的 Dubbo 接口: {}", interfaceClass.getName());
            return;
        }
        
        String interfaceName = interfaceClass.getSimpleName();
        dubboInterfaces.put(interfaceName, interfaceClass);
        
        log.info("发现 Dubbo 接口: {} -> {}", interfaceName, interfaceClass.getName());
        
        try {
            // 创建代理对象
            Object proxy = dubboProxyFactory.createProxy(interfaceClass);
            dubboProxies.put(interfaceName, proxy);
            
            // 注册为 Spring Bean（如果需要）
            registerAsSpringBean(interfaceClass, proxy);
            
            // 扫描接口中的函数方法
            scanInterfaceFunctions(interfaceClass);
            
            log.info("成功创建 Dubbo 代理: {}", interfaceName);
            
        } catch (Exception e) {
            log.error("创建 Dubbo 代理失败: {}", interfaceName, e);
        }
    }

    /**
     * 注册为 Spring Bean
     */
    private void registerAsSpringBean(Class<?> interfaceClass, Object proxy) {
        // 在实际实现中，可以通过 BeanDefinitionRegistry 注册
        // 这里先跳过，因为需要更复杂的 Spring 集成
        log.debug("跳过 Spring Bean 注册: {}", interfaceClass.getSimpleName());
    }

    /**
     * 扫描接口中的函数方法
     */
    private void scanInterfaceFunctions(Class<?> interfaceClass) {
        // 检查接口是否也有 @Functions 注解
        Functions functionsAnnotation = interfaceClass.getAnnotation(Functions.class);
        
        Method[] methods = interfaceClass.getDeclaredMethods();
        int functionCount = 0;
        
        for (Method method : methods) {
            Function functionAnnotation = method.getAnnotation(Function.class);
            
            if (functionAnnotation != null) {
                functionCount++;
                log.debug("发现 Dubbo 接口函数: {}.{}", interfaceClass.getSimpleName(), method.getName());
                
                // 将接口方法注册到函数扫描器中
                try {
                    registerInterfaceFunction(interfaceClass, method, functionAnnotation);
                } catch (Exception e) {
                    log.warn("注册接口函数失败: {}.{}", interfaceClass.getSimpleName(), method.getName(), e);
                }
            }
        }
        
        if (functionCount > 0) {
            log.info("接口 {} 包含 {} 个函数方法", interfaceClass.getSimpleName(), functionCount);
        }
    }

    /**
     * 注册接口函数到函数扫描器
     */
    private void registerInterfaceFunction(Class<?> interfaceClass, Method method, Function functionAnnotation) {
        // 由于接口没有实现，我们需要特殊处理
        // 这里可以创建一个包装器或者直接使用代理对象
        log.debug("接口函数注册: {}.{} -> {}", 
                interfaceClass.getSimpleName(), method.getName(), functionAnnotation.name());
    }

    // ===== 公共 API 方法 =====

    /**
     * 获取所有 Dubbo 接口
     */
    public Map<String, Class<?>> getAllDubboInterfaces() {
        return new HashMap<>(dubboInterfaces);
    }

    /**
     * 获取 Dubbo 接口代理对象
     */
    public Object getDubboProxy(String interfaceName) {
        return dubboProxies.get(interfaceName);
    }

    /**
     * 获取 Dubbo 接口代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getDubboProxy(Class<T> interfaceClass) {
        String interfaceName = interfaceClass.getSimpleName();
        return (T) dubboProxies.get(interfaceName);
    }

    /**
     * 检查接口是否为 Dubbo 服务
     */
    public boolean isDubboService(Class<?> interfaceClass) {
        return interfaceClass.isInterface() && 
               interfaceClass.isAnnotationPresent(DubboService.class) &&
               dubboInterfaces.containsValue(interfaceClass);
    }

    /**
     * 获取 Dubbo 服务信息
     */
    public Map<String, Object> getDubboServiceInfo() {
        Map<String, Object> serviceInfo = new HashMap<>();
        
        for (Map.Entry<String, Class<?>> entry : dubboInterfaces.entrySet()) {
            String interfaceName = entry.getKey();
            Class<?> interfaceClass = entry.getValue();
            DubboService dubboService = interfaceClass.getAnnotation(DubboService.class);
            
            Map<String, Object> info = new HashMap<>();
            info.put("interfaceClass", interfaceClass.getName());
            info.put("version", dubboService.version());
            info.put("group", dubboService.group());
            info.put("timeout", dubboService.timeout());
            info.put("retries", dubboService.retries());
            info.put("loadbalance", dubboService.loadbalance());
            info.put("async", dubboService.async());
            info.put("protocol", dubboService.protocol());
            info.put("description", dubboService.description());
            info.put("tags", Arrays.asList(dubboService.tags()));
            info.put("proxyCreated", dubboProxies.containsKey(interfaceName));
            
            serviceInfo.put(interfaceName, info);
        }
        
        return serviceInfo;
    }

    /**
     * 重新扫描 Dubbo 接口
     */
    public void rescanDubboInterfaces() {
        log.info("重新扫描 Dubbo 接口...");
        
        dubboInterfaces.clear();
        dubboProxies.clear();
        
        scanDubboInterfaces();
        
        log.info("Dubbo 接口重新扫描完成");
    }

    /**
     * 获取扫描统计信息
     */
    public Map<String, Object> getScanStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalInterfaces", dubboInterfaces.size());
        stats.put("totalProxies", dubboProxies.size());
        stats.put("serviceStats", dubboProxyFactory.getServiceStats());
        stats.put("registeredServices", dubboProxyFactory.getRegisteredServices());
        
        return stats;
    }
}
