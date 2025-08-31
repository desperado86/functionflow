package com.functionflow.demo.functions;

import com.functionflow.demo.annotation.Function;
import com.functionflow.demo.annotation.Functions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 简单远程服务实现类
 * 提供 SimpleRemoteService 的本地实现（用于开发和测试）
 */
@Slf4j
@Component
@Functions(
    name = "简单远程服务实现",
    description = "提供基本的远程服务功能的本地实现",
    category = "远程服务"
)
public class SimpleRemoteServiceImpl implements SimpleRemoteService {

    /**
     * 简单的问候功能
     */
    @Function(
        name = "远程问候",
        description = "远程问候服务",
        category = "基础功能"
    )
    @Override
    public String sayHello(String name) {
        log.info("执行远程问候: {}", name);
        if (name == null || name.trim().isEmpty()) {
            return "Hello, Anonymous!";
        }
        return "Hello, " + name + "! (来自本地服务)";
    }

    /**
     * 获取当前时间
     */
    @Function(
        name = "获取时间",
        description = "获取服务器当前时间",
        category = "基础功能"
    )
    @Override
    public String getCurrentTime() {
        log.info("获取当前时间");
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 简单计算
     */
    @Function(
        name = "简单计算",
        description = "执行简单的数学计算",
        category = "计算功能"
    )
    @Override
    public double calculate(double a, double b, String operation) {
        log.info("执行简单计算: {} {} {}", a, operation, b);
        
        if (operation == null) {
            throw new IllegalArgumentException("操作符不能为空");
        }
        
        switch (operation.toLowerCase()) {
            case "add":
            case "+":
                return a + b;
            case "subtract":
            case "-":
                return a - b;
            case "multiply":
            case "*":
                return a * b;
            case "divide":
            case "/":
                if (b == 0) {
                    throw new IllegalArgumentException("除数不能为零");
                }
                return a / b;
            case "power":
            case "^":
                return Math.pow(a, b);
            case "mod":
            case "%":
                return a % b;
            default:
                throw new IllegalArgumentException("不支持的操作: " + operation);
        }
    }
}
