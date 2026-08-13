package com.example.sign.sign;

import com.example.sign.config.SignPracticeProperties;
import com.example.sign.signature.HmacSignService;
import com.example.sign.support.SignLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 02. 签名计算：Canonical String 构建 + HMAC-SHA256。
 *
 * 完整演示「客户端该做的两件事」：先拼待签名字符串，再用 appkey 算签名。
 * 返回的 canonicalString + signature 前端可拿来与任何 HMAC-SHA256 工具对照复算。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignService {

    private final HmacSignService signService;
    private final SignPracticeProperties props;
    private final SignLogStore logStore;

    /**
     * 完整签名计算演示：组装 9 字段 Canonical String → 算 HMAC-SHA256。
     *
     * 故意用可复算的固定输入（不含随机字段随机变化的部分用「演示值」），
     * 并把每步结果都返回，方便前端对照标准 HMAC-SHA256 工具验证。
     */
    public Map<String, Object> compute(String method, String uri, String query, String body) {
        Map<String, Object> result = new LinkedHashMap<>();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 解析 query 为参数表（演示值，服务端从请求里解析）
        Map<String, String> queryParams = parseQuery(query);
        // 参与签名的请求头（此处按文档示例：appid/timestamp/nonce）
        Map<String, String> signedHeaders = new LinkedHashMap<>();
        signedHeaders.put("X-App-Id", props.getDemoAppId());
        signedHeaders.put("X-Timestamp", timestamp);
        signedHeaders.put("X-Nonce", nonce);

        String contentMd5 = signService.contentMd5(body);
        String contentType = body.isEmpty() ? "" : "application/json";
        String canonicalQuery = signService.canonicalQueryString(queryParams);
        String canonicalHeaders = signService.canonicalHeaders(signedHeaders);
        String hashedPayload = signService.hashedPayload(body);

        String toSign = signService.buildCanonicalString(
                method, contentMd5, contentType, timestamp, nonce, uri, queryParams, signedHeaders, body);
        String signature = signService.hmacSha256(props.getDemoAppKey(), toSign);

        result.put("appid", props.getDemoAppId());
        result.put("appkey", mask(props.getDemoAppKey()));
        result.put("timestamp", timestamp);
        result.put("nonce", nonce);
        result.put("fields", new LinkedHashMap<String, Object>() {{
            put("HTTPMethod", method);
            put("Content-MD5", contentMd5.isEmpty() ? "(空串)" : contentMd5);
            put("Content-Type", contentType.isEmpty() ? "(空串)" : contentType);
            put("Timestamp", timestamp);
            put("Nonce", nonce);
            put("CanonicalURI", uri);
            put("CanonicalQueryString", canonicalQuery.isEmpty() ? "(空串)" : canonicalQuery);
            put("CanonicalHeaders", canonicalHeaders.replace("\n", "\\n"));
            put("HashedPayload", hashedPayload);
        }});
        result.put("canonicalString", toSign);
        result.put("signature", signature);
        result.put("tip", "把 canonicalString 和 appkey 喂给任意 HMAC-SHA256 工具，都应得到相同 signature——"
                + "这就是「两端算法一致才能对上」的落地演示。");

        logStore.add("sign", props.getDemoAppId(), true, "签名计算演示");
        return result;
    }

    /**
     * Canonical String 9 字段拆解。
     */
    public Map<String, Object> canonical() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("template", "HTTPMethod\\nContent-MD5\\nContent-Type\\nTimestamp\\nNonce\\n"
                + "CanonicalURI\\nCanonicalQueryString\\nCanonicalHeaders\\nHashedPayload");
        result.put("fields", new LinkedHashMap<String, Object>() {{
            put("HTTPMethod", "大写，如 GET / POST");
            put("Content-MD5", "请求体 MD5（Base64），无 body 填空串");
            put("Content-Type", "如 application/json，无 body 填空串");
            put("Timestamp", "Unix 秒时间戳，如 1723537860");
            put("Nonce", "随机字符串防重放，如 UUID");
            put("CanonicalURI", "规范化请求路径，如 /api/v1/users");
            put("CanonicalQueryString", "参数按 key 排序后 key1=val1&key2=val2");
            put("CanonicalHeaders", "参与签名的头按 key 排序后 key1:val1\\nkey2:val2");
            put("HashedPayload", "请求体 SHA256 哈希，无 body 用空串哈希");
        }});
        result.put("tip", "9 个字段必须用固定顺序、固定分隔符（\\n）拼接：任何一端拼错一位，签名就对不上。");
        return result;
    }

    /**
     * 手工验签对照：给定 appkey 与待签串，重算签名并与传入签名比对。
     */
    public Map<String, Object> verifyManual(String appKey, String toSign, String signature) {
        Map<String, Object> result = new LinkedHashMap<>();
        String recomputed = signService.hmacSha256(appKey, toSign);
        boolean matched = signService.secureCompare(recomputed, signature);
        result.put("recomputed", recomputed);
        result.put("clientSignature", signature);
        result.put("matched", matched);
        result.put("tip", matched ? "重算签名一致：验签通过" : "重算签名不一致：验签失败（appkey 或待签串有差异）");
        return result;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("whyOrdered", "待签串必须按固定顺序拼接：客户端和服务端才能算出完全相同的字符串；"
                + "顺序、分隔符、空字段规则都要在文档里定死");
        result.put("emptyRules", "无 body：Content-MD5 与 Content-Type 填空串、HashedPayload 用空串的 SHA256；"
                + "无 query：CanonicalQueryString 为空串；无参与签名头：CanonicalHeaders 为空串");
        result.put("whyUriQueryHeaders", "把 uri / query / headers 纳入签名，任何一处被篡改都会导致签名不匹配——"
                + "这就是完整性校验的根源");
        result.put("javaApi", "javax.crypto.Mac（HmacSHA256）+ SecretKeySpec(appkey) + doFinal(toSign)；"
                + "比对用 MessageDigest.isEqual 防时序攻击");
        result.put("tip", "面试：能把 9 字段顺序背下来 + 说明空字段规则，再讲一句「顺序和空字段规则要两端一致」，就很扎实。");
        return result;
    }

    /** 解析 a=1&b=2 为 Map */
    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) {
            return result;
        }
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            if (idx > 0) {
                result.put(pair.substring(0, idx), pair.substring(idx + 1));
            }
        }
        return result;
    }

    /** 脱敏展示 appkey（真实工程绝不回显） */
    private String mask(String key) {
        if (key == null || key.length() < 8) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
