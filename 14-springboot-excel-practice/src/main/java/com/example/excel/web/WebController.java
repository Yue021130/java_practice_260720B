package com.example.excel.web;

import com.example.excel.common.ApiResponse;
import com.example.excel.support.ExcelWebSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 09. Web 下载与导入实战实验接口。
 */
@RestController
@RequestMapping("/api/web")
@RequiredArgsConstructor
@Tag(name = "09. Web 下载与导入实战", description = "响应头规范 / 中文文件名 / 上传校验 / 权限与防盗链")
public class WebController {

    private final WebService service;

    @GetMapping("/download")
    @Operation(summary = "带规范响应头下载", description = "Content-Type / Content-Disposition(RFC5987) / Content-Length 齐全")
    public ResponseEntity<byte[]> download() {
        return ExcelWebSupport.xlsx("员工名单.xlsx", service.downloadBytes());
    }

    @GetMapping("/download-rule")
    @Operation(summary = "下载规范速记（八股）", description = "响应头 / 中文文件名 / 权限 / 防盗链")
    public ApiResponse<Map<String, Object>> downloadRule() {
        return ApiResponse.success(service.downloadRule());
    }

    @PostMapping("/import")
    @Operation(summary = "真实上传导入", description = "multipart 上传，校验大小/类型/空文件后解析")
    public ApiResponse<Map<String, Object>> importFile(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(service.importFile(file));
    }

    @GetMapping("/upload-limit")
    @Operation(summary = "上传限制说明", description = "multipart 配置 / 超大文件方案")
    public ApiResponse<Map<String, Object>> uploadLimit() {
        return ApiResponse.success(service.uploadLimit());
    }
}
