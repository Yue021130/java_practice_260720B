package com.example.sign.interceptor;

import com.example.sign.config.SignAuthInterceptor;
import com.example.sign.config.SignPracticeProperties;
import com.example.sign.signature.HmacSignService;
import com.example.sign.signature.SignVerifier;
import com.example.sign.support.SignLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 09. 拦截器实战。
 *
 * 真实工程的做法：签名校验写在拦截器 / 过滤器 / 网关，业务接口一个注解搞定。
 * 这里演示：① /generate 扮演客户端生成合法签名；② /protected 是标注 @RequireSign
 * 的受保护接口（未带签名被拦截器 401）；③ /secure-demo 用统一校验器跑一遍闭环
 * （和拦截器完全相同的代码），展示「篡改 → 拒绝」。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterceptorService {

    private final HmacSignService signService;
    private final SignVerifier verifier;
    private final SignPracticeProperties props;
    private final SignLogStore logStore;

    /**
     * 生成一组合法签名（模拟客户端）：appid / timestamp / nonce / uri / signature。
     * 前端或测试拿到这组值，放进 X- 头即可访问受保护接口。
     */
    public Map<String, Object> generate(String uri) {
        Map<String, Object> result = new LinkedHashMap<>();
        String appId = props.getDemoAppId();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String method = "GET";

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(SignAuthInterceptor.HEADER_APP_ID, appId);
        headers.put(SignAuthInterceptor.HEADER_TIMESTAMP, timestamp);
        headers.put(SignAuthInterceptor.HEADER_NONCE, nonce);

        String toSign = signService.buildCanonicalString(method, "", "", timestamp, nonce, uri,
                new LinkedHashMap<>(), headers, "");
        String signature = signService.hmacSha256(props.getDemoAppKey(), toSign);

        result.put("appId", appId);
        result.put("timestamp", timestamp);
        result.put("nonce", nonce);
        result.put("uri", uri);
        result.put("signature", signature);
        result.put("headers", new LinkedHashMap<String, Object>() {{
            put(SignAuthInterceptor.HEADER_APP_ID, appId);
            put(SignAuthInterceptor.HEADER_TIMESTAMP, timestamp);
            put(SignAuthInterceptor.HEADER_NONCE, nonce);
            put(SignAuthInterceptor.HEADER_SIGNATURE, signature);
        }});
        result.put("tip", "把上面 4 个头原样带上访问 /api/interceptor/protected，拦截器验签通过 → 200；"
                + "缺头或篡改任一字段 → 401。");

        logStore.add("interceptor", appId, true, "生成签名（模拟客户端）");
        return result;
    }

    /**
     * 验签闭环演示：服务端「扮演客户端」生成签名 → 用统一校验器验签。
     * tamper=true 时篡改 uri，校验器拒绝——与拦截器完全相同的代码路径。
     */
    public Map<String, Object> secureDemo(boolean tamper) {
        Map<String, Object> result = new LinkedHashMap<>();
        String appId = props.getDemoAppId();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String originalUri = "/api/interceptor/protected";

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(SignAuthInterceptor.HEADER_APP_ID, appId);
        headers.put(SignAuthInterceptor.HEADER_TIMESTAMP, timestamp);
        headers.put(SignAuthInterceptor.HEADER_NONCE, nonce);

        // 客户端按「原始 uri」计算签名
        String toSign = signService.buildCanonicalString("GET", "", "", timestamp, nonce, originalUri,
                new LinkedHashMap<>(), headers, "");
        String clientSignature = signService.hmacSha256(props.getDemoAppKey(), toSign);

        // 攻击者视角：篡改发送的 uri（签名仍是按原始 uri 算的，改的只是请求本身）
        String sentUri = tamper ? "/api/interceptor/hacked" : originalUri;
        SignVerifier.VerifyResult v = verifier.verify(appId, timestamp, nonce, "GET",
                sentUri, new LinkedHashMap<>(), headers, "", clientSignature);

        result.put("tampered", tamper);
        result.put("uri", originalUri);
        result.put("sentUri", sentUri);
        result.put("clientSignature", clientSignature);
        result.put("passed", v.passed());
        result.put("reason", v.reason());
        result.put("tip", tamper
                ? "请求的 uri 被改成 /api/interceptor/hacked，但签名是按原始 uri(/api/interceptor/protected) 算的 → "
                + "校验器拒绝（" + v.reason() + "）。真实拦截器对带同样签名的请求也会 401。"
                : "完整闭环：客户端生成签名 → 统一校验器验签通过。真实拦截器对相同请求同样放行。");

        logStore.add("interceptor", appId, v.passed(), tamper ? "篡改 uri" : "正常调用");
        return result;
    }

    /**
     * 受保护接口的业务数据（只有通过拦截器鉴权才能到达这里）。
     */
    public Map<String, Object> protectedData(String data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("message", "你已通过 HMAC-SHA256 签名鉴权，进入受保护接口");
        result.put("tip", "这个接口标注了 @RequireSign：没带合法签名，请求根本到不了这里（被拦截器 401）。");
        return result;
    }

    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mechanism", new LinkedHashMap<String, Object>() {{
            put("注解", "@RequireSign 标在类/方法上，声明「这里要签名」");
            put("拦截器", "SignAuthInterceptor 注册到 /**，preHandle 里发现 @RequireSign 就走 SignVerifier 校验");
            put("统一逻辑", "时间戳 → nonce → appkey → 重算比对，全部收敛在 SignVerifier，拦截器与演示共用");
            put("失败", "返回 401 + 结构化原因，不抛堆栈");
        }});
        result.put("vsAlternatives", new LinkedHashMap<String, Object>() {{
            put("过滤器 Filter", "更早介入（Servlet 层），能拦所有请求包括静态资源；拦截器只拦 SpringMVC 的 Handler");
            put("拦截器 Interceptor", "能拿到 HandlerMethod（读注解），本项目选择；适合业务鉴权");
            put("AOP", "注解 + 切面也能做，但拿不到 request/response 上下文那么方便");
            put("网关 Gateway", "分布式下的统一入口鉴权（一次校验全部下游），签名逻辑可下沉到网关");
        }});
        result.put("tip", "单机用拦截器、微服务用网关：把「验签」放在请求链的最前面，业务代码零侵入。");
        return result;
    }
}
