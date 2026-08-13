package com.example.sign.body;

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
 * 06. 请求体完整性：Content-MD5 / HashedPayload 防 body 篡改。
 */
@RestController
@RequestMapping("/api/body")
@RequiredArgsConstructor
@Tag(name = "06. 请求体完整性", description = "Content-MD5 / HashedPayload / 篡改 body 即签名失败")
public class BodyController {

    private final BodyService service;

    @GetMapping("/demo")
    @Operation(summary = "body 完整性演示", description = "body 参与签名：篡改 body 后签名对不上")
    public ApiResponse<Map<String, Object>> demo(@RequestParam(defaultValue = "{\"name\":\"张三\",\"age\":20}") String body,
                                                 @RequestParam(defaultValue = "false") boolean tamper) {
        return ApiResponse.success(service.demo(body, tamper));
    }

    @GetMapping("/explain")
    @Operation(summary = "请求体完整性速记（八股）", description = "为什么 body 要参与签名 / Content-MD5 vs HashedPayload")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}
