package com.example.sign.simplified;

import com.example.sign.config.SignPracticeProperties;
import com.example.sign.signature.HmacSignService;
import com.example.sign.support.SignLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 08. 简化版方案：appid + timestamp + nonce + uri + sorted_params。
 *
 * 接口不多、安全要求不高时，文档第五节给了简化方案：
 *   待签名字符串 = appid + timestamp + nonce + uri + sorted_params_json
 * 少了 Content-MD5 / Content-Type / CanonicalHeaders / HashedPayload，
 * 实现更轻，但 body 与头不参与签名（代价是完整性弱一些）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimplifiedService {

    private final HmacSignService signService;
    private final SignPracticeProperties props;
    private final SignLogStore logStore;

    /**
     * 简化签名演示：拼「appid+timestamp+nonce+uri+排序参数」并计算签名。
     */
    public Map<String, Object> demo(String uri, String params) {
        Map<String, Object> result = new LinkedHashMap<>();
        String appId = props.getDemoAppId();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 参数排序（TreeMap 字典序）
        Map<String, String> sortedParams = parseParams(params);
        String paramsJson = new TreeMap<>(sortedParams).toString();   // {k=v, k2=v2} 顺序稳定

        // 简化版待签串
        String toSign = appId + timestamp + nonce + uri + paramsJson;
        String signature = signService.hmacSha256(props.getDemoAppKey(), toSign);

        result.put("appid", appId);
        result.put("timestamp", timestamp);
        result.put("nonce", nonce);
        result.put("uri", uri);
        result.put("sortedParams", sortedParams);
        result.put("paramsJson", paramsJson);
        result.put("toSign", toSign);
        result.put("signature", signature);
        result.put("tip", "简化版就 5 要素拼成一个字符串（appid+timestamp+nonce+uri+排序参数）再 HMAC："
                + "比标准 9 字段版少一半字段，适合内部接口，但 body 不参与签名。");

        logStore.add("simplified", appId, true, "简化签名");
        return result;
    }

    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("formula", "待签名字符串 = appid + timestamp + nonce + uri + sorted_params_json；"
                + "签名 = HMAC-SHA256(appkey, 待签名字符串)");
        result.put("whenToSimplify", new String[]{
                "接口数量少、都是内部/半公开调用",
                "安全要求中等：主要诉求是「别被裸调 + 基本防重放」",
                "不想维护复杂的 9 字段规范（文档、SDK 成本）"
        });
        result.put("tradeoffs", new LinkedHashMap<String, Object>() {{
            put("少了什么", "Content-MD5 / Content-Type / CanonicalHeaders / HashedPayload 都不参与签名");
            put("代价", "body 不参与 → 请求体可被篡改而签名不变（需配合 TLS 或业务层校验）");
            put("好处", "实现简单、易调试、字段少不易拼错");
        }});
        result.put("keyPrinciples", new String[]{
                "1. appkey 绝不出现在请求中",
                "2. 时间戳 + nonce 必须同时存在，缺一不可防重放",
                "3. 签名比对用常量时间比较（MessageDigest.isEqual），不用 =="
        });
        result.put("tip", "简化版可以当快速原型；正式开放 API 建议升级标准版（body 参与签名）。");
        return result;
    }

    private Map<String, String> parseParams(String params) {
        Map<String, String> result = new LinkedHashMap<>();
        if (params == null || params.isEmpty()) {
            return result;
        }
        for (String pair : params.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                result.put(pair.substring(0, idx), pair.substring(idx + 1));
            }
        }
        return result;
    }
}
