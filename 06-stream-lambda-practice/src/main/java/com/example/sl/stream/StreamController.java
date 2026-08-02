package com.example.sl.stream;

import com.example.sl.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
@Tag(name = "Stream 基础", description = "Stream 创建、中间操作、终止操作与基本类型流")
public class StreamController {

    private final StreamService streamService;

    @PostMapping("/create")
    @Operation(summary = "Stream 创建方式", description = "collection.stream() / Stream.of / IntStream.range / iterate / generate")
    public ApiResponse<Map<String, Object>> create() {
        return ApiResponse.success(streamService.createDemo());
    }

    @PostMapping("/intermediate")
    @Operation(summary = "中间操作", description = "filter / map / flatMap / distinct / sorted / peek / limit / skip")
    public ApiResponse<Map<String, Object>> intermediate() {
        return ApiResponse.success(streamService.intermediateDemo());
    }

    @PostMapping("/terminal")
    @Operation(summary = "终止操作", description = "collect / reduce / forEach / findFirst / anyMatch / max")
    public ApiResponse<Map<String, Object>> terminal() {
        return ApiResponse.success(streamService.terminalDemo());
    }

    @PostMapping("/primitive")
    @Operation(summary = "基本类型流", description = "IntStream / LongStream / DoubleStream 与装箱成本")
    public ApiResponse<Map<String, Object>> primitive() {
        return ApiResponse.success(streamService.primitiveDemo());
    }
}
