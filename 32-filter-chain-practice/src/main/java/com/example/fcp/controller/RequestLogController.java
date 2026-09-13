package com.example.fcp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fcp.common.Result;
import com.example.fcp.dto.RequestLogVO;
import com.example.fcp.service.RequestLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 请求审计日志接口：数据由 TimingFilter 自动写入（含鉴权失败的 401）。
 */
@Tag(name = "链路日志", description = "TimingFilter 写入的请求审计日志")
@RestController
@RequestMapping("/api/request-log")
public class RequestLogController {

    private final RequestLogService requestLogService;

    public RequestLogController(RequestLogService requestLogService) {
        this.requestLogService = requestLogService;
    }

    @Operation(summary = "分页列表")
    @GetMapping("/page")
    public Result<Page<RequestLogVO>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String traceId,
                                           @RequestParam(required = false) String uri,
                                           @RequestParam(required = false) Integer statusCode) {
        return Result.ok(requestLogService.page(current, size, traceId, uri, statusCode));
    }
}
