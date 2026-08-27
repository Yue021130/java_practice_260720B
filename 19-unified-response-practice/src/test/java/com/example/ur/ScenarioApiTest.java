package com.example.ur;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全场景接口集成测试：验证统一返回结果封装是否生效。
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allScenariosShouldBehaveCorrectly() throws Exception {
        // 1. 正常查询单个用户：自动包装成 Result，code=0
        assertOkWithCode("/api/user/101", 0);

        // 1.1 查询单个用户（自定义成功提示）：直接返回 Result，不重复包装
        MvcResult detailWithMsgResult = mockMvc.perform(get("/api/user/detail-with-msg/101"))
                .andExpect(status().isOk())
                .andReturn();
        String detailWithMsgBody = detailWithMsgResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(detailWithMsgBody).contains("\"code\":0");
        assertThat(detailWithMsgBody).contains("\"msg\":\"查询成功\"");
        assertThat(detailWithMsgBody).contains("\"id\":101");

        // 2. 查询列表：自动包装成 Result<List<UserVO>>
        assertOkWithCode("/api/user/list", 0);

        // 3. 分页查询：自动包装成 Result<PageResult<UserVO>>
        assertOkWithCode("/api/user/page?pageNum=1&pageSize=3", 0);

        // 4. 创建用户：@Valid 校验通过，code=0
        String validUser = "{\"name\":\"周八\",\"age\":25,\"email\":\"zhouba@example.com\",\"phone\":\"13300133003\",\"password\":\"123456\"}";
        assertPostOkWithCode("/api/user/create", validUser, 0);

        // 5. 创建用户：@Valid 校验失败，code=400
        String invalidUser = "{\"name\":\"\",\"age\":-1,\"email\":\"invalid\",\"phone\":\"123\",\"password\":\"123\"}";
        assertPostOkWithCode("/api/user/create", invalidUser, 400);

        // 6. 更新不存在的用户：BusinessException，code=404
        String notExistUser = "{\"id\":99999,\"name\":\"不存在\",\"age\":20,\"email\":\"no@example.com\",\"phone\":\"13300133004\",\"password\":\"123456\"}";
        assertPostOkWithCode("/api/user/update", notExistUser, 404);

        // 7. 手动包装示例：Controller 返回 Result，不会重复包装
        assertOkWithCode("/api/user/manual-wrap/101", 0);

        // 8. 业务异常示例：code=404
        assertOkWithCode("/api/user/not-found", 404);

        // 9. String 返回值推荐做法：Controller 直接返回 Result<String>，JSON 结构
        MvcResult stringResult = mockMvc.perform(get("/api/user/raw-string"))
                .andExpect(status().isOk())
                .andReturn();
        String stringBody = stringResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(stringBody).contains("\"code\":0");
        assertThat(stringBody).contains("\"data\":\"ok\"");

        // 9.1 String 跳过包装对比示例：@IgnoreResultWrap 直接返回纯文本
        MvcResult stringBareResult = mockMvc.perform(get("/api/user/raw-string-bare"))
                .andExpect(status().isOk())
                .andReturn();
        String stringBareBody = stringBareResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(stringBareResult.getResponse().getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
        assertThat(stringBareBody).isEqualTo("ok");
        assertThat(stringBareBody).doesNotContain("\"code\"");

        // 10. 文件下载：跳过统一包装，Content-Type 是文件流
        MvcResult downloadResult = mockMvc.perform(get("/api/user/download"))
                .andExpect(status().isOk())
                .andReturn();
        String contentType = downloadResult.getResponse().getContentType();
        assertThat(contentType).isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        String downloadBody = downloadResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(downloadBody).doesNotContain("\"code\"");
    }

    private void assertOkWithCode(String url, int expectedCode) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("\"code\":" + expectedCode);
    }

    private void assertPostOkWithCode(String url, String json, int expectedCode) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("\"code\":" + expectedCode);
    }
}
