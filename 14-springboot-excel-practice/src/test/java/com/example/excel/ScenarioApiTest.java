package com.example.excel;

import com.example.excel.listener.ListenerService;
import com.example.excel.validate.ValidateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全场景接口集成测试。
 *
 * 覆盖 10 个章节的 JSON 接口 + 二进制下载接口 + 真实 multipart 上传导入。
 * 全部在内存生成文件，无任何外部依赖，快且稳定。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ValidateService validateService;

    @Autowired
    private ListenerService listenerService;

    @Test
    void basicScenarios() throws Exception {
        ok("/api/basic/export-demo");
        okPost("/api/basic/import-demo");
        ok("/api/basic/overview");
    }

    @Test
    void annotationScenarios() throws Exception {
        ok("/api/annotation/export-demo");
        okPost("/api/annotation/import-demo");
        ok("/api/annotation/explain");
    }

    @Test
    void styleScenarios() throws Exception {
        ok("/api/style/export-demo");
        ok("/api/style/explain");
    }

    @Test
    void mergeHeadScenarios() throws Exception {
        ok("/api/mergehead/export-demo");
        ok("/api/mergehead/explain");
    }

    @Test
    void bigDataScenarios() throws Exception {
        // 演示用小行数，避免测试过慢
        okPost("/api/bigdata/export-demo?rows=1000");
        okPost("/api/bigdata/compare?rows=1000");
        ok("/api/bigdata/strategy");
    }

    @Test
    void validateScenarios() throws Exception {
        MvcResult demo = mockMvc.perform(post("/api/validate/import-demo"))
                .andExpect(status().isOk())
                .andReturn();
        String body = demo.getResponse().getContentAsString(StandardCharsets.UTF_8);
        // 样本 8 行：4 好 4 坏（年龄/姓名空/学号格式/手机号 各一条）
        assertThat(body).contains("\"validCount\":4").contains("\"errorCount\":4");
        ok("/api/validate/rules");
    }

    @Test
    void listenerScenarios() throws Exception {
        // rows=250，默认 batchSize=100 → 100 / 100 / 50 三批
        MvcResult demo = mockMvc.perform(post("/api/listener/import-demo?rows=250"))
                .andExpect(status().isOk())
                .andReturn();
        String body = demo.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("\"totalRows\":250").contains("\"batchCount\":3");
        ok("/api/listener/explain");
    }

    @Test
    void templateScenarios() throws Exception {
        okPost("/api/template/fill-demo");
        ok("/api/template/explain");
    }

    @Test
    void webScenarios() throws Exception {
        ok("/api/web/download-rule");
        ok("/api/web/upload-limit");
    }

    @Test
    void pitfallScenarios() throws Exception {
        ok("/api/pitfall/list");
        ok("/api/pitfall/poi-vs-easyexcel");
        MvcResult mismatch = mockMvc.perform(get("/api/pitfall/head-mismatch-demo"))
                .andExpect(status().isOk())
                .andReturn();
        // 表头名差一个字，id 字段应读到 null
        assertThat(mismatch.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("\"idNull\":true");
        ok("/api/pitfall/tuning");
    }

    @Test
    void downloadEndpointsReturnXlsx() throws Exception {
        assertXlsx("/api/basic/download");
        assertXlsx("/api/annotation/download");
        assertXlsx("/api/style/download");
        assertXlsx("/api/mergehead/download");
        assertXlsx("/api/bigdata/download?rows=50");
        assertXlsx("/api/validate/sample-download");
        assertXlsx("/api/validate/error-download");
        assertXlsx("/api/template/template-download");
        assertXlsx("/api/template/fill-download");
        assertXlsx("/api/web/download");
    }

    @Test
    void realMultipartImportWorks() throws Exception {
        // 用服务的样本字节构造 multipart 上传，走真实上传链路
        byte[] sample = validateService.sampleBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "学生导入模板.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", sample);
        MvcResult result = mockMvc.perform(multipart("/api/validate/import").file(file))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("\"totalRows\":8").contains("\"errorCount\":4");

        // Web 上传（走 UserHead 解析）
        byte[] webSample = listenerService.generateBytes(5);
        MockMultipartFile webFile = new MockMultipartFile(
                "file", "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", webSample);
        MvcResult webResult = mockMvc.perform(multipart("/api/web/import").file(webFile))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(webResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"rows\":5");
    }

    @Test
    void uploadRejectsNonXlsx() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "data.xls", "application/octet-stream", "fake".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/web/import").file(file))
                .andExpect(status().is4xxClientError());
    }

    private void assertXlsx(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String ct = result.getResponse().getContentType();
                    assertThat(ct).contains("spreadsheetml");
                    assertThat(result.getResponse().getHeader("Content-Disposition")).contains("attachment");
                });
    }

    private void ok(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"code\":200");
    }

    private void okPost(String url) throws Exception {
        MvcResult result = mockMvc.perform(post(url))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"code\":200");
    }
}
