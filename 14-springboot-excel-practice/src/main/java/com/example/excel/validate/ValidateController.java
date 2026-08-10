package com.example.excel.validate;

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

import java.io.IOException;
import java.util.Map;

/**
 * 06. 数据校验与错误反馈实验接口。
 */
@RestController
@RequestMapping("/api/validate")
@RequiredArgsConstructor
@Tag(name = "06. 数据校验与错误反馈", description = "逐行校验 / 合法错误分行 / 问题清单回写 / 真实上传")
public class ValidateController {

    private final ValidateService service;

    @PostMapping("/import-demo")
    @Operation(summary = "校验导入演示（JSON）", description = "内存样本 8 行（3 坏 5 好），返回合法行/错误行/行号/原因")
    public ApiResponse<Map<String, Object>> importDemo() {
        return ApiResponse.success(service.importDemo());
    }

    @GetMapping("/sample-download")
    @Operation(summary = "下载样本文件", description = "含 3 条坏数据的导入模板，可手动下载后去「真实上传」试")
    public ResponseEntity<byte[]> sampleDownload() {
        return ExcelWebSupport.xlsx("学生导入模板.xlsx", service.sampleBytes());
    }

    @PostMapping("/import")
    @Operation(summary = "真实上传导入", description = "multipart 上传 .xlsx，走与演示相同的校验链路")
    public ApiResponse<Map<String, Object>> importFile(@RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.success(service.importFile(file.getInputStream(), file.getOriginalFilename()));
    }

    @GetMapping("/error-download")
    @Operation(summary = "下载问题清单.xlsx", description = "错误回写：把校验失败的行导出成问题清单")
    public ResponseEntity<byte[]> errorDownload() {
        return ExcelWebSupport.xlsx("问题清单.xlsx", service.errorReportBytes());
    }

    @GetMapping("/rules")
    @Operation(summary = "导入校验速记（八股）", description = "三层校验 / 校验时机 / 事务边界")
    public ApiResponse<Map<String, Object>> rules() {
        return ApiResponse.success(service.rules());
    }
}
