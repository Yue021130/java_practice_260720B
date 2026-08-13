package com.example.sign.verify;

import com.example.sign.config.SignPracticeProperties;
import com.example.sign.signature.HmacSignService;
import com.example.sign.support.AppKeyStore;
import com.example.sign.support.SignLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 03. 服务端验签：重算比对 + 篡改检测。
 *
 * 模拟完整链路：① 客户端拿 appkey 算签名；② 服务端从 appid 查回 appkey、
 * 按相同算法重算、常量时间比对。演示 `tamper` 参数篡改一个字段，
 * 服务端重算的签名立刻对不上 → 拒绝。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyService {

    private final HmacSignService signService;
    private final AppKeyStore appKeyStore;
    private final SignPracticeProperties props;
    private final SignLogStore logStore;

    public Map<String, Object> demo(String tamper) {
        Map<String, Object> result = new LinkedHashMap<>();
        String appId = props.getDemoAppId();
        String appKey = appKeyStore.getAppKey(appId);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String uri = "/api/v1/users";
        String body = "{\"name\":\"张三\",\"age\":20}";
        Map<String, String> query = new LinkedHashMap<>();
        query.put("page", "1");
        query.put("size", "20");
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-App-Id", appId);
        headers.put("X-Timestamp", timestamp);
        headers.put("X-Nonce", nonce);

        // 客户端计算签名（正确的）
        String clientToSign = signService.buildCanonicalString(
                "POST", signService.contentMd5(body), "application/json",
                timestamp, nonce, uri, query, headers, body);
        String clientSignature = signService.hmacSha256(appKey, clientToSign);

        // 模拟攻击者篡改一个字段（其余保持不变）
        String tamperedBody = body;
        String tamperedTimestamp = timestamp;
        String tamperedUri = uri;
        Map<String, String> tamperedQueryMap = new LinkedHashMap<>(query);
        switch (tamper) {
            case "body":
                tamperedBody = "{\"name\":\"李四\",\"age\":20}";
                break;
            case "timestamp":
                tamperedTimestamp = String.valueOf(Long.parseLong(timestamp) - 100);
                break;
            case "uri":
                tamperedUri = "/api/v1/users/1";
                break;
            case "query":
                tamperedQueryMap = new LinkedHashMap<>();
                tamperedQueryMap.put("page", "1");
                tamperedQueryMap.put("size", "999");
                break;
            case "none":
            default:
                break;
        }

        // 服务端按「收到的请求」重算签名（收到的可能是被篡改过的）
        String serverToSign = signService.buildCanonicalString(
                "POST", signService.contentMd5(tamperedBody), "application/json",
                tamperedTimestamp, nonce, tamperedUri, tamperedQueryMap, headers, tamperedBody);
        String serverSignature = signService.hmacSha256(appKey, serverToSign);
        boolean passed = signService.secureCompare(serverSignature, clientSignature);

        result.put("appid", appId);
        result.put("uri", uri);
        result.put("tamper", tamper);
        result.put("clientSignature", clientSignature);
        result.put("serverSignature", serverSignature);
        result.put("passed", passed);
        result.put("reason", passed ? "验签通过：两端签名一致"
                : "验签失败：请求中「" + tamperLabel(tamper) + "」被篡改，服务端重算的签名与客户端不一致");
        result.put("tip", "任何被签名覆盖的字段（body/时间戳/uri/query/头）被改动，客户端签名就失效——"
                + "这就是「篡改即拒绝」的完整性保证。");

        logStore.add("verify", appId, passed, "篡改字段=" + tamper);
        return result;
    }

    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("steps", new String[]{
                "1. 提取四要素：X-App-Id / X-Timestamp / X-Nonce / X-Signature（缺一 → 401）",
                "2. 时间戳校验：允许 ±5 分钟偏差，超窗 → 请求已过期",
                "3. nonce 去重：重复使用 → 重放攻击（Redis SETNX + TTL）",
                "4. 查 appkey：AppId 不存在 → 401",
                "5. 按相同算法重算签名，常量时间比对：不一致 → 401"
        });
        result.put("secureCompare", "用 MessageDigest.isEqual（常量时间比较）而非 ==/equals："
                + "避免攻击者靠响应耗时逐位猜签名（时序攻击）");
        result.put("tip", "验签顺序很讲究：先做廉价的拒绝（时间戳/nonce/查key），最后才做昂贵的 HMAC 计算。");
        return result;
    }

    private String tamperLabel(String tamper) {
        switch (tamper) {
            case "body":
                return "请求体 body";
            case "timestamp":
                return "时间戳";
            case "uri":
                return "请求路径 uri";
            case "query":
                return "查询参数 query";
            default:
                return "无";
        }
    }
}
