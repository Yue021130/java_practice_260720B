package com.example.sign.signature;

import com.example.sign.config.SignPracticeProperties;
import com.example.sign.support.AppKeyStore;
import com.example.sign.support.NonceStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 接口签名统一校验器（对应方案文档第四节「服务端校验逻辑」）。
 *
 * 封装完整的 5 步校验：提取四要素 → 时间戳窗口 → nonce 去重 → 查 appkey →
 * 重算签名并常量时间比对。拦截器（SignAuthInterceptor）与「拦截器实战」演示
 * 都走这一个实现，保证演示出来的逻辑和真实拦截链路完全一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignVerifier {

    private final HmacSignService signService;
    private final AppKeyStore appKeyStore;
    private final NonceStore nonceStore;
    private final SignPracticeProperties props;

    /**
     * 校验一个请求的签名。
     *
     * @param appId          appid
     * @param timestamp      时间戳（秒字符串）
     * @param nonce          一次性随机串
     * @param method         HTTP 方法（大写）
     * @param uri            请求路径
     * @param queryParams    查询参数（key→value）
     * @param signedHeaders  参与签名的请求头（key→value）
     * @param body           请求体原始内容
     * @param clientSignature 客户端传入的签名
     * @return 校验结果（passed + reason）
     */
    public VerifyResult verify(String appId, String timestamp, String nonce, String method,
                               String uri, Map<String, String> queryParams,
                               Map<String, String> signedHeaders, String body,
                               String clientSignature) {
        // 1. 四要素缺一不可
        if (appId == null || timestamp == null || nonce == null || clientSignature == null) {
            return VerifyResult.fail("鉴权要素缺失：需要 appid / timestamp / nonce / signature");
        }

        // 2. 时间戳校验（±skew 秒）
        long now = System.currentTimeMillis() / 1000L;
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return VerifyResult.fail("时间戳格式非法");
        }
        if (Math.abs(now - ts) > props.getTimestampSkewSeconds()) {
            return VerifyResult.fail("请求已过期：时间戳偏差超过 " + props.getTimestampSkewSeconds() + " 秒");
        }

        // 3. nonce 去重（防重放）
        if (!nonceStore.tryAcquire(nonce)) {
            return VerifyResult.fail("nonce 重复使用，疑似重放攻击");
        }

        // 4. 查 appkey
        String appKey = appKeyStore.getAppKey(appId);
        if (appKey == null) {
            return VerifyResult.fail("AppId 不存在：" + appId);
        }

        // 5. 重算签名并常量时间比对
        String toSign = signService.buildCanonicalString(
                method, signService.contentMd5(body),
                body == null || body.isEmpty() ? "" : "application/json",
                timestamp, nonce, uri, queryParams, signedHeaders, body);
        String serverSignature = signService.hmacSha256(appKey, toSign);
        if (!signService.secureCompare(serverSignature, clientSignature)) {
            return VerifyResult.fail("签名不匹配");
        }

        return VerifyResult.ok();
    }

    /** 校验结果 */
    public static class VerifyResult {
        private final boolean passed;
        private final String reason;

        private VerifyResult(boolean passed, String reason) {
            this.passed = passed;
            this.reason = reason;
        }

        static VerifyResult ok() {
            return new VerifyResult(true, "验签通过");
        }

        static VerifyResult fail(String reason) {
            return new VerifyResult(false, reason);
        }

        public boolean passed() {
            return passed;
        }

        public String reason() {
            return reason;
        }
    }
}
