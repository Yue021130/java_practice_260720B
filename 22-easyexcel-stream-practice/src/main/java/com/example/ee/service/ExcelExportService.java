package com.example.ee.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.example.ee.config.ExportProperties;
import com.example.ee.entity.Order;
import com.example.ee.excel.ExportTaskStatus;
import com.example.ee.excel.OrderExportVO;
import com.example.ee.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * EasyExcel 导出服务。
 *
 * <p>演示三种导出方式：</p>
 * <ul>
 *     <li><b>全量加载导出（错误示范）</b>：一次性查出所有数据，内存随数据量线性增长，100万行轻松 OOM。</li>
 *     <li><b>流式导出（正确示范）</b>：分页查询 + {@link ExcelWriter} 分批写入，内存稳定在几十 MB。</li>
 *     <li><b>异步导出</b>：大文件后台生成，前端轮询进度，生成后下载。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private static final int BATCH_SIZE = 5000;

    private final OrderRepository orderRepository;
    private final ExportProperties exportProperties;
    private final DataGenerator dataGenerator;
    private final ExportTaskExecutor taskExecutor;

    private Path workDir;

    @PostConstruct
    public void init() throws IOException {
        this.workDir = Paths.get(exportProperties.getWorkDir()).toAbsolutePath().normalize();
        if (!Files.exists(workDir)) {
            Files.createDirectories(workDir);
        }
        log.info("[EasyExcel] 异步导出文件目录: {}", workDir);
    }

    /**
     * 生成模拟订单数据。
     */
    public Map<String, Object> generateData(long count) {
        return dataGenerator.generate(count);
    }

    /**
     * 错误示范：一次性加载全部数据再写入。
     *
     * <p>适用于小数据量演示；大数据量会 OOM。</p>
     */
    public void exportAllInMemory(HttpServletResponse response) throws IOException {
        long total = orderRepository.count();
        if (total == 0) {
            throw new IllegalStateException("没有数据，请先调用 /api/excel/generate 生成数据");
        }

        setExcelResponse(response, "orders-in-memory.xlsx");
        log.info("[错误示范] 开始全量加载导出，共 {} 行", total);

        // 一次性查出所有数据：内存杀手
        List<OrderExportVO> all = orderRepository.findAll().stream()
                .map(OrderExportVO::from)
                .collect(Collectors.toList());

        EasyExcel.write(response.getOutputStream(), OrderExportVO.class)
                .sheet("订单")
                .doWrite(all);

        log.info("[错误示范] 导出完成");
    }

    /**
     * 正确示范：分页查询 + 流式写入。
     *
     * <p>关键点：</p>
     * <ul>
     *     <li>每页只保留当前批次对象；</li>
     *     <li>使用 {@link ExcelWriter} 多次 write，不重复创建；</li>
     *     <li>最后必须 flush / close，否则尾数据可能丢失。</li>
     * </ul>
     */
    public void exportStream(HttpServletResponse response) throws IOException {
        long total = orderRepository.count();
        if (total == 0) {
            throw new IllegalStateException("没有数据，请先调用 /api/excel/generate 生成数据");
        }

        setExcelResponse(response, "orders-stream.xlsx");
        log.info("[流式导出] 开始流式导出，共 {} 行", total);

        long start = System.currentTimeMillis();
        long processed = 0;
        int page = 0;

        // ExcelWriter 是重量级对象，整个导出过程只创建一次
        ExcelWriter writer = EasyExcel.write(response.getOutputStream(), OrderExportVO.class).build();
        WriteSheet sheet = EasyExcel.writerSheet("订单").build();

        try {
            while (true) {
                Pageable pageable = PageRequest.of(page, BATCH_SIZE);
                Page<Order> batch = orderRepository.findAllByOrderByIdAsc(pageable);
                if (!batch.hasContent()) {
                    break;
                }

                List<OrderExportVO> rows = batch.getContent().stream()
                        .map(OrderExportVO::from)
                        .collect(Collectors.toList());

                writer.write(rows, sheet);
                processed += rows.size();
                page++;

                // 主动释放当前批次引用，帮助 GC
                rows.clear();

                if (page % 5 == 0) {
                    log.info("[流式导出] 已写入 {} / {} 行", processed, total);
                }

                if (batch.isLast()) {
                    break;
                }
            }
        } finally {
            // 必须关闭，否则最后一批数据可能还在缓冲区
            writer.finish();
        }

        long cost = System.currentTimeMillis() - start;
        log.info("[流式导出] 完成，{} 行，耗时 {} ms", processed, cost);
    }

    /**
     * 提交异步导出任务。
     *
     * <p>大文件不适合同步 HTTP 响应，改为后台生成，前端轮询进度。</p>
     */
    public ExportTaskStatus submitAsyncExport(long totalRows) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ExportTaskStatus status = new ExportTaskStatus();
        status.setTaskId(taskId);
        status.setStatus("PENDING");
        status.setTotalRows(totalRows);
        status.setCreateTime(LocalDateTime.now());

        taskExecutor.register(status);
        taskExecutor.runExport(taskId, totalRows);
        return status;
    }

    public ExportTaskStatus getTaskStatus(String taskId) {
        ExportTaskStatus status = taskExecutor.getStatus(taskId);
        if (status == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return status;
    }

    public Resource downloadAsyncFile(String taskId) {
        ExportTaskStatus status = getTaskStatus(taskId);
        if (!"SUCCESS".equals(status.getStatus())) {
            throw new IllegalStateException("文件尚未生成完成: " + status.getStatus());
        }
        File file = workDir.resolve("orders-" + taskId + ".xlsx").toFile();
        return new FileSystemResource(file);
    }

    private void setExcelResponse(HttpServletResponse response, String filename) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=" + encoded);
    }
}
