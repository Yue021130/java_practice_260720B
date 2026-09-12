package com.example.sfp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sfp.common.Result;
import com.example.sfp.dto.ApiLogVO;
import com.example.sfp.service.ApiLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志接口：查询 LogInterceptor 自动记录的请求日志。
 */
@Tag(name = "审计日志", description = "接口请求日志查询")
@RestController
@RequestMapping("/api/log")
public class ApiLogController {

    private final ApiLogService apiLogService;

    public ApiLogController(ApiLogService apiLogService) {
        this.apiLogService = apiLogService;
    }

    @Operation(summary = "分页查询审计日志", description = "可按 IP、URI、状态码、HTTP 方法筛选")
    @GetMapping("/page")
    public Result<Page<ApiLogVO>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String ip,
                                       @RequestParam(required = false) String uri,
                                       @RequestParam(required = false) Integer statusCode,
                                       @RequestParam(required = false) String method) {
        return Result.ok(apiLogService.page(current, size, ip, uri, statusCode, method));
    }
}
