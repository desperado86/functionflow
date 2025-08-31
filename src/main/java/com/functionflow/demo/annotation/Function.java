package com.functionflow.demo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 函数注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Function {

  /**
   * 函数命名空间
   */
  String namespace() default "";

  /**
   * 函数名称
   */
  String name() default "";

  /**
   * 函数描述
   */
  String description() default "";

  /**
   * 函数分类
   */
  String category() default "";

  /**
   * 函数版本
   */
  String version() default "1.0.0";

  /**
   * 是否异步执行
   */
  boolean async() default false;

  /**
   * 超时时间（毫秒）
   */
  long timeout() default 30000;

  /**
   * 是否可缓存
   */
  boolean cacheable() default false;

  /**
   * 缓存时间（秒）
   */
  int cacheTime() default 300;
}
