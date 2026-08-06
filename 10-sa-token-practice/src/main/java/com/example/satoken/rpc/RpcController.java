package com.example.satoken.rpc;

import cn.dev33.satoken.stp.StpUtil;
import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * RPC 调用鉴权与登录态传递思路演示。
 *
 * 微服务或 RPC 框架中，上游把当前 Token 放入上下文，下游取出后调用 StpUtil.login 指定 Token。
 */
@RestController
@RequestMapping("/api/rpc")
public class RpcController {

    /**
     * 上游服务：获取当前 Token，准备传递给下游。
     */
    @GetMapping("/upstream")
    public ApiResponse<Map<String, Object>> upstream() {
        Map<String, Object> data = new HashMap<>();
        data.put("loginId", StpUtil.getLoginIdDefaultNull());
        data.put("tokenValue", StpUtil.getTokenValue());
        data.put("tip", "上游把 tokenValue 放入 RPC 上下文（Dubbo Attachment / gRPC Metadata / Feign Header）");
        return ApiResponse.success(data);
    }

    /**
     * 下游服务：接收上游 Token 并设置为当前登录态。
     */
    @PostMapping("/downstream")
    public ApiResponse<Map<String, Object>> downstream(@RequestParam String tokenValue) {
        StpUtil.setTokenValue(tokenValue);
        Map<String, Object> data = new HashMap<>();
        data.put("tokenValue", StpUtil.getTokenValue());
        data.put("loginId", StpUtil.getLoginIdDefaultNull());
        data.put("tip", "下游从 RPC 上下文取出 Token 后调用 StpUtil.setTokenValue(tokenValue) 即可恢复登录态");
        return ApiResponse.success(data);
    }
}
