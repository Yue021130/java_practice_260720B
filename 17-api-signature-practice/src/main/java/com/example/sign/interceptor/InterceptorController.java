package com.example.sign.interceptor;

import com.example.sign.common.ApiResponse;
import com.example.sign.config.RequireSign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 09. 拦截器实战：@RequireSign 注解 + Spring 拦截器统一鉴权。
 *
 * 注意 {@code /api/interceptor/protected} 标注了 @RequireSign，
 * 必须带 X-App-Id / X-Timestamp / X-Nonce / X-Signature 头才能访问。
 */
@RestController
@RequestMapping("/api/interceptor")
@RequiredArgsConstructor
@Tag(name = "09. 拦截器实战", description = "@RequireSign 注解 + 拦截器统一鉴权 + 受保护接口")
public class InterceptorController {

    private final InterceptorService service;

    @GetMapping("/generate")
    @Operation(summary = "生成合法签名（模拟客户端）", description = "返回一组可直接用于 protected 接口的签名头")
    public ApiResponse<Map<String, Object>> generate(@RequestParam(defaultValue = "/api/interceptor/protected") String uri) {
        return ApiResponse.success(service.generate(uri));
    }

    @GetMapping("/secure-demo")
    @Operation(summary = "验签闭环演示", description = "服务端模拟客户端生成签名 → 走统一校验器 → 通过/拒绝（可篡改）")
    public ApiResponse<Map<String, Object>> secureDemo(@RequestParam(defaultValue = "false") boolean tamper) {
        return ApiResponse.success(service.secureDemo(tamper));
    }

    @GetMapping("/protected")
    @RequireSign
    @Operation(summary = "受保护接口（@RequireSign）", description = "未带合法签名 → 401；携带 → 200。用 /generate 拿签名头")
    public ApiResponse<Map<String, Object>> protectedApi(@RequestParam(defaultValue = "用户数据") String data) {
        return ApiResponse.success(service.protectedData(data));
    }

    @GetMapping("/explain")
    @Operation(summary = "拦截器实战速记（八股）", description = "注解 + 拦截器机制 / 与网关 / AOP 对比")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}
