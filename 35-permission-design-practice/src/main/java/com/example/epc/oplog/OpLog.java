package com.example.epc.oplog;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在 Controller 方法上，由 {@link OpLogAspect} 自动
 * 记录操作审计（操作人/权限点/IP/参数摘要/耗时）到 sys_op_log。
 *
 * <p>企业后台的审计要求「谁在什么时候做了什么」必须可回溯，
 * 靠人肉在方法里写日志必然漏——注解 + AOP 是机制兜底
 * （对应参考文档番外篇「从人肉检查到机制兜底」的同一思路）。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {

    /** 操作标题，如「新增用户」 */
    String value();

    /** 关联权限点标识，如 "user:create" */
    String perm() default "";
}
