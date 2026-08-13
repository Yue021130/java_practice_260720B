package com.example.sign.signature;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * HMAC-SHA256 请求签名引擎（真实可复用实现，非 mock）。
 *
 * 对应方案文档：
 * - 待签名字符串（Canonical String）：按固定顺序拼接 9 个字段，用 {@code \n} 分隔：
 *   HTTPMethod / Content-MD5 / Content-Type / Timestamp / Nonce /
 *   CanonicalURI / CanonicalQueryString / CanonicalHeaders / HashedPayload
 * - 签名 = HMAC-SHA256(appkey, Canonical String)
 * - 验签 = 服务端用同一 appkey 重算后与客户端签名比对（防时序攻击用
 *   {@code MessageDigest.isEqual}，对应文档的 {@code hmac.compare_digest}）
 *
 * 关键安全点（全程贯彻）：
 * 1. appkey 绝不参与传输，只出现在服务端签名计算里；
 * 2. 时间戳 + nonce 必须同时存在，缺一不可防重放；
 * 3. 签名比对必须用常量时间比较，不能用 {@code ==}。
 */
@Slf4j
@Service
public class HmacSignService {

    /** 参与签名的 HTTP 方法（大写） */
    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";

    /**
     * 计算 HMAC-SHA256 十六进制签名。
     *
     * @param appKey   应用密钥（服务端从 appid 查到，绝不传输）
     * @param toSign   待签名字符串
     * @return 64 位小写十六进制签名
     */
    public String hmacSha256(String appKey, String toSign) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 计算失败", e);
        }
    }

    /**
     * 构建标准 Canonical String（文档第 3 节的 9 字段版本）。
     *
     * @param method       HTTP 方法（GET/POST，大写）
     * @param contentMd5   请求体 MD5（无 body 传空串）
     * @param contentType  Content-Type（无 body 传空串）
     * @param timestamp    Unix 秒时间戳
     * @param nonce        随机串（防重放）
     * @param canonicalUri 规范化后的请求路径
     * @param queryParams  查询参数（内部按 key 排序）
     * @param headers      参与签名的请求头（key→value，内部按 key 排序）
     * @param body         请求体原始内容（参与 HashedPayload 计算）
     */
    public String buildCanonicalString(String method, String contentMd5, String contentType,
                                       String timestamp, String nonce, String canonicalUri,
                                       Map<String, String> queryParams, Map<String, String> headers,
                                       String body) {
        String canonicalQuery = canonicalQueryString(queryParams);
        String canonicalHeaders = canonicalHeaders(headers);
        String hashedPayload = hashedPayload(body);
        // 9 字段按固定顺序用 \n 拼接
        return method + "\n"
                + contentMd5 + "\n"
                + contentType + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + canonicalUri + "\n"
                + canonicalQuery + "\n"
                + canonicalHeaders + "\n"
                + hashedPayload;
    }

    /**
     * 规范化查询串：key 按字典序排序，key1=val1&key2=val2，空参数集返回空串。
     *
     * 为什么必须排序：同一组参数顺序不同会算出不同签名，排序后客户端与服务端
     * 才可能算出一样的签名（对应文档 CanonicalQueryString 一节）。
     */
    public String canonicalQueryString(Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return "";
        }
        TreeMap<String, String> sorted = new TreeMap<>(queryParams);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(e.getKey()).append('=').append(e.getValue() == null ? "" : e.getValue());
        }
        return sb.toString();
    }

    /**
     * 规范化请求头：key 转小写按字典序排序，key:value 换行分隔。
     * 只包含参与签名的头（如 x-app-id / x-timestamp / x-nonce），不含 host 等。
     */
    public String canonicalHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            sorted.put(e.getKey().toLowerCase(), e.getValue());
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            sb.append(e.getKey()).append(':').append(e.getValue()).append('\n');
        }
        return sb.toString();
    }

    /**
     * 请求体 SHA256 哈希（十六进制）。无 body 用空串哈希，保证签名对空体也稳定。
     */
    public String hashedPayload(String body) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((body == null ? "" : body).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 计算失败", e);
        }
    }

    /**
     * 请求体 MD5（Base64，对应文档 Content-MD5）。无 body 返回空串。
     */
    public String contentMd5(String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(body.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("MD5 计算失败", e);
        }
    }

    /**
     * 常量时间签名比对（对应文档 hmac.compare_digest）。
     * 用 {@code ==} 或 equals 会有时序攻击风险：攻击者可通过响应耗时逐位猜出签名。
     */
    public boolean secureCompare(String serverSignature, String clientSignature) {
        if (serverSignature == null || clientSignature == null) {
            return false;
        }
        return MessageDigest.isEqual(
                serverSignature.getBytes(StandardCharsets.UTF_8),
                clientSignature.getBytes(StandardCharsets.UTF_8));
    }
}
