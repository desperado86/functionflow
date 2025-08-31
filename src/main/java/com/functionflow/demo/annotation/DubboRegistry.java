package com.functionflow.demo.annotation;

import java.lang.annotation.*;

/**
 * Dubbo 注册中心配置注解
 * 用于配置 Dubbo 注册中心信息
 */
@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DubboRegistry {
    
    /**
     * 注册中心地址
     * 例如：zookeeper://127.0.0.1:2181
     */
    String address();
    
    /**
     * 注册中心协议
     */
    String protocol() default "zookeeper";
    
    /**
     * 注册中心端口
     */
    int port() default 2181;
    
    /**
     * 用户名
     */
    String username() default "";
    
    /**
     * 密码
     */
    String password() default "";
    
    /**
     * 会话超时时间
     */
    int timeout() default 60000;
    
    /**
     * 注册中心集群容错模式
     */
    String cluster() default "failover";
    
    /**
     * 是否默认注册中心
     */
    boolean isDefault() default true;
    
    /**
     * 注册中心名称
     */
    String name() default "default";
    
    /**
     * 是否启用
     */
    boolean enabled() default true;
}
