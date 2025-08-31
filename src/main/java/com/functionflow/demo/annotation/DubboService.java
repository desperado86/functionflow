package com.functionflow.demo.annotation;

import java.lang.annotation.*;

/**
 * Dubbo 服务注解
 * 用于标记需要通过 Dubbo 调用的远程服务接口
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DubboService {
    
    /**
     * 服务接口名，默认为当前接口的全限定名
     */
    String interfaceName() default "";
    
    /**
     * 服务版本
     */
    String version() default "";
    
    /**
     * 服务分组
     */
    String group() default "";
    
    /**
     * 超时时间（毫秒）
     */
    int timeout() default 3000;
    
    /**
     * 重试次数
     */
    int retries() default 2;
    
    /**
     * 负载均衡策略
     */
    String loadbalance() default "random";
    
    /**
     * 是否异步调用
     */
    boolean async() default false;
    
    /**
     * 注册中心地址，如果为空则使用全局配置
     */
    String registry() default "";
    
    /**
     * 协议名称
     */
    String protocol() default "dubbo";
    
    /**
     * 是否启用
     */
    boolean enabled() default true;
    
    /**
     * 服务描述
     */
    String description() default "";
    
    /**
     * 标签
     */
    String[] tags() default {};
}
