package com.example.sign.principle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 01. 核心原理：appid / appkey / 签名三要素与鉴权流程。
 *
 * 一句话：HMAC-SHA256 请求签名鉴权 = 用 appkey 对「请求关键信息」做带密钥的
 * 哈希（HMAC），客户端传 appid + 签名，服务端用 appid 查回 appkey 重算比对。
 * 全程不传输密钥本身，兼顾安全性与实现复杂度——AWS / 阿里云 / 微信支付同款。
 */
@Slf4j
@Service
public class PrincipleService {

    /**
     * 三要素速记。
     */
    public Map<String, Object> elements() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("elements", new LinkedHashMap<String, Object>() {{
            put("appid", "应用唯一标识，可公开传输（Header X-App-Id），用于服务端定位 appkey");
            put("appkey", "应用密钥，绝不传输，仅用于服务端签名计算（HMAC-SHA256 的密钥）");
            put("签名 Signature", "用 appkey 对请求关键信息做 HMAC-SHA256，客户端传签名，服务端验签");
        }});
        result.put("whyHmac", "HMAC = 带密钥的哈希：即使攻击者知道签名算法，没有 appkey 也算不出合法签名；"
                + "且 appkey 不在网络传输，抓包也无法还原密钥");
        result.put("tip", "记住一句话：appid 是「你是谁」，appkey 是「只有你和服务端知道的暗号」，签名是「用暗号盖的章」。");
        return result;
    }

    /**
     * 鉴权流程 6 步。
     */
    public Map<String, Object> flow() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("steps", new String[]{
                "① 客户端组装待签名字符串（Canonical String）",
                "② 客户端用 appkey 计算 HMAC-SHA256 签名",
                "③ 客户端发送请求：appid + 时间戳 + nonce + 签名（Header 或参数）",
                "④ 服务端根据 appid 查 appkey（AppId 不存在 → 拒绝）",
                "⑤ 服务端用相同算法重算签名",
                "⑥ 常量时间比对，一致则放行，否则 401"
        });
        result.put("diagram", "客户端 --(appid,timestamp,nonce,signature)--> 服务端：查appkey -> 重算 -> 比对 -> 放行/拒绝");
        result.put("tip", "签名只在客户端生成一次，服务端每次都独立重算：两端算法一致才能对上。");
        return result;
    }

    /**
     * 签名 vs 简单 API Key。
     */
    public Map<String, Object> vsApiKey() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("apiKey", new String[]{
                "直接传 key 走明文/可被截获：抓包、日志、Referer 都可能泄露",
                "没有防重放：同一个请求可以无限重放",
                "没有完整性：请求体被篡改服务端无从察觉",
                "适合：内部工具、低敏接口、临时调试"
        });
        result.put("signature", new String[]{
                "密钥不传输：只有 HMAC 计算结果在网络上",
                "防重放：时间戳 + nonce 双保险",
                "完整性：请求体参与签名，篡改即签名不匹配",
                "适合：对外开放 API、金融/支付级接口（AWS/阿里云/微信支付同款）"
        });
        result.put("conclusion", "复杂度只多一个「签名计算」，换来的却是密钥安全 + 防重放 + 完整性，性价比极高。");
        result.put("tip", "面试：被问「API 鉴权怎么做」先答 HMAC 签名，再说对比 API Key 的三个优势，就是标准答案。");
        return result;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("whyRecommended", new String[]{
                "安全性：密钥不传输（仅 HMAC 结果），防重放（时间戳+nonce），防篡改（body 参与签名）",
                "实现复杂度：中等，一套签名引擎 + 一个拦截器即可落地",
                "生态成熟：AWS SigV4、阿里云、微信支付、腾讯云全是 HMAC 系方案",
                "无状态：服务端无需保存会话，天然适合水平扩展与网关统一鉴权"
        });
        result.put("alternatives", new LinkedHashMap<String, Object>() {{
            put("简单 API Key", "直接传 key，低敏内部用");
            put("JWT (RS256)", "用户态鉴权 / SSO，携带身份声明");
            put("OAuth 2.0", "第三方开放平台，授权流程复杂");
            put("HMAC-SHA256 签名", "接口间调用鉴权，通用首选");
        }});
        result.put("tip", "一句话定位：接口与接口之间（机器对机器）的鉴权用 HMAC 签名；用户登录态用 JWT；第三方授权用 OAuth。");
        return result;
    }
}
