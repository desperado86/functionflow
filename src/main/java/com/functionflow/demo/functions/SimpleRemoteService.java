package com.functionflow.demo.functions;

import com.functionflow.demo.annotation.DubboService;
import com.functionflow.demo.annotation.Function;
import com.functionflow.demo.annotation.Functions;

/**
 * 简单远程服务接口
 * 演示基本的 Dubbo 服务功能
 */
@DubboService(
    interfaceName = "com.functionflow.demo.functions.SimpleRemoteService",
    version = "1.0.0",
    group = "default",
    timeout = 3000,
    description = "简单远程服务"
)
@Functions(
    name = "简单远程服务",
    description = "提供基本的远程服务功能",
    category = "远程服务"
)
public interface SimpleRemoteService {

    /**
     * 简单的问候功能
     */
    @Function(
        name = "远程问候",
        description = "远程问候服务",
        category = "基础功能"
    )
    String sayHello(String name);

    /**
     * 获取当前时间
     */
    @Function(
        name = "获取时间",
        description = "获取服务器当前时间",
        category = "基础功能"
    )
    String getCurrentTime();

    /**
     * 简单计算
     */
    @Function(
        name = "简单计算",
        description = "执行简单的数学计算",
        category = "计算功能"
    )
    double calculate(double a, double b, String operation);
}
