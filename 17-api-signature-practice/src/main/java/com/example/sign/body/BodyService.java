package com.example.sign.body;

import com.example.sign.signature.HmacSignService;
import com.example.sign.support.SignLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 06. 请求体完整性：Content-MD5 / HashedPayload 防 body 篡改。
 *
 * 光对 uri / query 签名还不够——POST 请求体如果不参与签名，攻击者改 body
 * 服务端无法察觉。两种做法（可同时用）：
 * - Content-MD5：请求头携带 body 的 MD5，服务端对 body 重算 MD5 比对；
 * - HashedPayload：body 的 SHA256 直接拼进 Canonical String 参与 HMAC。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BodyService {

    private final HmacSignService signService;
    private final SignLogStore logStore;

    /**
     * body 完整性演示：客户端按原始 body 算签名，服务端按「收到的 body」验签。
     * tamper=true 时收到的 body 被改动（金额被改），签名立刻失效。
     */
    public Map<String, Object> demo(String body, boolean tamper) {
        Map<String, Object> result = new LinkedHashMap<>();
        String receivedBody = tamper ? body.replace("20", "9999") : body;

        // 客户端（原始 body）侧
        String clientMd5 = signService.contentMd5(body);
        String clientPayloadHash = signService.hashedPayload(body);

        // 服务端（收到的 body）侧
        String serverMd5 = signService.contentMd5(receivedBody);
        String serverPayloadHash = signService.hashedPayload(receivedBody);

        boolean md5Match = clientMd5.equals(serverMd5);
        boolean payloadMatch = signService.secureCompare(clientPayloadHash, serverPayloadHash);

        result.put("originalBody", body);
        result.put("receivedBody", receivedBody);
        result.put("tampered", !body.equals(receivedBody));
        result.put("contentMd5", new LinkedHashMap<String, Object>() {{
            put("client", clientMd5);
            put("server", serverMd5);
            put("match", md5Match);
        }});
        result.put("hashedPayload", new LinkedHashMap<String, Object>() {{
            put("client", clientPayloadHash);
            put("server", serverPayloadHash);
            put("match", payloadMatch);
        }});
        result.put("passed", md5Match && payloadMatch);
        result.put("tip", tamper
                ? "收到的 body 被篡改（金额 20→9999）：Content-MD5 与 HashedPayload 双双失配，请求被拒——body 完整性生效"
                : "body 未篡改：Content-MD5 与 HashedPayload 一致，完整。body 参与签名后，改 body 就是改签名。");

        logStore.add("body", "demo", md5Match && payloadMatch, tamper ? "body 被篡改" : "body 完整");
        return result;
    }

    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("whyBody", "GET 的 uri/query 参与签名后，篡改会被发现；但 POST 的 body 若不参与，攻击者改 JSON 内容"
                + "（如把转账金额 20 改成 9999）服务端毫无察觉——必须把 body 纳入完整性校验");
        result.put("contentMd5", new String[]{
                "请求头带 Content-MD5：<Base64(MD5(body))>",
                "服务端对收到的 body 重算 MD5 比对",
                "防篡改，但不防重放、不防伪造（MD5 无密钥，攻击者可自行重算）",
                "很多网关/框架（如 AWS）用它校验 body"
        });
        result.put("hashedPayload", new String[]{
                "body 的 SHA256 十六进制直接拼进 Canonical String 的最后一个字段",
                "因为参与 HMAC，body 被改动 → 整个签名失配",
                "比 Content-MD5 更强：和 appkey 绑定，无法被攻击者自行伪造",
                "无 body 时用空串的哈希，保持签名稳定"
        });
        result.put("practice", "生产建议：HashedPayload 纳入签名（与密钥绑定），Content-MD5 可作为双重保险或网关约定");
        result.put("tip", "一句话：body 必须参与签名，否则签名鉴权就是「只护住了 URL，没护住内容」。");
        return result;
    }
}
