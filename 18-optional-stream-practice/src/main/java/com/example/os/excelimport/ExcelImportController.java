package com.example.os.excelimport;

import com.example.os.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 07 Excel 导入校验接口。
 */
@RestController
@RequestMapping("/api/excelimport")
@RequiredArgsConstructor
public class ExcelImportController {

    private final ExcelImportService excelImportService;

    @GetMapping("/validate")
    @Operation(summary = "Excel 导入校验", description = "逐行校验导入数据，返回成功行、失败行与空值率。")
    public ApiResponse<Map<String, Object>> validate() {
        return ApiResponse.ok(excelImportService.validate());
    }

    @GetMapping("/explain")
    @Operation(summary = "八股速记", description = "返回本场景的核心考点与常见陷阱。")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.ok(excelImportService.explain());
    }
}
