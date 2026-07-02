package com.edus.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {

    /**
     * 操作类型
     * 如：LOGIN/CREATE/UPDATE/DELETE/EXPORT/IMPORT等
     */
    String operationType() default "";

    /**
     * 操作模块
     * 如：用户管理/系统管理/权限管理等
     */
    String module() default "";

    /**
     * 操作描述
     * 描述具体做了什么操作
     */
    String description() default "";

    /**
     * 是否记录请求参数
     */
    boolean logParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean logResponse() default false;
}