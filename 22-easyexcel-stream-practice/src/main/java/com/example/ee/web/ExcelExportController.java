package com.example.ee.web;

import com.example.ee.common.ApiResponse;
import com.example.ee.excel.ExportTaskStatus;
import com.example.ee.service.ExcelExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Excel 导出 REST 接口。
 */
@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
@Tag(name = "EasyExcel 流式导出", description = "100万行导出内存优化实战")
public class ExcelExportController {

    private final ExcelExportService excelExportService;

    @PostMapping("/generate")
    @Operation(summary = "生成模拟订单数据")
    public ApiResponse<Map<String, Object>> generate(@RequestParam(defaultValue = "10000") long count) {
        return ApiResponse.ok(excelExportService.generateData(count));
    }

    @GetMapping("/export/in-memory")
    @Operation(summary = "错误示范：全量加载导出（小数据量可用，大数据量 OOM）")
    public void exportInMemory(HttpServletResponse response) throws IOException {
        excelExportService.exportAllInMemory(response);
    }

    @GetMapping("/export/stream")
    @Operation(summary = "正确示范：流式导出（分页查询 + ExcelWriter）")
    public void exportStream(HttpServletResponse response) throws IOException {
        excelExportService.exportStream(response);
    }

    @PostMapping("/export/async")
    @Operation(summary = "提交异步导出任务")
    public ApiResponse<ExportTaskStatus> submitAsync(@RequestParam(defaultValue = "100000") long totalRows) {
        return ApiResponse.ok(excelExportService.submitAsyncExport(totalRows));
    }

    @GetMapping("/export/async/{taskId}/status")
    @Operation(summary = "查询异步导出任务状态")
    public ApiResponse<ExportTaskStatus> asyncStatus(@PathVariable String taskId) {
        return ApiResponse.ok(excelExportService.getTaskStatus(taskId));
    }

    @GetMapping("/export/async/{taskId}/download")
    @Operation(summary = "下载异步导出的 Excel 文件")
    public ResponseEntity<Resource> downloadAsync(@PathVariable String taskId) {
        Resource resource = excelExportService.downloadAsyncFile(taskId);
        String encoded = URLEncoder.encode("orders-async.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encoded)
                .body(resource);
    }

    @GetMapping("/explain")
    @Operation(summary = "八股速记：EasyExcel 流式导出核心考点")
    public ApiResponse<Map<String, Object>> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", "EasyExcel 流式导出核心八股");

        Map<String, String> points = new LinkedHashMap<>();
        points.put("为什么 OOM", "一次性把所有数据查出来装 List，内存 = 数据行 × 每行对象大小；100万行轻松几个 G。");
        points.put("流式写入核心", "分页/游标查询 + ExcelWriter 多次 write + 最后 finish；任何时刻只保留一批数据。");
        points.put("ExcelWriter 复用", "一个 Sheet 对应一个 WriteSheet，整个导出过程只创建一个 writer，不要每页重建。");
        points.put("SXSSF 底层", "EasyExcel 默认基于 POI 的 SXSSF，只保留窗口行在内存，超出的刷盘到临时文件。");
        points.put("样式陷阱", "逐行设置单元格样式会产生大量样式对象，应使用注解统一设置或使用默认样式。");
        points.put("自动列宽", "autoWidth 需要缓存整列数据计算宽度，大数据量会占用内存，建议固定列宽。");
        points.put("单 Sheet 上限", "Excel 单 Sheet 最大 1048576 行，超过要拆 Sheet。");
        points.put("异步导出", "大文件/慢接口应后台生成，前端轮询进度，避免 HTTP 超时和阻塞用户。");
        points.put("数据库配合", "用 LIMIT/OFFSET 分页或 MyBatis Cursor/流式查询，不要用 select * from table。");
        points.put("JVM 参数", "大数据导出建议 -Xms 与 -Xmx 一致，避免堆扩容；必要时调大老年代。");
        result.put("points", points);

        Map<String, String> compare = new LinkedHashMap<>();
        compare.put("错误做法", "List<Order> all = orderRepository.findAll(); EasyExcel.write(...).doWrite(all);");
        compare.put("正确做法", "while(page.hasContent()) { writer.write(page.getContent(), sheet); page++; } writer.finish();");
        result.put("compare", compare);

        return ApiResponse.ok(result);
    }
}
