package com.example.sign.summary;

import com.example.sign.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 10. 选型对比：HMAC vs API Key vs JWT vs OAuth + 关键原则 + 常见坑。
 */
@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
@Tag(name = "10. 选型总结", description = "四种鉴权方案对比 / 三个关键原则 / 常见坑")
public class SummaryController {

    private final SummaryService service;

    @GetMapping("/overview")
    @Operation(summary = "鉴权方案全景", description = "七大类通信之外，接口鉴权这条线的完整图谱")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(service.overview());
    }

    @GetMapping("/compare")
    @Operation(summary = "四种方案对比表", description = "HMAC 签名 / API Key / JWT / OAuth 的安全性与复杂度")
    public ApiResponse<Map<String, Object>> compare() {
        return ApiResponse.success(service.compare());
    }

    @GetMapping("/principles")
    @Operation(summary = "三个关键原则", description = "appkey 不传输 / 时间戳+nonce 缺一不可 / 常量时间比对")
    public ApiResponse<Map<String, Object>> principles() {
        return ApiResponse.success(service.principles());
    }

    @GetMapping("/pitfalls")
    @Operation(summary = "常见坑与调优", description = "签名对不上的 5 大排查点 / 生产落地建议")
    public ApiResponse<Map<String, Object>> pitfalls() {
        return ApiResponse.success(service.pitfalls());
    }

    @GetMapping("/explain")
    @Operation(summary = "总结速记（八股）", description = "完整回答「接口鉴权怎么做」")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}
