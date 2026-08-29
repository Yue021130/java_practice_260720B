package com.example.ee.service;

import com.example.ee.excel.ExportTaskStatus;
import com.example.ee.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ExcelExportService 单元/集成测试。
 */
@SpringBootTest
class ExcelExportServiceTest {

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ExportTaskExecutor taskExecutor;

    @BeforeEach
    void clean() {
        orderRepository.deleteAll();
    }

    @Test
    void generateData_shouldCreateOrders() {
        Map<String, Object> result = excelExportService.generateData(1234);
        assertThat(result.get("generated")).isEqualTo(1234L);
        assertThat(orderRepository.count()).isEqualTo(1234L);
    }

    @Test
    void exportStream_shouldWriteExcel() throws Exception {
        excelExportService.generateData(100);

        MockHttpServletResponse response = new MockHttpServletResponse();
        excelExportService.exportStream(response);

        assertThat(response.getContentType())
                .contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getContentAsByteArray().length).isGreaterThan(0);
    }

    @Test
    void exportStream_shouldRejectWhenNoData() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThatThrownBy(() -> excelExportService.exportStream(response))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("没有数据");
    }

    @Test
    void submitAsyncExport_shouldCreateTask() throws InterruptedException {
        excelExportService.generateData(100);
        ExportTaskStatus status = excelExportService.submitAsyncExport(100);
        assertThat(status.getTaskId()).isNotBlank();

        // 等待异步任务完成
        for (int i = 0; i < 30; i++) {
            ExportTaskStatus latest = taskExecutor.getStatus(status.getTaskId());
            if ("SUCCESS".equals(latest.getStatus())) {
                break;
            }
            Thread.sleep(200);
        }

        ExportTaskStatus finalStatus = taskExecutor.getStatus(status.getTaskId());
        assertThat(finalStatus.getStatus()).isEqualTo("SUCCESS");
        assertThat(finalStatus.getProcessedRows()).isEqualTo(100L);
    }
}
