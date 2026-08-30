package com.example.eep.web;

import com.example.eep.common.ApiResponse;
import com.example.eep.service.EasypoiImportService;
import com.example.eep.service.EasyExcelExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Excel 导入导出 REST 接口。
 */
@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
@Tag(name = "Excel 导入导出", description = "Easypoi 导入校验 + EasyExcel 自定义 Converter")
public class ExcelController {

    private final EasypoiImportService easypoiImportService;
    private final EasyExcelExportService easyExcelExportService;

    // ==================== Easypoi 导入 ====================

    @PostMapping("/easypoi/import/basic")
    @Operation(summary = "基础导入（无校验）")
    public ApiResponse<?> easypoiImportBasic(@RequestParam("file") MultipartFile file) throws Exception {
        return ApiResponse.ok(easypoiImportService.importBasic(file));
    }

    @PostMapping("/easypoi/import/verify")
    @Operation(summary = "带校验导入：成功落库，失败回写错误日志")
    public ApiResponse<Map<String, Object>> easypoiImportVerify(@RequestParam("file") MultipartFile file) throws Exception {
        return ApiResponse.ok(easypoiImportService.importWithVerify(file));
    }

    @PostMapping("/easypoi/import/map")
    @Operation(summary = "Map 方式导入（无实体注解）")
    public ApiResponse<Map<String, Object>> easypoiImportMap(@RequestParam("file") MultipartFile file) throws Exception {
        return ApiResponse.ok(easypoiImportService.importByMap(file));
    }

    @PostMapping("/easypoi/import/duplicate")
    @Operation(summary = "组内重复校验导入（ThreadLocal）")
    public ApiResponse<Map<String, Object>> easypoiImportDuplicate(@RequestParam("file") MultipartFile file) throws Exception {
        return ApiResponse.ok(easypoiImportService.importWithDuplicateCheck(file));
    }

    @GetMapping("/easypoi/template")
    @Operation(summary = "下载 Easypoi 导入模板")
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        easypoiImportService.downloadTemplate(response);
    }

    // ==================== EasyExcel 导出 ====================

    @PostMapping("/easyexcel/init")
    @Operation(summary = "初始化商品数据")
    public ApiResponse<Map<String, Object>> initProducts() {
        return ApiResponse.ok(easyExcelExportService.initProducts());
    }

    @GetMapping("/easyexcel/export")
    @Operation(summary = "EasyExcel 导出商品（自定义 WhetherConverter）")
    public void exportProducts(HttpServletResponse response) throws Exception {
        easyExcelExportService.exportProducts(response);
    }

    // ==================== 八股速记 ====================

    @GetMapping("/explain")
    @Operation(summary = "八股速记：Excel 导入导出核心考点")
    public ApiResponse<Map<String, Object>> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", "Excel 导入导出核心八股");

        Map<String, String> points = new LinkedHashMap<>();
        points.put("Easypoi 适合", "复杂导入校验、错误日志回写、无注解 Map 导入、多线程导入");
        points.put("EasyExcel 适合", "大文件流式导出、低内存、自定义 Converter");
        points.put("IExcelVerifyHandler", "Easypoi 每行业务校验接口");
        points.put("IExcelModel / IExcelDataModel", "用于回写错误信息和行号");
        points.put("Converter", "EasyExcel 字段级导入导出转换器，如 是/否 ↔ 1/0");
        points.put("POI 版本冲突", "Easypoi 与 EasyExcel 混用时需统一 POI 版本");
        points.put("ThreadLocal 组内校验", "同一批 Excel 内重复行检测，用完注意清理");
        result.put("points", points);

        return ApiResponse.ok(result);
    }
}
