package com.functionflow.demo.config;

import com.functionflow.demo.core.FunctionScanner;

import com.functionflow.demo.functions.StandardLibraryFunctions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 函数发现初始化器
 */
@Slf4j
@Component
public class FunctionDiscoveryInitializer implements CommandLineRunner {
    
    @Autowired
    private FunctionScanner discoveryService;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("开始发现和注册函数...");
        
        // 使用包扫描功能（只扫描有注解的函数）
        discoveryService.scanPackages("com.functionflow.demo.functions");
        
        // 使用全扫描功能（包括无注解的函数）
        discoverAllFunctions();
        
        // 发现标准库函数（示例：Math类）
        discoverStandardLibraryFunctions();
        
        log.info("函数发现和注册完成，共发现 {} 个函数", discoveryService.getAllFunctions().size());
    }
    
    /**
     * 发现所有函数（包括无注解的函数）
     */
    private void discoverAllFunctions() {
        try {
            // 扫描 StandardLibraryFunctions 类的所有方法（包括无注解的）
            discoveryService.discoverAllFunctions(StandardLibraryFunctions.class);
            log.info("全扫描完成，包括无注解函数");
        } catch (Exception e) {
            log.warn("全扫描失败", e);
        }
    }
    
    /**
     * 发现标准库函数
     */
    private void discoverStandardLibraryFunctions() {
        try {
            // 这里可以添加对标准库函数的发现
            // 例如：Math类的静态方法
            log.info("标准库函数发现功能待扩展...");
        } catch (Exception e) {
            log.warn("标准库函数发现失败", e);
        }
    }
}
