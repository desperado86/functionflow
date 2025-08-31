package com.functionflow.demo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 函数集合注解
 * 用于标记包含函数方法的类
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Functions {
    
    /**
     * 函数集合命名空间
     */
    String namespace() default "";
    
    /**
     * 函数集合名称
     */
    String name() default "";
    
    /**
     * 函数集合描述
     */
    String description() default "";
    
    /**
     * 函数集合分类
     */
    String category() default "";
    
    /**
     * 函数集合版本
     */
    String version() default "1.0.0";
    
    
    
    /**
     * 是否启用
     */
    boolean enabled() default true;
}
