package com.example.sign.summary;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 10. 选型总结：HMAC 签名 vs API Key vs JWT vs OAuth。
 *
 * 给整条「接口鉴权」线收尾：什么场景选什么方案、三个关键原则、
 * 签名对不上的排查清单、生产落地建议。
 */
@Slf4j
@Service
public class SummaryService {

    /**
     * 鉴权方案全景。
     */
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("family", new LinkedHashMap<String, Object>() {{
            put("接口间鉴权（机器对机器）", "HMAC-SHA256 请求签名 ← 本模块主题，通用首选");
            put("用户登录态（人）", "JWT / Session：携带身份声明，无状态可扩展");
            put("第三方授权（开放平台）", "OAuth 2.0：授权码 / 客户端凭证，流程重但规范");
            put("内部低敏", "简单 API Key 直传（不推荐对外开放）");
        }});
        result.put("position", "HMAC 签名的定位：接口与接口之间调用的身份校验 + 防篡改 + 防重放，"
                + "不承载「用户身份」，只回答「这个调用方是谁、请求有没有被改过」。");
        result.put("tip", "本模块 01~09 章把 HMAC 方案的每个环节都做了可运行演示，配合本页就是完整闭环。");
        return result;
    }

    /**
     * 四种方案对比表（对应文档第六节）。
     */
    public Map<String, Object> compare() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("table", new LinkedHashMap<String, Object>() {{
            put("HMAC-SHA256 签名", "安全性 ★★★★★ / 复杂度 中 / 适用：通用方案，AWS/阿里云/微信支付同款");
            put("简单 API Key", "安全性 ★★ / 复杂度 低 / 适用：内部工具、低敏接口");
            put("JWT (RS256)", "安全性 ★★★★ / 复杂度 中 / 适用：用户态鉴权、SSO");
            put("OAuth 2.0", "安全性 ★★★★★ / 复杂度 高 / 适用：第三方开放平台");
        }});
        result.put("rule", "选型三问：调用方是谁？（应用/用户/第三方）→ 要防什么？（重放/篡改/身份伪造）→ 愿付多少复杂度？");
        result.put("tip", "拿不准就选 HMAC 签名：安全性达标、复杂度可控、生态成熟。");
        return result;
    }

    /**
     * 三个关键原则。
     */
    public Map<String, Object> principles() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("principles", new LinkedHashMap<String, Object>() {{
            put("1. appkey 绝不出现在请求中", "只传 appid + 签名结果；appkey 仅服务端算签名用，抓包拿不到密钥");
            put("2. 时间戳 + nonce 必须同时存在", "时间戳挡「老请求」，nonce 挡「窗口内重放」，缺一不可");
            put("3. 签名比对用常量时间比较", "MessageDigest.isEqual / hmac.compare_digest，不用 ==/equals，防时序攻击");
        }});
        result.put("extra", new String[]{
                "body 要参与签名（HashedPayload），否则改了 body 服务端不知道",
                "appkey 存服务端加密存储，泄露要能吊销轮换",
                "失败统一 401 + 结构化原因，不泄露内部细节"
        });
        result.put("tip", "三条原则 + body 参与签名，就是一套生产级 HMAC 鉴权的底线。");
        return result;
    }

    /**
     * 常见坑与调优。
     */
    public Map<String, Object> pitfalls() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("signatureMismatch", new String[]{
                "query 忘了排序（最最常见）",
                "Header 大小写不一致（X-App-Id vs x-app-id）",
                "空字段规则不一致（null vs 空串）",
                "URL 编码用错（+ vs %20）",
                "body 参与签名但读取 body 后未缓存，重算时读到空"
        });
        result.put("debug", "调试第一招：把客户端和服务端的 canonicalString 都打出来逐字 diff，差异一眼可见");
        result.put("production", new String[]{
                "签名校验放最前面：网关/过滤器，别等业务代码执行",
                "appkey 分环境、分应用，支持吊销与轮换",
                "时间戳窗口按业务时延敏感度调（默认 ±5min）",
                "配合 HTTPS：签名防重放防篡改，TLS 防窃听，两者不冲突",
                "监控：验签失败率、nonce 存储大小、时间戳异常分布"
        });
        result.put("tip", "生产上签名鉴权通常和限流、审计一起做在网关卡。");
        return result;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("interviewFlow", new String[]{
                "1. 定位：接口间鉴权首选 HMAC-SHA256 请求签名（AWS/阿里云同款）",
                "2. 三要素：appid（公开定位）、appkey（秘密、不传输）、签名（HMAC 结果）",
                "3. 算法：9 字段 Canonical String（method/md5/type/timestamp/nonce/uri/query/headers/payload）→ HMAC-SHA256",
                "4. 防重放：时间戳 ±窗口 + nonce 去重（Redis SETNX+TTL）",
                "5. 完整性：uri/query/body 都参与签名，篡改即失配",
                "6. 实现：拦截器/网关统一校验，业务接口一个注解",
                "7. 收尾：常量时间比对 + 失败 401"
        });
        result.put("oneSentence", "HMAC 签名鉴权 = 用 appkey 把「请求关键信息 + 时间戳 + nonce」做成带密钥的指纹，"
                + "服务端重算比对：密钥不传输、重放被拦、篡改必现——这就是它成为业界标配的原因。");
        result.put("tip", "被问「接口怎么鉴权」按这 7 步走一遍，从原理到落地全覆盖。");
        return result;
    }
}
