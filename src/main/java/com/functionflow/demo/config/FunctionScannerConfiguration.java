package com.functionflow.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

/**
 * 函数扫描器配置类
 * 使用 ImportBeanDefinitionRegistrar 来注册自定义的 BeanDefinitionScanner
 */
@Slf4j
@Configuration
public class FunctionScannerConfiguration implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {
        log.info("开始注册 FunctionBeanDefinitionScanner...");
        
        // 创建自定义的 BeanDefinitionScanner
        FunctionBeanDefinitionScanner scanner = new FunctionBeanDefinitionScanner(registry);
        
        // 设置扫描的基础包路径
        String[] basePackages = {
            "com.functionflow.demo.functions",
            "com.functionflow.demo.example"
        };
        
        // 执行扫描
        int scannedBeans = scanner.scan(basePackages);
        
        log.info("FunctionBeanDefinitionScanner 注册完成，扫描到 {} 个函数组件", scannedBeans);
    }
}
