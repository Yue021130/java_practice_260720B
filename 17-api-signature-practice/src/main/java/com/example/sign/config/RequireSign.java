package com.example.sign.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要 HMAC-SHA256 签名鉴权的接口。
 *
 * 用在 Controller 类或方法上，由 {@link SignAuthInterceptor} 拦截校验：
 * 请求必须携带 X-App-Id / X-Timestamp / X-Nonce / X-Signature，且签名通过才放行；
 * 缺失或校验失败返回 401。与 Spring 生态的 @RequirePermissions 等注解思路一致。
 *
 * 教学设计：只标注被保护的接口，其余演示接口不受影响。
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireSign {
}
