package com.example.eep.service;

import com.example.eep.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EasyExcel 导出服务测试。
 */
@SpringBootTest
class EasyExcelExportServiceTest {

    @Autowired
    private EasyExcelExportService easyExcelExportService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void clean() {
        productRepository.deleteAll();
    }

    @Test
    void initProducts_shouldCreate20Rows() {
        Map<String, Object> result = easyExcelExportService.initProducts();
        assertThat(result.get("count")).isEqualTo(20);
        assertThat(productRepository.count()).isEqualTo(20L);
    }

    @Test
    void exportProducts_shouldReturnExcel() throws Exception {
        easyExcelExportService.initProducts();
        MockHttpServletResponse response = new MockHttpServletResponse();
        easyExcelExportService.exportProducts(response);

        assertThat(response.getContentType())
                .contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getContentAsByteArray().length).isGreaterThan(0);
    }
}
