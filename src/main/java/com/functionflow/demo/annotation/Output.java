package com.functionflow.demo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 函数输出注解
 * 可以用于方法或方法返回值
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Output {

    /**
     * 输出参数名称
     */
    String name() default "";

    /**
     * 输出参数描述
     */
    String description() default "";

    /**
     * 输出参数类型
     */
    Class<?> type() default Object.class;

    /**
     * 是否必需
     */
    boolean required() default true;


}
