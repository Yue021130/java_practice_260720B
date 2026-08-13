package com.example.sign.canonical;

import com.example.sign.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 07. 规范化：URI / QueryString / Headers 排序规则演示。
 */
@RestController
@RequestMapping("/api/canonical")
@RequiredArgsConstructor
@Tag(name = "07. 规范化", description = "URI 编码 / query 排序 / headers 排序，两端才能算出同一签名")
public class CanonicalController {

    private final CanonicalService service;

    @GetMapping("/query-sort")
    @Operation(summary = "QueryString 排序演示", description = "同一组参数不同顺序，排序后规范化结果一致")
    public ApiResponse<Map<String, Object>> querySort() {
        return ApiResponse.success(service.querySort());
    }

    @GetMapping("/headers-sort")
    @Operation(summary = "Headers 排序演示", description = "请求头按 key 小写字典序拼接")
    public ApiResponse<Map<String, Object>> headersSort() {
        return ApiResponse.success(service.headersSort());
    }

    @GetMapping("/uri-encoding")
    @Operation(summary = "URI 规范化", description = "路径编码规则 / 为什么路径必须统一")
    public ApiResponse<Map<String, Object>> uriEncoding() {
        return ApiResponse.success(service.uriEncoding());
    }

    @GetMapping("/explain")
    @Operation(summary = "规范化速记（八股）", description = "为什么必须规范化 / 常见的坑")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}
