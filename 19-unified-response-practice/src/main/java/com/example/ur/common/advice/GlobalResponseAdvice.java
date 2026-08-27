package com.example.ur.common.advice;

import com.example.ur.common.result.Result;
import com.example.ur.common.result.ResultFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 全局统一返回结果包装。
 *
 * <p>所有 @ResponseBody 接口的返回值，除标注了 @IgnoreResultWrap 的以外，
 * 都会自动包装成 Result 结构。Controller 里只需返回裸实体、List、Map 等业务数据。</p>
 *
 * <p>处理流程：</p>
 * <ol>
 *     <li>检查方法是否标注 @IgnoreResultWrap，是则跳过</li>
 *     <li>检查 body 是否已经是 Result，避免重复包装；PageResult 会继续包装成 Result&lt;PageResult&lt;T&gt;&gt;</li>
 *     <li>void / null 返回成功的空 Result</li>
 *     <li>String 类型做特殊处理，解决 StringHttpMessageConverter 陷阱</li>
 *     <li>其余统一包装成 Result.success(body)</li>
 * </ol>
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.example.ur")
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 是否支持包装。
     *
     * <p>这里返回 true，表示所有 @ResponseBody 返回值都进入 beforeBodyWrite。
     * 具体跳不跳过在 beforeBodyWrite 里通过注解判断。</p>
     */
    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    /**
     * 响应体写出前调用，返回值将作为真正写出的对象。
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        // 1. 方法上标注了 @IgnoreResultWrap，则跳过包装（文件下载、导出等场景）
        if (returnType.hasMethodAnnotation(IgnoreResultWrap.class)) {
            log.debug("[GlobalResponseAdvice] 接口 {} 标注 @IgnoreResultWrap，跳过包装",
                    returnType.getMethod() != null ? returnType.getMethod().getName() : "unknown");
            return body;
        }

        // 2. body 已经是 Result，避免重复包装（这个判断一定要放在最前面，否则会出现 Result 套 Result）
        if (body instanceof Result) {
            return body;
        }

        // 注：PageResult 在这里不会被跳过，它会被包装成 Result<PageResult<T>>，
        //     这是文章推荐的做法，也是前端分页组件期望的结构。

        // 3. void 方法返回 null，包装成成功的空 Result
        if (body == null) {
            return ResultFactory.success();
        }

        // 4. String 类型处理：
        //    当 Controller 返回 String 时，Spring 默认会优先使用 StringHttpMessageConverter。
        //    如果 GlobalResponseAdvice 返回 Result 对象，StringHttpMessageConverter 无法处理对象，会调用 toString()，
        //    导致前端收到 "Result(code=0, ...)" 这种字符串 JSON。
        //    解决方案：配合 WebConfig 把 MappingJackson2HttpMessageConverter 放到 converters 列表第一位，
        //    这样即使方法签名返回 String，Jackson 也能被选上，从而正确把 Result 对象序列化成 JSON。
        if (body instanceof String) {
            return ResultFactory.success(body);
        }

        // 5. 其余一律包装成 Result.success(body)
        return ResultFactory.success(body);
    }
}
