package com.functionflow.demo.functions;

/**
 * 函数服务接口
 * 
 * 为函数类提供统一接口，便于 BeanPostProcessor 创建代理
 * 这是一个标记接口，用于识别函数服务类
 */
public interface FunctionService {
    
    /**
     * 获取服务名称
     */
    default String getServiceName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取服务描述
     */
    default String getServiceDescription() {
        return "函数服务";
    }
}
