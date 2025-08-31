package com.functionflow.demo.controller;

import com.functionflow.demo.config.DubboConfiguration;
import com.functionflow.demo.core.DubboInterfaceScanner;
import com.functionflow.demo.core.DubboProxyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Dubbo 管理控制器
 * 提供 Dubbo 服务的管理和监控接口
 */
@RestController
@RequestMapping("/api/dubbo")
@RequiredArgsConstructor
public class DubboController {

    private final DubboConfiguration dubboConfiguration;
    private final DubboInterfaceScanner dubboInterfaceScanner;
    private final DubboProxyFactory dubboProxyFactory;

    /**
     * 获取 Dubbo 配置信息
     */
    @GetMapping("/config")
    public ResponseEntity<DubboConfiguration> getDubboConfig() {
        return ResponseEntity.ok(dubboConfiguration);
    }

    /**
     * 获取所有 Dubbo 服务接口
     */
    @GetMapping("/services")
    public ResponseEntity<Map<String, Object>> getDubboServices() {
        return ResponseEntity.ok(dubboInterfaceScanner.getDubboServiceInfo());
    }

    /**
     * 获取 Dubbo 扫描统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDubboStats() {
        return ResponseEntity.ok(dubboInterfaceScanner.getScanStats());
    }

    /**
     * 获取 Dubbo 服务调用统计
     */
    @GetMapping("/service-stats")
    public ResponseEntity<Map<String, DubboProxyFactory.ServiceStats>> getServiceStats() {
        return ResponseEntity.ok(dubboProxyFactory.getServiceStats());
    }

    /**
     * 重新扫描 Dubbo 接口
     */
    @PostMapping("/rescan")
    public ResponseEntity<Map<String, Object>> rescanDubboInterfaces() {
        dubboInterfaceScanner.rescanDubboInterfaces();
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Dubbo 接口重新扫描完成");
        result.put("stats", dubboInterfaceScanner.getScanStats());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 清除服务调用统计
     */
    @DeleteMapping("/service-stats")
    public ResponseEntity<Map<String, String>> clearServiceStats() {
        dubboProxyFactory.clearServiceStats();
        
        Map<String, String> result = new HashMap<>();
        result.put("message", "Dubbo 服务调用统计已清除");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 清除所有缓存
     */
    @DeleteMapping("/caches")
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        dubboProxyFactory.clearAllCaches();
        
        Map<String, String> result = new HashMap<>();
        result.put("message", "所有 Dubbo 缓存已清除");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取 Dubbo 连接状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDubboStatus() {
        return ResponseEntity.ok(dubboProxyFactory.getDubboStatus());
    }

    /**
     * 测试 Dubbo 服务调用
     */
    @PostMapping("/test/{serviceName}/{methodName}")
    public ResponseEntity<Map<String, Object>> testDubboService(
            @PathVariable String serviceName,
            @PathVariable String methodName,
            @RequestBody(required = false) Map<String, Object> parameters) {
        
        try {
            Object proxy = dubboInterfaceScanner.getDubboProxy(serviceName);
            if (proxy == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "服务不存在: " + serviceName);
                return ResponseEntity.notFound().build();
            }
            
            // 在实际实现中，这里会通过反射调用方法
            // 现在返回模拟结果
            Map<String, Object> result = new HashMap<>();
            result.put("service", serviceName);
            result.put("method", methodName);
            result.put("parameters", parameters);
            result.put("result", "模拟调用结果");
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "调用失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 获取 Dubbo 服务详情
     */
    @GetMapping("/services/{serviceName}")
    public ResponseEntity<Map<String, Object>> getDubboServiceDetail(@PathVariable String serviceName) {
        Class<?> interfaceClass = dubboInterfaceScanner.getAllDubboInterfaces().get(serviceName);
        
        if (interfaceClass == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> serviceDetail = new HashMap<>();
        serviceDetail.put("serviceName", serviceName);
        serviceDetail.put("interfaceClass", interfaceClass.getName());
        serviceDetail.put("proxyCreated", dubboInterfaceScanner.getDubboProxy(serviceName) != null);
        
        // 获取方法信息
        java.lang.reflect.Method[] methods = interfaceClass.getDeclaredMethods();
        java.util.List<Map<String, Object>> methodInfos = new java.util.ArrayList<>();
        
        for (java.lang.reflect.Method method : methods) {
            Map<String, Object> methodInfo = new HashMap<>();
            methodInfo.put("name", method.getName());
            methodInfo.put("returnType", method.getReturnType().getSimpleName());
            methodInfo.put("parameterCount", method.getParameterCount());
            
            java.util.List<String> parameterTypes = new java.util.ArrayList<>();
            for (Class<?> paramType : method.getParameterTypes()) {
                parameterTypes.add(paramType.getSimpleName());
            }
            methodInfo.put("parameterTypes", parameterTypes);
            
            // 检查是否有 @Function 注解
            com.functionflow.demo.annotation.Function functionAnnotation = 
                    method.getAnnotation(com.functionflow.demo.annotation.Function.class);
            if (functionAnnotation != null) {
                methodInfo.put("functionName", functionAnnotation.name());
                methodInfo.put("functionDescription", functionAnnotation.description());
                methodInfo.put("functionCategory", functionAnnotation.category());
            }
            
            methodInfos.add(methodInfo);
        }
        
        serviceDetail.put("methods", methodInfos);
        
        return ResponseEntity.ok(serviceDetail);
    }

    /**
     * 获取 Dubbo 状态概览
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getDubboOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        // 基本信息
        overview.put("enabled", dubboConfiguration.isEnabled());
        overview.put("applicationName", dubboConfiguration.getApplicationName());
        
        // 注册中心信息
        Map<String, Object> registryInfo = new HashMap<>();
        DubboConfiguration.Registry registry = dubboConfiguration.getRegistry();
        registryInfo.put("address", registry.getAddress());
        registryInfo.put("protocol", registry.getProtocol());
        registryInfo.put("timeout", registry.getTimeout());
        registryInfo.put("enabled", registry.isEnabled());
        overview.put("registry", registryInfo);
        
        // 协议信息
        Map<String, Object> protocolInfo = new HashMap<>();
        DubboConfiguration.Protocol protocol = dubboConfiguration.getProtocol();
        protocolInfo.put("name", protocol.getName());
        protocolInfo.put("port", protocol.getPort());
        protocolInfo.put("threads", protocol.getThreads());
        protocolInfo.put("serialization", protocol.getSerialization());
        overview.put("protocol", protocolInfo);
        
        // 统计信息
        Map<String, Object> stats = dubboInterfaceScanner.getScanStats();
        overview.put("stats", stats);
        
        return ResponseEntity.ok(overview);
    }
}
