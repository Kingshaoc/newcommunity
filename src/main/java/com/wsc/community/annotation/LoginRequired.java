package com.wsc.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解 在方法上标注 表名访问这个方法需要登录
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)//运行时候有效
public @interface LoginRequired {
}
