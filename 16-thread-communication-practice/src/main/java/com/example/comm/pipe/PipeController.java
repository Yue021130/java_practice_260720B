package com.example.comm.pipe;

import com.example.comm.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 09. 基于 IO / 其他通道：管道流 + 跨进程通信思路。
 */
@RestController
@RequestMapping("/api/pipe")
@RequiredArgsConstructor
@Tag(name = "09. 管道与其他通道", description = "PipedStream 单向管道 / Socket / 共享内存跨进程思路")
public class PipeController {

    private final PipeService service;

    @GetMapping("/piped-demo")
    @Operation(summary = "管道流单向通信", description = "写线程写、读线程读，Piped 字节流/字符流传递")
    public ApiResponse<Map<String, Object>> pipedDemo(@RequestParam(defaultValue = "5") int messages) {
        return ApiResponse.success(service.pipedDemo(messages));
    }

    @GetMapping("/cross-process")
    @Operation(summary = "跨进程通道思路", description = "Socket 回环 / 共享内存(MappedByteBuffer) / 文件")
    public ApiResponse<Map<String, Object>> crossProcess() {
        return ApiResponse.success(service.crossProcess());
    }

    @GetMapping("/explain")
    @Operation(summary = "管道速记（八股）", description = "管道流本质 / 单线程死锁警告 / 线程内 vs 跨进程")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}
