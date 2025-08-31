package com.functionflow.demo.core;

import com.functionflow.demo.annotation.Function;
import com.functionflow.demo.annotation.Functions;
import com.functionflow.demo.model.FunctionMetadata;
import com.functionflow.demo.model.FunctionSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 函数元数据管理器
 * 负责在应用启动时生成和管理所有函数的元数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FunctionMetadataManager {

    private final ApplicationContext applicationContext;
    private final FunctionScanner functionScanner;
    
    // 存储函数元数据
    private final Map<String, FunctionMetadata> functionMetadataMap = new ConcurrentHashMap<>();
    
    // 存储类级别的元数据
    private final Map<String, ClassMetadata> classMetadataMap = new ConcurrentHashMap<>();
    
    // 启动时间
    private LocalDateTime startupTime;

    @PostConstruct
    public void initialize() {
        log.info("开始初始化函数元数据管理器...");
        startupTime = LocalDateTime.now();
        
        try {
            // 扫描并生成元数据
            scanAndGenerateMetadata();
            
            log.info("函数元数据管理器初始化完成，共生成 {} 个函数元数据", functionMetadataMap.size());
        } catch (Exception e) {
            log.error("函数元数据管理器初始化失败", e);
        }
    }

    /**
     * 扫描并生成函数元数据
     */
    private void scanAndGenerateMetadata() {
        // 获取所有带有 @Functions 注解的 Bean
        Map<String, Object> functionBeans = applicationContext.getBeansWithAnnotation(Functions.class);
        
        log.info("发现 {} 个函数组件 Bean", functionBeans.size());
        
        for (Map.Entry<String, Object> entry : functionBeans.entrySet()) {
            String beanName = entry.getKey();
            Object bean = entry.getValue();
            Class<?> beanClass = bean.getClass();
            
            // 处理代理类
            if (beanClass.getName().contains("$$") || beanClass.getName().contains("$Proxy")) {
                // Spring CGLIB 代理
                if (beanClass.getSuperclass() != Object.class) {
                    beanClass = beanClass.getSuperclass();
                } else {
                    // JDK 动态代理，跳过处理
                    log.debug("跳过 JDK 动态代理类: {}", beanClass.getName());
                    continue;
                }
            }
            
            log.debug("处理函数组件: {} -> {}", beanName, beanClass.getName());
            
            // 生成类级别元数据
            generateClassMetadata(beanClass, beanName);
            
            // 生成函数级别元数据
            generateFunctionMetadata(beanClass, beanName);
        }
    }

    /**
     * 生成类级别元数据
     */
    private void generateClassMetadata(Class<?> clazz, String beanName) {
        Functions functionsAnnotation = clazz.getAnnotation(Functions.class);
        
        ClassMetadata classMetadata = ClassMetadata.builder()
                .className(clazz.getName())
                .simpleName(clazz.getSimpleName())
                .beanName(beanName)
                .packageName(clazz.getPackage().getName())
                .name(functionsAnnotation != null ? functionsAnnotation.name() : clazz.getSimpleName())
                .description(functionsAnnotation != null ? functionsAnnotation.description() : "")
                .category(functionsAnnotation != null ? functionsAnnotation.category() : "")
                .version(functionsAnnotation != null ? functionsAnnotation.version() : "1.0.0")
                .namespace(functionsAnnotation != null ? functionsAnnotation.namespace() : clazz.getName())
                .functionCount(countFunctionMethods(clazz))
                .scanTime(LocalDateTime.now())
                .build();
        
        classMetadataMap.put(clazz.getName(), classMetadata);
        log.debug("生成类元数据: {}", classMetadata.getClassName());
    }

    /**
     * 生成函数级别元数据
     */
    private void generateFunctionMetadata(Class<?> clazz, String beanName) {
        Method[] methods = clazz.getDeclaredMethods();
        
        log.debug("扫描类 {} 的方法，共 {} 个方法", clazz.getName(), methods.length);
        
        for (Method method : methods) {
            Function functionAnnotation = method.getAnnotation(Function.class);
            
            if (functionAnnotation != null) {
                String functionId = generateFunctionId(clazz, method);
                
                // 获取函数 Schema
                FunctionSchema functionSchema = functionScanner.getFunction(functionId);
                
                if (functionSchema == null) {
                    log.warn("无法获取函数 Schema: {}", functionId);
                    continue;
                }
                
                FunctionMetadata metadata = FunctionMetadata.builder()
                        .functionId(functionId)
                        .methodName(method.getName())
                        .className(clazz.getName())
                        .beanName(beanName)
                        .name(functionAnnotation.name().isEmpty() ? method.getName() : functionAnnotation.name())
                        .description(functionAnnotation.description())
                        .category(functionAnnotation.category())
                        .version(functionAnnotation.version())
                        .namespace(functionAnnotation.namespace())
                        .async(functionAnnotation.async())
                        .cacheable(functionAnnotation.cacheable())
                        .cacheTime(functionAnnotation.cacheTime())
                        .timeout(functionAnnotation.timeout())
                        .parameterCount(method.getParameterCount())
                        .returnType(method.getReturnType().getName())
                        .parameterTypes(Arrays.stream(method.getParameterTypes())
                                .map(Class::getName)
                                .collect(Collectors.toList()))
                        .functionSchema(functionSchema)
                        .scanTime(LocalDateTime.now())
                        .build();
                
                functionMetadataMap.put(functionId, metadata);
                log.debug("生成函数元数据: {}", functionId);
            }
        }
    }

    /**
     * 统计类中的函数方法数量
     */
    private int countFunctionMethods(Class<?> clazz) {
        return (int) Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Function.class))
                .count();
    }

    /**
     * 生成函数ID
     */
    private String generateFunctionId(Class<?> clazz, Method method) {
        return clazz.getName() + "." + method.getName();
    }

    // ===== 公共 API 方法 =====

    /**
     * 获取所有函数元数据
     */
    public Collection<FunctionMetadata> getAllFunctionMetadata() {
        return new ArrayList<>(functionMetadataMap.values());
    }

    /**
     * 根据函数ID获取元数据
     */
    public FunctionMetadata getFunctionMetadata(String functionId) {
        return functionMetadataMap.get(functionId);
    }

    /**
     * 获取所有类元数据
     */
    public Collection<ClassMetadata> getAllClassMetadata() {
        return new ArrayList<>(classMetadataMap.values());
    }

    /**
     * 根据类名获取类元数据
     */
    public ClassMetadata getClassMetadata(String className) {
        return classMetadataMap.get(className);
    }

    /**
     * 根据分类获取函数元数据
     */
    public List<FunctionMetadata> getFunctionsByCategory(String category) {
        return functionMetadataMap.values().stream()
                .filter(metadata -> category.equals(metadata.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * 根据命名空间获取函数元数据
     */
    public List<FunctionMetadata> getFunctionsByNamespace(String namespace) {
        return functionMetadataMap.values().stream()
                .filter(metadata -> namespace.equals(metadata.getNamespace()))
                .collect(Collectors.toList());
    }

    /**
     * 搜索函数元数据
     */
    public List<FunctionMetadata> searchFunctions(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return functionMetadataMap.values().stream()
                .filter(metadata -> 
                    metadata.getName().toLowerCase().contains(lowerKeyword) ||
                    metadata.getDescription().toLowerCase().contains(lowerKeyword) ||
                    metadata.getCategory().toLowerCase().contains(lowerKeyword)
                )
                .collect(Collectors.toList());
    }

    /**
     * 获取元数据统计信息
     */
    public MetadataStats getMetadataStats() {
        Map<String, Long> categoryStats = functionMetadataMap.values().stream()
                .collect(Collectors.groupingBy(FunctionMetadata::getCategory, Collectors.counting()));
        
        Map<String, Long> namespaceStats = functionMetadataMap.values().stream()
                .collect(Collectors.groupingBy(FunctionMetadata::getNamespace, Collectors.counting()));
        
        long cacheableFunctions = functionMetadataMap.values().stream()
                .mapToLong(metadata -> metadata.isCacheable() ? 1 : 0)
                .sum();
        
        long asyncFunctions = functionMetadataMap.values().stream()
                .mapToLong(metadata -> metadata.isAsync() ? 1 : 0)
                .sum();
        
        return MetadataStats.builder()
                .totalFunctions(functionMetadataMap.size())
                .totalClasses(classMetadataMap.size())
                .categoryStats(categoryStats)
                .namespaceStats(namespaceStats)
                .cacheableFunctions(cacheableFunctions)
                .asyncFunctions(asyncFunctions)
                .startupTime(startupTime)
                .lastScanTime(LocalDateTime.now())
                .build();
    }

    /**
     * 刷新元数据（重新扫描）
     */
    public void refreshMetadata() {
        log.info("开始刷新函数元数据...");
        
        functionMetadataMap.clear();
        classMetadataMap.clear();
        
        scanAndGenerateMetadata();
        
        log.info("函数元数据刷新完成");
    }

    // ===== 内部类 =====

    /**
     * 类元数据
     */
    @lombok.Data
    @lombok.Builder
    public static class ClassMetadata {
        private String className;
        private String simpleName;
        private String beanName;
        private String packageName;
        private String name;
        private String description;
        private String category;
        private String version;
        private String namespace;
        private int functionCount;
        private LocalDateTime scanTime;
    }

    /**
     * 元数据统计信息
     */
    @lombok.Data
    @lombok.Builder
    public static class MetadataStats {
        private int totalFunctions;
        private int totalClasses;
        private Map<String, Long> categoryStats;
        private Map<String, Long> namespaceStats;
        private long cacheableFunctions;
        private long asyncFunctions;
        private LocalDateTime startupTime;
        private LocalDateTime lastScanTime;
    }
}
