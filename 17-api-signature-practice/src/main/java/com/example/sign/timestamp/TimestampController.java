package com.example.sign.timestamp;

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
 * 04. 防重放-时间戳：±窗口校验，过期/未来请求拒绝。
 */
@RestController
@RequestMapping("/api/timestamp")
@RequiredArgsConstructor
@Tag(name = "04. 防重放-时间戳", description = "±5 分钟窗口 / 过期与未来请求拒绝")
public class TimestampController {

    private final TimestampService service;

    @GetMapping("/demo")
    @Operation(summary = "时间戳窗口校验", description = "传一个时间戳，看是否在允许窗口内")
    public ApiResponse<Map<String, Object>> demo(@RequestParam(defaultValue = "now") String timestamp,
                                                 @RequestParam(defaultValue = "300") long skewSeconds) {
        return ApiResponse.success(service.demo(timestamp, skewSeconds));
    }

    @GetMapping("/explain")
    @Operation(summary = "时间戳防重放速记（八股）", description = "为什么能防重放 / 与 nonce 的分工")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}
