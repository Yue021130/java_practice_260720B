package com.example.tl.basic;

import com.example.tl.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/basic")
@RequiredArgsConstructor
@Tag(name = "基础原理", description = "ThreadLocal 线程隔离与 initialValue")
public class BasicController {

    private final BasicService basicService;

    @PostMapping("/isolation")
    @Operation(summary = "线程隔离", description = "两个线程对同一个 ThreadLocal 写不同值，结果互不干扰")
    public ApiResponse<Map<String, Object>> isolation() throws InterruptedException {
        return ApiResponse.success(basicService.isolationDemo());
    }

    @PostMapping("/initial")
    @Operation(summary = "initialValue / withInitial", description = "未 set 时返回默认值，remove 后恢复默认值")
    public ApiResponse<Map<String, Object>> initial() {
        return ApiResponse.success(basicService.initialDemo());
    }
}
