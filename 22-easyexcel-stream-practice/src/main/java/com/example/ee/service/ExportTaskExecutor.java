package com.example.ee.service;

import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.example.ee.config.ExportProperties;
import com.example.ee.entity.Order;
import com.example.ee.excel.ExportTaskStatus;
import com.example.ee.excel.OrderExportVO;
import com.example.ee.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 异步导出任务执行器。
 *
 * <p>单独拆出类，确保 {@link Async} 注解通过 Spring 代理生效。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportTaskExecutor {

    private static final int BATCH_SIZE = 5000;

    private final OrderRepository orderRepository;
    private final ExportProperties exportProperties;
    private final DataGenerator dataGenerator;

    /** 任务状态共享存储（生产环境应使用 Redis / DB）。 */
    private final Map<String, ExportTaskStatus> taskStore = new ConcurrentHashMap<>();

    /**
     * 注册任务状态，便于后续查询。
     */
    public void register(ExportTaskStatus status) {
        taskStore.put(status.getTaskId(), status);
    }

    public ExportTaskStatus getStatus(String taskId) {
        return taskStore.get(taskId);
    }

    /**
     * 异步执行导出，落盘到 workDir。
     */
    @Async
    public void runExport(String taskId, long totalRows) {
        ExportTaskStatus status = taskStore.get(taskId);
        status.setStatus("RUNNING");

        File file = new File(exportProperties.getWorkDir(), "orders-" + taskId + ".xlsx");

        try {
            long dbCount = orderRepository.count();
            if (dbCount < totalRows) {
                dataGenerator.generate(totalRows);
            }

            ExcelWriter writer = com.alibaba.excel.EasyExcel.write(file, OrderExportVO.class).build();
            WriteSheet sheet = com.alibaba.excel.EasyExcel.writerSheet("订单").build();

            long processed = 0;
            int page = 0;
            while (processed < totalRows) {
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
                status.setProcessedRows(processed);
                page++;

                if (processed >= totalRows) {
                    break;
                }
            }
            writer.finish();

            status.setStatus("SUCCESS");
            status.setFileUrl("/api/excel/export/async/" + taskId + "/download");
            status.setFinishTime(LocalDateTime.now());
            log.info("[异步导出] 任务 {} 完成，文件 {}", taskId, file.getAbsolutePath());
        } catch (Exception e) {
            status.setStatus("FAILED");
            status.setErrorMsg(e.getMessage());
            status.setFinishTime(LocalDateTime.now());
            log.error("[异步导出] 任务 {} 失败", taskId, e);
        }
    }
}
