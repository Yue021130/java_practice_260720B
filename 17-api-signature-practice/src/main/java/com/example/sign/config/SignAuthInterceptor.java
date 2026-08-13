package com.example.sign.config;

import com.example.sign.signature.SignVerifier;
import com.example.sign.support.SignLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 接口签名鉴权拦截器。
 *
 * 只拦截标注了 {@link RequireSign} 的接口，实际校验逻辑委托给
 * {@link SignVerifier}（时间戳 → nonce → appkey → 重算比对），
 * 通过则放行，失败返回 401。这样演示接口与真实拦截链路共用同一套校验代码。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignAuthInterceptor implements HandlerInterceptor {

    /** 参与签名的请求头名 */
    public static final String HEADER_APP_ID = "X-App-Id";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_SIGNATURE = "X-Signature";

    private final SignVerifier verifier;
    private final SignLogStore logStore;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 只校验标注了 @RequireSign 的 handler
        if (!isRequireSign(handler)) {
            return true;
        }

        String appId = request.getHeader(HEADER_APP_ID);
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);
        String clientSignature = request.getHeader(HEADER_SIGNATURE);

        // 参与签名的请求头（拦截器场景：appid/timestamp/nonce 三个头）
        Map<String, String> signedHeaders = new LinkedHashMap<>();
        if (appId != null) {
            signedHeaders.put(HEADER_APP_ID, appId);
        }
        if (timestamp != null) {
            signedHeaders.put(HEADER_TIMESTAMP, timestamp);
        }
        if (nonce != null) {
            signedHeaders.put(HEADER_NONCE, nonce);
        }
        Map<String, String> query = extractQuery(request);

        SignVerifier.VerifyResult result = verifier.verify(appId, timestamp, nonce,
                request.getMethod(), request.getRequestURI(), query, signedHeaders,
                "", clientSignature);

        if (!result.passed()) {
            log.warn("接口签名鉴权拒绝：{}（uri={}）", result.reason(), request.getRequestURI());
            logStore.add("interceptor", appId, false, result.reason());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"" + result.reason() + "\",\"data\":null}");
            return false;
        }

        logStore.add("interceptor", appId, true, "拦截器验签通过");
        log.info("接口签名鉴权通过：appId={}, uri={}", appId, request.getRequestURI());
        return true;
    }

    /** 是否标注了 @RequireSign（类级或方法级） */
    private boolean isRequireSign(Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return false;
        }
        HandlerMethod method = (HandlerMethod) handler;
        return method.hasMethodAnnotation(RequireSign.class)
                || method.getBeanType().isAnnotationPresent(RequireSign.class);
    }

    /** 提取查询参数（key→value，供签名计算） */
    private Map<String, String> extractQuery(HttpServletRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        request.getParameterMap().forEach((k, v) -> result.put(k, v.length > 0 ? v[0] : ""));
        return result;
    }
}
