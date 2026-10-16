package com.lemon.erp.framework.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 记录操作日志，加在 Controller 方法上
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /** 模块标题，如「用户管理」 */
    String title();

    OperType type() default OperType.OTHER;

    /** 是否记录请求参数 */
    boolean saveParams() default true;

    /** 是否记录返回结果 */
    boolean saveResult() default true;
}
