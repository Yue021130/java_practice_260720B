package com.example.satoken.signature;

import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * API 参数签名：防篡改、防重放。
 *
 * 客户端按规则拼接参数 + 时间戳 + 密钥生成签名；服务端用同样规则校验。
 */
@RestController
@RequestMapping("/api/signature")
public class SignatureController {

    private static final String APP_SECRET = "app-secret-123456";
    private static final long REPLAY_WINDOW_SECONDS = 300;

    /**
     * 生成签名示例。
     */
    @PostMapping("/generate")
    public ApiResponse<Map<String, String>> generateSignature(
            @RequestParam Map<String, String> params) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        params.put("timestamp", timestamp);
        String sign = buildSign(params, APP_SECRET);

        Map<String, String> data = new HashMap<>(params);
        data.put("signature", sign);
        data.put("tip", "请求时携带 timestamp 与 signature，服务端校验时间戳与签名");
        return ApiResponse.success(data);
    }

    /**
     * 校验签名。
     */
    @PostMapping("/verify")
    public ApiResponse<Map<String, Object>> verifySignature(
            @RequestParam Map<String, String> params,
            @RequestParam String signature,
            @RequestParam long timestamp) {
        Map<String, Object> result = new HashMap<>();

        // 防重放：时间戳在有效窗口内
        long now = System.currentTimeMillis() / 1000;
        boolean inWindow = Math.abs(now - timestamp) <= REPLAY_WINDOW_SECONDS;
        result.put("timestampValid", inWindow);

        // 校验签名
        params.put("timestamp", String.valueOf(timestamp));
        String expectSign = buildSign(params, APP_SECRET);
        boolean signValid = expectSign.equals(signature);
        result.put("signatureValid", signValid);
        result.put("expectSignature", expectSign);
        result.put("pass", inWindow && signValid);

        return ApiResponse.success(result);
    }

    /**
     * 签名算法：参数按 key 排序后拼接成字符串，再与密钥做 HMAC-SHA256（Base64）。
     */
    static String buildSign(Map<String, String> params, String secret) {
        List<String> keys = new ArrayList<>(params.keySet());
        keys.remove("signature");
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            sb.append(key).append("=").append(params.get(key)).append("&");
        }
        sb.append("secret=").append(secret);
        return hmacSha256(sb.toString(), secret);
    }

    private static String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 失败", e);
        }
    }
}
