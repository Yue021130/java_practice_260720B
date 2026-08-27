package com.example.caa;

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
 * 全场景接口集成测试：验证自定义注解 + AOP 切面是否生效。
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allScenariosShouldBehaveCorrectly() throws Exception {
        // 1. 操作日志示例
        assertOk("/api/demo/log?id=1");

        // 2. 权限校验：admin 角色通过
        assertOkWithHeader("/api/demo/permission/admin", "X-Role", "admin");
        // 3. 权限校验：user 角色访问 admin 接口被拒绝
        assertForbiddenWithHeader("/api/demo/permission/admin", "X-Role", "user");

        // 4. user:view 权限：admin 和 user 都能访问
        assertOkWithHeader("/api/demo/permission/user", "X-Role", "admin");
        assertOkWithHeader("/api/demo/permission/user", "X-Role", "user");

        // 5. 接口限流：快速调用 3 次，至少有一次被限流
        int limitedCount = 0;
        for (int i = 0; i < 3; i++) {
            MvcResult result = mockMvc.perform(get("/api/demo/rate-limit"))
                    .andReturn();
            String body = result.getResponse().getContentAsString();
            if (body.contains("\"code\":429")) {
                limitedCount++;
            }
        }
        assertThat(limitedCount).isGreaterThan(0);

        // 6. 数据脱敏：手机号、邮箱、身份证被脱敏
        MvcResult maskingResult = mockMvc.perform(get("/api/demo/masking"))
                .andExpect(status().isOk())
                .andReturn();
        String maskingBody = maskingResult.getResponse().getContentAsString();
        assertThat(maskingBody).contains("****");
        assertThat(maskingBody).doesNotContain("13800138001");
        assertThat(maskingBody).doesNotContain("zhangsan@example.com");

        // 7. 数据脱敏列表
        assertOk("/api/demo/masking-list");

        // 8. 耗时监控
        assertOk("/api/demo/timing");

        // 9. 注解组合：admin + 限流通过
        assertOkWithHeader("/api/demo/combine", "X-Role", "admin");

        // 10. 异常日志：业务异常被全局处理
        MvcResult errorResult = mockMvc.perform(get("/api/demo/error-log"))
                .andExpect(status().isOk())
                .andReturn();
        String errorBody = errorResult.getResponse().getContentAsString();
        assertThat(errorBody).contains("\"code\":500");

        // 11. 八股速记
        assertOk("/api/demo/explain");
    }

    private void assertOk(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"code\":200");
    }

    private void assertOkWithHeader(String url, String headerName, String headerValue) throws Exception {
        MvcResult result = mockMvc.perform(get(url).header(headerName, headerValue))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"code\":200");
    }

    private void assertForbiddenWithHeader(String url, String headerName, String headerValue) throws Exception {
        MvcResult result = mockMvc.perform(get(url).header(headerName, headerValue))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"code\":403");
    }
}
