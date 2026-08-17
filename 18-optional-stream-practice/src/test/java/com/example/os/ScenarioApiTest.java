package com.example.os;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全场景接口集成测试：验证所有 Controller 接口返回 200 且 data 非空。
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allScenariosShouldReturn200WithData() throws Exception {
        // 01 用户画像
        assertOk("/api/userprofile/aggregate?userId=1");
        assertOk("/api/userprofile/explain");

        // 02 订单报表
        assertOk("/api/report/summary");
        assertOk("/api/report/summary?days=7");
        assertOk("/api/report/by-status");
        assertOk("/api/report/explain");

        // 03 菜单权限树
        assertOk("/api/permission/tree");
        assertOk("/api/permission/tree?roleCode=admin");
        assertOk("/api/permission/explain");

        // 04 批量数据清洗
        assertOk("/api/dataclean/clean");
        assertOk("/api/dataclean/clean?maxRows=10");
        assertOk("/api/dataclean/explain");

        // 05 SKU 最优价格
        assertOk("/api/sku/best-price?productId=1");
        assertOk("/api/sku/best-price?productId=999");
        assertOk("/api/sku/explain");

        // 06 消息通知过滤
        assertOk("/api/notification/filter");
        assertOk("/api/notification/filter?userId=1&type=PROMOTION");
        assertOk("/api/notification/explain");

        // 07 Excel 导入校验
        assertOk("/api/excelimport/validate");
        assertOk("/api/excelimport/explain");

        // 08 分页再加工
        assertOk("/api/paging/transform");
        assertOk("/api/paging/transform?page=1&size=3");
        assertOk("/api/paging/explain");

        // 09 反模式对比
        assertOk("/api/pitfall/wrong-vs-right");
        assertOk("/api/pitfall/explain");
    }

    private void assertOk(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"code\":200");
        assertThat(body).contains("\"data\"");
    }
}
