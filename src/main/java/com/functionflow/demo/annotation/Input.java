package com.functionflow.demo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 函数输入参数注解
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Input {
    
    /**
     * 参数名称
     */
    String name() default "";
    
    /**
     * 参数描述
     */
    String description() default "";
    
    /**
     * 参数类型
     */
    Class<?> type() default Object.class;
    
    /**
     * 是否必需
     */
    boolean required() default true;
    
    /**
     * 默认值
     */
    String defaultValue() default "";
    

}
