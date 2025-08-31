package com.functionflow.demo.config;

import com.functionflow.demo.annotation.Function;
import com.functionflow.demo.annotation.Functions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;

import java.util.Set;

/**
 * 自定义的 ClassPath Bean Definition Scanner
 * 专门用于扫描带有 @Functions 注解的类
 */
@Slf4j
public class FunctionBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    public FunctionBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        super(registry, false); // 不使用默认过滤器
        
        // 添加自定义过滤器，只扫描带有 @Functions 注解的类
        addIncludeFilter(new AnnotationTypeFilter(Functions.class));
        
        log.info("FunctionBeanDefinitionScanner 初始化完成");
    }

    @Override
    protected boolean isCandidateComponent(@NonNull AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata();
        
        // 检查是否有 @Functions 注解
        boolean hasFunctionsAnnotation = metadata.hasAnnotation(Functions.class.getName());
        
        // 检查是否有方法带有 @Function 注解
        boolean hasFunctionMethods = hasFunctionMethods(metadata);
        
        boolean isCandidate = hasFunctionsAnnotation || hasFunctionMethods;
        
        if (isCandidate) {
            log.debug("发现候选函数组件: {}", metadata.getClassName());
        }
        
        return isCandidate;
    }

    /**
     * 检查类是否有带 @Function 注解的方法
     */
    private boolean hasFunctionMethods(AnnotationMetadata metadata) {
        try {
            Class<?> clazz = Class.forName(metadata.getClassName());
            return java.util.Arrays.stream(clazz.getDeclaredMethods())
                    .anyMatch(method -> method.isAnnotationPresent(Function.class));
        } catch (ClassNotFoundException e) {
            log.warn("无法加载类: {}", metadata.getClassName());
            return false;
        }
    }

    @Override
    @NonNull
    protected Set<BeanDefinitionHolder> doScan(@NonNull String... basePackages) {
        log.info("开始扫描函数组件，包路径: {}", java.util.Arrays.toString(basePackages));
        
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
        
        log.info("扫描完成，发现 {} 个函数组件", beanDefinitions.size());
        
        // 记录扫描到的组件
        for (BeanDefinitionHolder beanDefinitionHolder : beanDefinitions) {
            BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();
            if (beanDefinition instanceof AnnotatedBeanDefinition) {
                AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDefinition;
                String className = annotatedBeanDefinition.getMetadata().getClassName();
                log.info("注册函数组件 Bean: {}", className);
            }
        }
        
        return beanDefinitions;
    }

    @Override
    protected boolean checkCandidate(@NonNull String beanName, @NonNull BeanDefinition beanDefinition) throws IllegalStateException {
        boolean result = super.checkCandidate(beanName, beanDefinition);
        
        if (result && beanDefinition instanceof AnnotatedBeanDefinition) {
            AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDefinition;
            String className = annotatedBeanDefinition.getMetadata().getClassName();
            log.debug("验证候选组件: {} -> {}", beanName, className);
        }
        
        return result;
    }
}
