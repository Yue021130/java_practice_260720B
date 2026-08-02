package com.example.sl.optional;

import com.example.sl.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/optional")
@RequiredArgsConstructor
@Tag(name = "Optional", description = "空值安全处理")
public class OptionalController {

    private final OptionalService optionalService;

    @PostMapping("/safe")
    @Operation(summary = "空值安全", description = "ofNullable / map / filter / orElse / orElseThrow / ifPresent")
    public ApiResponse<Map<String, Object>> safe() {
        return ApiResponse.success(optionalService.safeDemo());
    }
}
