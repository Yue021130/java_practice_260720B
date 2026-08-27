package com.example.ur.common.advice;

import java.lang.annotation.*;

/**
 * 跳过统一结果包装。
 *
 * <p>标注在 Controller 方法上，表示该接口返回值不进行统一包装。
 * 适用场景：文件下载、Excel 导出、第三方回调（对方要求特定格式）等。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreResultWrap {
}
